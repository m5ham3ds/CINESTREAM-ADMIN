# استخراج تفاصيل العقد المشترك للوحة الإدارة — CineStream Admin Contract Extract
## Phase Shared Contract Extraction: CineStream Admin Dashboard Baseline

**تاريخ التدقيق والاستخراج:** 18 أكتوبر 2026  
**معرّف المرحلة والتقرير:** `PHASE_SHARED_CONTRACT_ADMIN_EXTRACT.md`  
**المرجع الأساسي المعتمد:** تقرير الجرد المرجعي للأدمن `PHASE_22.9-A_ADMIN_CONTRACT_INVENTORY_REPORT_AR.md`  
**قرار التعليقات الحاكم:** تأجيل ميزة التعليقات بالكامل وإزالة التحكم الإداري الموثق في `PHASE_22.10-B_DEFER_COMMENTS_FEATURE_REPORT_AR.md`  
**طبيعة المرحلة والقيود الإلزامية:** مهمة قراءة وتوثيق واستخراج عقود فقط — **دون أي تعديل على كود المشروع**، ودون نشر قواعد إلى الإنتاج، ودون حذف أو تعديل بيانات أو إعدادات Firebase، ودون المساس بـ QFilm.

---

### 1. المسارات الفعلية للملفات المرجعية المعتمدة في هذا التدقيق

استند هذا التقرير بدقة ومباشرة إلى الملفات الفعلية التالية داخل مستودع لوحة الإدارة:

1. **تقارير الجرد والمطابقة المشتركة:**
   - `/PHASE_22.9-A_ADMIN_CONTRACT_INVENTORY_REPORT_AR.md` (تقرير الجرد الشامل لعقود لوحة الإدارة).
   - `/PHASE_22.10-B_DEFER_COMMENTS_FEATURE_REPORT_AR.md` (تقرير تنفيذ تأجيل ميزة التعليقات وإزالة التحكم الإداري).
   - `/PHASES/PHASE_22.8-G2_ADMIN_RECONCILIATION_REPORT_AR.md` (تقرير المطابقة المشتركة للقرارات الإدارية).
   - `/PHASES/PHASE_22.8-F2_ADMIN_FINAL_VERIFICATION_REPORT_AR.md` (تقرير التحقق النهائي المستقل للوحة الإدارة).
   - `/PHASES/PHASE_22_SHARED_FIREBASE_CONTRACT_AR.md` (وثيقة العقد المشترك الموحد).
   - `/PHASES/FIREBASE_CONTRACT.md` (الميثاق المرجعي لعقود Firebase المشتركة).

2. **قواعد وبيئة اختبارات Firestore:**
   - `/firestore.rules` (ملف قواعد أمان Firestore المكون من 810 سطور، ببصمة `207aade4...`).
   - `/firestore-tests/firestore-rules.test.js` (جناح اختبارات Mocha لمحاكي Firestore - 114 اختبار).
   - `/firestore-tests/phase05b-comprehensive-rules.test.js` (جناح اختبارات القواعد الشاملة - 32 اختبار).
   - `/firestore-tests/phase03a1-rules.test.js` (جناح اختبارات أمان الاقتصاد والمسارات الحساسة - 36 اختبار).

3. **نماذج البيانات والعقود البرمجية في كود أندرويد (Kotlin):**
   - `/app/src/main/java/com/example/contract/FirebaseContract.kt` (عقد أسماء المجموعات والمسارات والمفاتيح).
   - `/app/src/main/java/com/example/models/Models.kt` (`User`, `UserReport`, `AppConfig`, `AppUpdate`, `NotificationRequest`).
   - `/app/src/main/java/com/example/models/ProRequest.kt` (`ProRequest`, `ProRequestStatus`).
   - `/app/src/main/java/com/example/models/ManagedExtension.kt` (`ManagedExtension`, `ExtensionStatus`).
   - `/app/src/main/java/com/example/models/SearchOrderConfig.kt` (`SearchOrderConfig`, `DubbingConfig`).

4. **مستودعات البيانات وطبقة العرض في لوحة الإدارة:**
   - `/app/src/main/java/com/example/repository/AdminRepository.kt` (إدارة المستخدمين، التكوين، البلاغات، والإشعارات).
   - `/app/src/main/java/com/example/repository/ProRequestRepository.kt` (إدارة طلبات الترقية واعتمادها ذرياً).
   - `/app/src/main/java/com/example/repository/SearchOrderRepository.kt` (إدارة ترتيب مصادر البحث والدبلجة).
   - `/app/src/main/java/com/example/ui/screens/UserDetailScreen.kt` (شاشة تفاصيل المستخدم بعد إزالة تحكم التعليقات).
   - `/app/src/main/java/com/example/ui/screens/ReportsScreen.kt` (شاشة مراجعة ومعالجة البلاغات).
   - `/app/src/main/java/com/example/ui/screens/ProRequestsScreen.kt` (شاشة فحص واعتماد طلبات Pro).

