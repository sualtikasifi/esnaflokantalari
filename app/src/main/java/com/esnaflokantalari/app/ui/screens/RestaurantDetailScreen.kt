package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.data.FavoritesStore
import com.esnaflokantalari.app.data.SampleData

@Composable
fun RestaurantDetailScreen(restaurantId: String, onBack: () -> Unit) {
    val restaurant = SampleData.restaurants.firstOrNull { it.id == restaurantId } ?: return
    val isFavorite = FavoritesStore.isFavorite(restaurantId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restaurant.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { FavoritesStore.toggle(restaurantId) }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favori",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(restaurant.category, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Icon(Icons.Filled.Star, contentDescription = null)
                Text(" ${restaurant.rating} · ${restaurant.reviewCount} yorum")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Adres", fontWeight = FontWeight.Bold)
            Text(restaurant.address)
            restaurant.dailySpecial?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bugünün Önerisi", fontWeight = FontWeight.Bold)
                Text(it)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Yol Tarifi Al")
            }
        }
    }
}
