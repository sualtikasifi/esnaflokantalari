package com.esnaflokantalari.app.data

import com.esnaflokantalari.app.BuildConfig
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.network.Circle
import com.esnaflokantalari.app.network.CenterLatLng
import com.esnaflokantalari.app.network.LocationRestriction
import com.esnaflokantalari.app.network.NetworkModule
import com.esnaflokantalari.app.network.Place
import com.esnaflokantalari.app.network.SearchNearbyRequest
import com.esnaflokantalari.app.network.SearchTextRequest
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class RestaurantResult {
    data class Success(val restaurants: List<Restaurant>, val isSampleData: Boolean) : RestaurantResult()
    data class Error(val message: String) : RestaurantResult()
}

/**
 * Restoran verisi kaynağı. API anahtarı yoksa veya istek başarısız olursa
 * uygulamanın örnek (mock) verisine geri döner, böylece uygulama her zaman
 * bir şeyler gösterebilir.
 */
object RestaurantRepository {

    suspend fun restaurantsForCity(cityName: String): RestaurantResult {
        if (!NetworkModule.hasApiKey) {
            return RestaurantResult.Success(SampleData.restaurantsForCity(cityName), isSampleData = true)
        }

        return try {
            val response = NetworkModule.placesApi.searchText(
                apiKey = BuildConfig.MAPS_API_KEY,
                request = SearchTextRequest(textQuery = "en iyi esnaf lokantası $cityName"),
            )
            val restaurants = response.places.map { it.toRestaurant(cityName) }
            if (restaurants.isEmpty()) {
                RestaurantResult.Success(SampleData.restaurantsForCity(cityName), isSampleData = true)
            } else {
                RestaurantResult.Success(restaurants, isSampleData = false)
            }
        } catch (e: Exception) {
            RestaurantResult.Success(SampleData.restaurantsForCity(cityName), isSampleData = true)
        }
    }

    suspend fun nearbyRestaurants(latitude: Double, longitude: Double, radiusMeters: Double = 3000.0): RestaurantResult {
        if (!NetworkModule.hasApiKey) {
            return RestaurantResult.Error("Google Haritalar bağlantısı henüz kurulmadı. README'deki kurulum adımlarını takip et.")
        }

        return try {
            val response = NetworkModule.placesApi.searchNearby(
                apiKey = BuildConfig.MAPS_API_KEY,
                request = SearchNearbyRequest(
                    locationRestriction = LocationRestriction(
                        circle = Circle(center = CenterLatLng(latitude, longitude), radius = radiusMeters),
                    ),
                ),
            )
            val restaurants = response.places
                .map { it.toRestaurant(city = "") }
                .map { restaurant ->
                    val distance = restaurant.latitude?.let { lat ->
                        restaurant.longitude?.let { lng ->
                            distanceInMeters(latitude, longitude, lat, lng)
                        }
                    }
                    restaurant.copy(distanceMeters = distance)
                }
                .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }

            RestaurantResult.Success(restaurants, isSampleData = false)
        } catch (e: Exception) {
            RestaurantResult.Error("Yakındaki lokantalar alınamadı: ${e.message}")
        }
    }

    private fun Place.toRestaurant(city: String): Restaurant {
        return Restaurant(
            id = id,
            name = displayName?.text ?: "İsimsiz Lokanta",
            city = city,
            category = primaryTypeDisplayName?.text ?: "Lokanta",
            rating = rating ?: 0.0,
            reviewCount = userRatingCount ?: 0,
            address = formattedAddress ?: "",
            mapsUrl = "https://www.google.com/maps/place/?q=place_id:$id",
            latitude = location?.latitude,
            longitude = location?.longitude,
        )
    }

    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
