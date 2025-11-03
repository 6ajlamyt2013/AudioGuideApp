package com.example.audioguideai.data.remote

import java.net.URLEncoder

/**
 * Builder для создания Overpass QL запросов
 */
class OverpassQueryBuilder {
    private val categoryBlocks = mutableListOf<String>()
    private var radius = 500
    private var lat = 0.0
    private var lon = 0.0

    fun setRadius(meters: Int): OverpassQueryBuilder {
        radius = meters
        return this
    }

    fun setLocation(latitude: Double, longitude: Double): OverpassQueryBuilder {
        lat = latitude
        lon = longitude
        return this
    }

    fun addCategory(key: String, values: List<String>? = null, nodeOnly: Boolean = false): OverpassQueryBuilder {
        if (values != null && values.isNotEmpty()) {
            val regexValues = values.joinToString("|")
            // node
            categoryBlocks.add("node[\"$key\"~\"^($regexValues)$\"](around:$radius,$lat,$lon);")
            // way (если не nodeOnly)
            if (!nodeOnly) categoryBlocks.add("way[\"$key\"~\"^($regexValues)$\"](around:$radius,$lat,$lon);")
        } else {
            // все значения ключа
            categoryBlocks.add("node[\"$key\"](around:$radius,$lat,$lon);")
            if (!nodeOnly) categoryBlocks.add("way[\"$key\"](around:$radius,$lat,$lon);")
        }
        return this
    }

    fun build(): String {
        val queryBlocks = categoryBlocks.joinToString("")
        val query = "[out:json][timeout:30];($queryBlocks);out center;"
        return URLEncoder.encode(query, "UTF-8")
    }
}

