# PHASE 22.21-B — CINESTREAM ADMIN DASHBOARD FIRESTORE DATA CONTRACT
## Standalone Canonical Firestore Data Contract, Paths, Field Schemas, and Authorization Conditions

**Project Identifier:** CineStream Admin Dashboard  
**Phase Identifier:** `PHASE 22.21-B`  
**Firebase Target Project (Local Evidence):** `cinestream-sulo` (Project Number: `979447256418`)  
**Rules Configuration Target:** `firestore.rules` (SHA-256: `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`)  
**Audit Timestamp:** October 2026  
**Operational Mode:** READ-ONLY CONTRACT SPECIFICATION  

---

## 1. Introduction and Scope

This contract document formalizes every Firestore path, document pattern, subcollection, field dictionary, expected data type, requiredness constraint, client-side mutability rule, and server-side authorization rule implemented in the **CineStream Admin Dashboard** codebase.

All assertions within this document are traced directly to concrete code lines in:
- `app/src/main/java/com/example/contract/FirebaseContract.kt`
- `app/src/main/java/com/example/repository/AdminRepository.kt`
- `app/src/main/java/com/example/repository/ManagedExtensionRepository.kt`
- `app/src/main/java/com/example/repository/SearchOrderRepository.kt`
- `app/src/main/java/com/example/repository/ProRequestRepository.kt`
- `app/src/main/java/com/example/repository/SupportRepository.kt`
- `app/src/main/java/com/example/models/*`
- `app/src/main/java/com/example/network/ResilientSyncManager.kt`
- `firestore.rules` (810 lines)

---

## 2. Global Path Pattern Inventory

