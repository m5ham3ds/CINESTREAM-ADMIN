# PHASE 20.1-B3-B — CINESTREAM ADMIN DASHBOARD FIRESTORE COMPATIBILITY AUDIT REPORT
# تقرير التدقيق الجنائي لتوافق لوحة تحكم الإدارة مع قواعد بيانات FIRESTORE المحصّنة

---

## 1. الملخص التنفيذي (Executive Summary)

* **معرّف المرحلة:** `PHASE 20.1-B3-B`
* **عنوان المرحلة:** CineStream Admin Dashboard Firestore Compatibility Audit
* **المشروع الخاضع للتدقيق:** CineStream Admin App (Management Plane) — تطبيق إدارة نظام CineStream
* **حزمة التطبيق (Package / Namespace):** `com.example` / `com.aistudio.cinestreamadmin.qzxkno`
* **طبيعة المهمة:** تدقيق تقني جنائي تحليلي للقراءة فقط (Read-Only Forensic Audit)
* **تاريخ التنفيذ:** 15 أكتوبر 2026
* **حالة تعديل الملفات:** **ممنوع التعديل — لم يتم إجراء أي تعديل على الشيفرة المصدرية أو القواعد (Zero Code Changes)**
* **الحكم الجنائي العام (Overall Verdict):** **PASS WITH SPECIFIC ARCHITECTURAL LIMITATIONS & BLOCKED EXTERNAL COMPARISONS** (اجتياز مقيّد بمحددات معمارية وتوقف المقارنة الخارجية لغياب مراجعها)

### خلاصة النتائج الجنائية:
1. **مصدر صلاحية الإدارة (Authoritative Gate):** تم التحقق قطعيًا من أن صلاحية الإدارة في شيفرة التطبيق (`AdminRepository.kt` و `LoginViewModel.kt`) وفي قواعد الأمان (`firestore.rules`) تستند حصريًا إلى المستند الرقابي `/admins/{uid}` بشرط `enabled == true`، بالإضافة إلى اعتماد البريد الإلكتروني المرجعي لمالك المشروع (`sulopros01@gmail.com`). تم استئصال الاعتماد القديم على حقل `/users/{uid}.role` أو أعلام `isAdmin` المحلية من مسار منح الصلاحيات كليًا.
2. **توافق مسارات وقواعد Firestore:** تتطابق عمليات لوحة الإدارة مع 23 مسارًا مصرّحًا بها في القواعد. لوحة الإدارة تكتب مباشرة إلى Firestore باستخدام Firebase Android Client SDK بموجب صلاحية `isAdmin()` المحققة في القواعد.
3. **سلامة الاقتصاد والنقاط (Points & Economy):** عمليات إصدار وتعديل النقاط تنفذ ذريًا عبر `firestore.runTransaction`، حيث تُحدّث أرصدة المستخدم الملخصة في `/users/{uid}` بالتزامن مع تدوين سجل مالي غير قابل للتعديل أو الحذف (Immutable Ledger) في السلسلة الفرعية `/users/{uid}/point_transactions/{txId}`. تفرض الشيفرة فحص عدم النزول تحت الصفر (Non-negative) وسقفًا أقصى للرصيد (1,000,000 نقطة).
4. **الترقيات والاشتراكات (Subscriptions & Pro Requests):** تعتمد الإدارة نموذج الكتابة المزدوجة المتوافق مع التطبيقات القديمة والحديثة (Canonical Dual-Write)، حيث تُسجّل الحقول القياسية بأحرف كبيرة (`subscriptionTier: "PRO"`, `subscriptionStatus: "ACTIVE"`) مع نظيراتها التاريخية (`isPremium: true`, `isPro: true`, `plan: "pro"`). تم رصد **ثغرة تدفق معمارية** في المعالجة الاحتياطية للأخطاء (Catch-block fallback) في `ProRequestRepository.kt`؛ إذ في حال فشل المعاملة الذرية دون اتصال، يُحدّث الطلب إلى `APPROVED` دون ترقية مستند المستخدم.
5. **نظام المحادثات والدعم الفني (Support System):** يستخدم مسار `/support_conversations/{conversationId}` والرسائل في `/messages/{messageId}` مع إسناد `senderRole: "admin"`. العمليات متوافقة 100% مع قواعد الأمان.
6. **حالة المراجع الخارجية المقارنة:** تم تسجيل المقارنة المباشرة مع تقرير تطبيق المستخدم `PHASE_20.1-B3-A_USER_APP_FIRESTORE_COMPATIBILITY_AUDIT_REPORT.md` ونسخة القواعد الخارجية B2 باعتبارها **BLOCKED** لعدم إرفاق الملفين في جلسة العمل الحالية، مع إجراء التدقيق الكامل على القواعد النشطة محليًا في المستودع (`firestore.rules` بحجم 376 سطرًا).

---

## 2. الملفات التي فُحصت فعليًا (Files Actually Audited)

تم فحص شجرة ملفات CineStream Admin Dashboard بالكامل داخل مساحة العمل الحالية، وشمل الفحص التفصيلي الملفات التالية:

### أ. ملفات القواعد والعقود المشتركة (Rules & Contracts):
* `/firestore.rules` (376 سطرًا — محرك الأمان وقواعد الوصول لكافة المجموعات)
* `/docs/FIREBASE_CONTRACT.md` (203 أسطر — العقد المشترك الموحد لمنظومة CineStream)
* `/docs/FIREBASE_CONTRACT_V1.md` (388 سطرًا — العقد المعياري v1.1.0 للتحكم وإدارة الحسابات)
* `/app/src/main/java/com/example/contract/FirebaseContract.kt` (210 أسطر — معرّفات المجموعات والمسارات والوثائق الموحدة)

### ب. طبقة المستودعات والوصول للبيانات (Repositories):
* `/app/src/main/java/com/example/repository/AdminRepository.kt` (1,499 سطرًا — العمليات الإدارية الأساسية للمستخدمين، الصلاحيات، الإعدادات، النقاط، الإعلانات، وسجلات التدقيق)
* `/app/src/main/java/com/example/repository/ProRequestRepository.kt` (298 سطرًا — إدارة واعتماد ورفض طلبات ترقية الاشتراكات)
* `/app/src/main/java/com/example/repository/SupportRepository.kt` (267 سطرًا — إدارة تذاكر الدعم والرسائل وتغيير الحالات)
* `/app/src/main/java/com/example/repository/SearchOrderRepository.kt` (174 سطرًا — ترتيب مزودي البحث عبر أنواع المحتوى والدبلجة)
* `/app/src/main/java/com/example/repository/ManagedExtensionRepository.kt` (539 سطرًا — إدارة كتالوج أدوات الكشط Scrapers والكتابة المزدوجة)

