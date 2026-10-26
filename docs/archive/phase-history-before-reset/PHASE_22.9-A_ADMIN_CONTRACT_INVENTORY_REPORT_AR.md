# تقرير الجرد والتدقيق الشامل لعقود لوحة الإدارة — PHASE 22.9-A
## CineStream Admin Dashboard Comprehensive Contract Inventory & Source-of-Truth Audit

**تاريخ التدقيق والتوثيق:** 18 أكتوبر 2026  
**معرّف المرحلة:** `PHASE 22.9-A — ADMIN DASHBOARD CONTRACT AUDIT`  
**نطاق التدقيق:** الفحص الداخلي الاستقصائي الشامل لكافة ملفات كود التطبيق، والنماذج، والمستودعات، وقواعد Firestore، وإعدادات البناء المتاحة داخل مساحة عمل لوحة الإدارة (`CineStream Admin Dashboard`).  
**طبيعة المرحلة:** تدقيق واستخراج عقود فقط — يُحظر إجراء أي تعديلات كودية، أو تغيير أسماء الحزم، أو تعديل قواعد Firestore، أو تنفيذ نشر سحابي.

---

### إقرار وحدود الوصول والمنهجية المتبعة
1. **حدود الوصول:** تم حصر هذا التدقيق بالكامل داخل ملفات مشروع لوحة الإدارة المتاحة في هذه الجلسة فقط، دون ادعاء الوصول إلى ملفات `CineStream User App` المباشرة أو فحصها.
2. **الاعتماد على التقارير المرجعية:** استند التقرير إلى العقود المشتركة والتقارير المرجعية الموجودة فعلياً داخل المشروع (`FIREBASE_CONTRACT.md`, `FIREBASE_CONTRACT_V1.md`, `PHASE_22_SHARED_FIREBASE_CONTRACT_AR.md`, `PHASE_22.8-F2`, `PHASE_22.8-G2`) مع توثيق اسم كل ملف ومساره.
3. **تنويه عدم كفاية الواجهة الإدارية:** تم الالتزام الصارم بالقاعدة المعمارية: *وجود واجهة مستخدم إدارية لعنصر ما لا يُعد دليلاً كافياً على أن التغيير يُنفذ سحابياً أو أن تطبيق المستخدم يطبقه*.
4. **تنويه عدم إثبات التوافق النهائي:** هذا التقرير يستخرج حقيقة ما ينفذه مشروع لوحة الإدارة محلياً، **ولا يثبت وحده بأي حال من الأحوال التوافق النهائي بين التطبيقين**، حيث يظل التوافق معلقاً حتى إجراء مقارنة ثنائية مطابقة مع تقرير User App في المراحل اللاحقة.

---

## 1. ملخص حالة CineStream Admin Dashboard الفعلية

| المحور المعماري | الحالة التشغيلية الفعلية | الأدلة الفنية المباشرة |
|---|:---:|---|
| **هوية المشروع وFirebase** | **VALID PROJECT ID / DIVERGENT APPLICATION ID** | `project_id: cinestream-sulo`، `project_number: 979447256418`، العميل المسجل في `google-services.json` هو `com.aistudio.cinestreamadmin.adcxz`، بينما `applicationId` في Gradle هو `com.aistudio.cinestreamadmin.eftzvl`. |
| **جاهزية كود أندرويد (Kotlin)** | **141/141 PASS (100%)** | نجاح 141 اختبار وحدة بنسبة 100% دون أي فشل أو أخطاء برمجية عند استثناء مهمة المعالجة الحسابية المتعارضة. |
| **محاكي قواعد Firestore** | **182/182 PASS (100%)** | اجتياز 182 فحصاً أمنياً شاملاً في محاكي Firestore المحلي (114 في Mocha و68 في Node Test) بنسبة 100%. |
| **أدوات البناء التلقائي (`compile_applet`)** | **BLOCKED BY GOOGLE SERVICES** | فشل المهمة `:app:processDebugGoogleServices` لعدم تطابق اسم الحزمة المؤقت مع العميل المسجل. |
| **حوكمة الصلاحيات (RBAC)** | **STRICT ZERO-TRUST** | حماية المالك (`sulopros01@gmail.com`)، حظر المشرف من ترقية نفسه، حظر العميل من تعديل 45 حقلاً حساساً بدالة `modifyingSensitiveUserFields()`. |
| **استقلالية الاشتراك عن الميزات** | **REMOVE_ADS ONLY** | حصر ميزة الاشتراك بإزالة الإعلانات دون التحكم في الصلاحيات التقنية أو الحظر أو جودات الفيديو. |
| **مسار التعليقات `/comments`** | **UNMAPPED & BLOCKED** | توجد راية `canComment` في حساب المستخدم وزر تحكم إداري، بينما يغيب مسار التعليقات وقواعده تماماً عن Firestore. |

---

## 2. الجرد الشامل لمسارات Firestore والحقول والنماذج والحالات

تم فحص كافة ملفات المصدر الحقيقية في `app/src/main/java/com/example/` لاستخراج العقود الحقيقية:

### أ. مسار سجلات المشرفين المعتمدين: `/admins/{uid}`
* **الملف والمستودع:**
  - النموذج: `app/src/main/java/com/example/models/Models.kt` (`AdminUser`)
  - العقد: `app/src/main/java/com/example/contract/FirebaseContract.kt` (`FirebaseCollections.ADMINS`)
  - المستودع: `app/src/main/java/com/example/repository/AdminRepository.kt` (`checkIsAdmin`, `adminsCollection`)
  - القواعد: `firestore.rules` (السطور 187-193)
* **المسار:** `/admins/{adminUid}` (معرف الوثيقة = UID المشرف)
* **الحقول والأنواع:**
  - `uid: String` (مطابق لمعرف الوثيقة ومعرف المصادقة)
  - `email: String` (البريد الإلكتروني للمشرف)
  - `role: String` (القيمة المعتمدة: `"admin"`, `"superadmin"`)
  - `enabled: Boolean` (القيمة المعتمدة لتفعيل الصلاحية: `true`)
  - `createdAt: Long` (طابع الإنشاء بالمللي ثانية)
