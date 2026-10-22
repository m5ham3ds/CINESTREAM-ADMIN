# PHASE 20.1-B3-C5.1 — INDEPENDENT POST-IMPLEMENTATION AUDIT REPORT
## CineStream Admin Dashboard ↔ Firebase Security, Contract Integrity & Regression Verification

---

**Phase Identifier:** `PHASE 20.1-B3-C5.1`  
**Execution Context:** CineStream Admin Dashboard Repository (`com.example` / `com.aistudio.cinestreamadmin.nkhixv`)  
**Historical Baselines:**  
- `PHASE_20.1-B3-C4_REMEDIATION_READINESS_PRODUCTION_RULES_VERIFICATION.md`  
- `docs/ADR-CINESTREAM-001_ACTIVE_BACKEND_FIREBASE.md` (`ACCEPTED — FIREBASE REMAINS ACTIVE BACKEND`)  
- `PHASE_20.1-B3-C5_FIREBASE_REMEDIATION_IMPLEMENTATION_VERIFICATION.md`  
**Execution Timestamp:** October 15, 2026 — 21:30 UTC  
**Auditor Roles:** Senior Android/Kotlin Engineer, Firebase Security Engineer, Firestore Rules Auditor, QA Engineer  
**Overall Audit Status:** **`CONDITIONAL PASS — SPECIFIC BLOCKERS REMAIN`**  

---

## 1. Executive Summary & Final Verdict

This independent forensic post-implementation audit was conducted on the CineStream Admin Dashboard codebase to verify whether the fixes claimed in **Phase 20.1-B3-C5** are present, syntactically and logically correct, secure, backward-compatible, and supported by reproducible evidence.

In strict alignment with **ADR-CINESTREAM-001**, Firebase Authentication, Cloud Firestore, Firestore Security Rules, and Cloudinary remain the sole active and authoritative infrastructure. The planned Cloudflare Workers / TypeScript backend remains strictly deferred and is zero-percent integrated.

### Overall Verdict: `CONDITIONAL PASS — SPECIFIC BLOCKERS REMAIN`

### Key Forensic Findings:
1. **Core Defect Remediation Confirmed (Code Level):**
   - **`DISC-01` (Pro Approval Atomicity):** **`VERIFIED PASS`**. The partial merge in the `catch` block that formerly dropped user subscription upgrades during transaction failure (`ProRequestRepository.kt`) has been completely excised. Failures are rethrown with `throw e`, preserving the `PENDING` request state and preventing unearned entitlements.
   - **`DISC-03` (Support Timestamps):** **`VERIFIED PASS`**. `SupportRepository.sendAdminReply()` dual-writes `"lastMessageAt"` and `"lastMessageTime"` in a single atomic batch. `SupportConversation.effectiveLastMessageTimestamp` deterministically resolves whichever timestamp exists.
   - **`DISC-04` (Notification Targeting):** **`VERIFIED PASS`**. `AdminRepository.sendNotification()` maps audience strings to canonical lowercase (`"all"`, `"user"`, `"pro"`) while preserving `targetType` (`"ALL"`, `"UID"`, `"PRO"`) and `targetUid`.
   - **`DISC-05` (Atomic App Updates):** **`VERIFIED PASS`**. `AdminRepository.publishAppUpdateComplete()` publishes update releases to both `/app_updates/{docId}` and `/config/app` simultaneously via `firestore.batch()`. Both `AppUpdatesScreen` and `ConfigScreen` route through this method.
   - **`DISC-07` (Elimination of `/config/global`):** **`VERIFIED PASS`**. `AdminRepository.getAppConfig()` queries exclusively `/config/app`. If missing, it emits default `AppConfig()` with safe non-privileged defaults. Zero queries are emitted for `/config/global`.
2. **Support Status Operational Risk (`DISC-02`):** **`PARTIAL (OPERATIONAL RISK)`**. Canonical lowercase serialization (`"open"`, `"pending"`, `"resolved"`, `"closed"`) and case-insensitive parsing are verified. However, `SupportConversationStatus.fromString()` falls back to `CLOSED` for unknown or null values. In `SupportInboxScreen.kt`, conversations with unknown or null statuses are excluded from the `OPEN` and `PENDING` active-support queues, hiding them from support agents unless filtered by `ALL` or `CLOSED`.
3. **Owner Security & Document Mutation (`REM-OWN-01`):** **`PARTIAL (EMAIL DEPENDENCY)`**. In `firestore.rules`, sub-admins are blocked from mutating or deleting documents in `/admins/{adminUid}` whose email matches `sulopros01@gmail.com`. However, if the owner document in `/admins/{ownerUid}` lacks the `email` field or has it nullified, the rule evaluates to `true` and allows mutation/deletion. Nevertheless, the owner enjoys server-side privilege immunity via `isAdmin()`, which evaluates the caller's JWT email directly via logical OR (`||`).
4. **Automated Test & Scanner Evidence:**
   - **Static Security Scanner (`scripts/static_security_scan.py`):** **`12/12 CHECKS PASSED (100%)`**.
   - **JVM Automated Unit Tests (`bash ./gradlew testDebugUnitTest`):** **`137/137 TESTS PASSED (0 FAILURES, 0 ERRORS)`** across 5 test suites.
