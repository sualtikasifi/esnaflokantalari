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
    /** Kendi ilçesindeki en yüksek puanlı lokanta mı? RestaurantRepository hesaplar. */
    val isBestInDistrict: Boolean = false,
) {
    /** Puanı olmayan mekanlar için "-" göstermek yerine bu kontrol kullanılır. */
    val hasRating: Boolean get() = (rating ?: 0.0) > 0.0

    /** Çok yüksek puan + çok sayıda yorum: tanınmış, kanıtlanmış bir mekan. */
    val isLegendary: Boolean get() = (rating ?: 0.0) >= 4.8 && (reviewCount ?: 0) >= 1000

    /**
     * Kartta gösterilecek tek rozet, önceliğe göre seçilir.
     * (emoji, etiket) — hiçbiri uymuyorsa null.
     */
    val badge: Pair<String, String>?
        get() = when {
            isLegendary -> "🏅" to "Efsane"
            isBestInDistrict -> "📍" to "İlçenin en iyisi"
            else -> null
        }

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

    /**
     * İlçe adı, adresin sonundaki "İlçe/İl" kalıbından çıkarılır (Google
     * Haritalar adresleri neredeyse hep bu şekilde bitiyor, ör. "Beykoz/İstanbul").
     * Ayrı bir ilçe alanı toplamadığımız için şimdilik en pratik kaynak bu.
     */
    val district: String?
        get() = DISTRICT_SUFFIX.find(address.trim())?.groupValues?.get(1)

    private companion object {
        val DISTRICT_SUFFIX = Regex("([\\p{L}]+)/[\\p{L}]+\\s*$")
    }
}
