package com.example.audioguideai.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.reflect.Type

interface OverpassApi {
    @GET("interpreter")
    suspend fun query(
        @Query(value = "data", encoded = true) query: String
    ): OverpassResponse
}

data class OverpassResponse(
    val version: Double?,
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    @JsonAdapter(TagsDeserializer::class)
    val tags: Map<String, String>?
)

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

class TagsDeserializer : JsonDeserializer<Map<String, String>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Map<String, String>? {
        if (json == null || !json.isJsonObject) return null
        
        val map = mutableMapOf<String, String>()
        json.asJsonObject.entrySet().forEach { entry ->
            val value = entry.value
            if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                map[entry.key] = value.asString
            }
        }
        return if (map.isEmpty()) null else map
    }
}