5. **Critical External Blockers:**
   - **Production Rules Parity:** `PRODUCTION RULES PARITY: UNVERIFIED`. Local rules are 403 lines, but live production cloud rules cannot be verified without Firebase Console export.
   - **Email Verification Enforcement:** `EMAIL VERIFICATION ENFORCEMENT: BLOCKED — CONSOLE CONFIGURATION NOT VERIFIED`. Local rules do not evaluate `request.auth.token.email_verified`.
   - **Cross-Project User App Alignment:** `CROSS-PROJECT COMPATIBILITY: BLOCKED — EXTERNAL REPO`. User App repository is in a separate workspace.

---

## 2. Files and Artifacts Actually Inspected

| Category | File Path | Line Count | Inspection Purpose |
|---|---|---|---|
| **Baselines & ADR** | `/PHASE_20.1-B3-C4_REMEDIATION_READINESS_PRODUCTION_RULES_VERIFICATION.md` | 451 | Forensic baseline and defect localization |
| | `/PHASE_20.1-B3-C5_FIREBASE_REMEDIATION_IMPLEMENTATION_VERIFICATION.md` | 247 | Implementation claims to verify |
| | `/docs/ADR-CINESTREAM-001_ACTIVE_BACKEND_FIREBASE.md` | 110 | Binding architecture directive |
| **Data Repositories** | `/app/src/main/java/com/example/repository/ProRequestRepository.kt` | 292 | Pro approval transaction rollback & exception flow |
| | `/app/src/main/java/com/example/repository/SupportRepository.kt` | 270 | Support status writes & dual timestamp batching |
| | `/app/src/main/java/com/example/repository/AdminRepository.kt` | 1,542 | Notifications, atomic updates, config fallback |
| **Domain Models** | `/app/src/main/java/com/example/models/SupportModels.kt` | 85 | SupportConversationStatus enum & timestamp fallback |
| | `/app/src/main/java/com/example/models/Models.kt` | 402 | NotificationRequest, AppConfig, AppUpdate, User |
| | `/app/src/main/java/com/example/contract/FirebaseContract.kt` | 210 | Canonical collection and document constants |
| **UI & ViewModels** | `/app/src/main/java/com/example/viewmodels/ProRequestsViewModel.kt` | 169 | Approval action caller and error propagation |
| | `/app/src/main/java/com/example/viewmodels/SupportViewModel.kt` | 187 | Support conversation filters and sorting |
| | `/app/src/main/java/com/example/viewmodels/ViewModels.kt` | 1,022 | ConfigViewModel OTA update caller & atomic publishing |
| | `/app/src/main/java/com/example/ui/screens/AppUpdatesScreen.kt` | 742 | App update release publishing screen |
| | `/app/src/main/java/com/example/ui/screens/ConfigScreen.kt` | 652 | Secondary config screen update publishing |
| | `/app/src/main/java/com/example/ui/screens/GlobalConfigScreen.kt` | 742 | Maintenance and feature toggling screen |
| | `/app/src/main/java/com/example/ui/screens/SupportInboxScreen.kt` | 517 | Support queue cards and status filter chips |
| | `/app/src/main/java/com/example/ui/screens/NotificationScreen.kt` | 515 | Broadcast dispatch UI & FCM notice |
| **Rules & Build** | `/firestore.rules` | 403 | Complete Firestore security ruleset |
| | `/firebase.json` | 14 | Firestore emulator port and rules path |
| | `/.env.example` | 4 | Secrets Gradle template placeholder |
| | `/app/build.gradle.kts` | 129 | Gradle build dependencies and secrets plugin config |
| **Test Suites** | `/scripts/static_security_scan.py` | 139 | 12-point static security verification script |
| | `/app/src/test/java/com/example/CineStreamAdminLogicTest.kt` | 2,956 | Domain logic, C5 assertions, filter/sort unit tests |
| | `/app/src/test/java/com/example/FirebaseTest.kt` | 13 | Smoke unit test |
| | `/app/src/test/java/com/example/SearchOrderArchitectureTest.kt` | 767 | Search order validator and serialization tests |
| | `/app/src/test/java/com/example/ExampleUnitTest.kt` | 17 | Standard JUnit test |
| | `/app/src/test/java/com/example/ExampleRobolectricTest.kt` | 33 | Robolectric context and Activity launch test |
| | `/app/build/test-results/testDebugUnitTest/*.xml` | 5 XMLs | Live test result logs (137 tests executed) |

---

## 3. C5 Claims Confirmed by Direct Evidence

The following claims from `PHASE_20.1-B3-C5_FIREBASE_REMEDIATION_IMPLEMENTATION_VERIFICATION.md` were confirmed via direct source inspection and test execution:

1. **`DISC-01` Fix in `ProRequestRepository.kt`:** Lines 196–200 catch `Exception`, log it, and execute `throw e`. The previous partial merge on `docRef` alone was completely removed.
2. **`DISC-03` Dual Timestamp Batching:** In `SupportRepository.kt` lines 177–185, `sendAdminReply()` writes both `"lastMessageAt" to now` and `"lastMessageTime" to now` in a single atomic batch.
3. **`DISC-04` Lowercase Notification Target:** In `AdminRepository.kt` lines 1186–1213, `sendNotification()` maps `target` to `"all"`, `"user"`, or `"pro"`, while preserving `targetType` (`"ALL"`, `"UID"`, `"PRO"`).
4. **`DISC-05` Atomic App Update Batch:** In `AdminRepository.kt` lines 1129–1164, `publishAppUpdateComplete()` writes both `/app_updates/{docId}` and `/config/app` simultaneously via `firestore.batch()`.
5. **`DISC-07` Elimination of `/config/global` Fallback:** In `AdminRepository.kt` lines 1020–1043, `getAppConfig()` queries strictly `/config/app`. Missing document emits default `AppConfig()` with safe non-privileged defaults.
6. **`DISC-06` 403-Line Security Rules Synchronization:** `/firestore.rules` contains 403 lines with social chat ban checks (`canChat != false && chatBan != true`), story ban checks (`canStory != false && storyBan != true`), `/fcmTokens`, and `/public` projections.
7. **`REM-BLD-01` Placeholder `.env.example`:** `/.env.example` exists at root containing `MAPS_API_KEY=PLACEHOLDER_KEY`.
8. **Static Scanner:** `scripts/static_security_scan.py` passes 12/12 checks.

