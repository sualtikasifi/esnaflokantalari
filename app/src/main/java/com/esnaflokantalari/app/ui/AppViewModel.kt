package com.esnaflokantalari.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esnaflokantalari.app.data.FavoritesStore
import com.esnaflokantalari.app.data.PhotoStore
import com.esnaflokantalari.app.data.RestaurantRepository
import com.esnaflokantalari.app.data.Suggestion
import com.esnaflokantalari.app.data.SuggestionsStore
import com.esnaflokantalari.app.location.LocationHelper
import com.esnaflokantalari.app.location.LocationResult
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.model.Restaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface NearbyState {
    data object NeedsPermission : NearbyState
    data object Locating : NearbyState
    data class Ready(
        val restaurants: List<Restaurant>,
        val cityName: String?,
        val refreshing: Boolean = false,
    ) : NearbyState
    data class Failed(val message: String, val canRetry: Boolean = true) : NearbyState
}

/** "Bugün ne yesem?" sonucunu ekrana taşıyan tek seferlik olay. */
sealed interface SurpriseEvent {
    data object Locating : SurpriseEvent
    data class Picked(val restaurantId: String, val cityName: String) : SurpriseEvent
    data class Failed(val message: String) : SurpriseEvent
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RestaurantRepository(application)
    private val favoritesStore = FavoritesStore(application)
    private val suggestionsStore = SuggestionsStore(application)
    private val photoStore = PhotoStore(application)

    val favorites: StateFlow<List<Restaurant>> = favoritesStore.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoritesStore.favoriteIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val suggestions: StateFlow<List<Suggestion>> = suggestionsStore.suggestions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** restaurantId -> cihazda saklanan fotoğrafın yolu */
    val photos: StateFlow<Map<String, String>> = photoStore.photos
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    private val _featured = MutableStateFlow<List<Restaurant>>(emptyList())
    val featured: StateFlow<List<Restaurant>> = _featured.asStateFlow()

    /** Uygulamaya gömülü fotoğrafı olan lokantaların kimlikleri. */
    private val _bundledPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val bundledPhotoIds: StateFlow<Set<String>> = _bundledPhotoIds.asStateFlow()

    private val _dataUpdatedAt = MutableStateFlow("")
    val dataUpdatedAt: StateFlow<String> = _dataUpdatedAt.asStateFlow()

    private val _nearby = MutableStateFlow<NearbyState>(NearbyState.NeedsPermission)
    val nearby: StateFlow<NearbyState> = _nearby.asStateFlow()

    private val _surprise = MutableStateFlow<SurpriseEvent?>(null)
    val surprise: StateFlow<SurpriseEvent?> = _surprise.asStateFlow()

    init {
        viewModelScope.launch {
            val dataset = repository.dataset()
            _cities.value = dataset.cities
            _dataUpdatedAt.value = dataset.updatedAt
            _featured.value = repository.featured()
            _bundledPhotoIds.value = repository.bundledPhotoIds()
        }
    }

    fun city(cityName: String): City? = _cities.value.firstOrNull { it.name == cityName }

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

    // --- Fotoğraflar ---

    fun savePhoto(restaurantId: String, source: Uri, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val path = photoStore.save(restaurantId, source)
            onResult(path != null)
        }
    }

    fun removePhoto(restaurantId: String) {
        viewModelScope.launch { photoStore.remove(restaurantId) }
    }

    /**
     * Cihazdaki tüm fotoğrafları zip'e toplar ve dosyayı geri verir.
     * Fotoğraflar sadece bu telefonda saklandığı için, herkese göstermek
     * istediğinde bu arşivi dışa aktarman gerekiyor.
     */
    fun exportPhotos(onResult: (File?) -> Unit) {
        viewModelScope.launch { onResult(photoStore.exportAll()) }
    }

    // --- Yakınımda ---

    fun loadNearby() {
        viewModelScope.launch {
            // Elimizde sonuç varsa listeyi ekranda tut, sadece "yenileniyor"
            // durumuna geç — böylece ekran boşalmıyor.
            val previous = _nearby.value as? NearbyState.Ready
            _nearby.value = previous?.copy(refreshing = true) ?: NearbyState.Locating
            when (val result = LocationHelper.currentLocation(getApplication())) {
                is LocationResult.PermissionMissing ->
                    _nearby.value = NearbyState.NeedsPermission

                is LocationResult.ServiceDisabled ->
                    _nearby.value = NearbyState.Failed(
                        "Cihazının konum servisi kapalı. Ayarlardan konumu açıp tekrar dene.",
                    )

                is LocationResult.Unavailable ->
                    _nearby.value = NearbyState.Failed(
                        "Konumun bulunamadı. Açık bir alanda tekrar denemeyi ya da " +
                            "haritalar uygulamasını bir kez açmayı deneyebilirsin.",
                    )

                is LocationResult.Success -> {
                    val results = repository.nearby(result.location.latitude, result.location.longitude)
                    val cityName = repository.nearestCity(
                        result.location.latitude,
                        result.location.longitude,
                    )?.name
                    _nearby.value = if (results.isEmpty()) {
                        NearbyState.Failed(
                            "Yakınında kayıtlı bir esnaf lokantası bulunamadı. " +
                                "Bildiğin bir yer varsa şehir sayfasından öner!",
                        )
                    } else {
                        NearbyState.Ready(results, cityName, refreshing = false)
                    }
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _nearby.value = NearbyState.Failed(
            "Konum izni verilmedi. Şehir listesinden gezinerek de lokantaları görebilirsin.",
            canRetry = true,
        )
    }

    // --- "Bugün ne yesem?" ---

    /** Konumu alıp kullanıcının bulunduğu ilden rastgele bir lokanta seçer. */
    fun surpriseMe() {
        viewModelScope.launch {
            _surprise.value = SurpriseEvent.Locating
            val result = LocationHelper.currentLocation(getApplication())

            val city = (result as? LocationResult.Success)?.let {
                repository.nearestCity(it.location.latitude, it.location.longitude)
            }

            if (city == null || city.restaurants.isEmpty()) {
                _surprise.value = SurpriseEvent.Failed(
                    when (result) {
                        is LocationResult.PermissionMissing ->
                            "Sana yakın bir yer önerebilmemiz için konum izni gerekiyor."
                        is LocationResult.ServiceDisabled ->
                            "Konum servisin kapalı. Açıp tekrar dener misin?"
                        else ->
                            "Konumun alınamadı. Şehir listesinden seçerek de keşfedebilirsin."
                    },
                )
                return@launch
            }

            val picked = city.restaurants.random()
            _surprise.value = SurpriseEvent.Picked(picked.id, city.name)
        }
    }

    fun clearSurprise() {
        _surprise.value = null
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
