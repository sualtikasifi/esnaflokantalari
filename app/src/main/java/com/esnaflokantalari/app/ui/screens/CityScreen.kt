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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.ui.components.RestaurantCard
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.Terracotta
import com.esnaflokantalari.app.ui.theme.TerracottaContainer

private const val ALL_DISTRICTS = "Tümü"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityScreen(
    cityName: String,
    city: City?,
    favoriteIds: Set<String>,
    suggestionCount: Int,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onSuggestClick: () -> Unit,
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
                actions = {
                    IconButton(onClick = onSuggestClick) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Lokanta öner", tint = Terracotta)
                    }
                },
            )
        },
    ) { padding ->
        val restaurants = city?.restaurants.orEmpty()

        if (restaurants.isEmpty()) {
            EmptyCityState(
                cityName = cityName,
                tagline = city?.tagline.orEmpty(),
                suggestionCount = suggestionCount,
                onSuggestClick = onSuggestClick,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }

        var selectedTag by remember(cityName) { mutableStateOf(ALL_TAG) }
        var selectedDistrict by remember(cityName) { mutableStateOf(ALL_DISTRICTS) }
        val tags = remember(restaurants) {
            listOf(ALL_TAG) + restaurants.flatMap { it.displayTags }.distinct()
        }
        val districts = remember(restaurants) {
            restaurants.mapNotNull { it.district }.distinct().sorted()
        }
        val filtered = remember(restaurants, selectedTag, selectedDistrict) {
            restaurants
                .filter { selectedTag == ALL_TAG || selectedTag in it.displayTags }
                .filter { selectedDistrict == ALL_DISTRICTS || it.district == selectedDistrict }
        }

        Column(modifier = Modifier.padding(padding)) {
            city?.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                Text(
                    tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            if (districts.isNotEmpty()) {
                DistrictDropdown(
                    districts = districts,
                    selected = selectedDistrict,
                    onSelect = { selectedDistrict = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (tags.size > 2) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    items(tags) { tag ->
                        val selected = tag == selectedTag
                        Text(
                            tag,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) TerracottaContainer else ChipBackground)
                                .clickable { selectedTag = tag }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    "$selectedDistrict ilçesinde bu etikette kayıtlı lokanta yok.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.id }) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        isFavorite = favoriteIds.contains(restaurant.id),
                        onClick = { onRestaurantClick(restaurant.id) },
                        onToggleFavorite = { onToggleFavorite(restaurant.id) },
                    )
                }

                item {
                    SuggestPrompt(cityName = cityName, suggestionCount = suggestionCount, onClick = onSuggestClick)
                }
            }
        }
    }
}

private const val ALL_TAG = "Tümü"

/**
 * İlçe seçimi: alfabetik sıralı, aşağı doğru açılan kaydırmalı bir liste.
 * Şehre ilk girişte "Tümü" seçili gelir, tüm lokantalar gösterilir.
 */
@Composable
private fun DistrictDropdown(
    districts: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ChipBackground)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (selected == ALL_DISTRICTS) "İlçe: Tümü" else "İlçe: $selected",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 4.dp),
            // ~5 satır görünür yükseklik; gerisi içeride kaydırılır. Menü tüm
            // ilçe sayısı kadar uzamayınca aşağıda her zaman yer buluyor ve
            // butonun üstüne taşmıyor.
            modifier = Modifier.heightIn(max = 260.dp),
        ) {
            DropdownMenuItem(
                text = { Text(ALL_DISTRICTS) },
                onClick = { onSelect(ALL_DISTRICTS); expanded = false },
            )
            districts.forEach { district ->
                DropdownMenuItem(
                    text = { Text(district) },
                    onClick = { onSelect(district); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun EmptyCityState(
    cityName: String,
    tagline: String,
    suggestionCount: Int,
    onSuggestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(TerracottaContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Restaurant,
                contentDescription = null,
                tint = Terracotta,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            "$cityName henüz boş",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            if (tagline.isNotBlank()) {
                "$tagline… ama listemizde henüz kayıtlı lokanta yok. İlk öneriyi sen yap!"
            } else {
                "Bu şehir için henüz kayıtlı lokanta yok. İlk öneriyi sen yap!"
            },
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        Button(onClick = onSuggestClick, shape = RoundedCornerShape(50)) {
            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Lokanta Öner")
        }
        if (suggestionCount > 0) {
            Text(
                "$suggestionCount önerin kayıtlı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SuggestPrompt(cityName: String, suggestionCount: Int, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$cityName'da bildiğin bir yer var mı?",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                if (suggestionCount > 0) "$suggestionCount önerin kayıtlı. Yenisini ekleyebilirsin."
                else "Listede olmayan esnaf lokantalarını bize öner.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            OutlinedButton(onClick = onClick, shape = RoundedCornerShape(50)) {
                Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Lokanta Öner")
            }
        }
    }
}
