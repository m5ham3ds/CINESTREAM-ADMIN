# PHASE 22.22-C — تقرير تدقيق مطابقة وتوافق قواعد Firestore للوحة الإدارة
## CINESTREAM ADMIN DASHBOARD — FIRESTORE RULES RECONCILIATION AUDIT REPORT

**تاريخ التدقيق والتنفيذ:** 20 أكتوبر 2026  
**الدور الهندسي:** مهندس أمن Firebase وتطوير Android وBackend أول (Senior Firebase Security Engineer)  
**حدود الجلسة والنطاق:** جلسة Google AI Studio المستقلة الخاصة بـ **CineStream Admin Dashboard** فقط  
**معرّف المرحلة:** `PHASE 22.22-C — ADMIN DASHBOARD FIRESTORE RULES RECONCILIATION AUDIT`  
**الحالة العامة للمرحلة:** **COMPLETED SUCCESSFULLY (AUDIT, RECONCILIATION VERIFICATION & PROVISIONAL CANDIDATE PREPARATION)**  
**تأكيد عدم النشر:** **NO PRODUCTION DEPLOYMENT OR FIRESTORE DATA MODIFICATION OCCURRED (STRICT READ-ONLY/LOCAL CANDIDATE PREPARATION)**  

---

## 1. ملخص تنفيذي وحالة اكتمال المرحلة (Executive Summary)

أُجري هذا التدقيق الشامل في إطار التحضير للمطابقة المركزية لقواعد Firestore (`PHASE 22.22-C`) لمنظومة CineStream. وقد تم تنفيذ جميع المهام المطلوبة بدقة متناهية ودون أدنى مساس بقواعد الإنتاج أو بياناته:

1. **التحقق من البصمة المرجعية للوحة الإدارة (Admin Baseline Verification):**
   - تم التحقق من ملف القواعد المحلي المعتمد `/firestore.rules`.
   - تطابق البصمة الرقمية المشفرة SHA-256 بنسبة **100% وبايتياً** البصمة المرجعية المعتمدة:
     `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` (الحجم: 29,652 بايت، 809 أسطر).
   - تم التحقق من أن إعدادات النشر في `/firebase.json` تشير حصرياً إلى هذا الملف (`"rules": "firestore.rules"`).
   - تم الإبقاء على ملف `/firestore.rules` الكنسي الأصلي **دون أي تعديل إطلاقاً**.

2. **تتبع واستقصاء عمليات Firestore الفعلية في كود لوحة الإدارة (Tracing Real Admin Operations):**
   - تم فحص وتوثيق كافة العمليات في جميع المستودعات (`AdminRepository`, `ManagedExtensionRepository`, `ProRequestRepository`, `SearchOrderRepository`, `SupportRepository`, `LoginViewModel`).
   - تم توثيق المسارات والحقول والصلاحيات ونوع العملية (وثيقة مفردة، استعلام، معاملة مجمعة، مستمع حي) في مصفوفة الأدلة البرمجية.

3. **مراجعة المخاطر الأمنية الحرجة (Critical Security Risk Review):**
   - **صلاحية الإدارة والمشرفين (`/admins`):** اشتراط توثيق البريد الإلكتروني للمالك (`email_verified == true`) وحصر منح وسحب الصلاحيات الإدارية في المالك الموثق حصراً (`isOwnerAccount()`).
   - **سجل النقاط وادعاءات المهام:** حظر تام لكتابة المستخدم العادي المباشرة في `/point_transactions` و `/task_claims` لحماية النزاهة المالية لنظام المكافآت.
   - **خصوصية الإشعارات:** عزل تام للإشعارات الفردية والخاصة بمشتركي Pro عن سائر المستخدمين.
   - **حماية الملفات الشخصية العامة وحوارات الدعم:** تقييد الحقول القابلة للتعديل ومنع تزوير الرتب الإدارية.
   - **إلغاء ميزة التعليقات:** التأكد القاطع من غياب أي واجهات أو عمليات للتعليقات، وإبقاؤها محظورة كلياً تحت الرفض الافتراضي العام.

4. **إعداد الملف المرشح المؤقت للمطابقة (Provisional Reconciliation Candidate):**
   - تم إنشاء ملف مرشح مستقل باسم:
     `/PHASE_22.22-C_ADMIN_RECONCILIATION_CANDIDATE.rules`
   - بصمته المحسوبة: `7a0b3a80b4cf23b631c4259583a9f0234e005294ddc1787a61a6deb7f6f18e2f` (29,881 بايت، 815 سطراً).
   - تضمن تحسينات آمنة محددة بدقة (السماح للمشرف وصاحب الحساب بقراءة الملف العام المقفل، دعم حالات الحروف في حالة تذاكر الدعم، وتصحيح قراءة إيصالات القراءة للمشاركين في المحادثة)، مع الإبقاء على كافة الضوابط المالية والإدارية المشددة.

5. **فحص القيود الخارجية وهوية Firebase:**
   - تم توثيق جميع نقاط عدم اليقين المتوقفة على كود تطبيق المستخدم بوسم: `NEEDS_EXTERNAL_COMPARISON`.
   - تم توثيق اختلافات قالب هوية حزمة الأندرويد في ملف القالب المحلي بوسم: `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` دون تغيير أي معرّف أو حزمة.

---

## 2. التحقق من البصمة المرجعية لقواعد لوحة الإدارة (Baseline Fingerprint Verification)

تم قياس البصمة الرقمية المشفرة والأبعاد الفعلية لملف قواعد لوحة الإدارة باستخدام الأدوات النظامية (`sha256sum`, `wc -c`, `wc -l`):

