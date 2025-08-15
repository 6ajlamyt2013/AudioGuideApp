package com.rashot.audioguideai.presentation.service

data class MapPoint(val latitude: Double, val longitude: Double) {
    fun toYandexPoint(): Point = Point(latitude, longitude)
}

@SuppressLint("MissingPermission")
fun getLocationUpdates(interval: Long): Flow<Location> = callbackFlow {
    val request = LocationRequest.create().apply {
        this.interval = interval
        this.priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.lastOrNull()?.let { location ->
                trySend(location)
            }
        }
    }

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        throw RuntimeException("Location permissions not granted")
    }

    client.requestLocationUpdates(
        request,
        callback,
        Looper.getMainLooper()
    )

    awaitClose {
        client.removeLocationUpdates(callback)
    }
}