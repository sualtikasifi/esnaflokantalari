package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.ui.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityScreen(
    cityName: String,
    state: LoadState,
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

                Column(modifier = Modifier.padding(padding)) {
                    if (state.isSampleData) {
                        Text(
                            "Örnek veri gösteriliyor (gerçek zamanlı bağlantı henüz kurulmadı)",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.restaurants) { restaurant ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRestaurantClick(restaurant.id) },
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(restaurant.name, fontWeight = FontWeight.Bold)
                                    Text(restaurant.category)
                                    androidx.compose.foundation.layout.Row {
                                        Icon(Icons.Filled.Star, contentDescription = null)
                                        Text(" ${restaurant.rating} · ${restaurant.reviewCount} yorum")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
