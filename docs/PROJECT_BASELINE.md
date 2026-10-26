# وثيقة خط الأساس للمشروع — PROJECT_BASELINE.md
## CineStream Admin Dashboard: Verified Project Baseline & Source-of-Truth

**تاريخ التوثيق:** 19 أكتوبر 2026  
**النظام والبيئة:** Google AI Studio Build (Android Runtime)  
**حالة التوثيق والمراحل:** تم إجراء التصفير الآمن وأرشفة المراحل السابقة (01 إلى 22.13-B) في `/docs/archive/phase-history-before-reset/`، وتجهيز مساحة العمل لبدء التوثيق النشط من **PHASE 1**.  
**الغرض من الوثيقة:** توفير سجل حقيقي وموثق للحالة الفعلية لمشروع لوحة الإدارة (`CineStream Admin Dashboard`) بالاعتماد على الفحص المباشر للملفات الحالية في مساحة العمل، مع التمييز الواضح بين ما تم التحقق منه محلياً وما تم نقله من تقارير سابقة.

---

## 1. بنية المشروع والوحدات الأساسية (Project Structure & Modules)

المشروع عبارة عن تطبيق أندرويد متكامل مكتوب بلغة Kotlin مع واجهات حديثة مبنية بتقنية Jetpack Compose، ويتبع بنية معمارية طبقية (MVVM + Repository Pattern):

### أ. البنية البرمجية والمجلدات الرئيسية:
- `app/src/main/java/com/example/`:
  - `MainActivity.kt`: نقطة الدخول الرئيسية، تدير التنقل والحالات والسمات.
  - `MyApplication.kt`: فئة التطبيق العامة، تهيئ السجلات وتشخيصات البدء.
  - `contract/`: يحتوي على `FirebaseContract.kt` (العقد الكنسي الموحد لجميع مسارات ومجموعات Firestore).
  - `diagnostics/`: يحتوي على `FirebaseInitializer.kt`, `AppLogger.kt`, `CrashHandler.kt`, `CrashReportActivity.kt`.
  - `media/`: يحتوي على `CloudinaryManager.kt` و `ApkUploadHelper.kt`.
  - `models/`: يحتوي على النماذج الأساسية (`Models.kt`, `ProRequest.kt`, `ManagedExtension.kt`, `SearchOrderConfig.kt`, `SupportModels.kt`, `EconomyModels.kt`, `DashboardAnalytics.kt`).
  - `network/`: يحتوي على `NetworkMonitor.kt` و `ResilientSyncManager.kt`.
  - `repository/`: يحتوي على مستودعات البيانات المتصلة بـ Firestore:
    - `AdminRepository.kt` (المستودع الرئيسي للمستخدمين والمشرفين والإعدادات والتقارير والإشعارات وسجلات التدقيق).
    - `ProRequestRepository.kt` (إدارة واعتماد ورفض طلبات ترقية Pro).
    - `ManagedExtensionRepository.kt` (إدارة كتالوج الإضافات والمكاشط).
    - `SearchOrderRepository.kt` (إدارة ترتيب مصادر البحث والدبلجة).
    - `SupportRepository.kt` (إدارة محادثات وتذاكر الدعم الفني).
  - `state/`: يحتوي على `AppSettings.kt` و `AppStrings.kt` (دعم اللغتين العربية والإنجليزية).
  - `ui/`:
    - `components/`: مكونات Compose المخصصة (`CineStreamUI.kt`).
    - `navigation/`: مسارات التنقل (`AppNavigation.kt`).
    - `screens/`: شاشات لوحة الإدارة (21 شاشة متخصصة تغطي كافة العمليات الإدارية).
    - `theme/`: الألوان والسمات والخطوط (`Color.kt`, `Theme.kt`, `Type.kt`).
  - `validation/`: التحقق من صحة المدخلات (`ManagedExtensionValidator.kt`, `SearchOrderValidator.kt`).
  - `viewmodels/`: نماذج العرض للربط بين الواجهات والمستودعات (`ViewModels.kt`, `ReportsViewModel.kt`, `ProRequestsViewModel.kt`, `EconomyViewModel.kt`, `SearchOrderViewModel.kt`, `ManagedExtensionsViewModel.kt`, `SupportViewModel.kt`, `LoginViewModel.kt`).

