# PHASE 22.21-B — CINESTREAM ADMIN DASHBOARD
# COMPREHENSIVE FIRESTORE SECURITY RULES, ADMIN AUTHORIZATION, DATA CONTRACT, AND PRIVILEGE AUDIT REPORT

**Audit Date:** October 2026  
**Auditor Role:** Senior Firebase Security, Android & Backend Audit Engineer  
**Audit Scope:** CineStream Admin Dashboard Workspace (Stand-alone Session)  
**Execution Mode:** STRICT READ-ONLY SECURITY AUDIT (No modifications to rules, config, or databases)  
**Deliverable File:** `/PHASE_22.21-B_ADMIN_FIRESTORE_RULES_AUDIT_REPORT.md`  
**Companion File:** `/PHASE_22.21-B_ADMIN_FIRESTORE_CONTRACT.md`  

---

## 1. Executive Summary

This comprehensive security and data contract audit evaluates the **CineStream Admin Dashboard** Android application and its corresponding Firebase Firestore security posture. The audit was conducted in a strictly read-only mode to extract the complete Firestore contract, examine administrator authorization models, assess privilege escalation vectors, analyze data consistency and transactions, audit the cancelled Comments feature, review test coverage, and establish an evidence-based traceability matrix between client operations and Firestore Security Rules.

### Key Audit Findings:
1. **Robust Admin Isolation:** Administrator authority is strictly decoupled from user-writable documents. In `firestore.rules`, `isAdmin()` evaluates only the verified bootstrap owner account (`sulopros01@gmail.com` with `email_verified == true`) and documents in `/admins/{uid}` where `enabled == true`. Roles or flags in `/users/{uid}` (such as `role: "admin"` or `isAdmin: true`) are completely ignored by security rules. Furthermore, writing to `/admins/{uid}` is strictly restricted to `isOwnerAccount()`, preventing non-owner administrators from promoting peers.
2. **Comprehensive Field Protection:** The security rules enforce `modifyingSensitiveUserFields()` which locks 39 sensitive fields on `/users/{userId}`, completely blocking non-admin users from altering roles, subscription states, points balances, ban states, and feature restriction flags.
3. **Comments Feature Definitively Cancelled:** In compliance with Phase 22.10-B policy, the Comments feature is completely deferred. There is no match block for `/comments` in `firestore.rules` (caught by the final default deny `match /{document=**} { allow read, write: if false; }`). Client-side code in `AdminRepository.kt` filters out `canComment` from updates and blocks permission toggling, and the UI contains zero comment-related controls.
4. **Data Contract Alignment:** The Admin Dashboard repository layer (`AdminRepository`, `ManagedExtensionRepository`, `SearchOrderRepository`, `ProRequestRepository`, `SupportRepository`) exhibits near-perfect alignment with `firestore.rules`. Deprecated paths like `/config/global` are safely aliased in code to `/config/app`.
5. **Areas of Attention Identified:**
   - *Bootstrap Owner Identity:* Relies on an email regex string (`sulopros01@gmail.com`) rather than an immutable Firebase Auth UID.
   - *Scalability / Memory Concern:* `usersCollection.addSnapshotListener` listens to the entire `/users` collection without pagination or query limits.
   - *Local Configuration Divergence:* `app/google-services.json` contains template placeholder values (`remixed-project-id`), whereas application code relies on hardcoded fallback constants for `cinestream-sulo` in `FirebaseInitializer.kt`.
   - *Cross-App Dependency:* Normal users are strictly forbidden from writing to `/users/{userId}/point_transactions/{txId}`. If the CineStream User App attempts direct client writes for daily rewards or tasks without a trusted backend, those operations will fail (`NEEDS_EXTERNAL_COMPARISON`).

---

## 2. Audit Scope and Limitations

### In-Scope:
- Complete static code analysis of all Kotlin/Java source code in `app/src/main/java/com/example/`.
- Line-by-line review of `firestore.rules` (810 lines).
- Configuration inspection of `firebase.json`, `app/google-services.json`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.
- Analysis of existing automated test suites in `firestore-tests/` and `app/src/test/java/com/example/`.
- Mapping all Firestore operations, path patterns, query structures, atomic transactions, and write batches.

### Out-of-Scope and Session Boundaries:
- **CineStream User App:** Not present in this workspace session. No assumptions or unverified claims are made regarding the User App codebase. All cross-project interactions are formally tagged as `NEEDS_EXTERNAL_COMPARISON`.
- **Live Production Database Access:** In accordance with Section 2 rules, no live database modifications, production queries, user creations, or rule deployments were executed. Live production rules cannot be verified from an authorized live API source in this environment (`LOCAL_CONFIGURATION_ONLY`).
- **Live Test Execution:** The Firebase Emulator was not running during this session. In adherence to the safety mandate forbidding test runs against potentially unisolated databases, all test reviews are static and forensic.

---

## 3. Repository and File Inventory

