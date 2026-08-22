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
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onSearchClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Esnaf Lokantaları") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Şehir Ara")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp),
            ) {
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
