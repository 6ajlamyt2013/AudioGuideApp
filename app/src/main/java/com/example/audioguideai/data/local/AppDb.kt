package com.example.audioguideai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.audioguideai.data.model.*

@Database(entities = [Poi::class, HistoryItem::class, Favorite::class, PoiTag::class], version = 2)
abstract class AppDb : RoomDatabase() {
    abstract fun poiDao(): PoiDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun tagsDao(): TagsDao
}
