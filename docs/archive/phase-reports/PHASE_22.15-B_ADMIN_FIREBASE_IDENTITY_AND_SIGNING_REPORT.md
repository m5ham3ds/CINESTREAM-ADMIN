# PHASE 22.15-B — تقرير استعادة هوية Firebase ومفتاح التصحيح الكنسي
## CineStream Admin Dashboard: Canonical Firebase Identity & Debug Signing Restoration

**تاريخ التنفيذ والتوثيق:** 19 أكتوبر 2026  
**البيئة التشغيلية:** Google AI Studio Build (Android Runtime)  
**معرّف المرحلة:** `PHASE 22.15-B — ADMIN DASHBOARD IDENTITY & SIGNING RESTORATION`  
**الحالة العامة للمرحلة:** **PARTIALLY COMPLETED / BLOCKED ON OFFICIAL GOOGLE-SERVICES.JSON**  
*(تمت استعادة مفتاح التصحيح الكنسي بنسبة 100%، ومواءمة `applicationId` بنسبة 100%، ونجاح بناء وتوقيع APK واختبارات الوحدة، وتوقفت جزئية ملف Firebase الرسمي وسُجلت كـ `BLOCKED` التزاماً بالشرط الصارم بحظر إنشاء JSON يدوياً في انتظار تزويد الملف الرسمي).*

---

## 1. جدول مقارنة القيم الكنسية المعتمدة (Before vs After)

| المحور / الخاصية | القيمة الكنسية المستهدفة | القيمة الفعلية قبل التنفيذ | القيمة الفعلية بعد التنفيذ | حالة المطابقة |
|---|---|---|---|:---:|
| **معرف التطبيق (`applicationId`)** | `com.aistudio.cinestreamadmin.adcxz` | `com.aistudio.cinestreamadmin.mwtssd` | `com.aistudio.cinestreamadmin.adcxz` | **MATCH (100%)** |
| **المجال البرمجي (`namespace`)** | `com.example` | `com.example` | `com.example` | **PRESERVED** |
| **بصمة SHA-1 لمفتاح التصحيح** | `54:A8:14:FB:04:DE:BD:98:3F:33:6E:F8:2E:87:82:0E:DB:76:0F:12` | `CB:4C:B4:9C:AB:19:12:B4:9B:43:35:79:25:93:74:F3:3F:F8:76:72` | `54:A8:14:FB:04:DE:BD:98:3F:33:6E:F8:2E:87:82:0E:DB:76:0F:12` | **MATCH (100%)** |
| **بصمة SHA-256 لمفتاح التصحيح** | `1F:32:AE:D3:12:51:DA:8A:AF:B8:BC:5F:E9:FB:07:C4:91:D7:98:77:1C:A0:A3:47:E3:4E:21:47:10:0E:41:64` | `3E:44:CF:42:77:5B:63:82:45:E5:44:6B:F6:2B:AB:6A:9B:03:C8:8F:9A:3B:85:19:F4:86:B4:9D:96:75:BD:2A` | `1F:32:AE:D3:12:51:DA:8A:AF:B8:BC:5F:E9:FB:07:C4:91:D7:98:77:1C:A0:A3:47:E3:4E:21:47:10:0E:41:64` | **MATCH (100%)** |
| **اسم مستعار المفتاح (`Alias`)** | `androiddebugkey` | `androiddebugkey` | `androiddebugkey` | **MATCH** |
| **Firebase Project ID** | `cinestream-sulo` | `remixed-project-id` (في JSON) / `cinestream-sulo` (في الكود) | `remixed-project-id` (في JSON) / `cinestream-sulo` (في الكود) | **BLOCKED (Awaiting JSON)** |
| **Firebase Project Number** | `979447256418` | `1234567890` (في JSON) / `979447256418` (في الكود) | `1234567890` (في JSON) / `979447256418` (في الكود) | **BLOCKED (Awaiting JSON)** |
| **Firebase App ID (Admin)** | `1:979447256418:android:ea5b570266ae1aa6a9883b` | `1:1234567890:android:remixedappid` (في JSON) / الكنسي في الكود | `1:1234567890:android:remixedappid` (في JSON) / الكنسي في الكود | **BLOCKED (Awaiting JSON)** |

---

## 2. تفاصيل الفحص وتعديل الملفات (Files Inspected & Modified)

### أ. الملفات التي تم تعديلها:
1. **`app/build.gradle.kts`:**
   - **الموقع:** السطر 17 في كتلة `android.defaultConfig`.
   - **التعديل:**
     ```kotlin
     <<<< TargetContent
         applicationId = "com.aistudio.cinestreamadmin.mwtssd"
     ==== ReplacementContent
         applicationId = "com.aistudio.cinestreamadmin.adcxz"
     >>>>
     ```
   - **السبب والضوابط:** مواءمة اسم الحزمة مع هوية تطبيق الإدارة المعتمدة، مع الإبقاء الصارم على `namespace = "com.example"`، وعدم إضافة أي `applicationIdSuffix` أو نكهات بناء جديدة (`productFlavors`).