### ج. نماذج البيانات (Domain Models):
* `/app/src/main/java/com/example/models/Models.kt` (389 سطرًا — كيانات المستخدم، الاشتراكات، الصلاحيات، آلة حالات الحظر)
* `/app/src/main/java/com/example/models/EconomyModels.kt` (267 سطرًا — حركات النقاط، المهام، لوحة المتصدرين، إعدادات الاقتصاد)
* `/app/src/main/java/com/example/models/ProRequest.kt` (68 سطرًا — حالات ووثائق طلبات الترقية)
* `/app/src/main/java/com/example/models/SupportModels.kt` (76 سطرًا — محادثات الدعم والرسائل والحالات)
* `/app/src/main/java/com/example/models/SearchOrderConfig.kt` (246 سطرًا — تكوين أولويات البحث وإعدادات الدبلجة)
* `/app/src/main/java/com/example/models/ManagedExtension.kt` (نماذج الإضافات والقدرات البرمجية)

### د. نماذج العرض وإدارة الحالة (ViewModels):
* `/app/src/main/java/com/example/viewmodels/LoginViewModel.kt` (267 سطرًا — المصادقة والتحقق من رتبة المسؤول وتخزين الجلسة)
* `/app/src/main/java/com/example/viewmodels/ViewModels.kt` (1,024 سطرًا — Dashboard, UserDetail, Config, Notification, AuditLogs, Extensions)
* `/app/src/main/java/com/example/viewmodels/EconomyViewModel.kt` (440 سطرًا — وحدة التحكم بالاقتصاد والمهام والمتصدرين)
* `/app/src/main/java/com/example/viewmodels/ProRequestsViewModel.kt` (169 سطرًا — مراقبة وفرز وتنفيذ قرارات طلبات الترقية)
* `/app/src/main/java/com/example/viewmodels/SupportViewModel.kt` (187 سطرًا — تصفية وعرض وإرسال رسائل الدعم)
* `/app/src/main/java/com/example/viewmodels/ReportsViewModel.kt` (91 سطرًا — استعراض وبحث وحل بلاغات المشاكل)
* `/app/src/main/java/com/example/viewmodels/SearchOrderViewModel.kt` (206 أسطر — تعديل وحفظ أولويات الكشط)
* `/app/src/main/java/com/example/viewmodels/ManagedExtensionsViewModel.kt` (186 سطرًا — إدارة الإضافات والتحقق من الروابط)

### هـ. شاشات واجهة المستخدم (UI Screens):
* `/app/src/main/java/com/example/ui/screens/UserDetailScreen.kt` (2,049 سطرًا)
* `/app/src/main/java/com/example/ui/screens/EconomyConsoleScreen.kt` (2,233 سطرًا)
* `/app/src/main/java/com/example/ui/screens/ManagedExtensionsScreen.kt` (1,475 سطرًا)
* `/app/src/main/java/com/example/ui/screens/SearchOrderScreen.kt` (895 سطرًا)
* `/app/src/main/java/com/example/ui/screens/AppUpdatesScreen.kt` (742 سطرًا)
* `/app/src/main/java/com/example/ui/screens/GlobalConfigScreen.kt` (742 سطرًا)
* `/app/src/main/java/com/example/ui/screens/NotificationScreen.kt` (515 سطرًا)
* `/app/src/main/java/com/example/ui/screens/ConfigScreen.kt` (652 سطرًا)
* `/app/src/main/java/com/example/ui/screens/ExtensionsScreen.kt` (358 سطرًا)

### و. أدوات التهيئة والتحقق والتشخيص (Diagnostics & Validation):
* `/app/src/main/java/com/example/diagnostics/FirebaseInitializer.kt` (134 سطرًا)
* `/app/src/main/java/com/example/validation/SearchOrderValidator.kt` (207 أسطر)
* `/app/src/main/java/com/example/validation/ManagedExtensionValidator.kt` (226 سطرًا)
* `/scripts/static_security_scan.py` (139 سطرًا — الفحص الأمني الثابت للقواعد والشيفرة)
* `/app/build.gradle.kts` و `/build.gradle.kts` (إعدادات البناء والتوقيع والمكتبات)

---

## 3. مصادر قواعد B2 المتاحة والملفات المفقودة (Available B2 Sources vs Missing References)

في ضوء التوجيهات الإلزامية الصارمة للمرحلة، تم فحص بيئة الجلسة للتحقق من المراجع المتاحة والمفقودة:

| المرجع المطلـوب | الحالة الفعلية في الجلسة | الدليل التقني | أثر الغياب على التدقيق |
|---|---|---|---|
| **نسخة قواعد Firestore النشطة بالمستودع** | **متاحة (CONFIRMED)** | الملف `/firestore.rules` موجود (376 سطرًا، 16,349 بايت) | تم التدقيق الجنائي الكامل على أساس هذا الملف المعتمد في مرحلة Phase 05B. |
| **العقد الموحد لمنظومة Firebase** | **متاحة (CONFIRMED)** | الملفان `/docs/FIREBASE_CONTRACT.md` و `/docs/FIREBASE_CONTRACT_V1.md` | شكّلا المرجع الأساسي لمقارنة الحقول والأنواع والمسارات. |
| **تقرير تدقيق تطبيق المستخدم B3-A** (`PHASE_20.1-B3-A_..._REPORT.md`) | **غير متوفر (BLOCKED)** | فحص البحث بأمر `find . -name "*B3*"` لم يعثر على أي ملف مطابق في مساحة العمل. | **المقارنة مع شيفرة أو تقرير User App مسجلة كـ BLOCKED** ولا يجوز التكهن بمخرجاته. |
| **نسخة قواعد B2 الخارجية المستقلة** | **غير متوفرة كملف منفصل (BLOCKED)** | لا يوجد ملف مسمّى `B2_firestore.rules` أو شبيهه. | تم تسجيل المقارنة مع نسخة خارجية كـ **BLOCKED**، بينما تم اعتماد `/firestore.rules` المحلي كقاعدة مرجعية معلنة. |
| **ملف إعدادات Firebase** (`google-services.json`) | **غير متوفر (CONFIRMED MISSING)** | غير موجود في `/app/` ويتم تجاوزه بإعداد `MissingGoogleServicesStrategy.WARN` | الاعتماد على معرّفات احتياطية في `FirebaseInitializer.kt`. |
| **محاكي Firestore المحلي (Local Emulator)** | **غير نشط (BLOCKED)** | الاتصال بالمنفذ `http://127.0.0.1:8085` يعيد خطأ رفض اتصال (Code 7). | تعذر تشغيل اختبارات التكامل الحي في `firestore-tests/`. |

---

## 4. جرد مسارات Firestore وعمليات الإدارة (Firestore Operations Inventory)

يوضح الجدول التالي كافة مسارات Firestore المستخدمة في لوحة الإدارة، وطبيعة العملية، والملفات المنفذة، ونوع العميل (Client SDK vs Backend)، وتوافقها مع قواعد الأمان:

| # | مسار Firestore الفعلي | العمليات (CRUD) | الملف المنفّذ والدالة | نوع التنفيذ | توافق القواعد | تصنيف الدليل |
|---|---|---|---|---|---|---|
| 1 | `/admins/{adminUid}` | Read | `AdminRepository.checkIsAdmin()` | Client SDK | مسموح (`isAdmin() \|\| uid == adminUid`) | **CONFIRMED** |
| 2 | `/admins/{adminUid}` | Write (Set) | `AdminRepository.checkIsAdmin()` (Owner bootstrap) & `setUserAdminRole()` | Client SDK | مسموح (`isAdmin() \|\| owner email`) | **CONFIRMED** |
| 3 | `/admins/{adminUid}` | Delete | `AdminRepository.deleteUser()` | Client SDK | مسموح (`isAdmin()`) | **CONFIRMED** |
| 4 | `/users/{userId}` | Read (Stream) | `AdminRepository.getAllUsers()`, `getUser()` | Client SDK | مسموح للمسؤول (`allow read: if isAdmin()`) | **CONFIRMED** |
| 5 | `/users/{userId}` | Create | `AdminRepository.createUser()` | Client SDK | مسموح للمسؤول (`allow create: if isAdmin()`) | **CONFIRMED** |
| 6 | `/users/{userId}` | Update (Merge) | `AdminRepository.resilientSetUser()`, `updateSubscription()`, `banAccount()`, `updateFeaturePermission()` | Client SDK | مسموح للمسؤول بتعديل كافة الحقول الحساسة | **CONFIRMED** |
| 7 | `/users/{userId}` | Delete | `AdminRepository.deleteUser()` | Client SDK | مسموح حصريًا للمسؤول (`allow delete: if isAdmin()`) | **CONFIRMED** |
| 8 | `/users/{userId}/point_transactions/{txId}` | Read (Stream) | `AdminRepository.getUserPointTransactions()` | Client SDK | مسموح للمسؤول وصاحب الحساب | **CONFIRMED** |
| 9 | `/users/{userId}/point_transactions/{txId}` | Create (Append) | `AdminRepository.adjustUserPoints()` داخل Transaction | Client SDK | مسموح حصريًا للمسؤول (`allow write: if isAdmin()`) | **CONFIRMED** |
| 10 | `/config/app` | Read (Stream) | `AdminRepository.getAppConfig()` | Client SDK | مسموح لأي مستخدم موثق | **CONFIRMED** |
| 11 | `/config/app` | Write (Set) | `AdminRepository.updateAppConfig()` | Client SDK | مسموح حصريًا للمسؤول (`allow write: if isAdmin()`) | **CONFIRMED** |
| 12 | `/config/global` (Legacy Fallback) | Read (Get) | `AdminRepository.getAppConfig()` (سطور 1040-1049) | Client SDK | **مرفوض بالقواعد (DENIED)** عبر `match /config/{document=**}` | **CONFIRMED RISK** |
| 13 | `/config/search_order` | Read (Stream) | `SearchOrderRepository.getSearchOrder()` | Client SDK | مسموح لجميع الموثقين | **CONFIRMED** |
| 14 | `/config/search_order` | Write (Set) | `SearchOrderRepository.saveSearchOrder()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 15 | `/config/features` | Read (Stream) | `AdminRepository.getFeatureControlConfig()` | Client SDK | مسموح لجميع الموثقين | **CONFIRMED** |
| 16 | `/config/features` | Write (Set) | `AdminRepository.updateFeatureControl()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 17 | `/config/economy` | Read (Stream) | `AdminRepository.getEconomyConfig()` | Client SDK | مسموح لجميع الموثقين | **CONFIRMED** |
| 18 | `/config/economy` | Write (Set) | `AdminRepository.saveEconomyConfig()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 19 | `/reward_tasks/{taskId}` | Read (Stream) | `AdminRepository.getAllRewardTasks()` | Client SDK | مسموح لجميع الموثقين | **CONFIRMED** |
| 20 | `/reward_tasks/{taskId}` | Write & Delete | `AdminRepository.saveRewardTask()`, `deleteRewardTask()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 21 | `/leaderboard/weekly_current` | Read (Stream) | `AdminRepository.getWeeklyLeaderboard()` | Client SDK | مسموح لجميع الموثقين (قراءة فقط بالإدارة) | **CONFIRMED** |
| 22 | `/managed_extensions/{extensionId}` | Read (Stream) | `ManagedExtensionRepository.getAllManagedExtensions()` | Client SDK | مسموح لجميع الموثقين | **CONFIRMED** |
| 23 | `/managed_extensions/{extensionId}` | Write & Delete | `ManagedExtensionRepository.saveManagedExtension()`, `deleteExtension()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 24 | `/extensions/{extensionId}` (Legacy) | Read & Write | `ManagedExtensionRepository` & `AdminRepository` | Client SDK | مسموح لجميع الموثقين قراءة، والإدارة كتابة | **CONFIRMED** |
| 25 | `/app_updates/{updateId}` | Read (Stream) | `AdminRepository.getAppUpdates()` | Client SDK | مسموح لجميع الموثقين | **CONFIRMED** |
| 26 | `/app_updates/{updateId}` | Write & Delete | `AdminRepository.createOrUpdateAppUpdate()`, `deleteAppUpdate()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 27 | `/notifications/{notificationId}` | Read (Stream) | `AdminRepository.getRecentNotifications()` | Client SDK | مسموح لجميع الموثقين | **CONFIRMED** |
| 28 | `/notifications/{notificationId}` | Write & Delete | `AdminRepository.sendNotification()`, `deleteNotification()` | Client SDK | مسموح حصريًا للمسؤول (سجل إعلانات داخل التطبيق) | **CONFIRMED** |
| 29 | `/auditLogs/{logId}` | Read (Stream) | `AdminRepository.getAuditLogs()` | Client SDK | مسموح حصريًا للمسؤول (قراءة وكتابة إلحاقية) | **CONFIRMED** |
| 30 | `/auditLogs/{logId}` | Create (Append) | `AdminRepository.logAudit()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 31 | `/reports/{reportId}` | Read (Stream) | `AdminRepository.getReports()` | Client SDK | مسموح للمسؤول قراءة كافة البلاغات | **CONFIRMED** |
| 32 | `/reports/{reportId}` | Update & Delete | `AdminRepository.updateReportStatus()`, `deleteReport()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 33 | `/support_conversations/{id}` | Read (Stream) | `SupportRepository.getAllConversations()` | Client SDK | مسموح للمسؤول وصاحب التذكرة | **CONFIRMED** |
| 34 | `/support_conversations/{id}` | Update (Batch) | `SupportRepository.sendAdminReply()`, `updateStatus()` | Client SDK | مسموح للمسؤول | **CONFIRMED** |
| 35 | `/support_conversations/{id}` | Delete | `SupportRepository.deleteConversation()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 36 | `.../messages/{messageId}` | Read & Create | `SupportRepository.getMessages()`, `sendAdminReply()` | Client SDK | مسموح للمسؤول مع تعيين `senderRole: "admin"` | **CONFIRMED** |
| 37 | `/pro_requests/{requestId}` | Read (Stream) | `ProRequestRepository.getAllProRequests()` | Client SDK | مسموح للمسؤول وصاحب الطلب | **CONFIRMED** |
| 38 | `/pro_requests/{requestId}` | Update (Tx) | `ProRequestRepository.approveRequest()`, `rejectRequest()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |
| 39 | `/pro_requests/{requestId}` | Delete | `ProRequestRepository.deleteRequest()` | Client SDK | مسموح حصريًا للمسؤول | **CONFIRMED** |