* **سلوك التنفيذ الفعلي في لوحة الإدارة:**
  - المالك الأصلي (`sulopros01@gmail.com`) يحصل على الصلاحية الفورية الدستورية عبر البريد الموثق، ويتم إنشاء وثيقة المشرف له تلقائياً كـ `superadmin`.
  - أي مشرف آخر يشترط أن تكون وثيقته موجودة في `/admins/{uid}` وبحقل `enabled == true`.
  - القواعد تحصر إنشاء وتعديل وحذف مستندات `/admins` بمالك المشروع حصراً عبر `isOwnerAccount()`. المشرفون العاديون يملكون صلاحية القراءة فقط لحساباتهم ولحسابات الإدارة.
* **الأدلة الفنية:** السطور 227-282 في `AdminRepository.kt` والسطور 187-193 في `firestore.rules`.
* **نقاط المقارنة مع User App:** التأكد من أن تطبيق المستخدمين يقرأ حسابه فقط ولا يمتلك أي مسار كتابة أو ترقية.

---

### ب. مسار ملفات وحسابات المستخدمين: `/users/{uid}`
* **الملف والمستودع:**
  - النموذج: `app/src/main/java/com/example/models/Models.kt` (`User`, `SubscriptionState`, `CanonicalSubscriptionTier`, `CanonicalSubscriptionStatus`, `CanonicalSubscriptionSource`)
  - العقد: `app/src/main/java/com/example/contract/FirebaseContract.kt` (`FirebaseCollections.USERS`)
  - المستودع: `app/src/main/java/com/example/repository/AdminRepository.kt` (`resilientSetUser`, `adjustUserPoints`, `banAccount`, `toggleFeature`)
  - القواعد: `firestore.rules` (السطور 54-181، والسطور 235-251)
* **المسار:** `/users/{userId}` (معرف الوثيقة = UID المستخدم)
* **جرد الحقول والأنواع والقيم المقبولة:**
  1. **بيانات الحساب والهوية الأساسية:**
     - `uid: String`, `id: String` (مطابق لـ UID)
     - `username: String`, `email: String`, `displayName: String`, `photoUrl: String`
     - `createdAt: Long`, `updatedAt: Long`, `lastLoginAt: Long`, `lastLoginTimestamp: Long`, `lastActiveAt: Long`
     - `isActive: Boolean` (الافتراضي: `true`)
     - `role: String` (الافتراضي: `"user"`)
     - `admin: Boolean`, `isAdmin: Boolean` (الافتراضي: `false`)
  2. **حقول الاشتراك المعيارية والتوافقية (Canonical & Legacy Subscription):**
     - `subscriptionTier: String` (القيم المعيارية: `"FREE"`, `"PRO_LITE"`, `"PRO"`)
     - `planId: String` (القيم المعيارية: `"free"`, `"pro_lite_1d"`, `"pro_lite_7d"`, `"pro_lite_10d"`, `"pro_30d"`)
     - `durationDays: Int?` (عدد أيام الاشتراك، `null` للباقة المجانية)
     - `subscriptionStatus: String` (القيم: `"ACTIVE"`, `"EXPIRED"`, `"CANCELED"`, `"PENDING"`)
     - `subscriptionSource: String` (القيم: `"MONEY"`, `"POINTS"`, `"ADMIN_GRANT"`, `"LEGACY"`)
     - `subscriptionReferenceId: String?` (معرف طلب Pro أو المعاملة)
     - `subscriptionStartedAt: Long?`, `subscriptionExpiresAt: Long?` (طوابع الصلاحية)
     - الحقول التوافقية القديمة: `isPremium: Boolean` (الافتراضي `false`), `isPro: Boolean`, `plan: String?`, `proPlan: String?`, `proExpiresAt: Long?`
  3. **حقول ملخص النقاط والاقتصاد المزدوجة (Reconciled Points Totals):**
     - `pointsBalance: Long` (الرصيد الفعلي الحالي، الافتراضي: `0L`)
     - `totalPointsEarned: Long? = null` (إجمالي النقاط المكتسبة - تسمية لوحة الإدارة)
     - `pointsEarnedTotal: Long? = null` (إجمالي النقاط المكتسبة - تسمية تطبيق المستخدمين)
     - `totalPointsSpent: Long? = null` (إجمالي النقاط المنفقة - تسمية لوحة الإدارة)
     - `pointsSpentTotal: Long? = null` (إجمالي النقاط المنفقة - تسمية تطبيق المستخدمين)
  4. **صلاحيات الميزات وحظر الحسابات (Account Bans & Feature Permissions):**
     - حظر الحساب العام: `isBanned: Boolean` (الافتراضي: `false`), `banReason: String?`, `banExpiresAt: Long?`
     - رايات الصلاحيات الإيجابية:
       * `canWatch: Boolean = true` (مشاهدة الفيديو)
       * `canDownload: Boolean = true` (التحميل دون اتصال)
       * `canChat: Boolean = true` (الدردشة الاجتماعية)
       * `canStory: Boolean = true` (القصص)
       * `canP2P: Boolean = true` (مشاركة الند للند)
       * `canComment: Boolean = true` (التعليق على الوسائط)
       * `canUpload: Boolean = false` (رفع المحتوى)
       * `canRequest: Boolean = true` (طلب الأعمال)
     - رايات الحظر السلبية (للتوافق القديم):
       * `watchBan: Boolean = false`, `downloadBan: Boolean = false`, `chatBan: Boolean = false`, `storyBan: Boolean = false`, `p2pBan: Boolean = false`
     - قيود الأجهزة وإعدادات DRM:
       * `deviceLimit: Int = 2`, `maxDevices: Int = 2`
       * `allowedQuality: String? = null` (الافتراضي إتاحة جميع الجودات)
       * `downloadLimit: Int? = null`, `offlineDaysOverride: Int? = null`, `forcedAdsOverride: Int? = null`