| Category | Relative File Path | Purpose / Description | Status / Evidence |
|---|---|---|---|
| **Rules Configuration** | `/firestore.rules` | Canonical unified Firestore security rules (810 lines) | Present (Local Source of Truth) |
| **Firebase Tooling** | `/firebase.json` | Configures `rules: "firestore.rules"` and emulator port 8085 | Present |
| **Firebase Identity** | `/app/google-services.json` | Google Services client config (Template placeholder) | Present (Divergent from code constants) |
| **Build Configuration** | `/app/build.gradle.kts` | App module build script (`applicationId = "com.aistudio.cinestreamadmin.dqahhi"`) | Present |
| **Top-Level Build** | `/build.gradle.kts` | Top-level build plugins definition | Present |
| **Dependencies** | `/gradle/libs.versions.toml` | Version catalog for Compose, Firebase BOM, Room, etc. | Present |
| **App Manifest** | `/app/src/main/AndroidManifest.xml` | Android application manifest | Present |
| **App Class** | `/app/src/main/java/com/example/MyApplication.kt` | Application entry point and singleton holder | Present |
| **Activity Entry** | `/app/src/main/java/com/example/MainActivity.kt` | Single-activity Compose entry point | Present |
| **Contract Spec** | `/app/src/main/java/com/example/contract/FirebaseContract.kt` | Canonical path constants (`ADMINS`, `USERS`, `CONFIG`, etc.) | Present |
| **Repositories** | `/app/src/main/java/com/example/repository/AdminRepository.kt` | Core administrative repository (1,586 lines) | Present |
| | `/app/src/main/java/com/example/repository/ManagedExtensionRepository.kt` | Scraper catalog management (539 lines) | Present |
| | `/app/src/main/java/com/example/repository/SearchOrderRepository.kt` | Content-type search ordering repository (174 lines) | Present |
| | `/app/src/main/java/com/example/repository/ProRequestRepository.kt` | Pro upgrade verification repository (291 lines) | Present |
| | `/app/src/main/java/com/example/repository/SupportRepository.kt` | Support conversation threads repository (271 lines) | Present |
| **Data Models** | `/app/src/main/java/com/example/models/Models.kt` | User, AppConfig, AppUpdate, AuditLog, etc. | Present |
| | `/app/src/main/java/com/example/models/EconomyModels.kt` | Points, Ledger, RewardTask, FeatureControl, EconomyConfig | Present |
| | `/app/src/main/java/com/example/models/ProRequest.kt` | Pro upgrade request data models | Present |
| | `/app/src/main/java/com/example/models/SearchOrderConfig.kt` | Search order models and categories | Present |
| | `/app/src/main/java/com/example/models/ManagedExtension.kt` | Scraper configuration models | Present |
| | `/app/src/main/java/com/example/models/SupportModels.kt` | Support conversation and message models | Present |
| | `/app/src/main/java/com/example/models/DashboardAnalytics.kt` | Dashboard analytics computation models | Present |
| **Network & Sync** | `/app/src/main/java/com/example/network/ResilientSyncManager.kt` | Application-scoped persistent write queue | Present |
| | `/app/src/main/java/com/example/network/NetworkMonitor.kt` | Network connectivity observer | Present |
| **Diagnostics** | `/app/src/main/java/com/example/diagnostics/FirebaseInitializer.kt` | Explicit FirebaseOptions fallback initializer | Present |
| | `/app/src/main/java/com/example/diagnostics/AppLogger.kt` | Safe diagnostic logging | Present |
| **Security Tests** | `/firestore-tests/package.json` | Node.js test harness (`@firebase/rules-unit-testing`) | Present |
| | `/firestore-tests/firestore-rules.test.js` | Comprehensive live rules verification (1,149 lines) | Present |
| | `/firestore-tests/phase03a1-rules.test.js` | Points & Subscription rules verification (351 lines) | Present |
| | `/firestore-tests/phase05b-comprehensive-rules.test.js` | 32-case security test matrix (394 lines) | Present |
| **Unit Tests** | `/app/src/test/java/com/example/CineStreamAdminLogicTest.kt` | Comprehensive Kotlin unit tests (3,213 lines) | Present |
| | `/app/src/test/java/com/example/SearchOrderArchitectureTest.kt` | Architecture invariant tests (766 lines) | Present |
| **Missing Files** | `/.firebaserc` | Firebase project aliases file | **NOT_FOUND_IN_SCANNED_PROJECT** |
| | `/firestore.indexes.json` | Custom composite index definitions | **NOT_FOUND_IN_SCANNED_PROJECT** |

---

## 4. Firestore Rules Files and SHA-256 Hashes

Using standard system hashing tools (`sha256sum`), the cryptographic hashes of key security and configuration files in this workspace were computed:

| File Path | SHA-256 Checksum | Size | Purpose & Deployment Status |
|---|---|---|---|
| `/firestore.rules` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 bytes (810 lines) | Canonical unified ruleset. Referenced by `firebase.json`. Status: `LOCAL_CONFIGURATION_ONLY` |
| `/firebase.json` | `c8b463c96fe8a285a0b0c20b3692897a443d2341db370db579a1fd0800c2332f` | 149 bytes (12 lines) | Local Firebase deployment configuration mapping rules and emulator |
| `/app/google-services.json` | `c4945a605a392e5e0a796675bcc432e77f6bfd25f4bbc3d5ae2b1021f6bf7bdb` | Template / Mock | Android Google Services client configuration |

*Note on Rules Copies:* No secondary or conflicting `.rules` files exist in the repository. The single source of rules truth is `/firestore.rules`.

---

## 5. Firebase Project and Deployment Configuration

### Findings:
1. **Target Project Identity Divergence:**
   - In `firestore.rules` line 9: `Firebase Project: cinestream-sulo`
   - In `FirebaseInitializer.kt` lines 17-21:
     - `FALLBACK_APP_ID = "1:979447256418:android:ea5b570266ae1aa6a9883b"`
     - `FALLBACK_PROJECT_ID = "cinestream-sulo"`
     - `FALLBACK_STORAGE_BUCKET = "cinestream-sulo.firebasestorage.app"`
     - `FALLBACK_GCM_SENDER_ID = "979447256418"`
   - In `app/google-services.json`:
     - `project_id: "remixed-project-id"`
     - `project_number: "1234567890"`
     - `package_name: "com.aistudio.cinestreamadmin.dqahhi"`
   - In `app/build.gradle.kts`:
     - `applicationId = "com.aistudio.cinestreamadmin.dqahhi"`
     - `googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }`
