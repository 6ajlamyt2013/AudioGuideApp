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
                repo.refreshAround(loc.latitude, loc.longitude)
                val list = repo.allPoi().firstOrNull().orEmpty().filter { it.category.name in set.enabledCategories }
                // геозоны
                GeofenceManager(getApplication()).setGeofences(list.map { it.id to Triple(it.lat, it.lon, set.radiusM.toFloat()) })
                // ближайший
                val nearest = list.minByOrNull { p -> GeoUtils.distanceMeters(loc.latitude, loc.longitude, p.lat, p.lon) }
                if (nearest != null) {
                    val d = GeoUtils.distanceMeters(loc.latitude, loc.longitude, nearest.lat, nearest.lon)
                    if (d <= set.radiusM) onEnterPoi(nearest)
                }
            }
        }
    }

    private var lastSpokenId: Long? = null
    private fun onEnterPoi(poi: Poi) {
        if (lastSpokenId == poi.id) return
        lastSpokenId = poi.id
        _currentPoi.value = poi
        tts.speak("${'$'}{poi.title}. ${'$'}{poi.description}")
        viewModelScope.launch { repo.addHistory(poi.id) }
    }

    fun stopSpeech() = tts.stop()
    fun nextPoi() { lastSpokenId = null }
}
