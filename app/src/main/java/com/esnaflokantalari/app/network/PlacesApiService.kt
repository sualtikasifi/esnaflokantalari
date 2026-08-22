package com.esnaflokantalari.app.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Places API (Text Search / Nearby Search, "Legacy" uçları).
 * https://developers.google.com/maps/documentation/places/web-service
 */
interface PlacesApiService {

    @GET("maps/api/place/textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("type") type: String = "restaurant",
        @Query("language") language: String = "tr",
        @Query("key") apiKey: String,
    ): PlacesTextSearchResponse

    @GET("maps/api/place/nearbysearch/json")
    suspend fun nearbySearch(
        @Query("location") location: String,
        @Query("radius") radiusMeters: Int = 3000,
        @Query("type") type: String = "restaurant",
        @Query("language") language: String = "tr",
        @Query("key") apiKey: String,
    ): PlacesTextSearchResponse
}

data class PlacesTextSearchResponse(
    val results: List<PlaceResult> = emptyList(),
    val status: String = "",
)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val rating: Double? = null,
    val user_ratings_total: Int? = null,
    val formatted_address: String? = null,
    val vicinity: String? = null,
    val types: List<String> = emptyList(),
    val geometry: Geometry? = null,
)

data class Geometry(
    val location: LatLngDto,
)

data class LatLngDto(
    val lat: Double,
    val lng: Double,
)
