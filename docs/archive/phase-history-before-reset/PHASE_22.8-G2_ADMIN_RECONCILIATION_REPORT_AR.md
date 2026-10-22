# تقرير المطابقة المشتركة للقرارات — PHASE 22.8-G2
## Admin Joint Reconciliation Report: CineStream Ecosystem

**تاريخ التوثيق والتدقيق:** 18 أكتوبر 2026  
**المرحلة:** `PHASE 22.8-G2 — ADMIN JOINT RECONCILIATION`  
**المرجع الأساسي للمقارنة:**
1. تقرير التحقق النهائي للوحة الإدارة: `PHASE_22.8-F2_ADMIN_FINAL_VERIFICATION_REPORT_AR.md`
2. تقرير التحقق النهائي لتطبيق المستخدم: `PHASE_22.8-F1_USER_APP_FINAL_VERIFICATION_REPORT_AR.md`  
**طبيعة المرحلة:** تدقيق تحليلي ومطابقة توثيقية للقرارات المعمارية المشتركة فقط، وليست مرحلة تنفيذ إصلاحات أو تعديلات كودية.

---

### المراجع المعتمدة للمشروع:
- **Firebase Project ID:** `cinestream-sulo`
- **Project Number:** `979447256418`
- **حزمة تطبيق المستخدمين المسجلة أصلًا:** `com.aistudio.cinestream.xyzabc`
- **حزمة لوحة الإدارة المسجلة أصلًا:** `com.aistudio.cinestreamadmin.adcxz`

---

## 1. تحليل سبب فشل مهمة `processDebugGoogleServices` والخيارات الهندسية المتاحة

### أ. التشخيص الدقيق بالأدلة المباشرة:
أظهرت الاختبارات الحية التي نُفذت في المرحلة F2 فشل أدوات البناء والتصريف التلقائي (`compile_applet` و `lint_applet` و `./gradlew testDebugUnitTest`) عند خطوة واحدة محددة:
```text
Execution failed for task ':app:processDebugGoogleServices'.
> No matching client found for package name 'com.aistudio.cinestreamadmin.eftzvl' in /app/google-services.json
```

#### الأدلة الفنية القطعية:
1. **في `app/build.gradle.kts` (السطر 14):**
   - حقل `applicationId` مضبوط على: `"com.aistudio.cinestreamadmin.eftzvl"`.
   - هذا المعرّف يمثل اسماً مؤقتاً تم توليده تلقائياً بواسطة بيئة استضافة Google AI Studio عند استيراد المشروع.
2. **في `app/google-services.json` (العملاء المسجلون):**
   - **العميل الأول (User App):** `package_name: "com.aistudio.cinestream.xyzabc"`, App ID: `1:979447256418:android:037a9b3a1393e550a9883b`.
   - **العميل الثاني (Admin App):** `package_name: "com.aistudio.cinestreamadmin.adcxz"`, App ID: `1:979447256418:android:ea5b570266ae1aa6a9883b`.
3. **آلية عمل الإضافة (Google Services Plugin Mechanism):**
   - تقوم أداة `processDebugGoogleServices` بمطابقة `applicationId` الخاص بالبناء مع قائمة العملاء في `google-services.json`.
   - نظراً لعدم وجود عميل مسجل باسم `com.aistudio.cinestreamadmin.eftzvl`، تفشل المهمة وتوقف عملية التجميع والتصريف بالكامل.
4. **دليل صحة الكود وسلامته الذاتية:**
   - عند عزل هذه المهمة المحددة وتشغيل:
     `./gradlew testDebugUnitTest -x :app:processDebugGoogleServices`
   - تم تجميع الكود واجتياز **141 اختبار وحدة بنسبة 100% دون أي خطأ كودي أو تعارض في التبعيات**.
   - كما يحتوي `FirebaseInitializer.kt` بالفعل على آلية تراجع صريحة تعتمد على العميل الأصلي (`FALLBACK_APP_ID = "1:979447256418:android:ea5b570266ae1aa6a9883b"`).

---

### ب. توثيق الخيارات الممكنة لمعالجة التعارض (دون تطبيق أي منها):

* **الخيار الأول (المطابقة مع التسجيل الأصلي المعتمد - Recommended):**
  - تغيير قيمة `applicationId` في `app/build.gradle.kts` من `com.aistudio.cinestreamadmin.eftzvl` إلى `com.aistudio.cinestreamadmin.adcxz`.
  - **المزايا:** تطابق فوري ومباشر مع عميل لوحة الإدارة المسجل في `google-services.json` دون الحاجة لأي تعديل في Firebase Console، واستعادة عمل `compile_applet` و`processDebugGoogleServices` فوراً.
  - **المحاذير:** يتطلب تثبيت هذا الاسم رسمياً في مستودع الكود.