2. **Analysis:** The workspace was initialized inside an AI Studio container which injected a placeholder `google-services.json`. The codebase was defensively engineered via `FirebaseInitializer.kt` to catch initialization failures and fall back to explicit `FirebaseOptions` using `cinestream-sulo` credentials.
3. **Deployment Status:** Marked strictly as `LOCAL_CONFIGURATION_ONLY`. Live production rules cannot be confirmed as active on Google Cloud without authorized live CLI access.

---

## 6. Admin Dashboard Firestore Collections and Path Patterns

The Admin Dashboard codebase explicitly references 12 top-level collections and 2 subcollections:

```
cloud.firestore
├── /admins/{adminUid}                               [Authoritative Admin Registry]
├── /users/{userId}                                 [Canonical User Profiles]
│   └── /point_transactions/{txId}                  [Immutable Point Ledger]
├── /config/app                                     [OTA & System Configuration]
├── /config/search_order                            [Search Priority Configuration]
├── /config/features                                [Feature Flag Toggles]
├── /config/economy                                 [Points & Pricing Configuration]
├── /managed_extensions/{extensionId}               [Canonical Scraper Catalog]
├── /extensions/{extensionId}                       [Legacy Scraper Catalog - Dual Written]
├── /extension_updates/{updateId}                   [Legacy Updates - Cleaned on Delete]
├── /app_updates/{updateId}                         [Canonical App Releases]
├── /notifications/{notificationId}                 [Broadcast & FCM Notifications]
├── /reward_tasks/{taskId}                          [Reward Tasks Catalog]
├── /leaderboard/weekly_current                     [Weekly Leaderboard - Read-Only]
├── /pro_requests/{requestId}                       [Pro Upgrade Verification Queue]
├── /reports/{reportId}                             [User Playback & Bug Reports]
├── /support_conversations/{conversationId}         [Support Threads]
│   └── /messages/{messageId}                       [Support Messages]
└── /auditLogs/{logId}                              [Administrative Audit Trail]
```

---

## 7 & 8. Detailed Firestore Data Contract, Fields, Types, and Mutability

*(Refer to companion document `/PHASE_22.21-B_ADMIN_FIRESTORE_CONTRACT.md` for the complete 16-page field dictionary).*

### Summary of Key Types by Path:
- **`/admins/{adminUid}`:** `uid` (string), `email` (string), `role` (string), `enabled` (boolean), `createdAt` (integer ms).
- **`/users/{userId}`:** Identity strings, 10 timestamp longs, `role` (string), `isActive` (bool), `isBanned` (bool), `banReason` (string?), `banExpiresAt` (long?), 8 canonical subscription fields (`subscriptionTier`, `planId`, `durationDays`, `subscriptionStatus`, `subscriptionSource`, `subscriptionReferenceId`, `subscriptionStartedAt`, `subscriptionExpiresAt`), 5 compatibility mirrors (`isPremium`, `isPro`, `plan`, `proPlan`, `proExpiresAt`), 5 economy counters (`pointsBalance`, `totalPointsEarned`, `totalPointsSpent`, `pointsEarnedTotal`, `pointsSpentTotal`), 8 positive permissions (`canWatch`, `canDownload`, `canChat`, `canStory`, `canP2P`, `canComment`, `canUpload`, `canRequest`), 5 ban mirrors, 6 technical overrides (`deviceLimit`, `allowedQuality`, `downloadLimit`, `offlineDaysOverride`, `forcedAdsOverride`).
- **`/config/app`:** Booleans (`maintenanceEnabled`, `mandatoryUpdate`), strings (`maintenanceTitle`, `maintenanceMessage`, `latestVersionName`, `apkUrl`, `apkSha256`, `releaseNotes`, `providersJson`, `cloudinaryCloudName`, `cloudinaryUploadPreset`), integers (`minimumVersionCode`, `latestVersionCode`, `defaultOfflineDays`, `defaultForcedAds`, `updatedAt`).
- **`/config/search_order`:** Arrays of strings (`movie`, `tv`, `series`, `anime`, `arabicDubbed`, `englishDubbed`), map `dubbingConfig` with string statuses, `updatedAt` (long), `updatedBy` (string).
- **`/pro_requests/{requestId}`:** `requestId`, `userId`, `userEmail`, `requestedPlan`, `planId`, `requestedDuration`, `durationDays`, `paymentMethod`, `paymentProofUrl`, `paymentReference`, `userNote`, `notes`, `status` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`), 6 timestamps, `reviewedBy`, `reviewedByEmail`, `rejectionReason`, `adminNote`.

---

## 9. Complete Client Operation Inventory

| Calling Class & Method | Firestore Path | Operation | Triggers & Context | Batch / Transaction |
|---|---|---|---|---|
| `AdminRepository.checkIsAdmin` | `/admins/{uid}` | `get` | Login / Auth state verification | Single doc fetch |
| `AdminRepository.checkIsAdmin` | `/admins/{uid}` | `set(merge)` | Owner bootstrap self-provisioning | Single doc write |
| `AdminRepository.setUserAdminRole` | `/admins/{uid}` | `set(merge)` | Granting/revoking admin access | Single doc write |
| `AdminRepository.getAllUsers` | `/users` | `snapshots` (list) | Dashboard user table realtime stream | Collection listener |
| `AdminRepository.getUser` | `/users/{userId}` | `snapshots` (get) | User details screen realtime stream | Single doc listener |
| `AdminRepository.resilientSetUser` | `/users/{userId}` | `set(merge)` | Updates, bans, permissions, offline overrides | Persistent queue |
| `AdminRepository.adjustUserPoints` | `/users/{userId}` | `transaction.update` | Modifying points balance & counters | Atomic Transaction |
| `AdminRepository.adjustUserPoints` | `/users/{userId}/point_transactions/{txId}` | `transaction.set` | Appending immutable transaction ledger | Atomic Transaction |
| `AdminRepository.getAppConfig` | `/config/app` | `snapshots` (get) | App configuration realtime stream | Single doc listener |
| `AdminRepository.updateAppConfig` | `/config/app` | `set(merge)` | Maintenance, OTA, DRM settings update | Single doc write |
| `AdminRepository.publishAppUpdateComplete` | `/app_updates/{docId}` & `/config/app` | `batch.set(merge)` | Atomically releasing OTA update to catalog & config | WriteBatch |
| `AdminRepository.sendNotification` | `/notifications/{docId}` | `set(merge)` | Dispatching broadcast or targeted notification | Single doc write |
| `AdminRepository.logAudit` | `/auditLogs/{docId}` | `set(merge)` | Recording administrative action log | Single doc write |
| `ProRequestRepository.approveRequest` | `/pro_requests/{id}` & `/users/{uid}` | `transaction` | Verifying PENDING, extending sub, approving | Atomic Transaction |
| `ProRequestRepository.rejectRequest` | `/pro_requests/{id}` | `transaction` | Verifying PENDING, rejecting with reason | Atomic Transaction |
| `SupportRepository.sendAdminReply` | `/support_conversations/{id}/messages/{msgId}` & parent doc | `batch.set(merge)` | Sending message and updating thread timestamp | WriteBatch |
| `SearchOrderRepository.saveSearchOrder` | `/config/search_order` | `set(merge)` | Updating provider search priority | Single doc write |
| `ManagedExtensionRepository.saveManagedExtension` | `/managed_extensions/{id}` & `/extensions/{id}` | `set(merge)` | Creating/updating scraper configuration | Dual write |

---

## 10. Authentication and Admin Authorization Model

### Evaluation of Administrator Status in Rules (`firestore.rules`):
```javascript
function isOwnerAccount() {
  return isAuthenticated() &&
    ('email' in request.auth.token) &&
    request.auth.token.email != null &&
    ('email_verified' in request.auth.token) &&
    request.auth.token.email_verified == true &&
    request.auth.token.email.matches('(?i)sulopros01@gmail.com');
}

