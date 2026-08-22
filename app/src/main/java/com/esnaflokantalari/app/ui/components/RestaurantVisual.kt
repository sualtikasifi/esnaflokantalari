package com.esnaflokantalari.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.esnaflokantalari.app.model.Restaurant

/**
 * Lokantanın görseli. Elimizde gerçek bir fotoğraf varsa onu gösterir;
 * yoksa lokantanın adından türetilen renkli bir kapak üretir.
 *
 * Bilinçli tercih: rastgele stok fotoğraf göstermiyoruz — kullanıcı gördüğü
 * fotoğrafın o mekana ait olduğunu sanmamalı.
 */
@Composable
fun RestaurantVisual(
    restaurant: Restaurant,
    modifier: Modifier = Modifier,
    initialsSize: TextUnit = 34.sp,
) {
    val photo = restaurant.photoUrl
    if (!photo.isNullOrBlank()) {
        AsyncImage(
            model = photo,
            contentDescription = restaurant.name,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }

    val palette = coverPalette(restaurant.id.ifBlank { restaurant.name })
    Box(
        modifier = modifier.background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            restaurant.initials(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = initialsSize,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

private fun Restaurant.initials(): String =
    name.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

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
