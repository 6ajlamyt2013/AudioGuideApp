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
    val MIN_DISPLACEMENT_M = intPreferencesKey("min_displacement_m")
    val MAX_OBJECTS_PER_CYCLE = intPreferencesKey("max_objects_per_cycle")
    val PAUSE_BETWEEN_OBJECTS_MS = intPreferencesKey("pause_between_objects_ms")
    val VOICE_PITCH = floatPreferencesKey("voice_pitch")
    val HAS_SETTINGS = booleanPreferencesKey("has_settings")
}

class SettingsRepo(private val context: Context) {
    val settings = context.settingsDataStore.data.map { p ->
        val radius = p[PrefsKeys.RADIUS_M] ?: 500
        val interval = p[PrefsKeys.INTERVAL_MS] ?: 5000
        val cats = p[PrefsKeys.ENABLED_CATEGORIES] 
            ?: Category.values().filter { it.enabledByDefault }.map { it.name }.toSet()
        val speed = p[PrefsKeys.VOICE_SPEED] ?: 0.9f
        val minDisplacement = p[PrefsKeys.MIN_DISPLACEMENT_M] ?: 50
        val maxObjects = p[PrefsKeys.MAX_OBJECTS_PER_CYCLE] ?: 5
        val pauseBetween = p[PrefsKeys.PAUSE_BETWEEN_OBJECTS_MS] ?: 2000
        val pitch = p[PrefsKeys.VOICE_PITCH] ?: 1.0f
        val hasSettings = p[PrefsKeys.HAS_SETTINGS] ?: false
        
        Settings(
            radiusM = radius,
            intervalMs = interval,
            enabledCategories = cats,
            voiceSpeed = speed,
            minDisplacementM = minDisplacement,
            maxObjectsPerCycle = maxObjects,
            pauseBetweenObjectsMs = pauseBetween,
            voicePitch = pitch,
            hasSettings = hasSettings
        )
    }

    suspend fun setRadius(m: Int) = context.settingsDataStore.edit { it[PrefsKeys.RADIUS_M] = m }
    suspend fun setInterval(ms: Int) = context.settingsDataStore.edit { it[PrefsKeys.INTERVAL_MS] = ms }
    suspend fun setCategories(c: Set<String>) = context.settingsDataStore.edit { 
        it[PrefsKeys.ENABLED_CATEGORIES] = c
        it[PrefsKeys.HAS_SETTINGS] = true
    }
    suspend fun setVoiceSpeed(v: Float) = context.settingsDataStore.edit { it[PrefsKeys.VOICE_SPEED] = v }
    suspend fun setMinDisplacement(m: Int) = context.settingsDataStore.edit { it[PrefsKeys.MIN_DISPLACEMENT_M] = m }
    suspend fun setMaxObjectsPerCycle(n: Int) = context.settingsDataStore.edit { it[PrefsKeys.MAX_OBJECTS_PER_CYCLE] = n }
    suspend fun setPauseBetweenObjects(ms: Int) = context.settingsDataStore.edit { it[PrefsKeys.PAUSE_BETWEEN_OBJECTS_MS] = ms }
    suspend fun setVoicePitch(p: Float) = context.settingsDataStore.edit { it[PrefsKeys.VOICE_PITCH] = p }
    
    suspend fun resetToDefaults() = context.settingsDataStore.edit {
        it[PrefsKeys.RADIUS_M] = 500
        it[PrefsKeys.INTERVAL_MS] = 5000
        it[PrefsKeys.ENABLED_CATEGORIES] = Category.values().filter { it.enabledByDefault }.map { it.name }.toSet()
        it[PrefsKeys.VOICE_SPEED] = 0.9f
        it[PrefsKeys.MIN_DISPLACEMENT_M] = 50
        it[PrefsKeys.MAX_OBJECTS_PER_CYCLE] = 5
        it[PrefsKeys.PAUSE_BETWEEN_OBJECTS_MS] = 2000
        it[PrefsKeys.VOICE_PITCH] = 1.0f
    }
}

data class Settings(
    val radiusM: Int,
    val intervalMs: Int,
    val enabledCategories: Set<String>,
    val voiceSpeed: Float,
    val minDisplacementM: Int = 50,
    val maxObjectsPerCycle: Int = 5,
    val pauseBetweenObjectsMs: Int = 2000,
    val voicePitch: Float = 1.0f,
    val hasSettings: Boolean = false
)