* **الخيار الثاني (تسجيل العميل المؤقت في Firebase Console):**
  - الإبقاء على `applicationId = "com.aistudio.cinestreamadmin.eftzvl"` في كود التطبيق، وإضافة تطبيق Android جديد في Firebase Console لنفس المشروع (`cinestream-sulo`) باسم الحزمة هذا، ثم تنزيل ملف `google-services.json` محدث يتضمن العملاء الثلاثة.
  - **المزايا:** عدم المساس بملف `build.gradle.kts`.
  - **المحاذير:** تراكم تسجيلات غير ضرورية في مشروع Firebase، واعتماد اسم حزمة عشوائي مولّد مؤقتاً.

* **الخيار الثالث (إدارة التجاوز على مستوى مهام Gradle):**
  - تعديل إعدادات مهمة Google Services في Gradle لتجاوز الخطأ تلقائياً في بيئة التطوير والاعتماد على التراجع اليدوي الموجود في `FirebaseInitializer.kt`.
  - **المزايا:** حل داخلي برمجي للبناء.
  - **المحاذير:** لا يُعتبر ممارسة قياسية لبيئات الإنتاج النهائية.

---

## 2. مقارنة مسار التعليقات بين تقرير F1 والواقع الفعلي في مشروع الإدارة

| وجه المقارنة | ما تم رصده في تقرير F1 ومحيط تطبيق المستخدمين | الواقع الفعلي في مشروع لوحة الإدارة (تقرير F2) | التقييم الفني والأثر التشغيلي |
|---|---|---|---|
| **الطلب والمفهوم الوظيفي** | يطرح تطبيق المستخدمين ميزة التعليقات على الأفلام والمسلسلات كخاصية اجتماعية تفاعلية. | تدعم واجهة الإدارة (`UserDetailScreen.kt`) زراً لتبديل حق التعليق (`canCommentDesc: "السماح بالتعليق على الأفلام والمسلسلات"`). | توجد حوكمة إدارية لصلاحية التعليق، ولكن لا توجد حاوية بيانات للميزة. |
| **راية الصلاحية في وثيقة المستخدم** | يتوقع قراءة صلاحية المستخدم للتعليق. | وثيقة المستخدم `/users/{uid}` تحتوي على `canComment: Boolean = true`، وهي محمية بدالة `modifyingSensitiveUserFields()` في `firestore.rules`. | **ALIGNED:** راية الصلاحية معرّفة ومحمية برمجياً وأمنياً من تعديل العميل. |
| **مسار التخزين في `FirebaseContract.kt`** | يُطرح التعامل مع مسار تعليقات (مثل `/comments` أو مسار فرعي). | **غائب تماماً:** كائن `FirebaseCollections` لا يحتوي على أي ثابت باسم `COMMENTS` أو `comments`. | **DISCREPANCY:** لا يوجد مسار معتمد في العقد البرمجي للوحة الإدارة. |
| **قواعد أمان Firestore (`firestore.rules`)** | يفترض وجود قواعد تسمح بالقراءة والكتابة للمستخدمين المصرح لهم. | **غائب تماماً:** لا يوجد أي `match /comments/{commentId}` ولا أي مسار فرعي للتعليقات. | **CRITICAL BLOCKER:** تخضع أي قراءة أو كتابة على مسار التعليقات لقاعدة الرفض الافتراضية `allow read, write: if false;`. |

### الاستنتاج المباشر لمسار التعليقات:
ميزة التعليقات في بيئة Cloud Firestore الحالية **معطلة تماماً على مستوى خادم وقواعد البيانات**. لا يمكن لأي مستخدم إضافة أو استرجاع تعليق من Firestore دون إضافة مسار صريح وقواعد أمان محكمة في `firestore.rules` وتوثيقه في `FirebaseContract.kt`.

---

## 3. العقود التي تتطلب قراراً معمارياً موحداً

### أ. عقد مسار وهيكل التعليقات (`/comments` Contract):
يتطلب حسم الخيارات التالية:
1. **نمط المسار:**
   - هل هو مسار منبسط عام: `/comments/{commentId}` (مع حقول `mediaId`, `mediaType`)؟
   - أم مسار فرعي شجري: `/media/{mediaId}/comments/{commentId}`؟
2. **مخطط مستند التعليق (Schema Fields):**
   - `id: String`, `mediaId: String`, `userId: String`, `username: String`, `userAvatar: String?`, `text: String`, `createdAt: Long`, `updatedAt: Long`, `isEdited: Boolean`, `likesCount: Long`, `isDeleted: Boolean`.
