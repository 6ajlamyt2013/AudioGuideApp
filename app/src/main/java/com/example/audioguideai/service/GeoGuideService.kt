package com.example.audioguideai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.audioguideai.MainActivity
import com.example.audioguideai.R
import com.example.audioguideai.data.Repository
import com.example.audioguideai.data.SettingsRepo
import com.example.audioguideai.data.model.Poi
import com.example.audioguideai.location.LocationForegroundService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class GeoGuideService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: Repository
    private lateinit var settingsRepo: SettingsRepo
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitializing = false

    private var isRunning = false
    private var lastLocation: Location? = null
    private val announcedIds = mutableSetOf<Long>()

    // Очередь POI для озвучивания
    private val speechQueue = ConcurrentLinkedQueue<SpeechItem>()
    private var isSpeaking = false

    data class SpeechItem(
        val poi: Poi,
        val text: String,
        val settings: com.example.audioguideai.data.Settings
    )

    companion object {
        const val CHANNEL_ID = "geoguide_channel"
        const val NOTIFICATION_ID = 1002
        private const val TAG = "GeoGuideService"

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow = _isRunningFlow

        fun start(context: Context) {
            val intent = Intent(context, GeoGuideService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GeoGuideService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Service onCreate")
        repository = Repository.get(applicationContext)
        settingsRepo = SettingsRepo(applicationContext)
        createNotificationChannel()

        // Запускаем инициализацию TTS в фоне
        serviceScope.launch(Dispatchers.Main) {
            initializeTTS()
        }

        // Запускаем обработчик очереди речи
        serviceScope.launch {
            processSpeechQueue()
        }
    }

    private suspend fun initializeTTS() = withContext(Dispatchers.Main) {
        if (ttsInitializing) {
            Log.w(TAG, "⚠️ TTS already initializing")
            return@withContext
        }

        ttsInitializing = true
        Log.d(TAG, "🔄 Starting TTS initialization (non-blocking)")

        // Проверяем доступность TTS движка
        val checkIntent = Intent()
        checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        val activities = packageManager.queryIntentActivities(checkIntent, 0)

        if (activities.isEmpty()) {
            Log.e(TAG, "❌ No TTS engine found on device")
            ttsInitializing = false
            return@withContext
        }

        try {
            tts = TextToSpeech(this@GeoGuideService) { status ->
                ttsInitializing = false
                Log.d(TAG, "🔄 TTS initialization callback, status: $status")

                if (status == TextToSpeech.SUCCESS) {
                    tts?.let { engine ->
                        val localeRU = Locale("en", "EN")
                        val result = engine.setLanguage(localeRU)

                        when (result) {
                            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                                Log.w(TAG, "⚠️ Russian language not available, trying English")
                                val enResult = engine.setLanguage(Locale.US)
                                ttsReady = (enResult != TextToSpeech.LANG_MISSING_DATA &&
                                        enResult != TextToSpeech.LANG_NOT_SUPPORTED)
                                if (ttsReady) {
                                    Log.w(TAG, "⚠️ Using English as fallback")
                                }
                            }
                            else -> {
                                Log.d(TAG, "✅ Russian language set successfully")
                                ttsReady = true
                            }
                        }

                        if (ttsReady) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                engine.setAudioAttributes(
                                    android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .build()
                                )
                            }
                            setupTTSListener(engine)
                            Log.d(TAG, "✅ TTS fully initialized and ready")
                        } else {
                            Log.e(TAG, "❌ No suitable language found for TTS")
                        }
                    }
                } else {
                    Log.e(TAG, "❌ TTS initialization failed with status: $status")
                    ttsReady = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during TTS initialization", e)
            ttsInitializing = false
            ttsReady = false
        }
    }

    private fun setupTTSListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "🔊 TTS started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "✅ TTS done: $utteranceId")
                isSpeaking = false
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "❌ TTS error: $utteranceId")
                isSpeaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "❌ TTS error: $utteranceId, code: $errorCode")
                isSpeaking = false
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification("Запуск сервиса..."))

        if (!isRunning) {
            isRunning = true
            _isRunningFlow.value = true
            serviceScope.launch { mainLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 Service onDestroy")
        isRunning = false
        _isRunningFlow.value = false

        tts?.let {
            it.stop()
            it.shutdown()
        }
        tts = null
        ttsReady = false

        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun mainLoop() = withContext(Dispatchers.Default) {
        Log.d(TAG, "✅ Main loop started (TTS initialization in background)")
        updateNotification("Сервис запущен")

        while (isRunning) {
            try {
                val settings = settingsRepo.settings.firstOrNull()
                if (settings != null) {
                    val currentLocation = LocationForegroundService.lastLocationFlow.value

                    if (currentLocation != null) {
                        Log.d(TAG, "📍 Location: ${currentLocation.latitude}, ${currentLocation.longitude}")

                        val shouldQuery = if (lastLocation == null) {
                            true
                        } else {
                            val distance = repository.calculateDistance(
                                lastLocation!!.latitude, lastLocation!!.longitude,
                                currentLocation.latitude, currentLocation.longitude
                            )
                            Log.d(TAG, "📏 Distance: ${distance}m (min: ${settings.minDisplacementM}m)")
                            distance >= settings.minDisplacementM
                        }

                        if (shouldQuery) {
                            lastLocation = currentLocation
                            updateNotification("Поиск объектов...")
                            Log.d(TAG, "🔍 Querying POIs...")

                            val pois = try {
                                repository.fetchOverpassPOIs(
                                    currentLocation.latitude,
                                    currentLocation.longitude,
                                    settings.radiusM,
                                    settings.enabledCategories
                                )
                            } catch (e: Repository.OverpassException) {
                                handleOverpassException(e)
                                null
                            }

                            if (pois != null) {
                                Log.d(TAG, "📦 Received ${pois.size} POIs")
                                val newPois = pois.filter { it.osmId !in announcedIds }
                                Log.d(TAG, "🆕 New POIs: ${newPois.size}")

                                if (newPois.isNotEmpty()) {
                                    processNewPois(newPois, currentLocation, settings)
                                } else {
                                    updateNotification("Новые объекты не найдены")
                                    delay(10000)
                                }
                            }
                        } else {
                            val ttsStatus = if (ttsReady) "TTS готов" else "TTS инициализируется"
                            updateNotification("Ожидание перемещения... ($ttsStatus)")
                            delay(5000)
                        }
                    } else {
                        updateNotification("Ожидание GPS...")
                        Log.w(TAG, "⚠️ No location")
                        delay(5000)
                    }
                } else {
                    Log.w(TAG, "⚠️ No settings")
                    delay(5000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in main loop", e)
                updateNotification("Ошибка: ${e.message}")
                delay(5000)
            }
        }
    }

    private suspend fun handleOverpassException(e: Repository.OverpassException) {
        when (e) {
            is Repository.OverpassException.RateLimitException -> {
                updateNotification("Лимит запросов. Ожидание 60 сек")
                delay(60000)
            }
            is Repository.OverpassException.TimeoutException -> {
                updateNotification("Таймаут. Повтор через 30 сек")
                delay(30000)
            }
            is Repository.OverpassException.NoInternetException -> {
                updateNotification("Нет интернета")
                delay(30000)
            }
            else -> {
                updateNotification("Ошибка сервера")
                delay(60000)
            }
        }
    }

    private suspend fun processNewPois(
        newPois: List<Poi>,
        currentLocation: Location,
        settings: com.example.audioguideai.data.Settings
    ) {
        val sortedPois = newPois.map { poi ->
            poi.copy(
                distanceFromUser = repository.calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    poi.lat,
                    poi.lon
                )
            )
        }
            .sortedBy { it.distanceFromUser }
            .take(settings.maxObjectsPerCycle)

        Log.d(TAG, "🎯 Found ${sortedPois.size} POIs to announce")
        updateNotification("Найдено ${sortedPois.size} объектов")

        // Добавляем POI в очередь озвучивания
        for (poi in sortedPois) {
            val speechText = "${poi.title}. Находится в ${formatDistance(poi.distanceFromUser)} от вас."
            speechQueue.offer(SpeechItem(poi, speechText, settings))

            // Отмечаем как объявленный и добавляем в историю сразу
            announcedIds.add(poi.osmId)
            repository.addHistory(poi, poi.distanceFromUser)
            Log.d(TAG, "📝 Added to speech queue: ${poi.title}")
        }

        val queueSize = speechQueue.size
        val status = if (ttsReady) "TTS готов" else "TTS недоступен"
        updateNotification("В очереди: $queueSize объектов ($status)")

        // Не блокируем основной цикл - озвучивание идет параллельно
        delay(5000)
    }

    private suspend fun processSpeechQueue() = withContext(Dispatchers.Default) {
        Log.d(TAG, "🎤 Speech queue processor started")

        while (isRunning) {
            try {
                val item = speechQueue.poll()

                if (item != null) {
                    if (ttsReady && tts != null && !isSpeaking) {
                        speakPoi(item)
                    } else {
                        // Если TTS не готов или уже говорит, возвращаем в очередь
                        speechQueue.offer(item)

                        if (!ttsReady && !ttsInitializing) {
                            // Пробуем переинициализировать TTS
                            Log.w(TAG, "⚠️ TTS not ready, attempting reinitialization")
                            withContext(Dispatchers.Main) {
                                initializeTTS()
                            }
                        }

                        delay(1000) // Ждем перед следующей попыткой
                    }
                } else {
                    // Очередь пуста, ждем
                    delay(500)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in speech queue processor", e)
                delay(1000)
            }
        }

        Log.d(TAG, "🛑 Speech queue processor stopped")
    }

    private suspend fun speakPoi(item: SpeechItem) = withContext(Dispatchers.Main) {
        val engine = tts ?: return@withContext

        isSpeaking = true
        Log.d(TAG, "🔊 Speaking: ${item.text}")

        try {
            engine.setSpeechRate(item.settings.voiceSpeed)
            engine.setPitch(item.settings.voicePitch)

            val utteranceId = "poi_${item.poi.osmId}_${System.currentTimeMillis()}"

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                engine.speak(item.text, TextToSpeech.QUEUE_ADD, params, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                val params = hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId)
                @Suppress("DEPRECATION")
                engine.speak(item.text, TextToSpeech.QUEUE_ADD, params)
            }

            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "❌ TTS speak() returned ERROR")
                isSpeaking = false
                return@withContext
            }

            // Ждем завершения с таймаутом
            var waitCount = 0
            while (isSpeaking && isRunning && waitCount < 300) { // 30 секунд максимум
                delay(100)
                waitCount++
            }

            if (waitCount >= 300) {
                Log.w(TAG, "⚠️ TTS timeout")
                engine.stop()
                isSpeaking = false
            }

            // Пауза между объектами
            delay(item.settings.pauseBetweenObjectsMs.toLong())

            Log.d(TAG, "✅ Announced: ${item.poi.title}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error speaking POI", e)
            isSpeaking = false
        }
    }

    private fun formatDistance(meters: Float): String =
        if (meters < 1000) {
            "${meters.toInt()} метрах"
        } else {
            String.format(Locale("en"), "%.1f километрах", meters / 1000f)
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GeoGuide Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновая работа аудиогида"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GeoGuideService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("GeoGuide")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stat_name, "Стоп", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, createNotification(text))
    }
}