---

### 2. المصفوفة الشاملة لاستخراج العقود وتصنيفها (Contract Extraction Matrix)

| # | الميزة / المحور المعماري | مسارات Firestore الفعلية | الحقول وأنواع البيانات والقيم المقبولة | أطراف القراءة والكتابة | حقل الملكية وآلية التحقق | سلوك القواعد المحلية (Emulator) | حالة التصنيف والتوافق |
|---|---|---|---|---|---|---|:---:|
| **1** | **قواعد Firestore وبنية الأمان** | `/*` (كافة المسارات)، مع الإغلاق الشامل بـ `match /{document=**}` | بنية قواعد محكمة بدون أي منح عشوائي (No Wildcard Grants)، تتضمن دوال حماية مثل `isAdmin()`, `isOwnerAccount()`, `modifyingSensitiveUserFields()`. | القراءة والكتابة محكومة بالتحقق الصارم من الهوية والأدوار. | يعتمد `request.auth.uid` ومطابقته لوثائق الحسابات أو الملكية. | **اجتياز 182/182 فحصاً أمنياً محلياً بنسبة 100%**. منع افتراضي شامل لكافة المسارات غير المعرفة. | **تحتاج إلى تأكيد**<br>(المحلي مجاز 100% / السحابي المنشور `UNVERIFIED`) |
| **2** | **البلاغات والشكاوى** | `/reports/{reportId}` | • `id: String`<br>• `userId: String`<br>• `userEmail: String`<br>• `type: String` (`"ISSUE"`, `"STREAM"`, `"BUG"`, `"CONTENT"`)<br>• `targetType: String` (`"STREAM"`, `"MEDIA"`, `"EXTENSION"`, `"USER"`)<br>• `targetId: String`<br>• `title: String`, `description: String`<br>• `status: String` (الأدمن يكتب: `"PENDING"`, `"RESOLVED"`, `"DISMISSED"`)<br>• `resolutionNotes: String`<br>• `createdAt: Long`<br>• `resolvedAt: Long?`<br>• `resolvedBy: String` | **القراءة:** صاحب البلاغ أو المشرف.<br>**الإنشاء:** المستخدم العادي (بحالة `PENDING` أو `pending` حصراً، ودون حقول الحل).<br>**التعديل/الحذف:** المشرف فقط. | **حقل الملكية:** `userId`.<br>**التحقق:** `request.resource.data.userId == request.auth.uid`. | تسمح بإنشاء البلاغ للمالك فقط بشرط خلوه من حقول الإشراف (`resolvedBy`, `resolvedAt`, `resolutionNotes`). تمنع تعديل المستخدم للبلاغ بعد إنشائه. | **متعارضة جزئياً / تحتاج إلى تأكيد**<br>(الأدمن يعتمد `"DISMISSED"` ويرفض `"rejected"`) |
| **3** | **طلبات ترقية Pro** | `/pro_requests/{requestId}` | • `requestId: String`<br>• `userId: String`, `userEmail: String`, `userName: String`<br>• `requestedPlan: String`, `planId: String?`<br>• `requestedDuration: String`, `durationDays: Long?`<br>• `paymentMethod: String?`, `paymentProofUrl: String?`, `paymentReference: String?`<br>• `userNote: String`, `notes: String?`<br>• `status: String` (`"PENDING"`, `"APPROVED"`, `"REJECTED"`, `"CANCELLED"`)<br>• `createdAt: Long`, `updatedAt: Long`<br>• `cancelledAt: Long?`<br>• `reviewedAt: Long?`, `reviewedBy: String?`<br>• `rejectionReason: String?`, `adminNote: String?` | **القراءة:** صاحب الطلب أو المشرف.<br>**الإنشاء:** المستخدم العادي (بحالة `PENDING` أو `pending`، ودون حقول المراجعة).<br>**الاعتماد والرفض:** المشرف ذرياً عبر `runTransaction`.<br>**الإلغاء:** صاحب الطلب حصرياً. | **حقل الملكية:** `userId`.<br>**التحقق:** `resource.data.userId == request.auth.uid && request.resource.data.userId == request.auth.uid`. | **قاعدة الإلغاء الصارمة:** يُسمح للمالك بنقل الحالة من `PENDING/pending` إلى `CANCELLED` حصراً وتعديل `['status', 'updatedAt', 'cancelledAt']` فقط. حظر إعادة فتح الطلبات. | **متوافقة بالكامل**<br>(تم توحيد المخطط وآلة الحالات وحمولة الإلغاء) |
| **4** | **التعليقات المؤجلة** | لا يوجد مسار `/comments` في العقد أو القواعد (يخضع لـ Default Deny) | • حقل الصلاحية في وثيقة المستخدم `/users/{uid}`:<br>  `canComment: Boolean = true` (محفوظ للتوافقية المخططية القديمة فقط).<br>• لا توجد وثائق أو حقول تعليقات نشطة. | **القراءة والكتابة:** محظورة ومرفوضة كلياً لجميع الأطراف على أي مسار تعليقات.<br>تعديل راية `canComment` محظور على المستخدم، ومُعطل ومشلول من طرف المشرف (PHASE 22.10-B). | **غير متاح** لعدم وجود مجموعة تعليقات في قاعدة البيانات. | تخضع أي محاولة وصول لمسار التعليقات لرفض فوري `PERMISSION_DENIED` بالقاعدة الافتراضية `allow read, write: if false;`. | **مؤجلة ومعطلة رسمياً**<br>(تأجيل كامل بموجب القرار المعماري 22.10-B) |
| **5** | **الإضافات المدارة** | • المسار المعياري في الأدمن: `/managed_extensions/{extensionId}`<br>• المسار التوافقي في القواعد: `/extensions/{extensionId}` | • `extensionId: String` / `id: String`<br>• `scraperKey: String`, `name: String`, `description: String`<br>• `baseUrl: String`, `searchUrl: String`, `iconUrl: String`<br>• `runtimeApiVersion: Int`, `definitionVersion: Int`, `minAppVersionCode: Int`<br>• `status: String` (`"ACTIVE"`, `"MAINTENANCE"`, `"DISABLED"`, `"DEPRECATED"`).<br>• `priority: Int`, `language: String`<br>• `capabilities: List<String>`, `contentTypes: List<String>`, `audioCapabilities: List<String>`<br>• `enabled: Boolean = true` | **القراءة:** كافة المستخدمين الموثقين `isAuthenticated()`.<br>**الكتابة:** المشرفون المعتمدون حصراً `isAdmin()`. | **إشراف إداري مركزي** (لا توجد ملكية فردية للمستخدمين). | القواعد تسمح بالقراءة لكافة الموثقين على كلا المسارين (`/managed_extensions` و `/extensions`)، وتحصر الكتابة بالمشرفين. تحظر القواعد والبيئة الأكواد التنفيذية الثنائية. | **تحتاج إلى تأكيد**<br>(المساران مدعومان بالقواعد، ويلزم تأكيد مسار القراءة الفعلي لدى العميل) |
| **6** | **الإعلانات والإشعارات** | `/notifications/{notificationId}` | • `id: String`<br>• `title: String`<br>• `body: String`, `message: String` (دعم مزدوج لنص الإشعار)<br>• `type: String` (`"SYSTEM"`, `"ANNOUNCEMENT"`, `"ALERT"`, `"PROMO"`)<br>• `target: String` (`"all"`, `"user"`, `"pro"`)<br>• `targetType: String` (`"ALL"`, `"UID"`, `"PRO"`)<br>• `targetUid: String?` (إلزامي إذا كان `target == "user"`)<br>• `createdBy: String`<br>• `createdAt: Long`, `expiresAt: Long?`<br>• `isActive: Boolean`, `status: String` | **القراءة:** المستخدمون الموثقون وفق سياسة الجمهور المستهدف.<br>**الكتابة:** المشرف فقط `allow write: if isAdmin();`. | **الاستهداف الفردي:** عبر `targetUid`.<br>**التحقق:** `resource.data.targetUid == request.auth.uid`. | **هندسة الرفض الافتراضي (Fail-Closed):**<br>• `target == 'all'`: متاح للجميع.<br>• `target == 'user'`: متاح للمطابق فقط.<br>• `target == 'pro'`: مقيد بالمشترك النشط.<br>• غياب `target`: محظور على المستخدمين ومتاح للمشرف فقط. | **متوافقة بالكامل**<br>(معتمدة ومفحوصة بالمحاكي في A03) |
| **7** | **ترتيب البحث ومصادر الدبلجة** | `/config/search_order` (وثيقة مفردة Singleton) | • `movie: List<String>` (معرفات المصادر المرتبة للأفلام)<br>• `tv: List<String>`, `series: List<String>` (مزدوج للأفلام والمسلسلات)<br>• `anime: List<String>`<br>• `arabicDubbed: List<String>`, `arabicDub: List<String>`, `arabic_dub: List<String>` (تسميات توافقية متعددة)<br>• `englishDubbed: List<String>`, `englishDub: List<String>`, `english_dub: List<String>`<br>• `dubbingConfig: Map<String, String>` (`arabicDubbedStatus`, `englishDubbedStatus`)<br>• `updatedAt: Long`, `updatedBy: String` | **القراءة:** كافة المستخدمين الموثقين `isAuthenticated()`.<br>**الكتابة:** المشرفون المعتمدون حصراً عبر `SearchOrderRepository`. | **إشراف إداري مركزي** (وثيقة تكوين عامة للنظام). | القواعد تسمح بالقراءة للموثقين والكتابة للمشرف. المسارات العشوائية والمهملة تحت `/config/*` محظورة ومرفوضة تماماً (`match /config/{document=**} { allow read, write: if false; }`). | **متوافقة بالكامل**<br>(دعم التسميات البديلة المتعددة وتأكيد 32 فحصاً معمارياً) |
| **8** | **تحديثات التطبيق والتكوين العام** | • التكوين العام: `/config/app`<br>• سجل التحديثات: `/app_updates/{updateId}` | **في وثيقة `/config/app`:**<br>• `maintenanceEnabled: Boolean = false`<br>• `maintenanceTitle: String`, `maintenanceMessage: String`<br>• `minimumVersionCode: Int`, `latestVersionCode: Int`, `latestVersionName: String`<br>• `apkUrl: String`, `apkSha256: String`<br>• `mandatoryUpdate: Boolean`, `releaseNotes: String`<br>• `providersJson: String` (كتالوج نصوص الموفرين)<br>• `defaultOfflineDays: Int = 2`, `defaultForcedAds: Int = 5`<br>• إعدادات الوسائط: `cloudinaryCloudName`, `cloudinaryUploadPreset` (دون API Secret نهائياً)<br>**في مسار `/app_updates/{id}`:**<br>• `id`, `versionCode`, `versionName`, `minVersionCode`, `apkUrl`, `apkSha256`, `mandatoryUpdate`, `releaseNotes`, `status` (`PUBLISHED`, `DRAFT`, `ARCHIVED`), `publishedBy`, `createdAt`, `updatedAt` | **القراءة:** كافة المستخدمين الموثقين `isAuthenticated()`.<br>**الكتابة:** المشرفون المعتمدون حصراً `allow write: if isAdmin();`. | **إشراف إداري مركزي**. | القراءة متاحة للموثقين والكتابة محصورة بالمشرف. المسار المهمل `/config/global` محظور صراحة ولا يُسمح بالوصول إليه. | **متوافقة بالكامل**<br>(مسار التكوين القياسي معتمد ومحمي) |