| الملف المفحوص | المسار الفعلي داخل المشروع | بصمة SHA-256 المحسوبة | الحجم (بايت) | عدد الأسطر | النتيجة ومطابقة المرجع |
|---|---|---|---|---|:---:|
| **قواعد لوحة الإدارة الحالية** | `/firestore.rules` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 | 809 | **تطابق 100% مع المرجع الكنسي المعتمد** |
| **نسخة المرشح السابقة (22.22-B)** | `/PHASE_22.22-B_ADMIN_FIRESTORE_RULES_UPDATE_CANDIDATE.rules` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 | 809 | **مطابقة تامة** |
| **النسخة الاحتياطية المسجلة** | `/PHASE_22.22-B_PRE_RESTORE_LOCAL_RULES_BACKUP.txt` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 | 809 | **مطابقة تامة** |
| **ملف النسخة المرجعية المرفقة** | `/CINESTREAM_ADMIN firestore.rules.txt` | `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277` | 29,652 | 809 | **مطابقة تامة** |
| **ملف القواعد القديمة للتحديث** | `/Rules.md` | `4bad2bf56454ed804247dab7a112c54e2db5da66ecfb8ef7b70fb7659bc63fae` | 23,661 | 710 | **مطابقة للملف المرفق** |
| **ملف المرشح المؤقت الجديد (22.22-C)** | `/PHASE_22.22-C_ADMIN_RECONCILIATION_CANDIDATE.rules` | `7a0b3a80b4cf23b631c4259583a9f0234e005294ddc1787a61a6deb7f6f18e2f` | 29,881 | 815 | **مرشح مؤقت مطور بدقة** |

### التحقق من تكوين النشر المحلي في `firebase.json`:
- يحدد ملف `firebase.json` بوضوح:
  ```json
  {
    "firestore": {
      "rules": "firestore.rules"
    },
    "emulators": {
      "firestore": {
        "port": 8085,
        "host": "127.0.0.1"
      }
    }
  }
  ```
- **إقرار عدم إثبات القواعد المنشورة سحابياً:** يؤكد هذا التدقيق أن مطابقة البصمة `207aade4...` تمت للملف المحلي المعتمد داخل المشروع. ولم يتم الادعاء بأن هذا الملف يمثل النسخة المنشورة حالياً في Firebase Console دون إثبات مباشر من السحابة.

### حالة المرفقات المرجعية المطلوبة:
- ملف `PHASE_22.22_CENTRAL_REVIEW_FIRESTORE_RULES.md`: **غير موجود في نظام ملفات هذه الجلسة** (تم تسجيل هذا القيد التزاماً بتعليمات التدقيق، والاعتماد على التقارير المرجعية المتوفرة محلياً والتحليل المباشر للكود).
- ملف `google-services(3).json`: **غير مرفق بهذا الاسم تحديداً**. يتوفر في المشروع الملف المرجعي المكافئ الموثق ببصمة `9bdb880b9e47424f8ce833de7631967ff3b37c84795b6d3075e7faa8e390d895` في المسار `public/assets/aistudio/google-services_9bdb880b.json`.

---

## 3. تتبع واستقصاء عمليات Firestore الفعلية في كود لوحة الإدارة (Trace Real Admin Operations)

يوثق الجدول التالي كافة مسارات وعمليات الوصول الفعلية في الكود المصدري للوحة الإدارة ومقارنتها بالقواعد الحالية:

| المسار في Firestore | الكود المصدر (الملف والسطر) | العملية البرمجية والحقول المقروءة/المكتوبة | الدور المطلوب في المصادقة | نوع الوصول في Firestore | القاعدة الحالية في `firestore.rules` | حكم التوافق والأدلة البرمجية |
|---|---|---|---|---|---|:---:|
| **`/admins/{adminUid}`** | `AdminRepository.kt:235-260`<br>`AdminRepository.kt:989-1015` | **قراءة:** `enabled, role, isSuperAdmin, email`<br>**كتابة/ترقية:** المالك يسجل نفسه كـ superadmin؛ أو يمنح/يسحب صلاحية المشرف (`enabled, isSuperAdmin, note, updatedAt, updatedBy`) | `isOwnerAccount()` للكتابة<br>`isAdmin()` أو صاحب الحساب للقراءة | Document Read / Set with Merge | الأسطر 187-193:<br>`allow read: if isAdmin() \|\| (isAuthenticated() && request.auth.uid == adminUid);`<br>`allow write: if isOwnerAccount();` | **VERIFIED IN ADMIN PROJECT**<br>محصورة للمالك فقط لمنع تصعيد الصلاحيات |
| **`/users/{userId}`** | `AdminRepository.kt:324`<br>`AdminRepository.kt:380-440` | **قراءة:** استعلام قائمة المستخدمين (limit 100)<br>**تحديث:** حظر المستخدمين (`isBanned, banReason, bannedAt, bannedBy`) وتعديل الصلاحيات (`canWatch, canDownload, canChat, canStory`) والاشتراكات (`isPro, subscriptionTier, proExpiresAt`) | `isAdmin()` | Collection Query / Document Update with Merge | الأسطر 235-249:<br>`allow read: if isAdmin() \|\| isOwner(userId);`<br>`allow update: if isAdmin() \|\| ...` | **VERIFIED IN ADMIN PROJECT**<br>مطابقة تامة |
| **`/users/{userId}/point_transactions/{txId}`** | `AdminRepository.kt:597-642`<br>`AdminRepository.kt:678-710` | **كتابة دفعة (Batch):** تعديل رصيد النقاط مع تسجيل حركة مالية (`id, userId, amount, type, reason, adminUid, adminEmail, timestamp`)<br>**قراءة:** استعلام حركات النقاط للمستخدم | `isAdmin()` للكتابة<br>`isAdmin()` أو المستخدم للقراءة | Batch Write / Collection Query | الأسطر 255-261:<br>`allow read: if isAdmin() \|\| isOwner(userId);`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>(المستخدم محظور من الكتابة لحماية الاقتصاد) |
| **`/users/{userId}/task_claims/{taskId}`** | `FirebaseContract.kt:140-143`<br>`AdminRepository.kt` | **قراءة:** فحص ادعاءات المهام المنجزة لمنع التكرار | `isAdmin()` أو صاحب الحساب | Document Read | الأسطر 266-271:<br>`allow read: if isAdmin() \|\| isOwner(userId);`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>كتابة العميل محظورة لحماية النزاهة |
| **`/public_profiles/{userId}`** | `firestore.rules:200-229`<br>`FirebaseContract.kt` | **حذف:** المشرف يحذف الملف في حال المخالفة<br>**قراءة:** قراءة الملفات العامة للمستخدمين | `isAdmin()` للحذف والقراءة<br>`isAuthenticated()` للملفات العامة | Document Read / Delete | الأسطر 200-229:<br>`allow read: if isAuthenticated() && resource.data.isProfilePublic == true;`<br>`allow delete: if isAdmin() \|\| isOwner(userId);` | **VERIFIED WITH REFINEMENT**<br>تم توسيع قراءة الملف الشخصي للمشرف ولصاحب الحساب في المرشح 22.22-C |
| **`/notifications/{notificationId}`** | `AdminRepository.kt:1257-1280` | **إنشاء:** إرسال إشعار عام أو موجه (`title, message, type, target, targetUid, createdAt, senderUid, senderEmail`)<br>**قراءة:** استعلام الإشعارات الإدارية | `isAdmin()` | Document Create / Collection Query | الأسطر 451-467:<br>`allow read: if isAdmin() \|\| ... target filtering;`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>عزل تام للجمهور المستهدف |
| **`/support_conversations/{convId}`** | `SupportRepository.kt:44-65`<br>`SupportRepository.kt:149-210` | **قراءة:** استعلام المحادثات الحية<br>**تحديث:** تعيين المشرف، وتحديث الحالة (`status, assignedAdminId, lastMessage, lastMessageTime, updatedAt`)<br>**حذف:** أرشفة أو حذف التذكرة | `isAdmin()` | Collection Query / Document Update / Batch | الأسطر 581-606:<br>`allow read: if isAdmin() \|\| ...;`<br>`allow update, delete: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>المشرف يملك الصلاحية الكاملة |
| **`/support_conversations/{convId}/messages/{msgId}`** | `SupportRepository.kt:112, 162-190` | **قراءة:** قراءة رسائل الدعم<br>**إنشاء:** إرسال رد المشرف (`id, senderId, senderEmail, senderRole="admin", message, timestamp, isAdmin=true`) | `isAdmin()` | Subcollection Query / Create | الأسطر 608-629:<br>`allow read: if isAdmin() \|\| ...;`<br>`allow create: if isAdmin() \|\| ...;` | **VERIFIED IN ADMIN PROJECT**<br>تشفير رتبة المرسل كمسؤول موثق |
| **`/pro_requests/{requestId}`** | `ProRequestRepository.kt:38, 156-200` | **قراءة:** استعلام الطلبات المعلقة<br>**معاملة مجمعة:** تحديث الطلب (`status="APPROVED", reviewedBy, reviewedAt`) وترقية المستخدم في `/users/{uid}` | `isAdmin()` | Collection Query / Transaction Update | الأسطر 531-550:<br>`allow read, delete: if isAdmin();`<br>`allow update: if isAdmin() \|\| userCancelPending;` | **VERIFIED IN ADMIN PROJECT**<br>مطابقة تامة للعملية والمعاملة |
| **`/managed_extensions/{id}` و `/extensions/{id}`** | `ManagedExtensionRepository.kt:147, 375-400` | **كتابة مزدوجة (Dual-write):** نشر الإضافات وسيرفرات البث في المسار الكنسي والتراثي معا (`name, version, packageUrl, scraperKey, apiBaseUrl, headers, selectors, priority, updatedAt`) | `isAdmin()` | Dual Document Set with Merge | الأسطر 412-446:<br>`allow read: if isAuthenticated();`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>مطابقة لكلا المسارين |
| **`/config/app`** | `AdminRepository.kt:1092-1120` | **قراءة وتحديث:** إعدادات الصيانة والتحديثات الإجبارية (`maintenanceMode, maintenanceMessage, minVersion, latestVersion, apkUrl, forceUpdate, announcement`) | `isAuthenticated()` للقراءة<br>`isAdmin()` للتحديث | Document Read / Write | الأسطر 355-364:<br>`allow read: if isAuthenticated();`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>المسار الكنسي النشط |
| **`/config/search_order`** | `SearchOrderRepository.kt:48, 116-140` | **قراءة وتحديث:** ترتيب أولويات محركات البحث والتوزيع الصوتي للأفلام والمسلسلات والأنمي | `isAuthenticated()` للقراءة<br>`isAdmin()` للتحديث | Document Read / Write | الأسطر 384-394:<br>`allow read: if isAuthenticated();`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>مطابقة لحقول الترتيب |
| **`/config/features` و `/config/economy`** | `AdminRepository.kt:765, 818` | **قراءة وتحديث:** مفاتيح تشغيل الميزات وأسعار المكافآت واشتراكات النقاط | `isAuthenticated()` للقراءة<br>`isAdmin()` للتحديث | Document Read / Write | الأسطر 366-382:<br>`allow read: if isAuthenticated();`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT**<br>مطابقة تامة |
| **`/config/global` (مسار مهمل)** | `AdminRepository.kt:1098` | **قراءة احتياطية قديمة فقط:** لا يتم الكتابة فيه إطلاقاً | - | Fallback Read only | الأسطر 407-409:<br>`allow read, write: if false;` | **VERIFIED IN ADMIN PROJECT**<br>المسار مقفل تماماً ويتم استخدام `/config/app` |
| **`/app_updates/{id}`** | `AdminRepository.kt:1191` | **قراءة ونشر:** سجل تحديثات التطبيق (OTA) مع تفاصيل الحزمة | `isAuthenticated()` للقراءة<br>`isAdmin()` للنشر | Document Create / Collection Query | الأسطر 470-478:<br>`allow read: if isAuthenticated();`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT** |
| **`/reports/{id}`** | `AdminRepository.kt:1300-1340` | **قراءة وتحديث:** مراجعة تقارير المشاكل وتغيير حالتها (`status="RESOLVED", resolvedBy, resolvedAt`) | `isAdmin()` | Collection Query / Update | الأسطر 553-578:<br>`allow read, update, delete: if isAdmin();`<br>`allow create: if isAuthenticated() ...;` | **VERIFIED IN ADMIN PROJECT** |
| **`/auditLogs/{id}`** | `AdminRepository.kt:290-315` | **تسجيل وقراءة:** سجل تدقيق العمليات الإدارية الحساسة | `isAdmin()` | Collection Query / Create | الأسطر 518-528:<br>`allow read, write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT** |
| **`/reward_tasks/{id}`** | `AdminRepository.kt:830-890` | **إدارة المهام:** إنشاء وتحديث وحذف مهام المكافآت | `isAuthenticated()` للقراءة<br>`isAdmin()` للكتابة | Document Set / Delete | الأسطر 481-490:<br>`allow read: if isAuthenticated();`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT** |
| **`/leaderboard/{id}`** | `AdminRepository.kt:895-920` | **قراءة:** استعراض متصدري قائمة الترتيب الأسبوعي | `isAuthenticated()` للقراءة<br>`isAdmin()` للكتابة | Document Read | الأسطر 493-515:<br>`allow read: if isAuthenticated();`<br>`allow write: if isAdmin();` | **VERIFIED IN ADMIN PROJECT** |
| **`/users/{userId}/fcmTokens/{id}`** | `firestore.rules:330-345` | **قراءة:** قراءة توكنات الأجهزة لاستهداف الإشعارات | `isAdmin()` أو صاحب الحساب | Subcollection Read | الأسطر 330-345:<br>`allow read: if isAdmin() \|\| isOwner(userId);`<br>`allow write: if isOwner(userId) ...;` | **VERIFIED IN ADMIN PROJECT** |

