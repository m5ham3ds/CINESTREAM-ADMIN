# PHASE 20.1-B3-C4 — REMEDIATION READINESS & PRODUCTION RULES VERIFICATION REPORT
## CineStream User App ↔ Admin Dashboard ↔ Firebase

**Phase Identifier:** `PHASE 20.1-B3-C4`  
**Execution Context:** CineStream Admin Dashboard Repository (`com.example` / `com.aistudio.cinestreamadmin.qzxkno`)  
**Cross-Project Baseline:** `PHASE_20.1-B3-C3_CROSS_PROJECT_FIREBASE_CONTRACT_RECONCILIATION.md`  
**Execution Mode:** STRICT READ-ONLY AUDIT & VERIFICATION  
**Code Modifications:** ZERO (0) Files Modified / Created / Deleted  
**Production Deployment:** ZERO (0) Deployments Attempted  
**Audit Timestamp:** October 15, 2026 — 09:50 UTC  
**Required Final State:** `REMEDIATION READINESS AUDIT COMPLETE — REPORT ONLY — NO CODE CHANGES — NO DEPLOYMENT`

---

## 1. Executive Summary

This forensic report establishes the remediation readiness of the CineStream system across the Admin Dashboard, User App, and Firebase Firestore layers. Following the contract reconciliation conducted in Phase 20.1-B3-C3, this phase revalidates all seven identified defects (`DISC-01` through `DISC-07`), audits owner security controls, investigates production rules deployment status, and formulates an actionable, conservative remediation roadmap.

### Key Conclusions:
1. **Production Rules Status:** The production deployment status of Firestore rules is **`UNVERIFIED`**. The local repository contains a 376-line rules file (Phase 05B baseline, SHA-256: `c55a8c59...`), while the User App contract reflects a 403-line rules file (Phase 20.1-B2 baseline). Because this container environment lacks Firebase CLI deployment credentials, active service account keys, or access to the live Firebase Management API, no claim is made that the local file matches the live production ruleset.
2. **Owner Authorization & Security:** 
   - Regular users cannot elevate their privileges or create documents in `/admins/{uid}` (`CONFIRMED SECURE`).
   - The project owner (`sulopros01@gmail.com`) possesses server-side administrative immunity: even if `/admins/{ownerUid}` is deleted or disabled by a sub-admin, the owner remains an authoritative admin because their email is checked directly via logical OR (`||`) in `isAdmin()`.
   - Neither `firestore.rules` nor the client code evaluates `request.auth.token.email_verified`.
3. **Revalidation of the Seven Defects:**
   - `DISC-01` (Pro approval fallback dropping user upgrade) is **CONFIRMED P1 DEFECT** in `ProRequestRepository.kt:186-199`.
   - `DISC-02` (Support status casing: `OPEN` vs `open`) is **CONFIRMED P2 DEFECT** in `SupportRepository.kt:208`.
   - `DISC-03` (Support timestamp key: `lastMessageAt` vs `lastMessageTime`) is **CONFIRMED P2 DEFECT** in `SupportRepository.kt:54, 177`.
   - `DISC-04` (Notification target casing: `ALL` vs `all`) is **CONFIRMED P2 DEFECT** in `AdminRepository.kt:1154`.
   - `DISC-05` (Dual update source desynchronization between `/config/app` and `/app_updates`) is **CONFIRMED P2 ARCHITECTURAL RISK**.
   - `DISC-06` (Rules drift between 376-line and 403-line files) is **CONFIRMED P2 DISCREPANCY**.
   - `DISC-07` (Legacy fallback to `/config/global` denied by rules) is **CONFIRMED P2 DEFECT** in `AdminRepository.kt:1041`.
4. **Remediation Readiness:** The project is **`CONDITIONALLY READY`** for remediation. Full execution must await the owner's explicit decisions on status casing and update sources, as well as the owner providing the production rules baseline.

---

## 2. Overall Readiness Determination

### **OVERALL READINESS: `CONDITIONALLY READY`**

