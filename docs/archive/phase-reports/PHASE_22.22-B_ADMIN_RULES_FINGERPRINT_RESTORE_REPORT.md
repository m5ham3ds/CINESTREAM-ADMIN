# PHASE 22.22-B — تقرير استعادة البصمة المرجعية لقواعد FIRESTORE وتدقيق هوية FIREBASE
## CINESTREAM ADMIN DASHBOARD — RESTORE ORIGINAL FIRESTORE RULES FINGERPRINT & SAFE UPDATE PREPARATION

**تاريخ التدقيق والتنفيذ:** 20 أكتوبر 2026  
**الدور الهندسي:** مهندس أمن Firebase وتطوير Android وBackend متخصص في لوحات الإدارة والأنظمة الحساسة  
**النطاق والبيئة التشغيلية:** جلسة Google AI Studio المستقلة الخاصة بـ **CineStream Admin Dashboard** فقط  
**معرّف المرحلة:** `PHASE 22.22-B — RESTORE ORIGINAL RULES FINGERPRINT & VERIFIED FIREBASE IDENTITY AUDIT`  
**الحالة العامة للمرحلة:** **COMPLETED SUCCESSFULLY (STRICT LOCAL RESTORATION & READ-ONLY AUDIT)**  
**الملف المرشح المجهز للتحديث:** `/PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules`  
**الملف الاحتياطي المسجل:** `/PHASE_22.22-B_PRE_RESTORE_LOCAL_RULES_BACKUP.txt`  

---

## 1. ملخص تنفيذي (Executive Summary)

تم إنجاز أهداف مرحلة `PHASE 22.22-B` بدقة متناهية ودون أدنى مساس ببيانات الإنتاج أو نشر أي قواعد إلى Firebase:
1. **التحقق من البصمة المرجعية للقواعد:** تم فحص ملف القواعد المعتمد محلياً لمشروع CineStream Admin Dashboard (`/firestore.rules`)، وتبيّن أن بصمته المشفرة SHA-256 تطابق **100% وبايتياً** البصمة المرجعية الأصلية المستهدفة (`207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`) بحجم 29,652 بايت و809 أسطر، مما يعني أن النسخة المعتمدة سليمة ومثبتة دون أي انحراف.
2. **تجهيز الملف المرشح المستقل:** تم استنساخ الملف المرجعي الأصلي المحقق بايتياً إلى ملف مرشح مستقل باسم `PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules` بنفس البصمة تماماً ليكون جاهزاً للمراجعة المركزية لاحقاً.
3. **مقارنة تفصيلية دلالية وأمنية مع القواعد الحالية المرفوعة (`Rules.md`):** جرت مقارنة نقدية وشاملة عبر `diff` بين النسخة المرجعية المستعادة (`207aade4...`) والنسخة المقدمة من المستخدم التي تمثل قواعد Firebase الحالية (`Rules.md` - بصمة `4bad2bf5...`). وكشفت المقارنة عن ثغرات واختلالات جسيمة في نسخة `Rules.md` تم سدها وتحصينها في النسخة المرجعية لـ Admin Dashboard، وأبرزها:
   - **ثغرة ترقية المشرفين في `Rules.md`:** كانت تسمح لأي مشرف عادي بكتابة سجلات المشرفين في `/admins/{uid}`، بينما تقصرها النسخة المرجعية حصراً على المالك الموثق `isOwnerAccount()`.
   - **ثغرة التلاعب بسجل النقاط في `Rules.md`:** كانت تسمح للمستخدم العادي بكتابة سجل معاملاته وادعاءات المهام مباشرة من التطبيق.
   - **انتهاك خصوصية الإشعارات في `Rules.md`:** كانت تتيح لأي مستخدم مسجل قراءة كافة الإشعارات الموجهة للآخرين أو لمشتركي Pro.
   - **حماية إسقاط الملف الشخصي العام:** غياب قواعد `/public_profiles/{userId}` كلياً في `Rules.md`.