---

## 4. مراجعة المخاطر الأمنية الحرجة ومستوياتها (Critical Security Risk Register)

تم تصنيف المخاطر الأمنية المكتشفة وفق منهجية معيارية دقيقة (Critical, High, Medium, Low):

| معرف الخطر | تصنيف الخطر ومستواه | الوصف والأثر الأمني | الدليل من القواعد الحالية والمقارنة مع `Rules.md` | الإجراء التصحيحي في لوحة الإدارة وقواعد المرشح |
|---|:---:|---|---|---|
| **RISK-AUTH-01** | **CRITICAL** | **ثغرة تصعيد الصلاحيات وترقية المشرفين (Privilege Escalation):** السماح لأي مشرف عادي بإنشاء أو ترقية مشرفين آخرين في `/admins/{uid}` كما كان موجوداً في `Rules.md` (`allow write: if isAdmin()`). | `Rules.md:177`<br>`firestore.rules:192` | **محصورة كلياً للمالك الأصلي الموثق:**<br>`allow write: if isOwnerAccount();`<br>تم الحفاظ على هذا القيد الصارم 100% في المرشح 22.22-C لمنع أي مشرف من التلاعب بالحسابات الإدارية. |
| **RISK-AUTH-02** | **CRITICAL** | **ثغرة انتحال حساب المالك ببريد غير موثق:** قبول البريد الإلكتروني للمالك `sulopros01@gmail.com` دون التحقق من تأكيده رسمياً من مزود الهوية (`email_verified == true`). | `Rules.md:37`<br>`firestore.rules:34-41` | **اشتراط التوثيق الرسمي الصارم:** دالة `isOwnerAccount()` تشترط `request.auth.token.email_verified == true`. تم تثبيتها في كافة القواعد المعتمدة والمرشحة. |
| **RISK-ECON-01** | **HIGH** | **ثغرة تزوير وتوليد النقاط المالية للعميل:** السماح للعميل العادي بكتابة حركات في سجل النقاط المالي `/users/{uid}/point_transactions` كما كان في `Rules.md` (`isOwner(userId)`). | `Rules.md:214`<br>`firestore.rules:255-261` | **حظر كتابة العميل التام:** قصر الكتابة حصراً على المشرف والخادم الموثوق (`allow write: if isAdmin();`). المستخدم يملك حق القراءة فقط لسجل معاملاته. |
| **RISK-ECON-02** | **HIGH** | **ثغرة تكرار المطالبة بالمكافآت (Replay Attack):** إمكانية قيام العميل بكتابة سجلات ادعاء المهام `/task_claims` مباشرة. | `Rules.md:227`<br>`firestore.rules:266-271` | **حظر الكتابة المباشرة من العميل:** قصر الكتابة على المشرف أو العمليات السحابية الموثوقة لمنع المطالبات الاحتيالية. |
| **RISK-PRIV-01** | **HIGH** | **انتهاك خصوصية الإشعارات الخاصة والموجهة:** السماح لجميع المستخدمين بقراءة كافة الإشعارات في `/notifications` دون فلترة الفئة المستهدفة. | `Rules.md:414`<br>`firestore.rules:451-467` | **فلترة جمهورية دقيقة:** حصر قراءة الإشعار بالجمهور العام (`target == 'all'`) أو صاحب المعرف المستهدف (`targetUid == uid`) أو مشتركي Pro الفعليين. |
| **RISK-SUPP-01** | **MEDIUM** | **العبث بحالة تذاكر الدعم الفني والحقول الإدارية:** إمكانية تعديل المستخدم العادي لتاريخ الإنشاء `createdAt` أو حالة التذكرة `status` أو المشرف المعين. | `Rules.md:538`<br>`firestore.rules:597-604` | **تقييد الحقول القابلة للتحديث:** حصر تحديث المستخدم العادي بالحقول: `['lastMessage', 'lastMessageTime', 'updatedAt', 'unreadCount', 'unreadCounts']` فقط، ومنع تغيير الحالة أو المشرف. |
| **RISK-SOC-01** | **MEDIUM** | **حجب إيصالات القراءة عن الشريك في المحادثة الثنائية:** في القواعد القديمة كان شرط القراءة في `/readReceipts/{uid}` يشترط `request.auth.uid == uid`، مما يمنع الطرف الثاني من قراءة إيصال قراءة شريكه! | `firestore.rules:743-744`<br>`Candidate:744-747` | **تصحيح الصلاحية للمشاركين:** السماح للمشارك في المحادثة بقراءة إيصالات قراءة الطرف الآخر مع بقاء الكتابة محصورة في صاحب المعرف فقط. |
| **RISK-COMM-01** | **INFO / ZERO RISK** | **ميزة التعليقات الملغاة (Comments):** إلغاء الميزة رسمياً والتأكد من عدم وجود أي ثغرات أو مسارات مفتوحة. | `Phase 22.10-B`<br>`firestore.rules:806` | **الحظر الافتراضي العام (Catch-All Deny):** لا يوجد مسار `/comments`؛ تسقط جميع الطلبات في `allow read, write: if false;`. |

