package com.esnaflokantalari.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esnaflokantalari.app.data.FavoritesStore
import com.esnaflokantalari.app.data.LastCityStore
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
import kotlinx.coroutines.flow.combine
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
        /** GPS ile şimdi değil, önceki bir ziyarette doğrulanan ile göre. */
        val isCached: Boolean = false,
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
    private val lastCityStore = LastCityStore(application)

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

    /** Uygulamaya gömülü fotoğrafı olan lokantaların kimlikleri. */
    private val _bundledPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val bundledPhotoIds: StateFlow<Set<String>> = _bundledPhotoIds.asStateFlow()

    private val _dataUpdatedAt = MutableStateFlow("")
    val dataUpdatedAt: StateFlow<String> = _dataUpdatedAt.asStateFlow()

    private val _nearby = MutableStateFlow<NearbyState>(NearbyState.NeedsPermission)
    val nearby: StateFlow<NearbyState> = _nearby.asStateFlow()

    private val _surprise = MutableStateFlow<SurpriseEvent?>(null)
    val surprise: StateFlow<SurpriseEvent?> = _surprise.asStateFlow()

    /**
     * Fotoğrafı olmayan lokantalar, il->ad sırasıyla. Bir fotoğraf kaydedilince
     * (yerel ya da gömülü) bu listeden otomatik düşer — kuyruk ekranı bu sayede
     * kendiliğinden bir sonraki lokantaya geçer.
     */
    val missingPhotoRestaurants: StateFlow<List<Restaurant>> =
        combine(cities, photos, bundledPhotoIds) { cityList, photoMap, bundled ->
            cityList.flatMap { it.restaurants }
                .filter { it.id !in photoMap && it.id !in bundled }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val dataset = repository.dataset()
            _cities.value = dataset.cities
            _dataUpdatedAt.value = dataset.updatedAt
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

    /** Kırpma ekranından gelen görseli kaydeder. */
    fun savePhotoBitmap(restaurantId: String, bitmap: android.graphics.Bitmap, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val path = photoStore.saveBitmap(restaurantId, bitmap)
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

    /**
     * Ekrana ilk girişte çağrılır. Daha önce doğrulanmış bir il varsa GPS
     * beklemeden hemen o ile ait önerileri gösterir — konum her açılışta
     * yeniden aranmaz, kullanıcı "Yenile"ye basana kadar aynı ile devam edilir.
     */
    fun loadNearby() {
        viewModelScope.launch {
            val cachedCity = lastCityStore.get()?.let { repository.city(it) }
            if (cachedCity != null && cachedCity.restaurants.isNotEmpty()) {
                _nearby.value = NearbyState.Ready(
                    restaurants = cachedCity.restaurants,
                    cityName = cachedCity.name,
                    refreshing = false,
                    isCached = true,
                )
                return@launch
            }
            _nearby.value = NearbyState.Locating
            fetchLiveNearby(fallback = null)
        }
    }

    /** "Yenile" butonu: her zaman gerçek bir GPS okuması yapar, il önbelleğini günceller. */
    fun refreshNearby() {
        viewModelScope.launch {
            val previous = _nearby.value as? NearbyState.Ready
            _nearby.value = previous?.copy(refreshing = true) ?: NearbyState.Locating
            fetchLiveNearby(fallback = previous)
        }
    }

    /**
     * GPS'ten canlı konum okur. Başarısız olursa, elde zaten gösterilen bir
     * liste varsa (ekran boşalmasın diye) onu korur; yoksa hata durumuna geçer.
     */
    private suspend fun fetchLiveNearby(fallback: NearbyState.Ready?) {
        when (val result = LocationHelper.currentLocation(getApplication())) {
            is LocationResult.PermissionMissing ->
                _nearby.value = fallback?.copy(refreshing = false) ?: NearbyState.NeedsPermission

            is LocationResult.ServiceDisabled ->
                _nearby.value = fallback?.copy(refreshing = false) ?: NearbyState.Failed(
                    "Cihazının konum servisi kapalı. Ayarlardan konumu açıp tekrar dene.",
                )

            is LocationResult.Unavailable ->
                _nearby.value = fallback?.copy(refreshing = false) ?: NearbyState.Failed(
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
                    fallback?.copy(refreshing = false) ?: NearbyState.Failed(
                        "Yakınında kayıtlı bir esnaf lokantası bulunamadı. " +
                            "Bildiğin bir yer varsa şehir sayfasından öner!",
                    )
                } else {
                    cityName?.let { lastCityStore.save(it) }
                    NearbyState.Ready(results, cityName, refreshing = false, isCached = false)
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

    /**
     * Daha önce doğrulanmış bir il varsa GPS beklemeden o ilden rastgele bir
     * lokanta seçer — "Yakınımda" sayfasında en son doğrulanan il neredeyse,
     * zar da aynı ilden önermeye devam eder. Hiç il bilinmiyorsa konumu alıp
     * ili belirler ve bir sonraki seferler için hatırlar.
     */
    fun surpriseMe() {
        viewModelScope.launch {
            val cachedCity = lastCityStore.get()?.let { repository.city(it) }
            if (cachedCity != null && cachedCity.restaurants.isNotEmpty()) {
                val picked = cachedCity.restaurants.random()
                _surprise.value = SurpriseEvent.Picked(picked.id, cachedCity.name)
                return@launch
            }

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

            lastCityStore.save(city.name)
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
