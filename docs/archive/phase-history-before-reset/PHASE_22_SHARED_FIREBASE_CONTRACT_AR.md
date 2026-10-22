# العقد المشترك الموحّد لـ Firebase — PHASE 22.6-B: CineStream Ecosystem
## Authoritative Shared Firebase Contract & Rules Convergence (User App ↔ Admin Dashboard)

**تاريخ التوثيق:** 2026-10-16  
**النطاق المشترك:** CineStream User App (`com.example`) ↔ CineStream Admin Dashboard (`com.example` / `com.aistudio.cinestreamadmin.eftzvl`)  
**المرجع المعماري الأساسي:** `ADR-CINESTREAM-001_ACTIVE_BACKEND_FIREBASE.md` (Firebase Cloud Firestore & Authentication)  
**معرّف المراجعة والمرحلة:** `PHASE_22.6-B — ADMIN_RULES_AND_CONTRACT_ALIGNMENT`  
**التقرير المرجعي لتطبيق المستخدم:** `PHASE_22.6-A_USER_APP_PRO_CANCELLATION_ALIGNMENT_REPORT_AR.md`  
**حالة المطابقة الإجمالية:** `RECONCILED_WITH_UNVERIFIED_DEPLOYMENT_BOUNDARY`  
(تم توحيد المنطق البرمجي وقواعد أمان إلغاء الاشتراكات بدقة 100% ومطابقة العقد الفعلي، مع إبقاء حدود مشروع وسحابة Firebase كـ `UNVERIFIED` لعدم كفاية الدليل الميداني على توحيد المشروع المنشور).  
**بصمة القواعد المحسوبة للوحة الإدارة (SHA-256):** `207aade42bc71dee5acc95568041b19d7f9c860047d79a506e830992361db277`  
**بصمة القواعد المرصودة لتطبيق المستخدم (PHASE 22.6-A):** `54c8570a25ece1fb6a57bfb0aa2560a50202240a30d4a4cf7a8afd694a18d5ef`  

---

### 1. مبادئ الحوكمة والسياسة المشتركة

1. **الخلفية النشطة المعتمدة:** تظل Firebase (Authentication & Cloud Firestore) هي المزود النشط والوحيد للهوية وقواعد البيانات، مع حظر ربط أي واجهات بديلة غير معتمدة.
2. **فصل الصلاحيات والأدوار (Zero-Trust Privilege Isolation):**
   - هوية المالك (`sulopros01@gmail.com`) محصنة من الإلغاء أو الحذف أو التعديل، والمالك هو المخول الوحيد بإدارة صلاحيات المشرفين في `/admins/{adminUid}` (`isOwnerAccount()`).
   - المشرفون العاديون يملكون صلاحيات تشغيلية مدققة ومقيدة بالقواعد (`isAdmin()`) ولا يملكون صلاحية ترقية أنفسهم أو غيرهم لمستوى المالك (منع تصعيد الصلاحيات A02).
   - المستخدم العادي ممنوع قطعياً من تعديل 45 حقلاً حساساً تشمل الاشتراك، والأدوار، وحظر الحساب، ورصيد النقاط، وسجلات التدقيق عبر `modifyingSensitiveUserFields()`.
3. **تحديثات السجلات الحساسة (Immutable & Audited):**
   - دفتر حركات النقاط (`point_transactions`) غير قابل للتعديل (Immutable Ledger) ويُدار ذرياً وحصرياً عبر المشرف (`allow write: if isAdmin()`).
   - طلبات Pro تنتقل وفق آلة حالات حتمية (`PENDING / pending -> APPROVED / REJECTED / CANCELLED`)، مع حصر قرار الموافقة والرفض بالمشرف ذرياً، وحصر حق الإلغاء بمالك الطلب فقط وهو في حالة `PENDING` أو `pending`.
4. **التوافق التنازلي الحذر ومنطق الحقول المشتركة (Dual-Read / Dual-Write):**
   - حل كافة الفروقات بين تسميات الحقول داخل طبقة المستودعات والنماذج عبر الكتابة والقراءة المزدوجة التوافقية دون الحاجة إلى ترحيل بيانات Production أو كسر أي طرف.
   - تمييز غياب الحقل (`null`) عن القيمة الصفرية المشروعة (`0L`) في ملخصات النقاط لمنع استبدال الصفر الصحيح بحقل قديم غير متزامن.