---

## 4. Claims Not Independently Verified or Found Incomplete

1. **Production Rules Parity Unverified:** The C5 report claimed production parity, but this was only local file alignment. Active production rules deployed to `cinestream-sulo` remain uninspected.
2. **Pro Approval Transaction Rollback Lacks Integration Test:** Although the code rethrows exceptions, there is zero unit/integration test in `app/src/test/` mocking a transaction failure in `approveRequest`. `FirebaseTest.kt` is only a dummy test (`assertTrue(true)`).
3. **`createOrUpdateAppUpdate()` Still Public:** `AdminRepository.kt` line 1108 retains `createOrUpdateAppUpdate()`, which updates `/app_updates` without updating `/config/app`. While UI screens do not invoke it, it remains accessible in the public repository API.
4. **Owner Document Protection Missing-Email Gap:** Sub-admin protection in `/admins/{adminUid}` relies on `resource.data.email`. If the document lacks `email`, sub-admins can modify or delete it.
5. **Support Casing Fallback Hides Active Conversations:** `SupportConversationStatus.fromString()` falls back to `CLOSED`, causing unmapped or null status conversations to disappear from active support queues.
6. **Gradle Wrapper Script Permissions:** Running `./gradlew` directly fails with exit code 126 (`Permission denied`) because `gradlew` lacks execute permission (`-rw-r--r--`). It must be executed via `bash ./gradlew`.

---

## 5. Detailed Forensic Results: DISC-01 through DISC-07

### DISC-01 — Pro Approval Transaction Integrity
* **Status:** **`VERIFIED PASS (CODE-LEVEL) / NOT TESTED (INTEGRATION-LEVEL)`**
* **Target File:** `app/src/main/java/com/example/repository/ProRequestRepository.kt` (lines 97–201)
* **Caller File:** `app/src/main/java/com/example/viewmodels/ProRequestsViewModel.kt` (lines 107–123)
* **Detailed Findings:**
  1. *Request cannot be marked APPROVED if user update fails:* **VERIFIED.** Both the user entitlement update (`transaction.set(userDocRef, userUpdates, SetOptions.merge())`, line 173) and the request status update (`transaction.update(docRef, updates)`, line 186) occur inside `firestore.runTransaction { ... }.await()`. In Cloud Firestore, writes inside a transaction commit atomically on the server; if the user document write fails or rules reject it, the entire transaction aborts.
  2. *Atomicity of user entitlement and request transition:* **VERIFIED.** Both writes are batched in the transaction commit.
  3. *Transaction failure propagated to caller:* **VERIFIED.** Lines 196–200 catch `Exception`, log it with `AppLogger.e`, and immediately `throw e`.
  4. *No catch block performs partial approval write:* **VERIFIED.** The old code that performed `docRef.set(updates, SetOptions.merge())` on lines 186–199 has been completely excised.
  5. *Audit records do not falsely report success:* **VERIFIED.** Line 190 (`adminRepository.logAudit(...)`) is located strictly *after* `}.await()`. If the transaction throws, line 190 is skipped.
  6. *Repeated approvals behavior:* **VERIFIED.** Line 119 reads the current request snapshot: `val currentStatus = ProRequestStatus.fromString(snapshot.getString("status"))`. If `currentStatus != ProRequestStatus.PENDING`, it throws `IllegalStateException`. Repeated approval attempts on already approved or rejected requests are blocked.
  7. *Administrator visibility of failure:* **VERIFIED.** In `ProRequestsViewModel.kt` lines 116–118, `catch (e: Exception)` catches the rethrown error and sets `_statusMessage.value = "Error approving request: ${e.message}"`, while `onSuccess()` is not called.
  8. *Remaining Risk:* If `userId` is present on the request document but the corresponding user document does not exist in `/users`, Firestore `SetOptions.merge()` will create a new sparse user document containing only the subscription fields, missing standard profile keys (`email`, `username`, `role`). However, the subscription entitlement itself is preserved. In `rejectRequest` (lines 249–263), a transaction failure still falls back to a standalone `docRef.set()` merge; this is acceptable because rejection involves only the request document and does not affect user account entitlements.

---

### DISC-02 — Support Status Compatibility
* **Status:** **`PARTIAL (CONFIRMED OPERATIONAL RISK)`**
* **Target Files:**
  - `app/src/main/java/com/example/models/SupportModels.kt` (lines 11–28)
  - `app/src/main/java/com/example/repository/SupportRepository.kt` (lines 66–67, 205–223)
  - `app/src/main/java/com/example/ui/screens/SupportInboxScreen.kt` (lines 177–227)