* **سلوك التنفيذ الفعلي في لوحة الإدارة:**
  - القراءة: المشرف يقرأ أي وثيقة مستخدم عبر الاستماع الحي `adminRepository.getUsers()`.
  - الكتابة: المشرف يحدث الوثيقة بدمج ذري `SetOptions.merge()`.
  - تعديل النقاط: يتم عبر `adminRepository.adjustUserPoints` الذي يكتب ذرياً التسميتين (`totalPointsEarned` و `pointsEarnedTotal`) داخل معاملة `runTransaction`.
  - قراءة النقاط: نموذج `User` يطبق دلالة الحفاظ على الصفر المشين:
    ```kotlin
    val effectivePointsEarned: Long get() = totalPointsEarned ?: pointsEarnedTotal ?: 0L
    val effectivePointsSpent: Long get() = totalPointsSpent ?: pointsSpentTotal ?: 0L
    ```
  - الأمان في القواعد: دالة `modifyingSensitiveUserFields()` تحظر على العميل تعديل 45 حقلاً حساساً تشمل كافة حقول الأدوار، والاشتراكات، والنقاط، والحظر، والقيود.
* **نقاط المقارنة مع User App:** التحقق من أن User App يعتمد التسمية المزدوجة للنقاط ولا يحاول كتابة أي حقل حساس محظور.

---

### ج. المسار الفرعي لدفتر حركات النقاط: `/users/{uid}/point_transactions/{txId}`
* **الملف والمستودع:**
  - النموذج: `app/src/main/java/com/example/models/EconomyModels.kt` (`PointTransaction`, `PointTransactionType`)
  - العقد: `FirebaseSubcollections.POINT_TRANSACTIONS`
  - المستودع: `AdminRepository.kt` (`adjustUserPoints`)
  - القواعد: `firestore.rules` (السطور 255-262)
* **المسار:** `/users/{userId}/point_transactions/{txId}`
* **الحقول والأنواع:**
  - `txId: String` (معرف حركة النقاط الفريد)
  - `userId: String` (معرف المستخدم صاحب الحركة)
  - `type: String` (القيم المعيارية لـ `PointTransactionType`: `"DAILY_LOGIN"`, `"REWARDED_AD"`, `"TASK_REWARD"`, `"GAME_REWARD"`, `"LEADERBOARD_REWARD"`, `"SUBSCRIPTION_REDEMPTION"`, `"ADMIN_GRANT"`, `"ADMIN_ADJUSTMENT"`, `"REVERSAL"`)
  - `amount: Long` (قيمة الحركة، موجبة للإضافة وسالبة للخصم)
  - `balanceBefore: Long`, `balanceAfter: Long` (الرصيد قبل وبعد الحركة)
  - `referenceId: String?` (معرف المرجع كمعرف الإعلان أو المهمة)
  - `description: String` (وصف الحركة للمستخدم)
  - `createdAt: Long` (طابع العملية)
  - `actorUid: String` (معرف المشرف أو النظام الذي أنشأ الحركة)
* **سلوك التنفيذ الفعلي والأمان:**
  - دفتر حركات غير قابل للتعديل (Immutable Ledger).
  - كتابة الحركات محصورة حصرياً بالمشرفين المعتمدين (`allow write: if isAdmin();`).
  - قراءة الحركات متاحة للمستخدم صاحب الحساب وللمشرفين فقط.

---

### د. المسار الفرعي لمطالبات المهام: `/users/{uid}/task_claims/{taskId}`
* **الملف والقواعد:**
  - العقد: `FirebaseSubcollections.TASK_CLAIMS`
  - القواعد: `firestore.rules` (السطور 266-271)
* **المسار:** `/users/{userId}/task_claims/{taskId}`
* **الحقول:** `taskId: String`, `claimedAt: Long`, `rewardPoints: Long`.
* **سلوك التنفيذ الفعلي والأمان:**
  - يستخدم لمنع تكرار الحصول على جوائز المهام (Anti-Replay).
  - القراءة متاحة لصاحب الحساب وللمشرف.
  - الكتابة محظورة كلياً على العميل ومحصورة بـ `isAdmin()` لحماية الاقتصاد من التلاعب.

---

### هـ. مسار طلبات ترقية CineStream Pro: `/pro_requests/{requestId}`
* **الملف والمستودع:**
  - النموذج: `app/src/main/java/com/example/models/ProRequest.kt` (`ProRequest`, `ProRequestStatus`)
  - العقد: `FirebaseCollections.PRO_REQUESTS`
  - المستودع: `app/src/main/java/com/example/repository/ProRequestRepository.kt` (`approveRequest`, `rejectRequest`, `deleteRequest`)
  - الشاشات: `app/src/main/java/com/example/ui/screens/ProRequestsScreen.kt`
  - القواعد: `firestore.rules` (السطور 496-547)
* **المسار:** `/pro_requests/{requestId}`
* **جرد الحقول والأنواع:**
  - `requestId: String` (معرف الوثيقة)
  - `userId: String`, `userEmail: String`, `userName: String`
  - `requestedPlan: String = "pro"`, `planId: String? = null`
  - `requestedDuration: String = "1 month"`, `durationDays: Long? = null`
  - `paymentMethod: String? = null`, `paymentProofUrl: String? = null`, `paymentReference: String? = null`
  - `userNote: String = ""`, `notes: String? = null`
  - `status: String` (القيم المعيارية: `"PENDING"`, `"APPROVED"`, `"REJECTED"`, `"CANCELLED"`)
  - `createdAt: Long = 0L`, `updatedAt: Long = 0L`
  - `cancelledAt: Long? = null` (طابع الإلغاء المرسل من العميل)
  - `reviewedAt: Long? = null`, `reviewedBy: String? = null`, `reviewedByEmail: String? = null`
  - `rejectionReason: String? = null`, `adminNote: String? = null`
* **سلوك التنفيذ الفعلي في لوحة الإدارة:**
  - الاعتماد (`approveRequest`): يتم حصرياً عبر معاملة ذرية `firestore.runTransaction`، تتحقق من أن الحالة الحالية `PENDING`، وتقوم بترقية حساب المستخدم في `/users/{userId}` وتمديد الصلاحية من التاريخ النشط وتحديث حالة الطلب إلى `APPROVED` وتسجيل سجل التدقيق.
  - الرفض (`rejectRequest`): يتم عبر معاملة ذرية تلزم إدخال سبب الرفض `rejectionReason` ونقل الحالة إلى `REJECTED`.
  - الإلغاء من المستخدم: محمي بالقواعد في `firestore.rules` (سطر 537) بحيث يُسمح للمالك فقط بنقل حالته من `PENDING` أو `pending` إلى `CANCELLED` المعيارية وتعديل الحقول الثلاثة الحصرية فقط: `['status', 'updatedAt', 'cancelledAt']`.