| Collection / Path Pattern | Classification | Dynamic ID Sourcing | Client Operations in Admin | Rules Authorization Condition |
|---|---|---|---|---|
| `/admins/{adminUid}` | Canonical Sensitive | Target Auth UID | `get`, `set(merge)`, `delete` | Read: `isAdmin()` or `auth.uid == adminUid`<br>Write: `isOwnerAccount()` |
| `/public_profiles/{userId}` | Canonical Public Projection | User Auth UID | None (User App Plane) | Read: `resource.data.isProfilePublic == true`<br>Write: `auth.uid == userId` / Admin delete |
| `/users/{userId}` | Canonical Primary User | User Auth UID | `list` (listener), `get` (listener), `set(merge)`, `update`, `delete` | Read: `isAdmin()` or `isOwner(userId)`<br>Create: `isAdmin()` or `hasSafeUserCreationDefaults()`<br>Update: `isAdmin()` or `(isOwner(userId) && !modifyingSensitiveUserFields())`<br>Delete: `isAdmin()` |
| `/users/{userId}/point_transactions/{txId}` | Canonical Subcollection Ledger | `tx_{timestamp}_{uuid}` | `list` (listener), `set` (in transaction) | Read: `isAdmin()` or `isOwner(userId)`<br>Write: `isAdmin()` |
| `/users/{userId}/task_claims/{taskId}` | Canonical Subcollection | Task ID | None (User App Plane) | Read: `isAdmin()` or `isOwner(userId)`<br>Write: `isAdmin()` |
| `/users/{userId}/library/{itemId}` | Subcollection (User Content) | Media / Item ID | None (User App Plane) | Read/Write: `isAdmin()` or `isOwner(userId)` |
| `/users/{userId}/history/{itemId}` | Subcollection (Watch History) | Media / Item ID | None (User App Plane) | Read/Write: `isAdmin()` or `isOwner(userId)` |
| `/users/{userId}/watched_episodes/{episodeId}` | Subcollection (Episodes) | Episode ID | None (User App Plane) | Read/Write: `isAdmin()` or `isOwner(userId)` |
| `/users/{userId}/settings/notifications` | Subcollection (Settings) | Fixed `"notifications"` | None (User App Plane) | Read: `isAdmin()` or `isOwner(userId)`<br>Write: `isAdmin()` or `(isOwner(userId) && !keys.hasAny([...]))` |
| `/users/{userId}/settings/{document=**}` | Subcollection (Wildcard) | Settings Document ID | None (User App Plane) | Read/Write: `isAdmin()` or `isOwner(userId)` |
| `/users/{userId}/fcmTokens/{installationId}` | Subcollection (Device Tokens) | Installation UUID | None (User App Plane) | Read: `isAdmin()` or `isOwner(userId)`<br>Create/Update: Schema validated<br>Delete: `isAdmin()` or `isOwner(userId)` |
| `/config/app` | Canonical Singleton Config | Fixed `"app"` | `get` (listener), `get` (server), `set(merge)`, `batch.set` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/config/search_order` | Canonical Singleton Config | Fixed `"search_order"` | `get` (listener), `get` (server), `set(merge)` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/config/features` | Canonical Singleton Config | Fixed `"features"` | `get` (listener), `set(merge)` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/config/economy` | Canonical Singleton Config | Fixed `"economy"` | `get` (listener), `set(merge)` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/config/{document=**}` (other) | Deprecated / Disallowed | Wildcard (e.g. `global`) | None (Aliased to `/config/app`) | Read/Write: `false` (Deny all) |
| `/managed_extensions/{extensionId}` | Canonical Scraper Catalog | Scraper / Extension ID | `list` (listener), `get`, `set(merge)`, `delete` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/managed_extensions/{extensionId}/{subcollection}/{docId}` | Subcollections | Subcollection Doc ID | None | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/extensions/{extensionId}` | Legacy Scraper Catalog | Extension ID | `list` (listener), `get`, `set(merge)`, `delete` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/extension_updates/{updateId}` | Legacy Extension Updates | Extension ID | `delete` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/app_updates/{updateId}` | Canonical OTA Releases | `versionCode` or Doc ID | `list` (listener), `set(merge)`, `batch.set`, `delete` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/notifications/{notificationId}` | Canonical Broadcast / FCM | Generated Doc ID | `list` (listener), `get` (server), `set(merge)`, `delete` | Read: `isAdmin()` or Audience Filter (`target == all` / `targetUid == auth.uid` / `target == pro`)<br>Write: `isAdmin()` |
| `/reward_tasks/{taskId}` | Canonical Rewards Catalog | Task ID (`task_{ms}`) | `list` (listener), `set(merge)`, `delete` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/leaderboard/weekly_current` | Canonical Singleton Stats | Fixed `"weekly_current"` | `get` (listener) [Read-Only] | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/leaderboard_history/{cycleId}` | Historical Stats Archive | Cycle ID (e.g. `2026-W42`) | None | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `/pro_requests/{requestId}` | Canonical Upgrade Queue | Request Doc ID | `list` (listener), `get` (server), `transaction` (Approve/Reject), `delete` | Read: `isAdmin()` or `userId == auth.uid`<br>Create: `isAdmin()` or Safe pending submission<br>Update: `isAdmin()` or User cancel pending<br>Delete: `isAdmin()` |
| `/reports/{reportId}` | Canonical Bug / Issue Reports | Report Doc ID | `list` (listener), `get` (server), `set(merge)`, `delete` | Read: `isAdmin()` or `userId == auth.uid`<br>Create: `isAdmin()` or Safe pending submission<br>Update/Delete: `isAdmin()` |
| `/support_conversations/{conversationId}` | Canonical Support Threads | Conversation Doc ID | `list` (listener), `get` (server), `set(merge)`, `batch.set`, `delete` | Read: `isAdmin()` or `userId == auth.uid`<br>Create: `isAdmin()` or `userId == auth.uid`<br>Update: `isAdmin()` or Metadata touch<br>Delete: `isAdmin()` |
| `/support_conversations/{conversationId}/messages/{messageId}` | Canonical Support Messages | Message Doc ID | `list` (listener), `batch.set`, `delete` | Read: `isAdmin()` or Thread Participant<br>Create: `isAdmin()` or User sender<br>Update/Delete: `isAdmin()` |
| `/conversations/{conversationId}` | Social Messaging (User Plane) | Conversation Doc ID | None | Read/Write: Participants / `isAdmin()` |
| `/conversations/{conversationId}/messages/{messageId}` | Social Messages (User Plane) | Message Doc ID | None | Read/Write: Participants / `isAdmin()` |
| `/conversations/{conversationId}/readReceipts/{uid}` | Social Read Receipts | User Auth UID | None | Read/Write: Participant Owner / `isAdmin()` |
| `/stories/{storyId}` | Social Clips / Stories | Story Doc ID | None | Read: `isAuthenticated()`<br>Create: Author with permission checks<br>Update/Delete: `isAdmin()` or Author |
| `/auditLogs/{logId}` | Canonical Administrative Audit | Generated Doc ID | `list` (listener), `get` (server), `set(merge)` | Read/Write: `isAdmin()` |
| `/audit_logs/{document=**}` | Deprecated Path | Wildcard | None | Read/Write: `false` (Deny all) |
| `/comments/{document=**}` | Cancelled / Deferred Feature | Wildcard | None | Read/Write: `false` (Default Deny) |
| `/{document=**}` | Catch-All Final Deny | Wildcard | None | Read/Write: `false` (Catch-All Deny) |

---

## 3. Detailed Entity Field Dictionaries

### 3.1 Document Path: `/admins/{adminUid}`
- **Source Code Entity:** `AdminUser` (`Models.kt` line 263)
- **Primary Repository:** `AdminRepository.kt` lines 46, 235, 255, 989, 991, 1043
- **Document ID:** Firebase Auth UID of the administrator

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Validation |
|---|---|---|---|---|---|
| `uid` | string | Required | Immutable after creation | Valid Firebase Auth UID | Implicit |
| `email` | string | Required | Owner-managed | Valid email address string | Implicit |
| `role` | string | Required | Owner-managed | `"superadmin"`, `"admin"` | Implicit |
| `enabled` | boolean | Required | Owner-managed | `true` (authorized), `false` (revoked) | Checked by `isAdmin()` helper (`enabled == true`) |
| `createdAt` | integer (ms) | Optional | Immutable | Epoch timestamp in milliseconds | Implicit |

---

### 3.2 Document Path: `/users/{userId}`
- **Source Code Entity:** `User` (`Models.kt` lines 104-172)
- **Primary Repository:** `AdminRepository.kt` lines 45, 64-192, 322-671, 937-1053
- **Document ID:** Firebase Auth UID of the user

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| **Identity & Metadata** | | | | | |
| `uid` / `id` | string | Required | Immutable | Matching document ID | Protected by `modifyingSensitiveUserFields()` |
| `username` | string | Optional | Mutable | Max length string | Client-writable by owner |
| `email` | string | Optional | Owner-controlled | Email address | Client-writable by owner |
| `displayName` | string | Optional | Mutable | Profile display name | Client-writable by owner |
| `photoUrl` | string | Optional | Mutable | URL string | Client-writable by owner |
| `appVersion` | string | Optional | Mutable | Client app version | Client-writable by owner |
| `createdAt` | integer / timestamp | Required | Immutable | Epoch milliseconds | Protected by `modifyingSensitiveUserFields()` |
| `updatedAt` | integer / timestamp | Required | Mutable | Updated timestamp | Client-writable by owner |
| `lastLoginAt` / `lastLoginTimestamp` | integer / timestamp | Optional | Mutable | Login timestamp | Client-writable by owner |
| `lastActiveAt` | integer / timestamp | Optional | Mutable | Heartbeat timestamp | Client-writable by owner |
| **Account Authority & State** | | | | | |
| `role` | string | Required | Admin-only | `"user"`, `"admin"` (informational in user doc) | Protected by `modifyingSensitiveUserFields()` |
| `admin` | boolean | Optional | Admin-only | `false` | Protected by `modifyingSensitiveUserFields()` |
| `isAdmin` | boolean | Optional | Admin-only | `false` | Protected by `modifyingSensitiveUserFields()` |
| `isActive` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `isBanned` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `banReason` | string | Optional | Admin-only | Reason string or `null` | Protected by `modifyingSensitiveUserFields()` |
| `banExpiresAt` | integer / timestamp | Optional | Admin-only | Expiry epoch ms or `null` (permanent) | Protected by `modifyingSensitiveUserFields()` |
| **Subscription (Phase 03A Canonical)** | | | | | |
| `subscriptionTier` | string | Required | Admin-only | `"FREE"`, `"PRO_LITE"`, `"PRO"` | Protected by `modifyingSensitiveUserFields()` |
| `planId` | string | Required | Admin-only | `"free"`, `"pro_lite_1d"`, `"pro_lite_7d"`, `"pro_lite_10d"`, `"pro_30d"` | Protected by `modifyingSensitiveUserFields()` |
| `durationDays` | integer | Optional | Admin-only | `1`, `7`, `10`, `30`, or `null` | Protected by `modifyingSensitiveUserFields()` |
| `subscriptionStatus` | string | Required | Admin-only | `"ACTIVE"`, `"EXPIRED"`, `"CANCELED"`, `"PENDING"` | Protected by `modifyingSensitiveUserFields()` |
| `subscriptionSource` | string | Required | Admin-only | `"MONEY"`, `"POINTS"`, `"ADMIN_GRANT"`, `"LEGACY"` | Protected by `modifyingSensitiveUserFields()` |
| `subscriptionReferenceId` | string | Optional | Admin-only | Request ID, Tx ID, or `null` | Protected by `modifyingSensitiveUserFields()` |
| `subscriptionStartedAt` | integer / timestamp | Optional | Admin-only | Epoch milliseconds | Protected by `modifyingSensitiveUserFields()` |
| `subscriptionExpiresAt` | integer / timestamp | Optional | Admin-only | Expiry epoch ms or `null` | Protected by `modifyingSensitiveUserFields()` |
| **Subscription (Legacy Mirrors)** | | | | | |
| `isPremium` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `isPro` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `plan` | string | Optional | Admin-only | `"free"`, `"pro_lite"`, `"pro"` | Protected by `modifyingSensitiveUserFields()` |
| `proPlan` | string | Optional | Admin-only | `"free"`, `"pro_lite"`, `"pro"` | Protected by `modifyingSensitiveUserFields()` |
| `proExpiresAt` | integer / timestamp | Optional | Admin-only | Expiry epoch ms or `null` | Protected by `modifyingSensitiveUserFields()` |
| **Points Economy (Summary)** | | | | | |
| `pointsBalance` | integer | Required | Admin-only | `0` to `1,000,000` | Protected by `modifyingSensitiveUserFields()` |
| `totalPointsEarned` | integer | Optional | Admin-only | `>= 0` | Protected by `modifyingSensitiveUserFields()` |
| `totalPointsSpent` | integer | Optional | Admin-only | `>= 0` | Protected by `modifyingSensitiveUserFields()` |
| `pointsEarnedTotal` | integer | Optional | Admin-only | Alias for `totalPointsEarned` | Protected by `modifyingSensitiveUserFields()` |
| `pointsSpentTotal` | integer | Optional | Admin-only | Alias for `totalPointsSpent` | Protected by `modifyingSensitiveUserFields()` |
| **Feature Permissions (Positive Flags)** | | | | | |
| `canWatch` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `canDownload` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `canChat` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `canStory` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `canP2P` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `canComment` | boolean | Preserved | Deferred (No write) | Default `true` (schema compat) | Protected by `modifyingSensitiveUserFields()` |
| `canUpload` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| `canRequest` | boolean | Required | Admin-only | `true`, `false` | Protected by `modifyingSensitiveUserFields()` |
| **Feature Permissions (Ban Flags)** | | | | | |
| `watchBan` | boolean | Optional | Admin-only | Inverted mirror of `canWatch` | Protected by `modifyingSensitiveUserFields()` |
| `downloadBan` | boolean | Optional | Admin-only | Inverted mirror of `canDownload` | Protected by `modifyingSensitiveUserFields()` |
| `chatBan` | boolean | Optional | Admin-only | Inverted mirror of `canChat` | Protected by `modifyingSensitiveUserFields()` |
| `storyBan` | boolean | Optional | Admin-only | Inverted mirror of `canStory` | Protected by `modifyingSensitiveUserFields()` |
| `p2pBan` | boolean | Optional | Admin-only | Inverted mirror of `canP2P` | Protected by `modifyingSensitiveUserFields()` |
| **Technical Overrides** | | | | | |
| `deviceLimit` / `maxDevices` | integer | Optional | Admin-only | Standard `2` (max default) | Protected by `modifyingSensitiveUserFields()` |
| `allowedQuality` | string | Optional | Admin-only | Quality string or `null` | Protected by `modifyingSensitiveUserFields()` |
| `downloadLimit` | integer | Optional | Admin-only | Numeric limit or `null` | Protected by `modifyingSensitiveUserFields()` |
| `offlineDaysOverride` | integer | Optional | Admin-only | Numeric days or `null` | Protected by `modifyingSensitiveUserFields()` |
| `forcedAdsOverride` | integer | Optional | Admin-only | Numeric count or `null` | Protected by `modifyingSensitiveUserFields()` |

---

### 3.3 Subcollection Path: `/users/{userId}/point_transactions/{txId}`
- **Source Code Entity:** `PointTransaction` (`EconomyModels.kt` lines 43-57)
- **Primary Repository:** `AdminRepository.kt` lines 580-671, 676-707
- **Document ID:** Formatted as `tx_{timestamp}_{8-char-uuid}`

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `txId` | string | Required | Immutable | `tx_{timestamp}_{uuid}` | Write allowed ONLY if `isAdmin()` |
| `userId` | string | Required | Immutable | Target user UID | Write allowed ONLY if `isAdmin()` |
| `type` | string | Required | Immutable | `DAILY_LOGIN`, `REWARDED_AD`, `TASK_REWARD`, `GAME_REWARD`, `LEADERBOARD_REWARD`, `SUBSCRIPTION_REDEMPTION`, `ADMIN_GRANT`, `ADMIN_ADJUSTMENT`, `REVERSAL` | Write allowed ONLY if `isAdmin()` |
| `amount` | integer | Required | Immutable | Positive (credit) or negative (debit) | Write allowed ONLY if `isAdmin()` |
| `balanceBefore` | integer | Required | Immutable | `>= 0` | Write allowed ONLY if `isAdmin()` |
| `balanceAfter` | integer | Required | Immutable | `>= 0` and `<= 1,000,000` | Write allowed ONLY if `isAdmin()` |
| `referenceId` | string | Optional | Immutable | Associated request ID, task ID, or `null` | Write allowed ONLY if `isAdmin()` |
| `description` | string | Required | Immutable | Explanation string | Write allowed ONLY if `isAdmin()` |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Write allowed ONLY if `isAdmin()` |
| `actorUid` | string | Required | Immutable | UID of admin or system executing write | Write allowed ONLY if `isAdmin()` |

---

### 3.4 Singleton Path: `/config/app`
- **Source Code Entity:** `AppConfig` (`Models.kt` lines 272-295)
- **Primary Repository:** `AdminRepository.kt` lines 47, 1057-1104, 1192, 1348
- **Document ID:** Fixed singleton `"app"`

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `maintenanceEnabled` | boolean | Required | Admin-only | `true`, `false` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `maintenanceTitle` | string | Required | Admin-only | Maintenance banner title | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `maintenanceMessage` | string | Required | Admin-only | Maintenance explanation message | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `minimumVersionCode` | integer | Required | Admin-only | `>= 1` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `latestVersionCode` | integer | Required | Admin-only | `>= 1` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `latestVersionName` | string | Required | Admin-only | Semantic version string (e.g. `"1.2.0"`) | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `apkUrl` | string | Required | Admin-only | HTTP/HTTPS URL | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `apkSha256` | string | Required | Admin-only | 64-char hex string | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `mandatoryUpdate` | boolean | Required | Admin-only | `true`, `false` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `releaseNotes` | string | Optional | Admin-only | Markdown or text release notes | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `providersJson` | string | Required | Admin-only | Valid JSON string structure | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `defaultOfflineDays` | integer | Required | Admin-only | `>= 0` (standard `2`) | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `defaultForcedAds` | integer | Required | Admin-only | `>= 0` (standard `5`) | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `cloudinaryCloudName` | string | Required | Admin-only | Cloudinary account name | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `cloudinaryUploadPreset` | string | Required | Admin-only | Unsigned upload preset | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Admin-only | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.5 Singleton Path: `/config/search_order`
- **Source Code Entity:** `SearchOrderConfig` (`SearchOrderConfig.kt` lines 100-245)
- **Primary Repository:** `SearchOrderRepository.kt` lines 49, 54-129
- **Document ID:** Fixed singleton `"search_order"`

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `movie` | array of string | Required | Admin-only | Extension IDs eligible for MOVIE | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `tv` | array of string | Required | Admin-only | Extension IDs eligible for TV/SERIES | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `series` | array of string | Required | Admin-only | Compatibility alias for `tv` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `anime` | array of string | Required | Admin-only | Extension IDs eligible for ANIME | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `arabicDubbed` | array of string | Required | Admin-only | Extension IDs eligible for ARABIC_DUB | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `arabicDub` / `arabic_dub` | array of string | Required | Admin-only | Backward-compatibility aliases | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `englishDubbed` | array of string | Required | Admin-only | Extension IDs eligible for ENGLISH_DUB | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `englishDub` / `english_dub` | array of string | Required | Admin-only | Backward-compatibility aliases | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `dubbingConfig` | map | Required | Admin-only | Map containing: | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `dubbingConfig.arabicDubbedStatus` | string | Required | Admin-only | `"ACTIVE"`, `"MAINTENANCE"`, `"DISABLED"` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `dubbingConfig.englishDubbedStatus` | string | Required | Admin-only | `"ACTIVE"`, `"MAINTENANCE"`, `"DISABLED"` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Admin-only | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedBy` | string | Optional | Admin-only | Administrator email address | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.6 Singleton Path: `/config/features`
- **Source Code Entity:** `FeatureControlConfig` (`EconomyModels.kt` lines 138-148)
- **Primary Repository:** `AdminRepository.kt` lines 712-772
- **Document ID:** Fixed singleton `"features"`

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `subscriptions` | map | Required | Admin-only | `{ status, disabledMessage, comingSoonMessage }` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `points` | map | Required | Admin-only | `{ status, disabledMessage, comingSoonMessage }` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `dailyLogin` | map | Required | Admin-only | `{ status, disabledMessage, comingSoonMessage }` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rewardedAds` | map | Required | Admin-only | `{ status, disabledMessage, comingSoonMessage }` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `tasks` | map | Required | Admin-only | `{ status, disabledMessage, comingSoonMessage }` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `leaderboard` | map | Required | Admin-only | `{ status, disabledMessage, comingSoonMessage }` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `status` (in each feature) | string | Required | Admin-only | `"ACTIVE"`, `"COMING_SOON"`, `"DISABLED"` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Admin-only | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedBy` | string | Optional | Admin-only | Admin UID or email | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.7 Singleton Path: `/config/economy`
- **Source Code Entity:** `EconomyConfig` (`EconomyModels.kt` lines 154-209)
- **Primary Repository:** `AdminRepository.kt` lines 777-825
- **Document ID:** Fixed singleton `"economy"`

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `redemptionCosts` | map of string to integer | Required | Admin-only | Keys: `pro_lite_1d`, `pro_lite_7d`, `pro_lite_10d`, `pro_30d`. Values: strictly `> 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `dailyLoginRewards` | array of integer | Required | Admin-only | Exactly 7 numeric values `>= 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rewardedAdPoints` | integer | Required | Admin-only | Points per ad `>= 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rewardedAdDailyCap` | integer | Required | Admin-only | Daily cap `>= 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rewardedAdCooldownSeconds` | integer | Required | Admin-only | Cooldown in seconds `>= 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Admin-only | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.8 Collection Path: `/managed_extensions/{extensionId}`
- **Source Code Entity:** `ManagedExtension` (`ManagedExtension.kt` lines 105-140)
- **Primary Repository:** `ManagedExtensionRepository.kt` lines 147, 343-390
- **Document ID:** Extension / Scraper ID (e.g. `"qfilm"`, `"witanime"`)

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `extensionId` / `id` | string | Required | Immutable | Alphanumeric slug identifier | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `scraperKey` | string | Required | Immutable | Identifies internal Kotlin scraper | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `name` | string | Required | Admin-only | Scraper title in Arabic / English | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `description` | string | Optional | Admin-only | Scraper description | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `baseUrl` | string | Required | Admin-only | Valid domain URL (e.g. `https://qfilm.vip`) | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `searchUrl` | string | Required | Admin-only | URL pattern with `%s` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `iconUrl` | string | Optional | Admin-only | Icon image URL | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `runtimeApiVersion` | integer | Required | Admin-only | `>= 1` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `definitionVersion` | integer | Required | Admin-only | `>= 1` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `minAppVersionCode` | integer | Required | Admin-only | `>= 1` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `status` | string | Required | Admin-only | `"ACTIVE"`, `"MAINTENANCE"`, `"DISABLED"`, `"DEPRECATED"` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `priority` | integer | Required | Admin-only | Sorting weight (e.g. `100`) | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `language` | string | Required | Admin-only | Language code (default `"ar"`) | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `capabilities` | array of string | Required | Admin-only | `SEARCH`, `DETAILS`, `EPISODES`, `SERVER_DISCOVERY`, `VIDEO_EXTRACTION`, etc. | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `contentTypes` | array of string | Required | Admin-only | `MOVIE`, `SERIES`, `ANIME`, `ASIAN_DRAMA`, `ARABIC_DUB`, `ENGLISH_DUB` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `audioCapabilities` | array of string | Required | Admin-only | `ARABIC_DUB`, `ENGLISH_DUB`, `ORIGINAL_AUDIO`, `MULTI_AUDIO` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `enabled` | boolean | Required | Admin-only | `true` if active and enabled | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Admin-only | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.9 Collection Path: `/app_updates/{updateId}`
- **Source Code Entity:** `AppUpdate` (`Models.kt` lines 321-334)
- **Primary Repository:** `AdminRepository.kt` lines 52, 1112-1215
- **Document ID:** Version code string or generated doc ID

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `id` | string | Required | Immutable | Matching doc ID | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `versionCode` | integer | Required | Admin-only | Integer build number `> 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `versionName` | string | Required | Admin-only | Semantic version string | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `minVersionCode` | integer | Required | Admin-only | Minimum supported version `> 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `apkUrl` | string | Required | Admin-only | HTTP/HTTPS URL | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `apkSha256` | string | Required | Admin-only | 64-char hex SHA-256 | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `mandatoryUpdate` | boolean | Required | Admin-only | `true`, `false` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `releaseNotes` | string | Optional | Admin-only | Release notes text | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `status` | string | Required | Admin-only | `"PUBLISHED"`, `"DRAFT"`, `"ARCHIVED"` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `publishedBy` | string | Required | Admin-only | Admin email or UID | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Admin-only | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.10 Collection Path: `/notifications/{notificationId}`
- **Source Code Entity:** `NotificationRequest` (`Models.kt` lines 337-368)
- **Primary Repository:** `AdminRepository.kt` lines 48, 1219-1306
- **Document ID:** Generated document ID

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `id` | string | Required | Immutable | Document ID | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `title` | string | Required | Admin-only | Notification title | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `body` / `message` | string | Required | Admin-only | Notification body text | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `type` | string | Required | Admin-only | `"SYSTEM"`, `"UPDATE"`, `"PROMO"` | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `target` | string | Required | Admin-only | `"all"`, `"user"`, `"pro"` | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `targetType` | string | Required | Admin-only | `"ALL"`, `"UID"`, `"PRO"` | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `targetUid` | string | Optional | Admin-only | User UID if `target == user` | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `createdBy` | string | Required | Admin-only | Admin UID | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `expiresAt` | integer (ms) | Optional | Admin-only | Expiry epoch milliseconds or `null` | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `isActive` | boolean | Required | Admin-only | `true`, `false` | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |
| `status` | string | Required | Admin-only | `"PENDING"`, `"SENT"` | Read: Audience match or `isAdmin()`<br>Write: `isAdmin()` |