4. **تدقيق هوية Firebase:** تم فحص ملف `google-services.json` المرفق ومقارنة سجل Admin Dashboard مع قيم المشروع المحلية، وتوثيق الاختلافات دون تعديلها، مع الوسم الإلزامي `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL`.
5. **سياسة ميزة التعليقات (Comments):** تم التأكد من أن كلا النسختين تخلو تماماً من مسار `/comments`، وتعتمدان سياسة الرفض الافتراضي العام (Catch-All Deny)، بما يتوافق 100% مع قرار التأجيل الحاسم الصادر في المرحلة `Phase 22.10-B`.

---

## 2. جدول جرد البصمات والأدلة الرقمية (SHA-256 Fingerprint Inventory)

تم حساب بصمات SHA-256 والأحجام وعدد الأسطر باستخدام الأدوات النظامية الفعلية (`sha256sum`, `wc -c`, `wc -l`) داخل بيئة العمل:

| الملف المفحوص | المسار الفعلي داخل الجلسة | بصمة SHA-256 المحسوبة فعلياً | الحجم (بايت) | عدد الأسطر | حالة المطابقة مع المرجع |
|---|---|---|---|---|:---:|
| **قواعد المشروع الحالية** | `/firestore.rules` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 | 809 | **تطابق 100% بايتياً** |
| **النسخة الاحتياطية قبل الاستعادة** | `/PHASE_22.22-B_PRE_RESTORE_LOCAL_RULES_BACKUP.txt` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 | 809 | **مطابقة تامة** |
| **الملف المرشح للتحديث** | `/PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 | 809 | **نسخة مرجعية محققة 100%** |
| **ملف القواعد الحالية المرفق (Rules.md)** | `/public/assets/aistudio/Rules_4bad2bf5.md` | `4bad2bf56454ed804247dab7a112c54e2db5da66ecfb8ef7b70fb7659bc63fae` | 23,661 | 710 | **تطابق 100% مع المرفق** |
| **ملف إعداد Firebase المرفق** | `/public/assets/aistudio/google-services_9bdb880b.json` | `9bdb880b9e47424f8ce833de7631967ff3b37c84795b6d3075e7faa8e390d895` | 2,553 | 85 (84 trailing) | **تطابق 100% مع المرفق** |
| **ملف google-services المحلي بالقالب** | `/app/google-services.json` | `c4945a605a392e5e0a796675bcc432e77f6bfd25f4bbc3d5ae2b1021f6bf7bdb` | 590 | 26 | **مختلف (قالب AI Studio المؤقت)** |

### إثبات سلامة الاستعادة:
- نظراً لأن بصمة ملف قواعد المشروع المفتوح في هذه الجلسة `/firestore.rules` تطابق بالفعل بصمة النسخة المرجعية المستهدفة:
  `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`
- تطبيقاً للبند 1.5 من شروط الخطوة 1 والخطوة 2، تم الإبقاء على الملف الأصلي سليماً كما هو دون أي استبدال تشويهي للمسافات أو التعليقات، وتم إنشاء النسخة المرشحة المطابقة بايتياً `PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules` لحفظ الحالة الكنسية.
- تم التحقق من أن إعدادات النشر المحلية في `/firebase.json` تشير إلى الملف نفسه: `"rules": "firestore.rules"`.

---

## 3. قسم إلزامي: تدقيق هوية تسجيل Firebase ومفتاح التصحيح (Firebase Identity Reference Check)

تطبيقاً للقيود الصارمة بوجوب تدقيق هوية Firebase محلياً وفصلها تماماً عن بصمة قواعد Firestore، جرى فحص ملف `public/assets/aistudio/google-services_9bdb880b.json` (SHA-256: `9bdb880b9e47424f8ce833de7631967ff3b37c84795b6d3075e7faa8e390d895`).

تم استخراج **سجل Admin Dashboard فقط** (العميل رقم 2 في الملف المرفق، الأسطر 46-82)، ومقارنته بالملفات الحالية في المشروع:

| عنصر الهوية والتسجيل | القيمة المرجعية في `google-services.json` المرفق | القيمة الفعلية في ملفات المشروع الحالية | موضع الدليل المحلي في المشروع | حالة المطابقة والوسم الإلزامي |
|---|---|---|---|:---:|
| **Firebase Project ID** | `cinestream-sulo` | `cinestream-sulo` (في الكود)<br>`remixed-project-id` (في json القالب) | `FirebaseInitializer.kt:20`<br>`app/google-services.json:3` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` (في ملف json القالب فقط) |
| **Firebase Project Number** | `979447256418` | `979447256418` (في الكود)<br>`1234567890` (في json القالب) | `FirebaseInitializer.kt:22`<br>`app/google-services.json:4` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` (في ملف json القالب فقط) |
| **Storage Bucket URL** | `cinestream-sulo.firebasestorage.app` | `cinestream-sulo.firebasestorage.app` | `FirebaseInitializer.kt:21` | **MATCH (مطابق في الكود)** |
| **Android Package Name** | `com.aistudio.cinestreamadmin.adcxz` | `com.aistudio.cinestreamadmin.dqahhi` | `app/build.gradle.kts:15`<br>`app/google-services.json:11` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` |
| **Firebase Android App ID** | `1:979447256418:android:ea5b570266ae1aa6a9883b` | `1:979447256418:android:ea5b570266ae1aa6a9883b` (في الكود)<br>`1:1234567890:android:remixedappid` (في json) | `FirebaseInitializer.kt:18`<br>`app/google-services.json:9` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` (في ملف json القالب) |
| **Android OAuth Client ID** | `979447256418-m7u3batvou0mmrubfkmsnmunvpedo0dd.apps.googleusercontent.com` | غير مدرج في json القالب المؤقت | `google-services_9bdb880b.json:55` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` |
| **Android Certificate SHA-1** | `54a814fb04debd983f336ef82e87820edb760f12` | `54:A8:14:FB:04:DE:BD:98:3F:33:6E:F8:2E:87:82:0E:DB:76:0F:12` (مطابق في التوقيع الكنسي المستعاد في سير عمل CI) | `.github/workflows/build.yml:30`<br>`PHASE_22.15-B Report` | **MATCH (مطابق لبصمة مفتاح التصحيح الكنسي)** |
| **Web OAuth Client ID** | `979447256418-rjb9a0991gvve0328n113dme67i4gpv2.apps.googleusercontent.com` | `1234567890-examplewebclientid.apps.googleusercontent.com` (في json القالب) | `app/google-services.json:16` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` |

### الملاحظات والقرارات الصارمة الخاصة بالهوية:
1. **عدم التعديل التلقائي:** التزاماً بتعليمات المستخدم الصريحة، لم يتم استبدال ملف `app/google-services.json` ولم يُعدل `applicationId` في `app/build.gradle.kts`. بقيت هذه الفروقات موثقة كما هي تحت الوسم `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL`.
2. **التمييز الصارم بين البصمات والهوية:**
   - البصمة الرقمية للقواعد: `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` هي بصمة محتوى ملف نصي (`firestore.rules`).
   - بصمة الشهادة: `54a814fb04debd983f336ef82e87820edb760f12` هي SHA-1 لشهادة توقيع تطبيق Android (`debug.keystore`) المسجلة لدى Google Services.
3. **حدود الجلسة والتأكيد القطعي:** **نؤكد قطعياً أن هذه الجلسة لم تتصل بملفات CineStream User App أو كوده أو بياناته، ولم تقم بفتحه أو تعديله.**

---

## 4. المقارنة التفصيلية الأمنية والدلالية (Semantic Diff: Rules.md vs Reference)

تم فحص الفروقات بين النسخة الحالية في Firebase (`Rules.md` - 710 أسطر) والنسخة المرجعية الكنسية لـ Admin Dashboard (`firestore.rules` - 809 أسطر):

### 1) مصادقة المشرف وتوثيق المالك (Admin Authentication & Owner Authority)
- **في `Rules.md` (الأسطر 37-47):**
  تتحقق دالة `isAdmin()` من بريد المالك عبر:
  ```javascript
  (request.auth.token.email != null && request.auth.token.email.matches('(?i)sulopros01@gmail.com'))
  ```
  **الخلل الأمني:** لا تشترط الدالة أن يكون البريد الإلكتروني موثقاً (`email_verified == true`). أي حساب ينشأ بهذا البريد لدى أي مزود غير موثق قد يحصل على صلاحيات إدارية!
- **في النسخة المرجعية `firestore.rules` (الأسطر 34-51):**
  تم إنشاء دالة مخصصة صارمة `isOwnerAccount()`:
  ```javascript
  function isOwnerAccount() {
    return isAuthenticated() &&
      ('email' in request.auth.token) &&
      request.auth.token.email != null &&
      ('email_verified' in request.auth.token) &&
      request.auth.token.email_verified == true &&
      request.auth.token.email.matches('(?i)sulopros01@gmail.com');
  }
  ```
  **الأثر الأمني:** تحصين كامل ضد الحسابات غير المؤكدة أو التلاعب بمزودي الهوية.

---

### 2) الصلاحيات على مجموعة المشرفين `/admins/{adminUid}` (Privilege Escalation Vector)
- **في `Rules.md` (الأسطر 177-187):**
  ```javascript
  allow write: if isAdmin() ||
    (
      isAuthenticated() &&
      request.auth.uid == adminUid &&
      request.auth.token.email != null &&
      request.auth.token.email.matches('(?i)sulopros01@gmail.com')
    );
  ```
  **الثغرة الكبرى في `Rules.md`:** الشرط `allow write: if isAdmin()` يسمح **لأي مشرف مفعل** بكتابة وتعديل وحذف أي وثيقة داخل `/admins`! هذا يمكّن المشرف العادي من ترقية مستخدمين آخرين إلى مشرفين، أو حرمان زملائه، أو تعطيل حسابات الإدارة الأخرى.
- **في النسخة المرجعية `firestore.rules` (الأسطر 187-193):**
  ```javascript
  // Only the verified owner account can grant/revoke admin authority.
  // Enabled admins may read admin records but cannot promote others.
  allow write: if isOwnerAccount();
  ```
  **الأثر الأمني والإداري:** سد ثغرة تصعيد الصلاحيات بالكامل. المشرفون العاديون يستطيعون قراءة السجلات فقط للتحقق، ولكن حصر منح الصلاحيات وترقية المديرين مقصور 100% على المالك الأصلي الموثق.

---

### 3) حماية إسقاط الملف الشخصي العام `/public_profiles/{userId}`
- **في `Rules.md`:**
  المجموعة **غير موجودة إطلاقاً**. وأي محاولة وصول إليها من تطبيق المستخدم أو لوحة الإدارة تُرفض بالرفض الافتراضي العام (Catch-all deny).
- **في النسخة المرجعية `firestore.rules` (الأسطر 200-229):**
  إضافة كنسية متكاملة لـ `/public_profiles/{userId}` تتيح قراءة الملفات العامة للمستخدمين المسجلين، مع تقييد صارم للمفاتيح المسموح بإنشائها وتحديثها للمستخدم نفسه (`uid`, `username`, `firstName`, `lastName`, `photoUrl`, `isOnline`, `isProfilePublic`, `bio`, `updatedAt`)، وحظر تسريب أي بيانات حساسة أو مالية إليها، مع منح المشرف حق الحذف فقط (`allow delete: if isAdmin()`).

---

### 4) سجل المعاملات المالية والنقاط `/users/{userId}/point_transactions/{txId}`
- **في `Rules.md` (الأسطر 214-224):**
  ```javascript
  // Temporary Firebase-only economy mode.
  allow write: if isAdmin() ||
    (
      isOwner(userId) &&
      request.resource.data.userId == userId
    );
  ```
  **الثغرة في `Rules.md`:** السماح للعميل العادي بكتابة سجل النقاط بنفسه (`isOwner(userId)`). يستطيع أي مستخدم عبر العميل التلاعب وإضافة نقاط وهمية لنفسه!
- **في النسخة المرجعية `firestore.rules` (الأسطر 255-261):**
  ```javascript
  // Client-authored ledger entries are not trusted. Use a trusted backend
  // for rewards/points mutations; do not relax this to make client writes work.
  allow write: if isAdmin();
  ```
  **الأثر الأمني:** حظر تام للكتابة من جانب المستخدم العادي، وحصر حق تسجيل المعاملات في الإدارة والمصادر الخلفية الموثوقة.  
  *(ملاحظة توافق خارجي: `NEEDS_EXTERNAL_COMPARISON` للتحقق من كيفية إرسال User App لمكافآت المهام).*

---

### 5) ادعاءات المهام `/users/{userId}/task_claims/{taskId}`
- **في `Rules.md` (الأسطر 227-237):**
  مفتوحة لكتابة المستخدم العادي (`isOwner(userId)`).
- **في النسخة المرجعية `firestore.rules` (الأسطر 266-271):**
  محصورة في الإدارة والخادم الموثوق (`allow write: if isAdmin();`) لمنع تكرار المطالبة بالمكافآت (Anti-replay protection).

---

### 6) خصوصية الإشعارات `/notifications/{notificationId}`
- **في `Rules.md` (الأسطر 414-418):**
  ```javascript
  allow read: if isAuthenticated();
  allow write: if isAdmin();
  ```
  **الخلل في `Rules.md`:** القراءة متاحة لكل مستخدم مسجل دون تمييز! يستطيع المستخدم العادي قراءة الإشعارات الموجهة حصراً لمستخدم بعينه (`target == 'user'`) أو الإشعارات الموجهة لفئة المشتركين (`target == 'pro'`).
- **في النسخة المرجعية `firestore.rules` (الأسطر 451-467):**
  قواعد فلترة جمهورية دقيقة جداً:
  ```javascript
  allow read: if isAdmin() || (
    isAuthenticated() && (
      (('target' in resource.data) && resource.data.target == 'all') ||
      (('target' in resource.data) && resource.data.target == 'user' &&
        ('targetUid' in resource.data) && resource.data.targetUid == request.auth.uid) ||
      (('target' in resource.data) && resource.data.target == 'pro' &&
        exists(/databases/$(database)/documents/users/$(request.auth.uid)) &&
        (
          get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isPro == true ||
          get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isPremium == true ||
          get(/databases/$(database)/documents/users/$(request.auth.uid)).data.subscriptionTier in ['pro', 'PRO']
        ))
    )
  );
  ```
  **الأثر الأمني:** الحفاظ التام على خصوصية الإشعارات وتوجيهها للجمهور المستحق فقط.

---

### 7) طلبات الترقية وإلغاء المستخدم `/pro_requests/{requestId}`
- **في `Rules.md` (الأسطر 486-489):**
  `allow update, delete: if isAdmin();`  
  المستخدم العادي لا يملك أي حق لإلغاء طلبه المعلق إذا غير رأيه أو أخطأ في التحويل.
- **في النسخة المرجعية `firestore.rules` (الأسطر 537-547):**
  السماح للمستخدم بإلغاء طلبه المعلق بنفسه بشروط أمان حازمة:
  ```javascript
  allow update: if isAdmin() || (
    isAuthenticated() &&
    resource.data.userId == request.auth.uid &&
    request.resource.data.userId == request.auth.uid &&
    (resource.data.status == 'PENDING' || resource.data.status == 'pending') &&
    request.resource.data.status == 'CANCELLED' &&
    request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'updatedAt', 'cancelledAt'])
  );
  ```
  **الأثر الأمني والوظيفي:** تمكين المستخدم من إلغاء طلبه دون السماح له بتغيير الخطة أو الموافقة على طلبه بنفسه أو حقن حقول إدارية.

---

### 8) محادثات الدعم الفني `/support_conversations`
- **في `Rules.md` (الأسطر 538-545):**
  التحديث غير مقيد بحقول محددة.
- **في النسخة المرجعية `firestore.rules` (الأسطر 593-605):**
  تقييد حقول التحديث المسموحة للمستخدم بـ `['lastMessage', 'lastMessageTime', 'updatedAt', 'unreadCount', 'unreadCounts']` مع منع العبث بتاريخ الإنشاء `createdAt` أو حالة المحادثة `status`.

---

### 9) المحادثات الاجتماعية والقصص `/conversations` و `/stories`
- **في `Rules.md`:**
  تحديث المحادثات والرسائل والقصص متاح بصورة عامة لأطراف المحادثة دون تقييد الحقول.
- **في النسخة المرجعية `firestore.rules`:**
  - اشتراط أن تتكون قائمة المشاركين من عضوين متمايزين تحديداً (`size() == 2`).
  - حظر تعديل عدادات القراءة `unreadCounts` من جانب العميل لمنع التلاعب بعدادات الطرف الآخر.
  - إضافة مسار إشعارات القراءة `/readReceipts/{uid}` (الأسطر 742-754).
  - حصر تعديل الرسائل في حالات التعديل أو الحذف للمرسل فقط وحصر التفاعلات (`reactions`) للطرفين.
  - تقييد تحديث القصة بـ `['imageUrl', 'timestamp']` فقط.

---

### 10) الحقول الحساسة المشتركة (Cross-Project Fields)
- أضافت النسخة المرجعية `firestore.rules` الحقلين `pointsEarnedTotal` و `pointsSpentTotal` إلى دالتي `modifyingSensitiveUserFields()` و `hasSafeUserCreationDefaults()`، لحماية تسميات النقاط التي قد يرسلها User App من أي تعديل غير مصرح به.

---

### 11) ميزة التعليقات الملغاة `/comments`
- **في كلتا النسختين (`Rules.md` و `firestore.rules`):**
  لا يوجد أي ذكر لمسار `/comments`.
  كافة الطلبات الموجهة للتعليقات تسقط في القاعدة الشاملة الأخيرة:
  ```javascript
  match /{document=**} {
    allow read, write: if false;
  }
  ```
  مما يعني أن ميزة التعليقات **ممنوعة كلياً ومحمية** في كليهما التزاماً بالقرار الصادر في `Phase 22.10-B`.

---

## 5. مصفوفة تتبع العمليات الإدارية في الكود وتطابقها مع القواعد المستعادة

| العملية البرمجية في لوحة الإدارة | الملف ورقم السطر | المسار في Firestore | الشرط في القواعد المرجعية | نتيجة التحقق الأمني |
|---|---|---|---|:---:|
| **فحص وترقية المالك** | `AdminRepository.kt:235, 255` | `/admins/{uid}` | `allow read: if isAdmin()...`<br>`allow write: if isOwnerAccount()` | **PASS_WITH_EVIDENCE** |
| **تعيين/سحب صلاحية الإدارة** | `AdminRepository.kt:989, 991` | `/admins/{uid}` | `allow write: if isOwnerAccount()` | **PASS_WITH_EVIDENCE** |
| **مراقبة قائمة المستخدمين** | `AdminRepository.kt:324` | `/users` | `allow read: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **تعديل بيانات وحظر المستخدم** | `AdminRepository.kt:380, 430` | `/users/{uid}` | `allow update: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **تعديل النقاط بالمعاملات** | `AdminRepository.kt:640, 642` | `/users/{uid}` و sub `/point_transactions` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **تعديل إعدادات التطبيق والصيانة** | `AdminRepository.kt:1092` | `/config/app` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **نشر ترتيب محركات البحث** | `SearchOrderRepository.kt:116` | `/config/search_order` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **نشر إعدادات الميزات والاقتصاد** | `AdminRepository.kt:765, 818` | `/config/features` و `/config/economy` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **إدارة الإضافات وسيرفرات البث** | `ManagedExtensionRepository.kt:376` | `/managed_extensions/{id}` و `/extensions/{id}` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **نشر تحديثات التطبيق OTA** | `AdminRepository.kt:1191` | `/app_updates/{id}` و `/config/app` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **إرسال الإشعارات المستهدفة** | `AdminRepository.kt:1257` | `/notifications/{id}` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **الموافقة على طلبات Pro بالمعاملات** | `ProRequestRepository.kt:187, 200` | `/pro_requests/{id}` و `/users/{uid}` | `allow update: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **الرد على محادثات الدعم الفني** | `SupportRepository.kt:189, 190` | `/support_conversations` و sub `/messages` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |
| **تسجيل سجلات التدقيق الإداري** | `AdminRepository.kt:302` | `/auditLogs/{id}` | `allow write: if isAdmin()` | **PASS_WITH_EVIDENCE** |