3. **قواعد الأمان المقترحة للتوحيد:**
   - **القراءة:** متاحة للمستخدمين الموثقين `allow read: if isAuthenticated();`.
   - **الإنشاء:** مشروط بأن يكون الكاتب هو صاحب المعرف، وغير محظور، ويمتلك الصلاحية:
     ```javascript
     allow create: if isAuthenticated() &&
       request.resource.data.userId == request.auth.uid &&
       get(/databases/$(database)/documents/users/$(request.auth.uid)).data.canComment != false &&
       get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isBanned != true;
     ```
   - **التعديل والحذف:** مقيد بصاحب التعليق (لتعديل النص فقط) أو المشرف (`isAdmin()`) للحذف الإداري والرقابي.

---

### ب. عقد البلاغات والشكاوى (`/reports` Contract):
1. **المسار المعتمد في الإدارة:** `/reports/{reportId}`.
2. **المخطط المعتمد في الإدارة:**
   - الحقول: `id`, `userId`, `userEmail`, `type` (`ISSUE`, `STREAM`, `BUG`, `CONTENT`), `targetType`, `targetId`, `title`, `description`, `status` (`PENDING`, `RESOLVED`, `DISMISSED`), `resolutionNotes`, `createdAt`, `resolvedAt`, `resolvedBy`.
3. **القرار الموحد المطلوب:**
   - إلزام تطبيق المستخدمين بإنشاء البلاغات بالحالة `PENDING` أو `pending` حصراً، وحظر إرسال أي حقول إشرافية (`resolvedBy`, `resolvedAt`, `resolutionNotes`) عند الإنشاء لتفادي الرفض الفوري بالقواعد.
   - حصر صلاحية تعديل الحالة أو كتابة ملاحظات الحل أو الحذف بالمشرفين (`isAdmin()`).

---

### ج. عقد الصلاحيات وفصل الأدوار (RBAC & Feature Isolation):
1. **فصل الاشتراك التجاري عن الصلاحيات الفنية:**
   - تأكيد القاعدة الدستورية: **اشتراك Pro هو لإزالة الإعلانات فقط (`REMOVE_ADS ONLY`)** ولا يمنح استثناءات لحظر الحساب أو فتح صلاحيات تقنية مقيدة.
2. **حصانة المالك:**
   - ترقية المشرفين محصورة بالمالك الأصلي (`sulopros01@gmail.com`) عبر `isOwnerAccount()`.
3. **حماية الحقول الحساسة:**
   - استمرار حظر 45 حقلاً حساساً على تعديل العميل في `/users/{uid}`.

---

## 4. الفصل المنهجي بين نتائج الاختبارات الفعلية والنتائج المنقولة

التزاماً بالشفافية الهندسية الصارمة، يتم هنا التمييز القاطع بين ما تم تنفيذه وإثباته فعلياً داخل هذه الجلسة، وما تم نقله من التقرير الآخر:

### أ. نتائج الاختبارات التي أعاد مشروع لوحة الإدارة تنفيذها فعلياً ومباشراً:
1. **اختبارات وحدة Kotlin في بيئة أندرويد (`testDebugUnitTest`):**
   - **المنفذ فعلياً:** 141 اختباراً عبر Gradle.
   - **النتيجة الحية:** **141 ناجحاً / 0 فاشل بنسبة 100%**.
   - **الأجنحة المنفذة:**
     * `CineStreamAdminLogicTest`: 105 اختبارات لمنطق الأعمال، وحسابات النقاط، وتوافق طلبات Pro، والحظر.
     * `SearchOrderArchitectureTest`: 32 اختباراً لهيكل ترتيب البحث.
     * `ExampleRobolectricTest`: اختباران للمكونات.
     * `FirebaseTest`: اختبار ربط.
     * `ExampleUnitTest`: اختبار وحدة.
2. **فحوصات محاكي قواعد Firestore (Firebase Local Emulator Suite):**
   - **المنفذ فعلياً:** 182 فحصاً أمنياً محلياً تم تشغيلها بواسطة Firebase CLI.
   - **النتيجة الحية:** **182 فحصاً ناجحاً / 0 فاشل بنسبة 100%**.
   - **الأجنحة المنفذة:**
     * `firestore-rules.test.js` (Mocha): 114 فحصاً أمنياً.
     * `phase05b-comprehensive-rules.test.js`: 32 فحصاً أمنياً.
     * `phase03a1-rules.test.js`: 36 فحصاً أمنياً.
3. **فحص أدوات البناء:**
   - تنفيذ `compile_applet` و`lint_applet` ورصد فشلهما الفعلي الحي عند مهمة `:app:processDebugGoogleServices`.

---

### ب. النتائج المنقولة من تقرير F1 / تقارير تطبيق المستخدم (دون إعادة تنفيذ محلية):
1. **اختبارات تطبيق المستخدمين (User App Test Suites):**
   - اختبارات تدفقات مشغل الفيديو والمكتبة والواجهات الخاصة بتطبيق المستخدم.
   - اختبارات استدعاء `PointsRepository.cancelProRequest` في كود تطبيق المستخدم.
