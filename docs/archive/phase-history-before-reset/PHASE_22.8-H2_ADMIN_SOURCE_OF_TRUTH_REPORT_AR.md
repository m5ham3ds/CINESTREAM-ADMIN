# تقرير التحقق النهائي من مصدر الحقيقة للوحة الإدارة — PHASE 22.8-H2
## Admin Source-of-Truth Verification Report: CineStream Admin Dashboard

**تاريخ التوثيق:** 18 أكتوبر 2026  
**المرحلة:** `PHASE 22.8-H2 — ADMIN SOURCE-OF-TRUTH VERIFICATION`  
**النطاق:** التحقق النهائي الحاسم والمباشر من ملفات المصدر الحقيقية للوحة الإدارة بالاستناد إلى تقارير F2 وG2 وF1 وG1، دون أي تعديل على الكود أو القواعد أو الحزم أو إعدادات Firebase.

---

### جدول ملخص نتائج مصدر الحقيقة (Source-of-Truth Summary)

| البند المفحوص | القيمة الفعلية من ملفات المصدر | الدليل المباشر (الملف والسطر) | الحالة والنتيجة |
|---|---|---|:---:|
| **معرف التطبيق في Gradle** | `com.aistudio.cinestreamadmin.eftzvl` | `app/build.gradle.kts` (السطر 14) | **DIVERGENCE** |
| **Project ID في Firebase** | `cinestream-sulo` | `app/google-services.json` (السطر 5) | **MATCH (100%)** |
| **Project Number في Firebase** | `979447256418` | `app/google-services.json` (السطر 3) | **MATCH (100%)** |
| **حزمة العميل 1 (تطبيق المستخدم)** | `com.aistudio.cinestream.xyzabc` | `app/google-services.json` (السطر 10) | **REGISTERED** |
| **حزمة العميل 2 (لوحة الإدارة)** | `com.aistudio.cinestreamadmin.adcxz` | `app/google-services.json` (السطر 46) | **REGISTERED** |
| **سبب فشل `processDebugGoogleServices`** | عدم وجود العميل `eftzvl` داخل `google-services.json` | مخرجات Gradle الحية عند استدعاء المهمة | **CONFIRMED** |
| **قيم حالات البلاغات المكتوبة فعلياً** | `RESOLVED` و `DISMISSED` (أحرف كبيرة) | `ReportsScreen.kt` (السطور 85-103) و `AdminRepository.kt` | **CANONICAL** |
| **حسم التعارض بين `DISMISSED` و `rejected`** | `DISMISSED` هو المعتمد للبلاغات؛ `REJECTED` خاص بطلبات Pro | `Models.kt` و `FIREBASE_CONTRACT.md` | **RESOLVED** |
| **نظام إدارة التعليقات في لوحة الإدارة** | **غير موجود نهائياً** (يوجد فقط راية `canComment` في حساب المستخدم) | مسح شاشات ومستودعات التطبيق و `firestore.rules` | **CONFIRMED VOID** |

---

## 1. الفحص الحرفي للهوية وإعدادات Firebase في الملفات المصدرية

تم فتح وفحص الملفين الحقيقيين وتسجيل القيم الحرفية بدقة متناهية:

### أ. ملف `app/build.gradle.kts`:
- **السطر 11:** `namespace = "com.example"`
- **السطر 14:** `applicationId = "com.aistudio.cinestreamadmin.eftzvl"`
- **السطور 12-16:**
  ```kotlin
  defaultConfig {
    applicationId = "com.aistudio.cinestreamadmin.eftzvl"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  ```

### ب. ملف `app/google-services.json`:
- **السطر 3:** `"project_number": "979447256418"`
- **السطر 4:** `"firebase_url": "https://cinestream-sulo-default-rtdb.firebaseio.com"`
- **السطر 5:** `"project_id": "cinestream-sulo"`
- **السطر 6:** `"storage_bucket": "cinestream-sulo.firebasestorage.app"`
- **العملاء المسجلون حرفياً في مصفوفة `client`:**
  1. **العميل رقم 1 (تطبيق المستخدم):**
     - **السطر 9:** `"mobilesdk_app_id": "1:979447256418:android:037a9b3a1393e550a9883b"`
     - **السطر 10:** `"package_name": "com.aistudio.cinestream.xyzabc"`
  2. **العميل رقم 2 (لوحة الإدارة):**
     - **السطر 45:** `"mobilesdk_app_id": "1:979447256418:android:ea5b570266ae1aa6a9883b"`
     - **السطر 46:** `"package_name": "com.aistudio.cinestreamadmin.adcxz"`
     - **السطر 53:** `"certificate_hash": "54a814fb04debd983f336ef82e87820edb760f12"`