---

## 5. نتائج التحقق من صلاحيات المسؤول (Admin Authentication & Authorization)

### أ. آلية التحقق ومصدر الصلاحية:
* **فحص القواعد (Server-Side Rules Engine):**
  تتحقق القواعد في السطور 16–22 من ملف `firestore.rules` من هوية المسؤول وفق الدالة التالية:
  ```javascript
  function isAdmin() {
    return isAuthenticated() && (
      (request.auth.token.email != null && request.auth.token.email.matches('(?i)sulopros01@gmail.com')) ||
      (exists(/databases/$(database)/documents/admins/$(request.auth.uid)) &&
       get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.enabled == true)
    );
  }
  ```
* **فحص شيفرة الإدارة (Client-Side Implementation):**
  في `AdminRepository.kt` (السطور 223–278):
  1. التحقق من البريد المرجعي لمالك المشروع (`sulopros01@gmail.com`). وفي حال تطابقه، يُمنح وصول الإدارة وتُنشأ وثيقة المالك في `/admins/{uid}` بصلاحية `superadmin` و `enabled = true`.
  2. الاستعلام عن الوثيقة الحصرية `/admins/{uid}` في خادم Firestore أولاً مع مهلة زمنية 2.5 ثانية ثم التراجع لذاكرة التخزين المؤقت المحلية (Local Cache).
  3. التحقق الصارم من الشرط: `adminDoc.getBoolean("enabled") == true`.
  4. في حال كانت الوثيقة غير موجودة أو كان `enabled == false`، تُمسح الجلسة فورًا عبر `AppSettings.clearAdminSession()` وتُعاد القيمة `false`.
  5. لا يوجد أي فحص تراجعي (Fallback) على مستند `/users/{uid}.role` إطلاقًا (حيث تم استئصاله واختباره بالكامل في مرحلة Phase 05A).

### ب. الفرق بين إظهار الواجهة ومنح الصلاحية في Firestore:
* **واجهة التطبيق (UI Presentation):**
  في `LoginViewModel.kt` (السطور 47–53)، يفحص التطبيق عند الإقلاع `AppSettings.hasActiveAdminSession(currentUser.uid)`. إذا وُجدت جلسة محلية سابقة، تُعرض واجهة لوحة التحكم لتفادي تعليق الشاشة (Offline Fast-Path)، ولكن بالتزامن مع ذلك يُطلق استدعاء غير متزامن `repository.checkIsAdmin(currentUser)`.
* **الحماية على مستوى خادم Firestore:**
  إظهار الواجهة محليًا للمستخدم لا يمنحه أي قدرة تنفيذية على مستوى Firestore؛ إذ إن محرك القواعد في الخادم يرفض أي قراءة لمجموعات الإدارة المحمية (`/auditLogs`, قراءة كافة `/users`, كتابة `/config/*`, تعديل النقاط) بمجرد عدم تحقق شرط `isAdmin()`، وتُطلق مكتبة Firestore استثناء `PERMISSION_DENIED` مما يؤدي لإغلاق تدفقات البيانات فوريًا.

### ج. حماية الحقول الحساسة:
* تم التحقق من حماية 45 حقلاً حساسًا في دالة `modifyingSensitiveUserFields()` بقواعد Firestore. يمنع الخادم مستخدمي تطبيق User App من تعديل هذه الحقول، في حين يتيح لمسؤولي لوحة الإدارة تعديلها عبر استثناء `allow update: if isAdmin()`.

---

## 6. نتائج النقاط والاقتصاد والمكافآت (Points, Rewards & Economy)

### أ. إصدار النقاط وتعديل الأرصدة (`adjustUserPoints`):
* **المسار المستهدف:** `/users/{userId}` و `/users/{userId}/point_transactions/{txId}`
* **طبيعة العملية:** ذرية بالكامل عبر `firestore.runTransaction` في `AdminRepository.kt` (السطور 577–658).
* **آلية المعاملة الذرية:**
  1. قراءة لقطة مستند المستخدم والتأكد من وجوده.
  2. قراءة الرصيد الحالي `pointsBalance` ومجموع المكتسب `totalPointsEarned` ومجموع المنفق `totalPointsSpent`.
  3. حساب الرصيد الجديد: `newBalance = oldBalance + amount`.
  4. التحقق البرمجي الصارم:
     - رفض العملية إذا كان الرصيد الناتج سالبًا (`newBalance < 0L`).
     - رفض العملية إذا تجاوز الرصيد السقف الأقصى (`newBalance > 1,000,000L`).
  5. توليد معرّف فريد غير مكرر للسجل المالي: `"tx_${now}_${UUID}"`.
  6. إنشاء وثيقة الحركة المالية غير القابلة للتعديل في السلسلة الفرعية:
     * `txId`: String
     * `userId`: String
     * `type`: `"ADMIN_GRANT"` (إذا كان المبلغ موجبًا) أو `"ADMIN_ADJUSTMENT"` (إذا كان سالبًا)
     * `amount`: Long
     * `balanceBefore`: Long
     * `balanceAfter`: Long
     * `description`: String (سبب التعديل الإلزامي)
     * `createdAt`: Long (Epoch Millis)
     * `actorUid`: String (معرّف المسؤول المنفذ)
  7. تحديث الرصيد ومجاميع النقاط وحقل `updatedAt` في مستند `/users/{userId}` ضمن نفس المعاملة الذرية.
  8. تسجيل حدث تدقيق إداري في `/auditLogs`.

### ب. توافق القواعد مع عمليات الاقتصاد:
* في `firestore.rules` (السطور 148–152):
  ```javascript
  match /point_transactions/{txId} {
    allow read: if isAdmin() || isOwner(userId);
    allow write: if isAdmin();
  }
  ```
  القواعد تمنع مستخدمي تطبيق User App نهائيًا من إنشاء أو تعديل سجلات النقاط، وتسمح للمسؤول حصريًا بإنشائها.
* **الملاحظة المعمارية (INFERRED RISK):**
  العملية تتم من خلال Client SDK المعتمد على صلاحيات المستخدم المسجل دخوله كمسؤول، ولا تعتمد على Backend منعزل أو Firebase Admin SDK. بالرغم من أمان ذلك بفضل قواعد `isAdmin()`، فإن الممارسات القياسية للأنظمة المالية تفضل أن تصدر النقاط حصريًا عبر Cloud Function موثوقة لمنع أي تلاعب في حال تسريب بيانات اعتماد حساب مسؤول.