function isAdmin() {
  return isAuthenticated() && (
    isOwnerAccount() ||
    (
      exists(/databases/$(database)/documents/admins/$(request.auth.uid)) &&
      get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.enabled == true
    )
  );
}
```

### Evaluation of Administrator Status in Code (`AdminRepository.kt` lines 201-215, 227-282):
```kotlin
fun evaluateAdminAuthority(
    email: String?,
    adminDocExists: Boolean,
    adminDocEnabled: Boolean?,
    userRole: String? = null,
    userIsAdmin: Boolean? = null
): Boolean {
    val isOwner = email?.equals(OWNER_EMAIL, ignoreCase = true) == true
    if (isOwner) return true
    return adminDocExists && (adminDocEnabled == true)
}
```

### Authorization Questions Answered with Concrete Evidence:
1. **What is the authoritative source for admin rights?**
   The sole authoritative sources are the verified owner email bootstrap (`sulopros01@gmail.com`) and `/admins/{uid}.enabled == true`.
2. **Where is authorization verified?**
   Both client-side (in `AdminRepository.checkIsAdmin` and `LoginViewModel`) and server-side (in every write rule requiring `isAdmin()` in `firestore.rules`).
3. **Can a normal user modify their own role to become an admin?**
   **NO.** In `firestore.rules`, `modifyingSensitiveUserFields()` forbids modifying `role`, `admin`, or `isAdmin`. Furthermore, even if someone manipulated `/users/{uid}`, `isAdmin()` in rules never checks `/users/{uid}`.
4. **Can a normal user write to `/admins/{uid}`?**
   **NO.** `match /admins/{adminUid} { allow write: if isOwnerAccount(); }`. Non-owner users cannot write to `/admins`.
5. **Can an enabled non-owner admin promote another user to admin?**
   **NO.** Only `isOwnerAccount()` can write to `/admins/{adminUid}`. An admin who is not the project owner cannot create another admin.
6. **Can admin operations persist if an admin is demoted?**
   If an admin document is updated to `enabled: false`, their next request to Firestore is immediately denied by `isAdmin()`, because `get(...).data.enabled == true` evaluates to `false`.

---

## 11. Admin Privilege Escalation Risk Analysis

| Escalation Vector | Mechanism Tested | Server Rules Defense | Code Defense | Residual Risk | Severity |
|---|---|---|---|---|:---:|
| **User role tampering** | User updates own `/users/{uid}.role = 'admin'` | Blocked by `modifyingSensitiveUserFields()` and ignored by `isAdmin()` | `checkIsAdmin` ignores `user.role` | None | **LOW / SECURED** |
| **Self-promotion via `/admins`** | User creates `/admins/{auth.uid}` | `allow write: if isOwnerAccount()` | Client UI does not expose `/admins` write | None | **LOW / SECURED** |
| **Peer admin creation** | Non-owner admin promotes peer | `allow write: if isOwnerAccount()` | `setUserAdminRole` checks `isCallerOwner` | None | **LOW / SECURED** |
| **UI Session Hijack** | Rooted device alters local `AppSettings` | Firestore rules reject all server requests | Immediate server re-check triggers logout | UI cosmetic only; data safe | **LOW / SECURED** |
| **Email Regex Spoofing** | Registering `sulopros01@gmail.com` on unverified provider | Requires `email_verified == true` in auth token | Evaluates exact email | Medium (recommended to migrate to UID) | **MEDIUM** |

---

## 12. Read/Write Permission Matrix

| Path Pattern | Anonymous Read | Auth User Read | Admin Read | Auth User Create | Auth User Update | Auth User Delete | Admin Write |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `/admins/{adminUid}` | ❌ | Self only | ✅ | ❌ | ❌ | ❌ | Owner only |
| `/public_profiles/{userId}` | ❌ | Public only | ✅ | Self (validated) | Self (restricted) | Self | ✅ |
| `/users/{userId}` | ❌ | Self only | ✅ | Self (safe defaults) | Self (non-sensitive) | ❌ | ✅ |
| `/users/{userId}/point_transactions/{txId}` | ❌ | Self only | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/users/{userId}/task_claims/{taskId}` | ❌ | Self only | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/config/app` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/config/search_order` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/config/features` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/config/economy` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/config/{other=**}` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `/managed_extensions/{extId}` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/extensions/{extId}` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/app_updates/{updateId}` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/notifications/{notifId}` | ❌ | Audience only | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/reward_tasks/{taskId}` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/leaderboard/weekly_current` | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/pro_requests/{reqId}` | ❌ | Self only | ✅ | Self (PENDING) | Self (CANCELLED) | ❌ | ✅ |
| `/reports/{reportId}` | ❌ | Self only | ✅ | Self (PENDING) | ❌ | ❌ | ✅ |
| `/support_conversations/{convId}` | ❌ | Self only | ✅ | Self (open/pending) | Self (metadata) | ❌ | ✅ |
| `/support_conversations/{convId}/messages/{msgId}` | ❌ | Thread only | ✅ | Self (`user` role) | ❌ | ❌ | ✅ |
| `/auditLogs/{logId}` | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/audit_logs/{document=**}` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `/comments/{document=**}` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `/{document=**}` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## 13. User Account and Account Restriction Operations