### ب. وحدات الأمان واختبارات القواعد:
- `firestore.rules`: قواعد الأمان الموحدة لـ Cloud Firestore (810 أسطر، تغطي نموذج الصفر ثقة Zero-Trust).
- `firestore-tests/`: أجنحة الاختبارات الأمنية لمحاكي Firestore المحلي (تتضمن `firestore-rules.test.js`, `phase05b-comprehensive-rules.test.js`, `phase03a1-rules.test.js`).

---

## 2. هوية الحزم وإعدادات البناء (Package Identity & Gradle Build Configuration)

### أ. القيم المفحوصة فعلياً في ملف `app/build.gradle.kts`:
- **`namespace`:** `"com.example"` (المجال البرمجي لكود Kotlin والموارد).
- **`applicationId` الحالي في الملف:** `"com.aistudio.cinestreamadmin.mwtssd"` (معرّف البيئة الحالية المعاد توليدها Remixed Instance).
- **`compileSdk`:** 36 (minorApiLevel = 1).
- **`minSdk`:** 24.
- **`targetSdk`:** 36.
- **`versionCode` / `versionName`:** 1 / "1.0".
- **`JavaVersion`:** VERSION_11.
- **إضافات البناء النشطة:**
  - `alias(libs.plugins.android.application)`
  - `alias(libs.plugins.kotlin.compose)`
  - `alias(libs.plugins.google.devtools.ksp)`
  - `alias(libs.plugins.roborazzi)`
  - `alias(libs.plugins.secrets)`
  - `alias(libs.plugins.google.services)`

### ب. المقارنة مع الهوية الإنتاجية الموثقة في تقرير PHASE 22.13-B:
- **المعرّف المسجل للإنتاج (Production Registered App ID):** `com.aistudio.cinestreamadmin.adcxz`.
- **المعرّف الحالي في بيئة AI Studio Build الحالية:** `com.aistudio.cinestreamadmin.mwtssd`.
- **التشخيص والقرار:** تم رصد هذا الاختلاف بين النسخة المنسوخة (Remixed) والنسخة الإنتاجية السابقة بدقة تامة. وتطبيقاً لقواعد حماية المشروع الصارمة، **لم ولن يتم تغيير أي معرّف حزمة أو إعدادات Gradle ضمن هذه المهمة**.

---

## 3. هوية Firebase وإعداداتها الميدانية (Firebase Identity & Configuration)

### أ. ملف `app/google-services.json` الحالي:
- **`project_id`:** `"remixed-project-id"`
- **`project_number`:** `"1234567890"`
- **`package_name` المسجل:** `"com.aistudio.cinestreamadmin.mwtssd"`
- **`mobilesdk_app_id`:** `"1:1234567890:android:remixedappid"`

### ب. الثوابت الكنسية الإنتاجية المحفوظة في `FirebaseInitializer.kt`:
يحتوي ملف `app/src/main/java/com/example/diagnostics/FirebaseInitializer.kt` (السطور 17-21) على ثوابت التراجع الكنسية المعتمدة للمشروع الإنتاجي لضمان عدم توقف التطبيق في حال عدم مطابقة الموارد:
- **`FALLBACK_PROJECT_ID`:** `"cinestream-sulo"`
- **`FALLBACK_GCM_SENDER_ID` (Project Number):** `"979447256418"`
- **`FALLBACK_APP_ID` (Admin):** `"1:979447256418:android:ea5b570266ae1aa6a9883b"` (الخاص بالحزمة `com.aistudio.cinestreamadmin.adcxz`)
- **`FALLBACK_API_KEY`:** `"AIzaSyC9CvaM9Mw3NiD-KsOiYvHZHj6XZJQJnPs"`
- **`FALLBACK_STORAGE_BUCKET`:** `"cinestream-sulo.firebasestorage.app"`

