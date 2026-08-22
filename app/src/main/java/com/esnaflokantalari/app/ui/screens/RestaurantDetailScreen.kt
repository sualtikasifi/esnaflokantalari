package com.esnaflokantalari.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.model.Restaurant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    if (restaurant == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Lokanta") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Bu lokanta bulunamadı.")
            }
        }
        return
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restaurant.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favori",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(restaurant.category, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Icon(Icons.Filled.Star, contentDescription = null)
                Text(" ${restaurant.rating} · ${restaurant.reviewCount} yorum")
            }
            restaurant.distanceMeters?.let { distance ->
                Spacer(modifier = Modifier.height(4.dp))
                Text("Yaklaşık ${(distance / 1000).let { "%.1f".format(it) }} km uzaklıkta")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Adres", fontWeight = FontWeight.Bold)
            Text(restaurant.address)
            restaurant.dailySpecial?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bugünün Önerisi", fontWeight = FontWeight.Bold)
                Text(it)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(restaurant.mapsUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Yol Tarifi Al")
            }
        }
    }
}
