package com.esnaflokantalari.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esnaflokantalari.app.model.Restaurant
import com.esnaflokantalari.app.ui.components.RestaurantVisual
import com.esnaflokantalari.app.ui.openInMaps

/**
 * Fotoğrafı eksik lokantaları sırayla gösterip tek tıkla fotoğraf eklemeyi
 * sağlar. Bir fotoğraf kaydedilince o lokanta [restaurants] listesinden
 * otomatik düşer (bkz. AppViewModel.missingPhotoRestaurants), bu yüzden
 * ekran kendiliğinden bir sonraki lokantaya geçer — kullanıcı her seferinde
 * şehir listesine dönüp yeniden aramak zorunda kalmaz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoQueueScreen(
    restaurants: List<Restaurant>,
    onPickPhoto: (restaurantId: String, Bitmap) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var skipped by remember { mutableStateOf(setOf<String>()) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCropRestaurantId by remember { mutableStateOf<String?>(null) }

    val queue = remember(restaurants, skipped) { restaurants.filterNot { it.id in skipped } }
    val current = queue.firstOrNull()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null && current != null) {
            pendingCropRestaurantId = current.id
            pendingCropUri = uri
        }
    }

    val cropUri = pendingCropUri
    val cropRestaurantId = pendingCropRestaurantId
    if (cropUri != null && cropRestaurantId != null) {
        PhotoCropScreen(
            imageUri = cropUri,
            onCancel = {
                pendingCropUri = null
                pendingCropRestaurantId = null
            },
            onConfirm = { bitmap ->
                onPickPhoto(cropRestaurantId, bitmap)
                pendingCropUri = null
                pendingCropRestaurantId = null
                Toast.makeText(context, "Fotoğraf eklendi", Toast.LENGTH_SHORT).show()
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotoğraf Ekle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            EmptyQueueState(
                totalRemaining = restaurants.size,
                skippedCount = skipped.size,
                onShowSkipped = { skipped = emptySet() },
                onBack = onBack,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                "${restaurants.size} lokantada fotoğraf eksik",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RestaurantVisual(
                restaurant = current,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp)),
                initialsSize = 40.sp,
            )

            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(current.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${current.city} · ${current.address.ifBlank { "adres yok" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { context.openInMaps(current) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Haritada Gör")
                    }
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Fotoğraf Seç")
                    }
                    OutlinedButton(
                        onClick = { skipped = skipped + current.id },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Atla, sıradakine geç")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQueueState(
    totalRemaining: Int,
    skippedCount: Int,
    onShowSkipped: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Celebration,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        if (totalRemaining == 0) {
            Text("Tüm lokantalarda fotoğraf var!", style = MaterialTheme.typography.titleLarge)
        } else {
            Text("Bu turda atladıkların: $skippedCount", style = MaterialTheme.typography.titleLarge)
            Text(
                "Hâlâ $totalRemaining lokantada fotoğraf eksik.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(onClick = onShowSkipped, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 20.dp)) {
                Text("Atlananları tekrar göster")
            }
        }
        OutlinedButton(onClick = onBack, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 12.dp)) {
            Text("Ana Sayfaya Dön")
        }
    }
}