5. **السياسة الأمنية المحصنة لحمولة الإلغاء (Strict Cancellation Payload):**
   - حمولة إلغاء طلب Pro من طرف العميل محصورة حصرياً في الحقول الثلاثة: `status`, `cancelledAt`, `updatedAt`.
   - الحالة الجديدة المستهدفة محصورة بالقيمة المعيارية `CANCELLED` (أحرف كبيرة حصراً).
   - منع أي تعديل على `userId` أو تفاصيل الخطة (`planId`, `durationDays`) أو بيانات الدفع والملاحظات (`paymentReference`, `notes`) أو حقول المراجعة والاعتماد.
6. **حدود مشروع Firebase وحالة القواعد المنشورة (Project & Deployment Boundary):**
   - **غير مثبت (UNVERIFIED):** لا تتوفر أدلة تشغيلية كافية في هذه البيئة تثبت أن المشروعين يعملان على نفس مشروع Firebase السحابي في Production أو على مشروعين مستقلين.
   - **عدم ادعاء توحيد القواعد المنشورة:** يمتلك كل مستودع ملف `firestore.rules` ببصمة ومحتوى مستقل، وبالتالي لا يُدّعى أن القواعد المنشورة في السحابة موحدة.

---

### 2. مصفوفة مقارنة العقد المشترك وحالة التوافق الثنائي