---

## 2. التأكد الحاسم من سبب فشل مهمة `processDebugGoogleServices`

### أ. الفحص الحي لإعدادات المشروع الحالية (دون استثناء المهمة أو تجاوز الخطأ):
عند تنفيذ أي أمر بناء أو اختبار يستدعي دورة حياة Gradle القياسية مثل `./gradlew testDebugUnitTest` أو أداة `compile_applet` أو `lint_applet`، تتوقف العملية فوراً عند المهمة:
```text
> Task :app:processDebugGoogleServices FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:processDebugGoogleServices'.
> No matching client found for package name 'com.aistudio.cinestreamadmin.eftzvl' in /app/google-services.json
```

### ب. التفسير المعماري للسبب الجذري:
1. إضافة Google Services Gradle Plugin (`alias(libs.plugins.google.services)`) تقوم بمطابقة القيمة المعرفة في `android.defaultConfig.applicationId` مع قيم `client_info.android_client_info.package_name` المسجلة داخل `google-services.json`.
2. القيمة الحالية في الكود هي `"com.aistudio.cinestreamadmin.eftzvl"`.
3. القيمتان الوحيدتان المتاحتان في ملف `google-services.json` هما:
   - `"com.aistudio.cinestream.xyzabc"`
   - `"com.aistudio.cinestreamadmin.adcxz"`
4. نظراً لعدم وجود أي تطابق، تفشل المهمة تلقائياً وتمنع توليد موارد Firebase التلقائية (`values/values.xml`).
5. **دليل عدم وجود أي عطل في كود التطبيق نفسه:**
   - عند عزل هذه المهمة المحددة وتشغيل `./gradlew testDebugUnitTest -x :app:processDebugGoogleServices`، ينجح البناء كاملاً وتجتاز **كافة اختبارات وحدة Kotlin البالغ عددها 141 اختباراً بنسبة 100% دون أي خطأ**.
   - كما أن ملف `FirebaseInitializer.kt` (السطر 17) يحتوي مسبقاً على كود تراجع يعتمد App ID الخاص بالحزمة المسجلة:
     `FALLBACK_APP_ID = "1:979447256418:android:ea5b570266ae1aa6a9883b"`.

---

## 3. حسم تعارض حالات البلاغات (`/reports`) بين `DISMISSED` و `rejected` والأحرف الكبيرة والصغيرة

تم فتح كود إدارة البلاغات، والنموذج، والواجهة، وقواعد Firestore، والمستندات التعاقدية للوقوف على مصدر الحقيقة الفعلي:

### أ. ما يكتبه تطبيق لوحة الإدارة فعلياً في قاعدة البيانات:
1. **في واجهة المستخدم (`app/src/main/java/com/example/ui/screens/ReportsScreen.kt`):**
   - السطور 85-103 (نافذة معالجة البلاغ `AlertDialog`):
     ```kotlin
     FilterChip(
         selected = resolutionStatus == "RESOLVED",
         onClick = { resolutionStatus = "RESOLVED" },
         label = { Text("RESOLVED") },
         ...
     )
     FilterChip(
         selected = resolutionStatus == "DISMISSED",
         onClick = { resolutionStatus = "DISMISSED" },
         label = { Text("DISMISSED") },
         ...
     )
     ```
   - السطر 53: الحالة الافتراضية للمعالجة هي `"RESOLVED"`.
   - السطر 125: عند الضغط على تأكيد، يتم استدعاء:
     `viewModel.resolveReport(rId, resolutionStatus, resolutionNotes)`
2. **في المستودع (`app/src/main/java/com/example/repository/AdminRepository.kt`):**
   - السطور 1550-1558:
     ```kotlin
     suspend fun updateReportStatus(reportId: String, status: String, notes: String) {
         val currentAdmin = auth.currentUser
         val updates = mapOf(
             "status" to status,
             "resolutionNotes" to notes,
             "resolvedAt" to System.currentTimeMillis(),
             "resolvedBy" to (currentAdmin?.uid ?: "admin")
         )
         reportsCollection.document(reportId).set(updates, SetOptions.merge()).await()
     ```
   - يكتب التطبيق القيمة التي تم تمريرها من الواجهة وهي حصراً: `"RESOLVED"` أو `"DISMISSED"` (أحرف كبيرة).

