package com.mauricior8.calorias.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.ui.unit.sp
import com.mauricior8.calorias.data.local.entity.Suplemento
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.model.TotalComida
import com.mauricior8.calorias.ui.state.MetricaConItem
import com.mauricior8.calorias.ui.theme.VerdeCompletado
import com.mauricior8.calorias.util.Clave
import com.mauricior8.calorias.util.Comidas
import com.mauricior8.calorias.util.claveMetrica
import com.mauricior8.calorias.util.colorDesdeHex
import com.mauricior8.calorias.util.formatoValor

/** Resumen del entrenamiento de hoy para la tarjeta de "Hoy". */
data class ResumenGymHoy(
    val titulo: String,
    val color: Color?,
    val ejercicios: Int,
    val hechas: Int,
    val totales: Int,
    val descanso: Boolean
)

/**
 * Pantalla "Hoy": tira de dias, resumen con anillo de calorias y macros, entrenamiento,
 * comidas, agua rapida, suplementos y las tarjetas de cada metrica.
 */
@Composable
fun MetricasScreen(
    metricas: List<MetricaConItem>,
    isLoading: Boolean,
    diaDescanso: Boolean,
    diasSemana: List<String>,
    indiceDia: Int,
    etiquetaDia: String,
    coloresDias: List<Color?>,
    diaCompletado: Boolean,
    fechaMillis: Long,
    racha: Int,
    onSeleccionarDia: (Int) -> Unit,
    onFechaManual: (Long) -> Unit,
    onToggleCompletado: (Boolean) -> Unit,
    onLimpiarDia: () -> Unit,
    onCopiarDiaAnterior: () -> Unit,
    onVerDetalle: () -> Unit,
    onAgregar: (metricaId: String, valor: Float) -> Unit,
    onAgregarAlimento: () -> Unit,
    onAgregarAgua: () -> Unit,
    onVerHistorial: (MetricaConfig) -> Unit,
    onEditar: (MetricaConfig) -> Unit,
    onEliminar: (MetricaConfig) -> Unit,
    onLimpiarMetrica: (String) -> Unit,
    onMoverArriba: (String) -> Unit,
    onMoverAbajo: (String) -> Unit,
    totalesPorComida: List<TotalComida>,
    gymHoy: ResumenGymHoy?,
    onAbrirGym: () -> Unit,
    suplementos: List<Suplemento>,
    suplementosTomados: Set<Int>,
    onSuplemento: (Int, Boolean) -> Unit,
    onGestionarSuplementos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                item(key = "cabecera_dia") {
                    CabeceraDia(
                        diasSemana, indiceDia, etiquetaDia, coloresDias, diaCompletado, fechaMillis,
                        onSeleccionarDia, onFechaManual, onToggleCompletado,
                        onLimpiarDia, onCopiarDiaAnterior, onVerDetalle
                    )
                }
                if (metricas.any { claveMetrica(it.config.nombre) == Clave.CALORIAS }) {
                    item(key = "hero") { HeroResumen(metricas, diaDescanso, racha) }
                }
                if (gymHoy != null) {
                    item(key = "gym_hoy") { TarjetaGymHoy(gymHoy, onAbrirGym) }
                }
                if (totalesPorComida.isNotEmpty()) {
                    item(key = "por_comida") { ResumenComidas(metricas = metricas, totales = totalesPorComida) }
                }
                item(key = "btn_alimento") {
                    Button(onClick = onAgregarAlimento, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar alimento")
                    }
                }
                metricas.firstOrNull { claveMetrica(it.config.nombre) == Clave.AGUA }?.let { agua ->
                    item(key = "agua") { TarjetaAgua(agua, onAgregarAgua) }
                }
                if (suplementos.isNotEmpty()) {
                    item(key = "suplementos") {
                        TarjetaSuplementos(suplementos, suplementosTomados, onSuplemento, onGestionarSuplementos)
                    }
                }
                if (metricas.isEmpty()) {
                    item(key = "vacio") {
                        Text(
                            "No hay métricas. Pulsa + para crear una.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
                items(metricas, key = { it.config.id }) { item ->
                    MetricaCard(
                        item = item,
                        esPrimero = item == metricas.first(),
                        esUltimo = item == metricas.last(),
                        onAgregar = { valor -> onAgregar(item.config.id, valor) },
                        onVerHistorial = { onVerHistorial(item.config) },
                        onEditar = { onEditar(item.config) },
                        onEliminar = { onEliminar(item.config) },
                        onLimpiar = { onLimpiarMetrica(item.config.id) },
                        onMoverArriba = { onMoverArriba(item.config.id) },
                        onMoverAbajo = { onMoverAbajo(item.config.id) }
                    )
                }
            }
        }
    }
}

