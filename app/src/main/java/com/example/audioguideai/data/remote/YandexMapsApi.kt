package com.example.audioguideai.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface YandexMapsApi {
    @GET("search")
    suspend fun search(
        @Query("apikey") apikey: String,
        @Query("text") text: String,
        @Query("ll") ll: String, // longitude,latitude
        @Query("spn") spn: String, // span
        @Query("rspn") rspn: Int = 1, // restrict to span
        @Query("results") results: Int = 20,
        @Query("lang") lang: String = "ru_RU",
        @Query("format") format: String = "json"
    ): YandexSearchResponse
}

data class YandexSearchResponse(
    val type: String,
    val properties: YandexSearchProperties,
    val features: List<YandexFeature>
)

data class YandexSearchProperties(
    val ResponseMetaData: YandexResponseMetaData
)

data class YandexResponseMetaData(
    val SearchResponse: YandexSearchMetaData
)

data class YandexSearchMetaData(
    val found: Int,
    val display: String
)

data class YandexFeature(
    val type: String,
    val geometry: YandexGeometry,
    val properties: YandexFeatureProperties
)

data class YandexGeometry(
    val type: String,
    val coordinates: List<Double> // [longitude, latitude]
)

data class YandexFeatureProperties(
    val name: String,
    val description: String?,
    val CompanyMetaData: YandexCompanyMetaData?
)

data class YandexCompanyMetaData(
    val id: String,
    val name: String,
    val address: String?,
    val Categories: List<YandexCategory>?,
    val Phones: List<YandexPhone>?,
    val Hours: YandexHours?,
    val description: String?
)

data class YandexCategory(
    val `class`: String,
    val name: String
)

data class YandexPhone(
    val type: String,
    val formatted: String
)

data class YandexHours(
    val text: String,
    val Availabilities: List<YandexAvailability>
)

data class YandexAvailability(
    val Everyday: Boolean,
    val TwentyFourHours: Boolean
)

