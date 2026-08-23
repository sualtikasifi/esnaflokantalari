package com.esnaflokantalari.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import kotlin.math.roundToInt

private const val CROP_DECODE_MAX_EDGE_PX = 2200

/**
 * Galeriden seçilen (kırpılmamış) ekran görüntüsünü, kart oranına (16:9)
 * kilitli bir çerçeve içinde kaydırıp yakınlaştırarak kırpmayı sağlar.
 *
 * Ayrı bir kırpma uygulamasına geçmeye gerek kalmasın diye eklendi: artık
 * ekran görüntüsü olduğu gibi seçilip burada kırpılabiliyor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCropScreen(
    imageUri: Uri,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sourceBitmap by produceState<Bitmap?>(initialValue = null, imageUri) {
        value = decodeRotated(context, imageUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotoğrafı Kırp") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "İptal")
                    }
                },
            )
        },
    ) { padding ->
        val bitmap = sourceBitmap
        if (bitmap == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            var frameSize by remember { mutableStateOf(IntSize.Zero) }
            // scale: kullanıcının çerçeveyi kaplayan taban ölçeğin üstüne eklediği yakınlaştırma.
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val baseScale = remember(bitmap, frameSize) {
                if (frameSize.width == 0 || frameSize.height == 0) 1f
                else maxOf(
                    frameSize.width.toFloat() / bitmap.width,
                    frameSize.height.toFloat() / bitmap.height,
                )
            }

            // Çerçeve ilk ölçüldüğünde görseli ortala; taşan kenarlar eşit dağılsın.
            var centered by remember { mutableStateOf(false) }
            if (!centered && frameSize.width > 0 && frameSize.height > 0) {
                offset = Offset(
                    x = (frameSize.width - bitmap.width * baseScale) / 2f,
                    y = (frameSize.height - bitmap.height * baseScale) / 2f,
                )
                centered = true
            }

            fun clamp(candidate: Offset, effectiveScale: Float): Offset {
                val displayedW = bitmap.width * effectiveScale
                val displayedH = bitmap.height * effectiveScale
                val minX = frameSize.width - displayedW
                val minY = frameSize.height - displayedH
                return Offset(
                    x = candidate.x.coerceIn(minOf(minX, 0f), 0f),
                    y = candidate.y.coerceIn(minOf(minY, 0f), 0f),
                )
            }

            Text(
                "Doğru fotoğrafı görecek şekilde kaydır ve yakınlaştır",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black)
                    .onSizeChanged { frameSize = it }
                    .pointerInput(bitmap, frameSize) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldEffective = baseScale * scale
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            val newEffective = baseScale * newScale

                            val pannedOffset = offset + pan
                            val bitmapPointUnderCentroid = (centroid - pannedOffset) / oldEffective
                            var newOffset = centroid - bitmapPointUnderCentroid * newEffective
                            newOffset = clamp(newOffset, newEffective)

                            scale = newScale
                            offset = newOffset
                        }
                    },
            ) {
                val effectiveScale = baseScale * scale
                val widthDp = with(density) { bitmap.width.toDp() }
                val heightDp = with(density) { bitmap.height.toDp() }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(widthDp, heightDp)
                        .graphicsLayer {
                            scaleX = effectiveScale
                            scaleY = effectiveScale
                            translationX = offset.x
                            translationY = offset.y
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        val effectiveScale = baseScale * scale
                        val cropped = cropBitmap(bitmap, offset, effectiveScale, frameSize)
                        onConfirm(cropped)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Kaydet")
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("İptal")
                }
            }
        }
    }
}

private fun cropBitmap(source: Bitmap, offset: Offset, effectiveScale: Float, frameSize: IntSize): Bitmap {
    val left = (-offset.x / effectiveScale).roundToInt().coerceIn(0, source.width - 1)
    val top = (-offset.y / effectiveScale).roundToInt().coerceIn(0, source.height - 1)
    val width = (frameSize.width / effectiveScale).roundToInt().coerceIn(1, source.width - left)
    val height = (frameSize.height / effectiveScale).roundToInt().coerceIn(1, source.height - top)
    return Bitmap.createBitmap(source, left, top, width, height)
}

private fun decodeRotated(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    }

    var sampleSize = 1
    val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
    while (longestEdge > 0 && longestEdge / (sampleSize * 2) >= CROP_DECODE_MAX_EDGE_PX) {
        sampleSize *= 2
    }

    val bitmap = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }
    }.getOrNull() ?: return null

    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }
    val matrix = Matrix().apply { postRotate(degrees) }
    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrDefault(bitmap)
}
