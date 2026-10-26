# ملف بيان توافق لوحة الإدارة — PHASE 22.6-B: CineStream Admin Dashboard
## Admin Compatibility Manifest & Authoritative Local Contract (Post-Cancellation Alignment)

**تاريخ التوثيق:** 2026-10-16  
**مشروع مساحة العمل:** CineStream Admin Dashboard (`com.example` / `com.aistudio.cinestreamadmin.eftzvl`)  
**معرّف المرحلة:** `PHASE_22.6-B — ADMIN_RULES_AND_CONTRACT_ALIGNMENT`  
**التقرير المرجعي لتطبيق المستخدم:** `PHASE_22.6-A_USER_APP_PRO_CANCELLATION_ALIGNMENT_REPORT_AR.md`  
**بصمة القواعد المحسوبة للوحة الإدارة (SHA-256):** `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`  
**بصمة القواعد المرصودة لتطبيق المستخدم (PHASE 22.6-A):** `54c8570a25ece1fb6a57bfb0aa2560a50202240a30d4a4cf7a8afd694a18d5ef`  
**حالة التوافق المحلي:** `100% LOCAL COMPLIANCE & VERIFIED` (توافق محلي تام ومختبر، مع بقاء وحدة النشر السحابي مصنفة كـ `UNVERIFIED`).

---

### 1. هوية المشروع والملفات المرجعية المفحوصة والمحدثة

تم استخراج وتحديث بيانات هذا البيان بالكامل من فحص واختبار ملفات المصدر الحقيقية للوحة الإدارة:
* **عقود Firestore المركزية:** `app/src/main/java/com/example/contract/FirebaseContract.kt`
* **قواعد الأمان النشطة والمحصنة:** `firestore.rules` (810 سطور، SHA-256: `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`)
* **المستودعات (Repositories):**
  - `AdminRepository.kt` (المستخدمون، الكتابة المزدوجة الذرية للنقاط، التكوين، الإشعارات المحصنة، سجل التدقيق، ترتيب البحث)
  - `SupportRepository.kt` (محادثات الدعم الفني، المزامنة المزدوجة للطوابع `lastMessageAt`/`lastMessageTime`، الحالات المعيارية بالحروف الصغيرة)
  - `ProRequestRepository.kt` (طلبات الترقية، استيعاب المخطط المزدوج، المعاملات الذرية `runTransaction`)
  - `ManagedExtensionRepository.kt` (كتالوج الإضافات المدارة بدون حزم APK)
  - `SearchOrderRepository.kt` (ترتيب محركات ومصادر البحث)
* **النماذج (Models & Enums):**
  - `ProRequest.kt` (`ProRequest`, `ProRequestStatus`: دعم PENDING, APPROVED, REJECTED, CANCELLED، وخاصية `cancelledAt`، واستيعاب `planId`/`requestedPlan`، `durationDays`/`requestedDuration`، `paymentReference`/`paymentProofUrl`، `notes`/`userNote`)
  - `Models.kt` (`User` مع حقول النقاط المعرفة كـ `Long? = null` لتمييز الصفر عن الغياب ومنطق الأولوية `effectivePointsEarned`/`effectivePointsSpent`)
  - `SupportModels.kt` (`SupportConversation` مع التوافق المزدوج للطوابع الزمنية والحالات الصغرى `open, pending, resolved, closed`)
  - `EconomyModels.kt` (`PointTransaction`, `PointLedgerEntry`, `TaskClaim`, `RewardTask`)
  - `SearchOrderConfig.kt` (`SearchOrderConfig`)
* **واجهات المستخدم (UI Screens):**
  - `ProRequestsScreen.kt` (عرض البيانات الموحدة والمعيارية للطلبات، دعم تفاصيل مرجع الدفع وعدد الأيام وملاحظات المستخدم والحالة الملغاة)
  - `UserDetailScreen.kt` (عرض إجماليات النقاط المتوافقة `effectivePointsEarned`/`effectivePointsSpent`)
  - `EconomyConsoleScreen.kt` (عرض ملخصات النقاط المتوافقة في جدول المستخدمين)
