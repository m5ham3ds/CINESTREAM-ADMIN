# AI_AGENT_START_HERE_AR.md — فهرس التنفيذ الإلزامي

> **هذا الجذر هو مشروع CineStream Admin Dashboard وحده.** لا يوجد مشروع User App داخل هذه الحزمة. لا تطلب من المستخدم فتح مشروعين في الجلسة ذاتها.

## الهدف النهائي

إصلاح توافق كود لوحة الإدارة مع Firebase Authentication وCloud Firestore والقواعد المرشحة، مع الحفاظ على سلطات المالك وعدم التوسع في صلاحيات المشرفين. التعديل يكون في ملفات المصدر الفعلية للمشروع المفتوح. لا تنشر أي شيء إلى Firebase الإنتاجي ولا تنشئ backend بديلًا.

## ترتيب القراءة — لا تتخطّ أي بند

1. `AI_AGENT_START_HERE_AR.md` (هذا الملف).
2. `PHASES/PHASE_20.1-B3-C7_FIREBASE_REMEDIATION_EXECUTION_MASTER.md` — اقرأ النسخة الخاصة بلوحة الإدارة كاملة، بما في ذلك دفعات `A1–A6` وشروط القبول.
3. `docs/ADR-CINESTREAM-001_ACTIVE_BACKEND_FIREBASE.md` — القرار المعماري الملزم: Firebase هو الخلفية النشطة، وأي Cloudflare Workers مؤجل.
4. `docs/FIREBASE_CONTRACT.md`.
5. `docs/FIREBASE_CONTRACT_V1.md`.
6. `firestore.rules` — اقرأ الملف كاملًا. هذه نسخة مرشحة محلية وليست موافقة نشر.
7. `PHASE_20.1-B3-B_ADMIN_DASHBOARD_FIRESTORE_COMPATIBILITY_AUDIT_REPORT.md`.
8. `PHASE_20.1-B3-C2_COMPREHENSIVE_ADMIN_DASHBOARD_FIREBASE_COMPATIBILITY_AUDIT.md`.
9. `PHASE_20.1-B3-C3_CROSS_PROJECT_FIREBASE_CONTRACT_RECONCILIATION.md`.
10. `PHASE_20.1-B3-C4_REMEDIATION_READINESS_PRODUCTION_RULES_VERIFICATION.md`.
11. `PHASE_20.1-B3-C5_FIREBASE_REMEDIATION_IMPLEMENTATION_VERIFICATION.md`.
12. `PHASE_20.1-B3-C5.1_INDEPENDENT_POST_IMPLEMENTATION_AUDIT.md`.
13. `PHASE_20.1-B3-C6_FIREBASE_SECURITY_HARDENING_AND_EVIDENCE_REPORT.md`.
14. `docs/reports/PHASE_05A_ADMIN_AUTHORITY_HARDENING_REPORT.md` — مراجعة سلطة الإدارة والمالك.
15. `docs/reports/PHASE_05B_FIRESTORE_RULES_FINALIZATION_REPORT.md` — تاريخ تقوية القواعد.
16. `docs/reports/PHASE_04B_ADMIN_ECONOMY_MANAGEMENT_CONSOLE_REPORT.md` — وظائف الاقتصاد في لوحة الإدارة.
17. `docs/reports/PHASE_SUBSCRIPTION_POINTS_01_FORENSIC_AUDIT.md`.
18. `docs/reports/PHASE_SUBSCRIPTION_POINTS_02A_ADMIN_CANONICAL_CONTRACT.md`.
19. `docs/reports/PHASE_SUBSCRIPTION_POINTS_03A1_ADMIN_RULES_HARDENING_REPORT.md`.
20. `docs/reports/PHASE_SUBSCRIPTION_POINTS_03C_ADMIN_AUDIT_REPORT.md`.
21. `docs/reports/PHASE_FIRESTORE_ADMIN_ALIGNMENT_REPORT.md`.
22. `docs/reports/PHASE_FIRESTORE_LEGACY_PATH_DEPENDENCY_AUDIT.md`.
23. `docs/reports/PHASE_C8.0_ADMIN_PRODUCTION_LEGACY_DATA_INVENTORY_AUDIT.md`.

**مسارات التقارير:** البنود 7–13 توجد في جذر المشروع؛ البنود 14–23 توجد داخل `docs/reports/`. الحزمة تحتوي على هذه المراجع المحددة فقط لتقليل الالتباس. إذا غاب ملف في المسار المكتوب، ابحث بالاسم الكامل في المشروع مرة واحدة، وسجّل مساره الفعلي. لا تخترع تقريرًا غير موجود. اقرأ التقارير التاريخية للتوجيه فقط، واحسم القرار من كود المصدر الحالي وعقد Firestore والقواعد الموجودة الآن. التقارير السابقة قرائن فقط؛ احسم كل مشكلة من المصدر الحالي والقواعد الحالية.

## ملفات المصدر الأساسية المطلوب فتحها فعليًا