/** Tira Lun-Dom + fecha, "dia completado" y menu de acciones del dia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CabeceraDia(
    diasSemana: List<String>,
    indiceDia: Int,
    etiquetaDia: String,
    coloresDias: List<Color?>,
    diaCompletado: Boolean,
    fechaMillis: Long,
    onSeleccionarDia: (Int) -> Unit,
    onFechaManual: (Long) -> Unit,
    onToggleCompletado: (Boolean) -> Unit,
    onLimpiarDia: () -> Unit,
    onCopiarDiaAnterior: () -> Unit,
    onVerDetalle: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TiraSemana(diasSemana.map { it.take(3) }, indiceDia, coloresDias, onSeleccionarDia)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(etiquetaDia, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { mostrarDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha exacta", tint = MaterialTheme.colorScheme.primary)
            }
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Acciones del día")
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Ver detalle del día") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                        onClick = { menuAbierto = false; onVerDetalle() }
                    )
                    DropdownMenuItem(
                        text = { Text("Copiar día anterior") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = { menuAbierto = false; onCopiarDiaAnterior() }
                    )
                    DropdownMenuItem(
                        text = { Text("Limpiar día") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = { menuAbierto = false; onLimpiarDia() }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = diaCompletado,
                onCheckedChange = onToggleCompletado,
                colors = CheckboxDefaults.colors(checkedColor = VerdeCompletado)
            )
            Text(
                "Día completado (calorías y macros)",
                color = if (diaCompletado) VerdeCompletado else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (diaCompletado) FontWeight.Bold else FontWeight.Normal
            )
        }
    }

    if (mostrarDatePicker) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fechaMillis)
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let(onFechaManual)
                    mostrarDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
}

/** Anillo grande de calorias con lo que queda y barras de macros. */
@Composable
private fun HeroResumen(metricas: List<MetricaConItem>, diaDescanso: Boolean, racha: Int) {
    val cal = metricas.firstOrNull { claveMetrica(it.config.nombre) == Clave.CALORIAS } ?: return
    val color = colorDesdeHex(cal.config.colorHex)
    val limite = cal.limite
    val restante = limite?.let { it - cal.totalHoy }

    Bloque {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnilloGrande(cal.progreso, color, Modifier.size(132.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatoValor(cal.totalHoy), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(
                        limite?.let { "de ${formatoValor(it)} ${cal.config.unidad}" } ?: cal.config.unidad,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (restante != null) {
                    Text(
                        if (restante >= 0) "Te quedan ${formatoValor(restante)} ${cal.config.unidad}"
                        else "Te pasaste ${formatoValor(-restante)} ${cal.config.unidad}",
                        fontWeight = FontWeight.SemiBold,
                        color = if (restante < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (diaDescanso) Insignia("Día de descanso", MaterialTheme.colorScheme.secondary)
                    if (racha > 0) Insignia("Racha: $racha", VerdeCompletado)
                }
                listOf(Clave.PROTEINAS, Clave.CARBOHIDRATOS, Clave.GRASAS).forEach { clave ->
                    val m = metricas.firstOrNull { claveMetrica(it.config.nombre) == clave } ?: return@forEach
                    val c = colorDesdeHex(m.config.colorHex)
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(m.config.nombre, style = MaterialTheme.typography.labelMedium, color = c, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatoValor(m.totalHoy) + (m.limite?.let { "/${formatoValor(it)}" } ?: "") + " ${m.config.unidad}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        BarraProgreso(m.progreso, if (m.limite != null && m.totalHoy > m.limite!!) MaterialTheme.colorScheme.error else c, alto = 6)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaGymHoy(r: ResumenGymHoy, onAbrir: () -> Unit) {
    Bloque(titulo = "Entrenamiento de hoy", acento = r.color) {
        if (r.descanso) {
            Text("Hoy es día de descanso.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(r.titulo, fontWeight = FontWeight.SemiBold)
            Text(
                "${r.ejercicios} ejercicios · ${r.hechas} de ${r.totales} series",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            BarraProgreso(if (r.totales == 0) 0f else r.hechas / r.totales.toFloat(), r.color ?: MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onAbrir) { Text(if (r.hechas == 0) "Empezar" else "Continuar") }
        }
    }
}

@Composable
private fun TarjetaAgua(agua: MetricaConItem, onAgregarAgua: () -> Unit) {
    val color = colorDesdeHex(agua.config.colorHex)
    Bloque(titulo = agua.config.nombre, acento = color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatoValor(agua.totalHoy) + (agua.limite?.let { " / ${formatoValor(it)}" } ?: "") + " ${agua.config.unidad}",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                BarraProgreso(agua.progreso, color)
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onAgregarAgua) { Text("+250 ml") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaSuplementos(
    suplementos: List<Suplemento>,
    tomados: Set<Int>,
    onSuplemento: (Int, Boolean) -> Unit,
    onGestionar: () -> Unit
) {
    Bloque(titulo = "Suplementos (${tomados.size}/${suplementos.size})") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            suplementos.forEach { s ->
                FilterChip(
                    selected = s.id in tomados,
                    onClick = { onSuplemento(s.id, s.id !in tomados) },
                    label = { Text(s.nombre) }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onGestionar) { Text("Editar lista") }
        }
    }
}

/** Calorias y proteina del dia desglosadas por comida (solo alimentos con comida asignada). */
@Composable
private fun ResumenComidas(metricas: List<MetricaConItem>, totales: List<TotalComida>) {
    val calorias = metricas.firstOrNull { claveMetrica(it.config.nombre) == Clave.CALORIAS }?.config
    val proteinas = metricas.firstOrNull { claveMetrica(it.config.nombre) == Clave.PROTEINAS }?.config
    val filas = Comidas.LISTA.mapNotNull { comida ->
        val deLaComida = totales.filter { it.comida == comida }
        if (deLaComida.isEmpty()) return@mapNotNull null
        fun total(m: MetricaConfig?) = m?.let { cfg ->
            deLaComida.firstOrNull { it.metricaId == cfg.id }?.let { "${formatoValor(it.total)} ${cfg.unidad}" }
        }
        comida to listOfNotNull(total(calorias), total(proteinas)?.let { "Prot. $it" }).joinToString(" · ")
    }
    if (filas.isEmpty()) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Por comida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            filas.forEach { (comida, resumen) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(comida, fontWeight = FontWeight.SemiBold)
                    Text(resumen, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetricaCard(
    item: MetricaConItem,
    esPrimero: Boolean,
    esUltimo: Boolean,
    onAgregar: (Float) -> Unit,
    onVerHistorial: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onLimpiar: () -> Unit,
    onMoverArriba: () -> Unit,
    onMoverAbajo: () -> Unit
) {
    var entrada by remember { mutableStateOf("") }
    var menuAbierto by remember { mutableStateOf(false) }
    val color = colorDesdeHex(item.config.colorHex)
    val progresoAnim by animateFloatAsState(targetValue = item.progreso, label = "progreso")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mini-grafica segun el tipo elegido (Anillo, Pastel o Barras).
                MiniGrafica(
                    tipo = item.config.tipoGrafica,
                    progreso = progresoAnim,
                    tieneLimite = item.limite != null,
                    color = color
                )

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.config.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val limiteTxt = item.limite?.let { " / ${formatoValor(it)}" } ?: ""
                    Text(
                        text = "${formatoValor(item.totalHoy)}$limiteTxt ${item.config.unidad}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Boton historial.
                IconButton(onClick = onVerHistorial) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Ver historial",
                        tint = color
                    )
                }

                // Menu de acciones.
                Box {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuAbierto = false; onEditar() }
                        )
                        DropdownMenuItem(
                            text = { Text("Subir") },
                            enabled = !esPrimero,
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) },
                            onClick = { menuAbierto = false; onMoverArriba() }
                        )
                        DropdownMenuItem(
                            text = { Text("Bajar") },
                            enabled = !esUltimo,
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                            onClick = { menuAbierto = false; onMoverAbajo() }
                        )
                        DropdownMenuItem(
                            text = { Text("Limpiar datos de hoy") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            onClick = { menuAbierto = false; onLimpiar() }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuAbierto = false; onEliminar() }
                        )
                    }
                }
            }

            if (item.limite != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color.copy(alpha = 0.18f))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(progresoAnim)
                            .fillMaxSize()
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = entrada,
                    onValueChange = { entrada = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Sumar ${item.config.unidad}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = {
                        entrada.toFloatOrNull()?.let {
                            onAgregar(it)
                            entrada = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }
    }
}


/**
 * Mini-grafica de progreso para la tarjeta de metrica, segun el tipo elegido.
 */
@Composable
private fun MiniGrafica(
    tipo: String,
    progreso: Float,
    tieneLimite: Boolean,
    color: Color
) {
    val porcentaje = (progreso * 100).toInt()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        when (tipo) {
            "Pastel" -> {
                Canvas(modifier = Modifier.size(56.dp)) {
                    // Fondo del pastel.
                    drawArc(
                        color = color.copy(alpha = 0.18f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height)
                    )
                    // Porcion segun progreso.
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = (if (tieneLimite) progreso else 1f) * 360f,
                        useCenter = true,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height)
                    )
                }
            }
            "Barras" -> {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val anchoBarra = size.width * 0.45f
                    val x = (size.width - anchoBarra) / 2f
                    // Fondo.
                    drawRect(
                        color = color.copy(alpha = 0.18f),
                        topLeft = Offset(x, 0f),
                        size = Size(anchoBarra, size.height)
                    )
                    // Relleno segun progreso.
                    val altura = (if (tieneLimite) progreso else 1f) * size.height
                    drawRect(
                        color = color,
                        topLeft = Offset(x, size.height - altura),
                        size = Size(anchoBarra, altura)
                    )
                }
            }
            else -> {
                // Anillo (por defecto).
                CircularProgressIndicator(
                    progress = { if (tieneLimite) progreso else 1f },
                    modifier = Modifier.size(56.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.18f),
                    strokeWidth = 6.dp
                )
            }
        }
        if (tieneLimite) {
            Text(
                "$porcentaje%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