2. **`debug.keystore` و `debug.keystore.base64`:**
   - **الإجراء:** تم الاحتفاظ بنسخة احتياطية من المفتاح السابق في `/tmp/backup_debug.keystore`. ثم استُخرج المفتاح الأصلي المضمن في مسار `.github/workflows/build.yml` (الأسطر 28-30).
   - **التحقق:** تم فحص المفتاح المستعاد عبر `keytool` وأثبت التطابق المطلق 100% مع البصمتين الكنسيتين SHA-1 و SHA-256.
   - **السرية:** تم التحديث الصامت دون طباعة المفتاح أو سلاسل Base64 في المخرجات.

### ب. الملفات المفحوصة والمثبتة:
1. **`app/src/main/java/com/example/diagnostics/FirebaseInitializer.kt`:**
   - تم فحص الأسطر 16-22 والتأكد من أنها تعتمد الثوابت الكنسية الإنتاجية:
     ```kotlin
     private const val FALLBACK_APP_ID = "1:979447256418:android:ea5b570266ae1aa6a9883b"
     private const val FALLBACK_API_KEY = "AIzaSyC9CvaM9Mw3NiD-KsOiYvHZHj6XZJQJnPs"
     private const val FALLBACK_PROJECT_ID = "cinestream-sulo"
     private const val FALLBACK_STORAGE_BUCKET = "cinestream-sulo.firebasestorage.app"
     private const val FALLBACK_GCM_SENDER_ID = "979447256418"
     ```
   - لم تتطلب أي تعديل لأنها مطابقة مسبقاً للقيم الكنسية المعتمدة.

---

## 3. نتائج التحقق من استعادة ملف Firebase الرسمي (`app/google-services.json`)

تطبيقاً للشرط الصارم في بنود المهمة:
> *"ممنوع إنشاء JSON يدوياً أو الإبقاء على 'remixed-project-id' باعتباره مشروع الإنتاج.*  
> *إذا لم يتوفر ملف رسمي قابل للتحقق، أوقف هذه الجزئية وسجّل 'BLOCKED'؛ لا تستبدل الملف بقيم تخمينية ولا تحذف ملفاً أصلياً دون نسخة احتياطية."*

1. **فحص المصادر المتاحة:**
   - تم فحص ملف `.github/workflows/build.yml`، وتبيّن أنه يحتوي فقط على مفتاح التصحيح `debug.keystore` ولا يحتوي على ملف `google-services.json`.
   - لم يقم المستخدم بتضمين أو تمرير نص ملف `google-services.json` الرسمي في نص الطلب.
   - لا توجد نسخة تاريخية غير معدلة للملف الكامل داخل بيئة العمل، حيث تم استبدال الملف أثناء استنساخ البيئة السحابية الحالية بملف تجريبي مؤقت (`remixed-project-id`).
2. **القرار الهندسي:**
   - **تم الاحتفاظ بنسخة احتياطية** للملف الحالي في `/tmp/backup_google-services.json`.
   - **إيقاف وتجميد استبدال الملف فوراً وتسجيل الحالة كـ `BLOCKED`**، التزاماً بحظر الإنشاء اليدوي للملف وبحظر استخدام قيم تخمينية.
   - يلزم تزويد ملف `google-services.json` الرسمي المصدّر مباشرة من مشروع Firebase (`cinestream-sulo`) لإتمام عملية المحاذاة التلقائية الكاملة لمهمة Google Services.

---

## 4. نتائج أوامر البناء والاختبارات الميدانية (Execution Output Evidence)

### أ. تقرير توقيع المفاتيح (`./gradlew signingReport`):
- **الأمر:** `./gradlew signingReport`
- **الحالة:** **SUCCESS (2s)**
- **المخرجات:**
  ```text
  Variant: debug
  Config: debugConfig
  Store: /app/applet/debug.keystore
  Alias: androiddebugkey
  MD5: 34:8E:62:1F:1F:92:41:CD:A1:1D:F3:C2:48:8E:9D:22
  SHA1: 54:A8:14:FB:04:DE:BD:98:3F:33:6E:F8:2E:87:82:0E:DB:76:0F:12
  SHA-256: 1F:32:AE:D3:12:51:DA:8A:AF:B8:BC:5F:E9:FB:07:C4:91:D7:98:77:1C:A0:A3:47:E3:4E:21:47:10:0E:41:64
  Valid until: Thursday, January 15, 2054
  ```

### ب. مهمة معالجة خدمات جوجل (`:app:processDebugGoogleServices`):
- **الأمر:** `./gradlew :app:processDebugGoogleServices`
- **النتيجة:** **FAILED (Expected Divergence)**
- **المخرجات:**
  ```text
  Execution failed for task ':app:processDebugGoogleServices'.
  > No matching client found for package name 'com.aistudio.cinestreamadmin.adcxz' in /app/applet/app/google-services.json
  ```