افتح هذه الملفات كاملة، ثم تتبع النماذج وViewModels والشاشات التي تستدعيها:

- `app/src/main/java/com/example/repository/AdminRepository.kt`
- `app/src/main/java/com/example/repository/SupportRepository.kt`
- `app/src/main/java/com/example/repository/ProRequestRepository.kt`
- `app/src/main/java/com/example/contract/FirebaseContract.kt`
- `app/src/main/java/com/example/diagnostics/FirebaseInitializer.kt`
- `app/src/main/java/com/example/viewmodels/LoginViewModel.kt`
- `app/src/main/java/com/example/viewmodels/SupportViewModel.kt`
- `app/src/main/java/com/example/viewmodels/ProRequestsViewModel.kt`
- `app/src/main/java/com/example/viewmodels/EconomyViewModel.kt`
- `app/src/main/java/com/example/models/SupportModels.kt`
- `app/src/main/java/com/example/models/ProRequest.kt`
- `app/src/main/java/com/example/models/EconomyModels.kt`
- `app/src/main/java/com/example/ui/screens/NotificationScreen.kt`
- `app/src/main/java/com/example/ui/screens/SupportInboxScreen.kt`
- `app/src/main/java/com/example/ui/screens/SupportChatScreen.kt`
- `app/src/main/java/com/example/ui/screens/ProRequestsScreen.kt`
- `app/src/main/java/com/example/ui/screens/EconomyConsoleScreen.kt`
- `app/src/main/java/com/example/ui/screens/UserDetailScreen.kt`
- جميع المستدعين/النماذج الذين يكشف بحث المصدر ارتباطهم بالوظائف المستهدفة.

## جدول مواضع الفحص والدفعات

### A1 — مصادقة المسؤول والمالك
ابحث عن: `checkIsAdmin`, `isAdmin`, `isOwner`, `owner`, `FirebaseAuth.currentUser`, `LoginViewModel`, `FirebaseInitializer`, `FirebaseContract`، وأي bootstrapping للمسؤول.
- وجود مستخدم Firebase Auth وحده لا يكفي. تحقق من مسار التحقق الفعلي ورسائل الفشل وحماية الشاشات والـ repositories.
- لا تخمن UID المالك أو البريد ولا تغيّر آلية ملكيته. لا تمنح Admin عاديًا القدرة على إنشاء أو ترقية Admin آخر. لا تغيّر حسابات أو وثائق حية.

### A2 — الإشعارات وسجل التدقيق
افتح `AdminRepository.kt` ودوال `sendNotification`, `getRecentNotifications`, `logAudit` وأي عمليات write ثم audit.
- القيم القياسية لـ`target` هي `all`, `user`, `pro` بحروف صغيرة عندما يتفق المصدر والقواعد. لمستهدف `user` يجب أن يكون `targetUid` غير فارغ ومرتبطًا بطلب متحقق.
- لا تجعل `targetType` بديلًا عن الحقل الأمني `target`. سجّل التوافق القديم كحاجز ترحيل إذا لزم، ولا تجرِ ترحيلًا حيًا.
- يجب انتظار تأكيد الكتابة قبل تسجيل audit success. إذا نجح أثر وفشل audit، سجّل نجاحًا جزئيًا صريحًا لا نجاحًا كاملًا.

### A3 — دعم العملاء
افتح `SupportRepository.kt`, `SupportViewModel.kt`, `SupportModels.kt`, `SupportInboxScreen.kt`, `SupportChatScreen.kt`.
- المسار الصحيح هو `/support_conversations/{conversationId}` والرسائل `/support_conversations/{conversationId}/messages/{messageId}`. لا تستخدم `/conversations` الاجتماعية بدلًا منه.
- افحص `sendAdminReply`, `updateStatus`, `markAsReadByAdmin`, `deleteConversation`. لا تسجّل نجاحًا قبل نجاح Firestore ولا تبتلع أخطاء حذف الرسائل.
- وحّد الحالة المخزنة حسب العقد (مثل `open`, `resolved`, `closed` بصيغة صغيرة إن كان ذلك ما يقرره العقد الفعلي). لا تغيّر كل الحالات دون تتبع كل القارئين.

### A4 — طلبات Pro
افتح `ProRequestRepository.kt`, `ProRequestsViewModel.kt`, `ProRequestsScreen.kt`, `ProRequest.kt`.
- المسار `/pro_requests/{requestId}`. قراءة مالك الطلب تعتمد `resource.data.userId == request.auth.uid` وقواعد admin الموثقة.
- راجع `approveRequest`, `rejectRequest`, `deleteRequest`, `getAllProRequests`; افحص transaction وتحديث `/users/{uid}` وتحديث وثيقة الطلب كعملية واحدة حيث يدعم الكود ذلك.
- لا تعُد إلى شرط غير معرّف مثل `conversationId` داخل قاعدة طلبات Pro. لا تمنح المستخدم موافقة ذاتية أو تعديلات حقول إدارية.

