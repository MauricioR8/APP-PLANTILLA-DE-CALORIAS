package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.ui.state.Progreso
import com.mauricior8.calorias.util.colorDesdeHex
import com.mauricior8.calorias.util.fechaHora
import com.mauricior8.calorias.util.formatoValor
import java.util.Locale

/**
 * Pestana "Progreso": totales diarios de los ultimos 7 o 30 dias por metrica y
 * seguimiento del peso corporal con su tendencia.
 */
@Composable
fun ProgresoScreen(
    metricas: List<MetricaConfig>,
    progreso: Progreso,
    rangoDias: Int,
    pesos: List<PesoRegistro>,
    onRango: (Int) -> Unit,
    onAgregarPeso: (Float) -> Unit,
    onEliminarPeso: (PesoRegistro) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item(key = "consumo") {
            TarjetaConsumo(metricas, progreso, rangoDias, onRango)
        }
        item(key = "peso") {
            TarjetaPeso(pesos, onAgregarPeso, onEliminarPeso)
        }
    }
}

@Composable
private fun TarjetaConsumo(
    metricas: List<MetricaConfig>,
    progreso: Progreso,
    rangoDias: Int,
    onRango: (Int) -> Unit
) {
    var seleccionada by remember { mutableStateOf<String?>(null) }
    val metrica = metricas.firstOrNull { it.id == seleccionada } ?: metricas.firstOrNull()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Consumo diario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7 días", 30 to "30 días").forEach { (dias, texto) ->
                    FilterChip(selected = rangoDias == dias, onClick = { onRango(dias) }, label = { Text(texto) })
                }
            }
            if (metrica == null) {
                Text(
                    "Crea una métrica en \"Mi día\" para ver su progreso.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                return@Column
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metricas.forEach { m ->
                    FilterChip(
                        selected = m.id == metrica.id,
                        onClick = { seleccionada = m.id },
                        label = { Text(m.nombre) }
                    )
                }
            }

            val valores = progreso.totales[metrica.id] ?: List(progreso.etiquetas.size) { 0f }
            val color = colorDesdeHex(metrica.colorHex)
            GraficaDias(
                valores = valores,
                limite = metrica.limiteMaximo,
                color = color,
                colorLimite = MaterialTheme.colorScheme.error
            )
            if (progreso.etiquetas.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(progreso.etiquetas.first(), style = MaterialTheme.typography.labelSmall)
                    Text("hoy", style = MaterialTheme.typography.labelSmall)
                }
            }

            val conDatos = valores.filter { it > 0f }
            val promedio = if (conDatos.isEmpty()) 0f else conDatos.sum() / conDatos.size
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Dato("Promedio", "${formatoValor(promedio)} ${metrica.unidad}")
                Dato("Máximo", "${formatoValor(valores.maxOrNull() ?: 0f)} ${metrica.unidad}")
                Dato("Días con registro", "${conDatos.size} de ${valores.size}")
            }
            val limite = metrica.limiteMaximo
            if (limite != null && limite > 0f) {
                Text(
                    "Línea roja = meta de ${formatoValor(limite)} ${metrica.unidad}. " +
                        "Las barras que la superan se marcan en rojo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Dato(titulo: String, valor: String) {
    Column {
        Text(titulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.SemiBold)
    }
}

/** Barras por dia con linea punteada en la meta; las que la superan se pintan en rojo. */
@Composable
private fun GraficaDias(
    valores: List<Float>,
    limite: Float?,
    color: Color,
    colorLimite: Color
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (valores.isEmpty()) return@Canvas
        val tope = maxOf(valores.max(), limite ?: 0f, 1f) * 1.1f
        val ancho = size.width / valores.size
        val barra = ancho * 0.6f
        valores.forEachIndexed { i, v ->
            val alto = v / tope * size.height
            val excede = limite != null && v > limite
            drawRoundRect(
                color = if (excede) colorLimite else color,
                topLeft = Offset(i * ancho + (ancho - barra) / 2f, size.height - alto),
                size = Size(barra, alto),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
        if (limite != null && limite > 0f) {
            val y = size.height - limite / tope * size.height
            drawLine(
                color = colorLimite,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        }
    }
}

@Composable
private fun TarjetaPeso(
    pesos: List<PesoRegistro>,
    onAgregar: (Float) -> Unit,
    onEliminar: (PesoRegistro) -> Unit
) {
    var entrada by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Peso corporal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = entrada,
                    onValueChange = { entrada = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Peso de hoy (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    entrada.replace(',', '.').toFloatOrNull()?.let {
                        onAgregar(it)
                        entrada = ""
                    }
                }) { Text("Guardar") }
            }

            if (pesos.isEmpty()) {
                Text(
                    "Registra tu peso para ver la tendencia.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                return@Column
            }

            val ultimo = pesos.last()
            val haceUnaSemana = ultimo.timestamp - 7L * 24 * 60 * 60 * 1000
            val referencia = pesos.lastOrNull { it.timestamp <= haceUnaSemana }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Dato("Actual", "${formatoKg(ultimo.kg)} kg")
                if (referencia != null) {
                    Dato("vs. hace 7 días", formatoCambio(ultimo.kg - referencia.kg))
                }
                if (pesos.size > 1) {
                    Dato("vs. primer registro", formatoCambio(ultimo.kg - pesos.first().kg))
                }
            }

            if (pesos.size >= 2) {
                Spacer(Modifier.height(10.dp))
                GraficaPeso(pesos.takeLast(60), MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(8.dp))
            pesos.takeLast(5).reversed().forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${formatoKg(p.kg)} kg", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        fechaHora(p.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { onEliminar(p) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar peso", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** Linea de tendencia del peso, con el eje X proporcional al tiempo real entre mediciones. */
@Composable
private fun GraficaPeso(pesos: List<PesoRegistro>, color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val minKg = pesos.minOf { it.kg }
        val maxKg = pesos.maxOf { it.kg }
        val margen = maxOf((maxKg - minKg) * 0.15f, 0.5f)
        val bajo = minKg - margen
        val rango = (maxKg + margen) - bajo
        val t0 = pesos.first().timestamp
        val duracion = maxOf(pesos.last().timestamp - t0, 1L)
        val pad = 6.dp.toPx()

        fun punto(p: PesoRegistro) = Offset(
            pad + (p.timestamp - t0).toFloat() / duracion * (size.width - 2 * pad),
            size.height - pad - (p.kg - bajo) / rango * (size.height - 2 * pad)
        )

        val ruta = Path()
        pesos.forEachIndexed { i, p ->
            val o = punto(p)
            if (i == 0) ruta.moveTo(o.x, o.y) else ruta.lineTo(o.x, o.y)
        }
        drawPath(ruta, color, style = Stroke(width = 3.dp.toPx()))
        pesos.forEach { drawCircle(color, radius = 4.dp.toPx(), center = punto(it)) }
    }
}

private fun formatoKg(kg: Float) = String.format(Locale.getDefault(), "%.1f", kg)

private fun formatoCambio(delta: Float): String {
    val signo = if (delta > 0.05f) "+" else ""
    return "$signo${String.format(Locale.getDefault(), "%.1f", delta)} kg"
}