### ج. بيانات تطبيق المستخدم (User App) الموثقة تاريخياً في تقرير PHASE 22.13-B:
- **اسم الحزمة المسجل لتطبيق المستخدم:** `com.aistudio.cinestream.xyzabc`
- **معرّف التطبيق (App ID) للمستخدم:** `1:979447256418:android:037a9b3a1393e550a9883b`

---

## 4. نموذج المصادقة والصلاحيات (Firebase Authentication & Authority Architecture)

### أ. صلاحية المالك (Bootstrap Project Owner):
- البريد الإلكتروني الكنسي: `sulopros01@gmail.com`
- يتم التحقق منه في القواعد عبر دالة `isOwnerAccount()`:
  - التحقق من المصادقة: `isAuthenticated()`
  - التحقق من توثيق البريد: `request.auth.token.email_verified == true`
  - التحقق الحرفي غير الحساس لحالة الأحرف من تطابق البريد مع `sulopros01@gmail.com`.
- في طبقة الكود (`AdminRepository.kt` السطور 227-250): يحصل المالك على رتبة `superadmin` فورياً ويتم إنشاء/مزامنة وثيقته في `/admins/{uid}` بصورة ذرية.

### ب. المشرفون المعتمدون (Administrators):
- المصدر الكنسي الوحيد لصلاحية الإشراف في Firestore هو وجود وثيقة في `/admins/{uid}` بحيث يكون `data.enabled == true`.
- تم إلغاء أي اعتماد أو تراجع إلى حقول `users.role` أو `users.isAdmin` منعاً لتصعيد الصلاحيات (Privilege Escalation Prevention).
- المشرفون العاديون لا يستطيعون ترقية أنفسهم أو تعديل وثائق المشرفين الآخرين؛ إنشاء وتعديل وحذف مستندات `/admins` محصور بالمالك فقط.

---

## 5. جرد مسارات Firestore ومجموعاتها الكنسية (Firestore Collections Inventory)

