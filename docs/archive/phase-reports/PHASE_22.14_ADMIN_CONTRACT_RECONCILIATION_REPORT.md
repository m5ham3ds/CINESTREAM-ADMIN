# تقرير مراجعة وتوفيق العقود المشتركة — PHASE 22.14
## CineStream Admin Dashboard: Cross-App Contract Reconciliation Audit Report

**تاريخ التدقيق والتوثيق:** 19 أكتوبر 2026  
**معرّف المرحلة:** `PHASE 22.14 — CROSS-APP CONTRACT RECONCILIATION (ADMIN PERSPECTIVE)`  
**طبيعة المرحلة:** مراجعة وتدقيق استقصائي للأدلة الفنية للعقود المشتركة بين `CineStream Admin Dashboard` و `CineStream User App`.  
**القيد المعماري:** مرحلة تدقيق ومطابقة وثائقية فقط — **يُحظر تماماً تعديل كود التطبيق، أو تغيير مخططات Firestore، أو تعديل إعدادات الحزم وFirebase، أو نشر القواعد إلى الإنتاج**.  
**نتيجة تنظيف التوثيق (Part A):** **PASS**  
**نتيجة مراجعة العقود من جانب Admin (Part B):** **PASS WITH LIMITATIONS**  
*(النتيجة مقيدة بحدود جلسة Admin، حيث تظل مطابقة تطبيق المستخدم معلقة بانتظار المقارنة الميدانية مع تقرير جلسة User App المستقلة).*

---

## 1. الملخص التنفيذي (Executive Summary)

تم إنجاز المراجعة الفنية الشاملة لجميع العقود التقنية المشتركة بين تطبيق لوحة الإدارة (`CineStream Admin Dashboard`) وتطبيق المستخدمين (`CineStream User App`)، استناداً حصرياً إلى الأدلة المتاحة داخل مساحة عمل Admin، وبموجب حدود الوصول المستقلة.

