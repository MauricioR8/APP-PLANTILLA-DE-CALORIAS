package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.util.colorDesdeHex

/** Bloque base de la app: tarjeta redondeada con titulo opcional. */
@Composable
fun Bloque(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    acento: Color? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (titulo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (acento != null) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(acento))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }
            contenido()
        }
    }
}

/** Dato con titulo pequeno y valor destacado. */
@Composable
fun Dato(titulo: String, valor: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier) {
        Text(titulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/** Etiqueta pequena de color (pendiente, sugerencia...). */
@Composable
fun Insignia(texto: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

/** Tira de 7 dias (Lun-Dom); el seleccionado se rellena y cada dia puede llevar un punto de color. */
@Composable
fun TiraSemana(
    etiquetas: List<String>,
    seleccionado: Int,
    colores: List<Color?>,
    onElegir: (Int) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        etiquetas.forEachIndexed { i, texto ->
            val activo = i == seleccionado
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onElegir(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    texto,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (activo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colores.getOrNull(i) ?: Color.Transparent)
                )
            }
        }
    }
}

/** Anillo de progreso con contenido al centro. */
@Composable
fun AnilloGrande(
    progreso: Float,
    color: Color,
    modifier: Modifier = Modifier,
    centro: @Composable () -> Unit
) {
    val pista = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val grosor = 14.dp.toPx()
            val lado = size.minDimension - grosor
            val origen = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
            drawArc(pista, 0f, 360f, false, origen, Size(lado, lado), style = Stroke(grosor, cap = StrokeCap.Round))
            drawArc(
                color, -90f, 360f * progreso.coerceIn(0f, 1f), false, origen, Size(lado, lado),
                style = Stroke(grosor, cap = StrokeCap.Round)
            )
        }
        centro()
    }
}

/** Barra horizontal de progreso con color propio. */
@Composable
fun BarraProgreso(progreso: Float, color: Color, modifier: Modifier = Modifier, alto: Int = 8) {
    Box(
        modifier
            .fillMaxWidth()
            .height(alto.dp)
            .clip(RoundedCornerShape(alto.dp))
            .background(color.copy(alpha = 0.18f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progreso.coerceIn(0f, 1f))
                .height(alto.dp)
                .background(color)
        )
    }
}

/** Linea de tiempo (x = tiempo real) con puntos; opcionalmente una segunda linea (media movil). */
@Composable
fun GraficaLinea(
    puntos: List<Pair<Long, Float>>,
    color: Color,
    modifier: Modifier = Modifier,
    segunda: List<Float>? = null,
    colorSegunda: Color = Color.Gray,
    alto: Int = 140
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(alto.dp)
    ) {
        if (puntos.size < 2) return@Canvas
        val valores = puntos.map { it.second } + (segunda ?: emptyList())
        val minV = valores.min()
        val maxV = valores.max()
        val margen = maxOf((maxV - minV) * 0.15f, 0.5f)
        val bajo = minV - margen
        val rango = (maxV + margen) - bajo
        val t0 = puntos.first().first
        val duracion = maxOf(puntos.last().first - t0, 1L)
        val pad = 6.dp.toPx()

        fun punto(t: Long, v: Float) = Offset(
            pad + (t - t0).toFloat() / duracion * (size.width - 2 * pad),
            size.height - pad - (v - bajo) / rango * (size.height - 2 * pad)
        )

        fun ruta(valoresY: List<Float>): Path {
            val p = Path()
            puntos.forEachIndexed { i, (t, _) ->
                val o = punto(t, valoresY[i])
                if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y)
            }
            return p
        }
        if (segunda != null && segunda.size == puntos.size) {
            drawPath(ruta(segunda), colorSegunda, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        drawPath(ruta(puntos.map { it.second }), color.copy(alpha = if (segunda != null) 0.45f else 1f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        puntos.forEach { (t, v) -> drawCircle(color, radius = 3.5.dp.toPx(), center = punto(t, v)) }
    }
}

/** Barras horizontales (una por categoria) con una banda objetivo [minimo, maximo] opcional. */
@Composable
fun BarrasHorizontales(
    filas: List<Triple<String, Int, Color>>,
    minimo: Int? = null,
    maximo: Int? = null,
    escala: Int = 24
) {
    val pista = MaterialTheme.colorScheme.surfaceVariant
    val banda = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        filas.forEach { (nombre, valor, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nombre, modifier = Modifier.width(86.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                ) {
                    val tope = maxOf(escala, valor, maximo ?: 0).toFloat()
                    val radio = CornerRadius(8.dp.toPx())
                    drawRoundRect(pista, size = size, cornerRadius = radio)
                    if (minimo != null && maximo != null) {
                        drawRect(
                            banda,
                            topLeft = Offset(minimo / tope * size.width, 0f),
                            size = Size((maximo - minimo) / tope * size.width, size.height)
                        )
                    }
                    if (valor > 0) {
                        drawRoundRect(color, size = Size(valor / tope * size.width, size.height), cornerRadius = radio)
                    }
                }
                Text(
                    "$valor", modifier = Modifier.width(32.dp), textAlign = TextAlign.End,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
fun colorRutina(hex: String): Color = colorDesdeHex(hex)
