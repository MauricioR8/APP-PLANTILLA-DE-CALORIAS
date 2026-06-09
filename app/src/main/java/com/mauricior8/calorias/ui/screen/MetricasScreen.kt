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
import com.mauricior8.calorias.ui.state.MetricaConItem
import com.mauricior8.calorias.ui.theme.VerdeCompletado
import com.mauricior8.calorias.util.colorDesdeHex
import com.mauricior8.calorias.util.formatoValor

/**
 * Lista de metricas con tarjetas rediseñadas. Permite sumar valores, ver
 * historial, editar, eliminar y reordenar cada metrica.
 */
@Composable
fun MetricasScreen(
    metricas: List<MetricaConItem>,
    isLoading: Boolean,
    diasSemana: List<String>,
    indiceDia: Int,
    etiquetaDia: String,
    diaCompletado: Boolean,
    fechaMillis: Long,
    onSeleccionarDia: (Int) -> Unit,
    onFechaManual: (Long) -> Unit,
    onToggleCompletado: (Boolean) -> Unit,
    onLimpiarDia: () -> Unit,
    onAgregar: (metricaId: String, valor: Float) -> Unit,
    onAgregarAlimento: () -> Unit,
    onVerHistorial: (MetricaConfig) -> Unit,
    onEditar: (MetricaConfig) -> Unit,
    onEliminar: (MetricaConfig) -> Unit,
    onLimpiarMetrica: (String) -> Unit,
    onMoverArriba: (String) -> Unit,
    onMoverAbajo: (String) -> Unit,
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
                item(key = "selector_dia") {
                    SelectorDia(
                        diasSemana = diasSemana,
                        indiceDia = indiceDia,
                        etiquetaDia = etiquetaDia,
                        diaCompletado = diaCompletado,
                        fechaMillis = fechaMillis,
                        onSeleccionarDia = onSeleccionarDia,
                        onFechaManual = onFechaManual,
                        onToggleCompletado = onToggleCompletado,
                        onLimpiarDia = onLimpiarDia
                    )
                }
                if (metricas.isNotEmpty()) {
                    item(key = "metas") { MetasPanel(metricas = metricas) }
                }
                item(key = "btn_alimento") {
                    Button(
                        onClick = onAgregarAlimento,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar alimento")
                    }
                }
                if (metricas.isEmpty()) {
                    item(key = "vacio") {
                        Text(
                            "No hay metricas. Pulsa + para crear una.",
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

/**
 * Selector del dia de la semana (Lunes-Domingo), con marca de "dia completado"
 * y boton para limpiar/reiniciar los datos del dia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDia(
    diasSemana: List<String>,
    indiceDia: Int,
    etiquetaDia: String,
    diaCompletado: Boolean,
    fechaMillis: Long,
    onSeleccionarDia: (Int) -> Unit,
    onFechaManual: (Long) -> Unit,
    onToggleCompletado: (Boolean) -> Unit,
    onLimpiarDia: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { menuAbierto = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(etiquetaDia, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir dia")
                    }
                    DropdownMenu(
                        expanded = menuAbierto,
                        onDismissRequest = { menuAbierto = false }
                    ) {
                        diasSemana.forEachIndexed { i, dia ->
                            DropdownMenuItem(
                                text = { Text(dia) },
                                onClick = { menuAbierto = false; onSeleccionarDia(i) }
                            )
                        }
                    }
                }
                IconButton(onClick = { mostrarDatePicker = true }) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Elegir fecha exacta",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onLimpiarDia) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Limpiar dia",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = diaCompletado,
                    onCheckedChange = onToggleCompletado,
                    colors = CheckboxDefaults.colors(checkedColor = VerdeCompletado)
                )
                Text(
                    "Dia completado (calorias y macros)",
                    color = if (diaCompletado) VerdeCompletado else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (diaCompletado) FontWeight.Bold else FontWeight.Normal
                )
            }
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
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}

/**
 * Panel superior con las metas/limites del dia. Cada atributo se muestra con su
 * propio color (el de la metrica) y una barra de progreso; avisa cuando se pasa.
 */
@Composable
private fun MetasPanel(metricas: List<MetricaConItem>) {
    val conLimite = metricas.filter { it.config.limiteMaximo != null }
    if (conLimite.isEmpty()) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Metas del dia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            conLimite.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                val color = colorDesdeHex(item.config.colorHex)
                val limite = item.config.limiteMaximo ?: 0f
                val excedido = item.totalHoy > limite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.config.nombre,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                    Text(
                        text = "${formatoValor(item.totalHoy)} / ${formatoValor(limite)} ${item.config.unidad}" +
                            if (excedido) "  ¡Excedido!" else "",
                        fontWeight = if (excedido) FontWeight.Bold else FontWeight.Normal,
                        color = if (excedido) MaterialTheme.colorScheme.error else color
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color.copy(alpha = 0.18f))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(item.progreso)
                            .fillMaxSize()
                            .background(if (excedido) MaterialTheme.colorScheme.error else color)
                    )
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
                    tieneLimite = item.config.limiteMaximo != null,
                    color = color
                )

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.config.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val limiteTxt = item.config.limiteMaximo?.let { " / ${formatoValor(it)}" } ?: ""
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
                        Icon(Icons.Default.MoreVert, contentDescription = "Mas opciones")
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

            if (item.config.limiteMaximo != null) {
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
