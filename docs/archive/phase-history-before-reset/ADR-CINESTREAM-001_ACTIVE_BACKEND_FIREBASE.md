# ARCHITECTURE DECISION RECORD (ADR)

# ADR-CINESTREAM-001: Active Backend Architecture Decision — Firebase Remains Active Backend

| Attribute | Details |
|---|---|
| **Decision ID** | `ADR-CINESTREAM-001` |
| **Title** | Active Backend Architecture Decision: Firebase Remains Active Backend |
| **Status** | **ACCEPTED — FIREBASE REMAINS THE ACTIVE BACKEND** |
| **Date** | 2026-10-15 |
| **Phase Reference** | Phase C4.1 (Cross-Project Architecture Governance) |
| **Target Projects** | CineStream User App (`com.example`) ↔ CineStream Admin Dashboard (`com.example` / `com.aistudio.cinestreamadmin.qzxkno`) |
| **Authoritative Engine** | Google Cloud Firestore & Firebase Authentication |
| **Owner Identity** | `sulopros01@gmail.com` |

---

## 1. Context & Problem Statement

A secondary backend architecture based on Cloudflare Workers and TypeScript is under active development. Its planned stages encompass a serverless worker foundation, TMDB media detection and metadata engine, and Firestore event sourcing with idempotency controls.

A critical governance question arose during Phase C4 regarding whether current remediation work across the User App and Admin Dashboard should depend on, integrate with, or transition toward this emerging backend, or whether existing production workflows must remain anchored to the established Firebase stack.

Proceeding with premature integration or treating an incomplete secondary backend as a prerequisite introduces severe stability risks, architectural drift, untested dependencies, and delays in remediating confirmed operational defects in production.

---

## 2. Binding Architecture Decision

**Firebase remains the active, authoritative backend for the existing CineStream User App and Admin Dashboard until the project owner explicitly approves a future migration.**

### Active Production Services:
1. **Firebase Authentication:** Sole identity provider, credential validator, and JWT issuer for all user, subscriber, and administrator accounts.
2. **Cloud Firestore:** Sole operational and persistent datastore for:
   - User account profiles, ban states, and granular feature restrictions (`/users/{uid}`).
   - Subscriptions, entitlement tiers, and Pro upgrade requests (`/pro_requests/{requestId}`).
   - Points balances, economic ledgers, and transaction audits (`/users/{uid}/point_transactions/{txId}`).
   - Real-time customer support conversations and chat threads (`/support_conversations/{uid}`).
   - Broadcast and targeted in-app system notifications (`/notifications/{notificationId}`).
   - Global application settings, scheduled maintenance mode, scraper search order, and feature gating flags (`/config/*`).
   - Administrative and security audit trails (`/auditLogs/{logId}`).
3. **Firestore Security Rules:** Sole server-side access control and authorization engine governing all database reads, writes, and role enforcement.
4. **Cloudinary:** Sole authorized media asset storage and content delivery network for image and APK payloads.

---

## 3. Future Backend Deferred — Non-Interference Mandate

The planned Cloudflare Workers / TypeScript backend is **NOT authorized** to replace, supplement, or intercept existing production workflows at this stage.

### Explicit Operational Invariants:
* **No Endpoint Binding:** Do not connect the User App or Admin Dashboard to new or unapproved backend endpoints (`worker.cinestream...`, custom proxy routes, etc.).
* **No Authority Migration:** Do not migrate authentication, session management, or administrative privilege gates away from Firebase Auth and `/admins/{uid}`.
* **No Workflow Relocation:** Do not move existing business logic (points adjustments, subscription approvals, account restrictions) out of the Firebase Firestore client/rules architecture.
* **No Incomplete Dependencies:** Do not introduce code dependencies, network clients, or runtime assumptions relying on unreleased backend services.
* **No Premature Activation:** Do not deploy or activate any secondary backend components as part of current remediation efforts.
* **No Blocker Creation:** The ongoing development of the secondary backend shall **NOT** be treated as a blocker for remediating the seven confirmed Firebase defects identified in Phase C4 (`DISC-01` through `DISC-07`).

---

## 4. Current Remediation Scope & Alignment

All ongoing and upcoming remediation tasks (Phase C5 onwards) shall be executed strictly within the existing Firebase architecture:

1. **Resolution of Confirmed Defects:**
   - **`DISC-01` (P1):** Fix the Pro approval fallback in `ProRequestRepository.kt` to ensure atomic updates to `/users/{uid}` and `/pro_requests/{id}` without dropping user subscription entitlements.
   - **`DISC-02` (P2):** Standardize support ticket status serialization on canonical lowercase strings (`"open"`, `"resolved"`, `"closed"`).
   - **`DISC-03` (P2):** Implement dual-write timestamping (`lastMessageAt` and `lastMessageTime`) in `SupportRepository.kt`.
   - **`DISC-04` (P2):** Standardize notification target casing on lowercase strings (`"all"`, `"user"`).
   - **`DISC-05` (P2):** Implement composite atomic publishing for app updates across `/config/app` and `/app_updates/{id}`.
   - **`DISC-06` (P2):** Reconcile Firestore security rules across repositories to ensure hardened social and token checks are synchronized.
   - **`DISC-07` (P2):** Eliminate the dead `/config/global` fallback call in `AdminRepository.kt`.
2. **Preservation of Owner Authority:**
   - Every legitimate administrative capability of the project owner (`sulopros01@gmail.com`) is strictly preserved.
   - No owner account, role, permission, or administrative screen shall be removed, disabled, or weakened.
3. **Production Rules Governance:**
   - Never deploy an unverified rules file. The production rules baseline must be verified prior to deploying rules modifications to live cloud infrastructure.

---

## 5. Future Migration Compatibility Criteria

To ensure architectural cleanliness and avoid technical debt while respecting this decision:

* Fixes must be designed with clean repository interfaces and stable data contracts.
* No speculative migration abstractions or unneeded architectural indirection shall be introduced unless directly required by the Firebase solution.
* Any future migration from Firebase to an alternative backend shall be treated as an independent project requiring:
  1. A formal, dedicated project phase.
  2. An explicit, written authorization and architecture sign-off from the project owner.
  3. A comprehensive, published Migration & Cutover Plan.
  4. End-to-end integration and load testing in an isolated staging environment.
  5. An approved, battle-tested rollback strategy.

---

## 6. Execution Restrictions & Compliance Statement

This task is **documentation-only**:
* **Source Code Modified:** NONE (0 files).
* **Configuration / Rules Modified:** NONE (0 files).
* **Database Records Modified:** NONE (0 records).
* **Deployments Executed:** NONE (0 deployments).
* **New Backend Integrations Created:** NONE (0 integrations).

---

## 7. Status Declaration

`ARCHITECTURE DECISION RECORDED — FIREBASE REMAINS ACTIVE — FUTURE BACKEND DEFERRED — NO CODE CHANGES — NO DEPLOYMENT`
