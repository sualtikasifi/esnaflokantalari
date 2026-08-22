package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.data.SampleData

@Composable
fun CityScreen(cityName: String, onBack: () -> Unit, onRestaurantClick: (String) -> Unit) {
    val restaurants = SampleData.restaurantsForCity(cityName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cityName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        if (restaurants.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                Text("Bu şehir için henüz lokanta eklenmedi. Yakında burada olacak!")
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(restaurants) { restaurant ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRestaurantClick(restaurant.id) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(restaurant.name, fontWeight = FontWeight.Bold)
                        Text(restaurant.category)
                        Column {
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