### ب. نموذج البيانات المعتمد (`app/src/main/java/com/example/models/Models.kt`):
- السطور 382-396:
  ```kotlin
  @IgnoreExtraProperties
  data class UserReport(
      val id: String = "",
      val userId: String = "",
      val userEmail: String = "",
      val type: String = "ISSUE",
      val targetType: String = "",
      val targetId: String = "",
      val title: String = "",
      val description: String = "",
      val status: String = "PENDING",
      val resolutionNotes: String = "",
      val createdAt: Long = System.currentTimeMillis(),
      val resolvedAt: Long? = null,
      val resolvedBy: String = ""
  )
  ```
  - الحالة الافتراضية عند الإنشاء هي `"PENDING"` (أحرف كبيرة).

### ج. قواعد أمان Firestore (`firestore.rules`):
- السطور 553-575:
  ```javascript
  match /reports/{reportId} {
    allow read: if isAdmin() ||
      (isAuthenticated() && resource.data.userId == request.auth.uid);

    allow create: if isAdmin() || (
      isAuthenticated() &&
      request.resource.data.userId == request.auth.uid &&
      (
        request.resource.data.status == 'PENDING' ||
        request.resource.data.status == 'pending'
      ) &&
      (!('resolvedBy' in request.resource.data) ||
        request.resource.data.resolvedBy == '' ||
        request.resource.data.resolvedBy == null) &&
      (!('resolvedAt' in request.resource.data) ||
        request.resource.data.resolvedAt == null) &&
      (!('resolutionNotes' in request.resource.data) ||
        request.resource.data.resolutionNotes == '' ||
        request.resource.data.resolutionNotes == null)
    );

    allow update, delete: if isAdmin();
  }
  ```
  - عند إنشاء البلاغ من قِبل المستخدم العادي: تقبل القواعد كلاً من `'PENDING'` (أحرف كبيرة) و `'pending'` (أحرف صغيرة) لضمان التوافق التنازلي.
  - عند تحديث البلاغ من قِبل المشرف: يملك المشرف الصلاحية الكاملة لتحديث الحالة وكتابة الملاحظات وطابع الحل.

### د. حسم التعارضات:
1. **التعارض بين `"DISMISSED"` و `"rejected"`:**
   - **الحسم القاطع:** القيمة القانونية والمعتمدة في عقد البلاغات هي **`"DISMISSED"`** حصراً.
   - كلمة `"rejected"` أو `"REJECTED"` **لا توجد إطلاقاً في أي ملف يخص البلاغات** في لوحة الإدارة (لا في `Models.kt`، ولا `ReportsScreen.kt`، ولا `AdminRepository.kt`، ولا `FIREBASE_CONTRACT.md`).
   - حالة `"REJECTED"` تخص حصراً مسار طلبات الترقية `/pro_requests/{requestId}` (`PENDING -> APPROVED / REJECTED / CANCELLED`).
   - استخدام `"rejected"` للبلاغات هو انحراف دلالي غير معتمد ويجب تصحيحه في أي طرف يستخدمه إلى `"DISMISSED"`.
2. **التعارض بين الأحرف الكبيرة والأحرف الصغيرة:**
   - **الكتابة الإدارية:** تُكتب دائماً بأحرف كبيرة: `"PENDING"`, `"RESOLVED"`, `"DISMISSED"`.
   - **القراءة والتصفية:** يتعامل كود الإدارة بمرونة تامة مع الأحرف (Case-Insensitive):
     - `ReportsScreen.kt` السطر 174: `allReports.count { it.status.equals("PENDING", ignoreCase = true) }`.
     - `ReportsViewModel.kt` السطر 48: `list.filter { it.status.equals(filter, ignoreCase = true) }`.
   - **الإنشاء:** يدعم كلاً من `"PENDING"` و `"pending"`.
   - **الخلاصة:** المعيار الرسمي الموحد هو **الأحرف الكبيرة (UPPERCASE)**: `PENDING`, `RESOLVED`, `DISMISSED`.

---

## 4. التحقق الحاسم من حقيقة وجود واجهة أو نظام إدارة التعليقات

بناءً على الفحص الشامل لكامل شجرات الكود في `app/src/main/`:

### أ. فحص شاشات واجهة المستخدم (UI Screens):
- فحص مسار `app/src/main/java/com/example/ui/screens/`:
  - الشاشات المتاحة هي:
    `AdminProfileScreen`, `AppUpdatesScreen`, `AuditLogsScreen`, `ConfigScreen`, `DashboardOverviewContent`, `DashboardScreen`, `DiagnosticScreen`, `EconomyConsoleScreen`, `ExtensionsScreen`, `ExtensionsUpdatesScreen`, `GlobalConfigScreen`, `LoginScreen`, `ManagedExtensionsScreen`, `NotificationScreen`, `ProRequestsScreen`, `ReportsScreen`, `SearchOrderScreen`, `SettingsScreen`, `SupportChatScreen`, `SupportInboxScreen`, `UserDetailScreen`.
  - **النتيجة:** **لا توجد أي شاشة باسم `CommentsScreen` أو `CommentsModerationScreen`**.

