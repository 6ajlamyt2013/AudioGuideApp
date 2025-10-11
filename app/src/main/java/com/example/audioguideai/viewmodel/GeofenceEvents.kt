package com.example.audioguideai.viewmodel

import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

object GeofenceEvents {
    private var handler: ((Long) -> Unit)? = null
    fun subscribe(h: (Long) -> Unit) { handler = h }
    fun unsubscribe() { handler = null }
    fun handleIntent(ctx: Context, intent: Intent) {
        val ev = GeofencingEvent.fromIntent(intent) ?: return
        if (ev.hasError()) return
        val ids = ev.triggeringGeofences?.mapNotNull { it.requestId.toLongOrNull() }.orEmpty()
        if (ev.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            ev.geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            ids.firstOrNull()?.let { handler?.invoke(it) }
        }
    }
}
