package com.example.audioguideai.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.audioguideai.data.model.Category
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore("settings")

object PrefsKeys {
    val RADIUS_M = intPreferencesKey("radius_m")
    val INTERVAL_MS = intPreferencesKey("interval_ms")
    val ENABLED_CATEGORIES = stringSetPreferencesKey("enabled_categories")
    val VOICE_SPEED = floatPreferencesKey("voice_speed")
}

class SettingsRepo(private val context: Context) {
    val settings = context.settingsDataStore.data.map { p ->
        val radius = p[PrefsKeys.RADIUS_M] ?: 150
        val interval = p[PrefsKeys.INTERVAL_MS] ?: 5000
        val cats = p[PrefsKeys.ENABLED_CATEGORIES] ?: Category.values().map { it.name }.toSet()
        val speed = p[PrefsKeys.VOICE_SPEED] ?: 1.0f
        Settings(radius, interval, cats, speed)
    }

    suspend fun setRadius(m: Int) = context.settingsDataStore.edit { it[PrefsKeys.RADIUS_M] = m }
    suspend fun setInterval(ms: Int) = context.settingsDataStore.edit { it[PrefsKeys.INTERVAL_MS] = ms }
    suspend fun setCategories(c: Set<String>) = context.settingsDataStore.edit { it[PrefsKeys.ENABLED_CATEGORIES] = c }
    suspend fun setVoiceSpeed(v: Float) = context.settingsDataStore.edit { it[PrefsKeys.VOICE_SPEED] = v }
}

data class Settings(
    val radiusM: Int,
    val intervalMs: Int,
    val enabledCategories: Set<String>,
    val voiceSpeed: Float,
)
