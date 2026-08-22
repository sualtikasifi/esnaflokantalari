package com.esnaflokantalari.app.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Places API (New).
 * https://developers.google.com/maps/documentation/places/web-service/text-search
 * https://developers.google.com/maps/documentation/places/web-service/nearby-search
 */
interface PlacesApiService {

    @Headers(
        "Content-Type: application/json",
        "X-Goog-FieldMask: places.id,places.displayName,places.rating,places.userRatingCount,places.formattedAddress,places.location,places.primaryTypeDisplayName",
    )
    @POST("v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Body request: SearchTextRequest,
    ): PlacesSearchResponse

    @Headers(
        "Content-Type: application/json",
        "X-Goog-FieldMask: places.id,places.displayName,places.rating,places.userRatingCount,places.formattedAddress,places.location,places.primaryTypeDisplayName",
    )
    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Body request: SearchNearbyRequest,
    ): PlacesSearchResponse
}

data class SearchTextRequest(
    val textQuery: String,
    val languageCode: String = "tr",
    val includedType: String = "restaurant",
)

data class SearchNearbyRequest(
    val locationRestriction: LocationRestriction,
    val includedTypes: List<String> = listOf("restaurant"),
    val languageCode: String = "tr",
    val maxResultCount: Int = 20,
)

data class LocationRestriction(val circle: Circle)
data class Circle(val center: CenterLatLng, val radius: Double)
data class CenterLatLng(val latitude: Double, val longitude: Double)

data class PlacesSearchResponse(
    val places: List<Place> = emptyList(),
)

data class Place(
    val id: String,
    val displayName: LocalizedText? = null,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val formattedAddress: String? = null,
    val location: LatLngDto? = null,
    val primaryTypeDisplayName: LocalizedText? = null,
)

data class LocalizedText(val text: String, val languageCode: String? = null)

data class LatLngDto(val latitude: Double, val longitude: Double)
