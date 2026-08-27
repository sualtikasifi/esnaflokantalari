package com.esnaflokantalari.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.formatCount
import com.esnaflokantalari.app.ui.formatRating
import com.esnaflokantalari.app.ui.locationLabel
import com.esnaflokantalari.app.ui.openInMaps
import com.esnaflokantalari.app.ui.theme.StarGold
import com.esnaflokantalari.app.ui.theme.Terracotta

/**
 * Veri-öncelikli lokanta kartı. Görsel küçük bir rozete indirgenir; isim,
 * puan ve rozetler kartın asıl anlatısı olur — böylece illüstrasyonlu
 * kapaklar kartın yarısını kaplayıp "oyun" hissi vermez.
 *
 * Yerleşim, boşluk bırakmayacak şekilde sıkıdır: görselin sağındaki sütun
 * üç satırı (ad / konum · kategori / puan) taşır, altta tek sıra hâlinde
 * rozet ve "Haritada gör" butonu yer alır.
 */
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    localPhotoPath: String? = null,
    hasBundledPhoto: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasRealPhoto = restaurantHasRealPhoto(restaurant, localPhotoPath, hasBundledPhoto)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(15.dp)),
                ) {
                    RestaurantVisual(
                        restaurant = restaurant,
                        modifier = Modifier.size(58.dp),
                        iconSize = 24.dp,
                        initialsSize = 16.sp,
                        localPhotoPath = localPhotoPath,
                        hasBundledPhoto = hasBundledPhoto,
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 13.dp)
                        .weight(1f),
                ) {
                    Text(
                        restaurant.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        restaurant.contextLine(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        if (restaurant.hasRating) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Puan",
                                tint = StarGold,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                restaurant.rating!!.formatRating(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 5.dp),
                            )
                        }
                        Text(
                            restaurant.statsLine(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = if (restaurant.hasRating) 6.dp else 0.dp),
                        )
                    }
                }

                if (onToggleFavorite != null) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                            tint = Terracotta,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RestaurantChip(restaurant)
                if (!hasRealPhoto) {
                    MapsLinkButton(
                        onClick = { context.openInMaps(restaurant) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** "Kağıthane/İstanbul · Lokanta" — konum ve kategori tek satırda. */
private fun Restaurant.contextLine(): String {
    val place = locationLabel()
    val category = displayTags.firstOrNull()
    return if (category != null && badge != null) "$place · $category" else place
}

/** Puanın yanındaki ikincil bilgiler: yorum sayısı ve fiyat. */
private fun Restaurant.statsLine(): String = buildList {
    reviewCount?.takeIf { it > 0 }?.let { add("${it.formatCount()} yorum") }
    priceLabel?.let { add(it) }
    if (isEmpty() && !hasRating) add("Puan bilgisi yok")
}.joinToString(" · ")