---

### و. مسار بلاغات وشكاوى المستخدمين: `/reports/{reportId}`
* **الملف والمستودع:**
  - النموذج: `app/src/main/java/com/example/models/Models.kt` (`UserReport`)
  - العقد: `FirebaseCollections.REPORTS`
  - المستودع: `AdminRepository.kt` (`updateReportStatus`, `deleteReport`)
  - الشاشات: `app/src/main/java/com/example/ui/screens/ReportsScreen.kt`, `ReportsViewModel.kt`
  - القواعد: `firestore.rules` (السطور 553-575)
* **المسار:** `/reports/{reportId}`
* **جرد الحقول والأنواع:**
  - `id: String` (معرف الوثيقة)
  - `userId: String`, `userEmail: String`
  - `type: String = "ISSUE"` (الأنواع المعتمدة: `"ISSUE"`, `"STREAM"`, `"BUG"`, `"CONTENT"`)
  - `targetType: String` (الفئات: `"STREAM"`, `"MEDIA"`, `"EXTENSION"`, `"USER"`)
  - `targetId: String` (معرف الوسيط أو الإضافة المبلغ عنها)
  - `title: String`, `description: String`
  - `status: String = "PENDING"` (القيم المعيارية في الإدارة: `"PENDING"`, `"RESOLVED"`, `"DISMISSED"`)
  - `resolutionNotes: String = ""` (ملاحظات المشرف حول المعالجة)
  - `createdAt: Long` (طابع الإنشاء)
  - `resolvedAt: Long? = null` (طابع المعالجة)
  - `resolvedBy: String = ""` (معرف المشرف الذي عالج البلاغ)
* **سلوك التنفيذ الفعلي:**
  - إنشاء العميل: تقبل القواعد إنشاء البلاغ بحالة `'PENDING'` أو `'pending'` مع حظر كتابة أي حقول تخص الحل (`resolvedBy`, `resolvedAt`, `resolutionNotes`).
  - معالجة الإدارة: تتيح واجهة `ReportsScreen.kt` للمشرف اختيارين للمعالجة: `"RESOLVED"` أو `"DISMISSED"`، ويتم تحديث الوثيقة عبر `adminRepository.updateReportStatus` وتوثيق ذلك في سجل التدقيق الأمني.
  - كلمة `"rejected"` غير مستخدمة نهائياً في كود أو واجهات البلاغات بالإدارة.

---

### ز. مسار الإعلانات والإشعارات: `/notifications/{notificationId}`
* **الملف والمستودع:**
  - النموذج: `app/src/main/java/com/example/models/Models.kt` (`NotificationRequest`)
  - العقد: `FirebaseCollections.NOTIFICATIONS`
  - المستودع: `AdminRepository.kt` (`sendNotification`, `deleteNotification`)
  - الشاشات: `app/src/main/java/com/example/ui/screens/NotificationScreen.kt`
  - القواعد: `firestore.rules` (السطور 451-467)
* **المسار:** `/notifications/{notificationId}`
* **جرد الحقول والأنواع:**
  - `id: String` (معرف الوثيقة)
  - `title: String`, `body: String`, `message: String` (توافق مزدوج لنص الإشعار)
  - `type: String = "SYSTEM"` (`"SYSTEM"`, `"ANNOUNCEMENT"`, `"ALERT"`, `"PROMO"`)
  - `target: String = "all"` (الجمهور المستهدف: `"all"`, `"user"`, `"pro"`)
  - `targetType: String` (قيمة Enum المقابلة: `"ALL"`, `"UID"`, `"PRO"`)
  - `targetUid: String?` (إلزامي إذا كان `target == "user"`)
  - `createdBy: String` (معرف المشرف المرسل)
  - `createdAt: Long`, `expiresAt: Long?`, `isActive: Boolean = true`, `status: String = "PENDING"`
* **سلوك التنفيذ والأمان:**
  - الإنشاء محصور كلياً بالمشرفين (`allow write: if isAdmin();`).
  - القراءة للمستخدمين تخضع لسياسة **Fail-Closed**:
    * إذا كان `target == 'all'`: متاح لكافة الموثقين.
    * إذا كان `target == 'user'`: متاح فقط إذا كان `targetUid == request.auth.uid`.
    * إذا كان `target == 'pro'`: متاح فقط لمن يملك اشتراكاً نشطاً.
    * إذا كان حقل `target` غائباً: يُحظر عن كافة المستخدمين العاديين، ومتاح للمشرف فقط.

---

### ح. مسار محادثات وتذاكر الدعم الفني: `/support_conversations/{conversationId}`
* **الملف والمستودع:**
  - النماذج: `app/src/main/java/com/example/models/SupportModels.kt` (`SupportConversation`, `SupportMessage`, `SupportConversationStatus`)
  - العقد: `FirebaseCollections.SUPPORT_CONVERSATIONS` والمسار الفرعي `FirebaseSubcollections.MESSAGES`
  - المستودع: `app/src/main/java/com/example/repository/SupportRepository.kt`
  - القواعد: `firestore.rules` (السطور 581-627)
* **المسارات:**
  - التذكرة: `/support_conversations/{conversationId}` (معرف التذكرة = UID المستخدم)
  - الرسائل: `/support_conversations/{conversationId}/messages/{messageId}`
* **جرد الحقول والأنواع:**
  - التذكرة: `conversationId`, `userId`, `userEmail`, `userName`, `subject`, `status` (`"open"`, `"pending"`, `"resolved"`, `"closed"` بحروف صغيرة حصراً)، `lastMessage`, `lastMessageAt`, `lastMessageTime` (توافق مزدوج للطوابع)، `lastSenderRole`, `unreadByAdmin`, `unreadByUser`, `createdAt`, `updatedAt`.
  - الرسالة: `messageId`, `conversationId`, `senderId`, `senderRole` (`"user"` أو `"admin"`), `senderEmail`, `text`, `mediaUrl: String?`, `timestamp: Long`, `read: Boolean`.
