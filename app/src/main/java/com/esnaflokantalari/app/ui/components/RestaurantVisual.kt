package com.esnaflokantalari.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.esnaflokantalari.app.model.Restaurant

/**
 * Lokantanın görseli. Elimizde gerçek bir fotoğraf varsa (CSV'deki foto_url)
 * onu gösterir; yoksa yemek türüne göre ikonlu, renkli bir kapak üretir.
 *
 * Bilinçli tercih: rastgele stok fotoğraf göstermiyoruz — kullanıcı gördüğü
 * fotoğrafın o mekana ait olduğunu sanmamalı.
 */
@Composable
fun RestaurantVisual(
    restaurant: Restaurant,
    modifier: Modifier = Modifier,
    initialsSize: TextUnit = 34.sp,
    iconSize: Dp = 40.dp,
    localPhotoPath: String? = null,
    hasBundledPhoto: Boolean = false,
) {
    // Öncelik sırası: cihazda çekilen fotoğraf -> uygulamaya gömülü fotoğraf
    // -> veri dosyasındaki foto_url -> üretilen renkli kapak.
    val model: Any? = when {
        localPhotoPath != null -> File(localPhotoPath)
        hasBundledPhoto -> "file:///android_asset/photos/${restaurant.id}.jpg"
        !restaurant.photoUrl.isNullOrBlank() -> restaurant.photoUrl
        else -> null
    }
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = restaurant.name,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }

    // Kaydırma sırasında her karede yeniden üretilmesin diye hatırlanır.
    val category = remember(restaurant.id) { categoryFor(restaurant) }

    if (category.key in ILLUSTRATED_CATEGORIES) {
        AsyncImage(
            model = "file:///android_asset/illustrations/${category.key}.png",
            contentDescription = restaurant.name,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(IllustrationCream),
        )
        return
    }

    val brush = remember(restaurant.id) { Brush.linearGradient(coverPalette(restaurant.id)) }
    val icon = category.icon
    val initials = remember(restaurant.id) { restaurant.initials() }

    Box(modifier = modifier.background(brush), contentAlignment = Alignment.Center) {
        // Arka planda soluk, büyük bir yemek ikonu — kapağa doku katar.
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(iconSize * 1.9f)
                .alpha(0.16f),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(iconSize),
            )
            Text(
                initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = initialsSize,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun Restaurant.initials(): String =
    name.split(' ')
        .filter { it.isNotBlank() && it.first().isLetter() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

/** Bir yemek kategorisi: hem ikon-fallback hem de gömülü illüstrasyon anahtarı taşır. */
private enum class FoodCategory(val key: String, val icon: ImageVector) {
    Kebap("kebap", Icons.Filled.LocalFireDepartment),
    Corba("corba", Icons.Filled.SoupKitchen),
    Pide("pide", Icons.Filled.BakeryDining),
    Balik("balik", Icons.Filled.SetMeal),
    Kahvalti("kahvalti", Icons.Filled.Egg),
    Manti("manti", Icons.Filled.RamenDining),
    Doner("doner", Icons.Filled.LunchDining),
    Lokanta("lokanta", Icons.Filled.RiceBowl),
    Genel("genel", Icons.Filled.LocalDining),
}

/** Bu kategoriler için elle/Gemini ile üretilmiş, assets/illustrations altına gömülü görsel var. */
private val ILLUSTRATED_CATEGORIES = setOf("corba", "pide")

private val IllustrationCream = Color(0xFFF4E9DE)

/** Etiket ve ada bakarak uygun yemek kategorisini seçer. */
private fun categoryFor(restaurant: Restaurant): FoodCategory {
    val haystack = (restaurant.displayTags + restaurant.name + restaurant.category)
        .joinToString(" ")
        .lowercase()

    return when {
        haystack.containsAny("kebap", "kebab", "ocakbaşı", "et", "kasap") -> FoodCategory.Kebap
        haystack.containsAny("çorba", "corba", "paça", "beyran", "işkembe") -> FoodCategory.Corba
        haystack.containsAny("lahmacun", "pide", "börek", "fırın") -> FoodCategory.Pide
        haystack.containsAny("balık", "balik", "deniz") -> FoodCategory.Balik
        haystack.containsAny("kahvaltı", "kahvalti", "serpme") -> FoodCategory.Kahvalti
        haystack.containsAny("mantı", "manti", "makarna") -> FoodCategory.Manti
        haystack.containsAny("döner", "doner", "dürüm", "tantuni", "köfte", "burger") -> FoodCategory.Doner
        haystack.containsAny("sulu yemek", "ev yemek", "lokanta", "sofra", "pilav") -> FoodCategory.Lokanta
        else -> FoodCategory.Genel
    }
}

private fun String.containsAny(vararg needles: String): Boolean =
    needles.any { it in this }

/** Ada göre sabit bir renk çifti — aynı lokanta her zaman aynı rengi alır. */
private fun coverPalette(seed: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFB6392C), Color(0xFF7E2118)),
        listOf(Color(0xFFC97B29), Color(0xFF8A4A12)),
        listOf(Color(0xFF6E7F4B), Color(0xFF3F4E27)),
        listOf(Color(0xFF8C5A3C), Color(0xFF5A3623)),
        listOf(Color(0xFF3F6E78), Color(0xFF21454C)),
        listOf(Color(0xFF9A4B62), Color(0xFF5E2B3A)),
    )
    val index = (seed.sumOf { it.code } % palettes.size + palettes.size) % palettes.size
    return palettes[index]
}
