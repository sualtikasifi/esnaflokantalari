package com.esnaflokantalari.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lastCityDataStore by preferencesDataStore(name = "last_city")
private val LAST_CITY_KEY = stringPreferencesKey("last_known_city")

/**
 * En son GPS ile doğrulanan ilin adını saklar. "Yakınımda" ve "Bugün ne
 * yesem?" her seferinde yeni bir konum araması beklemek yerine, kullanıcı
 * konumunu değiştirdiğini belirtene kadar (Yenile ile) bu ile göre önerilere
 * devam eder.
 */
class LastCityStore(private val context: Context) {

    val cityName: Flow<String?> = context.lastCityDataStore.data
        .map { preferences -> preferences[LAST_CITY_KEY]?.takeIf { it.isNotBlank() } }

    suspend fun get(): String? = cityName.first()

    suspend fun save(name: String) {
        context.lastCityDataStore.edit { preferences -> preferences[LAST_CITY_KEY] = name }
    }
}
