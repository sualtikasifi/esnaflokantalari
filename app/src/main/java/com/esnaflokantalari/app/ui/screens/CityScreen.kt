package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.ui.LoadState
import com.esnaflokantalari.app.ui.components.RestaurantCard
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.TerracottaContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityScreen(
    cityName: String,
    state: LoadState,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit,
    onRestaurantClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cityName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            is LoadState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is LoadState.Failed -> Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                Text(state.message)
            }

            is LoadState.Loaded -> {
                if (state.restaurants.isEmpty()) {
                    Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                        Text("Bu şehir için henüz lokanta eklenmedi. Yakında burada olacak!")
                    }
                    return@Scaffold
                }

                var selectedCategory by remember(cityName) { mutableStateOf("Tümü") }
                val categories = remember(state.restaurants) {
                    listOf("Tümü") + state.restaurants.map { it.category }.distinct()
                }
                val filtered = if (selectedCategory == "Tümü") {
                    state.restaurants
                } else {
                    state.restaurants.filter { it.category == selectedCategory }
                }

                Column(modifier = Modifier.padding(padding)) {
                    if (state.isSampleData) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Örnek veri gösteriliyor.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(categories) { category ->
                            val selected = category == selectedCategory
                            Text(
                                category,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) TerracottaContainer else ChipBackground)
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filtered) { restaurant ->
                            RestaurantCard(
                                restaurant = restaurant,
                                isFavorite = favoriteIds.contains(restaurant.id),
                                onClick = { onRestaurantClick(restaurant.id) },
                                onToggleFavorite = { onToggleFavorite(restaurant.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
