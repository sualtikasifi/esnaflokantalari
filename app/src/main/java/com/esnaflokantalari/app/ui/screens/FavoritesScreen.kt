package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.components.RestaurantCard
import com.esnaflokantalari.app.ui.theme.CreamSurface
import com.esnaflokantalari.app.ui.theme.Terracotta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<Restaurant>,
    photos: Map<String, String>,
    onRestaurantClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onExploreClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Favorilerim") }) },
    ) { padding ->
        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(128.dp).clip(CircleShape).background(CreamSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(52.dp),
                    )
                }
                Text(
                    "Henüz favori lokantan yok",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    "Bir lokantayı beğenip kalp ikonuna dokun. Favorilerin cihazında saklanır.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                Button(onClick = onExploreClick, shape = RoundedCornerShape(50)) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Keşfetmeye Başla")
                }
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(favorites, key = { it.id }) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    isFavorite = true,
                    onClick = { onRestaurantClick(restaurant.id) },
                    onToggleFavorite = { onToggleFavorite(restaurant.id) },
                    localPhotoPath = photos[restaurant.id],
                )
            }
        }
    }
}