### ج. مهام المكافآت ولوحة المتصدرين:
* **كتالوج المهام (`/reward_tasks/{taskId}`):**
  لوحة الإدارة تدير المهام بالكامل (إضافة، تعديل، حذف، تفعيل/تعطيل) عبر `saveRewardTask` و `deleteRewardTask`. مدعومة بالتحقق الصارم `RewardTask.validate` (نوع المهمة، عدم سلبية النقاط، صيغة المعرف `^[a-zA-Z0-9_-]{2,64}$`). القواعد تسمح بالقراءة للمستخدمين والكتابة للمسؤول فقط.
* **لوحة المتصدرين (`/leaderboard/weekly_current`):**
  في `AdminRepository.kt` (السطور 885–919) وفي شاشة `EconomyConsoleScreen.kt`، تم التحقق من أن لوحة المتصدرين **للقراءة فقط (Strictly READ-ONLY)** في لوحة الإدارة. الشيفرة لا تحتوي على أي دالة لكتابة أو تصفير أو تسوية المتصدرين؛ حيث تترك هذه المهمة حصريًا لنظام التسوية الخارجي المجدول (Backend Cron Settlement).

---

## 7. نتائج الاشتراكات والترقيات (Subscriptions & Premium)

### أ. تحديث الاشتراكات عبر لوحة الإدارة (`updateSubscription`):
يتم تحديث اشتراك المستخدم في `AdminRepository.kt` (السطور 470–557) عبر النموذج الموحد التالي:
1. **قاعدة تمديد الاشتراك (Extension Rule):**
   يستعلم التطبيق عن انتهاء الاشتراك الحالي؛ فإذا كان الاشتراك ساريًا (`currentExpiresAt > now`)، يتم تمديد المدة الإضافية من تاريخ الانتهاء الحالي، أما إذا كان منتهيًا أو جديدًا، فيبدأ الاحتساب من اللحظة الحالية (`now`).
2. **الحقول التي تُحدّث وأنواع بياناتها:**

| الحقل في Firestore | النوع (Type) | القيمة عند الترقية (PRO) | القيمة عند الإلغاء (FREE) | التصنيف المعماري |
|---|---|---|---|---|
| `subscriptionTier` | String | `"PRO"` أو `"PRO_LITE"` (أحرف كبيرة) | `"FREE"` | المعياري القياسي (Canonical) |
| `planId` | String | `"pro_30d"` / `"pro_lite_7d"` | `"free"` | المعياري القياسي (Canonical) |
| `durationDays` | Int? | `30` / `7` / `10` / `1` | `null` | المعياري القياسي (Canonical) |
| `subscriptionStatus` | String | `"ACTIVE"` | `"ACTIVE"` أو `"FREE"` | المعياري القياسي (Canonical) |
| `subscriptionSource` | String | `"ADMIN_GRANT"` أو `"MONEY"` أو `"POINTS"` | `"ADMIN_GRANT"` | المعياري القياسي (Canonical) |
| `subscriptionReferenceId` | String? | معرّف الطلب أو null | `null` | المعياري القياسي (Canonical) |
| `subscriptionStartedAt` | Long | Epoch Millis الحالي | `now` | المعياري القياسي (Canonical) |
| `subscriptionExpiresAt` | Long? | Epoch Millis المستقبلي | `null` | المعياري القياسي (Canonical) |
| `isPremium` | Boolean | `true` | `false` | مرآة التوافق القديم (Mirror) |
| `isPro` | Boolean | `true` | `false` | مرآة التوافق القديم (Mirror) |
| `plan` | String? | `"pro"` أو `"pro_lite"` (أحرف صغيرة) | `"free"` | مرآة التوافق القديم (Mirror) |
| `proPlan` | String? | `"pro"` أو `"pro_lite"` (أحرف صغيرة) | `"free"` | مرآة التوافق القديم (Mirror) |
| `proExpiresAt` | Long? | Epoch Millis المستقبلي | `null` | مرآة التوافق القديم (Mirror) |
| `updatedAt` | Long | Epoch Millis الحالي | Epoch Millis الحالي | حقل طابع زمني عام |

3. **حتمية فصل الاشتراك عن جودة التشغيل (Absolute Benefit Invariant):**
   تم التأكد بنسبة 100% من أن حقول الجودة والتحميل (`allowedQuality`, `downloadLimit`, `canDownload`) **لا تُمس إطلاقًا** أثناء ترقية أو إلغاء الاشتراك. ميزة الاشتراك محصورة حصريًا في إزالة الإعلانات (`isAdFree`).

### ب. اعتماد ورفض طلبات الترقية (`ProRequestRepository`):
* **المسار:** `/pro_requests/{requestId}`
* **آلة الحالات (State Machine):**
  الطلبات تمر بدورة حياة أحادية الاتجاه: `PENDING` -> `APPROVED` أو `REJECTED`. ترفض دالتا `approveRequest` و `rejectRequest` تعديل أي طلب إذا كانت حالته الحالية غير `PENDING`.
* **الربط بين اعتماد الطلب وترقية المستخدم:**
  في المسار الطبيعي لـ `approveRequest` (السطور 113–185)، تنفذ العملية عبر `firestore.runTransaction` ذريًا؛ حيث تُقرأ بيانات الطلب وخطة الاشتراك والمدة ومعرّف المستخدم `userId`، ويتم تحديث مستند `/users/{userId}` بكافة حقول الاشتراك المعيارية والتوافقية بالتزامن مع تغيير حالة مستند `/pro_requests/{requestId}` إلى `APPROVED`.
* **الثغرة المكتشفة في المسار البديل (CONFIRMED EDGE CASE DEFECT):**
  في السطور 186–199 من `ProRequestRepository.kt`:
  ```kotlin
  } catch (e: Exception) {
      AppLogger.w("ProRequestRepository", "Transaction failed... Executing resilient merge.")
      val updates = hashMapOf<String, Any?>(
          "status" to ProRequestStatus.APPROVED.name,
          "reviewedAt" to now, ...
      )
      docRef.set(updates, SetOptions.merge())
  }
  ```
  عند فشل المعاملة الذرية (بسبب ضعف الاتصال أو انقطاعه)، ينتقل الكود لكتلة `catch`، ويقوم بتنفيذ `SetOptions.merge()` على مستند الطلب فقط لتغيير حالته إلى `APPROVED`، **دون تحديث مستند المستخدم `/users/{userId}`**! هذا يؤدي إلى حالة عدم اتساق (Inconsistent State) يظهر فيها الطلب كمعتمد بينما يظل المستخدم في الفئة المجانية.

---

## 8. نتائج الدعم الفني (Support Conversations)

### أ. قراءة تذاكر ورسائل الدعم:
* تدفق المحادثات في `SupportRepository.kt` يقرأ المجموعات:
  - التذاكر الرئيسية: `/support_conversations` مرتبة تنازليًا حسب `lastMessageAt`.
  - الرسائل: `/support_conversations/{conversationId}/messages` مرتبة تصاعديًا حسب `timestamp` مع حد أقصى 100 رسالة.
