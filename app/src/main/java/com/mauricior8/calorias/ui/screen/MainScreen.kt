package com.mauricior8.calorias.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Pestana(val titulo: String, val icono: ImageVector) {
    METRICAS("Mi día", Icons.Filled.Star),
    PROGRESO("Progreso", Icons.Filled.BarChart),
    NOTAS("Notas", Icons.Filled.Edit),
    CALCULADORA("Calculadora", Icons.AutoMirrored.Filled.List),
    TABLAS("Tablas", Icons.Filled.DateRange)
}

/** Accion destructiva en espera de confirmacion del usuario. */
private class AccionPendiente(
    val titulo: String,
    val mensaje: String,
    val textoConfirmar: String,
    val accion: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notas by viewModel.notas.collectAsStateWithLifecycle()
    val historialCalculos by viewModel.historialCalculos.collectAsStateWithLifecycle()
    val indiceDia by viewModel.indiceDiaSeleccionado.collectAsStateWithLifecycle()
    val etiquetaDia by viewModel.etiquetaDiaSeleccionado.collectAsStateWithLifecycle()
    val diaCompletado by viewModel.diaCompletado.collectAsStateWithLifecycle()
    val fechaMillis by viewModel.fechaSeleccionadaMillis.collectAsStateWithLifecycle()
    val alimentosGuardados by viewModel.alimentosGuardados.collectAsStateWithLifecycle()
    val totalesPorComida by viewModel.totalesPorComida.collectAsStateWithLifecycle()
    val progreso by viewModel.progreso.collectAsStateWithLifecycle()
    val rangoDias by viewModel.rangoDias.collectAsStateWithLifecycle()
    val pesos by viewModel.pesos.collectAsStateWithLifecycle()

    var pestana by remember { mutableStateOf(Pestana.METRICAS) }

    // Estado de dialogos.
    var mostrarEditor by remember { mutableStateOf(false) }
    var metricaEnEdicion by remember { mutableStateOf<MetricaConfig?>(null) }
    var metricaHistorial by remember { mutableStateOf<MetricaConfig?>(null) }
    var mostrarAlimento by remember { mutableStateOf(false) }
    var mostrarObjetivos by remember { mutableStateOf(false) }
    var mostrarRecordatorio by remember { mutableStateOf(false) }
    var menuAbierto by remember { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf<AccionPendiente?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.mensajes.collect { snackbarHostState.showSnackbar(it) }
    }

    // ---- Respaldo (Storage Access Framework: el usuario elige donde guardar o abrir) ----
    val contexto = LocalContext.current
    val guardarRespaldo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportar { json ->
                val salida = contexto.contentResolver.openOutputStream(uri, "wt")
                    ?: throw java.io.IOException("No se pudo abrir el archivo")
                salida.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    val abrirRespaldo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val texto = try {
                contexto.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
            if (texto == null) {
                viewModel.avisar("No se pudo leer el archivo")
            } else {
                pendiente = AccionPendiente(
                    titulo = "Restaurar respaldo",
                    mensaje = "Se reemplazarán TODOS tus datos actuales por los del archivo. " +
                        "Esta acción no se puede deshacer.",
                    textoConfirmar = "Restaurar"
                ) { viewModel.importar(texto) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(pestana.titulo) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text("Calcular objetivos") },
                            onClick = { menuAbierto = false; mostrarObjetivos = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Recordatorio de agua") },
                            onClick = { menuAbierto = false; mostrarRecordatorio = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Guardar respaldo") },
                            onClick = {
                                menuAbierto = false
                                val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                guardarRespaldo.launch("calorias-respaldo-$fecha.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restaurar respaldo") },
                            onClick = {
                                menuAbierto = false
                                abrirRespaldo.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Pestana.entries.forEach { p ->
                    NavigationBarItem(
                        selected = pestana == p,
                        onClick = { pestana = p },
                        icon = { Icon(p.icono, contentDescription = p.titulo) },
                        label = { Text(p.titulo, maxLines = 1) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (pestana == Pestana.METRICAS) {
                FloatingActionButton(onClick = {
                    metricaEnEdicion = null
                    mostrarEditor = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva métrica")
                }
            }
        }
    ) { padding ->
        when (pestana) {
            Pestana.METRICAS -> MetricasScreen(
                metricas = state.metricas,
                isLoading = state.isLoading,
                diasSemana = viewModel.diasSemana,
                indiceDia = indiceDia,
                etiquetaDia = etiquetaDia,
                diaCompletado = diaCompletado,
                fechaMillis = fechaMillis,
                onSeleccionarDia = viewModel::seleccionarDia,
                onFechaManual = viewModel::setFechaManual,
                onToggleCompletado = viewModel::marcarDiaCompletado,
                onLimpiarDia = {
                    pendiente = AccionPendiente(
                        titulo = "Limpiar día",
                        mensaje = "Se borrarán todos los registros de $etiquetaDia.",
                        textoConfirmar = "Limpiar"
                    ) { viewModel.limpiarDia() }
                },
                onAgregar = viewModel::agregarRegistro,
                onAgregarAlimento = { mostrarAlimento = true },
                onVerHistorial = { metricaHistorial = it },
                onEditar = { metricaEnEdicion = it; mostrarEditor = true },
                onEliminar = { metrica ->
                    pendiente = AccionPendiente(
                        titulo = "Eliminar métrica",
                        mensaje = "Se borrará \"${metrica.nombre}\" junto con TODO su historial.",
                        textoConfirmar = "Eliminar"
                    ) { viewModel.eliminarMetrica(metrica) }
                },
                onLimpiarMetrica = { id ->
                    val nombre = state.metricas.firstOrNull { it.config.id == id }?.config?.nombre ?: "esta métrica"
                    pendiente = AccionPendiente(
                        titulo = "Limpiar datos de hoy",
                        mensaje = "Se borrarán los registros de \"$nombre\" de $etiquetaDia.",
                        textoConfirmar = "Limpiar"
                    ) { viewModel.limpiarMetrica(id) }
                },
                onMoverArriba = viewModel::moverArriba,
                onMoverAbajo = viewModel::moverAbajo,
                totalesPorComida = totalesPorComida,
                modifier = Modifier.padding(padding)
            )

            Pestana.PROGRESO -> ProgresoScreen(
                metricas = state.metricas.map { it.config },
                progreso = progreso,
                rangoDias = rangoDias,
                pesos = pesos,
                onRango = viewModel::setRangoProgreso,
                onAgregarPeso = viewModel::agregarPeso,
                onEliminarPeso = viewModel::eliminarPeso,
                modifier = Modifier.padding(padding)
            )

            Pestana.NOTAS -> NotasScreen(
                notas = notas,
                onGuardar = { id, texto -> viewModel.guardarNota(texto, id) },
                onEliminar = viewModel::eliminarNota,
                modifier = Modifier.padding(padding)
            )

            Pestana.CALCULADORA -> CalculadoraScreen(
                historial = historialCalculos,
                onCalcular = viewModel::calcular,
                onLimpiarHistorial = viewModel::limpiarHistorialCalculos,
                modifier = Modifier.padding(padding)
            )

            Pestana.TABLAS -> TablasScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }

    // Dialogo agregar alimento (aporta a varias metricas).
    if (mostrarAlimento) {
        AgregarAlimentoDialog(
            metricas = state.metricas.map { it.config },
            alimentos = alimentosGuardados,
            onConfirmar = { nombre, valores, comida ->
                viewModel.agregarAlimento(nombre, valores, comida)
                mostrarAlimento = false
            },
            onAplicar = { alimento, comida -> viewModel.aplicarAlimentoGuardado(alimento, comida) },
            onEditarGuardado = { a, nombre, valores ->
                viewModel.editarAlimentoGuardado(a, nombre, valores)
            },
            onEliminarGuardado = { viewModel.eliminarAlimentoGuardado(it) },
            onGuardarNota = { viewModel.guardarNota(it) },
            onBuscar = viewModel::buscarAlimentos,
            onCancelar = { mostrarAlimento = false }
        )
    }

    // Dialogo crear/editar metrica.
    if (mostrarEditor) {
        MetricaEditorDialog(
            metricaExistente = metricaEnEdicion,
            onConfirmar = { nombre, unidad, limite, colorHex, tipoGrafica ->
                val enEdicion = metricaEnEdicion
                if (enEdicion == null) {
                    viewModel.crearMetrica(nombre, unidad, limite, colorHex, tipoGrafica)
                } else {
                    viewModel.editarMetrica(
                        enEdicion.copy(
                            nombre = nombre.trim(),
                            unidad = unidad,
                            limiteMaximo = limite,
                            colorHex = colorHex,
                            tipoGrafica = tipoGrafica
                        )
                    )
                }
                mostrarEditor = false
                metricaEnEdicion = null
            },
            onCancelar = {
                mostrarEditor = false
                metricaEnEdicion = null
            }
        )
    }

    // Dialogo historial.
    metricaHistorial?.let { metrica ->
        HistorialDialog(
            metrica = metrica,
            viewModel = viewModel,
            onCerrar = { metricaHistorial = null }
        )
    }

    if (mostrarObjetivos) {
        ObjetivosDialog(
            pesoInicial = pesos.lastOrNull()?.kg,
            onAplicar = viewModel::aplicarObjetivos,
            onCerrar = { mostrarObjetivos = false }
        )
    }

    if (mostrarRecordatorio) {
        RecordatorioDialog(
            onAviso = viewModel::avisar,
            onCerrar = { mostrarRecordatorio = false }
        )
    }

    pendiente?.let { p ->
        ConfirmarDialog(
            titulo = p.titulo,
            mensaje = p.mensaje,
            textoConfirmar = p.textoConfirmar,
            onConfirmar = { p.accion(); pendiente = null },
            onCancelar = { pendiente = null }
        )
    }
}
