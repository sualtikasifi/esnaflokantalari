package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.data.SampleData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCityClick: (String) -> Unit,
    onFavoritesClick: () -> Unit,
    onNearbyClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Esnaf Lokantaları") }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable { onNearbyClick() },
                    ) {
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.NearMe, contentDescription = null) },
                            headlineContent = { Text("Yakınımdaki Lokantalar") },
                            supportingContent = { Text("Konumuna göre en yakın esnaf lokantaları") },
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable { onFavoritesClick() },
                    ) {
                        ListItem(
                            headlineContent = { Text("Favorilerim") },
                            supportingContent = { Text("Kaydettiğin lokantalar") },
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
                items(SampleData.cities) { city ->
                    Card(modifier = Modifier.clickable { onCityClick(city.name) }) {
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
                            headlineContent = { Text(city.name) },
                            supportingContent = { Text("En iyi esnaf lokantalarını keşfet") },
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}