---

## 5. مصفوفة توافق المسارات المشتركة مع المراجعة المركزية (Shared-Path Compatibility Matrix)

تم إعداد المصفوفة وفق الضوابط الصارمة لفصل المشروعات، مع وضع الأدلة البرمجية المباشرة من لوحة الإدارة وتحديد ما يتطلب مقارنة خارجية:

| المسار المشترك | عمليات لوحة الإدارة الملاحظة في الكود | السلوك الأمني المتوقع من المراجعة المركزية | الدليل من الكود الفعلي للوحة الإدارة | التعارضات المحتملة أو الأدلة المعلقة | حكم التوافق (Verdict) |
|---|---|---|---|---|:---:|
| **`/admins/{adminUid}`** | فحص وترقية المشرفين وتعيين وسحب الصلاحيات | حصر حق ترقية المشرفين في المالك الموثق `isOwnerAccount()` فقط | `AdminRepository.kt:235-260`<br>`AdminRepository.kt:989-1015` | لا يوجد تعارض؛ القواعد الحالية متوافقة 100% | **VERIFIED IN ADMIN PROJECT** |
| **`/users/{userId}`** | فحص المستخدمين، الحظر، تعديل الصلاحيات والاشتراكات | المشرف يملك حق القراءة والتحديث الكامل؛ المستخدم يقرأ ويحدث بياناته غير الحساسة فقط | `AdminRepository.kt:324`<br>`AdminRepository.kt:380, 430` | لا يوجد تعارض في عمليات لوحة الإدارة | **VERIFIED IN ADMIN PROJECT** |
| **`/users/{userId}/point_transactions`** | تسجيل الحركات المالية التعديلية للمشرف وقراءة السجل | المشرف يكتب؛ المستخدم يقرأ فقط؛ لا كتابة مباشرة للمستخدم | `AdminRepository.kt:597, 642, 678` | **سؤال للمقارنة الخارجية:** هل يرسل User App طلبات نقاط من العميل مباشرة؟ إذا كان كذلك، فسيتلقى `PERMISSION_DENIED` وسيحتاج إلى Cloud Function | **NEEDS_EXTERNAL_COMPARISON** |
| **`/users/{userId}/task_claims`** | قراءة سجل ادعاءات المهام من قبل الإدارة | حظر الكتابة من جانب المستخدم لمنع تكرار المهام | `FirebaseContract.kt:140` | يعتمد على كيفية تنفيذ إتمام المهام في تطبيق المستخدم | **NEEDS_EXTERNAL_COMPARISON** |
| **`/public_profiles/{userId}`** | حذف الملفات المخالفة؛ عرض الملفات العامة | إسقاط اجتماعي آمن خالي من البيانات المالية والحساسة | `PHASE_22.21-B_ADMIN_FIRESTORE_CONTRACT.md` | التحقق من استعلامات البحث وتوفر أسماء المستخدمين في User App | **NEEDS_EXTERNAL_COMPARISON** |
| **`/notifications/{id}`** | إنشاء وإرسال الإشعارات الموجهة والعامة | حصر القراءة بالجمهور المستهدف وحصر الإرسال بالإدارة | `AdminRepository.kt:1257-1280` | يجب أن يستخدم User App فلاتر تطابق الجمهور لعدم التعرض للرفض | **VERIFIED IN ADMIN PROJECT** |
| **`/support_conversations/{id}`** | متابعة التذاكر وتعيين المشرف وتحديث الحالة والردود | إنشاء المستخدم للتذكرة؛ رد المشرف؛ تقييد حقول العميل | `SupportRepository.kt:44, 112, 162` | هل يملك المستخدم في User App خيار إغلاق التذكرة بنفسه (`status="closed"`)؟ حالياً القواعد تقيد تغيير الحالة بالمشرف | **NEEDS_EXTERNAL_COMPARISON** |
| **`/pro_requests/{id}`** | مراجعة الطلبات والموافقة عليها أو رفضها مجمعاً | المشرف يوافق؛ المستخدم يلغي طلبه المعلق فقط | `ProRequestRepository.kt:38, 187, 200` | مطابقة تامة لآلية التحقق والمعاملات في لوحة الإدارة | **VERIFIED IN ADMIN PROJECT** |
| **`/conversations/{id}/readReceipts`** | لا يوجد استخدام في لوحة الإدارة (مستوى مستخدمي التطبيق) | تسجيل وتحديث إيصال القراءة للمستخدم وقراءته للطرفين | `FirebaseContract.kt:88-92` | تم تصحيح القراءة للطرفين في المرشح 22.22-C | **NEEDS_EXTERNAL_COMPARISON** |
| **`/managed_extensions` و `/extensions`** | نشر وتحديث الإضافات مع الكتابة المزدوجة التلقائية | المشرف يكتب في المسارين؛ المستخدم المسجل يقرأ الكتالوج | `ManagedExtensionRepository.kt:375-400` | هل تحول User App كلياً إلى `/managed_extensions` أم ما زال يقرأ `/extensions`؟ | **NEEDS_EXTERNAL_COMPARISON** |
| **`/config/app`, `/features`, `/economy`, `/search_order`** | إدارة وتحديث كافة المعلمات التشغيلية والترتيب والاقتصاد | قراءة كافة المستخدمين المسجلين؛ التعديل حصري للمشرف | `AdminRepository.kt:765, 818, 1092`<br>`SearchOrderRepository.kt:48, 116` | مطابقة تامة وكاملة | **VERIFIED IN ADMIN PROJECT** |
| **`/comments`** | لا يوجد أي كود أو واجهة أو عملية للتعليقات | استمرار الرفض الشامل والتأكيد على إلغاء الميزة | تم التحقق من خلو الكود من أي استخدام | لا يوجد تعارض؛ الميزة ملغاة | **VERIFIED IN ADMIN PROJECT** |

