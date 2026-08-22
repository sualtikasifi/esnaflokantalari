package com.esnaflokantalari.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.components.RestaurantVisual
import com.esnaflokantalari.app.ui.formatRating
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.StarGold
import com.esnaflokantalari.app.ui.theme.Terracotta
import com.esnaflokantalari.app.ui.theme.TerracottaContainer
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cities: List<City>,
    featured: List<Restaurant>,
    dataUpdatedAt: String,
    onCityClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onSurpriseMe: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Esnaf Lokantaları") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(greetingMessage(), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Türkiye'nin dört bir yanından esnaf lokantaları",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ChipBackground)
                            .clickable { onSearchClick() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Terracotta)
                        Text(
                            "Şehir ara (81 il)",
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (featured.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(TerracottaContainer)
                                .clickable { onSurpriseMe() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = null, tint = Terracotta)
                            Text(
                                "Bugün ne yesem? Bana bir yer seç",
                                modifier = Modifier.padding(start = 10.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            if (featured.isNotEmpty()) {
                item {
                    SectionHeader("Öne Çıkanlar", "En yüksek puanlı esnaf lokantaları")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        items(featured, key = { it.id }) { restaurant ->
                            FeaturedCard(restaurant) { onRestaurantClick(restaurant.id) }
                        }
                    }
                }
            }

            item {
                SectionHeader("Şehirler", "${cities.count { it.hasRestaurants }} ilde lokanta kayıtlı")
            }

            items(cities.chunked(2), key = { it.first().slug }) { rowCities ->
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
private fun FeaturedCard(restaurant: Restaurant, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(170.dp),
    ) {
        Column {
            Box {
                RestaurantVisual(
                    restaurant = restaurant,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    initialsSize = 30.sp,
                )
                if (restaurant.hasRating) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.94f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            restaurant.rating!!.formatRating(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF241A15),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    restaurant.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    restaurant.city,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
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