---

## 6. المسائل المتوقفة على التحقق الخارجي (NEEDS_EXTERNAL_COMPARISON)

1. **كتابة سجل النقاط لمستخدمي التطبيق:**  
   القواعد المرجعية تحظر على المستخدم العادي الكتابة في `/users/{uid}/point_transactions`. يجب التحقق من أن CineStream User App يعتمد على دوال سحابية (Cloud Functions) أو خادم موثوق لمنح مكافآت الإعلانات والدخول اليومي، لأنه سيتلقى `PERMISSION_DENIED` إذا حاول الكتابة المباشرة.
2. **استهلاك كتالوج الإضافات:**  
   لوحة الإدارة تكتب في مسارين متزامنين: المسار الكنسي `/managed_extensions` والمسار التراثي `/extensions`. يجب التأكد من المسار الذي يستمع إليه User App.
3. **تسجيل أجهزة FCM:**  
   القواعد المرجعية في المسار `/users/{uid}/fcmTokens/{installationId}` تشترط تطابق الوقت `createdAt == request.time`. يجب مقارنتها مع كود User App للتأكد من استخدام `FieldValue.serverTimestamp()`.

---

## 7. الاختبارات وحالة التحقق قبل النشر

1. **حالة بيئة الاختبارات:**
   - بيئة محاكي Firebase Emulator لم تكن قيد التشغيل في هذه الجلسة.
   - التزاماً بالقيود الإلزامية بعدم تنفيذ أي اختبارات قد تتصل بقاعدة الإنتاج، لم يتم تشغيل اختبارات حية.
