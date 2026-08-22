package com.esnaflokantalari.app.data

import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.model.Restaurant

/**
 * Geçici örnek veri. İleride Google Places API ile değiştirilecek.
 */
object SampleData {

    val cities = listOf(
        City("İstanbul", "istanbul"),
        City("Ankara", "ankara"),
        City("İzmir", "izmir"),
        City("Bursa", "bursa"),
        City("Gaziantep", "gaziantep"),
        City("Antalya", "antalya"),
        City("Konya", "konya"),
        City("Adana", "adana"),
    )

    val restaurants = listOf(
        Restaurant(
            id = "adana-kaya-kebap",
            name = "Kaya Kebap",
            city = "Adana",
            category = "Kebap Restoranı",
            rating = 4.9,
            reviewCount = 18823,
            address = "Seyhan, Adana",
            mapsUrl = "https://maps.google.com",
            dailySpecial = "Adana Kebap",
        ),
        Restaurant(
            id = "adana-istah-kebap",
            name = "İştah Kebap 1950",
            city = "Adana",
            category = "Lokanta",
            rating = 4.6,
            reviewCount = 9021,
            address = "Çukurova, Adana",
            mapsUrl = "https://maps.google.com",
            dailySpecial = "İçli Köfte",
        ),
        Restaurant(
            id = "adana-kazim-buffe",
            name = "Kazım Büfe",
            city = "Adana",
            category = "Lokanta",
            rating = 4.5,
            reviewCount = 3120,
            address = "Yüreğir, Adana",
            mapsUrl = "https://maps.google.com",
        ),
        Restaurant(
            id = "gaziantep-imam-cagdas",
            name = "İmam Çağdaş",
            city = "Gaziantep",
            category = "Baklavacı & Kebapçı",
            rating = 4.7,
            reviewCount = 15234,
            address = "Şahinbey, Gaziantep",
            mapsUrl = "https://maps.google.com",
            dailySpecial = "Beyran Çorbası",
        ),
        Restaurant(
            id = "istanbul-siirt-seref",
            name = "Siirt Şeref Büryan",
            city = "İstanbul",
            category = "Büryan Kebapçısı",
            rating = 4.6,
            reviewCount = 7452,
            address = "Fatih, İstanbul",
            mapsUrl = "https://maps.google.com",
        ),
    )

    fun restaurantsForCity(cityName: String): List<Restaurant> =
        restaurants.filter { it.city.equals(cityName, ignoreCase = true) }
}
