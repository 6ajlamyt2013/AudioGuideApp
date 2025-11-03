package com.example.audioguideai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "poi")
@TypeConverters(CategoryConverter::class, StringListConverter::class)
data class Poi(
    @PrimaryKey val osmId: Long,  // OSM ID (уникальный идентификатор из OpenStreetMap)
    val type: String,              // "node" или "way"
    val title: String,
    val description: String? = null,
    val lat: Double,
    val lon: Double,
    val category: Category,
    val categories: List<String> = emptyList(), // Список OSM тегов для категоризации
    val distanceFromUser: Float = 0f,
    val isAnnounced: Boolean = false
) {
    // Для обратной совместимости с существующим кодом
    val id: Long get() = osmId
}

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poiOsmId: Long,  // OSM ID вместо локального ID
    val timestamp: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Float,
    val categories: String,  // JSON строка с категориями
    val categoryIcon: String
)

// Конвертеры для Room
class CategoryConverter {
    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(name: String): Category = Category.valueOf(name)
}

class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}