| # | العنصر / المسار | سلوك تطبيق المستخدم (User App) | سلوك لوحة الإدارة (Admin Dashboard) | العقد المشترك الموحّد | حالة التوافق | الدليل الفني والحالة |
|---|---|---|---|---|:---:|---|
| 1 | **مسار المشرفين**<br>`/admins/{uid}` | قراءة وثيقة حسابه فقط للتأكد من حالة الإشراف. | قراءة للمشرف وصاحب الحساب. ترقية وعزل المشرفين محصورة بالمالك (`isOwnerAccount()`). | قراءة للمشرف وصاحب الحساب. كتابة وترقية للمالك فقط. حظر تصعيد المشرفين العاديين. | **ALIGNED** | تم تثبيته بالقواعد واختبارات المحاكي (Case A01/A02). |
| 2 | **مسار المستخدمين**<br>`/users/{uid}` | قراءة وتحديث الملف الشخصي (الاسم والصورة فقط). | قراءة لكافة المستخدمين. إدارة الحظر والصلاحيات والاشتراكات. حظر حذف المالك. | قراءة لصاحب الحساب وللمشرف. تعديل العميل مقيد بالاسم والصورة. حظر تعديل 45 حقلاً حساساً. | **ALIGNED** | محمي بدالة `modifyingSensitiveUserFields()` ومفحوص بالمحاكي. |
| 3 | **إجماليات النقاط**<br>`users/{uid}` | يقرأ ويكتب `pointsEarnedTotal` و`pointsSpentTotal`. | يدعم `totalPointsEarned` و`pointsEarnedTotal` (وكذلك للمصروفة) مع تمييز الصفر عن الغياب. | **كتابة وقراءة مزدوجة متوافقة:** تحديث ذري لكلا الاسمين. القراءة تعتمد دلالة القيمة المتاحة مع تمييز الصفر (`0L`) عن الغياب (`null`) لمنع استبدال الصفر الصحيح. حظر كتابة العميل. | **ALIGNED** | تم إثباتها في `AdminRepository.adjustUserPoints` و`Models.kt` و`firestore.rules` واختبارات Kotlin. |
| 4 | **حقول الاشتراك**<br>`subscriptionTier`, `planId` | يعتمد `subscriptionTier` (`FREE`, `PRO_LITE`, `PRO`) و`planId` والحقل التوافقي `isPremium`. | يدعم القيم القياسية والتوافقية (`isPremium`, `isPro`, `proExpiresAt`, `subscriptionTier`). | الحقل القياسي بالأحرف الكبيرة (`"FREE"`, `"PRO_LITE"`, `"PRO"`). الحقول التوافقية `isPremium: Boolean`, `isPro: Boolean`. | **ALIGNED** | متوافق تماماً في `Models.kt` و`ProRequestRepository.kt`. |
| 5 | **طلبات Pro (المخطط)**<br>`/pro_requests/{id}` | ينشئ بالحقول: `requestId`, `userId`, `planId`, `durationDays`, `paymentReference`, `notes`, `status`. يرسل `cancelledAt` عند الإلغاء. | يدعم: `requestedPlan`, `requestedDuration`, `paymentMethod`, `paymentProofUrl`, `userNote`, `cancelledAt` مع الحقول النظيرة. | **مخطط موحد متكامل (Reconciled Schema):** يدعم كلا الطرفين بدون فقد. `cancelledAt` مدعوم صراحة. `paymentReference` كود نصي مستقل عن `paymentProofUrl`. `durationDays` مدعوم كـ Long/Int وString. | **ALIGNED** | معتمد في `ProRequest.kt` و`ProRequestsScreen.kt` و`ProRequestRepository.kt`. |
| 6 | **طلبات Pro (الحالات والإلغاء)**<br>`/pro_requests/{id}` | ينشئ بحالة `PENDING` أو `pending`. يلغي بالنقل إلى `CANCELLED` المعيارية وتحديث `status, cancelledAt, updatedAt`. | يدعم `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`. البت بالموافقة أو الرفض للمشرف ذرياً. | آلة حالات حتمية: `PENDING` أو `pending` ينتقل إلى `APPROVED` أو `REJECTED` (بواسطة المشرف) أو `CANCELLED` (بواسطة صاحب الطلب حصرياً). الحقول المعدلة محصورة بـ `status, cancelledAt, updatedAt`. الحالات الثلاث نهائية ولا إعادة فتح. | **ALIGNED** | محمي ومفحوص في `firestore.rules` (سطر 537-545) واختبارات المحاكي المحدثة. |
| 7 | **محادثات الدعم**<br>`/support_conversations/{id}` | معرف الوثيقة = UID المستخدم. يعتمد الحروف الصغيرة (`open`, `pending`, `resolved`, `closed`). | معرف الوثيقة = UID المستخدم. يدعم الحالات بالحروف الصغيرة (`open`, `pending`, `resolved`, `closed`). | معرف التذكرة يطابق UID المستخدم. إنشاء التذاكر مقيد بالحروف الصغيرة حصرياً (`open`, `pending`). رفض التذاكر الجديدة بالحالات الكبيرة. | **ALIGNED** | تم توحيد الحالات في `firestore.rules` (سطر 589) ورفض الأحرف الكبيرة في الاختبارات. |
| 8 | **طوابع رسائل الدعم**<br>`.../messages/{id}` | يعتمد `lastMessageTime` في الترتيب والمزامنة. | كان يعتمد `lastMessageAt`. تم التوحيد للكتابة المزدوجة للطرفين. | **تزامن مزدوج للطوابع الزمنية:** كل رد إداري يكتب `lastMessageAt` و`lastMessageTime` ذرياً معاً. القراءة تستخدم `effectiveLastMessageTimestamp`. | **ALIGNED** | تم تأكيد التحديث المزدوج للطابع الزمني (`DISC-03`) في `SupportRepository.kt`. |
| 9 | **فصل الدعم عن الدردشة** | تذاكر الدعم في `/support_conversations`. الدردشة الاجتماعية في `/conversations`. | لوحة الإدارة تستخدم فقط `/support_conversations` وممنوعة من `/conversations`. | عزل تام وحماية دستورية لخصوصية محادثات المستخدمين الاجتماعية. | **ALIGNED** | محمي بنصوص قاطعة في `firestore.rules` (سطر 633). |
| 10 | **دفتر حركات النقاط**<br>`point_transactions` | قراءة خاصة للمستخدم. حظر تام على إنشاء أو تعديل الحركات من العميل. | قراءة وكتابة المشرف عبر معاملات ذرية مع التحقق من الحدود وسجلات التدقيق. | دفتر حركات غير قابل للتعديل (Immutable Ledger). ممنوع من كتابة العميل نهائياً لحماية الاقتصاد (`allow write: if isAdmin()`). | **ALIGNED** | مجاز بنسبة 100% في اختبارات المحاكي `phase03a1` و`firestore-rules.test.js` (A07). |
| 11 | **مطالبات المهام**<br>`task_claims` | قراءة خاصة لمنع تكرار الجائزة. حظر كتابة العميل. | قراءة للمشرف وصاحب الحساب لمنع ازدواجية الجوائز. | حظر كتابة العميل لحماية النظام من هجمات إعادة المطالبة (`allow write: if isAdmin()`). | **ALIGNED** | تم التحقق التام في `firestore.rules` (سطر 270). |
| 12 | **التكوين وOTA**<br>`/config/app` | قراءة للموثقين `isAuthenticated()`. حظر كتابة العميل. | قراءة وكتابة المشرف حصرياً. | القراءة العامة لكافة الموثقين. الكتابة حصرية بالمشرفين المعتمدين. | **ALIGNED** | تم إلغاء الاعتماد على المسار المهمل `/config/global`. |
| 13 | **ترتيب البحث**<br>`/config/search_order` | قراءة لجميع الموثقين لترتيب مصادر المشاهدة. | قراءة وكتابة المشرف عبر `SearchOrderRepository.kt`. | قراءة لكافة المستخدمين. الكتابة والتعديل محصورة بالمشرف. | **ALIGNED** | تم فحصها واختبارها بـ 32 اختباراً معمارياً في `SearchOrderArchitectureTest`. |
| 14 | **سجلات التدقيق**<br>`/auditLogs/{id}` | حظر قراءة وكتابة العميل منعاً باتاً. | إضافة وتدقيق للمشرف مع كل إجراء إداري، وانتظار النجاح عبر `.await()`. | مسار موحد `/auditLogs`. حظر تام على غير المشرفين من القراءة أو الكتابة. | **ALIGNED** | تم إثباتها واختبارها في `firestore-rules.test.js` (A04). |
| 15 | **الإشعارات الموجهة**<br>`target: 'all' / 'user' / 'pro'` | قراءة بحسب الجمهور. الإلزام بمطابقة `targetUid` عند توجيه المستخدم. | إنشاء المشرف لـ `target` بالأحرف الصغيرة وإلزام `targetUid` للمستخدم الفردي. | قراءة عامة لـ `all`، خاصة لـ `user` بشرط `targetUid == auth.uid`، ومقيدة لـ `pro` بشرط اشتراك نشط. | **ALIGNED** | تم تثبيتها في `AdminRepository.sendNotification`. |
| 16 | **الإشعارات القديمة بدون `target`** | حظر القراءة لغير المشرفين إذا غاب حقل `target` (موقف آمن ضد التسريب). | تم تحصين القواعد لتطابق سياسة الرفض الافتراضي (Fail-Closed). | **موقف الأمان الموحد (Fail-Closed):** المستندات التي تفتقد `target` تُحظر قراءتها على المستخدمين العاديين، ومتاحة للمشرف للتدقيق والترحيل. | **ALIGNED** | معتمد بالسطر 454 في `firestore.rules` ومجتاز في اختبارات المحاكي. |
| 17 | **وحدة مشروع Firebase وقواعد النشر الحية** | ملف قواعد تطبيق المستخدم مستقل ببصمة (`54c8570a...`). | ملف `firestore.rules` في لوحة الإدارة مستقل ببصمة (`207aade4...`). | لم يثبت اشتراك التطبيقين في مشروع سحابي واحد. ملفا القواعد مستقلان. | **UNVERIFIED** | لا يمكن إثبات توحيد القواعد المنشورة في السحابة دون مقارنة ملفي المصدر الفعليين وفحص بيئة النشر الحية. |