* **جناح اختبارات المحاكي ووحدات الاختبار (المرحلة 22.6-B):**
  - `firestore-tests/firestore-rules.test.js`: **114 فحصاً أمنياً ناجحاً (0 فشل)**
  - `firestore-tests/phase05b-comprehensive-rules.test.js`: **32 فحصاً أمنياً ناجحاً (0 فشل)**
  - `firestore-tests/phase03a1-rules.test.js`: **36 فحصاً أمنياً ناجحاً (0 فشل)**
  - `app/src/test/java/com/example/CineStreamAdminLogicTest.kt`: **105 اختبارات منطق أعمال ناجحة**
  - `app/src/test/java/com/example/SearchOrderArchitectureTest.kt`: **32 اختباراً ناجحاً**
  - `app/src/test/java/com/example/ExampleRobolectricTest.kt`: **2 اختباران ناجحان**
  - `app/src/test/java/com/example/FirebaseTest.kt`: **1 اختبار ناجح**
  - `app/src/test/java/com/example/ExampleUnitTest.kt`: **1 اختبار ناجح**
  - **الإجمالي المحدث:** **182 فحصاً في المحاكي + 141 اختبار وحدة = 323 اختباراً ناجحاً بنسبة 100% دون أي فشل**.

---

### 2. جرد المسارات المشتركة في Cloud Firestore