| Assessment Category | Status | Evaluation & Justification |
|---|---|---|
| **Defect Localization & Diagnosis** | **READY** | All 7 defects are located down to exact file names, method signatures, and line numbers. Root causes are fully understood. |
| **Owner Authority & Workflow Safety** | **READY** | Zero risk of owner lockout or privilege truncation. All existing admin workflows are preserved. |
| **Schema & Data Compatibility** | **READY** | User bans, feature flags, subscriptions, and points ledgers are structurally compatible across both applications. |
| **Production Environment Verification** | **BLOCKED / UNVERIFIED** | Live production rules cannot be inspected directly without Firebase Console / CLI deployment access. |
| **Owner Architectural Decisions** | **PENDING OWNER REVIEW** | Standardizing support status casing and choosing the authoritative app update source require owner sign-off. |

---

## 3. Production Rules Verification Matrix

A comprehensive inspection of the workspace was conducted to identify Firebase project configurations, rule definitions, and deployment pipelines.

### 3.1 Inspected Configuration Files
* **`firebase.json` (Root Directory):**
  ```json
  {
    "firestore": {
      "rules": "firestore.rules"
    },
    "emulators": {
      "firestore": {
        "port": 8085,
        "host": "127.0.0.1"
      }
    }
  }
  ```
  *Analysis:* Explicitly associates Firestore security rules with the local file `firestore.rules`.
* **`.firebaserc`:** **ABSENT**. No `.firebaserc` file exists in the repository. The target Firebase project alias is not declared locally.
* **`FirebaseInitializer.kt` (Lines 16–22):**
  Contains hardcoded fallback parameters:
  - `FALLBACK_PROJECT_ID`: `"cinestream-sulo"`
  - `FALLBACK_STORAGE_BUCKET`: `"cinestream-sulo.firebasestorage.app"`
  - `FALLBACK_GCM_SENDER_ID`: `"979447256418"`
  - `FALLBACK_APP_ID`: `"1:979447256418:android:ea5b570266ae1aa6a9883b"`
* **GitHub Actions Workflow (`.github/workflows/build.yml`):**
  Only executes `./gradlew assembleDebug`. **Zero deployment steps** (`firebase deploy`) exist in CI/CD.

### 3.3 Production Rules Verification Table

| Item / Resource | Required Result / Target | Actual Evidence Found | Verification Status |
|---|---|---|---|
| **Admin Local Rules** | Path, File Size, SHA-256 | `/firestore.rules`<br>16,349 bytes (376 lines)<br>`c55a8c596cabfbd6ba99e4171fa33a25f57c816918d2c9826c7f7d93d1aba3a9` | **VERIFIED** |
| **User App Local Rules** | Path, File Size, SHA-256 (if available) | Documented in Phase 20.1-B3-C1 report (403 lines, Phase 20.1-B2 baseline). File is in external session. | **BLOCKED / EXTERNAL SESSION** |
| **Rules Path in `firebase.json`** | Declared rules target path | `"firestore.rules"` | **VERIFIED** |
| **Firebase Project ID** | Non-sensitive project identifier | `cinestream-sulo` (from `FirebaseInitializer.kt:19`) | **VERIFIED (FALLBACK ID)** |
| **Active Production Ruleset** | Live cloud rules verified | No Firebase CLI access, management token, or cloud console credentials available in container. | **UNVERIFIED** |
| **Evidence of Last Successful Deploy** | Deployment timestamp / commit reference | Zero deployment logs or release tags exist in repo or CI/CD workflow. | **BLOCKED** |
| **Local vs Production Parity** | Proven match between local and production | Cannot be confirmed without direct read access to Firebase Management API. | **UNVERIFIED** |

> **Statement on Production Parity:**  
> It is **NOT** assumed that `/firestore.rules` (376 lines) or the User App rules (403 lines) represents the active production configuration. Proving production parity requires the project owner to export the active ruleset from the Firebase Console or provide deployment logs.

---

## 4. Owner Security & Authorization Audit

A forensic re-examination of `/admins/{adminUid}` and all authorization helper functions in `firestore.rules` and `AdminRepository.kt` was conducted to verify administrative protection.

### 4.1 Questions & Empirical Evidence