### أبرز نتائج المراجعة:
1. **استقرار حوكمة الصلاحيات (RBAC & Authority):** تم إثبات عزل صلاحيات الإدارة بدقة، حيث يستند قرار الإشراف حصرياً إلى بريد المالك الكنسي `sulopros01@gmail.com` والوثيقة الكنسية `/admins/{uid}.enabled == true`، مع إبطال أي تصعيد للصلاحيات عبر حقول المستخدمين.
2. **اتساق آلة حالات طلبات Pro:** نموذج `ProRequest` ومستودع `ProRequestRepository` وقواعد Firestore تدعم آلة حالات حتمية رباعية (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`)، وتتوافق بدقة مع حمولة الإلغاء المصرح بها للعميل (`status, cancelledAt, updatedAt`).
3. **رصد تباين حالات البلاغات (`/reports`):** تبيّن أن لوحة الإدارة تكتب حصرياً حالتي المعالجة بالأحرف الكبيرة: `"RESOLVED"` و `"DISMISSED"`، بينما يُلزم التحقق من جلسة User App للتأكد من عدم وجود تعارض مع حالات الحروف الصغيرة أو مسميات أخرى (مثل `rejected` أو `closed`).
4. **ثبات تأجيل وتعطيل التعليقات (`/comments`):** تم التحقق القاطع من استمرار الالتزام بالقرار المعماري `PHASE 22.10-B`؛ حيث أزيل زر التحكم بالتعليقات من الواجهة، وعُطلت عمليات الكتابة الإدارية لـ `canComment`، ومسار `/comments` محظور تماماً برفض القواعد الافتراضي (`Default Deny`).
5. **توثيق تباين بيئة العمل المؤقتة (Remix vs Production):** تم رصد وضبط التباين بين معرف الحزمة المؤقت للبيئة الحالية `com.aistudio.cinestreamadmin.mwtssd` والمعرّف الكنسي المسجل للإنتاج `com.aistudio.cinestreamadmin.adcxz` والمشروع `cinestream-sulo`، دون إجراء أي تغيير برمجي حفاظاً على سلامة المشروع.

---

## 2. نطاق الملفات والتقارير المفحوصة (Audit Scope)

تم فحص ومقارنة الملفات المصدرية والتقارير التاريخية المتاحة في جلسة Admin:

### أ. ملفات كود لوحة الإدارة المفحوصة مباشرة:
- **العقود والنماذج:**
  - `app/src/main/java/com/example/contract/FirebaseContract.kt` (العقد الكنسي للمجموعات والمسارات v1).
  - `app/src/main/java/com/example/models/Models.kt` (نماذج `User`, `AdminUser`, `UserReport`, `AppConfig`, `AppUpdate`, `NotificationRequest`, `AuditLog`).
  - `app/src/main/java/com/example/models/ProRequest.kt` (نموذج طلبات الترقية وآلة الحالات `ProRequestStatus`).
  - `app/src/main/java/com/example/models/ManagedExtension.kt` (كتالوج الإضافات والمكاشط المدارة).
  - `app/src/main/java/com/example/models/SearchOrderConfig.kt` (تكوين ترتيب البحث وحالات مجموعات الدبلجة).
- **المستودعات ونماذج العرض:**
  - `app/src/main/java/com/example/repository/AdminRepository.kt`
  - `app/src/main/java/com/example/repository/ProRequestRepository.kt`
  - `app/src/main/java/com/example/repository/SearchOrderRepository.kt`
  - `app/src/main/java/com/example/viewmodels/ReportsViewModel.kt`
  - `app/src/main/java/com/example/viewmodels/ProRequestsViewModel.kt`
  - `app/src/main/java/com/example/ui/screens/ReportsScreen.kt`
  - `app/src/main/java/com/example/ui/screens/UserDetailScreen.kt`
- **قواعد الأمان والإعدادات:**
  - `firestore.rules` (قواعد الأمان الموحدة، 810 أسطر).
  - `app/build.gradle.kts` و `app/google-services.json` و `FirebaseInitializer.kt`.

### ب. التقارير المرجعية المشتركة المفحوصة من الأرشيف:
- `docs/archive/phase-history-before-reset/PHASE_22_SHARED_FIREBASE_CONTRACT_AR.md`
- `docs/archive/phase-history-before-reset/PHASE_22.9-A_ADMIN_CONTRACT_INVENTORY_REPORT_AR.md`
- `docs/archive/phase-history-before-reset/PHASE_22.8-H2_ADMIN_SOURCE_OF_TRUTH_REPORT_AR.md`
- `docs/archive/phase-history-before-reset/PHASE_22.10-B_DEFER_COMMENTS_FEATURE_REPORT_AR.md`
- `docs/archive/phase-history-before-reset/PHASE_22.13-B_FIREBASE_PACKAGE_ALIGNMENT_AND_BUILD_VERIFICATION_REPORT.md`

---

## 3. مصفوفة مراجعة وتوفيق العقود المشتركة (Cross-App Reconciliation Matrix)

| # | المجال | دليل Admin الحالي | دليل User App المتاح | نتيجة المطابقة | النقص أو الخطر | الإجراء المقترح |
|---|---|---|---|:---:|---|---|
| **1** | **A. حالات البلاغات وحقولها** (`/reports`) | • النموذج: `UserReport` (`status = "PENDING"`).<br>• الواجهة: خياران للمعالجة `"RESOLVED"` و `"DISMISSED"`.<br>• المستودع يكتب الحالة بالأحرف الكبيرة.<br>• القواعد تسمح بإنشاء `PENDING` أو `pending` للعميل، وتحديث المشرف حصراً. | وثائق المرحلة 22.8 تشير إلى أن العميل ينشئ بـ `PENDING` أو `pending`. لم يُعثر في ملفات Admin على تعريف كود Kotlin المقابل لـ `Report` في User App. | **UNVERIFIED** | خطر عدم تعرف User App على الحالات الكبيرة `"RESOLVED"` أو `"DISMISSED"` إذا كان يعتمد أحرفاً صغيرة (`resolved`) أو مسمى بديل (`rejected`, `closed`). | طلب استخراج نموذج `Report` وحالاته المعتمدة من جلسة User App لتوحيد القيم النصية وحالة الأحرف. |
| **2** | **B. طلبات Pro والاعتماد والإلغاء** (`/pro_requests`) | • النموذج: `ProRequest` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`).<br>• الاعتماد يحدّث المستخدم ذرياً (`subscriptionTier`, `planId`, `durationDays`, `subscriptionStatus="ACTIVE"`, `subscriptionSource="MONEY"`).<br>• الإلغاء مقيد بالقواعد للمالك وللحقول الثلاثة: `status, cancelledAt, updatedAt`. | العقد المشترك 22.6-B يثبت تطابق مخطط الإلغاء (`CANCELLED`) وتحديث الحقول الثلاثة، ويدعم كلا الطرفين `planId` و`durationDays` و`paymentReference`. | **MATCH** | خطر ضئيل: هل يدعم User App باقة `PRO_LITE` أم يطلب `PRO` فقط؟ | طلب تأكيد باقات الاشتراك المدعومة في واجهة إنشاء الطلبات في User App. |
| **3** | **C. مسارات Firestore وصلاحيات RBAC** | • القواعد تحدد 24 مساراً معيارياً وتمنع المسارات المهملة والتعليقات بنظام `Default Deny`.<br>• حماية المالك `isOwnerAccount()`.<br>• حظر تعديل 45 حقلاً حساساً بدالة `modifyingSensitiveUserFields()`. | بصمة قواعد Admin تختلف عن بصمة قواعد User App التاريخية (`207aade4...` مقابل `54c8570a...`). لم يتم نشر القواعد للسحاب. | **UNVERIFIED** | عدم تطابق ملفي القواعد محلياً قد يؤدي لتباين سلوك الصلاحيات في حال نشر أي منهما دون توحيد. | عدم نشر القواعد للإنتاج، ومطابقة ملفي `firestore.rules` في جلسة توحيد لاحقة قبل أي نشر. |
| **4** | **D. ترتيب البحث ومصادر الدبلجة** (`/config/search_order`) | • الوثيقة Singleton: `/config/search_order`.<br>• الفئات: `movie`, `tv` (بديل: `series`), `anime`, `arabicDubbed` (بديل: `arabicDub`, `arabic_dub`), `englishDubbed` (بديل: `englishDub`, `english_dub`).<br>• `dubbingConfig`: حالة الدبلجة العربية (`ACTIVE`) والإنجليزية (`MAINTENANCE`).<br>• الإضافات كائنات نقية (Zero Bytecode). | العقد 22.6-B يؤكد اعتماد User App على القراءة من `/config/search_order` لترتيب مكاشطه الداخلية. الكتابة المزدوجة للأدمن تضمن دعم التسميات البديلة. | **MATCH** | كيفية تعامل User App مع المحتوى المدبلج مقابل المترجم عند تشابه عناوين TMDB غير مرئية في ملفات Admin. | طلب توثيق آلية الفلترة والعرض الداخلي لنتائج الدبلجة من جلسة User App. |
| **5** | **E. هوية Firebase والحزم المسجلة** | • في `FirebaseInitializer.kt`: Project: `cinestream-sulo`، Number: `979447256418`، Admin App ID: `...ea5b570266ae1aa6a9883b`.<br>• في البيئة الحالية: `mwtssd` و `remixed-project-id`. | تقرير 22.13-B يوثق أن User App مسجل كـ `com.aistudio.cinestream.xyzabc` على نفس المشروع `cinestream-sulo`. | **MISMATCH** | تباين معرّف الحزمة وبيانات Firebase في بيئة العمل المؤقتة المعاد توليدها مقارنة بالقيم الإنتاجية الأصلية. | الإبقاء التام على الملفات الحالية دون تعديل، وتوثيق الفجوة ليتم حسم التوقيع والهوية عند الانتقال للإنتاج. |
| **6** | **F. بقاء التعليقات معطلة** (`/comments`) | • قرار 22.10-B مطبق بالكامل.<br>• واجهة تفاصيل المستخدم خالية من زر التعليق.<br>• المستودع والـ ViewModel يحظران كتابة `canComment`.<br>• مسار `/comments` محظور بالقواعد تلقائياً. | تقرير 22.10-B المشترك يؤكد الاتفاق الثنائي على تأجيل الميزة بالكامل حتى إشعار آخر. | **MATCH** | أي محاولة قراءة أو كتابة من User App على `/comments` ستُرفض فورا بـ `PERMISSION_DENIED`. | التحقق في جلسة User App من أن ميزة التعليقات معطلة برمجياً ولا ترسل طلبات شبكية. |

