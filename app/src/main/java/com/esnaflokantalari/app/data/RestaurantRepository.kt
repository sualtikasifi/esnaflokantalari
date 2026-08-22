package com.esnaflokantalari.app.data

import com.esnaflokantalari.app.BuildConfig
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.network.NetworkModule
import com.esnaflokantalari.app.network.PlaceResult
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
            val response = NetworkModule.placesApi.textSearch(
                query = "en iyi esnaf lokantası $cityName",
                apiKey = BuildConfig.MAPS_API_KEY,
            )
            val restaurants = response.results.map { it.toRestaurant(cityName) }
            if (restaurants.isEmpty()) {
                RestaurantResult.Success(SampleData.restaurantsForCity(cityName), isSampleData = true)
            } else {
                RestaurantResult.Success(restaurants, isSampleData = false)
            }
        } catch (e: Exception) {
            RestaurantResult.Success(SampleData.restaurantsForCity(cityName), isSampleData = true)
        }
    }

    suspend fun nearbyRestaurants(latitude: Double, longitude: Double, radiusMeters: Int = 3000): RestaurantResult {
        if (!NetworkModule.hasApiKey) {
            return RestaurantResult.Error("Google Haritalar bağlantısı henüz kurulmadı. README'deki kurulum adımlarını takip et.")
        }

        return try {
            val response = NetworkModule.placesApi.nearbySearch(
                location = "$latitude,$longitude",
                radiusMeters = radiusMeters,
                apiKey = BuildConfig.MAPS_API_KEY,
            )
            val restaurants = response.results
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

    private fun PlaceResult.toRestaurant(city: String): Restaurant {
        val lat = geometry?.location?.lat
        val lng = geometry?.location?.lng
        return Restaurant(
            id = place_id,
            name = name,
            city = city,
            category = types.firstOrNull { it != "restaurant" && it != "food" && it != "point_of_interest" }
                ?.replace('_', ' ')
                ?.replaceFirstChar { it.uppercase() }
                ?: "Lokanta",
            rating = rating ?: 0.0,
            reviewCount = user_ratings_total ?: 0,
            address = formatted_address ?: vicinity ?: "",
            mapsUrl = "https://www.google.com/maps/place/?q=place_id:$place_id",
            latitude = lat,
            longitude = lng,
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