| المسار / المجموعة | طبيعة الاستخدام في لوحة الإدارة | سياسة القراءة في القواعد | سياسة الكتابة في القواعد | الحالة الكنسية |
|---|---|---|---|:---:|
| `/admins/{uid}` | سجلات المشرفين المعتمدين وحالات التفعيل | المشرف وصاحب الحساب | المالك حصراً (`isOwnerAccount()`) | **CANONICAL ACTIVE** |
| `/users/{uid}` | حسابات المستخدمين، الصلاحيات، الحظر، والاشتراك | المشرف وصاحب الحساب | المشرف (كامل) / العميل (الاسم والصورة فقط) | **CANONICAL ACTIVE** |
| `/users/{uid}/point_transactions/{txId}` | دفتر حركات النقاط غير القابل للتعديل | المشرف وصاحب الحساب | المشرف حصراً (حظر العميل تماماً) | **CANONICAL ACTIVE** |
| `/users/{uid}/task_claims/{taskId}` | مطالبات المهام وسجل منع تكرار المكافأة | المشرف وصاحب الحساب | المشرف حصراً (حظر العميل تماماً) | **CANONICAL ACTIVE** |
| `/config/app` | تكوين النظام العام، الصيانة، وتحديثات OTA | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/config/search_order` | ترتيب مصادر البحث للمحتوى وحالات الدبلجة | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/config/features` | رايات التحكم بالميزات البرمجية للنظام | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/config/economy` | معايير اقتصاد النقاط وتسعير الاسترداد | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/managed_extensions/{id}` | كتالوج الإضافات والمكاشط المدارة (بيانات فقط) | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/extensions/{id}` | مسار المكاشط القديم (محفوظ للتوافق التنازلي) | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **LEGACY COMPATIBLE** |
| `/app_updates/{id}` | سجلات تحديثات التطبيق وتوزيع ملفات APK | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/notifications/{id}` | إعلانات النظام والإشعارات الموجهة | الموثقون وفق سياسة الجمهور (`target`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/auditLogs/{id}` | سجل التدقيق والرقابة الإدارية على العمليات | المشرف حصراً (`isAdmin()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/reports/{id}` | بلاغات الأعطال ومشاكل المشاهدة من المستخدمين | المشرف وصاحب البلاغ | إنشاء: العميل (`PENDING`) / معالجة: المشرف | **CANONICAL ACTIVE** |
| `/support_conversations/{id}` | تذاكر ومحادثات الدعم الفني بين العميل والإدارة | المشرف وصاحب التذكرة | إنشاء: العميل (`open`, `pending`) / ردود: الطرفان | **CANONICAL ACTIVE** |
| `/pro_requests/{id}` | طلبات الترقية لباقة Pro وإثباتات الدفع | المشرف وصاحب الطلب | إنشاء وإلغاء: العميل / بت واعتماد: المشرف | **CANONICAL ACTIVE** |
| `/reward_tasks/{id}` | كتالوج المهام اليومية والمكافآت | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/leaderboard/weekly_current` | الترتيب الأسبوعي النشط للمستخدمين الأكثر نقاطاً | جميع الموثقين (`isAuthenticated()`) | المشرف حصراً (`isAdmin()`) | **CANONICAL ACTIVE** |
| `/comments/{id}` | مسار التعليقات على الوسائط والأفلام | **محظور كلياً** (Default Deny) | **محظور كلياً** (Default Deny) | **DEFERRED & BLOCKED** |
| `/config/global` | مسار التكوين القديم الملغى | **محظور ومرفوض صراحة** | **محظور ومرفوض صراحة** | **DEPRECATED & DENIED** |
| `/{document=**}` | أي مسارات غير مصرح بها صراحة | **محظورة كلياً** (`allow read, write: if false;`) | **محظورة كلياً** (`allow read, write: if false;`) | **FAIL-CLOSED SAFEGUARD** |

---

## 6. نماذج البيانات والعقود البرمجية المعتمدة (Data Models & Contracts)

1. **نموذج المستخدم (`User` في `Models.kt`):**
   - يدعم حقول الاشتراك الكنسية: `subscriptionTier` (`"FREE"`, `"PRO_LITE"`, `"PRO"`), `planId`, `durationDays`, `subscriptionStatus`, `subscriptionSource`, `subscriptionReferenceId`, `subscriptionStartedAt`, `subscriptionExpiresAt`.
   - يدعم حقول التوافق التنازلي: `isPremium`, `isPro`, `plan`, `proPlan`, `proExpiresAt`.
   - يدعم الحقول المزدوجة لاقتصاد النقاط: `totalPointsEarned` / `pointsEarnedTotal` و `totalPointsSpent` / `pointsSpentTotal`.
   - يحافظ على مبدأ استقلالية الاشتراك: مزايا الاشتراك محصورة بإزالة الإعلانات فقط (`REMOVE_ADS ONLY`). لا يتم منح الصلاحيات التقنية أو رفع الحظر عبر الاشتراك.
2. **نموذج طلبات الترقية (`ProRequest` في `ProRequest.kt`):**
   - الحالات المدعومة في آلة الحالات: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.
   - التوافق التام مع حقول تطبيق المستخدم: `planId`, `durationDays`, `paymentReference`, `notes`, `cancelledAt`.
3. **نموذج البلاغات (`UserReport` في `Models.kt`):**
   - الحالات المعتمدة: `PENDING`, `RESOLVED`, `DISMISSED`.
4. **نموذج الإضافات والمكاشط (`ManagedExtension` في `ManagedExtension.kt`):**
   - الحالات: `ACTIVE`, `MAINTENANCE`, `DISABLED`, `DEPRECATED`.
   - يحتوي على بيانات تعريفية فقط (Zero Bytecode / Pure Config).
5. **نموذج ترتيب البحث ودعم الدبلجة (`SearchOrderConfig` في `SearchOrderConfig.kt`):**
   - الفئات: `movie`, `tv` (مع اسم التوافق `series`), `anime`, `arabicDubbed` (مع الأسماء التوافقية `arabicDub`, `arabic_dub`), `englishDubbed` (مع الأسماء التوافقية `englishDub`, `english_dub`).
   - حالة مجموعات الدبلجة (`DubbingConfig`): `arabicDubbedStatus` (الافتراضي `ACTIVE`), `englishDubbedStatus` (الافتراضي `MAINTENANCE`).

---

## 7. حالة الاختبارات ونتائج التحقق الفعلي (Testing & Verification Status)

استناداً إلى الفحص الفعلي للأكواد والتقارير الموثقة في Phase 22.12-B و Phase 22.13-B:

### أ. اختبارات وحدة أندرويد لكود Kotlin (Unit Tests):
- **المجموع الإجمالي:** **142 اختبار وحدة** في مسار `app/src/test/java/com/example/`.
- **التوزيع حسب الفئات:**
  1. `CineStreamAdminLogicTest`: 106 اختبارات (تغطي منطق الإشراف، الاشتراكات، حظر الميزات، وتأجيل التعليقات 22.10-B).
  2. `SearchOrderArchitectureTest`: 32 اختباراً (تغطي سلامة ترتيب البحث وتوافق مجموعات الدبلجة).
  3. `ExampleRobolectricTest`: اختباران.
  4. `ExampleUnitTest`: اختبار واحد.
  5. `FirebaseTest`: اختبار واحد.
- **آخر نتيجة مسجلة موثقة:** **142/142 PASS (100% نجاح كامل)**.

### ب. اختبارات محاكي قواعد Firestore المحلي (Firestore Rules Emulation):
- **المجموع الإجمالي:** **182 فحصاً أمنياً شاملاً** في مجلد `firestore-tests/`.
- **التوزيع:**
  1. جناح Mocha (`firestore-rules.test.js`): 114 فحصاً ناجحاً (تغطي عزل المالك، حمولة إلغاء Pro، حظر الحقول الحساسة، وقواعد الإشراف).
  2. جناح Node Native Test (`phase05b` و `phase03a1`): 68 فحصاً ناجحاً (تغطي حظر المسارات المهملة والرفض الشامل واقتصاد النقاط).
- **آخر نتيجة مسجلة موثقة:** **182/182 PASS (100% نجاح كامل)** على محاكي Firestore المحلي (`cloud-firestore-emulator-v1.22.0.jar`).

---

## 8. حالة ميزة التعليقات والقيود الأمنية (Comments Feature Status)

- **القرار المعماري المعتمد (PHASE 22.10-B):** ميزة التعليقات **مؤجلة بالكامل وليست ميزة نشطة**.
- **الواجهة:** تم حذف عنصر التحكم بصلاحية التعليق `PermissionToggleRow` من شاشة تفاصيل المستخدم `UserDetailScreen.kt`.
- **المستودع وViewModel:** تم حظر أي محاولة كتابة إدارية تستهدف تعديل `canComment` في `AdminRepository.kt` و `UserDetailViewModel`.
- **النموذج:** تم الإبقاء على حقل `canComment: Boolean = true` في `Models.kt` حصرياً للحفاظ على التوافق المخططي التنازلي مع الوثائق القديمة.
- **القواعد:** مسار `/comments` غير معرّف ويخضع لحظر الرفض الشامل التلقائي (`match /{document=**} { allow read, write: if false; }`).

---

## 9. القيود والمحددات المعمارية الصارمة (Strict Constraints & Guardrails)

1. **عدم النشر إلى Production:** يُمنع تماماً تشغيل أمر `deploy_firebase` أو نشر أي قواعد لبيئة السحابة الحية ضمن هذه المهمة.
2. **حظر ترحيل Cloudflare Workers:** تظل Firebase Firestore & Auth هي البنية الخلفية المعتمدة، ولا يوجد أي تصريح بالترحيل إلى Cloudflare Workers أو أي واجهة خارجية.
3. **تجميد الكود والإعدادات:** يُمنع تعديل أي ملف في كود التطبيق (Kotlin, Java, XML, Gradle) أو إعدادات Firebase (`google-services.json`).
