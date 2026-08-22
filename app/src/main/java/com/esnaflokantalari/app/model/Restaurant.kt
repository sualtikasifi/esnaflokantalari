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
)

data class City(
    val name: String,
    val slug: String,
)
