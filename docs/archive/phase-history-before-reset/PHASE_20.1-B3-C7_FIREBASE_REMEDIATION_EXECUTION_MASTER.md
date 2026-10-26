# PHASE 20.1-B3-C7 — CineStream Admin Dashboard فقط — دليل التنفيذ التنفيذي الرئيسي

**نطاق هذه الجلسة:** مشروع Android/Kotlin واحد هو **CineStream Admin Dashboard**. هذه الوثيقة نسخة مخصصة للوحة الإدارة فقط؛ لا تنفذ مهام تطبيق المستخدمين من هذه الجلسة. يجب تعديل ملفات المشروع المفتوح مباشرةً، لا إنشاء مشروع بديل ولا الاكتفاء بكتابة خطة.

**نقطة البداية الإلزامية:** اقرأ `AI_AGENT_START_HERE_AR.md` في جذر المشروع، ثم اقرأ هذه الوثيقة كاملة. اتبع مسارات الملفات الدقيقة الواردة في الفهرس؛ في نسخة لوحة الإدارة توجد بعض تقارير المراجعة في جذر المشروع أو `docs/reports/` وليس بالضرورة في `PHASES/`. إذا لم يوجد الملف في المسار الدقيق، ابحث بالاسم الكامل داخل المشروع وسجّل المسار الذي عُثر عليه، ولا تخترع مسارًا.

**قاعدة التنفيذ:** نفّذ `S0` ثم دفعات `A1 → A6` بالترتيب والحالة الفعلية. في كل دورة إنجاز نفّذ دفعة محددة واحدة أو أصغر مجموعة آمنة، واحفظ التقدم في `REPAIR_PROGRESS_AR.md` قبل التوقف. عندما يكتب المستخدم `استمر` اقرأ ملف التقدم أولًا وتابع من أول دفعة غير مكتملة.

## 0. تعليمات إلزامية للوكيل — اقرأ هذا الملف كاملًا قبل لمس أي ملف

أنت تعمل داخل مساحة عمل Google AI Studio التي تحتوي على **مشروع CineStream Admin Dashboard واحد فقط**. لا تفترض وجود تطبيق المستخدمين في مساحة العمل هذه. هذه جلسة مستقلة؛ لا تفترض أن لديك ذاكرة أو نتائج محادثة ChatGPT أخرى، ولا تدّعِ أنك قرأت ملفًا ليس موجودًا في مساحة العمل.

1. اقرأ هذا الملف من بدايته إلى نهايته. لا تختصره إلى خطة فقط، ولا تبدأ بالرد قبل فحص الملفات المشار إليها.
2. حدّد نوع المشروع المفتوح من الملفات الفعلية:
   - **User App:** مشروع Android/Kotlin الذي يحتوي على `app/src/main/java/com/example/data/repository/SocialRepository.kt` و`NotificationRepository.kt`، وملف `PHASES/PHASE_20.1-B3-C1_COMPREHENSIVE_USER_APP_FIREBASE_ADMIN_CONTRACT_AUDIT.md`.
   - **Admin Dashboard:** مشروع Android/Kotlin الذي يحتوي على `app/src/main/java/com/example/repository/AdminRepository.kt` و`SupportRepository.kt` و`ProRequestRepository.kt`، و`app/src/main/java/com/example/contract/FirebaseContract.kt`.
   - إذا لم تتطابق الشجرة، لا تختر مسارًا افتراضيًا ولا تنفّذ إصلاحات مشروع آخر؛ وثّق الاختلاف أولًا وواصل فقط في الأجزاء التي تثبت مطابقتها.
