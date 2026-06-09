package com.mauricior8.calorias.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta principal: indigo + teal, moderna y de buen contraste.
private val Indigo = Color(0xFF4F46E5)
private val IndigoClaro = Color(0xFFEEF0FF)
private val Teal = Color(0xFF0EA5A4)
private val Coral = Color(0xFFF97316)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoClaro,
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Coral,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEDEFF5),
    onSurfaceVariant = Color(0xFF44474E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    tertiary = Color(0xFFFDBA74),
    background = Color(0xFF111317),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2A2D33),
    onSurfaceVariant = Color(0xFFC4C6CF)
)

@Composable
fun CaloriasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