| المسار المرجعي | طبيعة المستند | الكاتب الأساسي | القارئ الأساسي | القيد الأمني في `firestore.rules` |
|---|---|---|---|---|
| `/admins/{adminUid}` | وثيقة المشرف | مالك المشروع فقط (`isOwnerAccount()`) | المشرفون والمستخدم صاحب الحساب | `allow read: if isAdmin() \|\| (isAuthenticated() && request.auth.uid == adminUid); allow write: if isOwnerAccount();` |
| `/users/{uid}` | الملف الشخصي للمستخدم | المشرف (حقول إدارية واشتراكات) / المستخدم (اسمه وصورته) | المستخدم نفسه / المشرف | قراءة خاصة بالمالك والمشرف. تعديل المستخدم محمي بـ `modifyingSensitiveUserFields()` لمنع تعديل 45 حقلاً حساساً تشمل إجماليات النقاط ورصيدها. |
| `/users/{uid}/point_transactions/{txId}` | دفتر حركات النقاط | المشرف فقط (معاملة ذرية) | المستخدم نفسه / المشرف | قراءة خاصة. كتابة ممنوعة على العميل (`allow write: if isAdmin()`). غير قابل للتعديل (Immutable Ledger). |
| `/users/{uid}/task_claims/{taskId}` | مطالبات المهام | المشرف / دوال النظام | المستخدم نفسه / المشرف | قراءة خاصة للمستخدم. كتابة العميل ممنوعة تماماً (`allow write: if isAdmin()`). |
| `/notifications/{notificationId}` | الإعلانات والإشعارات | المشرف فقط (`isAdmin()`) | المستخدمون المصرح لهم بحسب الجمهور | قراءة مقيدة بالجمهور الصريح (`target == 'all'`, `user` مع مطابقة `targetUid`, `pro` مع اشتراك نشط). المستندات المفتقدة لـ `target` تخضع لسياسة **Fail-Closed** وترفض للمستخدم العادي. |
| `/support_conversations/{convId}` | تذاكر ومحادثات الدعم (معرف الوثيقة = UID المستخدم) | المشرف / المستخدم صاحب التذكرة | المشرف / صاحب التذكرة | قراءة مشتركة لصاحب التذكرة والمشرف. إنشاء العميل مقيد بـ `['open', 'pending']` بالحروف الصغيرة حصرياً. رفض الحالات الكبيرة. |
| `/support_conversations/{convId}/messages/{msgId}` | رسائل الدعم الفني | المشرف / صاحب التذكرة | المشرف / صاحب التذكرة | إنشاء مقيد بـ `senderId == request.auth.uid`. حظر تزوير دور المشرف (`senderRole == 'admin'`). |
| `/pro_requests/{requestId}` | طلبات ترقية CineStream Pro | المستخدم (إنشاء وإلغاء مقيد) / المشرف (مراجعة) | المستخدم صاحب الطلب / المشرف | إنشاء بحالة `PENDING` أو `pending` دون حقول المراجعة. المستخدم صاحب الطلب مسموح له فقط بنقل حالته من `PENDING` أو `pending` إلى `CANCELLED` وتعديل الحقول الثلاثة الحصرية `['status', 'updatedAt', 'cancelledAt']`. منع إعادة فتح الحالات النهائية. الحذف للمشرف فقط. |
| `/config/app` | التكوين العام وOTA والصيانة | المشرف فقط | كافة الموثقين | قراءة للموثقين `isAuthenticated()`. كتابة المشرفين فقط `isAdmin()`. |
| `/config/search_order` | ترتيب محركات البحث | المشرف فقط | كافة الموثقين | قراءة للموثقين. كتابة المشرفين فقط. |
| `/config/features` | بوابات وتفعيلات الميزات | المشرف فقط | كافة الموثقين | قراءة للموثقين. كتابة المشرفين فقط. |
| `/config/economy` | معايير النقاط والمكافآت | المشرف فقط | كافة الموثقين | قراءة للموثقين. كتابة المشرفين فقط. |
| `/auditLogs/{logId}` | سجلات التدقيق الأمني | المشرف فقط (إلحاق) | المشرفون فقط | قراءة وكتابة محصورة بـ `isAdmin()`. حظر تام على المستخدمين العاديين. |
| `/managed_extensions/{extId}` | كتالوج الإضافات البرمجية | المشرف فقط | كافة الموثقين | قراءة للموثقين. كتابة وتعديل وحذف للمشرفين فقط. |
| `/app_updates/{updateId}` | كتالوج تحديثات التطبيق | المشرف فقط | كافة الموثقين | قراءة للموثقين. كتابة للمشرفين فقط. |
| `/reports/{reportId}` | بلاغات الأعطال والمحتوى | المستخدم (إنشاء) / المشرف (حل) | المشرف / صاحب البلاغ | إنشاء بحالة `PENDING` فقط. تحديث الحالة والملاحظات للمشرف فقط. |
| `/conversations/{convId}` | المحادثات الاجتماعية | المستخدمون المشاركون | المشاركون والمشرف | قراءة وإنشاء مقيد بالمشاركين والتحقق من عدم الحظر (`canChat`). |
| `/stories/{storyId}` | القصص التفاعلية | المستخدمون الموثقون | المستخدمون الموثقون | إنشاء للموثق غير المحظور. تعديل مقيد بالصورة والطابع. |
| `/{document=**}` | مسار الرفض الشامل | لا أحد | لا أحد | `allow read, write: if false;` (حظر صريح لكافة المسارات غير المعرفة). |

---

### 3. مخططات المستندات وحلول التوافق المنفذة