3. افتح الملف الحقيقي قبل تعديل أي رمز. تقارير PHASE القديمة قرائن للبحث وليست دليلًا على أن العيب ما زال موجودًا أو أن إصلاحًا قديمًا صحيح. قد تكون بعض التقارير كتبت قبل تغيير العقد أو القواعد.
4. المطلوب **تعديل ملفات المصدر في مساحة العمل مباشرةً**، لا إعطاء snippets فقط، ولا الاكتفاء بتقرير أو diff نصي. إذا كانت نقطة ما مطبقة بالفعل، أثبت ذلك من الكود الحالي ولا تعِد تعديلها لمجرد ظهورها في التقرير.
5. اعمل بدفعات صغيرة متسلسلة. بعد كل دفعة حدّث `REPAIR_PROGRESS_AR.md` في جذر المشروع. إذا اقترب انتهاء السياق، توقف عند نقطة آمنة بعد حفظ هذا الملف، ولا تبدأ دفعة جديدة غير مكتملة.
6. عند استلام كلمة «استمر»، اقرأ `REPAIR_PROGRESS_AR.md` أولًا، ثم افتح الملفات التي لم يكتمل فحصها، وتابع من نقطة الاستئناف دون إعادة العمل المنجز.
7. إذا كان الإصلاح الصحيح يحتاج Backend موثوقًا أو قرارًا من مالك المشروع أو ترحيلًا للبيانات، **لا تخترع بديلًا ولا تخفف القواعد**. حافظ على السلوك الآمن وسجّل حاجزًا واضحًا مع السبب والقرار المطلوب.
8. لا تدّعِ أن Build أو Emulator أو اختبارًا قد نجح إذا لم يتم تنفيذه فعليًا. اختبارات التنفيذ مؤجلة عمدًا إلى المرحلة النهائية بعد مراجعة المشروعين.

## 1. ثوابت المعمارية والأمان

- الخلفية النشطة الحالية هي **Firebase Authentication + Cloud Firestore**، وتخزين الوسائط الحالي **Cloudinary**. أبق هذه البنية كما هي.
- **لا تنقل المشروع إلى Cloudflare Workers**، ولا تربط أو تنشئ Cloud Functions أو API أو Backend أو خدمة وسيطة جديدة من تلقاء نفسك. إذا كان الكود الحالي يستعمل نقطة Backend موجودة بالفعل، افحصها ووثّقها؛ لا تنشئ نقطة جديدة.
- لا تشغّل أوامر مثل `firebase deploy`، ولا تغيّر المشروع النشط عبر `firebase use`، ولا تنفّذ أي كتابة أو حذف في قاعدة بيانات حية، ولا تحدّث أسرارًا أو مفاتيح.
- لا تخمّن UID المالك، ولا تغيّر `isOwnerAccount()` أو آلية تحديد المالك، ولا توسّع صلاحيات المشرفين أو تضف إنشاء/ترقية مسؤولين من الواجهة.
- لا تفتح قراءة `/users/{uid}` للمستخدمين الآخرين لتصحيح البحث، ولا تسمح للمستخدم بتغيير رصيد النقاط أو سجل المعاملات أو مطالبات المكافآت أو عدّادات مستخدم آخر.
- لا تتجاوز خطأ صلاحيات بحذف رسائل أو حسابات أو بيانات؛ لا تبتلع فشل الحذف/الكتابة بصمت ولا تعرض نجاحًا قبل تأكيد الكتابة.
- لا تعدّل UI أو التصميم أو مسارات البحث/التشغيل/المحتوى غير المتعلقة مباشرةً بإصلاح Firebase، ولا تجرِ إعادة هيكلة شاملة.
- احفظ أي تغيير للقواعد في `firestore.rules` المرشح داخل المشروع فقط، ولا تنشره. لا توسع السماح لمجرد أن الكود الحالي يعتمد على كتابة غير مسموحة.

## 2. ترتيب الحقيقة عند وجود تعارض في المصادر

اقرأ وقارن هذه الملفات الموجودة في المشروع الحالي:

1. `firestore.rules` — القواعد المرشحة داخل المشروع المفتوح؛ هذه ليست موافقة على نشرها.
2. `docs/FIREBASE_CONTRACT.md` إذا كان موجودًا، وأي `docs/FIREBASE_CONTRACT_V1.md` موجود.
3. النماذج والـ constants الحقيقية في Kotlin.
4. مواضع القراءة والكتابة الحقيقية في Repositories وViewModels.
5. تقارير الفحص السابقة داخل `PHASES/` أو جذر المشروع.

