package com.rashot.audioguideai.data.model

@Entity(tableName = "poi")
data class PointOfInterest(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val isFavorite: Boolean = false
)