---

## 6. تفاصيل ومسوغات التعديلات في الملف المرشح (Proposed Rule Changes in Candidate)

تم إعداد الملف المرشح المؤقت `/PHASE_22.22-C_ADMIN_RECONCILIATION_CANDIDATE.rules` اعتماداً على الأساس الأمني للوحة الإدارة `firestore.rules`، مع إدخال 3 تعديلات مبررة ومدروسة أمنياً ووظيفياً:

### التعديل الأول: ترويسة ملف المرشح وتوثيق حالة المطابقة
- **الموضع:** الأسطر 6-18 في المرشح.
- **التغيير:** توثيق أن هذا الملف هو مرشح مؤقت لمرحلة `PHASE 22.22-C` غير منشور سحابياً، ويحافظ على متطلبات الأمان المشددة (حصر كتابة `/admins` بالمالك، وسجل النقاط بالإدارة، ورفض التعليقات).

### التعديل الثاني: توسيع قراءة الملف الشخصي العام للمشرف ولصاحب الحساب
- **الموضع:** الأسطر 201-206 في المرشح:
  ```javascript
  match /public_profiles/{userId} {
    allow read: if isAdmin() || (
      isAuthenticated() && (
        resource.data.isProfilePublic == true ||
        request.auth.uid == userId
      )
    );
  ```
- **المسوغ الأمني والوظيفي:** في النسخة الأساسية كان الشرط مقتصراً على `resource.data.isProfilePublic == true`، مما يؤدي إلى حرمان المشرف من فحص الحسابات المبلغ عنها إذا كان الملف الشخصي غير علني، وكذلك حرمان المستخدم نفسه من معاينة إسقاط ملفه الشخصي العام المحفوظ. هذا التعديل يسمح للمشرف ولصاحب الحساب بالقراءة دون فتح الملفات الخاصة لبقية المستخدمين.

### التعديل الثالث: دعم حالات الحروف عند إنشاء تذاكر الدعم الفني
- **الموضع:** السطر 593 في المرشح:
  ```javascript
  request.resource.data.status in ['open', 'pending', 'OPEN', 'PENDING']
  ```
- **المسوغ الوظيفي:** كود لوحة الإدارة يتعامل مع الحالات بالأحرف الصغيرة والكبيرة (`SupportModels.kt`). شمول الحالتين يمنع حدوث رفض `PERMISSION_DENIED` إذا أرسل العميل الحالة بأحرف كبيرة مع الحفاظ على حصرها في التذاكر المفتوحة والانتظار فقط.

### التعديل الرابع: تصحيح قراءة إيصالات القراءة لشريك المحادثة الثنائية
- **الموضع:** الأسطر 746-750 في المرشح:
  ```javascript
  match /readReceipts/{uid} {
    allow read: if isAdmin() || (
      isAuthenticated() &&
      request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.participants
    );
  ```
- **المسوغ الأمني والوظيفي:** في الأساس القديم، كان الشرط يطلب `request.auth.uid == uid` في القراءة، مما يمنع شريك المحادثة في الطرف الآخر من رؤية متى قرأ زميله الرسالة! تم تصحيح القراءة لتكون متاحة لجميع أطراف المحادثة الموثقين كـ `participants`، مع بقاء الكتابة محصورة حصراً بالطرف المعني (`request.auth.uid == uid`) والتحديث بزمن الخادم الصارم (`request.time`).

---

## 7. بيانات وبصمة الملف المرشح الجديد (Candidate Fingerprint & Change Summary)

تم حساب البصمة الرقمية والأبعاد للملف المرشح الجديد:
- **اسم ومسار الملف:** `/PHASE_22.22-C_ADMIN_RECONCILIATION_CANDIDATE.rules`
- **بصمة SHA-256 المحسوبة:** `7a0b3a80b4cf23b631c4259583a9f0234e005294ddc1787a61a6deb7f6f18e2f`
- **حجم الملف:** 29,881 بايت
- **عدد الأسطر:** 815 سطراً
- **طبيعة التغييرات مقارنة بالأساس:** 4 تعديلات طفيفة ومحددة بدقة (توسيع قراءة المشرف وصاحب الحساب لـ `/public_profiles`، وتصحيح قراءة إيصالات القراءة للمشاركين، ودعم صيغ الأحرف لتذاكر الدعم، وتوثيق الترويسة).
- **حالة ملف القواعد الأساسي `/firestore.rules`:** **سليم ومحفوظ دون أي تغيير إطلاقاً (SHA-256: `207aade4...`).**