**التعارض المعروف الذي يجب تسويته وتوثيقه:** كاتب الإشعارات في `AdminRepository.sendNotification()` يحول الجمهور إلى `target = "all" | "user" | "pro"`، وقواعد Firestore المرشحة تستخدم هذه القيم الصغيرة نفسها، بينما قد تعرض بعض وثائق العقد القديمة قيمًا مثل `ALL` أو `UID` أو `PRO`. لا تخلط القيم ولا تغيّر القواعد لتناسب وثيقة قديمة. افحص نماذج البيانات والقواعد، ثم وحّد توثيق الحقول على القيم التي تتفق عليها قواعد هذا الإصلاح والكود التنفيذي؛ سجّل المستندات القديمة التي تحتاج ترحيلًا مستقلًا.

**تمييز مسارات إلزامي:**
- `/conversations/{conversationId}` هو مسار المحادثات الاجتماعية التي يستخدمها `SocialRepository` في User App.
- `/support_conversations/{conversationId}` هو مسار محادثات دعم العملاء الذي يستخدمه Admin Dashboard، وله `/messages/{messageId}`.
- لا تستبدل أحد المسارين بالآخر ولا تعتبرهما Collection واحدة. إيصالات القراءة الاجتماعية في `/conversations/{conversationId}/readReceipts/{uid}` منفصلة عن رسائل الدعم.

## 3. الملفات المرجعية المطلوب قراءتها في جلسة Admin Dashboard

- `docs/FIREBASE_CONTRACT.md`
- `docs/FIREBASE_CONTRACT_V1.md` إذا كان موجودًا
- `firestore.rules`
- `PHASE_20.1-B3-C2_COMPREHENSIVE_ADMIN_DASHBOARD_FIREBASE_COMPATIBILITY_AUDIT.md` إذا كان موجودًا
- `PHASE_20.1-B3-C3_CROSS_PROJECT_FIREBASE_CONTRACT_RECONCILIATION.md` إذا كان موجودًا
- `PHASE_20.1-B3-C4_REMEDIATION_READINESS_PRODUCTION_RULES_VERIFICATION.md` إذا كان موجودًا
- `PHASE_20.1-B3-C5_FIREBASE_REMEDIATION_IMPLEMENTATION_VERIFICATION.md` إذا كان موجودًا
- `PHASE_20.1-B3-C5.1_INDEPENDENT_POST_IMPLEMENTATION_AUDIT.md` إذا كان موجودًا
- `PHASE_20.1-B3-C6_FIREBASE_SECURITY_HARDENING_AND_EVIDENCE_REPORT.md` إذا كان موجودًا
- `docs/reports/PHASE_05A_ADMIN_AUTHORITY_HARDENING_REPORT.md`
- `docs/reports/PHASE_05B_FIRESTORE_RULES_FINALIZATION_REPORT.md`
- `docs/reports/PHASE_04B_ADMIN_ECONOMY_MANAGEMENT_CONSOLE_REPORT.md`
- `docs/reports/PHASE_SUBSCRIPTION_POINTS_01_FORENSIC_AUDIT.md`
- `docs/reports/PHASE_SUBSCRIPTION_POINTS_02A_ADMIN_CANONICAL_CONTRACT.md`
- `docs/reports/PHASE_SUBSCRIPTION_POINTS_03A1_ADMIN_RULES_HARDENING_REPORT.md`
- `docs/reports/PHASE_SUBSCRIPTION_POINTS_03C_ADMIN_AUDIT_REPORT.md`
- `docs/reports/PHASE_FIRESTORE_ADMIN_ALIGNMENT_REPORT.md`
- `docs/reports/PHASE_FIRESTORE_LEGACY_PATH_DEPENDENCY_AUDIT.md`
- `docs/reports/PHASE_C8.0_ADMIN_PRODUCTION_LEGACY_DATA_INVENTORY_AUDIT.md`
- `app/src/main/java/com/example/repository/AdminRepository.kt`
- `app/src/main/java/com/example/repository/SupportRepository.kt`
- `app/src/main/java/com/example/repository/ProRequestRepository.kt`
- `app/src/main/java/com/example/contract/FirebaseContract.kt`
- `app/src/main/java/com/example/models/Models.kt`
- `app/src/main/java/com/example/models/SupportModels.kt`
- `app/src/main/java/com/example/models/ProRequest.kt`
- `app/src/main/java/com/example/viewmodels/SupportViewModel.kt`
- `app/src/main/java/com/example/viewmodels/ProRequestsViewModel.kt`
- `app/src/main/java/com/example/viewmodels/EconomyViewModel.kt`
- `app/src/main/java/com/example/viewmodels/LoginViewModel.kt`
- `app/src/main/java/com/example/ui/screens/NotificationScreen.kt`
- `app/src/main/java/com/example/ui/screens/SupportInboxScreen.kt`
- `app/src/main/java/com/example/ui/screens/SupportChatScreen.kt`
- `app/src/main/java/com/example/ui/screens/ProRequestsScreen.kt`
- `app/src/main/java/com/example/ui/screens/EconomyConsoleScreen.kt`
- `app/src/main/java/com/example/ui/screens/UserDetailScreen.kt`
- `app/src/main/java/com/example/diagnostics/FirebaseInitializer.kt` إذا كان موجودًا

