package com.esnaflokantalari.app.model

data class City(
    val name: String,
    val slug: String,
    val plate: Int? = null,
    val tagline: String = "",
    val restaurants: List<Restaurant> = emptyList(),
) {
    val hasRestaurants: Boolean get() = restaurants.isNotEmpty()
}