* **Detailed Findings:**
  1. *New writes use intended lowercase canonical values:* **VERIFIED PASS.** `SupportRepository.updateStatus()` writes `newStatus.canonicalValue` (lines 210–213), producing `"open"`, `"pending"`, `"resolved"`, or `"closed"`.
  2. *Legacy uppercase values parsed correctly:* **VERIFIED PASS.** `SupportConversationStatus.fromString()` evaluates `value?.trim()?.lowercase()`, successfully mapping `"OPEN"` -> `OPEN`, `"PENDING"` -> `PENDING`, `"RESOLVED"` -> `RESOLVED`, `"CLOSED"` -> `CLOSED`. Verified by unit test `testPhaseC5SupportStatusCanonicalLowercase` in `CineStreamAdminLogicTest.kt`.
  3. *Unknown or missing values handling:* **VERIFIED FAIL / OPERATIONAL RISK.**
     In `SupportModels.kt` line 24:
     ```kotlin
     else -> CLOSED // Safe fallback: never silently convert unknown to OPEN/active
     ```
     In `SupportRepository.kt` line 67:
     ```kotlin
     val safeStatus = SupportConversationStatus.fromString(rawStatus).canonicalValue
     ```
     If a document in `/support_conversations` has a missing `status` field, a null value, or an unmapped string (such as `"new"`, `"active"`, `"in_progress"`, `"waiting_admin"`), `fromString()` maps it to `SupportConversationStatus.CLOSED`.
  4. *Compatibility with UI filters and queries:* **CONFIRMED DEFECT.** In `SupportInboxScreen.kt` lines 177–227, the filter chips are:
     - `ALL (${totalCount})`
     - `OPEN (${openCount})`
     - `PENDING (${pendingCount})`
     - `RESOLVED (${resolvedCount})`
     - `CLOSED (${totalCount - openCount - pendingCount - resolvedCount})`
     Because unmapped/null statuses evaluate to `CLOSED`, any active conversation with a corrupt or alternative status will **disappear from the `OPEN` and `PENDING` active-support lists**. An administrator monitoring active tickets will never see these conversations unless they intentionally filter by `ALL` or `CLOSED`.
  5. *Status transitions preserve business logic:* **VERIFIED PASS.** Updating status to any of the 4 valid states works as intended.
  6. *Firestore Rules alignment:* **VERIFIED PASS.** In `/firestore.rules` line 307–315, support conversations can be updated by admin or conversation owner without strict status string constraints in the Admin ruleset.

---

### DISC-03 — Support Timestamp Compatibility
* **Status:** **`VERIFIED PASS`**
* **Target Files:**
  - `app/src/main/java/com/example/models/SupportModels.kt` (lines 43–64)
  - `app/src/main/java/com/example/repository/SupportRepository.kt` (lines 68–75, 177–185)
  - `app/src/main/java/com/example/viewmodels/SupportViewModel.kt` (line 83)
* **Detailed Findings:**
  1. *Dual timestamp write:* **VERIFIED PASS.** In `SupportRepository.sendAdminReply()` (lines 177–185), the batch update payload contains:
     ```kotlin
     "lastMessageAt" to now,
     "lastMessageTime" to now, // Compatibility with User App contract (DISC-03)
     ```
  2. *Both values represent same logical event:* **VERIFIED PASS.** Both fields are set to the identical variable `val now = System.currentTimeMillis()`.
  3. *Single-field documents remain readable:* **VERIFIED PASS.** In `SupportRepository.kt` lines 68–75:
     ```kotlin
     val lastAt = doc.getLong("lastMessageAt") ?: conv?.lastMessageAt ?: 0L
     val lastTime = doc.getLong("lastMessageTime") ?: conv?.lastMessageTime ?: 0L
     conv?.copy(
         lastMessageAt = if (lastAt > 0L) lastAt else lastTime,
         lastMessageTime = if (lastTime > 0L) lastTime else lastAt
     )
     ```
     If only `lastMessageTime` is present (from the User App), `lastMessageAt` receives its value. If only `lastMessageAt` is present (from legacy Admin), `lastMessageTime` receives its value.
  4. *Sorting and filtering behavior:* **VERIFIED PASS.** In `SupportRepository.kt` line 80, the list is sorted by `it.effectiveLastMessageTimestamp`, which evaluates `lastMessageAt`, `lastMessageTime`, `updatedAt`, and `createdAt`.
     - *Minor Discrepancy:* In `SupportViewModel.kt` line 83, `filteredConversations` sorts by `res.sortedByDescending { it.lastMessageAt }`. Because `lastMessageAt` was populated from `lastTime` during repository deserialization, this works for any conversation with either timestamp. However, for a conversation where both are 0, repository sorting uses `createdAt` while ViewModel sorting leaves them at 0.
  5. *Timestamp types vs Milliseconds:* **VERIFIED PASS.** All writes in the Admin Dashboard use `Long` (milliseconds). Both fields deserialize via `doc.getLong()`. If User App documents store millisecond integers, compatibility is 100%. If User App writes `FieldValue.serverTimestamp()` (Firestore Timestamp object), `doc.getLong()` returns null, which safely falls back to `0L` without throwing.

---

