package com.example.audioguideai.data.local

import androidx.room.*
import com.example.audioguideai.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {
    @Query("SELECT * FROM poi") fun all(): Flow<List<Poi>>
    @Query("SELECT * FROM poi WHERE osmId = :osmId") suspend fun byOsmId(osmId: Long): Poi?
    @Query("SELECT * FROM poi WHERE osmId = :osmId") suspend fun byId(osmId: Long): Poi?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(list: List<Poi>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(poi: Poi)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC") fun all(): Flow<List<HistoryItem>>
    @Insert suspend fun insert(item: HistoryItem)
    @Query("DELETE FROM history WHERE timestamp < :threshold") suspend fun deleteOlderThan(threshold: Long)
    @Query("DELETE FROM history") suspend fun clearAll()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite") fun all(): Flow<List<Favorite>>
    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE poiId=:poiId)") fun isFavorite(poiId: Long): Flow<Boolean>
    @Insert suspend fun add(f: Favorite)
    @Query("DELETE FROM favorite WHERE poiId=:poiId") suspend fun remove(poiId: Long)
}

@Dao
interface TagsDao {
    @Query("SELECT * FROM poi_tag WHERE poiId=:poiId") fun byPoi(poiId: Long): Flow<List<PoiTag>>
    @Insert suspend fun add(t: PoiTag)
    @Query("DELETE FROM poi_tag WHERE poiId=:poiId AND tag=:tag") suspend fun remove(poiId: Long, tag: String)
}
