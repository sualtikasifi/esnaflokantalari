package com.esnaflokantalari.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.esnaflokantalari.app.location.LocationHelper
import com.esnaflokantalari.app.ui.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    state: LoadState,
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
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Yakınındaki esnaf lokantalarını gösterebilmemiz için konumuna ihtiyacımız var.")
                Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                    Text("Konuma İzin Ver")
                }
            }
            return@Scaffold
        }

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
                        Text("Yakınında listelenecek bir esnaf lokantası bulunamadı.")
                    }
                    return@Scaffold
                }
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(padding),
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
                                restaurant.distanceMeters?.let { distance ->
                                    Text("${"%.1f".format(distance / 1000)} km uzaklıkta")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