* **سلوك التنفيذ والأمان:**
  - إنشاء التذاكر مقيد بالحروف الصغيرة حصرياً `['open', 'pending']` ومعرف الوثيقة يطابق UID المستخدم.
  - ردود الإدارة تكتب ذرياً الطابعين `lastMessageAt` و `lastMessageTime` في كل رسالة لضمان ثبات الترتيب في الطرفين.
  - العزل التام عن محادثات المستخدمين الاجتماعية `/conversations`.

---

### ط. مسار التكوين العام وتحديثات OTA: `/config/app`
* **الملف والمستودع:**
  - النموذج: `app/src/main/java/com/example/models/Models.kt` (`AppConfig`)
  - العقد: `FirebaseConfigDocs.APP`
  - المستودع: `AdminRepository.kt` (`getAppConfig`, `updateAppConfig`)
  - القواعد: `firestore.rules` (السطور 382-385)
* **المسار:** `/config/app` (وثيقة مفردة Singleton)
* **الحقول والأنواع:**
  - `maintenanceEnabled: Boolean = false` (راية الصيانة العامة)
  - `maintenanceTitle: String`, `maintenanceMessage: String`
  - `minimumVersionCode: Int = 1`, `latestVersionCode: Int = 1`, `latestVersionName: String = "1.0.0"`
  - `apkUrl: String = ""`, `apkSha256: String = ""`, `mandatoryUpdate: Boolean = false`, `releaseNotes: String = ""`
  - `providersJson: String` (كتالوج موفري الوسائط الديناميكي)
  - `defaultOfflineDays: Int = 2`, `defaultForcedAds: Int = 5`
  - إعدادات الوسائط (Cloudinary): `cloudinaryCloudName: String`, `cloudinaryUploadPreset: String` (بدون API Secret نهائياً)
  - `updatedAt: Long`
* **سلوك التنفيذ والأمان:**
  - القراءة متاحة لكافة الموثقين، والكتابة محصورة بالمشرفين.
  - تم إلغاء الاعتماد نهائياً على المسار المهمل `/config/global` وهو محظور بالقواعد.

---

### ي. مسار ترتيب محركات ومصادر البحث: `/config/search_order`
* **الملف والمستودع:**
  - النماذج: `app/src/main/java/com/example/models/SearchOrderConfig.kt` (`SearchOrderConfig`, `DubbingConfig`, `SearchOrderCategory`, `DubbingGroupStatus`)
  - العقد: `FirebaseConfigDocs.SEARCH_ORDER`
  - المستودع: `app/src/main/java/com/example/repository/SearchOrderRepository.kt`
  - الفحص والتحقق: `app/src/main/java/com/example/validation/SearchOrderValidator.kt`
  - القواعد: `firestore.rules` (السطور 389-392)
* **المسار:** `/config/search_order` (وثيقة مفردة Singleton)
* **الحقول والأنواع:**
  - `movie: List<String>` (قائمة معرفات الإضافات المرتبة للأفلام)
  - `tv: List<String>`, `series: List<String>` (اسم توافقي مطابق لـ tv)
  - `anime: List<String>`
  - `arabicDubbed: List<String>`, `arabicDub: List<String>`, `arabic_dub: List<String>` (تسميات توافقية مزدوجة)
  - `englishDubbed: List<String>`, `englishDub: List<String>`, `english_dub: List<String>` (تسميات توافقية مزدوجة)
  - `dubbingConfig: Map<String, String>` (`arabicDubbedStatus: "ACTIVE"`, `englishDubbedStatus: "MAINTENANCE"`)
  - `updatedAt: Long`, `updatedBy: String`
* **سلوك التنفيذ:** تدعم لوحة الإدارة ترتيب القوائم بأطوال غير محدودة دون أي أكواد برمجية تنفيذية، مع كتابة الحقول التوافقية لضمان عمل كافة محركات البحث في تطبيق المستخدمين.

---

### ك. مسارات كتالوج الإضافات البرمجية وتحديثات التطبيق:
1. **كتالوج الإضافات المدارة `/managed_extensions/{extensionId}`:**
   - النموذج: `app/src/main/java/com/example/models/ManagedExtension.kt` (`ManagedExtension`)
   - الحقول: `extensionId`, `scraperKey`, `name`, `description`, `baseUrl`, `searchUrl`, `iconUrl`, `runtimeApiVersion`, `definitionVersion`, `minAppVersionCode`, `status` (`ACTIVE`, `MAINTENANCE`, `DISABLED`, `DEPRECATED`), `priority`, `language`, `capabilities`, `contentTypes`, `audioCapabilities`, `enabled`, `createdAt`, `updatedAt`.
   - **الخاصية المعمارية الصارمة:** مستودع تكوين نقي 100% خالٍ تماماً من أي ملفات APK أو حزم ثنائية أو أكواد جافاسكريبت أو DexClassLoader.
2. **كتالوج إصدارات التطبيق `/app_updates/{updateId}`:**
   - النموذج: `app/src/main/java/com/example/models/Models.kt` (`AppUpdate`)
   - الحقول: `id`, `versionCode`, `versionName`, `minVersionCode`, `apkUrl`, `apkSha256`, `mandatoryUpdate`, `releaseNotes`, `status` (`PUBLISHED`, `DRAFT`, `ARCHIVED`), `publishedBy`, `createdAt`, `updatedAt`.

---

### ل. مسارات الاقتصاد والمكافآت ومتصدرين النقاط:
1. **قوائم المهام `/reward_tasks/{taskId}`:**
   - الحقول: `taskId`, `title`, `description`, `rewardPoints`, `taskType` (`CUSTOM`, `WATCH_VIDEO`, `FOLLOW_SOCIAL`, `SURVEY`, `SHARE_APP`), `actionUrl`, `isActive`, `expiresAt`, `createdAt`, `updatedAt`.
2. **إعدادات الاقتصاد `/config/economy` وبوابات الميزات `/config/features`:**
   - أسعار باقات Pro مقابل النقاط: `pro_lite_1d: 50L`, `pro_lite_7d: 250L`, `pro_lite_10d: 350L`, `pro_30d: 1000L`.
   - سلم جوائز الدخول اليومي لـ 7 أيام، وضوابط الإعلانات المكافئة.
3. **متصدرين النقاط الأسبوعي `/leaderboard/weekly_current`:**
   - حقول القراءة: `cycleId`, `updatedAt`, `rankings: List<LeaderboardEntry>`.

---

