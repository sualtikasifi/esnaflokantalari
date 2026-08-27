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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.R
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.components.MapsLinkButton
import com.esnaflokantalari.app.ui.components.RestaurantBadgeIcon
import com.esnaflokantalari.app.ui.components.RestaurantVisual
import com.esnaflokantalari.app.ui.components.restaurantHasRealPhoto
import com.esnaflokantalari.app.ui.formatCount
import com.esnaflokantalari.app.ui.formatRating
import com.esnaflokantalari.app.ui.locationLabel
import com.esnaflokantalari.app.ui.openInMaps
import com.esnaflokantalari.app.ui.theme.ChipBackground
import com.esnaflokantalari.app.ui.theme.StarGold
import com.esnaflokantalari.app.ui.theme.Terracotta
import com.esnaflokantalari.app.ui.theme.TerracottaContainer

/** Ana sayfadaki "canın ne çekiyor" çipleri — tools/build_dataset.py'deki TAG_RULES ile eşleşir. */
val FOOD_CATEGORIES = listOf("Kebap", "Çorba", "Sulu Yemek", "Lahmacun & Pide", "Mantı", "Kahvaltı", "Döner", "Balık")

private const val MAX_LOCATION_PREVIEW = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cities: List<City>,
    dataUpdatedAt: String,
    lastKnownCityName: String?,
    lastKnownDistrictName: String?,
    appVersion: String,
    onCityClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onNearbyClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    // Kaydırma sırasında her karede yeniden hesaplanmasın diye önceden ayrılır.
    // Şehirler assets/restaurants.json içinde zaten nüfusa göre büyükten
    // küçüğe sıralı geliyor (bkz. tools/build_dataset.py).
    val cityRows = remember(cities) { cities.chunked(2) }
    val filledCityCount = remember(cities) { cities.count { it.hasRestaurants } }

    // İl vitrini: son bilinen konumun ilindeki lokantalar.
    val cityPreview = remember(cities, lastKnownCityName) {
        lastKnownCityName
            ?.let { name -> cities.firstOrNull { it.name == name } }
            ?.restaurants
            ?.take(MAX_LOCATION_PREVIEW)
            .orEmpty()
    }

    // İlçe vitrini: aynı ildeki lokantalar arasından, adresinden çıkarılan
    // ilçesi son bilinen ilçeyle eşleşenler (bkz. Restaurant.district).
    val districtPreview = remember(cities, lastKnownCityName, lastKnownDistrictName) {
        if (lastKnownDistrictName == null) {
            emptyList()
        } else {
            lastKnownCityName
                ?.let { name -> cities.firstOrNull { it.name == name } }
                ?.restaurants
                ?.filter { it.district == lastKnownDistrictName }
                ?.take(MAX_LOCATION_PREVIEW)
                .orEmpty()
        }
    }

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
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ChipBackground)
                            .clickable { onSearchClick() }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Terracotta)
                        Text(
                            "Şehir veya lokanta ara",
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (cityPreview.isNotEmpty()) {
                item {
                    SectionHeader(
                        "$lastKnownCityName civarında",
                        "Son bilinen konumundaki lokantalar",
                        onSeeAll = onNearbyClick,
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(cityPreview, key = { it.id }) { restaurant ->
                            FeaturedCard(
                                restaurant = restaurant,
                                onClick = { onRestaurantClick(restaurant.id) },
                            )
                        }
                    }
                }
            }

            if (districtPreview.isNotEmpty()) {
                item {
                    SectionHeader(
                        "$lastKnownDistrictName civarında",
                        "Bulunduğun ilçedeki lokantalar",
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(districtPreview, key = { it.id }) { restaurant ->
                            FeaturedCard(
                                restaurant = restaurant,
                                onClick = { onRestaurantClick(restaurant.id) },
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Canın ne çekiyor?") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(FOOD_CATEGORIES) { tag ->
                        Text(
                            tag,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ChipBackground)
                                .clickable { onTagClick(tag) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
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

@Composable
private fun SectionHeader(title: String, subtitle: String? = null, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (onSeeAll != null) {
            Row(
                modifier = Modifier.clickable { onSeeAll() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.NearMe, contentDescription = null, tint = Terracotta, modifier = Modifier.size(18.dp))
                Text(
                    "Tümü",
                    color = Terracotta,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    restaurant: Restaurant,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val hasRealPhoto = restaurantHasRealPhoto(restaurant)

    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp).height(FEATURED_CARD_HEIGHT),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp).fillMaxHeight()) {
            // Görsel ve puan yan yana: dar kartın genişliğini boşluk bırakmadan doldurur.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(13.dp)),
                    ) {
                        RestaurantVisual(
                            restaurant = restaurant,
                            modifier = Modifier.size(48.dp),
                            iconSize = 20.dp,
                            initialsSize = 14.sp,
                        )
                    }
                    restaurant.badge?.let { badge ->
                        RestaurantBadgeIcon(
                            badge = badge,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp),
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 11.dp)) {
                    if (restaurant.hasRating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Puan",
                                tint = StarGold,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                restaurant.rating!!.formatRating(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    restaurant.reviewCount?.takeIf { it > 0 }?.let {
                        Text(
                            "${it.formatCount()} yorum",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // İki satırlık sabit alan — kartların yüksekliği isim uzunluğundan etkilenmesin.
            Text(
                restaurant.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                restaurant.locationLabel(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!hasRealPhoto) {
                MapsLinkButton(
                    onClick = { context.openInMaps(restaurant) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Vitrin kartlarının ortak yüksekliği. İçerik (görsel satırı + iki satır ad +
 * konum + buton) tam bu boyu doldurur; büyütmek altta boşluk açar.
 */
private val FEATURED_CARD_HEIGHT = 194.dp

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