- **التشخيص:** الإضافة تبحث عن العميل المسجل `com.aistudio.cinestreamadmin.adcxz` داخل الملف، وتفشل نظراً لأن الملف الموجود حالياً هو الملف المؤقت للبيئة (`mwtssd`). نجاح هذه المهمة يتطلب تزويد الملف الرسمي المعتمد.

### ج. تجميع حزمة التصحيح (`:app:assembleDebug`):
- **الأمر:** `./gradlew :app:assembleDebug -x :app:processDebugGoogleServices`
- **الحالة:** **BUILD SUCCESSFUL in 1m 14s**
- **الملف المولد:** `app/build/outputs/apk/debug/app-debug.apk`

### د. فحص الحزمة وشهادة التوقيع عبر `apksigner` و `aapt2`:
- **أداة فحص التوقيع:** `/opt/android/sdk/build-tools/36.0.0/apksigner verify --verbose --print-certs app-debug.apk`
  - `Verified using v2 scheme: true`
  - `Signer #1 certificate SHA-1 digest: 54a814fb04debd983f336ef82e87820edb760f12` (**مطابق 100%**)
  - `Signer #1 certificate SHA-256 digest: 1f32aed31251da8aafb8bc5fe9fb07c491d798771ca0a347e34e2147100e4164` (**مطابق 100%**)
- **أداة فحص الحزمة:** `/opt/android/sdk/build-tools/36.0.0/aapt2 dump badging app-debug.apk`
  - `package: name='com.aistudio.cinestreamadmin.adcxz' versionCode='1' versionName='1.0'`
  - `application-label:'CineStream Admin'`
  - **النتيجة:** تطابق حتمي ومطلق للحزمة واسم التطبيق وشهادة التوقيع داخل ملف APK الفعلي.

### هـ. اختبارات وحدة أندرويد لكود Kotlin (`:app:testDebugUnitTest`):
- **الأمر:** `./gradlew :app:testDebugUnitTest -x :app:processDebugGoogleServices`
- **الحالة:** **BUILD SUCCESSFUL in 47s**
- **إحصائيات الفحص:** **142 اختبار ناجح بنسبة 100% (0 failures, 0 errors, 0 skipped)**:
  - `CineStreamAdminLogicTest`: 106 اختبارات ناجحة.
  - `SearchOrderArchitectureTest`: 32 اختباراً ناجحاً.
  - `ExampleRobolectricTest`: اختباران ناجحان.
  - `ExampleUnitTest`: اختبار واحد ناجح.
  - `FirebaseTest`: اختبار واحد ناجح.

### و. التحليل الثباتي (`:app:lintDebug`):
- **الأمر:** `./gradlew :app:lintDebug -x :app:processDebugGoogleServices`
- **الحالة:** **BUILD SUCCESSFUL in 1m 54s**
- **النتيجة:** **0 errors, 60 warnings** (خلو كامل من أي أخطاء برمجية قاطعة).

---

## 5. تأكيد الالتزام بالحدود الصارمة (Strict Constraints Adherence)

- [x] **عدم تعديل Firebase Console:** لم يتم إجراء أي استدعاء أو تغيير أو تسجيل عميل جديد.
- [x] **عدم تعديل أو نشر قواعد Firestore:** ملف `firestore.rules` لم يُمس ولم يُستدع أمر النشر.
- [x] **حظر ترحيل Cloudflare Workers:** بقاء Firebase كخلفية حصرية للمشروع.
- [x] **استمرار تعطيل التعليقات:** مسار `/comments` محظور وضوابط `PHASE 22.10-B` سارية بالكامل.
- [x] **عدم تعديل نمط `release`:** إعدادات التوقيع في `signingConfigs.release` بقيت كما هي دون تغيير.

---

## 6. الخلاصة والتقييم النهائي

1. **استعادة مفتاح التصحيح الكنسي وتوقيع APK:** **PASS** (بصمتا SHA-1 و SHA-256 متطابقتان بنسبة 100% ومثبتتان عبر `signingReport` و `apksigner`).
2. **مواءمة هوية الحزمة في البناء:** **PASS** (الحزمة داخل APK هي `com.aistudio.cinestreamadmin.adcxz`).
3. **سلامة الكود والاختبارات:** **PASS** (142/142 اختبار وحدة ناجحة، و 0 أخطاء Lint).
4. **استعادة ملف Firebase الرسمي (`google-services.json`):** **BLOCKED** (الملف الرسمي غير متوفر في مسار CI ولم يمرره المستخدم، وتطبيقاً للتعليمات تم الامتناع عن التخمين اليدوي بانتظار تزويد الملف الرسمي).
