package com.example.audioguideai.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WikipediaApi {
    @GET("w/api.php?action=query&list=geosearch&format=json&gsradius=1000&gslimit=20")
    suspend fun geoSearch(@Query("gscoord") coord: String): GeoSearchResponse

    @GET("w/api.php?action=query&prop=extracts&exintro=true&explaintext=true&format=json")
    suspend fun extracts(@Query("pageids") pageIds: String): ExtractsResponse
}

data class GeoSearchResponse(val query: GeoQuery?)
data class GeoQuery(val geosearch: List<GeoItem>)
data class GeoItem(val pageid: Long, val title: String, val lat: Double, val lon: Double)

data class ExtractsResponse(val query: ExtractsQuery?)
data class ExtractsQuery(val pages: Map<String, ExtractPage>)
data class ExtractPage(val pageid: Long, val title: String, val extract: String?)