#### 1. Can a standard registered user create an admin document for themselves?
* **Finding:** **NO (CONFIRMED SECURE).**
* **Direct Evidence:** `firestore.rules` line 128:
  ```javascript
  allow write: if isAdmin() || (isAuthenticated() && request.auth.uid == adminUid && request.auth.token.email != null && request.auth.token.email.matches('(?i)sulopros01@gmail.com'));
  ```
  A regular user is not an admin (`isAdmin() == false`) and their email does not match `sulopros01@gmail.com`. The write request is denied with `PERMISSION_DENIED`.

#### 2. Can a sub-admin modify or delete the owner's admin document?
* **Finding:** **YES AT DATABASE LEVEL, BUT NO AT REAL PRIVILEGE LEVEL (CONFIRMED FINDING).**
* **Direct Evidence:**
  - *Database Level:* Line 128 permits any active admin (`allow write: if isAdmin()`) to update or delete any document in `/admins/{adminUid}`. Neither `firestore.rules` nor `AdminRepository.setUserAdminRole()` (lines 947–972) checks whether `adminUid == ownerUid`. A rogue sub-admin could execute `docRef.delete()` on `/admins/{ownerUid}`.
  - *Privilege Immunity Level:* Even if `/admins/{ownerUid}` is deleted, the owner **DOES NOT LOSE ACCESS**. Line 18 of `firestore.rules` states:
    ```javascript
    (request.auth.token.email != null && request.auth.token.email.matches('(?i)sulopros01@gmail.com'))
    ```
    Because this check is linked with logical OR (`||`), the owner remains an authoritative admin on the server. Furthermore, logging into the Admin Dashboard immediately re-bootstraps the document via `AdminRepository.kt:228-246`.

#### 3. Does the owner's privilege depend on email, the admin document, or both?
* **Finding:** **BOTH, WITH INDEPENDENT FALLBACKS.**
* **Direct Evidence:**
  - In `firestore.rules` (lines 16–22): The owner email alone grants `isAdmin() == true` on the server.
  - In `AdminRepository.kt` (lines 228–246): The owner email auto-creates `/admins/{ownerUid}` with `enabled = true` and `role = "superadmin"`.
  - In User App Rules (Phase 20.1-B2 baseline): Test `E02` established that the User App rules require `/admins/{uid}.enabled == true`. The owner's self-bootstrap in the Admin Dashboard satisfies this requirement.

#### 4. Can deleting the owner's document impact their access or UI?
* **Finding:** **TEMPORARY UI NOTICE, ZERO SERVER LOCKOUT.**
* **Direct Evidence:** On the server, Firestore operations continue to succeed via the email rule. In the client UI, `AdminRepository.checkIsAdmin()` re-bootstraps the document on the next app start or refresh.

#### 5. Are Firebase Custom Claims or other claim mechanisms used?
* **Finding:** **NO (CONFIRMED).**
* **Direct Evidence:** AST search for `customClaims` and `CustomClaims` across `app/src/` returned zero matches. All references to "claims" pertain strictly to reward task redemption records (`task_claims`).

#### 6. Is there server-side protection independent of the client UI?
* **Finding:** **YES (CONFIRMED ZERO-TRUST).**
* **Direct Evidence:** Server-side security rules guard all collections. Direct API calls bypassing the Android UI cannot write to `/config/*`, modify `/admins/*`, or update protected user fields without valid authorization tokens.

#### 7. Does any administrative privilege depend on `email_verified`?
* **Finding:** **NO (CONFIRMED UNCHECKED).**
* **Direct Evidence:** AST search across `app/` and `firestore.rules` for `email_verified` returned zero occurrences. `request.auth.token.email_verified` is **not** evaluated.  
  *Risk:* If Firebase Authentication allows unverified email signups, an attacker could register `sulopros01@gmail.com` with an arbitrary password.  
  *Mitigation:* Firebase Authentication project settings must enforce email verification or disable password signup without verification.

---

## 5. Revalidation of Defects DISC-01 through DISC-07

Each defect identified in Phase 20.1-B3-C3 was re-verified against the active codebase:

### DISC-01 — Pro Approval Fallback Drops User Document Upgrade
* **Severity:** **`P1 — HIGH`**
* **Exact Code Location:** `app/src/main/java/com/example/repository/ProRequestRepository.kt`, lines 186–199.
* **Current Behavior:**
  ```kotlin
  } catch (e: Exception) {
      AppLogger.w("ProRequestRepository", "Transaction failed (network weak/offline): ${e.message}. Executing resilient merge.")
      val updates = hashMapOf<String, Any?>(
          "status" to ProRequestStatus.APPROVED.name,
          "reviewedAt" to now,
          "reviewedBy" to adminUid,
          "reviewedByEmail" to adminEmail,
          "updatedAt" to now
      )
      if (!adminNote.isNullOrBlank()) {
          updates["adminNote"] = adminNote.trim()
      }
      docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
  }
  ```
* **Revalidation Questions Answered:**
  - *Can a fallback path change status to `APPROVED` without upgrading the user?* **YES.** Line 189 writes `status = "APPROVED"` directly to `docRef` (`/pro_requests/{requestId}`). The user document update logic (lines 145–173) inside the transaction is completely skipped.
  - *Exact behavior on transaction failure:* The catch block executes `docRef.set(updates, SetOptions.merge())` on the request document only, and then proceeds to line 201 to log an audit record stating the subscription was granted.
  - *Does the request remain pending or record partial success?* **PARTIAL SUCCESS DEFECT.** The request is marked approved in Firestore, but the user account remains on the Free tier. Because its status is no longer `PENDING`, the admin cannot re-approve it without manual database intervention.

### DISC-02 — Support Ticket Status Casing Mismatch
* **Severity:** **`P2 — MEDIUM`**
* **Exact Code Location:** `app/src/main/java/com/example/repository/SupportRepository.kt`, line 208.
* **Current Behavior:** Admin writes `newStatus.name`, which serializes enum values as uppercase: `"OPEN"`, `"PENDING"`, `"RESOLVED"`, `"CLOSED"`.
* **User App Contract:** The User App writes `"open"` (`SupportViewModel.kt:105`) and its Firestore rules enforce `status in ['open', 'pending']` (`firestore.rules:287-290`).
* **Operational Impact:** Admin status changes written in uppercase risk being rejected by User App security rules or failing case-sensitive status filters in the User App.

### DISC-03 — Support Timestamp Field Name Mismatch
* **Severity:** **`P2 — MEDIUM`**
* **Exact Code Location:** `app/src/main/java/com/example/repository/SupportRepository.kt`, lines 54 and 177.
* **Current Behavior:** Admin writes `"lastMessageAt" to now` and sorts by `lastMessageAt`.
* **User App Contract:** User App writes and expects `lastMessageTime` (`SupportViewModel.kt`).
* **Operational Impact:** When an admin replies, `lastMessageAt` is updated, but `lastMessageTime` remains stale or null in the User App, causing conversation list previews to display outdated timestamps.

### DISC-04 — Notification Target Audience Casing Mismatch
* **Severity:** **`P2 — MEDIUM`**
* **Exact Code Location:** `app/src/main/java/com/example/repository/AdminRepository.kt`, lines 1154 and 1161.
* **Current Behavior:**
  ```kotlin
  val targetVal = if (request.target.isNotBlank()) request.target else request.targetType.name
  // Serializes to uppercase: "ALL", "PRO", "UID"
  ```
* **User App Contract:** `NotificationRepository.kt:125` filters against lowercase strings: `target == "all" || target == "user"`.
* **Operational Impact:** Targeted notifications created with `target: "UID"` or `"ALL"` may be filtered out and ignored by the User App client filter.
* **FCM Reality:** The Admin Dashboard writes a Firestore document to `/notifications/{id}`. It **does not** call the Firebase Cloud Messaging API directly. Real device tray alerts require a backend Cloud Function listening to the collection.