### م. مسار سجلات التدقيق الأمني: `/auditLogs/{logId}`
* **النموذج:** `AuditLog`
* **الحقول:** `id`, `actorUid`, `adminUid`, `adminEmail`, `action`, `targetType`, `targetId`, `details`, `createdAt`.
* **الأمان:** محصور كلياً بالمشرفين قراءة وكتابة (`allow read, write: if isAdmin();`). المسار القديم `/audit_logs` محظور صراحة.

---

## 3. التدقيق المعمق للعقود الحساسة

### أ. عقد التعليقات ومساراتها (Comments Reality & Truth):
1. **الواقع الفعلي في كود لوحة الإدارة:**
   - لا توجد أي شاشة أو واجهة مستخدم للتعليقات في كامل لوحة الإدارة (فحص `AppNavigation.kt` و `app/src/main/java/com/example/ui/screens/`).
   - لا يوجد أي نموذج عرض `CommentsViewModel` ولا مستودع `CommentRepository`.
   - لا توجد أي دوال لجلب التعليقات، أو حذفها، أو مراجعتها، أو التبليغ عنها.
   - في `FirebaseContract.kt`: **لا يوجد أي تعريف لمجموعة `/comments` أو مسار فرعي لها**.
   - في `firestore.rules`: **لا توجد أي قاعدة أمان تسمح بقراءة أو كتابة التعليقات**.
2. **ما هو موجود فعلياً فقط:**
   - حقل راية الصلاحية في وثيقة المستخدم: `val canComment: Boolean = true` في `Models.kt`.
   - زر تبديل في شاشة تفاصيل المستخدم `UserDetailScreen.kt`:
     `onToggle = { onToggle("comment", user.isCommentAllowed) }`.
   - حماية الحقل من تعديل العميل في `firestore.rules` (السطر 99 والسطر 164).
3. **الأثر التشغيلي:**
   - راية `canComment` هي مجرد قيد حوكمة على مستوى الحساب (مثل `canWatch` و `canChat`).
   - غياب مسار التعليقات وقواعده في Firestore يعني أن أي استدعاء سحابي لقراءة أو كتابة التعليقات يواجه فوراً رفضاً أمنياً قاطعاً `PERMISSION_DENIED` بالقاعدة الافتراضية الشاملة `match /{document=**} { allow read, write: if false; }`.

---

### ب. عقد طلبات Pro ودورة حياة الحالات:
1. **المسار:** `/pro_requests/{requestId}`.
2. **آلة الحالات الحتمية:**
   $$\text{PENDING / pending} \longrightarrow \begin{cases} \text{APPROVED} & (\text{بواسطة المشرف ذرياً عبر runTransaction}) \\ \text{REJECTED} & (\text{بواسطة المشرف ذرياً مع سبب الإلزام}) \\ \text{CANCELLED} & (\text{بواسطة صاحب الطلب حصرياً}) \end{cases}$$
3. **قاعدة الإلغاء الصارمة:**
   - التحقق المزدوج من المالك: `resource.data.userId == request.auth.uid && request.resource.data.userId == request.auth.uid`.
   - الحالة السابقة المقبولة: `PENDING` أو `pending` لضمان عدم حرمان الطلبات التاريخية.
   - الحالة المستهدفة: `CANCELLED` حصراً (أحرف كبيرة)، ورفض الحالات الصغرى `cancelled` أو التهجئات البديلة `CANCELED`.
   - الحقول المسموح بتعديلها محصورة في الحقول الثلاثة: `['status', 'updatedAt', 'cancelledAt']`.
   - حظر إعادة فتح أي حالة نهائية (`APPROVED`, `REJECTED`, `CANCELLED`).
   - الحذف محصور بالمشرفين فقط.

---

### ج. عقد الإشعارات وقواعد الاستهداف (Fail-Closed Architecture):
1. **المسار:** `/notifications/{notificationId}`.
2. **الاستهداف (`target`):**
   - `"all"`: عام لكافة المستخدمين المسجلين.
   - `"user"`: خاص بمستخدم محدد، ويلزم في الكود والقواعد توفير `targetUid` ومطابقته لـ `request.auth.uid`.
   - `"pro"`: مقيد بمشتركي باقات Pro النشطة حصراً.
3. **سياسة الحظر الافتراضي (Fail-Closed):**
   - أي إشعار قديم أو غير مصنف يفتقد حقل `target` يُمنع نهائياً عن المستخدمين العاديين ويتاح للمشرف فقط لمنع تسريب أي رسائل خاصة قديمة.

---

### د. عقد الصلاحيات وفصل الأدوار (Zero-Trust Privilege Boundary):
1. **حصانة المالك:**
   - الحساب الأصلي للمالك (`sulopros01@gmail.com`) هو الجهة الوحيدة المخولة بمنح أو سحب صلاحيات المشرفين في `/admins/{uid}` (`isOwnerAccount()`).
   - المشرفون العاديون يملكون صلاحيات تشغيلية مدققة (`isAdmin()`) ولا يملكون صلاحية ترقية أنفسهم أو غيرهم.
2. **حظر الحقول الحساسة:**
   - المستخدم العادي ممنوع قطعياً من تعديل 45 حقلاً حساساً تشمل الاشتراك، وحظر الحساب، ورصيد النقاط، وسجلات التدقيق عبر `modifyingSensitiveUserFields()`.
3. **فصل الصلاحيات الفنية عن الاشتراك:**
   - ميزة الاشتراك محصورة بـ **إزالة الإعلانات فقط (`REMOVE_ADS ONLY`)**.
   - قيود الحساب الفنية (الحظر من المشاهدة، أو التحميل، أو الدردشة، أو القصص، أو التعليق، أو قيود الأجهزة) هي صلاحيات مستقلة يديرها المشرف، ولا يرفعها اشتراك المستخدم في باقة Pro.

---

## 4. نتائج فحص قواعد Firestore والاختبارات المنفذة فعلياً

### أ. فحص قواعد أمان Firestore (`firestore.rules`):
- **حجم الملف:** 810 سطور برمجية.
- **بصمة التجزئة الرقمية (SHA-256):** `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`.
- **الموقف الأمني:** خلو القواعد تماماً من أي منح عشوائي (No Wildcard Grants)، واختتامها بقاعدة الرفض الشاملة:
  ```javascript
  match /{document=**} {
    allow read, write: if false;
  }
  ```

