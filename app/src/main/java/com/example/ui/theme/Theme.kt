package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    secondary = CosmicSecondary,
    tertiary = CosmicTertiary,
    background = CosmicBg,
    surface = CosmicSurface,
    surfaceVariant = CosmicSurfaceVariant,
    onBackground = CosmicOnBg,
    onSurface = CosmicOnSurface,
    onPrimary = CosmicOnPrimary
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),        // Indigo Light
    secondary = Color(0xFF0D9488),      // Teal Light
    tertiary = Color(0xFFC026D3),       // Magenta Light
    background = Color(0xFFF1F5F9),     // Light grey-blue bg
    surface = Color(0xFFFFFFFF),        // White surface
    surfaceVariant = Color(0xFFE2E8F0),  // Light gray border
    onBackground = Color(0xFF0F172A),   // Dark slate text
    onSurface = Color(0xFF1E293B),      // Dark slate text
    onPrimary = Color(0xFFFFFFFF)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CosmicColorScheme else CosmicLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