### DISC-04 — Notification Target Integrity
* **Status:** **`VERIFIED PASS (ADMIN-SIDE) / BLOCKED (USER-SIDE)`**
* **Target Files:**
  - `app/src/main/java/com/example/models/Models.kt` (lines 325–357)
  - `app/src/main/java/com/example/repository/AdminRepository.kt` (lines 1182–1220)
  - `app/src/main/java/com/example/ui/screens/NotificationScreen.kt` (lines 275–339)
* **Detailed Findings:**
  1. *Broadcast semantics (`all`):* **VERIFIED PASS.** `TargetType.ALL` maps to canonical target `"all"`. Payload writes `"target" to "all"`.
  2. *Pro audience semantics (`pro`):* **VERIFIED PASS.** `TargetType.PRO` maps to canonical target `"pro"`. Payload writes `"target" to "pro"`.
  3. *Specific user semantics (`user`):* **VERIFIED PASS.** `TargetType.UID` maps to canonical target `"user"`. Payload writes `"target" to "user"` and preserves `"targetUid" to request.targetUid`.
  4. *Conflict resolution between legacy and canonical fields:* **VERIFIED PASS.** Lines 1186–1197 resolve both `request.targetType` and `request.target`:
     ```kotlin
     val canonicalTarget = when (request.targetType) {
         NotificationRequest.TargetType.ALL -> "all"
         NotificationRequest.TargetType.UID -> "user"
         NotificationRequest.TargetType.PRO -> "pro"
     }
     val rawTarget = request.target.trim().lowercase()
     val finalTarget = when (rawTarget) {
         "all" -> "all"
         "uid", "user" -> "user"
         "pro" -> "pro"
         else -> canonicalTarget
     }
     ```
     The output document writes `"target" to finalTarget` (lowercase) and `"targetType" to request.targetType.name` (`"ALL"`, `"UID"`, `"PRO"`), guaranteeing compatibility with both modern and legacy readers.
  5. *User App reader verification:* **BLOCKED — EVIDENCE UNAVAILABLE.** The User App repository source is not available in this workspace. Direct inspection of whether the User App filters against `target == "all" || target == "user"` or `target == "pro"` cannot be performed.
  6. *FCM Delivery Misrepresentation:* **VERIFIED PASS.** The Admin Dashboard does not claim direct FCM tray delivery. Lines 330–334 of `NotificationScreen.kt` explicitly state to the admin:
     > *"Broadcasts are saved to /notifications; background Cloud Functions trigger real-time FCM push delivery to clients."*
     The document is written with `"status" to "PENDING"`. No push backend was improperly injected.

---

### DISC-05 — Atomic App Update Publishing
* **Status:** **`VERIFIED PASS (UI WORKFLOW) / PARTIAL (API BYPASS RISK)`**
* **Target Files:**
  - `app/src/main/java/com/example/repository/AdminRepository.kt` (lines 1108–1164)
  - `app/src/main/java/com/example/viewmodels/ViewModels.kt` (lines 631–705)
  - `app/src/main/java/com/example/ui/screens/AppUpdatesScreen.kt` (lines 148–158)
  - `app/src/main/java/com/example/ui/screens/ConfigScreen.kt` (lines 124–132)
* **Detailed Findings:**
  1. *Dual updates in single operation:* **VERIFIED PASS.** `AdminRepository.publishAppUpdateComplete()` builds a `firestore.batch()` that writes `appUpdatesCollection.document(docId)` and `configCollection.document(FirebaseConfigDocs.APP)` before calling `batch.commit().await()`.
  2. *Correct document IDs and schemas:* **VERIFIED PASS.**
     - `/app_updates/{docId}`: `docId` is `versionCode.toString()` (e.g. `"15"`). Schema matches `AppUpdate` data class.
     - `/config/app`: Document ID is `"app"`. Schema updates `latestVersionCode`, `latestVersionName`, `apkUrl`, `apkSha256`, `mandatoryUpdate`, `releaseNotes`, `minimumVersionCode`, and `updatedAt`.
  3. *Validation prior to commit:* **VERIFIED PASS.** `ConfigViewModel.updateOta()` validates:
     - `versionCode > 0` (line 641).
     - `apkUrl` starts with `"http://"` or `"https://"` (line 645).
     - Note: `publishAppUpdateComplete()` in `AdminRepository.kt` does not re-validate these constraints independently if called outside `ConfigViewModel`.
  4. *Failure propagation:* **VERIFIED PASS.** If `batch.commit().await()` fails, the exception is caught in `updateOta()`, setting `_statusMessage.value = "Failed to publish OTA: ${e.message}"`, which renders a Snackbar on both `AppUpdatesScreen` and `ConfigScreen`.
  5. *Retry and duplicate prevention:* **VERIFIED PASS.** Because `docId` is derived deterministically from `versionCode.toString()`, subsequent publishing calls for the same version overwrite/merge the document rather than creating redundant documents.
  6. *Alternate publishing paths / Bypass analysis:* **CONFIRMED RESIDUAL RISK.** `AdminRepository.kt` line 1108 still exposes:
     ```kotlin
     suspend fun createOrUpdateAppUpdate(update: AppUpdate)
     ```
     This method writes only to `/app_updates/{docId}` and does **not** update `/config/app`. A source code search confirmed that **zero callers** in `app/src/` currently invoke `createOrUpdateAppUpdate()`. However, the method remains in the public repository API.
  7. *User App reading source:* **BLOCKED — EVIDENCE UNAVAILABLE.** Whether the User App reads `/config/app` or `/app_updates` cannot be confirmed without the User App codebase.

---

