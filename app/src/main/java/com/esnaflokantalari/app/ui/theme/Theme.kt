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

val Terracotta = Color(0xFFB6392C)
val TerracottaDark = Color(0xFF8E2A1F)
val TerracottaContainer = Color(0xFFF6D9CF)
val Cream = Color(0xFFFBF3EA)
val CreamSurface = Color(0xFFF4E9DE)
val OnCream = Color(0xFF241A15)
val MutedBrown = Color(0xFF7A6A5F)
val ChipBackground = Color(0xFFEFE3D6)
val StarGold = Color(0xFFE0A22A)

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaDark,
    secondary = TerracottaDark,
    secondaryContainer = ChipBackground,
    background = Cream,
    onBackground = OnCream,
    surface = Color.White,
    onSurface = OnCream,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = MutedBrown,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A6),
    onPrimary = Color(0xFF690000),
    secondary = Color(0xFFE7BDB4),
    background = Color(0xFF201A17),
    surface = Color(0xFF2A211C),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
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
