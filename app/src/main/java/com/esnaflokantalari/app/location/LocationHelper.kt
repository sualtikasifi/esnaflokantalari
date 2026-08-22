package com.esnaflokantalari.app.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

data class LatLng(val latitude: Double, val longitude: Double)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): LatLng? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        } catch (e: Exception) {
            null
        }
        return location?.let { LatLng(it.latitude, it.longitude) }
    }
}
