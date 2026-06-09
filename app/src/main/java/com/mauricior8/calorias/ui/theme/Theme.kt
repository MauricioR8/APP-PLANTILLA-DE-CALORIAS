package com.mauricior8.calorias.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta moderna: violeta vibrante + teal + ambar (estilo fresco, nada de azul plano).
private val Violeta = Color(0xFF7C3AED)
private val VioletaClaro = Color(0xFFEDE9FE)
private val Teal = Color(0xFF14B8A6)
private val Ambar = Color(0xFFF59E0B)

/** Verde distintivo para el estado "dia completado". */
val VerdeCompletado = Color(0xFF22C55E)

private val LightColors = lightColorScheme(
    primary = Violeta,
    onPrimary = Color.White,
    primaryContainer = VioletaClaro,
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0F3D38),
    tertiary = Ambar,
    onTertiary = Color(0xFF3B2600),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFEDE9F4),
    onSurfaceVariant = Color(0xFF4A4458),
    error = Color(0xFFE11D48)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF4C2889),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF0F3D38),
    tertiary = Color(0xFFFCD34D),
    background = Color(0xFF131218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF1C1B22),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFFF6B81)
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