---

### ب. النتائج الفعلية للاختبارات الحية المنفذة في هذه الجلسة:

| جناح الاختبار / الأداة | بيئة التنفيذ | عدد الاختبارات | النتيجة الفعلية المباشرة | الملاحظات والأدلة |
|---|:---:|---|:---:|---|
| **جناح `firestore-rules.test.js`** | Mocha / Emulator | 114 | **114/114 PASS (100%)** | اختبارات حصانة المالك، حمولة إلغاء Pro، خصوصية الإشعارات، حظر حقول المستخدمين |
| **جناح `phase05b-comprehensive-rules`** | Node Test / Emulator | 32 | **32/32 PASS (100%)** | اختبارات المسارات المهملة والرفض الشامل وتحديثات OTA |
| **جناح `phase03a1-rules`** | Node Test / Emulator | 36 | **36/36 PASS (100%)** | اختبارات دفتر حركات النقاط ومطالبات المهام ومصفوفة الاقتصاد |
| **إجمالي فحوصات المحاكي (Emulator)** | **Firebase CLI** | **182** | **182/182 PASS (100%)** | **0 فشل، اجتياز أمني كامل بنسبة 100% في زمن قياسي** |
| **اختبارات وحدة كود أندرويد (Kotlin)** | `./gradlew testDebugUnitTest -x :app:processDebugGoogleServices` | 141 | **141/141 PASS (100%)** | 105 في `CineStreamAdminLogicTest`، و32 في `SearchOrderArchitectureTest`، و2 في `ExampleRobolectricTest`، و1 في `FirebaseTest`، و1 في `ExampleUnitTest` |
| **أدوات البناء القياسية (`compile_applet`)** | Gradle Lifecycle | - | **BUILD FAILED** | تعطل المهمة `:app:processDebugGoogleServices` لعدم تطابق اسم الحزمة المؤقت مع ملف التكوين |

---

## 5. قائمة بالمشكلات المؤكدة داخل مشروع الأدمن

1. **المشكلة المؤكدة الأولى: تعارض اسم الحزمة في البناء القياسي:**
   - **الواقع:** `applicationId = "com.aistudio.cinestreamadmin.eftzvl"` في `build.gradle.kts`، بينما `google-services.json` يسجل العميل `com.aistudio.cinestreamadmin.adcxz`.
   - **الأثر المباشر:** فشل مهام البناء التلقائية القياسية التي تعتمد على إضافة Google Services ما لم يتم عزل المهمة أو توحيد اسم الحزمة.
2. **المشكلة المؤكدة الثانية: الفراغ الحوكمي لمسار التعليقات:**
   - **الواقع:** وجود راية تحكم `canComment` في حساب المستخدم دون وجود أي مجموعة `/comments` أو قواعد أمان في Firestore لتخزين أو جلب التعليقات.
   - **الأثر المباشر:** تعطل أي استدعاء لقراءة أو كتابة التعليقات سحابياً.
3. **المشكلة المؤكدة الثالثة: حدود النشر السحابي غير المؤكدة (`UNVERIFIED`):**
   - **الواقع:** استقلال ملف `firestore.rules` وبصمته محلياً وعدم تنفيذ أي نشر سحابي حي (`No firebase deploy`).
   - **الأثر المباشر:** لا يمكن الجزم هندسياً بتطابق القواعد المنشورة في سحابة Firebase الحية مع الملف المحلي دون استعلام سحابي مباشر.

---

## 6. قائمة بالعقود التي تحتاج إلى مقارنة مع User App لاحقاً

عند توفر تقرير فحص `CineStream User App`، يجب تدقيق ومقارنة العقود التالية:

1. **عقد التعليقات (Comments Path & Schema):**
   - هل يستخدم تطبيق المستخدم مساراً منبسطاً `/comments/{id}` أم مساراً فرعياً تحت الوسائط `/media/{id}/comments/{id}`؟
   - ما هي الحقول والأنواع المكتوبة في وثيقة التعليق؟
2. **معرّف حزمة تطبيق المستخدم (User App Package Name):**
   - التأكد هل يستخدم `com.aistudio.cinestream.xyzabc` كما هو مسجل في `google-services.json`، أم اسماً آخر.
3. **عقد حمولة إنشاء البلاغات (`/reports`):**
   - التأكد من أن User App ينشئ البلاغات بحالة `PENDING` أو `pending` ولا يرسل حقولاً إدارية غير مصرحة تسبب رفض القواعد لبلاغاته.
4. **عقد استهلاك التكوين وترتيب البحث (`/config/*`):**
   - التحقق من الحقول التي يقرؤها User App من `/config/app` و `/config/search_order`.
5. **بصمة قواعد Firestore لتطبيق المستخدمين:**
   - مقارنة ملف القواعد الفعلي والبصمة المعتمدة في تطبيق المستخدم لضمان التوافق التام.

---

## 7. قسم منفصل للتعارضات المحتملة (دون تقديمها كحقائق مؤكدة)

*ملاحظة منهجية: هذه النقاط تمثل مساحات خطر واحتمالات تعارض تستوجب التدقيق الثنائي في المرحلة القادمة، ولا يُدّعى ثبوتها كحقائق قطعية في غياب فحص ملفات تطبيق المستخدم:*

1. **احتمال تعارض مسار التعليقات:** يُحتمل أن يكون تطبيق المستخدمين قد بنى شاشات تعليقات تعتمد على مسار Firestore محدد غير مدعوم في القواعد الحالية للوحة الإدارة.
2. **احتمال تعارض مسميات حالات البلاغات:** يُحتمل أن يتوقع تطبيق المستخدمين حالة باسم `"rejected"` عند رفض البلاغ، بينما تعتمد لوحة الإدارة والعقد الموحد حالة `"DISMISSED"`.
3. **احتمال وجود إصدارات قديمة من حمولة إلغاء Pro:** يُحتمل أن تحاول نسخ قديمة من تطبيق المستخدمين إرسال حقول إضافية عند الإلغاء بدلاً من الاقتصار على الحقول الثلاثة الحصرية المعتمدة في القواعد (`status, cancelledAt, updatedAt`).
4. **احتمال اعتماد تطبيق المستخدم على اسم حزمة مختلف في المصادقة:** يُحتمل أن يؤثر اختلاف أسماء الحزم بين Gradle وملف Firebase على تدفقات تسجيل الدخول بـ Google إن لم تكن الحزم موحدة في Firebase Console.