إذا كان ملف مرجعي بعينه غير موجود في هذا المسار، ابحث عن الاسم الكامل أو الوظيفة المقابلة داخل المشروع مرة واحدة، وسجّل المسار الفعلي. لا تعتبر اختلاف موقع التقرير دليلًا على غيابه ولا تفترض أن الميزة غير موجودة.

### 3.1 Admin Dashboard — دفعة A1: مصادقة المسؤول وصلاحياته

1. تتبّع `LoginViewModel.kt` و`FirebaseInitializer.kt` و`FirebaseContract.kt` وأي `checkIsAdmin`/`isAdmin`/`isOwner`/bootstrap flow.
2. وجود `FirebaseAuth.currentUser` وحده لا يكفي للسماح بواجهة الإدارة. تحقق من مسار التحقق الحقيقي ومن منع الدخول عند الفشل، لكن لا تغيّر نموذج الأدوار من تلقاء نفسك.
3. تأكد من أن أي ميزة إدارة مستخدمين أو مسؤولين لا تمنح المشرف العادي القدرة على إنشاء أو ترقية مشرف آخر. قواعد `/admins/{uid}` المرشحة تحصر الكتابة بحساب المالك؛ لا تضف شاشة أو استدعاء يلتف حول هذا الشرط.
4. لا تخمّن UID المالك ولا تغيّر بريده أو bootstrap. إن احتاج الأمر إثبات UID في Firebase Console فهذه خطوة يدوية لاحقة وليست عملية تنفذها الآن.

### 3.2 Admin Dashboard — دفعة A2: إنشاء الإشعارات وتاريخ التدقيق

**الملفات الأساسية:** `app/src/main/java/com/example/repository/AdminRepository.kt` و`app/src/main/java/com/example/ui/screens/NotificationScreen.kt` والنموذج الفعلي الذي يعرّف `NotificationRequest`.