2. **المراجعة الساكنة لاختبارات القواعد القائمة:**
   - تم فحص ملفات الاختبارات في `firestore-tests/`:
     - `firestore-rules.test.js` (1,149 سطراً يغطي 52 سيناريو أمني).
     - `phase03a1-rules.test.js` (351 سطراً يغطي حماية النقاط).
     - `phase05b-comprehensive-rules.test.js` (394 سطراً يغطي مصفوفة الحالات الـ 32).
   - كافة الاختبارات القائمة متطابقة دلالياً ومصممة للعمل مع النسخة المرجعية المستعادة (`207aade4...`).

---

## 8. قرار النشر والتوصيات النهائية

1. **تأكيد عدم النشر:** **لم يتم تنفيذ `firebase deploy` مطلقاً ولم يتم نشر أي قواعد إلى Firebase في هذه الجلسة.**
2. **سلامة بيانات الإنتاج:** **لم يتم إجراء أي تعديل أو كتابة أو حذف لأي بيانات حقيقية.**
3. **الملف المرشح جاهز للمراجعة المركزية:**
   تم حفظ النسخة المرجعية المحققة في:
   `/PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules`
   ببصمتها المرجعية المعتمدة:
   `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`
4. **الخطوة التالية الموصى بها:**
   مقارنة تقرير هذه الجلسة بتقرير User App الصادر في الجلسة الأخرى، للتأكد من توافق آلية منح النقاط وتسجيل التوكنات، ثم اعتماد النسخة الموحدة ونشرها مركزياً بموافقة صريحة من المستخدم.