---

### 3. التحليل الفني المفصل لكل ميزة واستخراج عقودها الفعلية

#### أولاً: قواعد أمان Firestore ومحددات البيئة السحابية (`firestore.rules`)
1. **الواقع الفعلي المسجل في لوحة الإدارة:**
   - ملف القواعد: `/firestore.rules` بحجم 810 أسطر وبصمة تجزئة SHA-256 رقمها `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`.
   - محاكي Firestore المحلي يجتاز **182 من أصل 182 فحصاً أمنياً بنسبة 100%**.
   - القواعد تنتهي بالإغلاق التام لكافة المسارات غير المعرفة:
     ```javascript
     match /{document=**} {
       allow read, write: if false;
     }
     ```
2. **حدود البيئة السحابية المنشورة (`UNVERIFIED`):**
   - لم يتم تنفيذ أي عملية نشر سحابي (`firebase deploy`) من هذه الجلسة التزاماً بالقيود.
   - تقرير User App (PHASE 22.6-A) رصد بصمة قواعد مختلفة (`54c8570a...`).
   - **التقييم الفني:** السلوك المحلي محكم ومثبت تماماً، ولكن تطابق القواعد المنشورة سحابياً في بيئة Production يتطلب تأكيداً مستقلاً.

---

#### ثانياً: عقد بلاغات وشكاوى المستخدمين (`/reports/{reportId}`)
1. **المسار وهيكل الوثيقة:**
   - المسار المعتمد: `/reports/{reportId}`.
   - النموذج: `app/src/main/java/com/example/models/Models.kt` (`UserReport`).