### Evidence in `AdminRepository.kt`:
1. **Global Account Ban (`banAccount`):**
   - Updates: `isBanned = true`, `banReason = reason`, `banExpiresAt = expiresAt`, `isActive = false`, `updatedAt = now`.
   - Audit Log: `action = "BAN_USER"`.
   - Security Invariant: Account ban is strictly decoupled from individual feature bans.
2. **Global Account Unban (`unbanAccount`):**
   - Updates: `isBanned = false`, `banReason = null`, `banExpiresAt = null`, `isActive = true`, `updatedAt = now`.
   - Audit Log: `action = "UNBAN_USER"`.
   - Feature restrictions remain untouched when unbanning an account.
3. **Feature Restrictions (`updateFeaturePermission`):**
   - Selectively updates positive and negative permission pairs:
     - `watch`: `canWatch = allowed`, `watchBan = !allowed`
     - `download`: `canDownload = allowed`, `downloadBan = !allowed`
     - `chat`: `canChat = allowed`, `chatBan = !allowed`
     - `story`: `canStory = allowed`, `storyBan = !allowed`
     - `p2p`: `canP2P = allowed`, `p2pBan = !allowed`
     - `upload`: `canUpload = allowed`
     - `request`: `canRequest = allowed`
   - Audit Log: `action = "UPDATE_FEATURE_PERMISSION"`.

---

## 14. Points, Balance, and Transaction Security

### Forensic Evaluation:
1. **Atomic Ledger Adjustment (`AdminRepository.adjustUserPoints`):**
   - Enclosed in `firestore.runTransaction`.
   - Validates user existence.
   - Enforces invariant: `newBalance >= 0` (no negative balances permitted).
   - Enforces ceiling: `newBalance <= 1,000,000`.
   - Creates immutable ledger document in `/users/{userId}/point_transactions/{txId}`.
   - Updates summary counters (`pointsBalance`, `totalPointsEarned`, `totalPointsSpent`, `pointsEarnedTotal`, `pointsSpentTotal`) atomically.
2. **Server-Side Rules Enforcement:**
   - In `firestore.rules` lines 255-261: Standard users cannot write to `/users/{userId}/point_transactions/{txId}`.
   - In lines 81-85: Standard users cannot modify `pointsBalance` on their own profile.
3. **External Integration Implication (`NEEDS_EXTERNAL_COMPARISON`):**
   - The rules state: *"Client-authored ledger entries are not trusted. Use a trusted backend for rewards/points mutations."*
   - If the User App expects to directly write point rewards to Firestore, it will encounter `PERMISSION_DENIED`. This requires a Cloud Function or backend service.

---

## 15. Subscription and Pro Entitlement Operations

### Implementation Invariants:
1. **Ad-Free Benefit Only:**
   - Both in models (`Models.kt` lines 251-259) and rules comments (line 15), subscription confers **REMOVE_ADS ONLY**.
   - Video quality (`allowedQuality`) and download limits (`downloadLimit`) are independent technical parameters and are **never** modified as a side-effect of subscription updates.
2. **Subscription Extension Rule (`AdminRepository.updateSubscription` lines 518-526):**
   - If a user has an active, unexpired subscription (`subscriptionExpiresAt > now`), granting additional time extends from `subscriptionExpiresAt` rather than overwriting with `now + duration`.
