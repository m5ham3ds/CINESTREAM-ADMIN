const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '../..');
const read = (rel) => fs.readFileSync(path.join(root, rel), 'utf8');
const activeRules = read('firestore.rules');
const candidateRules = read('docs/firebase/rules-history/candidates/PHASE_22.23_CENTRAL_UNIFIED_RULES_CANDIDATE.rules.txt');
const models = read('app/src/main/java/com/example/models/Models.kt');
const repository = read('app/src/main/java/com/example/repository/AdminRepository.kt');
const viewModels = read('app/src/main/java/com/example/viewmodels/ViewModels.kt');

function extractMatch(source, marker) {
  const at = source.indexOf(marker);
  assert.notEqual(at, -1, `Missing rules block: ${marker}`);
  const open = source.indexOf(' {', at) + 1;
  assert.ok(open > 0, `Missing opening brace: ${marker}`);
  let depth = 0;
  for (let i = open; i < source.length; i++) {
    if (source[i] === '{') depth++;
    if (source[i] === '}') depth--;
    if (depth === 0) return source.slice(at, i + 1);
  }
  throw new Error(`Unclosed block ${marker}`);
}

test('active project rules have a final deny-by-default and no comments route', () => {
  assert.match(activeRules, /match\s*\/\{document=\*\*\}\s*\{\s*allow\s+read\s*,\s*write\s*:\s*if\s+false\s*;/s);
  assert.doesNotMatch(activeRules, /match\s+\/comments(?:\/|\s*\{)/i);
});

test('Admin model cannot mark comments as allowed or miscount every user as restricted', () => {
  assert.match(models, /val isCommentAllowed:\s*Boolean get\(\) = false/);
  const summary = models.match(/val hasFeatureRestrictions:\s*Boolean\s*get\(\)\s*=\s*([^\n]+)/);
  assert.ok(summary, 'hasFeatureRestrictions expression must exist');
  assert.doesNotMatch(summary[1], /isCommentAllowed/);
});

test('Admin repository blocks direct attempts to reconfigure the cancelled Comments feature', () => {
  assert.match(repository, /normalizedFeatureKey in setOf\("comment", "comments", "cancomment"\)/);
  assert.match(repository, /Comments feature is cancelled and cannot be configured/);
  assert.match(repository, /normalizedFeature in setOf\("comment", "comments", "cancomment"\)/);
  assert.match(repository, /updates\.filterKeys[\s\S]*setOf\("comment", "comments", "cancomment"\)/);
  assert.match(viewModels, /normalizedPermissionKey in setOf\("comment", "comments", "cancomment"\)/);
  assert.match(viewModels, /Comments feature is cancelled and cannot be re-enabled/);
});

test('central candidate limits user support updates to message preview fields', () => {
  const block = extractMatch(candidateRules, 'match /support_conversations/{conversationId}');
  assert.match(block, /'lastMessage',\s*'lastMessageTime',\s*'updatedAt'/);
  assert.doesNotMatch(block, /affectedKeys\(\)\.hasOnly\(\[[^\]]*unreadCounts/s);
  assert.doesNotMatch(block, /affectedKeys\(\)\.hasOnly\(\[[^\]]*unreadCount'/s);
});

test('central candidate point ledger is append-only', () => {
  const block = extractMatch(candidateRules, 'match /point_transactions/{txId}');
  assert.match(block, /allow\s+create\s*:\s*if\s+isAdmin\(\)/);
  assert.match(block, /allow\s+update\s*,\s*delete\s*:\s*if\s+false/);
  assert.doesNotMatch(block, /allow\s+write\s*:/);
});

test('central candidate task claims are append-only', () => {
  const block = extractMatch(candidateRules, 'match /task_claims/{taskId}');
  assert.match(block, /allow\s+create\s*:\s*if\s+isAdmin\(\)/);
  assert.match(block, /allow\s+update\s*,\s*delete\s*:\s*if\s+false/);
  assert.doesNotMatch(block, /allow\s+write\s*:/);
});

test('central candidate audit log records are append-only', () => {
  const block = extractMatch(candidateRules, 'match /auditLogs/{logId}');
  assert.match(block, /allow\s+read\s*:\s*if\s+isAdmin\(\)/);
  assert.match(block, /allow\s+create\s*:\s*if\s+isAdmin\(\)/);
  assert.match(block, /allow\s+update\s*,\s*delete\s*:\s*if\s+false/);
  assert.doesNotMatch(block, /allow\s+read\s*,\s*write\s*:/);
});

test('central candidate retains final default deny and never adds a comments route', () => {
  assert.match(candidateRules, /match\s*\/\{document=\*\*\}\s*\{\s*allow\s+read\s*,\s*write\s*:\s*if\s+false\s*;/s);
  assert.doesNotMatch(candidateRules, /match\s+\/comments(?:\/|\s*\{)/i);
});