### A5 — الاقتصاد والنقاط وسجل الحركات
افتح `AdminRepository.kt` و`EconomyViewModel.kt`, `EconomyModels.kt`, `EconomyConsoleScreen.kt`, `UserDetailScreen.kt`. ابحث عن `adjustUserPoints`, `getUserPointTransactions`, `point_transactions`, `task_claims`, `pointsBalance`, `leaderboard`, `runTransaction`, `WriteBatch`.
- راجع تزامن تحديث الرصيد والـ ledger، وعدم وجود تعديل صامت أو سجل نجاح قبل اكتمال الكتابة.
- لا تضعف قواعد الاقتصاد لتسهيل الواجهة. إذا كان دفتر الحركات يتعارض مع قواعد تسمح بالتعديل، وثّق التعارض؛ لا تتخذ قرار صلاحيات مالية دائمًا من تلقاء نفسك.

### A6 — مسح شامل لعمليات Firestore في لوحة الإدارة
ابحث في كامل المشروع عن `.collection(`, `.document(`, `.get()`, `.set(`, `.update(`, `.delete(`, `runTransaction`, `WriteBatch`, `addSnapshotListener`, `logAudit(`.
- تحقق على الأقل من `/admins`, `/users`, `/users/{uid}/point_transactions`, `/notifications`, `/pro_requests`, `/support_conversations` و`/messages`, `/auditLogs`, `/config/app`, `/app_updates`, `/extensions` و`managed_extensions` بحسب المسارات الفعلية.
- لكل موضع دوّن الملف والدالة والمسار والعملية والصلاحية المتوقعة وهل ينتظر نجاح العملية وهل audit بعد النجاح. لا تكتفِ بالمجموعة المذكورة؛ راجع كامل نتائج البحث.
- افحص fallback القديمة مثل `/config/global` إذا ظهرت في المصدر، ولا تحذف توافقًا قديمًا قبل تتبع قراءاته ومكتباته.

## ترتيب التنفيذ وحدود كل دفعة

1. `S0`: جرد هوية المشروع والشجرة؛ اقرأ ملفات المراجع؛ سجّل baseline في `REPAIR_PROGRESS_AR.md`. لا تغيّر الكود قبلها.
2. `A1`: مصادقة وصلاحيات مسؤول/مالك.
3. `A2`: إشعارات + audit.
4. `A3`: رسائل الدعم + الحالة + الحذف.
5. `A4`: طلبات Pro ومعاملات الموافقة/الرفض.
6. `A5`: الاقتصاد والنقاط وسجل الحركة.
7. `A6`: sweep لكل استدعاءات Firestore.
8. `S6`: static review فقط، دون تشغيل الاختبارات النهائية.
9. `S7`: تقرير التنفيذ، الحواجز، SHA-256 للقواعد، وقائمة الاختبارات المؤجلة.

نفّذ دفعة واحدة أو مجموعة صغيرة آمنة في كل دورة. إذا تعذر إصلاح وظيفة لأنها تحتاج API موثوقًا أو قرار مالك، أبق الأمان كما هو، وثّق الحاجز، وتابع بقية المهام المستقلة. لا تترك تعديلًا نصف مكتمل دون تسجيل نقطة استئناف.

## سجل الاستئناف

أنشئ/حدّث `REPAIR_PROGRESS_AR.md` في الجذر بعد كل دفعة. سجّل المرحلة، الملفات المقروءة، الملفات المعدلة، المشكلة ودليلها من المصدر، التغيير الفعلي، نتيجة الفحص الثابت الذي تم تشغيله بالفعل، الحواجز، والمرحلة التالية. عند استلام `استمر` اقرأه أولًا، ولا تكرر التعديل المكتمل.

## ممنوع حتى موافقة المستخدم النهائية

- لا `firebase deploy` ولا تغييرات مباشرة على قاعدة الإنتاج ولا عمليات حذف/backfill/ترحيل حي.
- لا Gradle build ولا Firebase Emulator الآن؛ الاختبارات مؤجلة للنهاية.
- لا تغيّر UID المالك أو دوره أو طريقة تحديده، ولا تسمح لمسؤول عادي بترقية حساب آخر.
- لا توسّع القواعد كي تتوافق مع كتابة غير آمنة؛ لا تدمج Cloudflare Workers ولا تنشئ Backend/Cloud Functions جديدة.
- لا تدّعِ نجاح build/tests لم يتم تشغيلها.

## تعريف اكتمال الحزمة

لا يعتبر العمل مكتملاً بمجرد التقرير. يجب أن يكون لكل `A1–A6` وضع موثق وأن تكون ملفات المصدر قد عُدلت فعليًا أو ثبت عدم احتياجها للتعديل. اكتب `PHASE_20.1-B3-C7_EXECUTION_REPORT_AR.md` بالعربية مع جدول ملف/مشكلة/تغيير/دليل، والحواجز، والفحوص الفعلية، والاختبارات المؤجلة.
