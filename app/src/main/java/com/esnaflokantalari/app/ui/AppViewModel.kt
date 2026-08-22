package com.esnaflokantalari.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esnaflokantalari.app.data.FavoritesStore
import com.esnaflokantalari.app.data.RestaurantRepository
import com.esnaflokantalari.app.data.Suggestion
import com.esnaflokantalari.app.data.SuggestionsStore
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.model.Restaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface NearbyState {
    /** Konum izni henüz istenmedi ya da kullanıcı "daha sonra" dedi. */
    data object NeedsPermission : NearbyState
    data object Locating : NearbyState
    data class Ready(val restaurants: List<Restaurant>) : NearbyState
    data class Failed(val message: String) : NearbyState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RestaurantRepository(application)
    private val favoritesStore = FavoritesStore(application)
    private val suggestionsStore = SuggestionsStore(application)

    val favorites: StateFlow<List<Restaurant>> = favoritesStore.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoritesStore.favoriteIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val suggestions: StateFlow<List<Suggestion>> = suggestionsStore.suggestions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    private val _featured = MutableStateFlow<List<Restaurant>>(emptyList())
    val featured: StateFlow<List<Restaurant>> = _featured.asStateFlow()

    private val _dataUpdatedAt = MutableStateFlow("")
    val dataUpdatedAt: StateFlow<String> = _dataUpdatedAt.asStateFlow()

    private val _nearby = MutableStateFlow<NearbyState>(NearbyState.NeedsPermission)
    val nearby: StateFlow<NearbyState> = _nearby.asStateFlow()

    init {
        viewModelScope.launch {
            val dataset = repository.dataset()
            _cities.value = dataset.cities
            _dataUpdatedAt.value = dataset.updatedAt
            _featured.value = repository.featured()
        }
    }

    fun city(cityName: String): City? = _cities.value.firstOrNull { it.name == cityName }

    /**
     * Detay ekranı için lokantayı bulur. Önce gömülü veri, sonra favoriler
     * (aylık güncellemede listeden çıkmış bir favori de açılabilsin diye).
     */
    fun findRestaurant(restaurantId: String): Restaurant? =
        _cities.value.asSequence()
            .flatMap { it.restaurants.asSequence() }
            .firstOrNull { it.id == restaurantId }
            ?: favorites.value.firstOrNull { it.id == restaurantId }
            ?: (nearby.value as? NearbyState.Ready)?.restaurants?.firstOrNull { it.id == restaurantId }

    fun toggleFavorite(restaurant: Restaurant) {
        viewModelScope.launch { favoritesStore.toggle(restaurant) }
    }

    fun toggleFavoriteById(restaurantId: String) {
        findRestaurant(restaurantId)?.let { toggleFavorite(it) }
    }

    // --- Yakınımda ---

    fun onLocationPermissionDenied() {
        _nearby.value = NearbyState.NeedsPermission
    }

    fun onLocatingStarted() {
        _nearby.value = NearbyState.Locating
    }

    fun onLocationUnavailable() {
        _nearby.value = NearbyState.Failed(
            "Konumun alınamadı. Konum servisinin açık olduğundan emin olup tekrar dene.",
        )
    }

    fun loadNearby(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val results = repository.nearby(latitude, longitude)
            _nearby.value = if (results.isEmpty()) {
                NearbyState.Failed(
                    "Yakınında kayıtlı bir esnaf lokantası bulunamadı. " +
                        "Bildiğin bir yer varsa şehir sayfasından öner!",
                )
            } else {
                NearbyState.Ready(results)
            }
        }
    }

    // --- Öneriler ---

    fun addSuggestion(city: String, name: String, category: String, address: String, note: String) {
        viewModelScope.launch {
            suggestionsStore.add(
                Suggestion(
                    id = UUID.randomUUID().toString(),
                    city = city,
                    name = name.trim(),
                    category = category.trim().ifBlank { "Lokanta" },
                    address = address.trim(),
                    note = note.trim(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun removeSuggestion(id: String) {
        viewModelScope.launch { suggestionsStore.remove(id) }
    }

    fun markSuggestionSent(id: String) {
        viewModelScope.launch { suggestionsStore.markSent(id) }
    }
}
