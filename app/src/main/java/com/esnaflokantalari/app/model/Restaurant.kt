package com.esnaflokantalari.app.model

data class Restaurant(
    val id: String,
    val name: String,
    val city: String,
    val category: String,
    val rating: Double,
    val reviewCount: Int,
    val address: String,
    val mapsUrl: String,
    val dailySpecial: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceMeters: Double? = null,
) {
    /** Gerçek fotoğrafımız olmadığında gösterilecek, id'ye göre sabit bir kapak görseli. */
    val coverPhotoUrl: String
        get() = "https://picsum.photos/seed/$id/600/450"
}

data class City(
    val name: String,
    val slug: String,
)
