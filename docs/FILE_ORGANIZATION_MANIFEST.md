# File Organization Manifest / سجل تنظيم الملفات

Project: CineStream Admin Dashboard
Action: file organization only. No tests, Gradle tasks, emulator, or Firebase deployment were run.

## Root files kept in place because runtime/build/deploy configuration depends on them
- `firebase.json`, `.firebaserc`, `firestore.rules`, Gradle wrapper/configuration, and the Android `app/` tree.
- `app/google-services.json` remains the Android Google Services configuration.
- Android unit/instrumentation tests remain in the Gradle-standard `app/src/test` and `app/src/androidTest` locations.

## Moved items
- `CINESTREAM_ADMIN firestore.rules.txt` → `docs/firebase/rules-history/snapshots/CINESTREAM_ADMIN firestore.rules.txt`
- `CINESTREAM_FIREBASE_IDENTITY_LOCK.json` → `config/firebase/CINESTREAM_FIREBASE_IDENTITY_LOCK.json`
- `DOCUMENTATION_CLEANUP_REPORT.md` → `docs/archive/maintenance-reports/DOCUMENTATION_CLEANUP_REPORT.md`
- `FIREBASE_LINKAGE_AUDIT.md` → `docs/firebase/FIREBASE_LINKAGE_AUDIT.md`
- `PHASE_22.14_ADMIN_CONTRACT_RECONCILIATION_REPORT.md` → `docs/archive/phase-reports/PHASE_22.14_ADMIN_CONTRACT_RECONCILIATION_REPORT.md`
- `PHASE_22.15-B_ADMIN_FIREBASE_COMPLETION_REPORT.md` → `docs/archive/phase-reports/PHASE_22.15-B_ADMIN_FIREBASE_COMPLETION_REPORT.md`
- `PHASE_22.15-B_ADMIN_FIREBASE_IDENTITY_AND_SIGNING_REPORT.md` → `docs/archive/phase-reports/PHASE_22.15-B_ADMIN_FIREBASE_IDENTITY_AND_SIGNING_REPORT.md`
- `PHASE_22.21-B_ADMIN_FIRESTORE_CONTRACT.md` → `docs/firebase/contracts/PHASE_22.21-B_ADMIN_FIRESTORE_CONTRACT.md`
- `PHASE_22.21-B_ADMIN_FIRESTORE_RULES_AUDIT_REPORT.md` → `docs/firebase/audits/PHASE_22.21-B_ADMIN_FIRESTORE_RULES_AUDIT_REPORT.md`
- `PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules` → `docs/firebase/rules-history/candidates/PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules`
- `PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules.txt` → `docs/firebase/rules-history/candidates/PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules.txt`
- `PHASE_22.22-B_ADMIN_RULES_FINGERPRINT_RESTORE_REPORT.md` → `docs/archive/phase-reports/PHASE_22.22-B_ADMIN_RULES_FINGERPRINT_RESTORE_REPORT.md`
- `PHASE_22.22-B_PRE_RESTORE_LOCAL_RULES_BACKUP.txt` → `docs/firebase/rules-history/backups/PHASE_22.22-B_PRE_RESTORE_LOCAL_RULES_BACKUP.txt`
- `PHASE_22.22-C_ADMIN_RECONCILIATION_AUDIT_REPORT.md` → `docs/firebase/audits/PHASE_22.22-C_ADMIN_RECONCILIATION_AUDIT_REPORT.md`
- `PHASE_22.22-C_ADMIN_RECONCILIATION_CANDIDATE.rules` → `docs/firebase/rules-history/candidates/PHASE_22.22-C_ADMIN_RECONCILIATION_CANDIDATE.rules`
- `PHASE_22.23_CENTRAL_UNIFIED_RULES_CANDIDATE.rules.txt` → `docs/firebase/rules-history/candidates/PHASE_22.23_CENTRAL_UNIFIED_RULES_CANDIDATE.rules.txt`
- `PHASE_22.23_CINESTREAM_REPAIR_AND_TEST_REPORT_AR.md` → `docs/archive/phase-reports/PHASE_22.23_CINESTREAM_REPAIR_AND_TEST_REPORT_AR.md`
- `PHASE_22.24_FIREBASE_IDENTITY_AND_GOOGLE_SIGNIN_FIX_REPORT_AR.md` → `docs/archive/phase-reports/PHASE_22.24_FIREBASE_IDENTITY_AND_GOOGLE_SIGNIN_FIX_REPORT_AR.md`
- `PHASE_22.25_POST_REPORT_FIXES_AR.md` → `docs/archive/phase-reports/PHASE_22.25_POST_REPORT_FIXES_AR.md`
- `PROJECT_BASELINE.md` → `docs/PROJECT_BASELINE.md`
- `Rules.md` → `docs/firebase/rules-history/snapshots/Rules.md`
- `firestore-tests/firebase-identity-integrity.test.js` → `qa/firestore-tests/firebase-identity-integrity.test.js`
- `firestore-tests/firestore-rules.test.js` → `qa/firestore-tests/firestore-rules.test.js`
- `firestore-tests/firestore-static-contract.test.js` → `qa/firestore-tests/firestore-static-contract.test.js`
- `firestore-tests/package-lock.json` → `qa/firestore-tests/package-lock.json`
- `firestore-tests/package.json` → `qa/firestore-tests/package.json`
- `firestore-tests/phase03a1-rules.test.js` → `qa/firestore-tests/phase03a1-rules.test.js`
- `firestore-tests/phase05b-comprehensive-rules.test.js` → `qa/firestore-tests/phase05b-comprehensive-rules.test.js`

## Safety scope
- No Android application Kotlin/Java/XML source was changed for organization.
- QA file paths and links were updated where necessary after relocation; no suites were executed.
- No Firebase rules were deployed and no production database was changed.
- Historical reports, candidate rules, snapshots, and backups were moved, not deleted.