### DISC-06 — Firestore Rules Integrity
* **Status:** **`VERIFIED PASS (LOCAL SYNCHRONIZATION) / UNVERIFIED (PRODUCTION PARITY)`**
* **Target Files:** `/firestore.rules` (403 lines), `/firebase.json` (14 lines)
* **Detailed Findings:**
  1. *Rules Line Count & Structure:* `/firestore.rules` is exactly 403 lines (matching the 403-line Phase 20.1-B2 hardened specification).
  2. *`/admins/{adminUid}` Authorization:* Lines 127–133 enforce `isAdmin()` or owner email matching for read/write.
  3. *`/users/{userId}` Protection:*
     - Lines 25–73: `modifyingSensitiveUserFields()` protects 45 sensitive keys (including `role`, `isPremium`, `subscriptionTier`, `isPro`, `pointsBalance`, `canWatch`, `canChat`, `storyBan`, `deviceLimit`, `admin`, `isAdmin`).
     - Lines 76–123: `hasSafeUserCreationDefaults()` enforces 42 default constraints on user registration.
  4. *Pro Requests Security:* Lines 330–341 enforce that users can only create their own requests with status `PENDING`, and cannot forge `reviewedBy`, `reviewedAt`, or `rejectionReason`. Updates/deletions are restricted to `isAdmin()`.
  5. *Support Conversations Security:* Lines 307–327 require message senders to be the conversation owner with `senderRole == 'user'`. Updates and deletes are admin-only.
  6. *Notifications & Audit Logs:* Lines 275–285 restrict notification writes and audit log access strictly to `isAdmin()`.
  7. *FCM Tokens & Public Projections:* Dedicated match blocks exist for `/users/{userId}/fcmTokens/{installationId}` (lines 166–169) and `/users/{userId}/public/{docId}` (lines 172–175).
  8. *Social Chat & Story Bans:* Lines 372–380 enforce `canChat != false && chatBan != true`. Lines 386–394 enforce `canStory != false && storyBan != true`.
  9. *Catch-All Deny Rules:* Line 218 (`match /config/{document=**} { allow read, write: if false; }`) and line 397 (`match /{document=**} { allow read, write: if false; }`) are present.
  10. *Overlapping Match Permission Analysis:*
      - Under `/users/{userId}`:
        - `match /point_transactions/{txId}`: `allow write: if isAdmin();`
        - `match /task_claims/{taskId}`: `allow write: if isAdmin();`
        - `match /{subcollection}/{docId}`:
          ```javascript
          allow write: if isAdmin() || (isOwner(userId) && !(subcollection in ['point_transactions', 'task_claims']));
          ```
          The wildcard block explicitly excludes `point_transactions` and `task_claims`. Standard users cannot write to these subcollections via the wildcard rule.
        - `match /public/{docId}` allows `read: if isAuthenticated()`. The wildcard rule allows `read: if isAdmin() || isOwner(userId)`. Because permissions are OR-combined, the broader rule applies, intentionally allowing authenticated users to read public profiles.
      - There are no overlapping rules that inadvertently weaken administrative controls or user boundaries.
  11. *Production Parity Status:* **`PRODUCTION RULES PARITY: UNVERIFIED`**. Direct inspection of the live Firebase Cloud Console project has not occurred.

---

### DISC-07 — Configuration Fallback
* **Status:** **`VERIFIED PASS`**
* **Target File:** `app/src/main/java/com/example/repository/AdminRepository.kt` (lines 1020–1043)
* **Detailed Findings:**
  1. *Obsolete `/config/global` fallback removed:* **VERIFIED PASS.** In `getAppConfig()` (lines 1022–1043), when `snapshot == null || !snapshot.exists()`, line 1041 executes `trySend(AppConfig())` directly. The old call `configCollection.document(FirebaseConfigDocs.GLOBAL).get().addOnSuccessListener { ... }` has been completely deleted.
  2. *Missing `/config/app` handling:* **VERIFIED PASS.** If `/config/app` does not exist in Firestore, the flow emits `AppConfig()`.
  3. *Default values security audit:* **VERIFIED PASS.** In `AppConfig` (`Models.kt` lines 261–283):
     - `maintenanceEnabled = false` (does not inadvertently lock out all users).
     - `minimumVersionCode = 1` (does not trigger forced app obsolescence).
     - `latestVersionCode = 1`
     - `mandatoryUpdate = false`
     - `defaultOfflineDays = 2`
     - `defaultForcedAds = 5`
     - `cloudinaryCloudName = "cinestream"`
     - `cloudinaryUploadPreset = "cinestream_unsigned"` (unsigned client preset; no API secrets).
     No privileged or sensitive feature flags are accidentally enabled by default.
  4. *Observability of errors:* **VERIFIED PASS.** Lines 1024–1029:
     ```kotlin
     if (error != null) {
         AppLogger.w("AdminRepository", "Firestore config non-fatal error: ${error.message}")
         if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
             close(error)
         }
         return@addSnapshotListener
     }
     ```
     Actual Firestore network errors or permission rejections are not swallowed or converted to `AppConfig()`; `close(error)` is invoked, propagating the failure through the Flow.
  5. *Alternate callers of `/config/global`:* **VERIFIED PASS.** A codebase-wide search across `app/src/main/java/` for `FirebaseConfigDocs.GLOBAL` or `"global"` revealed zero queries. Lines 1066–1067 provide backward-compatibility aliases `getGlobalConfig()` and `updateGlobalConfig()`, but both forward directly to `getAppConfig()` and `updateAppConfig()`.

