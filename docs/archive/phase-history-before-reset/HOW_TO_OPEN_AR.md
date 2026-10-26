# فتح مشروع CineStream داخل Google AI Studio

هذه الحزمة تحتوي على **ملفات المشروع الفعلية** وليست مجرد برومبتات. المجلد `CineStream-AdminB2-main/` هو جذر مشروع مستقل.

## ما الذي تحتويه الحزمة؟
- `app/src/main/`: ملفات التطبيق المصدرية الفعلية.
- `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradlew`: ملفات بناء مشروع Android/Gradle.
- `app/src/main/AndroidManifest.xml`: بيان تطبيق Android.
- `firestore.rules`, `firebase.json`, `docs/`: قواعد Firebase والعقود والتوثيق الموجود في المصدر.
- `PHASES/PHASE_20.1-B3-C7_FIREBASE_REMEDIATION_EXECUTION_MASTER.md`: دليل المرحلة التفصيلي الخاص بهذا المشروع.
- `AI_AGENT_START_HERE_AR.md`: فهرس القراءة الإلزامي للوكيل.
- `PROMPTS/`: برومبت البداية والاستمرار لهذا المشروع وحده.
- `REPAIR_PROGRESS_AR.md`: سجل التقدم.

## طريقة الاستخدام
1. استخرج ملف ZIP كاملًا؛ ينبغي أن تحصل على مجلد واحد باسم `CineStream-AdminB2-main` بدل خلط ملفاته ببقية التنزيلات.
2. افتح/استورد **هذا المجلد وحده** في بيئة التطوير التي تعمل بها، أو ارفع ملفات هذا المشروع وحده إلى مساحة عمل AI Studio التي تدعم استيراد المصدر.
3. في محادثة المشروع، انسخ محتوى `PROMPTS/01_START_ADMIN_DASHBOARD_AR.txt`.
4. يجب أن يقرأ الوكيل `AI_AGENT_START_HERE_AR.md` ثم ملف PHASE كاملًا قبل أن يغيّر ملفات المصدر.
5. لا تفتح مشروع User App وAdmin Dashboard في الجلسة نفسها؛ لكل مشروع ملف ZIP ومحادثة منفصلان.

## قيود المرحلة
- اختبارات Firebase Emulator وبناء Gradle مؤجلان إلى النهاية.
- لا تنشر `firestore.rules` ولا تكتب إلى Firebase الإنتاجي أثناء هذه المرحلة.
- لا تدّعِ نجاح بناء أو اختبار ما لم يُنفّذ فعلًا.
