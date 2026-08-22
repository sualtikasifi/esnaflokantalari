package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.esnaflokantalari.app.model.Restaurant

@Composable
fun FavoritesScreen(
    favorites: List<Restaurant>,
    onBack: () -> Unit,
    onRestaurantClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorilerim") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        if (favorites.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Henüz favori lokantan yok. Bir lokantayı beğenip kalp ikonuna dokun.")
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(favorites) { restaurant ->
                Card(modifier = Modifier.clickable { onRestaurantClick(restaurant.id) }) {
                    ListItem(
                        headlineContent = { Text(restaurant.name) },
                        supportingContent = { Text("${restaurant.city} · ${restaurant.category}") },
                    )
                }
            }
        }
    }
}
