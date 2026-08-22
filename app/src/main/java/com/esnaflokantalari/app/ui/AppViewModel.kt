package com.esnaflokantalari.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esnaflokantalari.app.data.FavoritesStore
import com.esnaflokantalari.app.data.RestaurantRepository
import com.esnaflokantalari.app.data.RestaurantResult
import com.esnaflokantalari.app.model.Restaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LoadState {
    data object Loading : LoadState()
    data class Loaded(val restaurants: List<Restaurant>, val isSampleData: Boolean) : LoadState()
    data class Failed(val message: String) : LoadState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesStore = FavoritesStore(application)

    val favoriteIds: StateFlow<Set<String>> = favoritesStore.favoriteIds
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptySet())

    private val cityStates = mutableMapOf<String, MutableStateFlow<LoadState>>()
    private val nearbyState = MutableStateFlow<LoadState>(LoadState.Loading)

    fun toggleFavorite(restaurantId: String) {
        viewModelScope.launch { favoritesStore.toggle(restaurantId) }
    }

    fun cityState(cityName: String): StateFlow<LoadState> {
        return cityStates.getOrPut(cityName) {
            val flow = MutableStateFlow<LoadState>(LoadState.Loading)
            viewModelScope.launch {
                when (val result = RestaurantRepository.restaurantsForCity(cityName)) {
                    is RestaurantResult.Success -> flow.value = LoadState.Loaded(result.restaurants, result.isSampleData)
                    is RestaurantResult.Error -> flow.value = LoadState.Failed(result.message)
                }
            }
            flow
        }
    }

    fun nearbyState(): StateFlow<LoadState> = nearbyState

    fun loadNearby(latitude: Double, longitude: Double) {
        nearbyState.value = LoadState.Loading
        viewModelScope.launch {
            when (val result = RestaurantRepository.nearbyRestaurants(latitude, longitude)) {
                is RestaurantResult.Success -> nearbyState.value = LoadState.Loaded(result.restaurants, result.isSampleData)
                is RestaurantResult.Error -> nearbyState.value = LoadState.Failed(result.message)
            }
        }
    }

    fun findRestaurant(restaurantId: String): Restaurant? {
        val fromCities = cityStates.values.mapNotNull { (it.value as? LoadState.Loaded)?.restaurants }.flatten()
        val fromNearby = (nearbyState.value as? LoadState.Loaded)?.restaurants ?: emptyList()
        return (fromCities + fromNearby).firstOrNull { it.id == restaurantId }
            ?: com.esnaflokantalari.app.data.SampleData.restaurants.firstOrNull { it.id == restaurantId }
    }
}
