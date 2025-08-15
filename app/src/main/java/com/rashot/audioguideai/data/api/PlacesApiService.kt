package com.rashot.audioguideai.data.api

interface PlacesApiService {
    @GET("nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") key: String
    ): PlacesResponse

    data class PlacesResponse(val results: List<Place>)
    data class Place(
        val place_id: String,
        val name: String,
        val geometry: Geometry,
        val types: List<String>
    )
    data class Geometry(val location: Location)
    data class Location(val lat: Double, val lng: Double)
}