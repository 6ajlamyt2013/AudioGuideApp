# AudioGuideAI

Реалтайм аудиогид (Android): геотрекинг (ForegroundService + Fused Location), системные геозоны (Geofencing API),
Wikipedia GeoSearch + extracts -> NLP-классификатор категорий, WorkManager предзагрузка, RU/EN локализация,
избранное, и голосовые команды «хватит/дальше». Карта — Yandex MapKit.

## Быстрый старт
1. В файле `~/.gradle/gradle.properties` добавьте:
   ```
   mapkit.key=YOUR_YANDEX_MAPKIT_API_KEY
   speech.key=YOUR_SPEECHKIT_API_KEY   # опционально
   ```
2. Откройте проект в Android Studio (Koala+), синхронизируйте Gradle.
3. Запросите у пользователя разрешения: точная/фоновая геолокация, уведомления (Android 13+), микрофон (для команд).
4. Сборка/запуск: приложение центрирует карту и озвучивает POI при входе в радиус или геозону.

## Примечания
- По умолчанию используется системный TTS. Подключить Yandex SpeechKit можно через реализацию `YandexSpeechEngine` (см. `domain/SpeechEngine.kt`).
- Геозоны создаются из текущего набора POI и радиуса в настройках.