#### أ. وثيقة المستخدم `/users/{uid}`
* **حقول ملخص النقاط المتوافقة (Reconciled Points Totals):**
  - `pointsBalance`: Long (الرصيد الفعلي الحالي)
  - `totalPointsEarned`: Long? = null (إجمالي النقاط المكتسبة - اسم لوحة الإدارة)
  - `pointsEarnedTotal`: Long? = null (إجمالي النقاط المكتسبة - اسم تطبيق المستخدم)
  - `totalPointsSpent`: Long? = null (إجمالي النقاط المنفقة - اسم لوحة الإدارة)
  - `pointsSpentTotal`: Long? = null (إجمالي النقاط المنفقة - اسم تطبيق المستخدم)
  - **حظر كتابة العميل المباشرة:** محظورة ومصنفة ضمن `modifyingSensitiveUserFields()`.
  - **آلية التوافق:** يتم تنفيذ **الكتابة المزدوجة الذرية (Atomic Dual-write)** لكلا الاسمين عند أي حركة نقاط في `AdminRepository.adjustUserPoints`.
  - **دلالة القراءة وتمييز الصفر المشين (Zero-Preserving Null-Fallback):**
    ```kotlin
    val effectivePointsEarned: Long get() = totalPointsEarned ?: pointsEarnedTotal ?: 0L
    val effectivePointsSpent: Long get() = totalPointsSpent ?: pointsSpentTotal ?: 0L
    ```
    إذا كان `totalPointsEarned = 0L`، يتم الاحتفاظ بالصفر ولا يتم استبداله بحقل قديم غير متزامن. وإذا كان الحقل غائباً (`null`)، يتراجع المنطق إلى `pointsEarnedTotal`.
* **حقول الاشتراك المعتمدة والتوافقية:**
  - `subscriptionTier`: String (`"FREE"`, `"PRO_LITE"`, `"PRO"`)
  - `planId`: String, `durationDays`: Int?, `subscriptionStatus`: String, `subscriptionSource`: String.
  - الحقول التوافقية القديمة: `isPremium`: Boolean, `isPro`: Boolean, `proExpiresAt`: Long?.

#### ب. طلبات ترقية CineStream Pro `/pro_requests/{requestId}`
* **المخطط الموحد:**
  - الحقول: `requestId`, `userId`, `planId` / `requestedPlan`, `durationDays` / `requestedDuration`, `paymentReference` / `paymentProofUrl`, `notes` / `userNote`, `status`, `cancelledAt`.
* **سياسة إلغاء الطلب من المستخدم في القواعد:**
  - مسموح للمستخدم صاحب الطلب بنقل حالته حصرياً من `PENDING` أو `pending` إلى `CANCELLED` المعيارية.
  - التعديل مقيد بالقواعد: `request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'updatedAt', 'cancelledAt'])`.
  - حظر تام على تعديل `userId` أو الخطة أو بيانات الدفع أو حقول المراجعة.
  - حظر الحذف على العميل نهائياً.
  - حظر إعادة فتح أي حالة نهائية (`APPROVED`, `REJECTED`, `CANCELLED`).
* **المعاملة الذرية للبت في الطلب للمشرف:**
  - الموافقة والرفض تنفذ حصرياً عبر `firestore.runTransaction` في `ProRequestRepository.kt`.

---

### 4. بصمات قواعد الأمان ونتائج الاختبارات المفصلة

* **بصمة SHA-256 الحالية لقواعد `firestore.rules`:** `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`
* **نتائج الفحوصات في بيئة Firebase Local Emulator Suite (Phase 22.6-B):**
  - `firestore-rules.test.js`: **114 فحصاً ناجحاً (0 فشل)**
  - `phase05b-comprehensive-rules.test.js`: **32 فحصاً ناجحاً (0 فشل)**
  - `phase03a1-rules.test.js`: **36 فحصاً ناجحاً (0 فشل)**
  - **الإجمالي:** **182 فحصاً أمنياً ناجحاً بنسبة 100% دون أي إخفاق**.
* **نتائج اختبارات كود Kotlin للأندرويد:**
  - 141 اختبار وحدة ناجحاً بنسبة 100% دون أي فشل.
  - بناء وتصريف التطبيق: `Build Succeeded`.
* **تصنيف حدود النشر السحابي:**
  - المشروع السحابي الموحد: **`UNVERIFIED`**.
  - مقارنة ملف القواعد الفعلي لتطبيق المستخدم: **`ALIGNED`** على المستوى التشغيلي، و**`UNVERIFIED`** على مستوى النشر السحابي المباشر.
