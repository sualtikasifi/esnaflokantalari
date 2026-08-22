package com.esnaflokantalari.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// --- Açık tema ---
private val TerracottaLight = Color(0xFFB6392C)
private val TerracottaDarkLight = Color(0xFF8E2A1F)
private val TerracottaContainerLight = Color(0xFFF6D9CF)
private val CreamLight = Color(0xFFFBF3EA)
private val CreamSurfaceLight = Color(0xFFF4E9DE)
private val OnCreamLight = Color(0xFF241A15)
private val MutedBrownLight = Color(0xFF7A6A5F)
private val ChipBackgroundLight = Color(0xFFEFE3D6)
private val StarGoldLight = Color(0xFFC98A16)

// --- Karanlık tema ---
private val TerracottaDarkMode = Color(0xFFFFB4A6)
private val TerracottaContainerDarkMode = Color(0xFF5C2018)
private val SurfaceDarkMode = Color(0xFF2A211C)
private val BackgroundDarkMode = Color(0xFF1B1512)
private val OnSurfaceDarkMode = Color(0xFFF0E4DC)
private val MutedDarkMode = Color(0xFFBCA79B)
private val ChipBackgroundDarkMode = Color(0xFF3A2E27)
private val StarGoldDarkMode = Color(0xFFE8BC5A)

private val LightColors = lightColorScheme(
    primary = TerracottaLight,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainerLight,
    onPrimaryContainer = TerracottaDarkLight,
    secondary = TerracottaDarkLight,
    onSecondary = Color.White,
    secondaryContainer = ChipBackgroundLight,
    onSecondaryContainer = OnCreamLight,
    background = CreamLight,
    onBackground = OnCreamLight,
    surface = Color.White,
    onSurface = OnCreamLight,
    surfaceVariant = CreamSurfaceLight,
    onSurfaceVariant = MutedBrownLight,
    outline = MutedBrownLight,
)

private val DarkColors = darkColorScheme(
    primary = TerracottaDarkMode,
    onPrimary = Color(0xFF5C1008),
    primaryContainer = TerracottaContainerDarkMode,
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFE7BDB4),
    onSecondary = Color(0xFF44231D),
    secondaryContainer = ChipBackgroundDarkMode,
    onSecondaryContainer = OnSurfaceDarkMode,
    background = BackgroundDarkMode,
    onBackground = OnSurfaceDarkMode,
    surface = SurfaceDarkMode,
    onSurface = OnSurfaceDarkMode,
    surfaceVariant = ChipBackgroundDarkMode,
    onSurfaceVariant = MutedDarkMode,
    outline = MutedDarkMode,
)

/**
 * Temanın kendi renk şemasında karşılığı olmayan, uygulamaya özel renkler.
 * Böylece karanlık modda da doğru tonlar kullanılır.
 */
data class AppAccents(
    val star: Color,
    val chip: Color,
    val brand: Color,
    val brandContainer: Color,
    val softSurface: Color,
)

private val LocalAppAccents: ProvidableCompositionLocal<AppAccents> = compositionLocalOf {
    AppAccents(
        star = StarGoldLight,
        chip = ChipBackgroundLight,
        brand = TerracottaLight,
        brandContainer = TerracottaContainerLight,
        softSurface = CreamSurfaceLight,
    )
}

// Ekranlarda kısa yoldan kullanmak için.
val StarGold: Color @Composable get() = LocalAppAccents.current.star
val ChipBackground: Color @Composable get() = LocalAppAccents.current.chip
val Terracotta: Color @Composable get() = LocalAppAccents.current.brand
val TerracottaContainer: Color @Composable get() = LocalAppAccents.current.brandContainer
val CreamSurface: Color @Composable get() = LocalAppAccents.current.softSurface

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
)

@Composable
fun EsnafLokantalariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val accents = if (darkTheme) {
        AppAccents(
            star = StarGoldDarkMode,
            chip = ChipBackgroundDarkMode,
            brand = TerracottaDarkMode,
            brandContainer = TerracottaContainerDarkMode,
            softSurface = ChipBackgroundDarkMode,
        )
    } else {
        AppAccents(
            star = StarGoldLight,
            chip = ChipBackgroundLight,
            brand = TerracottaLight,
            brandContainer = TerracottaContainerLight,
            softSurface = CreamSurfaceLight,
        )
    }

    CompositionLocalProvider(LocalAppAccents provides accents) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
