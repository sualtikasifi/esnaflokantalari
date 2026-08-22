package com.esnaflokantalari.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.StarGold
import com.esnaflokantalari.app.ui.theme.Terracotta

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = restaurant.coverPhotoUrl,
                    contentDescription = restaurant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold, modifier = Modifier.padding(end = 4.dp))
                    Text(
                        if (restaurant.rating > 0) "%.1f".format(restaurant.rating) else "-",
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f)),
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = Terracotta,
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(restaurant.name, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryChip(restaurant.category)
                }
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    val locationText = restaurant.distanceMeters?.let { "%.1f km uzaklıkta".format(it / 1000) }
                        ?: "${restaurant.reviewCount} yorum"
                    Text(locationText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ChipBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
    )
}