### DISC-05 — Dual App Update Source Desynchronization
* **Severity:** **`P2 — MEDIUM`**
* **Exact Code Location:** `AdminRepository.kt:1062` (`/config/app`) and `AdminRepository.kt:1202` (`/app_updates`).
* **Current Behavior:** Managed in completely separate screens (`GlobalConfigScreen` vs `AppUpdatesScreen`) and written via independent non-atomic methods.
* **Operational Impact:** Admin can publish a new update release in `/app_updates` without updating `/config/app.latestVersionCode`. Because the User App cold start queries `/config/app` while the update dialog queries `/app_updates`, contradictory update instructions can be presented to users.

### DISC-06 — Cross-Project Firestore Rules Drift
* **Severity:** **`P2 — MEDIUM`**
* **Exact Code Location:** Local `/firestore.rules` (376 lines) vs User App `/firestore.rules` (403 lines).
* **Current Behavior:** The local 376-line rules file lacks dedicated rule clauses for:
  1. Social chat message ban checks (`canChat != false && chatBan != true`).
  2. Story creation ban checks (`canStory != false && storyBan != true`).
  3. Strict schema validation for `/users/{uid}/fcmTokens/{installationId}`.
  4. Public profile projections `/users/{uid}/public/{docId}`.
* **Operational Impact:** Deploying the 376-line file to production would overwrite and strip the server-side social restrictions hardened during Phase 20.1-B2.

### DISC-07 — Legacy Fallback to `/config/global` Denied by Rules
* **Severity:** **`P2 — LOW / COMPATIBILITY`**
* **Exact Code Location:** `AdminRepository.kt`, line 1041.
* **Current Behavior:**
  ```kotlin
  configCollection.document(FirebaseConfigDocs.GLOBAL).get().addOnSuccessListener { ... }
  ```
* **Rules Behavior:** Line 202 of `firestore.rules` enforces:
  ```javascript
  match /config/{document=**} { allow read, write: if false; }
  ```
* **Operational Impact:** When `/config/app` does not exist, the fallback query to `/config/global` is rejected with `PERMISSION_DENIED`, emitting an empty fallback object and generating warning logs.

---

## 6. Prioritized Remediation Plan (Design-Level Specification)

> **STRICT NOTICE:** The following plan specifies code changes to be implemented in a subsequent phase. **Zero code modifications have been made during this audit.**

