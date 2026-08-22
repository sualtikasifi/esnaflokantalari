package com.esnaflokantalari.app.data

import androidx.compose.runtime.mutableStateListOf

/**
 * Basit bellek-içi favori listesi. İleride kalıcı depolama (DataStore/Room) ile değiştirilecek.
 */
object FavoritesStore {
    val favoriteIds = mutableStateListOf<String>()

    fun toggle(restaurantId: String) {
        if (favoriteIds.contains(restaurantId)) {
            favoriteIds.remove(restaurantId)
        } else {
            favoriteIds.add(restaurantId)
        }
    }

    fun isFavorite(restaurantId: String): Boolean = favoriteIds.contains(restaurantId)
}