---

## 8. سجل وحالة الاختبارات المنفذة (Testing Execution and Results)

### أ. اختبارات منطق لوحة الإدارة في أندرويد (Unit Tests Executed & Passed):
تم تشغيل حزمة اختبارات الوحدة المنطقية الرسمية للتطبيق عبر أمر Gradle المباشر:
`./gradlew testDebugUnitTest`

**النتيجة الرسمية:** **BUILD SUCCESSFUL (142 Tests Executed, 142 Passed, 0 Failed, 0 Skipped)**
- `com.example.CineStreamAdminLogicTest`: **106 tests PASSED** (تغطي تقييم الصلاحيات، حظر المستخدمين، الاشتراكات، القيود، وتدقيق التوافق).
- `com.example.SearchOrderArchitectureTest`: **32 tests PASSED** (تغطي تكوين محركات البحث، مجموعات الدبلجة، والحقول الكنسية).
- `com.example.FirebaseTest`: **1 test PASSED** (فحص توافق التهيئة).
- `com.example.ExampleRobolectricTest`: **2 tests PASSED**.
- `com.example.ExampleUnitTest`: **1 test PASSED**.

### ب. اختبارات محاكي قواعد Firestore (Firestore Rules Emulator Tests):
- **حالة التنفيذ:** **NOT RUN (LOCAL EMULATOR AND TEST RUNNER DEPENDENCIES NOT ACTIVE)**.
- **أسباب عدم التشغيل المباشر:**
  1. مجلد حزم الاختبارات `firestore-tests/node_modules` غير مثبت محلياً في بيئة العمل.
  2. عملية محاكي Firebase Firestore Emulator ليست قيد التشغيل (المنفذ 8085 غير نشط).
  3. حظر الاتصال بقاعدة بيانات الإنتاج لأغراض الاختبار التزاماً بالمعايير الأمنية الصارمة.

### ج. مواصفات الاختبارات الـ 13 المطلوبة بالتفصيل (Test Specifications):
تمت المراجعة والتدقيق الساكن (Static Audit) لسيناريوهات الاختبار الـ 13 ومطابقتها مع القواعد:
1. **Unauthenticated reads and writes:** تسقط في دالة `isAuthenticated()` وتُرفض فوراً (`PERMISSION_DENIED`).
2. **Owner authentication with a verified email:** المالك (`sulopros01@gmail.com` مع `email_verified == true`) يُمنح صلاحية `isOwnerAccount()` وله حق الكتابة في `/admins`.
3. **Owner authentication with an unverified email:** يفشل في شرط `email_verified == true` في دالة `isOwnerAccount()` ويُرفض من كتابة `/admins`.
4. **Enabled admin operations:** أي حساب في `/admins/{uid}` يحمل `enabled == true` ينجح في شرط `isAdmin()` ويتمكن من إدارة المستخدمين والإعدادات والتذاكر والإشعارات.
5. **Attempts by ordinary users or subordinate admins to promote accounts:** تُرفض حتماً لأن كتابة `/admins` تشترط `isOwnerAccount()`، والمشرف العادي يملك حق القراءة فقط.
6. **Attempts to change privileged user fields:** المستخدم العادي يُرفض إذا حاول تعديل أي حقل من حقول `modifyingSensitiveUserFields()` (مثل `role`, `isPro`, `pointsBalance`, `isBanned`).
7. **Unauthorized point_transactions and task_claims writes:** محاولات المستخدم العادي للكتابة تُرفض فوراً بموجب `allow write: if isAdmin();`.
8. **Notification audience and target-user isolation:** المستخدم العادي يُمنع من قراءة الإشعارات الموجهة لمستخدمين آخرين أو الموجهة لمشتركي Pro إذا لم يكن مشتركاً فعلياً.
9. **Public versus private profile access:** الحسابات العامة (`isProfilePublic == true`) تقرأ من قبل المستخدمين المسجلين، بينما الحسابات المقفلة لا يقرؤها سوى المشرف وصاحب الحساب نفسه.
10. **Support ticket and internal-field tampering:** المستخدم العادي لا يستطيع تغيير `status` أو المشرف المعين، ويقتصر على حقول الرسائل والوقت وعداد القراءة.
11. **Cross-user and non-participant access to conversation read receipts:** غير المشارك في المحادثة يُحرم تماماً من القراءة والكتابة، بينما الشريك يقرأ فقط، وصاحب المعرف يكتب بإلزامية تطابق `request.time`.
12. **Reads and writes to /comments:** محظورة كلياً بدون أي استثناء وتسقط في الرفض الافتراضي العام.
13. **Final catch-all deny:** أي مسار غير معرّف يخضع للقاعدة الختامية: `match /{document=**} { allow read, write: if false; }`.

---

## 9. اختلافات الهوية وتغيير الإعدادات (Identity Reference and Change Control)

بناءً على فحص سجل لوحة الإدارة في ملف `google-services` المرجعي الصادر عن Firebase (بصمة `9bdb880b...`) مقارنة بملفات المشروع الحالية:

| عنصر الهوية | القيمة المرجعية الصادرة من Firebase | القيمة الحالية في ملفات لوحة الإدارة | موضع الدليل في المشروع | حالة التوافق والوسم الإلزامي |
|---|---|---|---|:---:|
| **Firebase Project ID** | `cinestream-sulo` | `cinestream-sulo` (في الكود)<br>`remixed-project-id` (في قالب json) | `FirebaseInitializer.kt:20`<br>`app/google-services.json:3` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` (في ملف json فقط) |
| **Firebase Project Number** | `979447256418` | `979447256418` (في الكود)<br>`1234567890` (في قالب json) | `FirebaseInitializer.kt:22`<br>`app/google-services.json:4` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` (في ملف json فقط) |
| **Storage Bucket URL** | `cinestream-sulo.firebasestorage.app` | `cinestream-sulo.firebasestorage.app` | `FirebaseInitializer.kt:21` | **MATCH (مطابق 100%)** |
| **Android Package Name** | `com.aistudio.cinestreamadmin.adcxz` | `com.aistudio.cinestreamadmin.dqahhi` | `app/build.gradle.kts:15`<br>`app/google-services.json:11` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` |
| **Firebase Android App ID** | `1:979447256418:android:ea5b570266ae1aa6a9883b` | `1:979447256418:android:ea5b570266ae1aa6a9883b` (في التهيئة) | `FirebaseInitializer.kt:18` | **MATCH (مطابق في كود الربط)** |
| **Android OAuth Client ID** | `979447256418-m7u3batvou0mmrubfkmsnmunvpedo0dd.apps.googleusercontent.com` | غير مدرج في json القالب المؤقت | `google-services_9bdb880b.json:55` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` |
| **Certificate SHA-1** | `54a814fb04debd983f336ef82e87820edb760f12` | `54:A8:14:FB:04:DE:BD:98:3F:33:6E:F8:2E:87:82:0E:DB:76:0F:12` | `.github/workflows/build.yml:30`<br>`debug.keystore` | **MATCH (مطابق لمفتاح التصحيح الكنسي)** |
| **Web OAuth Client ID** | `979447256418-rjb9a0991gvve0328n113dme67i4gpv2.apps.googleusercontent.com` | `1234567890-examplewebclientid.apps.googleusercontent.com` | `app/google-services.json:16` | `IDENTITY_MISMATCH_REQUIRES_EXPLICIT_APPROVAL` |

- **سياسة ضبط التغيير الصارمة (Change Control):** لم يتم تعديل أي من هذه القيم، ولم يتم استبدال ملف `app/google-services.json` أو تعديل `build.gradle.kts`، تطبيقاً للأمر الإلزامي بعدم تغيير إعدادات الهوية تلقائياً.

---

## 10. بنود عدم اليقين المتوقفة على المقارنة الخارجية (Tagged NEEDS_EXTERNAL_COMPARISON)

تم حصر وتوثيق البنود التي تتطلب مراجعة مستقلة مقابل تقرير أو كود CineStream User App:

1. **`NEEDS_EXTERNAL_COMPARISON` — آلية منح نقاط المكافآت:**
   - لوحة الإدارة وقواعد المرشح تحظران كتابة العميل العادي المباشرة في `/users/{uid}/point_transactions` و `/users/{uid}/task_claims`.
   - **المطلوب من جلسة User App:** التحقق مما إذا كان User App يعتمد على دوال سحابية (Cloud Functions) أو خادم خلفي، أم أنه يحاول الكتابة المباشرة من التطبيق، لتفادي خطأ `PERMISSION_DENIED`.

2. **`NEEDS_EXTERNAL_COMPARISON` — مسار قراءة كتالوج الإضافات:**
   - لوحة الإدارة تنفذ كتابة مزدوجة (Dual-write) للمسار الكنسي `/managed_extensions` والتراثي `/extensions`.
   - **المطلوب من جلسة User App:** التأكد من المسار الذي يستمع إليه User App فعلياً.

3. **`NEEDS_EXTERNAL_COMPARISON` — إغلاق تذاكر الدعم الفني من قبل المستخدم:**
   - قواعد لوحة الإدارة تمنع المستخدم العادي من تحديث حقل `status` في `/support_conversations/{id}`.
   - **المطلوب من جلسة User App:** التأكد مما إذا كان تطبيق المستخدم يوفر زراً للمستخدم لإغلاق تذكرته بنفسه (`status="closed"`). إذا كان الأمر كذلك، يتطلب تحديث القاعدة بالسماح بتحديث الحالة إلى `closed` فقط.

4. **`NEEDS_EXTERNAL_COMPARISON` — استعلامات الملفات العامة والتفاعل الاجتماعي:**
   - التأكد من الحقول والفلاتر التي يعتمدها User App عند البحث عن المستخدمين أو فحص توفر اسم المستخدم في `/public_profiles`.

---

## 11. تأكيد عدم النشر وسلامة الإنتاج (Deployment Safety Confirmation)

- **نؤكد بشكل قاطع وجازم:**
  1. لم يتم تنفيذ أمر النشر `firebase deploy` مطلقاً.
  2. لم يتم نشر أو تفعيل أي قواعد أمنية في Firebase Console.
  3. لم يتم تعديل أو استبدال ملف القواعد المحلي الأساسي `/firestore.rules`.
  4. لم يتم إجراء أي عمليات كتابة أو حذف أو تعديل على أي بيانات حية في قاعدة بيانات Firestore الإنتاجية.

---

## 12. القرارات والأدلة المحددة المطلوبة قبل الاعتماد النهائي (Decisions Required Before Final Approval)

قبل اتخاذ قرار توحيد القواعد ونشرها، يجب الحصول على القرارات التالية:
1. **اعتماد آلية النقاط في User App:** تأكيد اعتماد Cloud Functions لمنح مكافآت الإعلانات والدخول اليومي بدلاً من الكتابة المباشرة للعميل.
2. **اعتماد صيغة إغلاق التذاكر:** البت فيما إذا كان المستخدم في User App مخولاً بإغلاق تذكرته بنفسه لإضافتها إلى حقول التحديث المسموحة في `/support_conversations`.
3. **قرار مطابقة معرّف الحزمة (Package ID):** البت في توحيد معرّف الحزمة وتحديث `google-services.json` المحلي بموافقة صريحة للمشروع.
4. **اعتماد الملف المرشح 22.22-C:** مراجعة التقرير المقابل من جلسة User App واعتماد الملف المرشح الموحد مركزياً.