2. **حقول الوثيقة وأنواعها:**
   - `id: String` (معرف البلاغ المطابق لمعرف المستند).
   - `userId: String` (معرف المستخدم صاحب البلاغ - حقل التحقق من الملكية).
   - `userEmail: String` (البريد للتواصل).
   - `type: String` (نوع البلاغ: `"ISSUE"`, `"STREAM"`, `"BUG"`, `"CONTENT"`).
   - `targetType: String` (الجهة أو العنصر المبلغ عنه: `"STREAM"`, `"MEDIA"`, `"EXTENSION"`, `"USER"`).
   - `targetId: String` (معرف العنصر المبلغ عنه).
   - `title: String`, `description: String` (عنوان البلاغ وتفاصيل المشكلة).
   - `status: String` (حالة البلاغ في لوحة الإدارة: `"PENDING"`, `"RESOLVED"`, `"DISMISSED"`).
   - `resolutionNotes: String = ""` (ملاحظات المعالجة الإدارية).
   - `createdAt: Long` (طابع الإنشاء بالمللي ثانية).
   - `resolvedAt: Long? = null` (طابع المعالجة الإدارية).
   - `resolvedBy: String = ""` (معرف المشرف الذي قام بالمعالجة).
3. **سلوك القواعد والتحقق من الملكية:**
   - يُسمح بإنشاء البلاغ فقط إذا كان `request.resource.data.userId == request.auth.uid`.
   - يجب أن تكون الحالة عند الإنشاء: `request.resource.data.status in ['PENDING', 'pending']`.
   - يُحظر على العميل إرسال حقول المعالجة الإدارية (`resolvedBy`, `resolvedAt`, `resolutionNotes`).
   - التعديل والحذف محصوران بالمشرف (`allow update, delete: if isAdmin();`).