* متوافقة مع القواعد السطور 291–311 التي تسمح للمسؤول بالقراءة الكاملة.

### ب. إرسال ردود المسؤول:
* تنفذ عبر `sendAdminReply` باستخدام `firestore.batch()` لضمان الذرية بين:
  1. إنشاء مستند الرسالة في `/messages/{messageId}` مع إسناد الحقول:
     * `senderId`: معرّف المسؤول الحالي
     * `senderRole`: `"admin"`
     * `senderEmail`: بريد المسؤول
     * `text`: نص الرد
     * `timestamp`: Epoch Millis
     * `read`: `false`
  2. تحديث مستند المحادثة الرئيسي:
     * `lastMessage`: نص الرسالة
     * `lastMessageAt`: Epoch Millis
     * `lastSenderRole`: `"admin"`
     * `unreadByAdmin`: `false`
     * `unreadByUser`: `true`
     * `updatedAt`: Epoch Millis
* **توافق القواعد:** قواعد الرسائل تفرض شرط `senderRole == 'user'` فقط إذا كان الكاتب هو المستخدم العادي صاحب التذكرة، بينما تستثني المسؤول بصلاحية `allow create: if isAdmin() || ...`، مما يجعل ردود الإدارة متوافقة 100%.

### ج. حالات تذاكر الدعم:
* القيم المدعومة في `SupportConversationStatus`:
  `OPEN`, `PENDING`, `RESOLVED`, `CLOSED`.
* التراجع الآمن: أي قيمة غير معروفة تتراجع تلقائيًا إلى `CLOSED`.

### د. حذف التذاكر:
* دالة `deleteConversation` تحذف رسائل السلسلة الفرعية أولاً، ثم تحذف مستند المحادثة الرئيسي. مصرح بها للمسؤول حصريًا في القواعد (`allow delete: if isAdmin();`).

---

## 9. نتائج إدارة المستخدمين والبلاغات (User Management & Reports)

### أ. قراءة وإدارة المستخدمين:
* تعرض الشاشات قائمة المستخدمين من خلال `AdminRepository.getAllUsers()` مع دعم البحث الفوري بالاسم أو البريد أو المعرّف، والفلترة حسب الحالة (النشطون، المحظورون، المشتركون).
* تمييز معماري صارم بين نوعين من العقوبات في `Models.kt` و `UserDetailScreen.kt`:
  1. **الحظر الشامل للحساب (Global Account Ban):**
     يتحكم في حقول `isBanned`, `banReason`, `banExpiresAt`, `isActive`. الحظر المؤقت يحمل طابعًا زمنيًا في `banExpiresAt` يتم تقييمه تلقائيًا؛ فإذا انقضى الوقت، يُصنف الحساب كـ "BAN EXPIRED" دون رفع الحظر الفعلي إلا بقرار إداري.
  2. **تقييد الميزات الفردية (Feature Restrictions):**
     أعلام مستقلة (`canWatch`, `canDownload`, `canChat`, `canStory`, `canP2P`, `canComment`, `canUpload`, `canRequest`) تُكتب بشكل مزدوج مع الأعلام العكسية القديمة (`watchBan`, إلخ). تقييد ميزة معينة لا يحظر الحساب كليًا، وفك الحظر عن الحساب لا يلغي التقييدات السابقة.

### ب. منح وإلغاء صلاحية الإدارة:
* دالة `setUserAdminRole` في `AdminRepository.kt` (السطور 947–972):
  - عند منح الإدارة: تنشئ أو تحدث مستند `/admins/{userId}` وتضبط `enabled = true`، وتحدث حقل `role = "admin"` في مستند المستخدم.
  - عند سحب الإدارة: تضبط `enabled = false` في `/admins/{userId}` وتغير `role = "user"`.
  - متوافقة تمامًا مع منطق القواعد.

### ج. بلاغات المستخدمين ومشاكل التشغيل (`/reports`):
* تقرأ اللوحة البلاغات عبر `getReports()` وتعرض تفاصيلها (نوع المشكلة، معرّف المحتوى أو السيرفر، الشكوى).
* حل البلاغ يتم عبر `updateReportStatus` بتحديث:
  * `status`: `"RESOLVED"` أو `"DISMISSED"`
  * `resolutionNotes`: ملاحظات المسؤول
  * `resolvedAt`: الطابع الزمني
  * `resolvedBy`: معرّف المسؤول
* القواعد (السطور 286–287) تمنع المستخدم العادي من تعديل أو حل البلاغ وتخصصه للإدارة فقط.

---

## 10. نتائج الإشعارات والإعدادات (Notifications & Configuration)

### أ. بث الإشعارات العامة (`/notifications`):
* تنشئ دالة `sendNotification` مستند إشعار في `/notifications/{notificationId}` يتضمن:
  * `id`: معرّف المستند
  * `title`: العنوان
  * `body`: نص الإشعار
  * `type`: نوع الإشعار (`SYSTEM`, `ANNOUNCEMENT`, `ALERT`, `PROMO`, `UPDATE`)
  * `target` / `targetType`: الفئة المستهدفة (`ALL`, `PRO`, `UID`)
  * `targetUid`: معرّف المستخدم إذا كان مستهدفًا بعينه
  * `status`: `"PENDING"`
* **طبيعة العملية:** الكتابة تُنشئ سجلاً للإعلانات الداخلية (In-App Announcement). التوصيل الفعلي كإشعار فوري (FCM System Push Notification) يتطلب Cloud Function في الخادم تستمع لحدث `onCreate` وتستدعي `admin.messaging().sendEachForMulticast()`. الواجهة لا تزعم إرسال FCM مباشرة من الهاتف.

### ب. إعدادات النظام وتحديثات التطبيق (`/config/app` و `/app_updates`):
* تدير اللوحة إعدادات الصيانة وإصدارات التحديث وإعدادات DRM من خلال كتابة مستند موحد `/config/app`.
* **تعارض المسار القديم `/config/global`:**
  في `AdminRepository.kt` (سطر 1041)، في حال عدم وجود مستند `/config/app`، تحاول الشيفرة قراءة `/config/global`. بمراجعة القواعد، نجد أن السطر 202 يرفض صراحة أي مستند غير مصرّح به في `/config`:
  ```javascript
  match /config/{document=**} {
    allow read, write: if false;
  }
  ```
  لذلك فإن قراءة `/config/global` تفشل حتمًا بـ `PERMISSION_DENIED`.

### ج. إدارة الإضافات وترتيب البحث (`/managed_extensions` و `/config/search_order`):
* كتالوج الإضافات الحديث يكتب في `/managed_extensions` مع كتابة مزدوجة في الكتالوج القديم `/extensions`.
* تم التحقق من سلامة فحص أمن الروابط (SSRF Protection) في `ManagedExtensionValidator.kt`؛ حيث يرفض عناوين الشبكات المحلية (Localhost, Private IPs) ويفرض بروتوكول HTTPS الصارم.
* ترتيب البحث في `/config/search_order` يكتب قوائم المعرفات مرتبة لكل تصنيف (`movie`, `tv`, `anime`, `arabicDubbed`, `englishDubbed`) مع دمج إعدادات حالة الدبلجة `dubbingConfig` وأسماء الحقول البديلة للتوافق.

