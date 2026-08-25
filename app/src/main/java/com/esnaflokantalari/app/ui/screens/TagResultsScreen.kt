package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.components.RestaurantCard

/** Bir yemek türü etiketine (ör. "Kebap") göre, şehir sınırı olmadan tüm eşleşen lokantalar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagResultsScreen(
    tag: String,
    restaurants: List<Restaurant>,
    favoriteIds: Set<String>,
    photos: Map<String, String>,
    bundledPhotoIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onRestaurantClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        if (restaurants.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null)
                Text(
                    "$tag için henüz kayıtlı lokanta yok.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            return@Scaffold
        }

        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(restaurants, key = { it.id }) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        isFavorite = favoriteIds.contains(restaurant.id),
                        onClick = { onRestaurantClick(restaurant.id) },
                        onToggleFavorite = { onToggleFavorite(restaurant.id) },
                        localPhotoPath = photos[restaurant.id],
                        hasBundledPhoto = restaurant.id in bundledPhotoIds,
                    )
                }
            }
        }
    }
}
