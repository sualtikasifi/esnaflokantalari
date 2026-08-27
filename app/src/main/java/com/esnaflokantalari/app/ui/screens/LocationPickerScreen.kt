package com.esnaflokantalari.app.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.location.LocationHelper
import com.esnaflokantalari.app.model.City
import com.esnaflokantalari.app.ui.theme.Terracotta
import com.esnaflokantalari.app.ui.theme.TerracottaContainer

/**
 * İl/ilçe elle seçimi. Konum açıksa "Konumumu kullan" GPS'ten algılar;
 * kapalıysa kullanıcıyı açmaya yönlendirir ama listeden elle seçmeyi de
 * her zaman açık bırakır — konumu açmak istemeyen kullanıcı takılıp kalmaz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    cities: List<City>,
    onUseCurrentLocation: () -> Unit,
    onSelect: (cityName: String, districtName: String?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedCity by remember { mutableStateOf<City?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            onUseCurrentLocation()
        } else {
            Toast.makeText(
                context,
                "Konumu açman gerekiyor. Aşağıdan elle de seçebilirsin.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun useCurrentLocation() {
        if (LocationHelper.hasPermission(context)) {
            onUseCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCity?.name ?: "İl / ilçe seç") },
                navigationIcon = {
                    IconButton(onClick = { if (selectedCity != null) selectedCity = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        val city = selectedCity
        if (city == null) {
            Column(modifier = Modifier.padding(padding)) {
                Card(
                    onClick = ::useCurrentLocation,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TerracottaContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Terracotta)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Konumumu kullan", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Konum açıksa GPS'ten otomatik algılar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    "ya da elle seç",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(cities, key = { it.slug }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCity = c }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(c.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            val districts = remember(city) { city.restaurants.mapNotNull { it.district }.distinct().sorted() }
            LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    DistrictRow("Tüm il", onClick = { onSelect(city.name, null) })
                }
                items(districts) { district ->
                    DistrictRow(district, onClick = { onSelect(city.name, district) })
                }
            }
        }
    }
}

@Composable
private fun DistrictRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
