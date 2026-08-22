package com.esnaflokantalari.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.esnaflokantalari.app.location.LocationHelper
import com.esnaflokantalari.app.ui.LoadState
import com.esnaflokantalari.app.ui.components.RestaurantCard
import com.esnaflokantalari.app.ui.theme.TerracottaContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    state: LoadState,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onLocationReady: (Double, Double) -> Unit,
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionSkipped by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val location = LocationHelper.currentLocation(context)
            if (location != null) {
                onLocationReady(location.latitude, location.longitude)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yakınımdaki Lokantalar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        if (!hasPermission && !permissionSkipped) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                IconTextCard(
                    icon = Icons.Filled.LocationOn,
                    title = "Konum İzni Gerekli",
                    description = "Konumuna göre en iyi lokantaları görmek için izin ver.",
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Konuma İzin Ver")
                        }
                        androidx.compose.material3.TextButton(onClick = { permissionSkipped = true }) {
                            Text("Daha Sonra")
                        }
                    }
                }
            }
            return@Scaffold
        }

        when (state) {
            is LoadState.Loading -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                IconTextCard(
                    icon = Icons.Filled.Restaurant,
                    title = "Lokantalar aranıyor...",
                    description = "Sıcak bir çorba, taze ev yemekleri sizin için bulunuyor. Lütfen bekleyin.",
                    showSpinner = true,
                ) {}
            }

            is LoadState.Failed -> Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                Text(state.message)
            }

            is LoadState.Loaded -> {
                if (state.restaurants.isEmpty()) {
                    Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                        Text("Yakınında listelenecek bir esnaf lokantası bulunamadı.")
                    }
                    return@Scaffold
                }
                Column(modifier = Modifier.padding(padding)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Size en yakın ${state.restaurants.size} sonuç bulundu.",
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = {
                                val first = state.restaurants.first()
                                val uri = Uri.parse(
                                    "geo:0,0?q=${first.latitude ?: 0.0},${first.longitude ?: 0.0}(${first.name})",
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            shape = RoundedCornerShape(50),
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Haritada Gör")
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.restaurants) { restaurant ->
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

@Composable
private fun IconTextCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    showSpinner: Boolean = false,
    content: @Composable () -> Unit,
) {
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
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TerracottaContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (showSpinner) {
                    CircularProgressIndicator(modifier = Modifier.size(72.dp))
                }
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                description,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            content()
        }
    }
}
