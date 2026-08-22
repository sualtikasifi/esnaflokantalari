package com.esnaflokantalari.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Terracotta = Color(0xFFC0392B)
private val TerracottaDark = Color(0xFF8E2A1F)
private val Cream = Color(0xFFFFF8F0)

private val LightColors = lightColorScheme(
    primary = Terracotta,
    secondary = TerracottaDark,
    background = Cream,
)

private val DarkColors = darkColorScheme(
    primary = Terracotta,
    secondary = TerracottaDark,
)

@Composable
fun EsnafLokantalariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