4. **نقطة التعارض وعدم الحسم مع تطبيق المستخدمين:**
   - كود وواجهات لوحة الإدارة (`ReportsScreen.kt` و `AdminRepository.kt`) تعتمد حصرياً خياري الحل: `"RESOLVED"` أو الإهمال: `"DISMISSED"`.
   - كلمة `"rejected"` غير موجودة تماماً في مشروع الإدارة، بينما تشير تقارير فحص User App السابقة إلى احتمالية استخدامه أو توقعه لحالة `"rejected"`.

---

#### ثالثاً: عقد طلبات ترقية CineStream Pro (`/pro_requests/{requestId}`)
1. **المسار وهيكل الوثيقة:**
   - المسار: `/pro_requests/{requestId}`.
   - النماذج: `ProRequest.kt` و `ProRequestStatus`.
   - المستودع: `ProRequestRepository.kt`.
2. **الحقول المعتمدة والمطابقة المزدوجة:**
   - معرف المستند والطلب: `requestId: String`.
   - هوية المستخدم: `userId: String` (حقل الملكية), `userEmail: String`, `userName: String`.
   - تفاصيل الخطة والمدة (توفيق كامل بين تسميات الطرفين):
     * `requestedPlan: String = "pro"`, `planId: String? = null`.
     * `requestedDuration: String = "1 month"`, `durationDays: Long? = null`.
   - بيانات الدفع والإثبات:
     * `paymentMethod: String? = null`.
     * `paymentProofUrl: String? = null` (رابط صورة الإشعار في Cloudinary).
     * `paymentReference: String? = null` (رقم الحوالة أو المعاملة النصي المستقل).
   - الملاحظات: `userNote: String = ""`, `notes: String? = null`.
   - دورة حياة الحالة: `status: String` والقيم الحتمية:
     $$\text{PENDING} \longrightarrow \{\text{APPROVED}, \text{REJECTED}, \text{CANCELLED}\}$$
   - الطوابع الزمنية وحقول المراجعة والإلغاء:
     * `createdAt: Long`, `updatedAt: Long`.
     * `cancelledAt: Long? = null` (طابع الإلغاء المرسل من العميل).
     * `reviewedAt: Long?`, `reviewedBy: String?`, `reviewedByEmail: String?`.
     * `rejectionReason: String?`, `adminNote: String?`.
3. **ضوابط الإلغاء والأمان الصارم:**
   - الإلغاء محصور بمالك الطلب (`userId == request.auth.uid`).
   - الحالة السابقة يجب أن تكون `PENDING` أو `pending`.
   - الحالة الجديدة المسموح بها هي `CANCELLED` المعيارية حصراً بالأحرف الكبيرة.
   - الحقول المسموح بتعديلها عند الإلغاء محصورة حصرياً في القائمة: `['status', 'updatedAt', 'cancelledAt']`.
   - لا يُسمح بتعديل الخطة أو مراجع الدفع أو حقول المراجعة أثناء الإلغاء.
   - الاعتماد والرفض يتمان حصرياً بواسطة المشرف ذرياً عبر `firestore.runTransaction`.

---

