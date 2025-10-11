package com.example.audioguideai.geofencing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val pi: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context, 0, Intent(context, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("MissingPermission")
    fun setGeofences(points: List<Pair<Long, Triple<Double, Double, Float>>>) {
        client.removeGeofences(pi)
        if (points.isEmpty()) return
        val list = points.take(90).map { (id, triple) ->
            val (lat, lon, r) = triple
            Geofence.Builder()
                .setRequestId(id.toString())
                .setCircularRegion(lat, lon, r)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(15000)
                .build()
        }
        val req = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(list)
            .build()
        client.addGeofences(req, pi)
    }
}
