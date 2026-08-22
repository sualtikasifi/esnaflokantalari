package com.esnaflokantalari.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.esnaflokantalari.app.data.Suggestion
import com.esnaflokantalari.app.ui.theme.Terracotta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestScreen(
    cityName: String,
    suggestions: List<Suggestion>,
    onBack: () -> Unit,
    onSubmit: (name: String, category: String, address: String, note: String) -> Unit,
    onDelete: (String) -> Unit,
    onMarkSent: (String) -> Unit,
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val canSubmit = name.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$cityName · Lokanta Öner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "$cityName'da bildiğin bir esnaf lokantasını bize bildir. " +
                        "Önerin cihazında saklanır; istersen tek dokunuşla bize gönderirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Lokantanın adı *") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Ne yeniyor? (Kebap, Sulu Yemek, Çorba...)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Nerede? (mahalle, cadde)") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notun (neyi meşhur?)") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Button(
                    onClick = {
                        onSubmit(name, category, address, note)
                        name = ""
                        category = ""
                        address = ""
                        note = ""
                        Toast.makeText(context, "Önerin kaydedildi, teşekkürler!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Öneriyi Kaydet")
                }
            }

            if (suggestions.isNotEmpty()) {
                item {
                    Text(
                        "$cityName için önerilerin (${suggestions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(suggestions, key = { it.id }) { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        onDelete = { onDelete(suggestion.id) },
                        onSend = {
                            val sent = context.sendSuggestion(suggestion)
                            if (sent) onMarkSent(suggestion.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: Suggestion, onDelete: () -> Unit, onSend: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.name, style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(
                    suggestion.category.takeIf { it.isNotBlank() },
                    suggestion.address.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (suggestion.sent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(
                            "Gönderildi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Terracotta,
                        )
                    }
                }
            }
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Bize gönder", tint = Terracotta)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Sil")
            }
        }
    }
}

/**
 * Öneriyi paylaşım menüsüyle iletir (e-posta, WhatsApp, notlar...).
 * Sunucu maliyeti olmaması için bilinçli olarak paylaşım kullanılıyor.
 */
private fun android.content.Context.sendSuggestion(suggestion: Suggestion): Boolean {
    val body = buildString {
        appendLine("Yeni lokanta önerisi")
        appendLine("Şehir: ${suggestion.city}")
        appendLine("Ad: ${suggestion.name}")
        if (suggestion.category.isNotBlank()) appendLine("Kategori: ${suggestion.category}")
        if (suggestion.address.isNotBlank()) appendLine("Adres: ${suggestion.address}")
        if (suggestion.note.isNotBlank()) appendLine("Not: ${suggestion.note}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Lokanta önerisi: ${suggestion.name} (${suggestion.city})")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        startActivity(Intent.createChooser(intent, "Öneriyi gönder"))
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "Gönderim için uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
        false
    }
}
