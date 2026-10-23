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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.ui.state.Progreso
import com.mauricior8.calorias.ui.theme.VerdeCompletado
import com.mauricior8.calorias.util.Clave
import com.mauricior8.calorias.util.Cuerpo
import com.mauricior8.calorias.util.Gym
import com.mauricior8.calorias.util.ZonaVolumen
import com.mauricior8.calorias.util.claveMetrica
import com.mauricior8.calorias.util.colorDesdeHex
import com.mauricior8.calorias.util.fechaHora
import com.mauricior8.calorias.util.formatoValor
import java.util.Locale

private val TIPOS_MEDIDA = listOf("Cintura", "Pecho", "Brazo", "Pierna", "Cadera")

/** Pestana Progreso: Nutricion, Cuerpo y Fuerza. */
@Composable
fun ProgresoScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pestana) {
            listOf("Nutrición", "Cuerpo", "Fuerza").forEachIndexed { i, t ->
                Tab(selected = pestana == i, onClick = { pestana = i }, text = { Text(t) })
            }
        }
        when (pestana) {
            0 -> PestanaNutricion(viewModel)
            1 -> PestanaCuerpo(viewModel)
            else -> PestanaFuerza(viewModel)
        }
    }
}

private val Lista = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)

// ---------------------------------------------------------------------------------------
// Nutricion
// ---------------------------------------------------------------------------------------