---

## 4. الأدلة الفنية التفصيلية من جانب Admin Dashboard

### أ. مجال البلاغات (`/reports`):
1. **النموذج (`Models.kt` السطور 384-398):**
   ```kotlin
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
2. **الواجهة (`ReportsScreen.kt` السطور 85-103):**
   - الخيارات المتاحة للمشرف: `"RESOLVED"` و `"DISMISSED"`.
   - الفلترة في القائمة: `"ALL"`, `"PENDING"`, `"RESOLVED"`, `"DISMISSED"`.
3. **المستودع (`AdminRepository.kt` السطور 1558-1573):**
   - يكتب: `"status" to status` (حيث status تكون `"RESOLVED"` أو `"DISMISSED"` حصراً)، و `"resolutionNotes"`, `"resolvedAt"`, `"resolvedBy"`.
4. **القواعد (`firestore.rules` السطور 553-575):**
   - تسمح للعميل بإنشاء البلاغ فقط إذا كان `status == 'PENDING' || status == 'pending'` وبشرط أن تكون حقول المعالجة فارغة أو معدومة.
   - التحديث والحذف محصوران بالمشرفين (`allow update, delete: if isAdmin();`).

### ب. مجال طلبات Pro والاعتماد والإلغاء (`/pro_requests`):
1. **آلة الحالات (`ProRequest.kt` السطور 12-29):**
   - الحالات المعرفة: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.
   - في حال ورود قيمة غير معروفة، يتراجع النظام حتمياً إلى `PENDING` (صمام أمان يمنع اعتبار أي حالة مجهولة كـ `APPROVED`).
2. **الاعتماد الذري الموحد (`ProRequestRepository.kt` السطور 113-201):**
   - يتم الاعتماد عبر معاملة ذرية `runTransaction` تفحص أولاً أن حالة الطلب الحالية هي `PENDING`.
   - تحديث وثيقة المستخدم في `/users/{userId}`:
     - `subscriptionTier`: `"PRO"` أو `"PRO_LITE"`.
     - `planId`: `"pro_30d"`, `"pro_lite_7d"`, إلخ.
     - `subscriptionStatus`: `"ACTIVE"`.
     - `subscriptionSource`: `"MONEY"`.
     - `subscriptionReferenceId`: معرف الطلب `requestId`.
     - `isPremium = true`, `isPro = true`.
     - تمديد الصلاحية: إذا كان لدى المستخدم اشتراك نشط (`currentExpiresAt > now`)، يتم التمديد فوق التاريخ القائم تراكمياً.
   - تحديث وثيقة الطلب: `status = "APPROVED"`, `reviewedAt = now`, `reviewedBy = adminUid`.
3. **الإلغاء من طرف العميل (`firestore.rules` السطور 537-545):**
   - مصرح فقط لمالك الطلب (`resource.data.userId == request.auth.uid`).
   - يشترط أن تكون الحالة السابقة `PENDING` أو `pending`.
   - يشترط أن تكون الحالة الجديدة حصرياً `CANCELLED`.
   - يشترط ألا تتعدى الحقول المعدلة: `['status', 'updatedAt', 'cancelledAt']`.

### ج. مسارات Firestore وصلاحيات RBAC:
1. **مسار المشرفين (`/admins/{uid}`):**
   - الترقية والتعديل محصورة بمالك المشروع الكنسي `sulopros01@gmail.com` عبر `isOwnerAccount()`.
   - المشرفون العاديون يملكون صلاحية القراءة فقط.
2. **مسار المستخدمين (`/users/{uid}`):**
   - حظر العميل من تعديل 45 حقلاً حساساً تشمل الاشتراكات، الصلاحيات، الحظر، ورصيد النقاط بدالة `modifyingSensitiveUserFields()`.
3. **دفتر حركات النقاط (`/users/{uid}/point_transactions`):**
   - سجل حركات غير قابل للتعديل (Immutable Ledger)، حظر تام على كتابة العميل (`allow write: if isAdmin();`).
4. **مسار التعليقات (`/comments`):**
   - غير موجود في القواعد، ويخضع لحظر الرفض التلقائي العام في السطر 805 (`allow read, write: if false;`).

### د. ترتيب البحث ومصادر الدبلجة (`/config/search_order`):
1. **النموذج (`SearchOrderConfig.kt`):**
   - الفئات المستقلة: `movie`, `tv` (مع دعم `series`), `anime`, `arabicDubbed` (مع دعم `arabicDub` و `arabic_dub`), `englishDubbed` (مع دعم `englishDub` و `english_dub`).
   - حالة الدبلجة: `arabicDubbedStatus` (الافتراضي `ACTIVE`), `englishDubbedStatus` (الافتراضي `MAINTENANCE`).
2. **الكتابة المزدوجة التوافقية (`toMap()` السطور 168-186):**
   - يتم كتابة التسميات القياسية والتسميات البديلة ذرياً معاً في نفس الوثيقة لضمان ألا يتأثر أي إصدار قديم أو جديد من User App.

### هـ. هوية Firebase والحزم:
- الثوابت الكنسية الإنتاجية محفوظة في `FirebaseInitializer.kt`:
  - `Project ID: cinestream-sulo`
  - `Project Number: 979447256418`
  - `Admin App ID: 1:979447256418:android:ea5b570266ae1aa6a9883b`
- الحزمة المسجلة في تقرير 22.13-B هي `com.aistudio.cinestreamadmin.adcxz`.
- المعرف في البيئة الحالية المؤقتة هو `com.aistudio.cinestreamadmin.mwtssd`.

### و. بقاء التعليقات معطلة:
- واجهة `UserDetailScreen.kt` لا تعرض أي مفتاح للتعليقات.
- `AdminRepository.updateFeaturePermission` يعترض ويوقف أي محاولة لتعديل `"comment"` أو `"cancomment"`.
- `Models.kt` يبقي على `val canComment: Boolean = true` فقط لعدم كسر توافق قراءة الوثائق التاريخية في Firestore.

---

## 5. الفروقات المثبتة والبنود غير المحسومة (Discrepancies & Unresolved Items)

1. **التباين الفعلي الأول (حالات البلاغات):**
   - **الواقع في Admin:** الحالات المكتوبة هي `"RESOLVED"` و `"DISMISSED"` (أحرف كبيرة).
   - **غير المحسوم في User App:** هل يعرض تطبيق المستخدم البلاغات بناءً على مقارنة دقيقة للأحرف الكبيرة، أم يتوقع حالات بالحروف الصغيرة (`resolved`)، أم يتوقع حالات أخرى (`in_progress`, `closed`)؟
2. **التباين الفعلي الثاني (هوية الحزمة وبيئة العمل):**
   - **الواقع في ملفات Gradle و JSON الحالية:** `applicationId = "com.aistudio.cinestreamadmin.mwtssd"`، و `project_id = "remixed-project-id"`.
   - **الواقع الكنسي الإنتاجي:** `applicationId = "com.aistudio.cinestreamadmin.adcxz"`، و `project_id = "cinestream-sulo"`.
   - هذا التباين ناتج عن استنساخ البيئة السحابية المؤقتة، وتم التعامل معه بالتجميد الكامل دون تعديل.
3. **البند غير المحسوم الثالث (بصمة قواعد Firestore):**
   - لا تتوفر أدلة على القواعد المنشورة فعلياً في سحابة Production، وتوجد بصمتان مختلفتان لملفي القواعد في المستودعين.

---

## 6. التحقق من الضوابط والمحددات الأمنية (Safety Verification)

- [x] **بقاء التعليقات معطلة:** مؤكد 100%، لا توجد واجهات أو مستودعات أو عمليات كتابة نشطة للتعليقات، والمسار محظور في القواعد.
- [x] **عدم تعديل الحزم أو إعدادات البناء:** مؤكد 100%، لم يتم تعديل أي ملف Gradle أو `applicationId` أو `namespace`.
- [x] **عدم تعديل `google-services.json`:** مؤكد 100%، الملف لم يمس.
- [x] **عدم تعديل أو نشر قواعد Firestore:** مؤكد 100%، لم يتم استدعاء أدوات النشر أو تعديل `firestore.rules`.
- [x] **عدم الترحيل إلى Cloudflare Workers:** مؤكد 100%، Firebase تظل الخلفية الحصرية المعتمدة.

---

## 7. قائمة البيانات والمعلومات المطلوبة من جلسة User App (Requested Evidence)

لمواصلة عملية التوفيق الثنائي وإعداد خطة الإصلاح الموحدة، يلزم تزويدنا من جلسة CineStream User App بالبيانات التالية:
1. **نموذج البلاغات (`Report` / `UserReport`):**
   - أسماء الحقول الدقيقة وأنواعها.
   - القيم النصية لحالات البلاغ وحالة الأحرف المقبولة (هل هي `PENDING/RESOLVED/DISMISSED` أم حروف صغيرة)؟
   - هل يمتلك المستخدم واجهة لمتابعة ردود المشرف (`resolutionNotes`)؟
2. **نموذج طلبات الترقية (`ProRequest`):**
   - الحقول المرسلة عند إنشاء الطلب وإلغائه.
   - هل يتم إرسال `planId` كـ `pro_30d` أو `pro_lite_7d`، أم أسماء أخرى؟
3. **محرك البحث ومصادر الدبلجة:**
   - المفاتيح التي يقرأها تطبيق المستخدم من وثيقة `/config/search_order` (هل يقرأ `arabicDubbed` أم `arabicDub` أم `arabic_dub`)؟
   - كيفية تصنيف وتمييز السيرفرات ومسارات الصوت المدبلجة والمترجمة.
4. **ملف `google-services.json` وإعدادات البناء في User App:**
   - تأكيد `package_name` المسجل (`com.aistudio.cinestream.xyzabc`).
   - تأكيد تطابق `project_id` (`cinestream-sulo`) و `project_number` (`979447256418`).
5. **حالة ميزة التعليقات في User App:**
   - تأكيد إزالة أو تجميد أي واجهات لكتابة التعليقات على الوسائط والأفلام.

---

## 8. توصيات للمرحلة اللاحقة (Next Phase Recommendations)

*توصيات استرشادية فقط — لا يتم تنفيذها في هذه المرحلة:*
1. **عقد جلسة مطابقة ثنائية (Cross-Project Reconciliation Phase):** مقارنة نتائج هذا التقرير مباشرة مع التقرير المستخرج من جلسة User App.
2. **توحيد دلالة حالات البلاغات:** إذا تبين أن User App يتوقع أحرفاً صغيرة، يتم اعتماد القراءة والكتابة المزدوجة المتسامحة (Case-Insensitive Handling).
3. **توحيد وتثبيت ملف `firestore.rules`:** دمج القواعد في ملف موحد نهائي معتمد ببصمة تشفير واحدة قبل أي نشر إنتاجي.
4. **تثبيت حزم التطبيقات في بيئة الإنتاج:** مطابقة شهادات التوقيع وحزم Gradle مع عملاء Firebase المعتمدين في وثيقة المشروع.

---

## 9. النتيجة النهائية والتقييم الختامي (Final Assessment)

1. **مهمة تنظيف التوثيق والأرشفة وإعادة التعيين (PART A):**  
   **النتيجة:** **PASS**  
   *(تمت الأرشفة الكاملة لـ 38 وثيقة، ونُظفت مساحة العمل، وهُيئت مساحة المراحل النشطة لتبدأ من PHASE 1 دون أي خرق لسلامة الكود).*

2. **مهمة مراجعة العقود المشتركة من جانب لوحة الإدارة (PART B):**  
   **النتيجة:** **PASS WITH LIMITATIONS**  
   *(تم استخراج كافة الأدلة بدقة تامة من جانب Admin، وتوثيق الفروقات المحتملة مع User App. ومُنحت النتيجة بحدود القيود لعدم توفر الوصول المباشر لكود User App في هذه الجلسة).*