#### رابعاً: عقد التعليقات المؤجلة (Deferred Comments Contract)
1. **الواقع الهندسي بعد تنفيذ المرحلة 22.10-B:**
   - تم تطهير واجهة الإدارة (`UserDetailScreen.kt`) بحذف زر تحكم `canComment` تماماً.
   - تم تعديل `AdminRepository.updateFeaturePermission` لتعطيل وإيقاف أي عملية كتابة إدارية تستهدف `"comment"` أو `"canComment"`.
   - تم تعديل `AdminRepository.updateUser` لفلترة واستبعاد حقل `canComment` من أي تحديث للمستخدمين.
   - تم تحصين `UserDetailViewModel.togglePermission` بعدم إرسال أي طلب يخص التعليقات.
2. **حالة حقل `canComment` وقواعد البيانات:**
   - تم الإبقاء على حقل `canComment: Boolean = true` في نموذج `User` بملف `Models.kt`، مع توثيقه كحقل مؤجل ومحفوظ فقط لسلامة التوافقية المخططية مع وثائق Firestore الحالية وتجنب حدوث أخطاء تحويل (Deserialization Errors).
   - يستمر حظر الحقل على المستخدم العادي داخل دالة `modifyingSensitiveUserFields()` في `firestore.rules`.
3. **حالة مسار التخزين وقواعد Firestore:**
   - لا يوجد أي مسار لمجموعة التعليقات `/comments` في `FirebaseContract.kt` ولا في `firestore.rules`.
   - أي محاولة وصول لمسار التعليقات تواجه رفضاً أمنياً قاطعاً `PERMISSION_DENIED` بموجب القاعدة الافتراضية الشاملة.
   - **القرار المعماري الثابت:** الميزة مؤجلة بالكامل ولا توجد أي واجهات أو عمليات كتابة أو صلاحيات نشطة لها في لوحة الإدارة.

---

#### خامساً: عقد الإضافات البرمجية المدارة (`/managed_extensions` و `/extensions`)
1. **المسارات في الكود والقواعد:**
   - المسار المعتمد في كود لوحة الإدارة: `/managed_extensions/{extensionId}`.
   - المسار التوافقي القديم المدعوم في القواعد: `/extensions/{extensionId}`.
   - كلاهما محمي في `firestore.rules` (السطور 416-431) بالسماح بالقراءة للموثقين `isAuthenticated()` وحصر الكتابة بالمشرفين `isAdmin()`.
2. **الحقول والأنواع في النموذج (`ManagedExtension.kt`):**
   - `extensionId: String` (معرف الوثيقة).
   - `scraperKey: String` (المفتاح البرمجي للمكشطة).
   - `name: String`, `description: String`.
   - `baseUrl: String`, `searchUrl: String`, `iconUrl: String`.
   - `runtimeApiVersion: Int = 1`, `definitionVersion: Int = 1`, `minAppVersionCode: Int = 1`.
   - `status: String` (القيم المعيارية: `"ACTIVE"`, `"MAINTENANCE"`, `"DISABLED"`, `"DEPRECATED"`).
   - `priority: Int = 0`, `language: String = "ar"`.
   - `capabilities: List<String>`, `contentTypes: List<String>`, `audioCapabilities: List<String>`.
   - `enabled: Boolean = true`.
   - `createdAt: Long`, `updatedAt: Long`.
3. **السياسة الأمنية الصارمة:**
   - كتالوج تكوين نقي 100% (Pure Configuration Metadata).
   - خلو المستودع تماماً من أي ملفات APK تنفيذية أو حزم ثنائية أو جافاسكريبت أو تحميل ديناميكي للشيفرات البرمجية (`No DexClassLoader / No Remote Code Execution`).

---

#### سادساً: عقد الإعلانات والإشعارات الموجهة (`/notifications/{notificationId}`)
1. **المسار والنموذج:**
   - المسار: `/notifications/{notificationId}`.
   - النموذج: `Models.kt` (`NotificationRequest`).
   - المستودع: `AdminRepository.sendNotification`.
2. **الحقول وأنواعها:**
   - `id: String` (معرف الوثيقة).
   - `title: String` (عنوان الإشعار).
   - `body: String`, `message: String` (توافق مزدوج لنص المحتوى لضمان قراءة النسخ القديمة والحديثة).
   - `type: String = "SYSTEM"` (`"SYSTEM"`, `"ANNOUNCEMENT"`, `"ALERT"`, `"PROMO"`).
   - `target: String = "all"` (`"all"`, `"user"`, `"pro"`).
   - `targetType: String` (`"ALL"`, `"UID"`, `"PRO"`).
   - `targetUid: String?` (إلزامي عند الاستهداف الفردي).
   - `createdBy: String` (معرف المشرف الكاتب).
   - `createdAt: Long`, `expiresAt: Long?`, `isActive: Boolean = true`, `status: String = "PENDING"`.
