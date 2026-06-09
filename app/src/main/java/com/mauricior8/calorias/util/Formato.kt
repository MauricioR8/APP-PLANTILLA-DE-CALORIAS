package com.mauricior8.calorias.util

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Paleta de colores sugeridos para las metricas. */
val ColoresMetrica = listOf(
    "#EF5350", // rojo
    "#EC407A", // rosa
    "#AB47BC", // morado
    "#5C6BC0", // indigo
    "#42A5F5", // azul
    "#26C6DA", // cian
    "#26A69A", // teal
    "#66BB6A", // verde
    "#FFCA28", // ambar
    "#FF7043", // naranja
    "#8D6E63", // marron
    "#78909C"  // gris azulado
)

/** Convierte un hex "#RRGGBB" a Color, con fallback naranja. */
fun colorDesdeHex(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        Color(0xFFFF6D00)
    }

/** Formatea un valor float quitando decimales innecesarios. */
fun formatoValor(valor: Float): String =
    if (valor % 1f == 0f) valor.toInt().toString() else "%.1f".format(valor)

private val formatoFechaHora = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
private val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

fun fechaHora(timestamp: Long): String = formatoFechaHora.format(Date(timestamp))

fun hora(timestamp: Long): String = formatoHora.format(Date(timestamp))

/** Codifica un mapa metricaId->valor como "id=valor;id=valor". */
fun codificarValores(valores: Map<String, Float>): String =
    valores.entries.joinToString(";") { "${it.key}=${it.value}" }

/** Decodifica el string de [codificarValores] a un mapa. */
fun decodificarValores(texto: String): Map<String, Float> {
    if (texto.isBlank()) return emptyMap()
    return texto.split(";").mapNotNull { par ->
        val partes = par.split("=")
        if (partes.size == 2) {
            val valor = partes[1].toFloatOrNull() ?: return@mapNotNull null
            partes[0] to valor
        } else null
    }.toMap()
}
