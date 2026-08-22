package com.esnaflokantalari.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.location.LocationHelper
import com.esnaflokantalari.app.ui.NearbyState
import com.esnaflokantalari.app.ui.components.RestaurantCard
import com.esnaflokantalari.app.ui.theme.Terracotta
import com.esnaflokantalari.app.ui.theme.TerracottaContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    state: NearbyState,
    favoriteIds: Set<String>,
    photos: Map<String, String>,
    onToggleFavorite: (String) -> Unit,
    onRestaurantClick: (String) -> Unit,
    onRequestNearby: () -> Unit,
    onPermissionDenied: () -> Unit,
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) onRequestNearby() else onPermissionDenied()
    }

    fun requestLocation() {
        if (LocationHelper.hasPermission(context)) {
            onRequestNearby()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    // İzin zaten varsa ekran açılır açılmaz aramaya başla.
    LaunchedEffect(Unit) {
        if (LocationHelper.hasPermission(context) && state is NearbyState.NeedsPermission) {
            onRequestNearby()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Yakınımdaki Lokantalar") }) },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)

        when (state) {
            is NearbyState.NeedsPermission -> InfoCard(
                icon = Icons.Filled.LocationOn,
                title = "Konum İzni Gerekli",
                description = "Sana en yakın esnaf lokantalarını gösterebilmemiz için konumuna " +
                    "ihtiyacımız var. Konumun cihazından çıkmaz, hiçbir yere gönderilmez.",
                modifier = contentModifier,
            ) {
                Button(
                    onClick = { requestLocation() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Konuma İzin Ver")
                }
            }

            is NearbyState.Locating -> InfoCard(
                icon = Icons.Filled.Restaurant,
                title = "Konumun bulunuyor...",
                description = "Sıcak bir çorba, taze ev yemekleri sizin için aranıyor.",
                showSpinner = true,
                modifier = contentModifier,
            ) {}

            is NearbyState.Failed -> InfoCard(
                icon = Icons.Filled.LocationOn,
                title = "Sonuç alınamadı",
                description = state.message,
                modifier = contentModifier,
            ) {
                if (state.canRetry) {
                    Button(
                        onClick = { requestLocation() },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Tekrar Dene")
                    }
                }
            }

            is NearbyState.Ready -> Column(modifier = Modifier.padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        buildString {
                            append("Sana en yakın ${state.restaurants.size} lokanta")
                            state.cityName?.let { append(" · $it civarı") }
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { requestLocation() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Yenile")
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.restaurants, key = { it.id }) { restaurant ->
                        RestaurantCard(
                            restaurant = restaurant,
                            isFavorite = favoriteIds.contains(restaurant.id),
                            onClick = { onRestaurantClick(restaurant.id) },
                            onToggleFavorite = { onToggleFavorite(restaurant.id) },
                            localPhotoPath = photos[restaurant.id],
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    showSpinner: Boolean = false,
    action: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(20.dp), verticalArrangement = Arrangement.Center) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(76.dp).clip(CircleShape).background(TerracottaContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showSpinner) {
                        CircularProgressIndicator(modifier = Modifier.size(76.dp), strokeWidth = 3.dp)
                    }
                    Icon(icon, contentDescription = null, tint = Terracotta, modifier = Modifier.size(32.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    description,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                action()
            }
        }
    }
}
