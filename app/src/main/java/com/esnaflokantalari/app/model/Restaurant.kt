package com.esnaflokantalari.app.model

import android.net.Uri

data class Restaurant(
    val id: String,
    val name: String,
    val city: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val address: String = "",
    val phone: String? = null,
    val priceLevel: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapsUrl: String? = null,
    val photoUrl: String? = null,
    val note: String? = null,
    val distanceMeters: Double? = null,
) {
    /** Puanı olmayan mekanlar için "-" göstermek yerine bu kontrol kullanılır. */
    val hasRating: Boolean get() = (rating ?: 0.0) > 0.0

    /** Kartlarda gösterilecek etiketler; hiç etiket yoksa kategoriye düşer. */
    val displayTags: List<String>
        get() = tags.ifEmpty { listOf(category) }

    /** ₺ / ₺₺ / ₺₺₺ gösterimi. */
    val priceLabel: String?
        get() = priceLevel?.takeIf { it in 1..4 }?.let { "₺".repeat(it) }

    /**
     * Haritada açmak için kullanılacak adres. Koordinat varsa onu, yoksa
     * ad + adres araması kullanır — böylece maps_url girilmemiş olsa da çalışır.
     */
    fun mapsQuery(): String = when {
        !mapsUrl.isNullOrBlank() -> mapsUrl
        latitude != null && longitude != null -> "geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(name)})"
        else -> "geo:0,0?q=${Uri.encode("$name $address")}"
    }
}
