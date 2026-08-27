package com.esnaflokantalari.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.StarGold
import com.esnaflokantalari.app.ui.theme.Terracotta

/**
 * Lokantanın görseli. Elimizde gerçek bir fotoğraf varsa (CSV'deki foto_url)
 * onu gösterir; yoksa yemek türüne göre ikonlu, renkli bir kapak üretir.
 *
 * Bilinçli tercih: rastgele stok fotoğraf göstermiyoruz — kullanıcı gördüğü
 * fotoğrafın o mekana ait olduğunu sanmamalı.
 */
/**
 * Elimizde bu lokantaya ait gerçek bir fotoğraf var mı? Yoksa kapakta
 * kategori illüstrasyonu/ikonu gösteriliyor demektir — bu durumda arayüz
 * katmanları "Haritada gerçek fotoğrafları gör" gibi bir çağrı ekleyebilir.
 */
fun restaurantHasRealPhoto(
    restaurant: Restaurant,
    localPhotoPath: String? = null,
    hasBundledPhoto: Boolean = false,
): Boolean = localPhotoPath != null || hasBundledPhoto || !restaurant.photoUrl.isNullOrBlank()

/**
 * Kapak bir illüstrasyon/ikon olduğunda, kullanıcıyı gerçek fotoğraflar için
 * Google Haritalar'a yönlendiren küçük bir çağrı. Kartın üstüne bindirilir.
 */
@Composable
fun RealPhotoCta(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC241A15))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PhotoCamera,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            "Haritada gör",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

/**
 * Arka planlı ve çerçeveli "Haritada gör" butonu. Koyu temada dolgu rengi
 * kart yüzeyine yakın kaldığı için ayrıca ince bir kenarlık çizilir —
 * böylece havada duran bir yazı değil, gerçek bir buton gibi okunur.
 *
 * Genişliği çağıran belirler (`Modifier.weight(1f)` ya da `fillMaxWidth()`).
 */
@Composable
fun MapsLinkButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ChipBackground)
            .border(1.dp, Terracotta.copy(alpha = 0.45f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PhotoCamera,
            contentDescription = null,
            tint = Terracotta,
            modifier = Modifier.size(15.dp),
        )
        Text(
            "Haritada gör",
            color = Terracotta,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

/** Veriden türeyen altın rozet: 🏅 Efsane, 📍 İlçenin en iyisi. */
@Composable
fun AwardChip(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        color = Color(0xFF241A15),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(StarGold)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Rozeti olmayan kartlarda aynı yeri dolduran nötr kategori etiketi. */
@Composable
fun CategoryChip(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ChipBackground)
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/**
 * Kartın rozet alanı: varsa altın başarı rozeti, yoksa kategori etiketi.
 * Her kartta tam olarak bir tane bulunur — böylece kartların yüksekliği eşit kalır.
 */
@Composable
fun RestaurantChip(restaurant: Restaurant, modifier: Modifier = Modifier) {
    val badge = restaurant.badge
    if (badge != null) {
        AwardChip("${badge.first} ${badge.second}", modifier)
    } else {
        CategoryChip(restaurant.displayTags.first(), modifier)
    }
}

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
private val ILLUSTRATED_CATEGORIES = setOf(
    "corba", "pide", "kebap", "balik", "kahvalti", "manti", "doner", "lokanta",
)

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
