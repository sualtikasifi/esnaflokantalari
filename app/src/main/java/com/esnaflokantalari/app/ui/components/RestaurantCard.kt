package com.esnaflokantalari.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.formatCount
import com.esnaflokantalari.app.ui.formatDistance
import com.esnaflokantalari.app.ui.formatRating
import com.esnaflokantalari.app.ui.openInMaps
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.StarGold
import com.esnaflokantalari.app.ui.theme.Terracotta

/**
 * Veri-öncelikli lokanta kartı. Görsel küçük bir rozete indirgenir; isim,
 * puan ve rozetler kartın asıl anlatısı olur — böylece illüstrasyonlu
 * kapaklar kartın yarısını kaplayıp "oyun" hissi vermez.
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp)),
                ) {
                    RestaurantVisual(
                        restaurant = restaurant,
                        modifier = Modifier.size(50.dp),
                        iconSize = 20.dp,
                        initialsSize = 14.sp,
                        localPhotoPath = localPhotoPath,
                        hasBundledPhoto = hasBundledPhoto,
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        restaurant.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        restaurant.locationText(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                if (onToggleFavorite != null) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                            tint = Terracotta,
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                color = ChipBackground,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (restaurant.hasRating) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Puan",
                        tint = StarGold,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        restaurant.rating!!.formatRating(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                    restaurant.reviewCount?.takeIf { it > 0 }?.let {
                        Text(
                            "${it.formatCount()} yorum",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                } else {
                    Text(
                        "Puan bilgisi yok",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                restaurant.priceLabel?.let { price ->
                    Text(
                        price,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                restaurant.badge?.let { (emoji, label) -> BadgeChip("$emoji $label") }
                restaurant.displayTags.firstOrNull()?.let { TagChip(it) }
            }

            if (!hasRealPhoto) {
                Text(
                    "Haritada gör →",
                    color = Terracotta,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { context.openInMaps(restaurant) },
                )
            }
        }
    }
}

private fun Restaurant.locationText(): String = when {
    distanceMeters != null -> "${distanceMeters.formatDistance()} uzaklıkta"
    district != null -> "$city · $district"
    else -> city
}

@Composable
private fun TagChip(label: String) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ChipBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
    )
}

@Composable
private fun BadgeChip(label: String) {
    Text(
        label,
        color = Color(0xFF241A15),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(StarGold)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
    )
}
