package com.esnaflokantalari.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.esnaflokantalari.app.model.Restaurant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")
private val FAVORITES_KEY = stringSetPreferencesKey("favorite_restaurants")

/**
 * Favorileri lokantanın tamamıyla birlikte saklar (sadece kimliğiyle değil).
 * Böylece aylık veri güncellemesinde bir mekan listeden çıksa bile kullanıcının
 * favorisi kaybolmaz, ve kullanıcının kendi eklediği öneriler de favorilenebilir.
 */
class FavoritesStore(private val context: Context) {

    val favorites: Flow<List<Restaurant>> = context.favoritesDataStore.data
        .map { preferences ->
            (preferences[FAVORITES_KEY] ?: emptySet()).mapNotNull { it.toRestaurantOrNull() }
        }

    val favoriteIds: Flow<Set<String>> = favorites.map { list -> list.map { it.id }.toSet() }

    suspend fun toggle(restaurant: Restaurant) {
        context.favoritesDataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            val existing = current.firstOrNull { it.toRestaurantOrNull()?.id == restaurant.id }
            preferences[FAVORITES_KEY] = if (existing != null) {
                current - existing
            } else {
                current + restaurant.toJson()
            }
        }
    }
}

private fun Restaurant.toJson(): String = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("city", city)
    put("category", category)
    put("tags", JSONArray(tags))
    rating?.let { put("rating", it) }
    reviewCount?.let { put("reviewCount", it) }
    put("address", address)
    phone?.let { put("phone", it) }
    priceLevel?.let { put("priceLevel", it) }
    latitude?.let { put("latitude", it) }
    longitude?.let { put("longitude", it) }
    mapsUrl?.let { put("mapsUrl", it) }
    photoUrl?.let { put("photoUrl", it) }
    note?.let { put("note", it) }
}.toString()

private fun String.toRestaurantOrNull(): Restaurant? = runCatching {
    val json = JSONObject(this)
    val tagsArray = json.optJSONArray("tags")
    val tags = buildList {
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tagsArray.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }
    Restaurant(
        id = json.getString("id"),
        name = json.getString("name"),
        city = json.optString("city"),
        category = json.optString("category", "Lokanta"),
        tags = tags,
        rating = if (json.has("rating")) json.optDouble("rating").takeIf { !it.isNaN() } else null,
        reviewCount = if (json.has("reviewCount")) json.optInt("reviewCount") else null,
        address = json.optString("address"),
        phone = json.optString("phone").takeIf { it.isNotBlank() },
        priceLevel = if (json.has("priceLevel")) json.optInt("priceLevel") else null,
        latitude = if (json.has("latitude")) json.optDouble("latitude").takeIf { !it.isNaN() } else null,
        longitude = if (json.has("longitude")) json.optDouble("longitude").takeIf { !it.isNaN() } else null,
        mapsUrl = json.optString("mapsUrl").takeIf { it.isNotBlank() },
        photoUrl = json.optString("photoUrl").takeIf { it.isNotBlank() },
        note = json.optString("note").takeIf { it.isNotBlank() },
    )
}.getOrNull()