2. **بصمة قواعد Firestore لتطبيق المستخدمين:**
   - البصمة المنقولة: `54c8570a25ece1fb6a57bfb0aa2560a50202240a30d4a4cf7a8afd694a18d5ef` (تم رصدها في تقارير المقارنة دون فحص ملف تطبيق المستخدم المحلي).
3. **ملفات مصدر تطبيق المستخدمين:**
   - لم يتم فحص أي ملف مصدري خارج مساحة عمل لوحة الإدارة الحالية.

---

## 5. جدول القرارات المشتركة المطلوبة والأدلة والمسائل المعلقة

| # | المسألة المعمارية | القرار المشترك المطلوب | الأدلة الفنية المؤيدة | ما لا يزال غير محسوم (المعلق) |
|---|---|---|---|---|
| **1** | **معرّف حزمة لوحة الإدارة (`applicationId`)** | اعتماد `com.aistudio.cinestreamadmin.adcxz` كاسم حزمة رسمي للوحة الإدارة. | - مسجل بالاسم ذاته في `google-services.json` الأصلي.<br>- معرف بالاسم ذاته كـ Fallback في `FirebaseInitializer.kt`.<br>- يحل فشل `processDebugGoogleServices` فوراً. | الموعد المحدد لتطبيق تعديل `app/build.gradle.kts` في بيئة العمل المشتركة. |
| **2** | **مسار تخزين التعليقات** | إنشاء مسار موحد للتعليقات وقواعد أمان ملحقة في `firestore.rules`. | - وجود راية `canComment` في حساب المستخدم.<br>- وجود عنصر تحكم إداري في `UserDetailScreen.kt`.<br>- غياب المسار حالياً يسبب `PERMISSION_DENIED`. | هل المسار المعتمد هو `/comments/{id}` المنبسط أم مسار شجري فرعي تحت الوسائط؟ |
| **3** | **مخطط البلاغات (`/reports`)** | اعتماد المخطط الموحد وتجريد حمولة إنشاء العميل من حقول الحل. | - تطابق نموذج `UserReport` في الإدارة.<br>- حماية القواعد لحقول `resolvedBy`, `resolvedAt`, `resolutionNotes`. | التأكد من التزام تطبيق المستخدمين بقيم الحالات الصغرى/الكبرى وعدم إرسال حقول غير مصرحة. |
| **4** | **إلغاء طلبات Pro** | تثبيت الحمولة الثلاثية المعيارية (`status: 'CANCELLED'`, `cancelledAt`, `updatedAt`). | - مدعومة في `firestore.rules` (سطر 537).<br>- مدعومة في `ProRequest.kt`.<br>- مجازة بـ 11 فحصاً في المحاكي. | **محسومة بالكامل:** لا توجد نقاط خلاف معلقة حول آلية الإلغاء. |
| **5** | **إجماليات النقاط ودلالة الصفر** | اعتماد الكتابة المزدوجة الذرية وقراءة الصفر الحقيقي دون استبدال. | - مطبقة في `AdminRepository.adjustUserPoints`.<br>- معرفة في `Models.kt` بـ `effectivePointsEarned`.<br>- مجازة باختبارات وحدة Kotlin. | **محسومة بالكامل:** التوافق التنازلي تام بين التسميتين. |
| **6** | **حدود مشروع Firebase والنشر السحابي** | الإبقاء على تصنيف الربط السحابي كـ `UNVERIFIED` حتى التحقق المباشر من السحابة. | - لكل مشروع ملف `firestore.rules` وبصمة مستقلة.<br>- عدم تنفيذ `firebase deploy` محلياً. | التحقق الميداني من مطابقة القواعد المنشورة فعلياً في سحابة Firebase الحية. |

---

## 6. الخلاصة والتوصيات للمرحلة التالية

1. **الالتزام بالحدود:**
   - لم يتم إجراء أي تعديل على كود التطبيق أو قواعد Firestore أو ملفات التكوين.
   - لم يتم تغيير أسماء الحزم.
   - لم يتم تنفيذ أي نشر سحابي أو ربط مع أي خدمات وسيطة مثل Cloudflare.
2. **جاهزية لوحة الإدارة:**
   - لوحة الإدارة من الناحية المنطقية والتعاقدية محصنة وجاهزة للانتقال للمرحلة النهائية بمجرد حسم القرارات المعلقة في الجدول أعلاه مع الفريق المطور لتطبيق المستخدمين.
3. **التوقف الفوري:**
   - تم إيقاف العمل عند هذه المرحلة بانتظار التوجيهات اللاحقة.