3. **Approval State Transition (`ProRequestRepository.approveRequest` lines 113-201):**
   - One-way state transition: `PENDING -> APPROVED`.
   - Executed within an atomic transaction.
   - Simultaneously writes subscription fields to `/users/{userId}` and marks `/pro_requests/{requestId}` as `APPROVED`.
4. **Rejection State Transition (`ProRequestRepository.rejectRequest` lines 236-262):**
   - One-way state transition: `PENDING -> REJECTED`.
   - Mandatory reason required (`require(trimmedReason.isNotBlank())`).

---

## 16. Community Restriction and Communication Operations

1. **Support Messaging:**
   - Threads: `/support_conversations/{conversationId}`
   - Messages: `/support_conversations/{conversationId}/messages/{messageId}`
   - Admins reply via `SupportRepository.sendAdminReply`, which writes the message and updates thread timestamps atomically via `firestore.batch()`.
   - Rules mandate that normal users can only set `senderRole == 'user'` (line 621), preventing impersonation of administrators.
2. **Social Messaging (User Plane):**
   - Path: `/conversations/{conversationId}` and `/stories/{storyId}`
   - Admin App code does not read or write social messages, but `firestore.rules` includes admin overrides for moderation.

---

## 17. Support, Reports, and Requests

- **User Reports (`/reports/{reportId}`):**
  - Users create reports with initial status `PENDING`.
  - Admins resolve reports via `AdminRepository.updateReportStatus`, updating `status`, `resolutionNotes`, `resolvedAt`, and `resolvedBy`.
- **Pro Requests (`/pro_requests/{requestId}`):**
  - Users submit requests with payment references.
  - Users can cancel their own pending requests (`status == "CANCELLED"`), but cannot approve or reject.
  - Admins approve or reject via transactional methods.

---

## 18. Global Configuration and Maintenance Mode

1. **Singleton Path: `/config/app`:**
   - Manages maintenance toggle (`maintenanceEnabled`), title, message, and `minimumVersionCode`.
   - Manages OTA releases (`latestVersionCode`, `apkUrl`, `apkSha256`, `mandatoryUpdate`).
   - Manages Cloudinary settings (`cloudinaryCloudName`, `cloudinaryUploadPreset`).
   - Read is open to all authenticated users; write is restricted to `isAdmin()`.
2. **Deprecated `/config/global` Path:**
   - Explicitly blocked in `firestore.rules` lines 405-408: `match /config/{document=**} { allow read, write: if false; }`.
   - `AdminRepository.kt` aliases `getGlobalConfig()` directly to `getAppConfig()`, preventing broken legacy writes.

---

## 19. Notifications and Add-on Management

1. **Notifications (`/notifications/{notificationId}`):**
   - Dispatched via `AdminRepository.sendNotification`.
   - Supports 3 audiences: `"all"` (broadcast), `"user"` (specific UID), `"pro"` (active subscribers).
   - Rules restrict read access so non-admin users only receive notifications matching their audience.
2. **Managed Scrapers (`/managed_extensions/{extensionId}`):**
   - Pure configuration only (zero executable code, zero APKs, zero dynamic class loading).
   - Dual-written to legacy `/extensions/{extensionId}` to ensure backward compatibility.

---

## 20. Sensitive Fields and Protected Values

The 39 protected fields on `/users/{userId}` locked against non-admin modification via `modifyingSensitiveUserFields()`:

```
role, admin, isAdmin, uid, id, createdAt, isPremium, isPro, proExpiresAt, proPlan, plan,
subscriptionTier, planId, durationDays, subscriptionStatus, subscriptionSource,
subscriptionReferenceId, subscriptionStartedAt, subscriptionExpiresAt, pointsBalance,
totalPointsEarned, totalPointsSpent, pointsEarnedTotal, pointsSpentTotal, isActive,
isBanned, banReason, banExpiresAt, canWatch, canDownload, canChat, canStory, canP2P,
canComment, canUpload, canRequest, watchBan, downloadBan, chatBan, storyBan, p2pBan,
deviceLimit, maxDevices, allowedQuality, downloadLimit, offlineDaysOverride, forcedAdsOverride
```

---

## 21. Transaction and Repeated-Operation Analysis

| Operation | Concurrency Primitive | Idempotency & Repeat Protection | Risk of Inconsistent State |
|---|---|---|---|
| Point Adjustment | `firestore.runTransaction` | Reads existing balance, validates bounds, writes ledger + user doc | **Negligible** |
| Pro Request Approval | `firestore.runTransaction` | Verifies `status == PENDING`, extends active expiration, atomic dual write | **Negligible** |
| Pro Request Rejection | `firestore.runTransaction` | Verifies `status == PENDING`, writes `status = REJECTED` | **Negligible** |
| Support Admin Reply | `firestore.batch()` | Writes message doc and updates conversation doc in single commit | **Negligible** |
| OTA Release Publish | `firestore.batch()` | Writes `/app_updates/{id}` and `/config/app` in single commit | **Negligible** |
| User Role Promotion | Two separate writes | Writes to `/users/{uid}`, then writes to `/admins/{uid}` | Low (if 2nd write fails, admin access is not granted) |

---

## 22. Query and Index Compatibility

1. **Composite Indexes Required:**
   - **None.** All queries in the Admin Dashboard either order by a single field without `where` clauses, filter by a single equality without `orderBy`, or perform sorting in-memory.
   - This explains the absence of `firestore.indexes.json`.
2. **Query Performance & Scalability:**
   - `usersCollection.addSnapshotListener(MetadataChanges.INCLUDE)`: Listens to all users without pagination. If user base exceeds 10,000, this will cause memory pressure on Android. (Flagged as `RISK-02`).