1. راجع `AdminRepository.sendNotification(request)` والتحويل إلى `target`. القيم القياسية المخزنة هي `all`, `user`, `pro` (أحرف صغيرة) لأنها القيم التي تستخدمها القواعد المرشحة وكاتب الإشعار الحالي. عند `user` يجب أن يكون `targetUid` غير فارغ ومعرّفًا بشكل صحيح. لا تقبل هدفًا فرديًا بلا UID.
2. احتفظ بـ`targetType` فقط إذا كان مطلوبًا للتوافق القديم؛ لا تعتمد عليه بدل `target` ولا تخزّن حالات متعارضة بين الحقلين.
3. تحقق من `title`, `body` أو alias `message`, `type`, `createdBy`, `createdAt`, `expiresAt`, `isActive`, `status` حسب النموذج والقواعد. لا تغيّر نوع الوقت أو الحالة دون مراجعة جميع القارئين.
4. انتظر `docRef.set(...).await()` أو ما يكافئه قبل تسجيل سجل تدقيق يشير إلى نجاح. افحص `logAudit(...)` في جميع العمليات المشابهة: لا تسجل نجاح عملية ذات أثر قبل تأكيد كتابتها. إذا فشل audit بعد نجاح التغيير، أظهر الحالة الجزئية بدل الادعاء بأن كل الخطوات نجحت.
5. `getRecentNotifications()` يمكنه قراءة كامل ما تسمح به قواعد المسؤول، لكنه يجب أن يعالج خطأ listener ويزيل listener عند الإغلاق؛ لا تفتح هذه القراءة لحساب عادي في القواعد.
6. لا تُجرِ ترحيلًا أو تعديلًا جماعيًا لإشعارات قديمة على قاعدة حية. وثّق أن السجلات القديمة التي لا تحتوي `target` تحتاج ترحيلًا منفصلًا.

**معيار القبول الثابت:** كتابة موثقة ومؤكدة، `target` قياسي واحد، تحقق من `targetUid`، ولا audit نجاح قبل تأكيد الكتابة.

### 3.3 Admin Dashboard — دفعة A3: الدعم الفني والرسائل

**الملفات:** `SupportRepository.kt`, `SupportViewModel.kt`, `SupportModels.kt`, `SupportInboxScreen.kt`, `SupportChatScreen.kt`.

1. مسار الدعم هو `/support_conversations/{conversationId}` ورسائله في `/support_conversations/{conversationId}/messages/{messageId}`. لا تستعمل `/conversations` الاجتماعية بدلًا منه.
2. راجع دوال `updateStatus(conversationId, newStatus)`, `markAsReadByAdmin(conversationId)`, `deleteConversation(conversationId)` وكل دوال إرسال الرسائل الموجودة. تحقق من كتابة الحالة/القراءة/الحذف فعليًا وانتظر النجاح قبل تحديث الحالة الظاهرة أو السجل.
3. افحص القيم التي تُخزّن في `status`. اجعل التحويل من enum إلى Firestore متسقًا مع User App والقواعد، ولا تخزن `OPEN` أو `RESOLVED` عن طريق `.name` إذا كان العقد يتطلب `open` و`resolved`. لا تغيّر الحالات قبل فحص النماذج والشاشات والقواعد معًا.
4. لا تُعد فتح تذكرة ضمنيًا عند إرسال رسالة. افصل عملية تغيير الحالة عن إرسال الرسالة، ولا تكتب `createdAt` من جديد عند كل تحديث.
5. في `deleteConversation`, راجع المسار الأساسي والبديل. لا تسجّل نجاحًا قبل اكتمال العملية المطلوبة. إذا كانت القواعد تمنع حذف الرسائل الفرعية من العميل، لا تحاول الالتفاف عليها ولا تبتلع خطأ الحذف؛ أظهر فشلًا مفهومًا ودوّن ما بقي. لا تحذف السجلات تلقائيًا لتجاوز `PERMISSION_DENIED`.
6. احتفظ بحقول ملكية تذكرة الدعم كما يحددها العقد. لا تبدل `userId` أو `conversationId` أو `senderId` لتجعل الكتابة تمر.

**معيار القبول الثابت:** الحالة والقراءة والحذف تُسجّل بعد تأكيد العملية، ولا حذف مخفي/جزئي يُعرض كنجاح كامل.

### 3.4 Admin Dashboard — دفعة A4: طلبات Pro

**الملفات:** `ProRequestRepository.kt`, `ProRequestsViewModel.kt`, `ProRequestsScreen.kt`, `ProRequest.kt` والقواعد ذات الصلة.

