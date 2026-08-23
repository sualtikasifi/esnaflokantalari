package com.esnaflokantalari.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val Context.photosDataStore by preferencesDataStore(name = "restaurant_photos")
private val PHOTO_KEY = stringSetPreferencesKey("restaurant_photo_paths")

/**
 * Kullanıcının galeriden seçtiği lokanta fotoğraflarını sıkıştırıp cihazda saklar.
 *
 * Sıkıştırma yaklaşımı: uzun kenar en fazla 1440 piksele indirilir ve JPEG
 * kalite 88 ile kaydedilir. Bu, telefon ekranında gözle fark edilmeyen bir
 * kayıpla 3–5 MB'lık fotoğrafı ~200–400 KB'ye düşürür.
 */
class PhotoStore(private val context: Context) {

    private val photoDir: File
        get() = File(context.filesDir, "restaurant_photos").apply { mkdirs() }

    /** restaurantId -> dosya yolu */
    val photos: Flow<Map<String, String>> = context.photosDataStore.data
        .map { preferences ->
            (preferences[PHOTO_KEY] ?: emptySet()).mapNotNull { entry ->
                val id = entry.substringBefore(SEPARATOR, "")
                val path = entry.substringAfter(SEPARATOR, "")
                if (id.isBlank() || path.isBlank() || !File(path).exists()) null else id to path
            }.toMap()
        }

    /**
     * Seçilen görseli sıkıştırıp kaydeder, dosya yolunu döndürür.
     * Başarısız olursa null döner.
     */
    suspend fun save(restaurantId: String, source: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(source) ?: return@withContext null
        val rotated = applyExifRotation(source, bitmap)

        val target = File(photoDir, "$restaurantId.jpg")
        runCatching {
            target.outputStream().use { output ->
                rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
        }.onFailure { return@withContext null }

        rotated.recycle()
        if (rotated !== bitmap) bitmap.recycle()

        context.photosDataStore.edit { preferences ->
            val current = preferences[PHOTO_KEY] ?: emptySet()
            val withoutOld = current.filterNot { it.substringBefore(SEPARATOR) == restaurantId }
            preferences[PHOTO_KEY] = (withoutOld + "$restaurantId$SEPARATOR${target.absolutePath}").toSet()
        }

        target.absolutePath
    }

    suspend fun remove(restaurantId: String) {
        withContext(Dispatchers.IO) {
            File(photoDir, "$restaurantId.jpg").delete()
            context.photosDataStore.edit { preferences ->
                val current = preferences[PHOTO_KEY] ?: emptySet()
                preferences[PHOTO_KEY] = current
                    .filterNot { it.substringBefore(SEPARATOR) == restaurantId }
                    .toSet()
            }
        }
    }

    /**
     * Cihazdaki tüm lokanta fotoğraflarını tek bir zip dosyasına toplar.
     *
     * Fotoğraflar sadece bu telefonda saklandığı için, diğer kullanıcıların da
     * görmesi isteniyorsa bu arşivi dışa aktarıp uygulamanın `assets/photos/`
     * klasörüne koymak gerekiyor. Dosya adları lokanta kimliğidir.
     */
    suspend fun exportAll(): File? = withContext(Dispatchers.IO) {
        val files = photoDir.listFiles()?.filter { it.extension == "jpg" }.orEmpty()
        if (files.isEmpty()) return@withContext null

        val exportDir = File(context.cacheDir, "exports").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val archive = File(exportDir, "esnaflokantalari-fotograflar.zip")

        runCatching {
            ZipOutputStream(archive.outputStream().buffered()).use { zip ->
                files.forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }.onFailure { return@withContext null }

        archive
    }

    /** Cihazda kaç fotoğraf var. */
    suspend fun count(): Int = withContext(Dispatchers.IO) {
        photoDir.listFiles()?.count { it.extension == "jpg" } ?: 0
    }

    /** Fotoğrafı belleğe sığacak şekilde, kaliteyi koruyarak ölçekleyerek okur. */
    private fun decodeScaled(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
        }.onFailure { return null }

        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        if (longestEdge <= 0) return null

        // Önce kaba ölçekleme (bellek için), sonra tam hedefe indirme (kalite için).
        var sampleSize = 1
        while (longestEdge / (sampleSize * 2) >= MAX_EDGE_PX) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = runCatching {
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }.getOrNull() ?: return null

        val decodedLongest = maxOf(decoded.width, decoded.height)
        if (decodedLongest <= MAX_EDGE_PX) return decoded

        val ratio = MAX_EDGE_PX.toFloat() / decodedLongest
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    /** Telefonla çekilen fotoğraflar yan yatmasın diye EXIF dönüşü uygulanır. */
    private fun applyExifRotation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(source)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: return bitmap

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

    private companion object {
        const val SEPARATOR = "::"
        const val MAX_EDGE_PX = 1440
        const val JPEG_QUALITY = 88
    }
}
