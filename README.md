# Проверка.29 — Android

Kotlin WebView-приложение для https://proverka29.ru

- package: ru.proverka29.app
- minSdk 24, targetSdk 34, compileSdk 35
- versionCode 1, versionName 1.0
- AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9, JDK 17

## Возможности
WebView, JavaScript, DOM Storage, cookies, обработка tel/mailto/intent-ссылок, выбор файлов и камеры, разрешения камеры/микрофона, свайп-обновление, индикатор загрузки, офлайн-заглушка, кнопка назад, заставка, меню и нативный экран «О приложении». HTML-виджет находится в assets/widget.html и открывается из меню. Использовано компактное всплывающее меню: оно не отнимает место у сайта и не требует отдельной Drawer-библиотеки.

## Firebase
google-services.json сейчас содержит демонстрационные значения. Создайте проект на https://console.firebase.google.com/, добавьте Android-приложение с package ru.proverka29.app и замените файл настоящим google-services.json. До этого реальные push-уведомления не будут доставляться. Сервис FCM уже добавлен.

## Сборка
Android Studio + JDK 17: откройте папку, дождитесь Gradle Sync, затем Build → Build Bundle(s) / APK(s) → Build APK(s). APK: app/build/outputs/apk/debug/app-debug.apk.

GitHub Actions собирает APK при каждом push в main. Артефакт: Proverka29-debug-apk.

Для release: Build → Generate Signed Bundle / APK → APK → Create new keystore. Храните keystore и пароли отдельно, не коммитьте их.

## Иконка
Замените drawable/ic_launcher.xml своей иконкой или добавьте PNG в mipmap-mdpi (48x48), hdpi (72x72), xhdpi (96x96), xxhdpi (144x144), xxxhdpi (192x192) и поменяйте ссылки в Manifest.

## Важно
Телефон +7 (800) 000-00-00 взят из задания. Email поддержки не был указан: кнопка открывает пустое письмо, не подставляя выдуманный адрес. HTML-реестр и тарифы — демонстрационные данные из присланного фрагмента. Проверка домена — эвристика, не антивирус.