1. المسار القياسي هو `/pro_requests/{requestId}`. قراءة المستخدم العادي تُربط بـ`resource.data.userId == request.auth.uid`، والمسؤول يستند إلى دور موثّق بقواعد Firestore.
2. لا تعد إلى شرط يذكر `conversationId` غير معرّف في وثيقة الطلب. لا توسّع القراءة لتكون عامة لإصلاح query.
3. راجع `getAllProRequests()` وأخطاء listener، وفرز `createdAt`, وتحويل الحالة. لا تبتلع `PERMISSION_DENIED` ثم تعرض قائمة فارغة وكأنها لا تحتوي طلبات.
4. راجع مسار `approveRequest(...)` ورفض الطلب وتحديث `/users/{userId}` وطلب التحديث نفسه. أي عملية موافقة يجب أن تتحقق من أن الحالة `PENDING` ومن وجود `userId`، وأن تكتب تحديث المستخدم والطلب ذريًا في transaction إذا كان ذلك هو النمط الحالي. سجّل التدقيق بعد نجاح transaction فقط.
5. لا تسمح للعميل بإنشاء حقول قرار مثل `approvedBy`, `reviewedAt`, `subscriptionTier`, `isPro` أو تفعيل اشتراك لنفسه. لا تغير تجربة شراء/تفعيل الاشتراك إلا بما تسمح به القواعد والعقد.

### 3.5 Admin Dashboard — دفعة A5: الاقتصاد والنقاط وسجل الحركات

**الملفات:** `app/src/main/java/com/example/viewmodels/EconomyViewModel.kt`, `app/src/main/java/com/example/models/EconomyModels.kt`, `app/src/main/java/com/example/ui/screens/EconomyConsoleScreen.kt`, `app/src/main/java/com/example/ui/screens/UserDetailScreen.kt`, ومستودعات الاقتصاد التي يستدعيها المشروع.

1. جرد عمليات القراءة/الكتابة لحقول رصيد النقاط و`/users/{uid}/point_transactions/{txId}` و`/users/{uid}/task_claims/{taskId}` والجوائز والـ leaderboard.
2. افصل استعراض البيانات عن تسويتها. لا تسمح بمسار مباشر أو غير مدقق لتغيير رصيد مستخدم أو العبث بدفتر الحركات. قاعدة السماح الحالية لمسؤول بالكتابة لا تعني تلقائيًا أن العملية آمنة أو متوافقة مع مبدأ ledger غير القابل للتعديل؛ افحص العقد والسلوك والأثر التدقيقي.
3. إذا تعارضت قاعدة تسمح للمسؤول بالكتابة مع توثيق يقول إن دفتر الحركات immutable، لا تتجاهل التناقض ولا تختَر واحدة بصمت. قدّم توصية مبنية على المسار الفعلي، وطبّق فقط تصحيحًا لا يغيّر صلاحيات مالية بصورة غير مصرح بها. إذا احتاج الأمر قرار المالك، سجله كحاجز ولا تنشئ Backend جديدًا.
4. سجّل كل عملية تغيير حساسة بعد تأكيد نجاح Firestore فقط، مع تفاصيل لا تكشف بيانات شخصية أكثر من اللازم. لا تسجّل نجاحًا وهميًا عند رفض القواعد.

### 3.6 Admin Dashboard — دفعة A6: كامل استدعاءات Firestore

ابحث في كامل المشروع عن عمليات `.collection(`, `.document(`, `.get()`, `.set(`, `.update(`, `.delete(`, `runTransaction`, `WriteBatch`, `addSnapshotListener`, `logAudit(`. أنشئ مصفوفة توافق لكل المسارات الحساسة، على الأقل:

- `/admins/{uid}`
- `/users/{uid}` وحقول الاشتراك/الحظر/الرصيد
- `/users/{uid}/point_transactions/{txId}`
- `/notifications/{notificationId}`
- `/pro_requests/{requestId}`
- `/support_conversations/{conversationId}` و`/messages/{messageId}`
- `/auditLogs/{logId}`
- `/config/app`, `/app_updates`, و`/extensions` أو `managed_extensions` بحسب المستخدم الفعلي.