---

### 3. السياسات المعتمدة لإدارة البيانات المشتركة

#### أ. سياسة إلغاء طلبات CineStream Pro (Unified Lifecycle & Cancellation Contract):
1. **إنشاء الطلب من المستخدم:**
   - الحقول المقبولة: `requestId`, `userId`, `planId`, `durationDays`, `paymentReference`, `notes`, `createdAt`, `status: "PENDING"`.
   - القواعد تمنع وضع أي حقول إدارية أو اشتراكية (`reviewedBy`, `reviewedAt`, `subscriptionTier`, `isPremium`).
2. **إلغاء الطلب من المستخدم (`CANCELLED`):**
   - يُسمح لمالك الطلب وحده حصرياً بنقل حالة طلبه: `resource.data.userId == request.auth.uid && request.resource.data.userId == request.auth.uid`.
   - الحالة السابقة المقبولة في قاعدة البيانات: `PENDING` أو `pending` لضمان عدم حرمان أصحاب الطلبات المعلقة التاريخية المنشأة بأحرف صغيرة من الإلغاء.
   - الحالة الجديدة المسموح بها هي القيمة المعيارية المعتمدة حصرياً: `request.resource.data.status == 'CANCELLED'`.
   - الحقول المسموح بتعديلها محصورة في الحقول الثلاثة الحصرية: `['status', 'updatedAt', 'cancelledAt']` عبر `request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'updatedAt', 'cancelledAt'])`.
   - حظر تام على تعديل `userId` أو تفاصيل الخطة (`planId`, `durationDays`) أو بيانات الدفع والملاحظات (`paymentReference`, `notes`) أو أي من حقول المراجعة والاشتراك أثناء الإلغاء.
   - حظر إعادة فتح أي حالة نهائية (`APPROVED`, `REJECTED`, `CANCELLED`).
   - حظر الحذف على العميل نهائياً ويظل محصوراً بالمشرف (`allow delete: if isAdmin()`).
