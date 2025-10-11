package com.example.audioguideai.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PrefetchWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repo = Repository.get(applicationContext)
        val points = inputData.getString("waypoints")?.split(";")?.mapNotNull { s ->
            val p = s.split(",")
            if (p.size == 2) p[0].toDoubleOrNull()?.let { lat -> p[1].toDoubleOrNull()?.let { lon -> lat to lon } } else null
        }.orEmpty()
        for ((lat, lon) in points) repo.refreshAround(lat, lon)
        return Result.success()
    }
}