---

## 8. قائمة بأسماء الملفات والأدلة التي استند إليها التقرير

### أ. ملفات التكوين والهوية والبناء:
- `/app/build.gradle.kts`: معرف التطبيق والنسخة وإعدادات التوقيع.
- `/app/google-services.json`: رقم المشروع، معرف المشروع، وتسجيلات العملاء وشهادات التوقيع.
- `/app/src/main/AndroidManifest.xml`: الأنشطة والصلاحيات.
- `/app/src/main/java/com/example/diagnostics/FirebaseInitializer.kt`: كود تهيئة Firebase التراجعي والتخزين المؤقت.
- `/app/src/main/java/com/example/MyApplication.kt`: تهيئة التطبيق ومسجلات الأخطاء.

### ب. ملفات العقود والنماذج البرمجية:
- `/app/src/main/java/com/example/contract/FirebaseContract.kt`: العقد الموحد للمجموعات والمستندات.
- `/app/src/main/java/com/example/models/Models.kt`: نماذج المستخدمين، التكوين، البلاغات، الإشعارات، التحديثات، والاشتراكات.
- `/app/src/main/java/com/example/models/ProRequest.kt`: نموذج طلبات ترقية Pro وحالاتها وحمولة الإلغاء.
- `/app/src/main/java/com/example/models/EconomyModels.kt`: نماذج دفتر حركات النقاط، المهام، وبوابات الميزات.
- `/app/src/main/java/com/example/models/SupportModels.kt`: نماذج تذاكر الدعم والرسائل والحالات الصغرى.
- `/app/src/main/java/com/example/models/ManagedExtension.kt`: نموذج الإضافات المدارة ومواصفات السيرفرات.
- `/app/src/main/java/com/example/models/SearchOrderConfig.kt`: نموذج ترتيب البحث وحالات الدبلجة.

### ج. ملفات المستودعات والتحقق البرمجي:
- `/app/src/main/java/com/example/repository/AdminRepository.kt`: إدارة المستخدمين، التكوين، النقاط، الإشعارات، وسجل التدقيق.
- `/app/src/main/java/com/example/repository/ProRequestRepository.kt`: المعاملات الذرية لاعتماد ورفض طلبات Pro.
- `/app/src/main/java/com/example/repository/SupportRepository.kt`: إدارة التذاكر، الرسائل، والتزامن المزدوج للطوابع.
- `/app/src/main/java/com/example/repository/ManagedExtensionRepository.kt`: كتالوج الإضافات النقي.
- `/app/src/main/java/com/example/repository/SearchOrderRepository.kt`: حفظ وجلب ترتيب البحث.
- `/app/src/main/java/com/example/validation/SearchOrderValidator.kt`: التحقق البرمجي من صحة ترتيب البحث.
- `/app/src/main/java/com/example/validation/ManagedExtensionValidator.kt`: التحقق من مواصفات الإضافات.

### د. ملفات القواعد والاختبارات المعتمدة:
- `/firestore.rules`: قواعد الأمان المحدثة والمحصنة (810 سطور، بصمة `207aade4...`).
- `/firestore-tests/firestore-rules.test.js`: جناح اختبارات المحاكي المركزي (114 فحصاً).
- `/firestore-tests/phase05b-comprehensive-rules.test.js`: جناح اختبارات المحاكي الشامل (32 فحصاً).
- `/firestore-tests/phase03a1-rules.test.js`: جناح اختبارات محاكي الاقتصاد (36 فحصاً).
- `/app/src/test/java/com/example/CineStreamAdminLogicTest.kt`: اختبارات منطق الأعمال وحمولة الإلغاء والنقاط (105 اختبارات).
- `/app/src/test/java/com/example/SearchOrderArchitectureTest.kt`: اختبارات هيكل ترتيب البحث (32 اختباراً).
- `/app/src/test/java/com/example/ExampleRobolectricTest.kt`: اختبارات المكونات (اختباران).
- `/app/src/test/java/com/example/FirebaseTest.kt`: اختبار تكوين Firebase (اختبار واحد).
- `/app/src/test/java/com/example/ExampleUnitTest.kt`: اختبار وحدة قياسي (اختبار واحد).

### هـ. التقارير المرجعية السابقة:
- `/PHASES/FIREBASE_CONTRACT.md` و `/PHASES/FIREBASE_CONTRACT_V1.md`: العقود المرجعية للبيانات.
- `/PHASES/PHASE_22_SHARED_FIREBASE_CONTRACT_AR.md`: مصفوفة التوافق المشترك السابقة.
- `/PHASES/PHASE_22_CROSS_PROJECT_COMPATIBILITY_REPORT_AR.md`: تقرير التحقق المشترك.
- `/PHASES/PHASE_22.8-F2_ADMIN_FINAL_VERIFICATION_REPORT_AR.md`: تقرير التحقق النهائي للأدمن.
- `/PHASES/PHASE_22.8-G2_ADMIN_RECONCILIATION_REPORT_AR.md`: تقرير مطابقة القرارات.

---

## 9. التوضيح الصريح بعدم كفاية هذا التقرير وحده لإثبات التوافق

يؤكد هذا التقرير بصورة قاطعة لا لبس فيها:
- أن كافة البيانات والحقائق المذكورة هنا تعكس **الحالة الذاتية الفعلية لكود وقواعد CineStream Admin Dashboard فقط**.
- هذا التقرير **لا يثبت وحده التوافق النهائي بين لوحة الإدارة وتطبيق المستخدمين**.
- إن إثبات التوافق التشغيلي النهائي يتطلب بالضرورة استلام وفحص تقرير الجرد المماثل الصادر من `CineStream User App`، وإجراء مقارنة ثنائية متقابلة سطر بسطر لكافة المسارات والمخططات والحمولات، وحسم أي تباينات معمارية بقرارات مشتركة معتمدة قبل أي نشر أو إنتاج.

---
*تم إنجاز التقرير بالكامل والتوقف الفوري دون إجراء أي تعديلات كودية أو نشر سحابي.*