3. **قواعد الأمان وسلوك الجمهور المستهدف (Fail-Closed):**
   - الكتابة محصورة حصرياً بالمشرفين المعتمدين (`allow write: if isAdmin();`).
   - القراءة للمستخدمين مقيدة وفق سياسة الحظر الافتراضي:
     * إذا كان `target == 'all'`: متاح لكافة الموثقين.
     * إذا كان `target == 'user'`: متاح فقط إذا كان `targetUid == request.auth.uid`.
     * إذا كان `target == 'pro'`: متاح فقط لمشتركي Pro النشطين بالتحقق من وثيقة المستخدم.
     * إذا غاب حقل `target`: يُحظر تماماً عن كافة المستخدمين العاديين، ويتاح للمشرف فقط لغايات المراجعة والتدقيق والترحيل.

---

#### سابعاً: عقد ترتيب البحث ومصادر الدبلجة (`/config/search_order`)
1. **المسار والنموذج:**
   - المسار: `/config/search_order` (وثيقة مفردة Singleton تحت مجموعة `/config`).
   - النماذج: `SearchOrderConfig.kt` و `DubbingConfig.kt`.
   - المستودع: `SearchOrderRepository.kt`.
2. **الحقول والتسميات التوافقية المتعددة (Multi-Alias Compatibility):**
   - فئة الأفلام: `movie: List<String>`.
   - فئة المسلسلات: `tv: List<String>` و `series: List<String>` (كتابة مزدوجة لكلا التسميتين بنفس القائمة).
   - فئة الأنمي: `anime: List<String>`.
   - فئة الدبلجة العربية: `arabicDubbed: List<String>`, `arabicDub: List<String>`, `arabic_dub: List<String>` (كتابة وتزامن ثلاثي للحقول).
   - فئة الدبلجة الإنجليزية: `englishDubbed: List<String>`, `englishDub: List<String>`, `english_dub: List<String>`.
   - تكوين حالات مجموعات الدبلجة: `dubbingConfig: Map<String, String>` (مثل `arabicDubbedStatus: "ACTIVE"`).
   - طوابع المراجعة: `updatedAt: Long`, `updatedBy: String`.
3. **سلوك القواعد والأمان:**
   - القراءة متاحة لكافة المستخدمين الموثقين (`allow read: if isAuthenticated();`).
   - الكتابة محصورة بالمشرفين (`allow write: if isAdmin();`).
   - تم التحقق من ثبات ودقة المنطق البرمجي بـ 32 اختباراً معمارياً متخصصاً (`SearchOrderArchitectureTest`) اجتيزت بنسبة 100%.

---

#### ثامناً: عقد تحديثات التطبيق والتكوين العام (`/config/app` و `/app_updates`)
1. **المسارات الفعلية:**
   - وثيقة التكوين العامة: `/config/app`.
   - سجل التحديثات التفصيلي: `/app_updates/{updateId}`.
2. **الحقول وأنواعها في `/config/app`:**
   - حالة الصيانة: `maintenanceEnabled: Boolean = false`, `maintenanceTitle: String`, `maintenanceMessage: String`.
   - إصدارات التطبيق والتحديث الإجباري:
     * `minimumVersionCode: Int = 1` (الحد الأدنى المطلوب للتشغيل).
     * `latestVersionCode: Int = 1`, `latestVersionName: String = "1.0.0"`.
     * `apkUrl: String = ""`, `apkSha256: String = ""`.
     * `mandatoryUpdate: Boolean = false`, `releaseNotes: String = ""`.
   - نصوص كتالوج الموفرين: `providersJson: String`.
   - ضوابط المشاهدة والإعلانات: `defaultOfflineDays: Int = 2`, `defaultForcedAds: Int = 5`.
   - إعدادات وسائط Cloudinary الآمنة: `cloudinaryCloudName: String`, `cloudinaryUploadPreset: String` (خلو كامل من الـ API Secret).
3. **الحقول في سجل التحديثات `/app_updates/{updateId}`:**
   - `id: String`, `versionCode: Int`, `versionName: String`, `minVersionCode: Int`.
   - `apkUrl: String`, `apkSha256: String`, `mandatoryUpdate: Boolean`, `releaseNotes: String`.
   - `status: String` (`"PUBLISHED"`, `"DRAFT"`, `"ARCHIVED"`), `publishedBy: String`, `createdAt: Long`, `updatedAt: Long`.
4. **سلوك القواعد والمسارات المحظورة:**
   - القراءة متاحة لجميع الموثقين، والكتابة محصورة بالمشرفين.
   - المسارات المهملة والبديلة مثل `/config/global` و `/system_config/*` محظورة ومرفوضة قطعياً في القواعد.

---

### 4. قائمة بالفروقات والمسائل غير المحسومة (Discrepancies & Open Questions)

