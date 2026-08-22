package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.components.RestaurantVisual
import com.esnaflokantalari.app.ui.dial
import com.esnaflokantalari.app.ui.formatCount
import com.esnaflokantalari.app.ui.formatDistance
import com.esnaflokantalari.app.ui.formatRating
import com.esnaflokantalari.app.ui.openInMaps
import com.esnaflokantalari.app.ui.shareRestaurant
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.StarGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    if (restaurant == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Lokanta") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Bu lokanta artık listede değil.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack, shape = RoundedCornerShape(50)) { Text("Geri Dön") }
            }
        }
        return
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restaurant.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { context.shareRestaurant(restaurant) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Paylaş")
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            RestaurantVisual(
                restaurant = restaurant,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                initialsSize = 48.sp,
            )

            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(restaurant.city, style = MaterialTheme.typography.bodyLarge)
                    restaurant.priceLabel?.let {
                        Text(
                            " · $it",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (restaurant.hasRating) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Puan",
                            tint = StarGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            buildString {
                                append(" ${restaurant.rating!!.formatRating()}")
                                restaurant.reviewCount?.takeIf { it > 0 }?.let {
                                    append(" · ${it.formatCount()} yorum")
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                restaurant.distanceMeters?.let { distance ->
                    Text(
                        "${distance.formatDistance()} uzaklıkta",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    restaurant.displayTags.take(3).forEach { tag ->
                        Text(
                            tag,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ChipBackground)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                restaurant.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Neden burası?", style = MaterialTheme.typography.titleMedium)
                    Text(note, modifier = Modifier.padding(top = 4.dp))
                }

                if (restaurant.address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Adres", style = MaterialTheme.typography.titleMedium)
                    Text(restaurant.address, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { context.openInMaps(restaurant) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Yol Tarifi Al")
                }

                restaurant.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { context.dial(phone) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Ara: $phone")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