### ب. فحص خريطة التنقل (`AppNavigation.kt`):
- السطور 44-68 (كائن `sealed class Screen`):
  - يتضمن الوجهات: `Login`, `Dashboard`, `AppUpdates`, `GlobalConfig`, `Notifications`, `ManagedExtensions`, `SearchOrder`, `ExtensionsUpdates`, `Settings`, `Profile`, `AuditLogs`, `Reports`, `SupportInbox`, `SupportChat`, `ProRequests`, `Diagnostics`, `Economy`, `UserDetail`.
  - **النتيجة:** **لا يوجد أي مسار تنقل أو زر أو تبويب للتعليقات في نظام التنقل بالكامل**.

### ج. فحص نماذج العرض والمستودعات (ViewModels & Repositories):
- في `app/src/main/java/com/example/viewmodels/`:
  - لا يوجد أي `CommentsViewModel`.
- في `app/src/main/java/com/example/repository/`:
  - لا يوجد أي `CommentRepository`.
  - في `AdminRepository.kt`: لا توجد أي دوال لجلب التعليقات، أو حذف تعليق، أو إخفاء تعليق، أو مراجعة التعليقات المبلغ عنها.

### د. حقيقة ما هو موجود فعلياً في لوحة الإدارة:
الشيء الوحيد المرتبط بكلمة "تعليق" في كامل مشروع لوحة الإدارة هو:
1. **راية صلاحية في وثيقة المستخدم:** `val canComment: Boolean = true` في `Models.kt` (السطر 151).
2. **زر تبديل صلاحية في واجهة تفاصيل المستخدم:**
   - في `UserDetailScreen.kt` (السطور 1216-1222):
     ```kotlin
     FeatureToggleItem(
         title = AppStrings.canComment(currentLang),
         description = AppStrings.canCommentDesc(currentLang),
         enabled = user.isCommentAllowed,
         isPendingSync = isFeaturePending("canComment", "comment"),
         onToggle = { onToggle("comment", user.isCommentAllowed) },
         tag = "feature_toggle_comment"
     )
     ```
3. **دالة تبديل الراية في المستودع:**
   - في `AdminRepository.kt` (السطر 943):
     `"comment" -> mapOf("canComment" to allowed, "updatedAt" to now)`
4. **حماية الراية في القواعد:**
   - في `firestore.rules` (السطر 99): منع المستخدم من تعديل `'canComment'` لنفسه.

### هـ. الاستنتاج الحاسم:
**لا يوجد أي نظام لإدارة التعليقات (No Comments Moderation System) في لوحة الإدارة على الإطلاق.**  
وجود راية `canComment` هو مجرد راية تحكم فني في حساب المستخدم (Feature Toggle) مثلها مثل `canWatch` و `canDownload` و `canP2P`. لا توجد واجهات عرض للتعليقات، ولا مستودعات بيانات، ولا قواعد أمان في Firestore لتخزين أو قراءة التعليقات.

---

## 5. الخلاصة النهائية لتقرير مصدر الحقيقة

1. **الهوية:** معرّفات Firebase مطابقة تماماً للمراجع الأصلية (`cinestream-sulo` / `979447256418`). سبب فشل Google Services هو حصر تسجيل لوحة الإدارة في `google-services.json` على `com.aistudio.cinestreamadmin.adcxz` بينما كود Gradle يحمل الاسم المؤقت `com.aistudio.cinestreamadmin.eftzvl`.
2. **البلاغات:** حالات البلاغات المكتوبة في لوحة الإدارة هي `"RESOLVED"` و `"DISMISSED"` حصراً (أحرف كبيرة). استخدام `"rejected"` غير صحيح ويخص طلبات Pro حصراً.
3. **التعليقات:** خلو تام للوحة الإدارة من أي واجهة أو عمليات لإدارة التعليقات؛ والموجود فقط هو راية صلاحية الحساب `canComment`.
4. **الالتزام الكامل:** لم يتم إجراء أي تعديل على أي ملف أو قاعدة، ولم يتم تغيير أسماء الحزم، ولم يتم تفعيل أي خدمات جديدة.
