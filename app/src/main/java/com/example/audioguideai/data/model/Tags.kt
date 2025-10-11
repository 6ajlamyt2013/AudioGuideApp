package com.example.audioguideai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poi_tag")
data class PoiTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poiId: Long,
    val tag: String,
)
