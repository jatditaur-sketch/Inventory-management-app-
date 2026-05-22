package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ElegantColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = AccentPurpleBg,
    primaryContainer = AccentPurpleBg,
    onPrimaryContainer = LightPurpleAccent,
    secondary = LightPurpleAccent,
    onSecondary = AccentPurpleBg,
    background = DarkBackground,
    onBackground = OnBackground,
    surface = DarkSurface,
    onSurface = OnSurface,
    surfaceVariant = NavBackground,
    onSurfaceVariant = GraySecondary,
    error = LowStockRed,
    onError = LowStockText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for "Elegant Dark" design preference
    dynamicColor: Boolean = false, // Disable dynamic colors to keep StockSync brand theme intact
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ElegantColorScheme,
        typography = Typography,
        content = content
    )
}