```
┌────────────────────────────────────────────────────────────────────────┐
│                   REMEDIATION ROADMAP DEPENDENCY GRAPH                 │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   [REM-CFG-01] Remove /config/global Fallback                          │
│         │                                                              │
│   [REM-PRO-01] Eliminate Request-Only Fallback in Pro Approval (P1)   │
│         │                                                              │
│   [REM-SUP-01] Dual-Write Timestamps & Lowercase Statuses (P2)         │
│         │                                                              │
│   [REM-NOT-01] Lowercase Notification Targeting (P2)                   │
│         │                                                              │
│   [REM-UPD-01] Composite Atomic App Update Publishing (P2)             │
│         │                                                              │
│   [REM-OWN-01] Owner Document Mutation Protection in Rules (P2)        │
│         │                                                              │
│   [REM-RUL-01] Rules Consolidation (Merge 403-line Hardening) (P2)     │
│         │                                                              │
│   [REM-BLD-01] Root .env.example Placeholder for Clean Builds (P3)     │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### Remediation Item Specifications

#### 1. `REM-PRO-01` — Fix Inconsistent Pro Request Approval Catch-Block
* **Priority:** **`P1`**
* **Target File:** `app/src/main/java/com/example/repository/ProRequestRepository.kt` (lines 186–199).
* **Proposed Design:** Remove lines 186–199 that execute `docRef.set(updates, SetOptions.merge())` on the request alone. In the `catch` block, rethrow the exception or return a failure result to the ViewModel. If the transaction fails, the request must remain in `PENDING` status, and an error snackbar must inform the admin that the transaction could not be completed.
* **Dependencies:** None. Self-contained in Admin repository.
* **Testing Required:** Unit test simulating `FirebaseFirestoreException` during `runTransaction`; verify request remains `PENDING` and `/users/{uid}` is not modified.
* **Acceptance Criteria:** A failed approval leaves both documents unchanged; a successful approval updates both atomically.
* **Legacy Compatibility Risk:** Zero risk.

#### 2. `REM-SUP-01` — Standardize Support Ticket Status Casing & Dual-Write Timestamps
* **Priority:** **`P2`**
* **Target Files:** 
  - `app/src/main/java/com/example/models/SupportModels.kt`
  - `app/src/main/java/com/example/repository/SupportRepository.kt` (lines 54, 175–183, 202–214).
* **Proposed Design:**
  1. In `SupportRepository.updateStatus`, write canonical lowercase strings: `"open"`, `"pending"`, `"resolved"`, `"closed"`.
  2. In `SupportRepository.sendAdminReply`, write both timestamps simultaneously:
     ```kotlin
     "lastMessageAt" to now,
     "lastMessageTime" to now
     ```
  3. In `SupportModels.kt`, update `SupportConversationStatus.fromString()` to handle case-insensitively and serialize to lowercase via `@get:PropertyName("status")` or standard Jackson/Firestore property serialization.
* **Dependencies:** Coordinates with User App contract.
* **Testing Required:** Verify User App displays updated timestamp and reflects status change without rule rejection.
* **Acceptance Criteria:** Both `lastMessageAt` and `lastMessageTime` equal `now`; status in Firestore is lowercase.
* **Legacy Compatibility Risk:** Zero risk; dual-writing preserves backward compatibility for older readers.

#### 3. `REM-NOT-01` — Lowercase Notification Targeting
* **Priority:** **`P2`**
* **Target File:** `app/src/main/java/com/example/repository/AdminRepository.kt` (lines 1154–1163).
* **Proposed Design:** Serialize `target` as lowercase: `"all"`, `"user"` (mapping `"UID"` to `"user"`). Maintain `targetType` as a secondary field.
* **Dependencies:** User App `NotificationRepository.kt`.
* **Testing Required:** Dispatch notification; verify User App client filter accepts document.
* **Acceptance Criteria:** Firestore document contains `target: "all"` or `target: "user"`.
* **Legacy Compatibility Risk:** Zero risk.

#### 4. `REM-UPD-01` — Atomic Composite App Update Publishing
* **Priority:** **`P2`**
* **Target Files:**
  - `app/src/main/java/com/example/repository/AdminRepository.kt`
  - `app/src/main/java/com/example/viewmodels/ViewModels.kt` (`AppUpdatesViewModel`).
* **Proposed Design:** Create a unified repository function `publishAppUpdateComplete()` that uses `firestore.batch()` to write the release object to `/app_updates/{updateId}` and update `/config/app` (`latestVersionCode`, `latestVersionName`, `apkUrl`, `mandatoryUpdate`) simultaneously.
* **Dependencies:** Admin Dashboard UI.
* **Testing Required:** Publish an update; verify both `/config/app` and `/app_updates/{id}` reflect identical version metadata.
* **Acceptance Criteria:** Single button tap writes both documents atomically.
* **Legacy Compatibility Risk:** Zero risk.

#### 5. `REM-OWN-01` — Protect Owner Document Against Sub-Admin Mutation
* **Priority:** **`P2`**
* **Target File:** `/firestore.rules` (lines 126–129).
* **Proposed Design:** Hardcode an exclusion rule preventing sub-admins from modifying or deleting `/admins/{ownerUid}`:
  ```javascript
  match /admins/{adminUid} {
    allow read: if isAuthenticated() && (request.auth.uid == adminUid || isAdmin());
    allow write: if (request.auth.token.email != null && request.auth.token.email.matches('(?i)sulopros01@gmail.com')) ||
                 (isAdmin() && !adminUid.matches('(?i)OWNER_UID_PLACEHOLDER'));
  }
  ```
* **Dependencies:** Requires owner UID.
* **Testing Required:** Emulator test verifying sub-admin cannot delete owner document.
* **Acceptance Criteria:** Sub-admin receives `PERMISSION_DENIED` when attempting to delete owner doc.
* **Legacy Compatibility Risk:** Zero risk.

#### 6. `REM-CFG-01` — Remove Dead `/config/global` Fallback Call
* **Priority:** **`P2`**
* **Target File:** `app/src/main/java/com/example/repository/AdminRepository.kt` (lines 1040–1051).
* **Proposed Design:** Remove the fallback read to `FirebaseConfigDocs.GLOBAL`. If `/config/app` does not exist, emit `AppConfig()` directly.
* **Dependencies:** None.
* **Testing Required:** Clear `/config/app`; verify clean logs without `PERMISSION_DENIED` warnings.
* **Acceptance Criteria:** Zero queries emitted for `/config/global`.
* **Legacy Compatibility Risk:** Zero risk.

#### 7. `REM-RUL-01` — Reconcile Workspace Rulesets to 403-Line Hardened Baseline
* **Priority:** **`P2`**
* **Target File:** `/firestore.rules`.
* **Proposed Design:** Update Admin Dashboard rules file to incorporate the 403-line Phase 20.1-B2 hardening baseline, adding social chat ban checks, story ban checks, and FCM token validation while retaining the owner email bootstrap.
* **Dependencies:** Coordination across workspaces.
* **Testing Required:** Execute full 49-test suite on local emulator.
* **Acceptance Criteria:** 49/49 emulator tests pass.
* **Legacy Compatibility Risk:** Zero risk.

#### 8. `REM-BLD-01` — Add Placeholder `.env.example` to Project Root
* **Priority:** **`P3`**
* **Target File:** Root Directory (`/.env.example`).
* **Proposed Design:** Add an empty or template `.env.example` file to allow the Secrets Gradle Plugin to evaluate cleanly during automated builds.
* **Dependencies:** None.
* **Testing Required:** Run `./gradlew testDebugUnitTest`.
* **Acceptance Criteria:** Build succeeds without configuration listener failure.
* **Legacy Compatibility Risk:** Zero risk.

---

## 7. Test Plan & Acceptance Criteria

The following three-tier test suite must be executed during the subsequent implementation phase:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          THREE-TIER TEST PLAN                          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   TIER 1: Local JVM Unit Tests (Mocked Repositories & Logic)           │
│   - Pro approval transaction failure & rollback                        │
│   - Support status lowercase serialization                             │
│   - Dual-timestamp writing                                             │
│   - Notification target casing mapping                                 │
│                                                                        │
│   TIER 2: Firestore Emulator Integration Tests (Port 8085)             │
│   - Sub-admin blocked from modifying owner doc                         │
│   - Regular user blocked from writing to /admins                       │
│   - Owner email retains isAdmin() without doc                          │
│   - Pro request approval updates both docs atomically                  │
│   - Chat-banned user blocked from sending messages                     │
│                                                                        │
│   TIER 3: Live Staging / Smoke Tests (Real Network)                    │
│   - End-to-end support message exchange between apps                   │
│   - In-app update dialog triggered by composite publish                │
│   - Points adjustment reflected in user profile                        │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### Detailed Acceptance Criteria Table

| Test Case Identifier | Test Description & Layer | Target Assertion | Pass / Fail Condition |
|---|---|---|---|
| **TEST-AUTH-01** | Regular user self-promotion (`Emulator`) | Write to `/admins/{uid}` with `enabled: true` | Must return `PERMISSION_DENIED`. |
| **TEST-AUTH-02** | Sub-admin mutating owner doc (`Emulator`) | Sub-admin deleting `/admins/{ownerUid}` | Must return `PERMISSION_DENIED`. |
| **TEST-AUTH-03** | Owner access preservation (`Emulator`) | Authenticated as owner with no `/admins` doc | Query to `/config/app` write must succeed. |
| **TEST-PRO-01** | Pro approval transaction failure (`Unit`) | Simulate network error during `runTransaction` | Request status must remain `PENDING`; user doc unchanged. |
| **TEST-PRO-02** | Pro approval normal path (`Emulator`) | Execute `approveRequest` with valid admin | Both request and user docs updated atomically. |
| **TEST-SUP-01** | Support reply timestamp dual-write (`Unit`) | Admin sends reply in support chat | Output map contains `lastMessageAt == now` and `lastMessageTime == now`. |
| **TEST-SUP-02** | Support status lowercase (`Emulator`) | Admin changes status to resolved | Document in Firestore contains `status: "resolved"`. |
| **TEST-NOT-01** | Notification target casing (`Unit`) | Dispatch global notification | Output map contains `target: "all"`. |
| **TEST-UPD-01** | Composite update publishing (`Unit`) | Call `publishAppUpdateComplete` | Batch contains writes for both `/config/app` and `/app_updates/{id}`. |
| **TEST-CFG-01** | Config fallback removal (`Unit`) | Trigger read when `/config/app` is missing | Zero network calls emitted for `/config/global`. |

---

## 8. Blockers & Evidence Required from the Project Owner

To transition from `CONDITIONALLY READY` to active execution in Phase C5, the following inputs and decisions must be provided by the project owner:

| Blocker ID | Domain | Missing Input / Evidence Required | Impact if Unresolved |
|---|---|---|---|
| **BLK-01** | **Production Rules Verification** | Export or copy of the active ruleset currently running in the Firebase Console for `cinestream-sulo`. | Cannot prove whether production is running the 376-line or 403-line ruleset. |
| **BLK-02** | **Owner UID for Rule Hardening** | The exact Firebase Auth UID of `sulopros01@gmail.com`. | Needed to hardcode owner document protection in `/admins/{adminUid}`. |
| **BLK-03** | **Decision on Status Casing** | Confirmation to standardize on lowercase string literals (`"open"`, `"resolved"`) across both apps. | Required before modifying `SupportRepository.kt`. |
| **BLK-04** | **Decision on App Update Source** | Confirmation to adopt composite dual-publishing for app releases. | Required before refactoring `AppUpdatesViewModel.kt`. |
| **BLK-05** | **Email Verification Enforcement** | Confirmation that the Firebase Console enforces email verification for password accounts. | Protects against unverified email spoofing for the owner address. |

---

## 9. Explicit Confirmation of Constraints

In strict compliance with the Phase C4 directives:

* **Were any source code files modified?**  
  **`NO`** (Zero source files were edited, created, renamed, or deleted).
* **Were any configuration files or Gradle scripts modified?**  
  **`NO`** (Zero configuration files were altered).
* **Were any Firestore rules, Cloud Functions, or cloud resources deployed?**  
  **`NO`** (Zero deployments performed).
* **Were any database records, user accounts, or admin documents changed?**  
  **`NO`** (Zero database mutations executed).
* **Has production rules parity been proven?**  
  **`UNVERIFIED`** (No Firebase Console or CLI management credentials were provided in this session; direct access to live production rules was not possible).

---

## 10. Active Backend Architecture Decision (ADR-CINESTREAM-001)

* **Decision Identifier:** `ADR-CINESTREAM-001`
* **Status:** **`ACCEPTED — FIREBASE REMAINS THE ACTIVE BACKEND`**
* **Standalone Documentation:** `/docs/ADR-CINESTREAM-001_ACTIVE_BACKEND_FIREBASE.md`

### Core Tenets of the Binding Decision:
1. **Firebase Remains Active:** Firebase Authentication, Cloud Firestore, Firestore Security Rules, and Cloudinary remain the sole active, authoritative infrastructure powering CineStream User App and Admin Dashboard.
2. **Future Backend Deferred:** The emerging Cloudflare Workers / TypeScript backend is strictly deferred and is **NOT authorized** to replace, supplement, or intercept existing production workflows at this stage.
3. **No Migration During Remediation:** Do not connect either application to new backend endpoints, do not migrate authorization, and do not treat the future backend as a blocker for fixing current Firebase defects.
4. **Remediation Invariant:** All seven defects (`DISC-01` to `DISC-07`) will be addressed within the current Firebase architecture, preserving all owner privileges (`sulopros01@gmail.com`) and existing operational workflows.
5. **Future Migration Criteria:** Any future migration requires a separate phase, an explicit owner decision, a documented migration plan, compatibility testing, and an approved rollback strategy.

### Final Status Declaration:
`ARCHITECTURE DECISION RECORDED — FIREBASE REMAINS ACTIVE — FUTURE BACKEND DEFERRED — NO CODE CHANGES — NO DEPLOYMENT`