@Composable
private fun PestanaNutricion(vm: MainViewModel) {
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val progreso by vm.progreso.collectAsStateWithLifecycle()
    val rango by vm.rangoDias.collectAsStateWithLifecycle()
    val racha by vm.racha.collectAsStateWithLifecycle()
    val gasto by vm.gastoReal.collectAsStateWithLifecycle()
    val perfil by vm.perfilBase.collectAsStateWithLifecycle()
    val metricas = estado.metricas.map { it.config }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = Lista) {
        item("resumen") { ResumenSemanal(metricas, progreso, racha) }
        item("consumo") { TarjetaConsumo(metricas, progreso, rango, vm::setRangoProgreso) }
        item("gasto") {
            Bloque(titulo = "Gasto energético real") {
                val g = gasto
                if (g == null) {
                    Text(
                        "Con 10 o más días de calorías y registros de peso en 2 semanas, calculo cuánto gastas " +
                            "realmente (comparando lo que comes con cómo cambia tu peso).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Dato("Comes (promedio)", "${g.kcalPromedio} kcal")
                        Dato("Cambio de peso", signo(g.kgPorSemana) + " kg/sem")
                        Dato("Gastas (estimado)", "${g.gasto} kcal")
                    }
                    val ajuste = perfil?.meta?.ajuste ?: 0.0
                    val sugerido = (g.gasto * (1 + ajuste)).toInt()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Meta sugerida${perfil?.meta?.let { " (${it.etiqueta})" } ?: ""}: $sugerido kcal",
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalButton(onClick = { vm.aplicarCaloriasSugeridas(sugerido) }, modifier = Modifier.padding(top = 6.dp)) {
                        Text("Usar como meta de calorías")
                    }
                    Text(
                        "Basado en ${g.diasConDatos} días con datos. Es una estimación: pesa siempre en condiciones parecidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumenSemanal(metricas: List<MetricaConfig>, progreso: Progreso, racha: Int) {
    Bloque(titulo = "Resumen de ${progreso.dias} días") {
        val cal = metricas.firstOrNull { claveMetrica(it.nombre) == Clave.CALORIAS }
        val dias = (progreso.totales[cal?.id] ?: emptyList()).count { it > 0f }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Dato("Racha de días completos", "$racha ${if (racha == 1) "día" else "días"}")
            Dato("Días con registro", "$dias de ${progreso.dias}")
        }
        listOf(Clave.CALORIAS, Clave.PROTEINAS, Clave.CARBOHIDRATOS, Clave.GRASAS).forEach { clave ->
            val m = metricas.firstOrNull { claveMetrica(it.nombre) == clave } ?: return@forEach
            val conDatos = (progreso.totales[m.id] ?: emptyList()).filter { it > 0f }
            if (conDatos.isEmpty()) return@forEach
            val promedio = conDatos.average().toFloat()
            val meta = m.limiteMaximo
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m.nombre, fontWeight = FontWeight.SemiBold, color = colorDesdeHex(m.colorHex))
                Text(
                    "${formatoValor(promedio)}" + (meta?.let { " / ${formatoValor(it)}" } ?: "") + " ${m.unidad}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (meta != null && meta > 0f) BarraProgreso(promedio / meta, colorDesdeHex(m.colorHex))
        }
    }
}

@Composable
private fun TarjetaConsumo(metricas: List<MetricaConfig>, progreso: Progreso, rangoDias: Int, onRango: (Int) -> Unit) {
    var seleccionada by remember { mutableStateOf<String?>(null) }
    val metrica = metricas.firstOrNull { it.id == seleccionada } ?: metricas.firstOrNull()

    Bloque(titulo = "Consumo diario") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7 to "7 días", 30 to "30 días").forEach { (dias, texto) ->
                FilterChip(selected = rangoDias == dias, onClick = { onRango(dias) }, label = { Text(texto) })
            }
        }
        if (metrica == null) {
            Text("Crea una métrica en \"Hoy\" para ver su progreso.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp))
            return@Bloque
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metricas.forEach { m ->
                FilterChip(selected = m.id == metrica.id, onClick = { seleccionada = m.id }, label = { Text(m.nombre) })
            }
        }
        val valores = progreso.totales[metrica.id] ?: List(progreso.etiquetas.size) { 0f }
        GraficaDias(valores, metrica.limiteMaximo, colorDesdeHex(metrica.colorHex), MaterialTheme.colorScheme.error)
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
                "Línea roja = meta de ${formatoValor(limite)} ${metrica.unidad}. Las barras que la superan se marcan en rojo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** Barras por dia con linea punteada en la meta; las que la superan se pintan en rojo. */
@Composable
private fun GraficaDias(valores: List<Float>, limite: Float?, color: Color, colorLimite: Color) {
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        if (valores.isEmpty()) return@Canvas
        val tope = maxOf(valores.max(), limite ?: 0f, 1f) * 1.1f
        val ancho = size.width / valores.size
        val barra = ancho * 0.6f
        valores.forEachIndexed { i, v ->
            val alto = v / tope * size.height
            drawRoundRect(
                color = if (limite != null && v > limite) colorLimite else color,
                topLeft = Offset(i * ancho + (ancho - barra) / 2f, size.height - alto),
                size = Size(barra, alto),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
        if (limite != null && limite > 0f) {
            val y = size.height - limite / tope * size.height
            drawLine(colorLimite, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
        }
    }
}

// ---------------------------------------------------------------------------------------
// Cuerpo
// ---------------------------------------------------------------------------------------

@Composable
private fun PestanaCuerpo(vm: MainViewModel) {
    val pesos by vm.pesos.collectAsStateWithLifecycle()
    val medidas by vm.medidas.collectAsStateWithLifecycle()
    var entradaPeso by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TIPOS_MEDIDA.first()) }
    var entradaMedida by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = Lista) {
        item("peso") {
            Bloque(titulo = "Peso corporal") {
                CampoGuardar("Peso de hoy (kg)", entradaPeso, { entradaPeso = it }) {
                    entradaPeso.replace(',', '.').toFloatOrNull()?.let { vm.agregarPeso(it); entradaPeso = "" }
                }
                if (pesos.isEmpty()) {
                    Text("Registra tu peso para ver la tendencia.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp))
                    return@Bloque
                }
                val puntos = pesos.takeLast(90).map { it.timestamp to it.kg }
                val ultimo = pesos.last()
                val media = Cuerpo.mediaMovil(puntos)
                val ritmo = Cuerpo.ritmoKgPorSemana(pesos.map { it.timestamp to it.kg })
                val hace7 = pesos.lastOrNull { it.timestamp <= ultimo.timestamp - 7L * 24 * 60 * 60 * 1000 }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Dato("Actual", "${kg1(ultimo.kg)} kg")
                    Dato("Media 7 días", "${kg1(media.last())} kg")
                    if (ritmo != null) Dato("Ritmo", signo(ritmo) + " kg/sem")
                    else if (hace7 != null) Dato("vs. 7 días", signo(ultimo.kg - hace7.kg) + " kg")
                }
                if (puntos.size >= 2) {
                    Spacer(Modifier.height(10.dp))
                    GraficaLinea(puntos, MaterialTheme.colorScheme.onSurfaceVariant, segunda = media,
                        colorSegunda = MaterialTheme.colorScheme.primary)
                    Text(
                        "Línea de color = media de 7 días (menos ruido que el peso diario).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                pesos.takeLast(4).reversed().forEach { p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${kg1(p.kg)} kg", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(fechaHora(p.timestamp), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { vm.eliminarPeso(p) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar peso", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item("medidas") {
            Bloque(titulo = "Medidas corporales") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TIPOS_MEDIDA.forEach { t -> FilterChip(selected = tipo == t, onClick = { tipo = t }, label = { Text(t) }) }
                }
                Spacer(Modifier.height(8.dp))
                CampoGuardar("$tipo (cm)", entradaMedida, { entradaMedida = it }) {
                    entradaMedida.replace(',', '.').toFloatOrNull()?.let { vm.agregarMedida(tipo, it); entradaMedida = "" }
                }
                val deTipo = medidas.filter { it.tipo == tipo }
                if (deTipo.isEmpty()) {
                    Text("Aún no hay medidas de $tipo.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp))
                } else {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Dato("Actual", "${kg1(deTipo.last().cm)} cm")
                        if (deTipo.size > 1) Dato("Desde el inicio", signo(deTipo.last().cm - deTipo.first().cm) + " cm")
                    }
                    if (deTipo.size >= 2) {
                        Spacer(Modifier.height(8.dp))
                        GraficaLinea(deTipo.map { it.timestamp to it.cm }, MaterialTheme.colorScheme.secondary)
                    }
                    deTipo.takeLast(3).reversed().forEach { m ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${kg1(m.cm)} cm", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(fechaHora(m.timestamp), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { vm.eliminarMedida(m) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar medida", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoGuardar(etiqueta: String, valor: String, onCambio: (String) -> Unit, onGuardar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = valor,
            onValueChange = { onCambio(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
            label = { Text(etiqueta) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onGuardar) { Text("Guardar") }
    }
}

// ---------------------------------------------------------------------------------------
// Fuerza
// ---------------------------------------------------------------------------------------

@Composable
private fun PestanaFuerza(vm: MainViewModel) {
    val semana by vm.semanaGym.collectAsStateWithLifecycle()
    val ejercicios by vm.ejercicios.collectAsStateWithLifecycle()
    val pesos by vm.pesosEjercicio.collectAsStateWithLifecycle()
    val sugerencias by vm.sugerenciasDePeso.collectAsStateWithLifecycle()
    var elegido by remember { mutableIntStateOf(0) }

    val conHistorial = ejercicios.filter { e -> pesos.count { it.ejercicioId == e.id } >= 1 && e.pesoKg != null }
    val ejercicio = conHistorial.firstOrNull { it.id == elegido } ?: conHistorial.firstOrNull()

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = Lista) {
        item("adherencia") {
            Bloque(titulo = "Cumplimiento de la semana") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Dato("Sesiones completas", "${semana.completas} de ${semana.planeadas}")
                    Dato("Series de fuerza", "${semana.volumen.values.sum()}")
                }
                Spacer(Modifier.height(8.dp))
                BarraProgreso(
                    if (semana.planeadas == 0) 0f else semana.completas / semana.planeadas.toFloat(),
                    MaterialTheme.colorScheme.primary
                )
            }
        }
        item("volumen") {
            Bloque(titulo = "Series por grupo muscular (semana)") {
                val filas = Gym.GRUPOS_FUERZA.map { g ->
                    val n = semana.volumen[g] ?: 0
                    val color = when (Gym.zona(n)) {
                        ZonaVolumen.OPTIMO -> VerdeCompletado
                        ZonaVolumen.ALTO -> MaterialTheme.colorScheme.error
                        ZonaVolumen.BAJO -> MaterialTheme.colorScheme.tertiary
                    }
                    Triple(g, n, if (n == 0) MaterialTheme.colorScheme.surfaceVariant else color)
                }
                BarrasHorizontales(filas, minimo = Gym.SERIES_MIN, maximo = Gym.SERIES_MAX)
                Text(
                    "Banda = ${Gym.SERIES_MIN}-${Gym.SERIES_MAX} series por semana, rango habitual para hipertrofia. " +
                        "Ámbar: por debajo, verde: óptimo, rojo: por encima.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        if (sugerencias.isNotEmpty()) {
            item("sugerencias") {
                Bloque(titulo = "Toca subir de peso", acento = MaterialTheme.colorScheme.tertiary) {
                    ejercicios.filter { it.id in sugerencias }.forEach { e ->
                        val nuevo = sugerencias.getValue(e.id)
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.nombre, fontWeight = FontWeight.SemiBold)
                                Text("${e.pesoKg?.let { Gym.kg(it) }} → ${Gym.kg(nuevo)} kg",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalButton(onClick = { vm.subirPeso(e, nuevo) }) { Text("Aplicar") }
                        }
                    }
                }
            }
        }
        item("pesos") {
            Bloque(titulo = "Progreso de cargas") {
                if (ejercicio == null) {
                    Text("Aún no hay pesos de trabajo registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    return@Bloque
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    conHistorial.forEach { e ->
                        FilterChip(selected = e.id == ejercicio.id, onClick = { elegido = e.id }, label = { Text(e.nombre) })
                    }
                }
                val historial = pesos.filter { it.ejercicioId == ejercicio.id }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Dato("Actual", "${Gym.kg(historial.last().kg)} kg")
                    Dato("Récord", "${Gym.kg(historial.maxOf { it.kg })} kg")
                    Dato("Desde el inicio", signo(historial.last().kg - historial.first().kg) + " kg")
                }
                if (historial.size >= 2) {
                    Spacer(Modifier.height(8.dp))
                    GraficaLinea(historial.map { it.timestamp to it.kg }, MaterialTheme.colorScheme.primary)
                } else {
                    Text("Cuando subas de carga se dibujará la gráfica.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

private fun kg1(v: Float) = String.format(Locale.getDefault(), "%.1f", v)

private fun signo(delta: Float): String {
    val s = if (delta > 0.05f) "+" else ""
    return s + String.format(Locale.getDefault(), "%.1f", delta)
}
