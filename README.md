# CineStream Admin Dashboard — Project Layout

هذا الفهرس يوضح أماكن الملفات حتى تبقى ملفات التطبيق الفعلية منفصلة عن أدوات الفحص والتوثيق التاريخي.

## ملفات تشغيل وإعدادات أساسية — أبقها في الجذر
- `app/`: مصدر تطبيق Android. ملف Google Services النشط هو `app/google-services.json`.
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/`, `gradlew`, `gradlew.bat`: إعدادات Gradle والبناء.
- `firebase.json`, `.firebaserc`, `firestore.rules`: إعداد Firebase CLI؛ الملف المحدد هو `firestore.rules` في الجذر.
- `debug.keystore`: شهادة Debug المحلية، وليست مفتاح Release.
- `.env.example`, `metadata.json`: إعدادات نموذجية وبيانات مساحة العمل.

## هوية Firebase
- `app/google-services.json`: ملف إعداد Android الذي يقرأه Google Services Gradle Plugin.
- `config/firebase/CINESTREAM_FIREBASE_IDENTITY_LOCK.json`: مرجع تستخدمه فحوص الهوية المساعدة، وليس بديلًا لملف Google Services.

## الفحص والاختبارات
- `qa/firestore-tests/`: أدوات اختبار قواعد Firestore وفحوص الهوية/التوافق الساكنة.
- `app/src/test/` و`app/src/androidTest/`: اختبارات Android في مسارات Gradle القياسية؛ لا تنقلها خارج `app/src`.
- لم تُشغّل اختبارات أو Gradle أثناء عملية التنظيم.

## التوثيق والمرجع
- `docs/PROJECT_BASELINE.md`: خط أساس تاريخي؛ توجد ملاحظات قديمة فيه، فراجع تقارير المراحل الأحدث عند الحاجة.
- `docs/archive/phase-history-before-reset/`: الأرشيف التاريخي السابق.
- `docs/archive/phase-reports/`: تقارير المراحل الأحدث المحفوظة.
- `docs/archive/maintenance-reports/`: تقارير تنظيم وصيانة سابقة.
- `docs/firebase/contracts/`: عقود البيانات.
- `docs/firebase/audits/`: تقارير تدقيق Firebase/Firestore.
- `docs/firebase/rules-history/candidates/`: مرشحات قواعد غير نشطة.
- `docs/firebase/rules-history/snapshots/` و`backups/`: نسخ مرجعية واحتياطية للقواعد.
- `PHASES/README.md`: فهرس المراحل النشطة.
- `docs/FILE_ORGANIZATION_MANIFEST.md`: سجل الملفات التي نُقلت ومساراتها الجديدة.

**تنبيه:** ملفات `docs/firebase/rules-history/` مرجعية فقط وليست أهداف نشر. لا تنشر قواعد Firestore دون مراجعة واعتماد منفصلين.