---

## 11. جدول توافق العمليات مع قواعد B2 المعتمدة (Compatibility Matrix)

| الوظيفة في لوحة الإدارة | المسار في Firestore | العملية | شرط القواعد المطابق | النتيجة والحكم | تصنيف الدليل |
|---|---|---|---|---|---|
| فحص هوية المسؤول | `/admins/{uid}` | Get | `request.auth.uid == adminUid \|\| isAdmin()` | **COMPLIANT** | CONFIRMED |
| جرد قائمة المستخدمين | `/users` | List / Query | `allow read: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| حظر وتعديل حساب مستخدم | `/users/{userId}` | Update | `allow update: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| تعديل ميزات المستخدم | `/users/{userId}` | Update | `allow update: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| ترقية وإلغاء الاشتراك | `/users/{userId}` | Update | `allow update: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| حذف مستخدم نهائيًا | `/users/{userId}` | Delete | `allow delete: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| تدوين حركة نقاط بالدفتر | `/users/{uid}/point_transactions/{id}` | Create (Tx) | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| قراءة دفتر حركات النقاط | `/users/{uid}/point_transactions` | List | `allow read: if isAdmin() \|\| isOwner(userId)` | **COMPLIANT** | CONFIRMED |
| حفظ إعدادات الميزات | `/config/features` | Write | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| حفظ إعدادات الاقتصاد | `/config/economy` | Write | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| حفظ أولويات البحث | `/config/search_order` | Write | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| حفظ إعدادات التطبيق العامة | `/config/app` | Write | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| قراءة الإعدادات القديمة | `/config/global` | Read | محظورة بـ `match /config/{document=**}` | **INCOMPATIBLE / DENIED** | CONFIRMED |
| إدارة كتالوج المهام | `/reward_tasks/{taskId}` | Write / Delete | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| عرض لوحة المتصدرين | `/leaderboard/weekly_current` | Read | `allow read: if isAuthenticated()` | **COMPLIANT** | CONFIRMED |
| إدارة الإضافات المدارة | `/managed_extensions/{extId}` | Write / Delete | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| إدارة الإضافات القديمة | `/extensions/{extId}` | Write / Delete | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| حفظ تحديثات التطبيق | `/app_updates/{updateId}` | Write / Delete | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| إرسال وحذف الإشعارات | `/notifications/{notifId}` | Write / Delete | `allow write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| قراءة وتدوين سجل التدقيق | `/auditLogs/{logId}` | Read / Create | `allow read, write: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| حل وحذف البلاغات | `/reports/{reportId}` | Update / Delete | `allow update, delete: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| إرسال رد دعم فني | `/support_conversations/.../messages` | Create | `allow create: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| تغيير حالة تذكرة دعم | `/support_conversations/{id}` | Update | `allow update: if isAdmin()` | **COMPLIANT** | CONFIRMED |
| اعتماد / رفض طلب Pro | `/pro_requests/{requestId}` | Update (Tx) | `allow update: if isAdmin()` | **COMPLIANT** | CONFIRMED |

---

## 12. مقارنة مع تقرير User App والمراجع المفقودة (User App Comparison Status)

* **الحالة الرسمية للمقارنة:** **BLOCKED (متوقفة لعدم توفر المدخلات المرجعية)**
* **بيان العجز المرجعي:**
  وفقًا للبند الثاني من وثيقة التعليمات ("إذا لم تتوفر قواعد B2 المحصّنة، سجّل المقارنة باعتبارها BLOCKED، ولا تخمّن محتوى القواعد")، فإن تقرير تطبيق المستخدمين `PHASE_20.1-B3-A_USER_APP_FIRESTORE_COMPATIBILITY_AUDIT_REPORT.md` لم يُرفق بمساحة العمل الحالية، كما أن ملفات مشروع CineStream User App غير موجودة في هذا المستودع.
* **قائمة ما يلزم توفيره لإتمام المقارنة الشاملة لاحقًا:**
  1. ملف التقرير الجنائي لتطبيق المستخدم: `PHASE_20.1-B3-A_USER_APP_FIRESTORE_COMPATIBILITY_AUDIT_REPORT.md`.
  2. شجرة ملفات مستودع CineStream User App ذات الصلة (خاصة: نماذج المستخدم والاشتراكات، مستودع المحادثات، ومستودع طلبات Pro).
  3. ملف القواعد النهائي المعتمد في Phase 20.1-B2 في حال كان يختلف عن `firestore.rules` (376 سطرًا) الموجود محليًا.
* **المقارنة التقديرية بناءً على العقد المشترك المتوفر (`FIREBASE_CONTRACT_V1.md`):**
  - **المسارات:** متطابقة بنسبة 100% مع العقد المشترك.
  - **حالات الدعم:** كلاهما متفق على `OPEN`, `PENDING`, `RESOLVED`, `CLOSED`.
  - **حالات طلبات Pro:** كلاهما متفق على `PENDING`, `APPROVED`, `REJECTED`.
  - **تنسيق التواريخ:** Long (Epoch Millis) عبر الطرفين.

---

## 13. المخاطر المؤكدة والاستنتاجات غير المؤكدة (Risks & Inferences)

### أ. المخاطر المؤكدة (Confirmed Vulnerabilities & Bugs):
1. **ثغرة عدم اتساق اعتماد طلبات Pro عند فشل المعاملة (CRITICAL / HIGH):**
   في `ProRequestRepository.kt` السطور 186–199، عند فشل `runTransaction` نتيجة ضعف الشبكة، يقوم الكود الاحتياطي بتحديث حالة المستند في `/pro_requests/{requestId}` إلى `APPROVED` فقط، ويهمل كليًا تحديث مستند المستخدم في `/users/{userId}`. النتيجة: يرى المستخدم طلبه معتمدًا، بينما لا يحصل حسابه على الاشتراك الفعلي.
2. **محاولة قراءة مسار محظور بالقواعد `/config/global` (LOW / COMPATIBILITY):**
   في `AdminRepository.kt` السطر 1041، يحاول التطبيق قراءة `/config/global` في حال غياب `/config/app`. قواعد Firestore تحظر صراحة أي مستند فرعي في `/config` بخلاف المستندات الأربعة المعرفة، مما يسبب خطأ `PERMISSION_DENIED` في سجلات النظام.
3. **فشل إعداد البناء في بيئة خالية من `.env.example` (BUILD CONFIG ISSUE):**
   إعداد `secrets` في `app/build.gradle.kts` يطلب إلزاميًا وجود ملف `.env.example` كملف خصائص افتراضي. غياب هذا الملف يمنع Gradle من إكمال مرحلة Project Evaluation على الحاويات النظيفة.

### ب. الاستنتاجات غير المؤكدة (Inferred Hypotheses):
1. **الاعتماد على Client SDK بدلاً من Backend موثوق للنقاط (ARCHITECTURAL INFERENCE):**
   تعديل النقاط يتم مباشرة من هاتف المسؤول. بالرغم من حمايته بقواعد `isAdmin()`، إلا أنه في حال اختراق مفاتيح أو حساب المسؤول، لا يوجد تحقق منطقي خارجي (Cloud Function) يمنع التلاعب بأرصدة النقاط أو سقفها.
2. **عدم إرسال إشعارات FCM حقيقية من التطبيق (SYSTEM INFERENCE):**
   كتابة الإشعار في `/notifications` لا تؤدي بمفردها لظهور إشعار في شريط الحالة بهواتف المستخدمين ما لم تكن هناك Cloud Function مفعلة على مشروع Firebase تستمع للمجموعة.

---

## 14. الاختبارات المنفذة فعليًا ونتائجها (Executed Tests & Empirical Results)

التزامًا بالبند الثالث الثامن ("لا تدّعِ إجراء اختبار لم يتم تنفيذه فعليًا")، نورد نتائج الاختبارات والأوامر التي تم تشغيلها فعليًا أثناء جلسة التدقيق:

### 1. الفحص الأمني الثابت (Static Security Scanner):
* **الأمر المنفذ:** `python3 scripts/static_security_scan.py`
* **النتيجة:** **اجتياز كامل (12 / 12 PASS — 100%)**
* **التفاصيل:**
  - قواعد Firestore الإصدار 2: `rules_version = '2';` (PASS)
  - سلطة الإدارة محصورة في `/admins/{uid}.enabled == true`: (PASS)
  - سلطة الإدارة تتجاهل تمامًا `users.role`: (PASS)
  - حماية الحقول الحساسة: فحص 45 حقل مستخدم محمي ضد التعديل (PASS)
  - عزل محادثات الدعم والتحقق من `senderRole == 'user'`: (PASS)
  - حماية طلبات Pro وفرض حالة `PENDING` ومنع تزوير المراجع: (PASS)
  - حصر كتابة الإضافات وسجلات التدقيق بالمسؤول فقط: (PASS)
  - وجود قاعدة الرفض الشامل الافتراضية `match /{document=**} { allow read, write: if false; }`: (PASS)
  - عدم وجود أذونات تخزين خطيرة في `AndroidManifest.xml`: (PASS)
  - عدم وجود مفاتيح سرية مكشوفة في كود Kotlin: (PASS)
  - خلو التطبيق من التحميل الديناميكي للشيفرات `DexClassLoader` (DCL): (PASS)

### 2. فحص محاكي Firestore المحلي (Emulator Connectivity Check):
* **الأمر المنفذ:** `curl -s -m 2 http://127.0.0.1:8085`
* **النتيجة:** **Exit code 7 (Failed to connect to host)**
* **التفسير:** محاكي Firestore غير قيد التشغيل داخل الحاوية في هذه الجلسة، مما حال دون تنفيذ اختبارات Mocha المعتمدة على المحاكي في `firestore-tests/`.

