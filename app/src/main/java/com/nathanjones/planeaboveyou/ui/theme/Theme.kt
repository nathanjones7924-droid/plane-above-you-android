package com.nathanjones.planeaboveyou.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A7EBB),
    secondary = Color(0xFF66B2FF),
    tertiary = Color(0xFF80CBC4),
    surface = Color(0xFF1C1C1E),
    background = Color(0xFF121212),
    error = Color(0xFFFF5252),
    onSurface = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF2196F3),
    tertiary = Color(0xFF009688),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F5),
    error = Color(0xFFD32F2F),
    onSurface = Color(0xFF000000)
)

@Composable
fun PlaneAboveYouTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
