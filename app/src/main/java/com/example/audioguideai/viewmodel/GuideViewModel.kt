package com.example.audioguideai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioguideai.data.Repository
import com.example.audioguideai.data.SettingsRepo
import com.example.audioguideai.data.model.Poi
import com.example.audioguideai.domain.AndroidTtsEngine
import com.example.audioguideai.domain.GeoUtils
import com.example.audioguideai.geofencing.GeofenceManager
import com.example.audioguideai.location.LocationForegroundService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GuideViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val settings = SettingsRepo(app)
    private val tts = AndroidTtsEngine(app)

    private val _currentPoi = MutableStateFlow<Poi?>(null)
    val currentPoi: StateFlow<Poi?> = _currentPoi.asStateFlow()

    init {
        GeofenceEvents.subscribe { poiId ->
            viewModelScope.launch {
                repo.allPoi().firstOrNull()?.find { it.id == poiId }?.let { onEnterPoi(it) }
            }
        }
        viewModelScope.launch {
            combine(LocationForegroundService.lastLocationFlow.filterNotNull(), settings.settings) { loc, set ->
                Pair(loc, set)
            }.collect { (loc, set) ->
                android.util.Log.d("GuideViewModel", "📍 Location update: (${loc.latitude}, ${loc.longitude})")
                android.util.Log.d("GuideViewModel", "⚙️ Settings: radius=${set.radiusM}m, enabled categories=${set.enabledCategories.size}")
                val list = repo.allPoi().firstOrNull().orEmpty().filter { it.category.name in set.enabledCategories }
                android.util.Log.d("GuideViewModel", "🗺️ Total POIs: ${repo.allPoi().firstOrNull()?.size ?: 0}, Filtered: ${list.size}")
                GeofenceManager(getApplication()).setGeofences(list.map { it.id to Triple(it.lat, it.lon, set.radiusM.toFloat()) })
                val nearest = list.minByOrNull { p -> GeoUtils.distanceMeters(loc.latitude, loc.longitude, p.lat, p.lon) }
                if (nearest != null) {
                    val d = GeoUtils.distanceMeters(loc.latitude, loc.longitude, nearest.lat, nearest.lon)
                    android.util.Log.d("GuideViewModel", "🎯 Nearest POI: '${nearest.title}' at ${d.toInt()}m (threshold: ${set.radiusM}m)")
                    if (d <= set.radiusM) onEnterPoi(nearest)
                } else {
                    android.util.Log.d("GuideViewModel", "❌ No POIs found in the area")
                }
            }
        }
    }

    private var lastSpokenId: Long? = null
    private fun onEnterPoi(poi: Poi) {
        if (lastSpokenId == poi.id) return
        lastSpokenId = poi.id
        _currentPoi.value = poi
        val categoryText = poi.category.titleRu
        val description = poi.description ?: ""
        val speechText = "$categoryText. ${poi.title}. $description"
        android.util.Log.i("GuideViewModel", "🎤 SPEAKING: $speechText")
        tts.speak(speechText)
        viewModelScope.launch {
            val distance = repo.calculateDistance(
                LocationForegroundService.lastLocationFlow.value?.latitude ?: 0.0,
                LocationForegroundService.lastLocationFlow.value?.longitude ?: 0.0,
                poi.lat, poi.lon
            )
            repo.addHistory(poi, distance)
        }
    }

    fun stopSpeech() = tts.stop()
    fun nextPoi() { lastSpokenId = null }
}
