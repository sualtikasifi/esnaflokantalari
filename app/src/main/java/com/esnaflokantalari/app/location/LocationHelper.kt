package com.esnaflokantalari.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class LatLng(val latitude: Double, val longitude: Double)

sealed interface LocationResult {
    data class Success(val location: LatLng) : LocationResult
    data object PermissionMissing : LocationResult
    data object ServiceDisabled : LocationResult
    data object Unavailable : LocationResult
}

object LocationHelper {

    private const val FRESH_FIX_TIMEOUT_MS = 15_000L

    /**
     * Konumu üç aşamada dener:
     *   1. Son bilinen konum (anında döner)
     *   2. Taze konum ölçümü (zaman aşımlı)
     *   3. Sistem konum sağlayıcısının son kaydı
     *
     * Tek bir yönteme güvenmek cihazlarda sık sık boş dönüyordu.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): LocationResult {
        if (!hasPermission(context)) return LocationResult.PermissionMissing
        if (!isLocationEnabled(context)) return LocationResult.ServiceDisabled

        val client = LocationServices.getFusedLocationProviderClient(context)

        runCatching { client.lastLocation.await() }
            .getOrNull()
            ?.let { return LocationResult.Success(LatLng(it.latitude, it.longitude)) }

        val fresh = withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) {
            runCatching {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            }.getOrNull()
        }
        if (fresh != null) return LocationResult.Success(LatLng(fresh.latitude, fresh.longitude))

        systemLastKnown(context)?.let { return LocationResult.Success(it) }

        return LocationResult.Unavailable
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /** Play Services boş dönerse sistemin kendi kaydına bakılır. */
    @SuppressLint("MissingPermission")
    private fun systemLastKnown(context: Context): LatLng? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        for (provider in providers) {
            val location = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            if (location != null) return LatLng(location.latitude, location.longitude)
        }
        return null
    }
}
