package com.example.audioguideai.data

import android.content.Context
import androidx.room.Room
import com.example.audioguideai.BuildConfig
import com.example.audioguideai.data.local.AppDb
import com.example.audioguideai.data.local.HistoryDao
import com.example.audioguideai.data.local.MIGRATION_1_2
import com.example.audioguideai.data.local.PoiDao
import com.example.audioguideai.data.model.*
import com.example.audioguideai.data.remote.OverpassApi
import com.example.audioguideai.data.remote.OverpassQueryBuilder
import com.example.audioguideai.domain.CategoryClassifier
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class Repository private constructor(context: Context) {
    private val db: AppDb = Room.databaseBuilder(context, AppDb::class.java, "audio_guide.db")
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration() // Временно для разработки
        .build()
    private val poiDao: PoiDao = db.poiDao()
    private val historyDao: HistoryDao = db.historyDao()

    private val overpassClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val overpassRetrofit = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/api/")
        .client(overpassClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val overpassApi = overpassRetrofit.create(OverpassApi::class.java)

    fun allPoi(): Flow<List<Poi>> = poiDao.all()
    fun history(): Flow<List<HistoryItem>> = historyDao.all()

    suspend fun addHistory(poi: Poi, distance: Float) {
        val categoryIcon = poi.category.icon
        val categoriesJson = Gson().toJson(poi.categories)
        historyDao.insert(HistoryItem(
            poiOsmId = poi.osmId,
            timestamp = System.currentTimeMillis(),
            name = poi.title,
            latitude = poi.lat,
            longitude = poi.lon,
            distance = distance,
            categories = categoriesJson,
            categoryIcon = categoryIcon
        ))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) { historyDao.clearAll() }

    suspend fun cleanupOldHistory() = withContext(Dispatchers.IO) {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        historyDao.deleteOlderThan(sevenDaysAgo)
    }

    /**
     * Обратная совместимость для старых вызовов
     */
    suspend fun refreshAround(lat: Double, lon: Double) {
        try {
            val defaultCategories = Category.values().map { it.name }.toSet()
            fetchOverpassPOIs(lat = lat, lon = lon, radius = 500, enabledCategories = defaultCategories)
        } catch (_: OverpassException) {
            // Глушим сетевые исключения, чтобы не падать на UI-потоке
        } catch (_: Exception) {
        }
    }

    /**
     * Запрос объектов через Overpass API на основе выбранных категорий
     */
    suspend fun fetchOverpassPOIs(
        lat: Double,
        lon: Double,
        radius: Int,
        enabledCategories: Set<String>
    ): List<Poi> = withContext(Dispatchers.IO) {
        try {
            val queryBuilder = OverpassQueryBuilder()
                .setLocation(lat, lon)
                .setRadius(radius)

            Category.values().forEach { category ->
                if (category.name in enabledCategories) {
                    queryBuilder.addCategory(
                        key = category.osmKey,
                        values = category.osmValues,
                        nodeOnly = category.nodeOnly
                    )
                }
            }

            val query = queryBuilder.build()
            android.util.Log.d("Repository", "🔍 Overpass query: ${URLDecoder.decode(query, "UTF-8")}")

            val response = overpassApi.query(query)
            android.util.Log.d("Repository", "✅ Found ${response.elements.size} elements from Overpass API")

            val pois = response.elements.mapNotNull { element ->
                try {
                    val (poiLat, poiLon) = when {
                        element.type == "node" && element.lat != null && element.lon != null -> Pair(element.lat, element.lon)
                        element.type == "way" && element.center != null -> Pair(element.center.lat, element.center.lon)
                        else -> return@mapNotNull null
                    }

                    val name = element.tags?.get("name:ru") ?: element.tags?.get("name")
                    if (name.isNullOrBlank()) return@mapNotNull null

                    val detectedCategories = mutableListOf<String>()
                    element.tags?.forEach { (key, value) ->
                        Category.values().forEach { cat ->
                            if (cat.osmKey == key) {
                                if (cat.osmValues == null || cat.osmValues.contains(value)) {
                                    detectedCategories.add(cat.name)
                                }
                            }
                        }
                    }

                    val category = if (detectedCategories.isNotEmpty()) {
                        Category.values().find { it.name == detectedCategories.first() } ?: Category.TOURISM
                    } else {
                        CategoryClassifier.classify(name, element.tags?.get("description") ?: "")
                    }

                    val description = element.tags?.get("description")
                        ?: element.tags?.get("note")
                        ?: element.tags?.get("tourism")

                    Poi(
                        osmId = element.id,
                        type = element.type,
                        title = name,
                        description = description,
                        lat = poiLat,
                        lon = poiLon,
                        category = category,
                        categories = detectedCategories,
                        distanceFromUser = 0f,
                        isAnnounced = false
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Repository", "❌ Error parsing element ${element.id}: ${e.message}", e)
                    null
                }
            }

            if (pois.isNotEmpty()) {
                poiDao.upsertAll(pois)
                android.util.Log.i("Repository", "✅ Successfully loaded ${pois.size} POIs from Overpass API")
            } else {
                android.util.Log.w("Repository", "⚠️ No POIs found in the area")
            }

            pois
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                429 -> throw OverpassException.RateLimitException()
                504 -> throw OverpassException.TimeoutException()
                in 500..599 -> throw OverpassException.ServerException(e.code())
                else -> throw OverpassException.HttpException(e.code())
            }
        } catch (e: java.net.UnknownHostException) {
            throw OverpassException.NoInternetException()
        } catch (e: Exception) {
            throw OverpassException.UnknownException(e)
        }
    }

    sealed class OverpassException(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class RateLimitException : OverpassException("Превышен лимит запросов")
        class TimeoutException : OverpassException("Таймаут запроса")
        class ServerException(code: Int) : OverpassException("Ошибка сервера: $code")
        class HttpException(code: Int) : OverpassException("HTTP ошибка: $code")
        class NoInternetException : OverpassException("Нет подключения к интернету")
        class UnknownException(cause: Throwable) : OverpassException("Неизвестная ошибка", cause)
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }

    companion object {
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
