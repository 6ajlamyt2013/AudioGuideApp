package com.example.audioguideai.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.audioguideai.viewmodel.GeofenceEvents

class GeofenceReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GeofenceEvents.handleIntent(context, intent)
    }
}