3. **مراجعة الإدارة والبت في الطلب (`APPROVED` / `REJECTED`):**
   - تتم العملية حصرياً عبر معاملة ذرية `firestore.runTransaction`.
   - عند الموافقة: يتم تحديث الطلب إلى `APPROVED` وترقية اشتراك المستخدم في `/users/{userId}` وتمديد الصلاحية من تاريخ الانتهاء النشط وتوثيق الإجراء في سجل التدقيق.
   - عند الرفض: يلزم توفير سبب الرفض `rejectionReason` ونقل الحالة إلى `REJECTED`.

#### ب. سياسة إجماليات النقاط ودلالة القراءة المتوافقة:
1. **الكتابة المزدوجة الذرية (Atomic Dual-Write):**
   - عند تعديل النقاط بواسطة المشرف عبر `AdminRepository.adjustUserPoints`، يتم التحديث الذري المتزامن لكلا الاسمين داخل نفس المعاملة:
     - `totalPointsEarned` و `pointsEarnedTotal` للقيمة المكتسبة الجديدة.
     - `totalPointsSpent` و `pointsSpentTotal` للقيمة المنفقة الجديدة.
2. **حظر الكتابة المباشرة للعميل:**
   - المستخدم العادي محظور قطعياً من كتابة أو تعديل أي من حقول النقاط الخمسة (`pointsBalance`, `totalPointsEarned`, `totalPointsSpent`, `pointsEarnedTotal`, `pointsSpentTotal`) عبر دالة `modifyingSensitiveUserFields()`.
   - دفتر الحركات `/users/{userId}/point_transactions` ومطالبات المهام `/task_claims` محمية ومحصورة بالمشرفين (`allow write: if isAdmin()`).
3. **دلالة القراءة المطبقة فعلياً (Zero-Preserving Null-Fallback Logic):**
   - في `app/src/main/java/com/example/models/Models.kt`:
     ```kotlin
     val effectivePointsEarned: Long
         get() = totalPointsEarned ?: pointsEarnedTotal ?: 0L

     val effectivePointsSpent: Long
         get() = totalPointsSpent ?: pointsSpentTotal ?: 0L
     ```
   - **تمييز غياب الحقل عن القيمة الصفرية:** الحقلان في نموذج `User` معرفان كـ `Long? = null`.
   - إذا كان الحقل الرئيسي موجوداً في الوثيقة بقيمة صفرية مشروعة (`0L`)، فإن معامل `?:` يحتفظ بالصفر المشين ولا يستبدله بحقل قديم غير متزامن (`pointsEarnedTotal > 0`).
   - إذا كان الحقل الرئيسي غائباً (`null`)، يتراجع المنطق إلى الحقل البديل `pointsEarnedTotal` (أو `0L` إذا كان كلاهما غائباً).

#### ج. سياسة محادثات وتذاكر الدعم الفني:
- معرف المستند هو UID المستخدم (`conversationId == userId`).
- الحالات المقبولة لإنشاء التذاكر الجديدة محصورة بالحروف الصغيرة حصرياً (`open`, `pending`).
- القواعد ترفض إنشاء تذاكر جديدة بحالات الحروف الكبيرة (`OPEN`, `PENDING`).
- لا يتم المساس أو الترحيل القسري للمستندات التاريخية القديمة.
- الطوابع الزمنية `lastMessageAt` و`lastMessageTime` يتم كتابتها متزامنة في كل رد إداري لضمان ثبات الترتيب في واجهات الطرفين.

#### د. سياسة الإشعارات وقواعد الجمهور (Fail-Closed):
- أي إشعار مجهول أو مفتقر لحقل `target` يُمنع عن المستخدمين العاديين لمنع تسريب أي رسائل خاصة قديمة.
- المشرف وحده يستطيع قراءة الإشعارات غير المصنفة لغايات التدقيق والترحيل الإداري الآمن.