لكل عملية سجّل: ملف/دالة، مسار Firestore، العملية، من يملك الحق وفق القواعد، هل تنتظر نجاح الكتابة، وهل يسجّل audit بعد النجاح. راجع المجموعات التي لم ترد أعلاه أيضًا؛ لا تفترض أن القائمة شاملة.

## 4. قواعد Firestore الموحدة بين المشروعين

- في كل مشروع، راجع `firestore.rules` كاملة. أكّد أن قواعد `/public_profiles/{userId}` لا تكشف غير الحقول العامة، وأن `/users/{userId}` يبقى خاصًا، وأن `/conversations/{conversationId}/readReceipts/{uid}` مقيد بمالك إيصال القراءة، وأن `/pro_requests/{requestId}` يستخدم `resource.data.userId` لقراءة صاحب الطلب.
- تأكد من أن `/admins/{adminUid}` لا يسمح للمشرف العادي بترقية مشرف آخر أو تغيير صلاحيات المالك.
- افحص شروط `/stories`, `/notifications`, `/support_conversations`, `/point_transactions` و`/task_claims` مقابل جميع الكتابات الفعلية في المشروع.
- إذا عدّلت القواعد المرشحة، اشرح كل تغيير وسبب ضرورة أمانه، واحتفظ بالنسخة الكاملة داخل `firestore.rules`. لا تنشرها.
- لأن الجلستين منفصلتان، قد لا يتمكن الوكيل من فتح مشروع الجلسة الثانية. لا يدّعي أنه تحقق من التطابق المتبادل. أعطِ في التقرير النهائي hash SHA-256 لمحتوى `firestore.rules`، واجعل مقارنة القواعد بين المشروعين بندًا مطلوبًا قبل Emulator النهائي. لا تنشئ قاعدة بديلة مختلفة باسم غامض.

## 5. حفظ التقدم والتعامل مع الأخطاء

في جذر المشروع أنشئ أو حدّث `REPAIR_PROGRESS_AR.md` بعد كل دفعة. يجب أن يحتوي:

- اسم المشروع الذي فُحص.
- تاريخ/دفعة التنفيذ.
- الملفات التي فُحصت، حتى لو لم تُعدّل.
- قائمة كل ملف تغير فعليًا والمسار الكامل النسبي وملخص التغيير.
- العيب ودليل وجوده من المصدر قبل الإصلاح.
- ما إذا كان الإصلاح قد طُبق أو كان مطبقًا أصلًا أو تعذر بأمان.
- الفحوص الثابتة التي أُجريت وأمرها ونتيجتها الحقيقية.
- اختبارات لم تُنفذ بعد، مع ذكر أن Emulator وGradle build مؤجلان عمدًا.
- الحواجز المتبقية والقرار المطلوب، لا عبارة عامة مثل «يحتاج مراجعة».
- SHA-256 لملف `firestore.rules` بعد أي تعديل للقواعد.

لا تكتب في `REPAIR_PROGRESS_AR.md` أو الرد النهائي أن إصلاحًا ناجح إلا إذا تغير الكود أو تحقق الوكيل من وجوده بالفعل. لا تملأ التقرير بملفات لم يفتحها. لا تذكر نجاح اختبار لم يتم تشغيله.

## 6. مراحل التنفيذ الإلزامية وتسلسلها

اتبع هذه المراحل بالترتيب، مع تعديل المصدر فعليًا:

1. **S0 — Baseline:** جرد بنية المشروع، قراءة العقد والقواعد والتقارير المحددة، حفظ حالة أولية في `REPAIR_PROGRESS_AR.md`. لا كتابة تطبيق قبل هذه الخطوة.
2. **S1 — Reads/security:** إصلاح قراءات الإشعارات وخصوصية الملفات الشخصية والتأكد من أن الاستعلامات قابلة للسماح وفق القواعد.
3. **S2 — Identity/lifecycle:** إصلاح دورة حياة FCM (في User App) أو مصادقة الإدارة/المالك (في Admin Dashboard).
4. **S3 — Writes/data integrity:** إصلاح الرسائل/إيصالات القراءة/الدعم/حذف الحساب (User App) أو الإشعارات/التدقيق/الدعم/طلبات Pro (Admin Dashboard).
5. **S4 — Economy and sensitive writes:** مراجعة جميع النقاط والمكافآت والاشتراكات والصلاحيات المالية. لا تسوّغ كتابة غير موثوقة؛ الحواجز مقبولة إذا شرحت بدقة.
6. **S5 — Full call-site sweep:** البحث الشامل عن بقية عمليات Firestore والـ listeners والمعاملات وعمليات التدقيق.
7. **S6 — Static review only:** افحص imports، التوقيعات، مسارات البيانات، أخطاء منطقية ظاهرة، توازن البنية، وعدم وجود APIs/paths قديمة. نفّذ فحوصًا ثابتة متاحة ومحدودة فقط؛ لا تشغل Emulator أو Gradle build في هذه المرحلة.
8. **S7 — Handoff:** حدّث `REPAIR_PROGRESS_AR.md`، واكتب `PHASE_20.1-B3-C7_EXECUTION_REPORT_AR.md` في جذر المشروع، واذكر الملفات المعدلة والعوائق والفحوص المنفذة والمؤجلة، واطبع SHA-256 للقواعد. لا تقول إن المشروع جاهز للإنتاج.

لا تقفز إلى S7 مع وجود مراحل لم تُراجع. إذا انتهى الوقت/السياق، احفظ أقرب تقدم واذكر رقم المرحلة التالية بوضوح.

## 7. ما لا يُنفذ إلا في المرحلة النهائية بعد موافقة المستخدم

- Firebase Emulator واختبارات السماح/الرفض والانحدار لـFirestore Rules.
- Gradle/Kotlin build واختبارات التطبيق التي تتطلب بناءً.
- مقارنة `firestore.rules` النهائية بين User App وAdmin Dashboard وحسم الاختلافات.
- التحقق اليدوي من UID المالك بواسطة مالك المشروع.
- أي ترحيل للوثائق القديمة، أو backfill لـ`public_profiles`، أو تعديل بيانات حية.
- أي نشر لقواعد أو تطبيق أو Backend.

**لا تنفذ هذه البنود الآن.** اذكرها كمؤجلة، ولا تخلط بين فحص المصدر ونجاح اختبار تنفيذي.

## 8. صيغة التقرير النهائي المطلوبة

اكتب التقرير بالعربية وبالمسارات الفعلية، ويجب أن يحتوي الأقسام التالية بالترتيب:

1. `نطاق المشروع الذي فُحص`
2. `ملخص النتائج المثبتة من المصدر`
3. `الملفات المعدلة` — جدول: المسار | المشكلة/المتطلب | التغيير الفعلي | سبب الأمان.
4. `ملفات فُحصت ولم تتغير` — مع سبب عدم الحاجة إلى تعديلها.
5. `الحواجز التي تتطلب قرارًا أو Backend موثوقًا`
6. `الفحوص التي نُفذت فعلًا`
7. `الاختبارات المؤجلة عمدًا`
8. `قواعد Firestore — SHA-256 وحالة المزامنة غير المحسومة مع المشروع الآخر`
9. `المرحلة التالية عند قول المستخدم: استمر` أو `جاهز للمراجعة النهائية` إذا اكتملت المراحل المسموحة.

ابدأ الآن بالمرحلة S0، ثم نفّذ أول دفعة Admin Dashboard ناقصة بأمان. لا تنفّذ أي دفعة User App من هذه الجلسة. لا تكتفِ بإعادة صياغة هذا الملف؛ المهمة هي تنفيذ تغييرات صحيحة داخل المصدر.
