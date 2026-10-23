package com.mauricior8.calorias.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.PesoEjercicio
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.util.Gym
import com.mauricior8.calorias.util.fechaHora
import kotlinx.coroutines.delay

/**
 * Pestana Gym: sesion del dia (bloque diario + rutina que toca), con tres vistas:
 * hoy, lista de rutinas y editor de una rutina.
 */
@Composable
fun GymScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var vista by rememberSaveable { mutableStateOf("hoy") }
    var rutinaEditId by rememberSaveable { mutableIntStateOf(0) } // 0 = rutina nueva

    BackHandler(enabled = vista != "hoy") { vista = if (vista == "editor") "rutinas" else "hoy" }

    when (vista) {
        "rutinas" -> RutinasView(
            viewModel = viewModel,
            onAbrir = { rutinaEditId = it; vista = "editor" },
            onVolver = { vista = "hoy" },
            modifier = modifier
        )
        "editor" -> RutinaEditorView(
            viewModel = viewModel,
            rutinaId = rutinaEditId,
            onVolver = { vista = "rutinas" },
            modifier = modifier
        )
        else -> GymHoy(viewModel, onRutinas = { vista = "rutinas" }, modifier = modifier)
    }
}

@Composable
private fun GymHoy(vm: MainViewModel, onRutinas: () -> Unit, modifier: Modifier) {
    val rutinasDelDia by vm.rutinasDelDia.collectAsStateWithLifecycle()
    val todas by vm.rutinas.collectAsStateWithLifecycle()
    val series by vm.seriesDelDia.collectAsStateWithLifecycle()
    val semana by vm.semanaGym.collectAsStateWithLifecycle()
    val sugerencias by vm.sugerenciasDePeso.collectAsStateWithLifecycle()
    val pesos by vm.pesosEjercicio.collectAsStateWithLifecycle()
    val sesion by vm.sesionDelDia.collectAsStateWithLifecycle()
    val indice by vm.indiceDiaSeleccionado.collectAsStateWithLifecycle()
    val etiqueta by vm.etiquetaDiaSeleccionado.collectAsStateWithLifecycle()

    var descanso by remember { mutableStateOf<Int?>(null) }
    var claveDescanso by remember { mutableIntStateOf(0) }
    var editarPeso by remember { mutableStateOf<EjercicioRutina?>(null) }
    var verHistorial by remember { mutableStateOf<EjercicioRutina?>(null) }
    var cronometro by remember { mutableStateOf<EjercicioRutina?>(null) }
    var confirmarReinicio by remember { mutableStateOf(false) }
    var notaSesion by remember(sesion?.nota) { mutableStateOf(sesion?.nota ?: "") }

    val hechasPorEjercicio = remember(series) {
        series.groupBy({ it.ejercicioId }, { it.indice }).mapValues { it.value.toSet() }
    }
    val coloresDia = remember(todas) {
        (1..7).map { iso ->
            todas.firstOrNull { !it.diaria && Gym.aplicaEn(it, iso) }?.let { colorRutina(it.colorHex) }
        }
    }
    val hayFuerza = rutinasDelDia.any { !it.rutina.diaria }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = if (descanso != null) 120.dp else 96.dp)
        ) {
            item(key = "dias") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TiraSemana(
                        etiquetas = vm.diasSemana.map { it.take(3) },
                        seleccionado = indice,
                        colores = coloresDia,
                        onElegir = vm::seleccionarDia
                    )
                    Text(etiqueta, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            item(key = "semana") {
                Bloque(titulo = "Esta semana") {
                    val plan = semana.planeadas
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Dato("Sesiones completas", "${semana.completas} de $plan")
                        Dato("Series de fuerza", "${semana.volumen.values.sum()}")
                    }
                    Spacer(Modifier.height(8.dp))
                    BarraProgreso(if (plan == 0) 0f else semana.completas / plan.toFloat(), MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onRutinas) { Text("Editar rutinas") }
                    }
                }
            }

            if (!hayFuerza) {
                item(key = "descanso") {
                    Bloque(titulo = "Día de descanso") {
                        Text(
                            "Hoy no toca rutina de fuerza. Aprovecha para el bloque diario y recuperar.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            rutinasDelDia.forEach { rd ->
                item(key = "rutina_${rd.rutina.id}") {
                    val color = colorRutina(rd.rutina.colorHex)
                    Bloque(titulo = rd.rutina.nombre, acento = color) {
                        if (!rd.rutina.nota.isNullOrBlank()) {
                            Text(
                                rd.rutina.nota,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        if (rd.ejercicios.isEmpty()) {
                            Text("Sin ejercicios. Agrégalos en \"Editar rutinas\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        rd.ejercicios.forEachIndexed { i, ej ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            FilaEjercicio(
                                ej = ej,
                                hechas = hechasPorEjercicio[ej.id] ?: emptySet(),
                                color = color,
                                sugerido = sugerencias[ej.id],
                                onSerie = { idx, hecha ->
                                    vm.marcarSerie(ej.id, idx, hecha)
                                    if (hecha && !ej.esTiempo && ej.grupo !in Gym.CATEGORIAS_NO_FUERZA) {
                                        descanso = ej.descansoSeg
                                        claveDescanso++
                                    }
                                },
                                onPeso = { editarPeso = ej },
                                onHistorial = { verHistorial = ej },
                                onCronometro = { cronometro = ej }
                            )
                        }
                    }
                }
            }

            item(key = "nota_sesion") {
                Bloque(titulo = "Notas de la sesión") {
                    OutlinedTextField(
                        value = notaSesion,
                        onValueChange = { notaSesion = it },
                        label = { Text("Molestias, sensaciones, cambios…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { confirmarReinicio = true }) {
                            Text("Reiniciar series del día", color = MaterialTheme.colorScheme.error)
                        }
                        FilledTonalButton(onClick = { vm.guardarNotaSesion(notaSesion) }) { Text("Guardar nota") }
                    }
                }
            }
        }

        descanso?.let { s ->
            BarraDescanso(
                segundos = s,
                clave = claveDescanso,
                onCerrar = { descanso = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }

    editarPeso?.let { ej ->
        SubirPesoDialog(
            ejercicio = ej,
            onConfirmar = { kg -> vm.subirPeso(ej, kg); editarPeso = null },
            onCancelar = { editarPeso = null }
        )
    }
    verHistorial?.let { ej ->
        HistorialPesoDialog(ej, pesos.filter { it.ejercicioId == ej.id }) { verHistorial = null }
    }
    cronometro?.let { ej ->
        IntervaloDialog(
            ejercicio = ej,
            onTerminado = {
                (0 until ej.series).forEach { vm.marcarSerie(ej.id, it, true) }
                cronometro = null
            },
            onCerrar = { cronometro = null }
        )
    }
    if (confirmarReinicio) {
        ConfirmarDialog(
            titulo = "Reiniciar series",
            mensaje = "Se desmarcarán todas las series de $etiqueta.",
            textoConfirmar = "Reiniciar",
            onConfirmar = { vm.limpiarSeriesDelDia(); confirmarReinicio = false },
            onCancelar = { confirmarReinicio = false }
        )
    }
}

@Composable
private fun FilaEjercicio(
    ej: EjercicioRutina,
    hechas: Set<Int>,
    color: Color,
    sugerido: Float?,
    onSerie: (Int, Boolean) -> Unit,
    onPeso: () -> Unit,
    onHistorial: () -> Unit,
    onCronometro: () -> Unit
) {
    val total = Gym.totalSeries(ej)
    val completo = Gym.completo(ej, hechas)
    val esFuerza = ej.grupo !in Gym.CATEGORIAS_NO_FUERZA && !ej.esTiempo

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onHistorial)
            ) {
                Text(
                    ej.nombre,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (completo) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (completo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                val detalle = buildString {
                    append("${ej.series} × ${ej.reps}${if (ej.esTiempo) " s" else ""}")
                    if (ej.vecesPorDia > 1) append(" · ${ej.vecesPorDia} veces al día")
                    append(" · descanso ${reloj(ej.descansoSeg)}")
                }
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!ej.nota.isNullOrBlank()) {
                    Text(ej.nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (esFuerza) {
                AssistChip(
                    onClick = onPeso,
                    label = {
                        Text(
                            ej.pesoKg?.let { "${Gym.kg(it)} kg" } ?: "Definir peso",
                            fontWeight = FontWeight.Bold,
                            color = if (ej.pesoKg == null) MaterialTheme.colorScheme.tertiary else color
                        )
                    }
                )
            } else if (ej.esTiempo) {
                FilledTonalButton(onClick = onCronometro) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Cronómetro")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        FlowRowSeries(total, ej.series, hechas, color, onSerie)
        if (sugerido != null && esFuerza) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Insignia("Completaste todas tus series: prueba ${Gym.kg(sugerido)} kg", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSeries(total: Int, porBloque: Int, hechas: Set<Int>, color: Color, onSerie: (Int, Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until total) {
            val hecha = i in hechas
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (hecha) color else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSerie(i, !hecha) },
                contentAlignment = Alignment.Center
            ) {
                if (hecha) {
                    Icon(Icons.Default.Check, contentDescription = "Serie ${i + 1} hecha", tint = Color.White)
                } else {
                    Text("${i % porBloque + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Cuenta atras del descanso entre series; vibra y suena al terminar. */
@Composable
private fun BarraDescanso(segundos: Int, clave: Int, onCerrar: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val vista = LocalView.current
    var fin by remember(clave) { mutableLongStateOf(System.currentTimeMillis() + segundos * 1000L) }
    var restante by remember(clave) { mutableIntStateOf(segundos) }

    DisposableEffect(Unit) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
    LaunchedEffect(clave, fin) {
        while (true) {
            val r = ((fin - System.currentTimeMillis() + 999) / 1000).toInt()
            restante = r.coerceAtLeast(0)
            if (r <= 0) break
            delay(250)
        }
        avisarFin(ctx)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (restante == 0) "¡A la siguiente serie!" else "Descanso",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(reloj(restante), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            TextButton(onClick = { fin += 15_000L }) { Text("+15 s") }
            TextButton(onClick = onCerrar) { Text(if (restante == 0) "Cerrar" else "Saltar") }
        }
    }
}

/** Cronometro por intervalos para ejercicios por tiempo (ej: slant board 3 x 80 s con 15 s de descanso). */
@Composable
private fun IntervaloDialog(ejercicio: EjercicioRutina, onTerminado: () -> Unit, onCerrar: () -> Unit) {
    val ctx = LocalContext.current
    val vista = LocalView.current
    var serie by remember { mutableIntStateOf(1) }
    var trabajando by remember { mutableStateOf(true) }
    var restante by remember { mutableIntStateOf(ejercicio.reps) }
    var corriendo by remember { mutableStateOf(false) }
    var terminado by remember { mutableStateOf(false) }

    DisposableEffect(corriendo) {
        vista.keepScreenOn = corriendo
        onDispose { vista.keepScreenOn = false }
    }
    LaunchedEffect(corriendo, serie, trabajando) {
        if (!corriendo) return@LaunchedEffect
        while (restante > 0) {
            delay(1000)
            restante--
        }
        avisarFin(ctx)
        if (trabajando) {
            if (serie >= ejercicio.series) {
                corriendo = false
                terminado = true
            } else {
                trabajando = false
                restante = ejercicio.descansoSeg
            }
        } else {
            serie++
            trabajando = true
            restante = ejercicio.reps
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(ejercicio.nombre) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (terminado) "¡Terminado!" else "Serie $serie de ${ejercicio.series} · ${if (trabajando) "Trabajo" else "Descanso"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    reloj(restante),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (trabajando) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
                if (!terminado) {
                    Button(onClick = { corriendo = !corriendo }) { Text(if (corriendo) "Pausar" else "Iniciar") }
                }
            }
        },
        confirmButton = {
            if (terminado) TextButton(onClick = onTerminado) { Text("Marcar series") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

/** Cambia el peso de trabajo (en kilos) y lo anota en el historial. */
@Composable
private fun SubirPesoDialog(ejercicio: EjercicioRutina, onConfirmar: (Float) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf(ejercicio.pesoKg?.let { Gym.kg(it) } ?: "") }
    val actual = texto.replace(',', '.').toFloatOrNull()

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Peso de ${ejercicio.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Kilos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5f, 1f, 2.5f, 5f).forEach { paso ->
                        OutlinedButton(onClick = { texto = Gym.kg((actual ?: 0f) + paso) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Text("+${Gym.kg(paso)}")
                        }
                    }
                }
                Text(
                    "Cuando subes de carga, se guarda en el historial para ver tu progreso mes a mes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = actual != null && actual > 0f, onClick = { actual?.let(onConfirmar) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Grafica del peso de trabajo de un ejercicio a lo largo del tiempo y su historial. */
@Composable
fun HistorialPesoDialog(ejercicio: EjercicioRutina, pesos: List<PesoEjercicio>, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(ejercicio.nombre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pesos.isEmpty()) {
                    Text("Aún no hay pesos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val maximo = pesos.maxOf { it.kg }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Dato("Actual", "${Gym.kg(pesos.last().kg)} kg")
                        Dato("Récord", "${Gym.kg(maximo)} kg")
                        Dato("Cambios", "${pesos.size}")
                    }
                    if (pesos.size >= 2) {
                        GraficaLinea(pesos.map { it.timestamp to it.kg }, MaterialTheme.colorScheme.primary)
                    }
                    pesos.takeLast(6).reversed().forEach { p ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${Gym.kg(p.kg)} kg", fontWeight = FontWeight.SemiBold)
                            Text(fechaHora(p.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
