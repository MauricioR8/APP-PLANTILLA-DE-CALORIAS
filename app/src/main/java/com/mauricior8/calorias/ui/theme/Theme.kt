package com.mauricior8.calorias.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Naranja = Color(0xFFFF6D00)
private val NaranjaOscuro = Color(0xFFE65100)
private val VerdeAcento = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = Naranja,
    secondary = VerdeAcento,
    tertiary = NaranjaOscuro
)

private val DarkColors = darkColorScheme(
    primary = Naranja,
    secondary = VerdeAcento,
    tertiary = NaranjaOscuro
)

@Composable
fun CaloriasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