### 3. اختبارات البناء و JVM Unit Tests:
* **الأمر المنفذ:** `compile_applet` و `bash ./gradlew testDebugUnitTest`
* **النتيجة:** **فشل مرحلة التهيئة (Project Evaluation Failure - Exit Status 1)**
* **سبب الفشل الفعلي:**
  ```
  A problem occurred configuring project ':app'.
  > Failed to notify project evaluation listener.
     > The file '/app/applet/.env.example' could not be found
  ```
* **الملاحظة:** تطبيق القيود الإلزامية الصارمة لهذه المرحلة ("ممنوع تعديل أي ملف") منع إنشاء ملف `.env.example` لحل المشكلة، وتم تسجيل النتيجة بأمانة تامة كما حدثت فعليًا.

---

## 15. قائمة الإصلاحات المقترحة مرتبة حسب الخطورة (Remediation Plan)

| الأولوية | مستوى الخطورة | المشكلة / العيب المكتشف | الموضع البرمجي | الإصلاح المقترح مستقبلاً |
|---|---|---|---|---|
| **1** | **مرتفع جدًا (HIGH)** | سقوط ترقية مستند المستخدم في المعالجة الاحتياطية لـ `approveRequest` | `ProRequestRepository.kt` (السطور 186–199) | إعادة محاولة المعاملة الذرية أو تضمين تحديث مستند المستخدم في عملية الدمج الاحتياطية (`resilientSetUser`) بدلاً من تحديث الطلب بمفرده. |
| **2** | **متوسط (MEDIUM)** | محاولة قراءة مستند محظور بالقواعد `/config/global` | `AdminRepository.kt` (السطور 1040–1049) | إزالة استدعاء التراجع لـ `/config/global` نهائيًا والاعتماد الحصري على `/config/app`. |
| **3** | **متوسط (MEDIUM)** | غياب ملف `.env.example` مما يكسر بناء Gradle النظيف | جذر المشروع (`/.env.example`) | إضافة ملف `.env.example` فارغ أو بقيم توضيحية لتمكين Secrets Gradle Plugin من إكمال مرحلة Configuration. |
| **4** | **منخفض (LOW)** | تأخر تسجيل خروج المسؤول محليًا عند سحب صلاحيته إذا كانت الجلسة مخزنة | `LoginViewModel.kt` (السطور 60–66) | إلغاء شرط `!hasLocalAdminSession` عند فشل `checkIsAdmin` لإجبار التطبيق على تسجيل الخروج فور ثبوت نفي الصلاحية من الخادم. |
| **5** | **تحسين معماري (INFO)** | الاعتماد المباشر على Client SDK لإصدار النقاط | `AdminRepository.adjustUserPoints` | نقل إصدار النقاط إلى Cloud Functions خلف واجهة موثوقة تستخدم Firebase Admin SDK. |

---

## 16. إقرار حدود الوصول وشرط الإنهاء (Conclusion & Scope Boundary Statement)

* تم تنفيذ هذا التدقيق التقني الجنائي بنمط **القراءة والتحليل الفاحص فقط (Strictly Read-Only)** دون إدخال أي تعديلات على شيفرة التطبيق أو قواعد البيانات أو جداول الإنتاج.
* نؤكد صراحةً أن المقارنة الشاملة المباشرة مع تطبيق المستخدم `CineStream Users App` لم تكتمل وسُجلت كـ **BLOCKED** لعدم وجود ملفات ذلك التطبيق أو تقريره الجنائي B3-A في مساحة العمل الحالية، ولا يُدعى اكتمال المطابقة بين التطبيقين إلا بعد توفير مراجع التطبيق المقابل.
* كافة النتائج المستخلصة في هذا التقرير مثبتة بأرقام السطور والأدلة القاطعة من الشيفرة المصدرية الحالية للمشروع.
