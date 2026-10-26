const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const root = path.resolve(__dirname, '../..');
const lock = JSON.parse(fs.readFileSync(path.join(root, 'config/firebase/CINESTREAM_FIREBASE_IDENTITY_LOCK.json'), 'utf8'));
const servicePath = path.join(root, 'app/google-services.json');
const services = JSON.parse(fs.readFileSync(servicePath, 'utf8'));
const expected = lock.applications.admin;

test('Firebase project, app ID, package, and JSON fingerprint match the identity lock', () => {
  assert.equal(services.project_info.project_id, lock.firebase.projectId);
  assert.equal(services.project_info.project_number, lock.firebase.projectNumber);
  const actualJsonSha = crypto.createHash('sha256').update(fs.readFileSync(servicePath)).digest('hex');
  assert.equal(actualJsonSha, lock.firebase.googleServicesJsonSha256);
  const client = services.client.find(c => c.client_info.android_client_info.package_name === expected.applicationId);
  assert.ok(client, `Missing registered Android client for ${expected.applicationId}`);
  assert.equal(client.client_info.mobilesdk_app_id, expected.firebaseAppId);
  const gradle = fs.readFileSync(path.join(root, 'app/build.gradle.kts'), 'utf8');
  assert.ok(gradle.includes('applicationId = "' + expected.applicationId + '"'));
});

test('Android OAuth client and certificate SHA-1 match the identity lock', () => {
  const client = services.client.find(c => c.client_info.android_client_info.package_name === expected.applicationId);
  const oauth = client.oauth_client.find(o => o.client_type === 1);
  assert.ok(oauth);
  assert.equal(oauth.client_id, expected.androidOauthClientId);
  assert.equal(oauth.android_info.certificate_hash.toLowerCase(), expected.debugSha1);
  const result = spawnSync('keytool', ['-list', '-v', '-keystore', path.join(root, 'debug.keystore'), '-storepass', lock.android.debugKeystorePassword, '-alias', lock.android.debugKeystoreAlias], {encoding:'utf8'});
  assert.equal(result.status, 0, result.stderr || result.stdout);
  const sha1 = result.stdout.match(/SHA1:\s*([0-9A-F:]+)/i);
  const sha256 = result.stdout.match(/SHA256:\s*([0-9A-F:]+)/i);
  assert.ok(sha1, 'keytool did not report a SHA-1 fingerprint');
  assert.ok(sha256, 'keytool did not report a SHA-256 fingerprint');
  assert.equal(sha1[1].replaceAll(':','').toLowerCase(), expected.debugSha1);
  assert.equal(sha256[1].replaceAll(':','').toLowerCase(), expected.debugSha256);
});

test('Web OAuth client matches the shared server client ID', () => {
  const client = services.client.find(c => c.client_info.android_client_info.package_name === expected.applicationId);
  assert.ok(client.oauth_client.some(o => o.client_type === 3 && o.client_id === lock.firebase.webOauthClientId));
});

test('Google sign-in clears both loading states in the Admin ViewModel', () => {
  const src = fs.readFileSync(path.join(root, 'app/src/main/java/com/example/viewmodels/LoginViewModel.kt'), 'utf8');
  assert.match(src, /isGoogleLoading = false/);
  assert.match(src, /isLoading = false,\s*isGoogleLoading = false/);
  assert.match(src, /isGoogleLoading = true/);
});

test('Admin Google account selection is explicit and the web client ID comes from generated Google Services resources', () => {
  const login = fs.readFileSync(path.join(root, 'app/src/main/java/com/example/ui/screens/LoginScreen.kt'), 'utf8');
  const strings = fs.readFileSync(path.join(root, 'app/src/main/res/values/strings.xml'), 'utf8');
  assert.match(login, /R\.string\.default_web_client_id/);
  assert.match(login, /setAutoSelectEnabled\(false\)/);
  assert.doesNotMatch(strings, /name="default_web_client_id"/);
});



test('Legacy comment fields cannot activate Comments and stale unit expectations remain disabled', () => {
  const model = fs.readFileSync(path.join(root, 'app/src/main/java/com/example/models/Models.kt'), 'utf8');
  const unitTests = fs.readFileSync(path.join(root, 'app/src/test/java/com/example/CineStreamAdminLogicTest.kt'), 'utf8');
  assert.match(model, /val isCommentAllowed: Boolean get\(\) = false/);
  assert.match(unitTests, /isCommentAllowed must remain false because Comments are cancelled/);
  assert.match(unitTests, /legacy canComment field alone must not count as an active restriction/);
  assert.doesNotMatch(unitTests, /assertTrue\(defaultUser\.isCommentAllowed\)/);
});
