package com.esnaflokantalari.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.esnaflokantalari.app.model.Restaurant
import java.text.NumberFormat
import java.util.Locale

private val TURKISH = Locale("tr", "TR")
private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(TURKISH)

/** 18823 → "18.823" */
fun Int.formatCount(): String = numberFormat.format(this)

/** 4.85 → "4.9" (Türkçe'de "4,9") */
fun Double.formatRating(): String = String.format(TURKISH, "%.1f", this)

/** 2350.0 → "2,4 km", 640.0 → "640 m" */
fun Double.formatDistance(): String =
    if (this < 1000) "${this.toInt()} m" else String.format(TURKISH, "%.1f km", this / 1000)

/** Kartlarda gösterilecek konum metni: uzaklık > ilçe/il > il. */
fun Restaurant.locationLabel(): String = when {
    distanceMeters != null -> "${distanceMeters!!.formatDistance()} uzaklıkta"
    district != null -> "$district/$city"
    else -> city
}

/**
 * Lokantayı harita uygulamasında açar. Harita uygulaması yoksa çökmek yerine
 * kullanıcıya bilgi verir.
 */
fun Context.openInMaps(restaurant: Restaurant) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(restaurant.mapsQuery()))
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val fallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("${restaurant.name} ${restaurant.address}")),
        )
        try {
            startActivity(fallback)
        } catch (e2: ActivityNotFoundException) {
            Toast.makeText(this, "Harita uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Telefon uygulamasını numarayla açar (aramayı kullanıcı başlatır). */
fun Context.dial(phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "Telefon uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
    }
}

/** Lokantayı WhatsApp/mesaj vb. ile paylaşır. */
fun Context.shareRestaurant(restaurant: Restaurant) {
    val text = buildString {
        append(restaurant.name)
        if (restaurant.city.isNotBlank()) append(" — ${restaurant.city}")
        if (restaurant.address.isNotBlank()) append("\n${restaurant.address}")
        restaurant.rating?.let { append("\n⭐ ${it.formatRating()}") }
        append("\n\nGurme uygulamasından paylaşıldı.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, restaurant.name)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(Intent.createChooser(intent, "Paylaş"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "Paylaşılacak uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
    }
}
