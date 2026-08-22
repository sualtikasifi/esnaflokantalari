package com.esnaflokantalari.app.data

import android.content.Context
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tüm lokanta verisi uygulamanın içine gömülü `assets/restaurants.json`
 * dosyasından okunur. Hiçbir ağ isteği yapılmaz, API anahtarı gerekmez,
 * uygulama internetsiz de tam çalışır.
 *
 * Veriyi güncellemek için: tools/data/restaurants.csv dosyasını düzenle,
 * `python3 tools/build_dataset.py` çalıştır, uygulamayı yeniden derle.
 */
class RestaurantRepository(private val context: Context) {

    @Volatile
    private var cache: Dataset? = null

    data class Dataset(
        val updatedAt: String,
        val cities: List<City>,
    ) {
        val allRestaurants: List<Restaurant> = cities.flatMap { it.restaurants }
    }

    suspend fun dataset(): Dataset {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            synchronized(this@RestaurantRepository) {
                cache ?: parse().also { cache = it }
            }
        }
    }

    suspend fun cities(): List<City> = dataset().cities

    suspend fun city(name: String): City? =
        dataset().cities.firstOrNull { it.name.equalsTr(name) }

    suspend fun restaurant(id: String): Restaurant? =
        dataset().allRestaurants.firstOrNull { it.id == id }

    /** Puanı en yüksek, yorumu en çok lokantalar — ana sayfa vitrini için. */
    suspend fun featured(limit: Int = 10): List<Restaurant> =
        dataset().allRestaurants
            .filter { it.hasRating }
            .sortedWith(
                compareByDescending<Restaurant> { it.rating ?: 0.0 }
                    .thenByDescending { it.reviewCount ?: 0 },
            )
            .take(limit)

    /** Verilen konuma en yakın lokantalar. Koordinatı olmayanlar elenir. */
    suspend fun nearby(latitude: Double, longitude: Double, limit: Int = 30): List<Restaurant> =
        dataset().allRestaurants
            .mapNotNull { restaurant ->
                val lat = restaurant.latitude ?: return@mapNotNull null
                val lng = restaurant.longitude ?: return@mapNotNull null
                restaurant.copy(distanceMeters = distanceInMeters(latitude, longitude, lat, lng))
            }
            .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
            .take(limit)

    suspend fun searchCities(query: String): List<City> {
        val cities = cities()
        if (query.isBlank()) return cities
        return cities.filter { it.name.containsTr(query) }
    }

    private fun parse(): Dataset {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val cityArray = root.optJSONArray("cities") ?: JSONArray()

        val cities = buildList {
            for (i in 0 until cityArray.length()) {
                val cityJson = cityArray.getJSONObject(i)
                val cityName = cityJson.getString("name")
                add(
                    City(
                        name = cityName,
                        slug = cityJson.optString("slug"),
                        plate = cityJson.optIntOrNull("plate"),
                        tagline = cityJson.optString("tagline"),
                        restaurants = cityJson.optJSONArray("restaurants").toRestaurants(cityName),
                    ),
                )
            }
        }

        return Dataset(updatedAt = root.optString("updatedAt"), cities = cities)
    }

    private fun JSONArray?.toRestaurants(cityName: String): List<Restaurant> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val item = getJSONObject(i)
                add(
                    Restaurant(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        city = cityName,
                        category = item.optString("category", "Lokanta"),
                        tags = item.optJSONArray("tags").toStringList(),
                        rating = item.optDoubleOrNull("rating"),
                        reviewCount = item.optIntOrNull("reviewCount"),
                        address = item.optString("address"),
                        phone = item.optStringOrNull("phone"),
                        priceLevel = item.optIntOrNull("priceLevel"),
                        latitude = item.optDoubleOrNull("latitude"),
                        longitude = item.optDoubleOrNull("longitude"),
                        mapsUrl = item.optStringOrNull("mapsUrl"),
                        photoUrl = item.optStringOrNull("photoUrl"),
                        note = item.optStringOrNull("note"),
                    ),
                )
            }
        }
    }

    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private companion object {
        const val ASSET_NAME = "restaurants.json"
    }
}

private val TURKISH = Locale("tr", "TR")

/** Türkçe'ye duyarlı küçük harfe çevirme — "İSTANBUL" → "istanbul". */
internal fun String.lowercaseTr(): String = lowercase(TURKISH)

internal fun String.equalsTr(other: String): Boolean = lowercaseTr() == other.lowercaseTr()

internal fun String.containsTr(other: String): Boolean = lowercaseTr().contains(other.lowercaseTr())

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (isNull(key)) null else optInt(key).takeIf { has(key) }

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}
