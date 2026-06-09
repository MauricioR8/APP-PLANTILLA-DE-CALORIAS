package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.util.ColoresMetrica
import com.mauricior8.calorias.util.colorDesdeHex
import com.mauricior8.calorias.util.formatoValor

/**
 * Dibuja una grafica a partir de pares (etiqueta, valor) segun el tipo elegido:
 * "Barras", "Histograma", "Pastel" o "Picos".
 */
@Composable
fun GraficaTabla(
    datos: List<Pair<String, Float>>,
    tipo: String,
    modifier: Modifier = Modifier
) {
    val validos = datos.filter { it.second > 0f }
    if (validos.isEmpty()) {
        Text(
            "No hay valores numericos para graficar en esta columna.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(8.dp)
        )
        return
    }

    val colores = validos.indices.map { colorDesdeHex(ColoresMetrica[it % ColoresMetrica.size]) }
    val maximo = validos.maxOf { it.second }
    val total = validos.sumOf { it.second.toDouble() }.toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
        ) {
            when (tipo) {
                "Pastel" -> dibujarPastel(validos, colores, total)
                "Picos" -> dibujarPicos(validos, colores, maximo)
                else -> dibujarBarras(validos, colores, maximo, tipo == "Histograma")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Leyenda con color, etiqueta y valor.
        validos.forEachIndexed { i, (etiqueta, valor) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(colores[i], CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
                }
                Text(formatoValor(valor), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dibujarBarras(
    datos: List<Pair<String, Float>>,
    colores: List<Color>,
    maximo: Float,
    histograma: Boolean
) {
    val n = datos.size
    val separacion = if (histograma) 0f else size.width * 0.02f
    val anchoBarra = (size.width - separacion * (n + 1)) / n
    datos.forEachIndexed { i, (_, valor) ->
        val alturaBarra = (valor / maximo) * size.height
        val x = separacion + i * (anchoBarra + separacion)
        drawRect(
            color = colores[i],
            topLeft = Offset(x, size.height - alturaBarra),
            size = Size(anchoBarra, alturaBarra)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dibujarPicos(
    datos: List<Pair<String, Float>>,
    colores: List<Color>,
    maximo: Float
) {
    if (datos.size == 1) {
        val y = size.height - (datos[0].second / maximo) * size.height
        drawCircle(colores[0], radius = 8f, center = Offset(size.width / 2f, y))
        return
    }
    val paso = size.width / (datos.size - 1)
    val puntos = datos.mapIndexed { i, (_, valor) ->
        Offset(i * paso, size.height - (valor / maximo) * size.height)
    }
    val path = Path().apply {
        moveTo(puntos.first().x, puntos.first().y)
        puntos.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color = colores.first(), style = Stroke(width = 5f))
    puntos.forEachIndexed { i, p ->
        drawCircle(colores[i], radius = 7f, center = p)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dibujarPastel(
    datos: List<Pair<String, Float>>,
    colores: List<Color>,
    total: Float
) {
    var inicio = -90f
    val lado = minOf(size.width, size.height)
    val topLeft = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
    datos.forEachIndexed { i, (_, valor) ->
        val barrido = (valor / total) * 360f
        drawArc(
            color = colores[i],
            startAngle = inicio,
            sweepAngle = barrido,
            useCenter = true,
            topLeft = topLeft,
            size = Size(lado, lado)
        )
        inicio += barrido
    }
}