بناءً على المقارنة بين عقود لوحة الإدارة الحالية والتقارير المرجعية المشتركة، تبرز المسائل التالية كعناصر غير محسومة تتطلب المواءمة في الخطوات القادمة:

1. **مسألة حالة البلاغ المرفوض (`rejected` مقابل `DISMISSED`):**
   - **الواقع في الأدمن:** يعتمد كود وقواعد لوحة الإدارة الحالات `"PENDING"`, `"RESOLVED"`, `"DISMISSED"`.
   - **الوضع في تطبيق المستخدم:** يُحتمل أن يتوقع تطبيق المستخدمين قراءة حالة باسم `"rejected"` عند رفض البلاغ.
   - **السؤال غير المحسوم:** هل يجب دعم كتابة مزدوجة للحالتين أو مواءمة تطبيق المستخدم ليعتمد القيمة المعيارية `"DISMISSED"`؟

2. **مسار قراءة كتالوج الإضافات البرمجية (`/managed_extensions` مقابل `/extensions`):**
   - **الواقع في الأدمن:** الكود يعتمد المسار المعياري `/managed_extensions/{id}`، بينما تدعم القواعد كلا المسارين (`/managed_extensions` و `/extensions`).
   - **السؤال غير المحسوم:** ما هو المسار الفعلي الذي يستمع إليه تطبيق المستخدمين لجلب قائمة الموفرين؟ هل يحتاج الأدمن إلى مزامنة مزدوجة لكلا المسارين أم يتم تثبيت المسار المعياري؟

3. **حسم قرار التعليقات المؤجلة في العقد المشترك:**
   - **الواقع في الأدمن:** تم شل وإزالة أي تحكم أو واجهة للتعليقات تماشيًا مع قرار التأجيل في 22.10-B، مع الإبقاء على حظر مسار `/comments` في القواعد.
   - **المطلوب حسمه مع تطبيق المستخدم:** تأكيد خلو تطبيق المستخدمين من محاولات كتابة لمسار التعليقات، أو تعطيل شاشات التعليقات فيه تجنباً لأخطاء `PERMISSION_DENIED`.

4. **عدم تطابق اسم حزمة لوحة الإدارة مع Google Services:**
   - **الواقع في الأدمن:** `applicationId` في Gradle هو `com.aistudio.cinestreamadmin.eftzvl`، بينما العميل المسجل في `google-services.json` هو `com.aistudio.cinestreamadmin.adcxz`.
   - **الأثر:** يتسبب ذلك في فشل مهمة `processDebugGoogleServices` في أدوات البناء التلقائية القياسية ما لم يتم توحيد الاسم.

5. **حدود النشر السحابي الفعلي (`UNVERIFIED DEPLOYMENT BOUNDARY`):**
   - **الواقع:** لا تتوفر أدلة داخل جلسة العمل تثبت أن القواعد المنشورة فعلياً في سحابة Firebase الحية (`cinestream-sulo`) مطابقة لملف القواعد المحلي `firestore.rules` (SHA: `207aade4...`)، حيث لا يُسمح بتنفيذ نشر سحابي دون قرار مشترك.

---

### 5. ملخص النتائج وإقرار الامتثال والحدود

1. **اكتمال استخراج العقود بنجاح:** تم استخراج كافة التفاصيل الميدانية الدقيقة للعقود الثمانية المطلوبة بالاعتماد المباشر على ملفات الكود والنماذج والمستودعات وقواعد Firestore في لوحة الإدارة.
2. **احترام قرار تأجيل التعليقات:** تم الحفاظ الكامل على حالة تعطيل التعليقات المقرة في المرحلة 22.10-B، دون إضافة أي واجهات أو صلاحيات أو عمليات كتابة أو فتح للمسار في القواعد.
3. **الامتثال الصارم للقيود الإلزامية:**
   - لم يتم تعديل أي كود أو ملفات في المشروع.
   - لم يتم نشر أي قواعد أمان إلى سحابة Firebase (`No firebase deploy`).
   - لم يتم حذف أو تغيير أي بيانات أو صلاحيات للمستخدمين.
   - لم يتم تغيير إعدادات Firebase، أو أسماء الحزم، أو شهادات SHA.
   - لم يتم المساس بـ QFilm.
4. **تنويه عدم كفاية الاختبار المحلي:** يؤكد هذا التقرير اجتياز كافة اختبارات كود لوحة الإدارة محلياً بنسبة 100% (142 اختبار وحدة في Kotlin، و182 فحصاً أمنياً في محاكي Firestore)، **دون الادعاء بأن التكامل الميداني الحي مع تطبيق المستخدمين (CineStream User App) قد اختُبر أو تم التحقق منه على خوادم الإنتاج الفعلية**.
