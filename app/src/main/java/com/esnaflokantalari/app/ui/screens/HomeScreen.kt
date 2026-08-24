package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.R
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.Terracotta
import com.esnaflokantalari.app.ui.theme.TerracottaContainer
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cities: List<City>,
    dataUpdatedAt: String,
    photoCount: Int,
    photoMissingCount: Int,
    appVersion: String,
    onCityClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSurpriseMe: () -> Unit,
    onExportPhotos: () -> Unit,
    onOpenPhotoQueue: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Kaydırma sırasında her karede yeniden hesaplanmasın diye önceden ayrılır.
    // Şehirler assets/restaurants.json içinde zaten nüfusa göre büyükten
    // küçüğe sıralı geliyor (bkz. tools/build_dataset.py).
    val cityRows = remember(cities) { cities.chunked(2) }
    val filledCityCount = remember(cities) { cities.count { it.hasRestaurants } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                        )
                        Text("Gurme", modifier = Modifier.padding(start = 8.dp))
                    }
                },
                actions = {
                    // Menü her zaman görünür: gizli bir menü bulunamaz.
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                            },
                            text = {
                                Text(
                                    if (photoMissingCount > 0) {
                                        "Fotoğraf ekle ($photoMissingCount eksik)"
                                    } else {
                                        "Fotoğraf ekle"
                                    },
                                )
                            },
                            enabled = photoMissingCount > 0,
                            onClick = {
                                menuOpen = false
                                onOpenPhotoQueue()
                            },
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            },
                            text = {
                                Text(
                                    if (photoCount > 0) {
                                        "Fotoğrafları dışa aktar ($photoCount)"
                                    } else {
                                        "Fotoğrafları dışa aktar"
                                    },
                                )
                            },
                            // Fotoğraf yokken tıklanamaz ama görünür kalır,
                            // böylece özelliğin var olduğu belli olur.
                            enabled = photoCount > 0,
                            onClick = {
                                menuOpen = false
                                onExportPhotos()
                            },
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            text = { Text("Sürüm $appVersion") },
                            enabled = false,
                            onClick = {},
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 12.dp)) {
                    Text(greetingMessage(), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Türkiye'nin dört bir yanından esnaf lokantaları",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(50))
                                .background(ChipBackground)
                                .clickable { onSearchClick() }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = Terracotta)
                            Text(
                                "Şehir ara (81 il)",
                                modifier = Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(TerracottaContainer)
                                .clickable { onSurpriseMe() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = "Bugün ne yesem?", tint = Terracotta)
                        }
                    }
                }
            }

            item {
                SectionHeader("Şehirler", "$filledCityCount ilde lokanta kayıtlı")
            }

            items(cityRows, key = { row -> row.first().slug }) { rowCities ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowCities.forEach { city ->
                        CityCard(city, modifier = Modifier.weight(1f)) { onCityClick(city.name) }
                    }
                    if (rowCities.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                // Google Haritalar verisi kullanıldığı için atıf zorunlu.
                Text(
                    buildString {
                        append("Mekan bilgileri Google Haritalar'dan derlenmiştir.")
                        if (dataUpdatedAt.isNotBlank()) append(" Son güncelleme: $dataUpdatedAt")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
        }
    }
}

private fun greetingMessage(): String =
    when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 6..10 -> "Günaydın! Kahvaltı vakti"
        in 11..14 -> "Bugün nerede yemek yiyelim?"
        in 15..17 -> "İkindi keyfi"
        in 18..21 -> "İyi akşamlar, sofra vakti"
        else -> "Gece açsan doğru yerdesin"
    }

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        subtitle?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CityCard(city: City, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (city.hasRestaurants) TerracottaContainer else ChipBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.LocationCity,
                    contentDescription = null,
                    tint = if (city.hasRestaurants) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(city.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (city.hasRestaurants) "${city.restaurants.size} lokanta" else "Öneri bekliyor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
