package com.esnaflokantalari.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")
private val FAVORITE_IDS_KEY = stringSetPreferencesKey("favorite_restaurant_ids")

/**
 * Favori lokanta id'lerini cihazda kalıcı olarak saklar (uygulama kapatılıp
 * açılsa bile kaybolmaz). Jetpack DataStore kullanır.
 */
class FavoritesStore(private val context: Context) {

    val favoriteIds: Flow<Set<String>> = context.favoritesDataStore.data
        .map { preferences -> preferences[FAVORITE_IDS_KEY] ?: emptySet() }

    suspend fun toggle(restaurantId: String) {
        context.favoritesDataStore.edit { preferences ->
            val current = preferences[FAVORITE_IDS_KEY] ?: emptySet()
            preferences[FAVORITE_IDS_KEY] = if (current.contains(restaurantId)) {
                current - restaurantId
            } else {
                current + restaurantId
            }
        }
    }
}
