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
private val LAST_DISTRICT_KEY = stringPreferencesKey("last_known_district")

/**
 * En son GPS ile doğrulanan il/ilçeyi saklar. "Yakınımda" ve "Bugün ne
 * yesem?" her seferinde yeni bir konum araması beklemek yerine, kullanıcı
 * konumunu değiştirdiğini belirtene kadar (Yenile ile) bu il/ilçeye göre
 * önerilere devam eder.
 */
class LastCityStore(private val context: Context) {

    val cityName: Flow<String?> = context.lastCityDataStore.data
        .map { preferences -> preferences[LAST_CITY_KEY]?.takeIf { it.isNotBlank() } }

    val districtName: Flow<String?> = context.lastCityDataStore.data
        .map { preferences -> preferences[LAST_DISTRICT_KEY]?.takeIf { it.isNotBlank() } }

    suspend fun get(): String? = cityName.first()

    /**
     * İl adını her zaman günceller. İlçe sadece verildiğinde güncellenir —
     * null geçilirse (ör. "Bugün ne yesem?" sadece il belirlediğinde) ve il
     * değişmediyse daha önce bilinen ilçe olduğu gibi kalır; il değiştiyse
     * artık geçersiz olacağından silinir.
     *
     * [forceClearDistrict] elle seçimde kullanılır: kullanıcı bilerek "Tümü"
     * (ilçesiz) seçtiğinde, il değişmemiş olsa bile eski ilçe silinsin diye.
     */
    suspend fun save(name: String, district: String? = null, forceClearDistrict: Boolean = false) {
        context.lastCityDataStore.edit { preferences ->
            val cityChanged = preferences[LAST_CITY_KEY] != name
            preferences[LAST_CITY_KEY] = name
            when {
                district != null -> preferences[LAST_DISTRICT_KEY] = district
                cityChanged || forceClearDistrict -> preferences.remove(LAST_DISTRICT_KEY)
            }
        }
    }
}
