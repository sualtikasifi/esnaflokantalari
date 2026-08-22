package com.esnaflokantalari.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.suggestionsDataStore by preferencesDataStore(name = "suggestions")
private val SUGGESTIONS_KEY = stringSetPreferencesKey("user_suggestions")

/**
 * Kullanıcının önerdiği lokantalar. Sunucu maliyeti olmaması için cihazda
 * saklanır; kullanıcı isterse "Bize Gönder" ile e-posta/paylaşım üzerinden
 * iletir. Gelen öneriler aylık güncellemede CSV'ye eklenir.
 */
data class Suggestion(
    val id: String,
    val city: String,
    val name: String,
    val category: String,
    val address: String,
    val note: String,
    val createdAt: Long,
    val sent: Boolean = false,
)

class SuggestionsStore(private val context: Context) {

    val suggestions: Flow<List<Suggestion>> = context.suggestionsDataStore.data
        .map { preferences ->
            (preferences[SUGGESTIONS_KEY] ?: emptySet())
                .mapNotNull { it.toSuggestionOrNull() }
                .sortedByDescending { it.createdAt }
        }

    fun suggestionsForCity(cityName: String): Flow<List<Suggestion>> =
        suggestions.map { list -> list.filter { it.city.equalsTr(cityName) } }

    suspend fun add(suggestion: Suggestion) {
        context.suggestionsDataStore.edit { preferences ->
            val current = preferences[SUGGESTIONS_KEY] ?: emptySet()
            preferences[SUGGESTIONS_KEY] = current + suggestion.toJson()
        }
    }

    suspend fun remove(id: String) {
        context.suggestionsDataStore.edit { preferences ->
            val current = preferences[SUGGESTIONS_KEY] ?: emptySet()
            preferences[SUGGESTIONS_KEY] = current.filterNot {
                it.toSuggestionOrNull()?.id == id
            }.toSet()
        }
    }

    suspend fun markSent(id: String) {
        context.suggestionsDataStore.edit { preferences ->
            val current = preferences[SUGGESTIONS_KEY] ?: emptySet()
            preferences[SUGGESTIONS_KEY] = current.map { raw ->
                val suggestion = raw.toSuggestionOrNull()
                if (suggestion?.id == id) suggestion.copy(sent = true).toJson() else raw
            }.toSet()
        }
    }
}

private fun Suggestion.toJson(): String = JSONObject().apply {
    put("id", id)
    put("city", city)
    put("name", name)
    put("category", category)
    put("address", address)
    put("note", note)
    put("createdAt", createdAt)
    put("sent", sent)
}.toString()

private fun String.toSuggestionOrNull(): Suggestion? = runCatching {
    val json = JSONObject(this)
    Suggestion(
        id = json.getString("id"),
        city = json.optString("city"),
        name = json.getString("name"),
        category = json.optString("category"),
        address = json.optString("address"),
        note = json.optString("note"),
        createdAt = json.optLong("createdAt"),
        sent = json.optBoolean("sent"),
    )
}.getOrNull()