---

### 3.11 Collection Path: `/reward_tasks/{taskId}`
- **Source Code Entity:** `RewardTask` (`EconomyModels.kt` lines 64-98)
- **Primary Repository:** `AdminRepository.kt` lines 830-891
- **Document ID:** Task slug or `task_{ms}`

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `taskId` | string | Required | Immutable | 2-64 chars alphanumeric/hyphens | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `title` | string | Required | Admin-only | Task title | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `description` | string | Optional | Admin-only | Task description | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rewardPoints` | integer | Required | Admin-only | Points `>= 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `taskType` | string | Required | Admin-only | `CUSTOM`, `WATCH_VIDEO`, `FOLLOW_SOCIAL`, `SURVEY`, `SHARE_APP` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `actionUrl` | string | Optional | Admin-only | URL string or `null` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `isActive` | boolean | Required | Admin-only | `true`, `false` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `expiresAt` | integer (ms) | Optional | Admin-only | Expiry epoch ms or `null` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Admin-only | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.12 Document Path: `/leaderboard/weekly_current`
- **Source Code Entity:** `WeeklyLeaderboard` (`EconomyModels.kt` lines 241-245)
- **Primary Repository:** `AdminRepository.kt` lines 897-931 [Read-Only]
- **Document ID:** Fixed singleton `"weekly_current"`

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `cycleId` | string | Required | Trusted Backend | Format e.g. `"WEEKLY_CURRENT"` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `updatedAt` | integer (ms) | Required | Trusted Backend | Epoch milliseconds | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rankings` | array of maps | Required | Trusted Backend | Array of entry objects | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rankings[i].rank` | integer | Required | Trusted Backend | `1, 2, 3, ...` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rankings[i].userId` | string | Required | Trusted Backend | User Auth UID | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rankings[i].displayName` | string | Required | Trusted Backend | Display name string | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rankings[i].points` | integer | Required | Trusted Backend | Weekly points `>= 0` | Read: `isAuthenticated()`<br>Write: `isAdmin()` |
| `rankings[i].photoUrl` | string | Optional | Trusted Backend | Avatar URL string | Read: `isAuthenticated()`<br>Write: `isAdmin()` |

---

### 3.13 Collection Path: `/pro_requests/{requestId}`
- **Source Code Entity:** `ProRequest` (`ProRequest.kt` lines 38-66)
- **Primary Repository:** `ProRequestRepository.kt` lines 38-290
- **Document ID:** Generated document ID

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `requestId` | string | Required | Immutable | Document ID | Read: `isAdmin()` or `userId == auth.uid`<br>Create: `status in [PENDING, pending]`<br>Update: Admin or User cancel<br>Delete: `isAdmin()` |
| `userId` | string | Required | Immutable | Requesting user Auth UID | Same |
| `userEmail` | string | Required | Mutable | User email address | Same |
| `userName` | string | Optional | Mutable | User display name | Same |
| `requestedPlan` | string | Optional | Mutable | `"pro"`, `"pro_lite"` | Protected from user update |
| `planId` | string | Optional | Mutable | SKU string (e.g. `"pro_30d"`) | Protected from user update |
| `requestedDuration` | string | Optional | Mutable | Duration label | Protected from user update |
| `durationDays` | integer | Optional | Mutable | Numeric duration | Protected from user update |
| `paymentMethod` | string | Optional | Mutable | Payment gateway/method | Protected from user update |
| `paymentProofUrl` | string | Optional | Mutable | Proof screenshot URL | Protected from user update |
| `paymentReference` | string | Optional | Mutable | Transaction reference | Protected from user update |
| `userNote` / `notes` | string | Optional | Mutable | User submission note | Protected from user update |
| `status` | string | Required | Transition-controlled | `"PENDING"`, `"APPROVED"`, `"REJECTED"`, `"CANCELLED"` | Admin can change; User can ONLY transition `PENDING` -> `CANCELLED` |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Protected from user update |
| `updatedAt` | integer (ms) | Required | Mutable | Epoch milliseconds | Mutable on state transitions |
| `cancelledAt` | integer (ms) | Optional | User-cancellable | Epoch milliseconds when cancelled | User can set only on cancel |
| `reviewedAt` | integer (ms) | Optional | Admin-only | Epoch milliseconds when reviewed | Protected from user writes |
| `reviewedBy` | string | Optional | Admin-only | Admin UID | Protected from user writes |
| `reviewedByEmail` | string | Optional | Admin-only | Admin email address | Protected from user writes |
| `rejectionReason` | string | Optional | Admin-only | Rejection justification | Protected from user writes |
| `adminNote` | string | Optional | Admin-only | Internal administrator notes | Protected from user writes |

---

### 3.14 Collection Path: `/reports/{reportId}`
- **Source Code Entity:** `UserReport` (`Models.kt` lines 384-399)
- **Primary Repository:** `AdminRepository.kt` lines 51, 1507-1584
- **Document ID:** Generated document ID

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `id` | string | Required | Immutable | Document ID | Read: `isAdmin()` or `userId == auth.uid`<br>Create: `status in [PENDING, pending]`<br>Update/Delete: `isAdmin()` |
| `userId` | string | Required | Immutable | Submitting user Auth UID | Same |
| `userEmail` | string | Required | Mutable | User email | Same |
| `type` | string | Required | Mutable | `"ISSUE"`, `"PLAYBACK"`, etc. | Same |
| `targetType` | string | Optional | Mutable | Target type | Same |
| `targetId` | string | Optional | Mutable | Target ID | Same |
| `title` | string | Required | Mutable | Report title | Same |
| `description` | string | Required | Mutable | Issue description | Same |
| `status` | string | Required | Admin-only | `"PENDING"`, `"RESOLVED"`, `"DISMISSED"` | Admin can update; User cannot update |
| `resolutionNotes` | string | Optional | Admin-only | Resolution notes | Admin-only |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | User cannot modify after creation |
| `resolvedAt` | integer (ms) | Optional | Admin-only | Resolution epoch ms | Admin-only |
| `resolvedBy` | string | Optional | Admin-only | Admin UID | Admin-only |

---

### 3.15 Path: `/support_conversations/{conversationId}` & Subcollection `messages/{messageId}`
- **Source Code Entities:** `SupportConversation` & `SupportMessage` (`SupportModels.kt`)
- **Primary Repository:** `SupportRepository.kt` lines 44-270
- **Document ID:** Thread ID and Message ID

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| **Conversation Document** | | | | | |
| `conversationId` | string | Required | Immutable | Document ID | Read: `isAdmin()` or `userId == auth.uid`<br>Write: `isAdmin()` or User metadata touch |
| `userId` | string | Required | Immutable | User Auth UID | Same |
| `userEmail` | string | Required | Mutable | User email | Same |
| `userName` | string | Optional | Mutable | User display name | Same |
| `subject` | string | Optional | Mutable | Thread subject | Same |
| `status` | string | Required | Admin-managed | `"open"`, `"pending"`, `"resolved"`, `"closed"` | User creation allowed only with `"open"` / `"pending"`; status updates restricted to Admin |
| `lastMessage` | string | Optional | Mutable | Last message snippet | User allowed to update with message |
| `lastMessageAt` / `lastMessageTime` | integer (ms) | Optional | Mutable | Timestamp of last message | User allowed to update with message |
| `lastSenderRole` | string | Optional | Mutable | `"user"`, `"admin"` | User cannot forge `"admin"` |
| `unreadByAdmin` | boolean | Optional | Mutable | `true`, `false` | Mutable |
| `unreadByUser` | boolean | Optional | Mutable | `true`, `false` | Mutable |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Immutable |
| `updatedAt` | integer (ms) | Required | Mutable | Epoch milliseconds | Mutable |
| **Messages Subcollection** | | | | | |
| `messageId` | string | Required | Immutable | Message Document ID | Read: Participant or `isAdmin()`<br>Create: Participant or `isAdmin()`<br>Update/Delete: `isAdmin()` |
| `conversationId` | string | Required | Immutable | Parent conversation ID | Same |
| `senderId` | string | Required | Immutable | Sender Auth UID | Must equal `request.auth.uid` for non-admins |
| `senderRole` | string | Required | Immutable | `"user"` or `"admin"` | Non-admins MUST have `senderRole == 'user'` |
| `senderEmail` | string | Optional | Immutable | Sender email | Same |
| `text` | string | Required | Immutable | Message content string | Same |
| `mediaUrl` | string | Optional | Immutable | Attached media URL | Same |
| `timestamp` | integer (ms) | Required | Immutable | Epoch milliseconds | Same |
| `read` | boolean | Optional | Mutable | Read status flag | Same |

---

### 3.16 Collection Path: `/auditLogs/{logId}`
- **Source Code Entity:** `AuditLog` (`Models.kt` lines 371-382)
- **Primary Repository:** `AdminRepository.kt` lines 49, 287-306, 1357-1382
- **Document ID:** Generated document ID

| Field Name | Expected Type | Requiredness | Mutability | Valid Values / Constraints | Firestore Rules Protection |
|---|---|---|---|---|---|
| `id` | string | Required | Immutable | Document ID | Read/Write allowed ONLY if `isAdmin()` |
| `actorUid` | string | Required | Immutable | Administrator Auth UID | Read/Write allowed ONLY if `isAdmin()` |
| `adminUid` | string | Required | Immutable | Mirror of `actorUid` | Read/Write allowed ONLY if `isAdmin()` |
| `adminEmail` | string | Required | Immutable | Administrator email address | Read/Write allowed ONLY if `isAdmin()` |
| `action` | string | Required | Immutable | Action verb (e.g. `BAN_USER`, `GRANT_SUBSCRIPTION`, etc.) | Read/Write allowed ONLY if `isAdmin()` |
| `targetType` | string | Required | Immutable | `USER`, `CONFIG`, `EXTENSION`, `APP_UPDATE`, `NOTIFICATION`, `REPORT`, `SUPPORT`, `PRO_REQUEST`, `TASK`, `AUTH` | Read/Write allowed ONLY if `isAdmin()` |
| `targetId` | string | Required | Immutable | Target entity ID | Read/Write allowed ONLY if `isAdmin()` |
| `details` | string | Required | Immutable | Human-readable action log | Read/Write allowed ONLY if `isAdmin()` |
| `createdAt` | integer (ms) | Required | Immutable | Epoch milliseconds | Read/Write allowed ONLY if `isAdmin()` |

---

## 4. Summary of Data Contract Conformance

1. **Every active operation in Admin Dashboard matches an explicit allow rule in `firestore.rules`.**
2. **All deprecated paths are either aliased in code (`/config/global` -> `/config/app`) or explicitly denied by rules (`/audit_logs/{document=**}`, `/config/{document=**}`).**
3. **The Comments feature is completely disabled in both code and rules:**
   - No `/comments` path in rules (caught by default deny).
   - `canComment` updates filtered out in `AdminRepository.kt` and `ViewModels.kt`.
   - No UI controls present in `UserDetailScreen.kt`.
4. **Subscription state remains strictly decoupled from playback quality and DRM:**
   - Pro subscriptions only control ad removal (`isAdFree`).
   - Video quality and download permissions remain independent feature flags.