---

## 6. Owner Protection Audit (`REM-OWN-01`)

### 6.1 Logic Analysis of `/admins/{adminUid}`
Target Rule (`firestore.rules` lines 127–133):
```javascript
match /admins/{adminUid} {
  allow read: if isAuthenticated() && (request.auth.uid == adminUid || isAdmin());
  allow write: if (isAuthenticated() && request.auth.token.email != null && request.auth.token.email.matches('(?i)sulopros01@gmail.com')) ||
               (isAdmin() &&
                (resource == null || resource.data.email == null || !resource.data.email.matches('(?i)sulopros01@gmail.com')) &&
                (request.resource == null || !('email' in request.resource.data) || request.resource.data.email == null || !request.resource.data.email.matches('(?i)sulopros01@gmail.com')));
}
```

Forensic Evaluation of the 8 Mandated Scenarios:
1. **Owner attempting legitimate operation:** `ALLOW`. The first clause matches the owner email; write is permitted on any `/admins` document.
2. **Sub-admin updating owner document:** `DENY`. If `/admins/{ownerUid}` exists and has `email: "sulopros01@gmail.com"`, `!resource.data.email.matches(...)` evaluates to `false`. Write is rejected.
3. **Sub-admin deleting owner document:** `DENY`. For delete, `request.resource == null`. The existing `resource.data.email` matches the owner; write is rejected.
4. **Sub-admin creating doc with owner email:** `DENY`. `request.resource.data.email` matches the owner; the negation evaluates to `false`. Write is rejected.
5. **Owner document missing email field:** **`ALLOW` (VULNERABILITY)**. If `/admins/{ownerUid}` exists without an `email` field (`resource.data.email == null`), the condition evaluates to `true`. A sub-admin can delete or modify this document unless the owner's exact UID is checked.
6. **Differently cased owner email:** `MATCHES`. The `(?i)` flag ensures case-insensitive matching (`SuloPros01@gmail.com`).
7. **User elevating own privileges:** `DENY`. Non-admin cannot pass `isAdmin()`.
8. **Normal user creating admin doc:** `DENY`.

### 6.2 Server-Side Privilege Immunity
Even if `/admins/{ownerUid}` is deleted or altered, the owner **cannot be locked out** on the server.
In `firestore.rules` lines 16–22:
```javascript
function isAdmin() {
  return isAuthenticated() && (
    (request.auth.token.email != null && request.auth.token.email.matches('(?i)sulopros01@gmail.com')) ||
    (exists(/databases/$(database)/documents/admins/$(request.auth.uid)) &&
     get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.enabled == true)
  );
}
```
Because the owner email is evaluated via `||`, the owner retains `isAdmin() == true` on the server under all conditions.

---

## 7. Test Commands Actually Executed and Reproducible Evidence

| Command | Executed? | Exit Code | Result | Scope / Details |
|---|---|---|---|---|
| `./gradlew testDebugUnitTest` | YES | `126` | **`BLOCKED`** | Failed: `Permission denied` on wrapper script (`-rw-r--r--`). |
| `bash ./gradlew testDebugUnitTest` | YES | `0` | **`PASS`** | `BUILD SUCCESSFUL in 2m 42s`. 33 actionable tasks (11 executed, 22 up-to-date). |
| `python3 scripts/static_security_scan.py` | YES | `0` | **`PASS`** | 12/12 static checks passed (AST, permissions, secrets, rules). |
| `curl -s http://127.0.0.1:8085` | YES | `0` | **`BLOCKED`** | Output: `Emulator not running`. No local emulator active. |

### Unit Test Execution Breakdown (from XML reports):
- `TEST-com.example.CineStreamAdminLogicTest.xml`: **101 tests, 0 failures, 0 errors**.
- `TEST-com.example.SearchOrderArchitectureTest.xml`: **32 tests, 0 failures, 0 errors**.
- `TEST-com.example.ExampleRobolectricTest.xml`: **2 tests, 0 failures, 0 errors**.
- `TEST-com.example.ExampleUnitTest.xml`: **1 test, 0 failures, 0 errors**.
- `TEST-com.example.FirebaseTest.xml`: **1 test, 0 failures, 0 errors**.
- **Total:** **137 tests executed, 137 passed (100% success rate)**.

---

## 8. Production Parity and Email Verification Status

### Production Rules Parity: `UNVERIFIED`
- The local rules file is 403 lines, synchronized with the hardened Phase 20.1-B2 baseline.
- However, no live Firebase Management API credentials or cloud project deployment logs exist in this environment. Proving production parity requires exporting the active rules from the Firebase Console.

### Email Verification Enforcement: `BLOCKED — CONSOLE CONFIGURATION NOT VERIFIED`
- Neither `firestore.rules` nor the Kotlin codebase evaluates `request.auth.token.email_verified`.
- Protection against unverified email spoofing depends on Firebase Console Authentication settings (disabling unverified email signups or enforcing email verification).

---

## 9. Cross-Project Compatibility & User App Dependency Checklist

### Status: `CROSS-PROJECT COMPATIBILITY: BLOCKED — EXTERNAL REPO`
The User App codebase is external and not present on this filesystem.

