package com.rashot.audioguideai.presentation.viewmodel

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
    private val poiRepository: POIRepository,
    private val ttsService: TextToSpeechService
) : ViewModel() {
    private val _uiState = mutableStateOf(MainScreenState())
    val uiState: State<MainScreenState> = _uiState

    fun startTracking() {
        viewModelScope.launch {
            locationTracker.getLocationUpdates(5000L).collect { location ->
                _uiState.value = _uiState.value.copy(
                    currentLocation = location,
                    isActive = true
                )
                checkNearbyPOIs(location)
            }
        }
    }

    private suspend fun checkNearbyPOIs(location: Location) {
        val pois = poiRepository.getNearbyPOIs(
            location = location,
            radius = 200,
            categories = listOf("museum", "park")
        )
        pois.firstOrNull()?.let { poi ->
            _uiState.value = _uiState.value.copy(
                currentPoi = poi,
                distanceToPoi = calculateDistance(location, poi)
            )
            ttsService.speak(poi.description)
        }
    }

    private fun calculateDistance(location: Location, poi: PointOfInterest): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            poi.latitude,
            poi.longitude,
            results
        )
        return results[0]
    }
}

data class MainScreenState(
    val currentLocation: Location? = null,
    val currentPoi: PointOfInterest? = null,
    val distanceToPoi: Float = 0f,
    val isActive: Boolean = false
)