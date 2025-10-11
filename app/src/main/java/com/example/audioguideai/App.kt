package com.example.audioguideai

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val key = BuildConfig.MAPKIT_API_KEY.ifEmpty { "YOUR_YANDEX_MAPKIT_API_KEY" }
        MapKitFactory.setApiKey(key)
        MapKitFactory.initialize(this)

    }
}