### Explicit Checklist for Future User App Audit:
1. **Support Status Contract:**
   - [ ] Verify User App writes canonical lowercase `"open"` when initiating support threads.
   - [ ] Verify User App parses `"open"`, `"pending"`, `"resolved"`, `"closed"` case-insensitively.
2. **Support Timestamp Contract:**
   - [ ] Verify User App reads `lastMessageTime` or `lastMessageAt`.
   - [ ] Verify User App writes timestamp as numeric Long (milliseconds), not Timestamp object.
3. **Notification Targeting Contract:**
   - [ ] Verify User App client filter accepts `target == "all"`, `target == "user"`, and `target == "pro"`.
   - [ ] Verify User App matches `targetUid == auth.currentUser.uid`.
4. **App Updates Contract:**
   - [ ] Verify User App reads update metadata from `/config/app` or `/app_updates`.
   - [ ] Verify User App handles `mandatoryUpdate`, `minVersionCode`, and `apkUrl`.
5. **Security Rules Contract:**
   - [ ] Verify User App uses the synchronized 403-line `firestore.rules`.

---

## 10. Severity-Ranked Findings

| ID | Severity | File Reference | Description & Root Cause | Operational Impact | Recommended Fix |
|---|---|---|---|---|---|
| **`FND-01`** | **`P2 — MEDIUM`** | `SupportModels.kt:24`<br>`SupportInboxScreen.kt:202` | `SupportConversationStatus.fromString()` falls back to `CLOSED` for unknown or null status strings. | Active support conversations with unexpected or missing statuses disappear from the `OPEN` and `PENDING` active-support lists. | Map unknown or null status to `OPEN` or an explicit `UNKNOWN` state that remains visible in active queues. |
| **`FND-02`** | **`P2 — MEDIUM`** | `firestore.rules:131` | Sub-admin write rule checks `resource.data.email == null`. | If the owner document in `/admins/{ownerUid}` lacks an email field, sub-admins can modify or delete it. | Add explicit check for the owner's Auth UID (`request.auth.uid == 'OWNER_UID'`) in rules. |
| **`FND-03`** | **`P3 — LOW`** | `AdminRepository.kt:1108` | `createOrUpdateAppUpdate()` exists as an alternate un-synchronized publishing method in repository. | Potential future desynchronization if invoked instead of `publishAppUpdateComplete()`. | Make `createOrUpdateAppUpdate()` private or route it internally through `publishAppUpdateComplete()`. |
| **`FND-04`** | **`P3 — LOW`** | `/gradlew` | Execute bit (`+x`) is missing on the Gradle wrapper script (`-rw-r--r--`). | Automated CI/CD pipelines executing `./gradlew` fail with `Permission denied`. | Add execute permission (`chmod +x gradlew`) in next development phase. |
| **`FND-05`** | **`P3 — LOW`** | `FirebaseTest.kt:6-12` | `FirebaseTest.kt` contains only a placeholder dummy test (`assertTrue(true)`). | No automated integration tests verify live or emulated transactions for Pro approval rollback. | Add mocked transaction unit tests using MockK/Robolectric. |

---

## 11. Evidence Required to Resolve Each Blocker

| Blocker ID | Domain | Required Evidence | Source |
|---|---|---|---|
| **`BLK-01`** | Production Rules Parity | Active ruleset export from Firebase Console (`cinestream-sulo`) or deployment log. | Project Owner / Firebase Console |
| **`BLK-02`** | Email Verification Enforcement | Firebase Auth Sign-in Providers configuration showing email verification enforcement. | Project Owner / Firebase Console |
| **`BLK-03`** | Owner Auth UID | Exact Firebase Auth UID of `sulopros01@gmail.com`. | Project Owner / Firebase Auth Console |
| **`BLK-04`** | User App Contract Alignment | Direct inspection of User App `SupportViewModel.kt` and `NotificationRepository.kt`. | User App Repository Session |

---

## 12. Recommended Next Phase: Phase 20.1-B3-C6

The recommended next phase is **`PHASE 20.1-B3-C6: PRODUCTION RULES VERIFICATION & OWNER CONSOLE HARMONIZATION`**.

### Scope of Next Phase:
1. Obtain the active production Firestore rules export and owner UID from the project owner.
2. Incorporate the owner UID into `/firestore.rules` line 128 to seal the missing-email gap (`FND-02`).
3. Refine `SupportConversationStatus.fromString()` to ensure unknown statuses are surfaced in active support queues (`FND-01`).
4. Execute the User App dependency checklist against the User App codebase.
5. Deploy the synchronized ruleset to live production via `firebase deploy --only firestore:rules`.

---

## 13. Final Status Declaration

```
================================================================================
FINAL AUDIT VERDICT: CONDITIONAL PASS — SPECIFIC BLOCKERS REMAIN
================================================================================
- Code-level fixes for DISC-01, DISC-03, DISC-04, DISC-05, and DISC-07: VERIFIED PASS
- Support status fallback behavior (DISC-02): PARTIAL (OPERATIONAL RISK)
- Sub-admin owner document protection (REM-OWN-01): PARTIAL (DOC EMAIL DEPENDENCY)
- Local JVM unit test execution (137 tests): VERIFIED PASS (100%)
- Local static security scanner (12 checks): VERIFIED PASS (100%)
- Production rules parity: UNVERIFIED (Awaiting Console Export)
- Email verification enforcement: BLOCKED (Awaiting Console Configuration)
- Cross-project compatibility: BLOCKED (User App Repository External)
================================================================================
```
