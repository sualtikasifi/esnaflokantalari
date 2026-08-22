package com.esnaflokantalari.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Terracotta = Color(0xFFC0392B)
private val TerracottaDark = Color(0xFF8E2A1F)
private val TerracottaContainer = Color(0xFFFFDAD3)
private val Cream = Color(0xFFFFF8F0)
private val OnCream = Color(0xFF201A17)
private val SurfaceLight = Color(0xFFFFFBF9)

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    secondary = TerracottaDark,
    background = Cream,
    onBackground = OnCream,
    surface = SurfaceLight,
    onSurface = OnCream,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A6),
    onPrimary = Color(0xFF690000),
    secondary = Color(0xFFE7BDB4),
    background = Color(0xFF201A17),
    surface = Color(0xFF201A17),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun EsnafLokantalariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