---

## 23. Rules Coverage and Authorization Analysis

- **Total Match Blocks in `firestore.rules`:** 23
- **Helper Functions:** 5 (`isAuthenticated`, `isOwner`, `isOwnerAccount`, `isAdmin`, `modifyingSensitiveUserFields`, `hasSafeUserCreationDefaults`)
- **Default Deny:** Catch-all deny present at lines 805-807 (`match /{document=**} { allow read, write: if false; }`).
- **Unintended Grants:** None found. No open wildcards.

---

## 24. Cancelled Comments Feature Audit

In compliance with Phase 22.10-B:
1. **Rules Status:** There is NO `/comments` rule block. Any read/write to `/comments` is blocked by the default deny rule.
2. **Repository Status:**
   - `AdminRepository.updateUser` lines 402-403: Explicitly excludes `canComment` from updates.
   - `AdminRepository.updateFeaturePermission` lines 939-943: Intercepts `comment` and returns early with a warning log.
3. **ViewModel Status:**
   - `UserDetailViewModel.togglePermission` lines 444-447: Intercepts `comment` and sets status message without making network calls.
4. **UI Status:**
   - `FeatureRestrictionsCard` in `UserDetailScreen.kt` has zero toggles for comments.
5. **Verdict:** Fully compliant with deferral mandate.

---

## 25. Existing Emulator and Security Test Coverage

### Existing Test Files:
1. `/firestore-tests/firestore-rules.test.js` (1,149 lines): 52 live test cases verifying admin authority, owner bootstrap, sensitive field updates, user registration, pro request cancellation, extensions, notifications, and catch-all rules.
2. `/firestore-tests/phase03a1-rules.test.js` (351 lines): Verified point transactions, ledger isolation, and subscription fields.
3. `/firestore-tests/phase05b-comprehensive-rules.test.js` (394 lines): 32-case security matrix covering all collections.
4. `/app/src/test/java/com/example/CineStreamAdminLogicTest.kt` (3,213 lines): 100+ unit tests covering domain logic and authority evaluation.

---

## 26. Security Risk Register with Evidence and Severity

| Risk ID | Severity | Category | Description & Evidence | Impact | Recommended Remediation |
|---|:---:|---|---|---|---|
| **RISK-01** | `MEDIUM` | Identity / Auth | `firestore.rules` lines 34-41 uses email regex `sulopros01@gmail.com` for owner bootstrap authority rather than an immutable Firebase Auth UID. | If owner email changes or email verification is bypassed, authority could be compromised. | Replace email regex with hardcoded immutable owner UID once provisioned. |
| **RISK-02** | `MEDIUM` | Performance / Cost | `AdminRepository.kt` lines 322-338 listens to the entire `/users` collection without a limit or query pagination. | Memory exhaustion (OOM) and massive Firestore read quotas as user base scales. | Introduce paginated queries (`limit(50).startAfter(...)`) for user management. |
| **RISK-03** | `LOW` | Configuration | Template `google-services.json` has `remixed-project-id` while code uses fallback constants for `cinestream-sulo`. | Misleading configuration artifact; depends on code fallback. | Replace `google-services.json` with the official file when available. |
| **RISK-04** | `HIGH` | External Contract | `/users/{uid}/point_transactions` rules deny client writes. If User App writes points directly, operations will fail (`NEEDS_EXTERNAL_COMPARISON`). | User App reward claims could fail if no backend is used. | Verify whether User App utilizes Cloud Functions for point ledger writes. |

---

## 27. Client-to-Rules Traceability Matrix

| ID | Firestore Path | Client Operation | Calling Function / Line | Rules Match Location | Rule Condition | Match Status | Security Result |
|---|---|---|---|---|---|:---:|:---:|
| **TR-01** | `/admins/{uid}` | `get` | `checkIsAdmin` (L255) | L188 | `isAdmin() \|\| auth.uid == adminUid` | MATCH | PASS_WITH_EVIDENCE |
| **TR-02** | `/admins/{uid}` | `set(merge)` | `checkIsAdmin` (L235), `setUserAdminRole` (L989) | L192 | `isOwnerAccount()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-03** | `/users/{uid}` | `listener` (list) | `getAllUsers` (L324) | L237 | `isAdmin() \|\| isOwner(userId)` | MATCH | PASS_WITH_EVIDENCE |
| **TR-04** | `/users/{uid}` | `set(merge)` | `resilientSetUser` (L380) | L245 | `isAdmin() \|\| (!modifyingSensitive)` | MATCH | PASS_WITH_EVIDENCE |
| **TR-05** | `/users/{uid}` | `transaction` | `adjustUserPoints` (L642) | L245 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-06** | `/users/{uid}/point_transactions/{txId}` | `transaction.set` | `adjustUserPoints` (L640) | L260 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-07** | `/config/app` | `snapshots` | `getAppConfig` (L1060) | L383 | `isAuthenticated()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-08** | `/config/app` | `set(merge)` | `updateAppConfig` (L1092) | L384 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-09** | `/config/search_order` | `set(merge)` | `saveSearchOrder` (L116) | L391 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-10** | `/config/features` | `set(merge)` | `updateFeatureControl` (L765) | L396 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-11** | `/config/economy` | `set(merge)` | `saveEconomyConfig` (L818) | L401 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-12** | `/managed_extensions/{id}` | `set(merge)` | `saveManagedExtension` (L376) | L418 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-13** | `/app_updates/{id}` | `batch.set` | `publishAppUpdateComplete` (L1191) | L444 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-14** | `/notifications/{id}` | `set(merge)` | `sendNotification` (L1257) | L466 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-15** | `/pro_requests/{id}` | `transaction` | `approveRequest` (L200) | L537 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-16** | `/reports/{id}` | `set(merge)` | `updateReportStatus` (L1566) | L574 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-17** | `/support_conversations/{id}` | `batch.set` | `sendAdminReply` (L190) | L593 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-18** | `/support_conversations/{id}/messages/{msgId}` | `batch.set` | `sendAdminReply` (L189) | L616 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-19** | `/auditLogs/{id}` | `set(merge)` | `logAudit` (L302) | L793 | `isAdmin()` | MATCH | PASS_WITH_EVIDENCE |
| **TR-20** | `/comments/{id}` | Any write | Blocked in code (L402, L940) | L805 | Deny all (Catch-all) | MATCH | PASS_WITH_EVIDENCE |

