package com.example.audioguideai.data

import android.content.Context
import androidx.room.Room
import com.example.audioguideai.data.local.AppDb
import com.example.audioguideai.data.local.HistoryDao
import com.example.audioguideai.data.local.PoiDao
import com.example.audioguideai.data.model.*
import com.example.audioguideai.data.remote.WikipediaApi
import com.example.audioguideai.domain.CategoryClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Repository private constructor(context: Context) {
    private val db: AppDb = Room.databaseBuilder(context, AppDb::class.java, "audio_guide.db").build()
    private val poiDao: PoiDao = db.poiDao()
    private val historyDao: HistoryDao = db.historyDao()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://en.wikipedia.org/")
        .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val wiki = retrofit.create(WikipediaApi::class.java)

    fun allPoi(): Flow<List<Poi>> = poiDao.all()
    fun history(): Flow<List<HistoryItem>> = historyDao.all()

    suspend fun addHistory(poiId: Long) { historyDao.insert(HistoryItem(poiId = poiId, timestamp = System.currentTimeMillis())) }

    suspend fun refreshAround(lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        val geo = wiki.geoSearch("$lat|$lon").query?.geosearch.orEmpty()
        if (geo.isEmpty()) return@withContext
        val ids = geo.joinToString(",") { it.pageid.toString() }
        val extracts = wiki.extracts(ids).query?.pages.orEmpty()
        val mapped = geo.map { g ->
            val ex = extracts[g.pageid.toString()]?.extract ?: ""
            val cat = CategoryClassifier.classify(g.title, ex)
            Poi(
                title = g.title,
                description = ex,
                lat = g.lat,
                lon = g.lon,
                category = cat
            )
        }
        poiDao.upsertAll(mapped)
    }

    companion object {
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