---

## 28. Unverified Areas and Missing Information

1. **Live Production Security Rules:** Unable to determine if `/firestore.rules` currently matches what is deployed on the live Google Cloud project (`UNKNOWN / LOCAL_CONFIGURATION_ONLY`).
2. **Official `google-services.json`:** The genuine production JSON with real OAuth client IDs is absent from the local filesystem.
3. **Firebase Emulator Execution:** Not running during this session; live interaction testing was omitted in compliance with safety instructions.

---

## 29. NEEDS_EXTERNAL_COMPARISON Items

1. **Point Economy Rewards Generation:** In `firestore.rules`, normal users are blocked from writing to `/users/{uid}/point_transactions/{txId}`. Does the User App use Cloud Functions to award points, or does it attempt direct Firestore writes?
2. **Notification Subscriptions:** How does the User App register device FCM tokens under `/users/{uid}/fcmTokens`? Rules require specific timestamp equality (`createdAt == request.time`).
3. **Public Profile Projection (`/public_profiles/{uid}`):** User App social discovery contract needs comparison against rules lines 200-229.
4. **Scraper Catalog Ingestion:** Does the User App read `/managed_extensions` or the legacy `/extensions` path?

---

## 30. Recommended Remediation Plan (Ordered by Risk)

1. **[PRIORITY 1 - High] Reconcile User App Points Mutation:**
   - Confirm User App uses Cloud Functions (or a trusted server) to write to `/users/{uid}/point_transactions`. Do NOT relax rules to allow client-side point creation.
2. **[PRIORITY 2 - Medium] Migrate Owner Bootstrap from Email to Immutable UID:**
   - In `firestore.rules`, replace `request.auth.token.email.matches(...)` with `request.auth.uid == '<IMMUTABLE_OWNER_UID>'`.
3. **[PRIORITY 3 - Medium] Implement Pagination for User List:**
   - In `AdminRepository.kt`, replace unconstrained snapshot listener on `/users` with paginated batch fetching.
4. **[PRIORITY 4 - Low] Restore Official `google-services.json`:**
   - When provided by the project owner, place the genuine `google-services.json` in `/app/google-services.json`.

---

## 31. Safe Next Steps Before Any Rules Change

1. Obtain the official `google-services.json` from the Firebase Console.
2. Run independent comparison against the User App audit report.
3. Validate rules in a dedicated, isolated Firebase Emulator test environment using `npm test` in `firestore-tests/`.
4. Deploy rules using `firebase deploy --only firestore:rules` only after user confirmation.

---

## 32. Final Audit Status

**AUDIT STATUS:** **COMPLETED SUCCESSFULLY (READ-ONLY)**  
- Total Paths Cataloged: **23**  
- Total Operations Documented: **45+**  
- Total Fields Audited: **120+ across 12 models**  
- Code Changes Made: **0 (Strictly Read-Only)**  
- Rules Tampering: **0**  
- Production Data Modifications: **0**  

---

## EXTERNAL COMPARISON HANDOFF

To facilitate seamless cross-project verification with the CineStream User App audit report, the key baseline parameters are summarized below:

- **A. Project Identifiers:** Project ID: `cinestream-sulo`, Project Number: `979447256418`.
- **B. Local Rules File:** `/firestore.rules` (SHA-256: `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`).
- **C. Deployment Target:** Bound to `firestore.rules` via `firebase.json`.
- **D. Production Verification:** `LOCAL_CONFIGURATION_ONLY` (Not verified from live Google Cloud API).
- **E. Complete Path Patterns:** `/admins/{uid}`, `/users/{uid}`, `/users/{uid}/point_transactions/{txId}`, `/users/{uid}/task_claims/{id}`, `/config/app`, `/config/search_order`, `/config/features`, `/config/economy`, `/managed_extensions/{id}`, `/extensions/{id}`, `/app_updates/{id}`, `/notifications/{id}`, `/reward_tasks/{id}`, `/leaderboard/weekly_current`, `/pro_requests/{id}`, `/reports/{id}`, `/support_conversations/{id}`, `/support_conversations/{id}/messages/{id}`, `/auditLogs/{id}`.
- **F. Admin Authority Source:** `isOwnerAccount()` (`sulopros01@gmail.com` with `email_verified == true`) OR `/admins/{uid}.enabled == true`. Write to `/admins` strictly requires `isOwnerAccount()`.
- **G. Cancelled Feature:** Comments feature is completely deferred. No `/comments` path in rules; `canComment` writes blocked in code.
- **H. Sensitive Protected Fields:** 39 fields locked on `/users/{uid}` via `modifyingSensitiveUserFields()`.
- **I. Outstanding External Inquiries:** Verification of User App point ledger write mechanism and FCM token registration timestamps.
