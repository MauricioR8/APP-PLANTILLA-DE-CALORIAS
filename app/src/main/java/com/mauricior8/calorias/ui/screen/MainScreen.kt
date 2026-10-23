package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.util.Gym

private enum class Pestana(val titulo: String, val icono: ImageVector) {
    HOY("Hoy", Icons.Filled.Star),
    GYM("Gym", Icons.Filled.FitnessCenter),
    PROGRESO("Progreso", Icons.Filled.BarChart),
    NOTAS("Notas", Icons.Filled.Edit),
    MAS("Más", Icons.Filled.Menu)
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
    val indiceDia by viewModel.indiceDiaSeleccionado.collectAsStateWithLifecycle()
    val etiquetaDia by viewModel.etiquetaDiaSeleccionado.collectAsStateWithLifecycle()
    val diaCompletado by viewModel.diaCompletado.collectAsStateWithLifecycle()
    val fechaMillis by viewModel.fechaSeleccionadaMillis.collectAsStateWithLifecycle()
    val alimentosGuardados by viewModel.alimentosGuardados.collectAsStateWithLifecycle()
    val totalesPorComida by viewModel.totalesPorComida.collectAsStateWithLifecycle()
    val pesos by viewModel.pesos.collectAsStateWithLifecycle()
    val racha by viewModel.racha.collectAsStateWithLifecycle()
    val rutinas by viewModel.rutinas.collectAsStateWithLifecycle()
    val rutinasDelDia by viewModel.rutinasDelDia.collectAsStateWithLifecycle()
    val seriesDelDia by viewModel.seriesDelDia.collectAsStateWithLifecycle()
    val suplementos by viewModel.suplementos.collectAsStateWithLifecycle()
    val suplementosTomados by viewModel.suplementosTomados.collectAsStateWithLifecycle()
    val registrosDelDia by viewModel.registrosDelDia.collectAsStateWithLifecycle()
    val perfil by viewModel.perfilBase.collectAsStateWithLifecycle()

    var pestana by rememberSaveable { mutableStateOf(Pestana.HOY) }

    // Estado de dialogos.
    var mostrarEditor by remember { mutableStateOf(false) }
    var metricaEnEdicion by remember { mutableStateOf<MetricaConfig?>(null) }
    var metricaHistorial by remember { mutableStateOf<MetricaConfig?>(null) }
    var mostrarAlimento by remember { mutableStateOf(false) }
    var mostrarObjetivos by remember { mutableStateOf(false) }
    var mostrarDetalle by remember { mutableStateOf(false) }
    var mostrarSuplementos by remember { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf<AccionPendiente?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.mensajes.collect { snackbarHostState.showSnackbar(it) }
    }

    // Colores de la tira de dias (segun la rutina de fuerza de cada dia) y resumen del gym de hoy.
    val coloresDias = remember(rutinas) {
        (1..7).map { iso ->
            rutinas.firstOrNull { !it.diaria && Gym.aplicaEn(it, iso) }?.let { colorRutina(it.colorHex) }
        }
    }
    val gymHoy = remember(rutinasDelDia, seriesDelDia, rutinas) {
        if (rutinas.isEmpty()) {
            null
        } else {
            val fuerza = rutinasDelDia.firstOrNull { !it.rutina.diaria }
            val ejercicios = rutinasDelDia.flatMap { it.ejercicios }
            val hechas = seriesDelDia.count { s ->
                ejercicios.any { it.id == s.ejercicioId && s.indice < Gym.totalSeries(it) }
            }
            ResumenGymHoy(
                titulo = fuerza?.rutina?.nombre ?: "Bloque diario",
                color = fuerza?.rutina?.colorHex?.let { colorRutina(it) },
                ejercicios = ejercicios.size,
                hechas = hechas,
                totales = ejercicios.sumOf { Gym.totalSeries(it) },
                descanso = fuerza == null
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(pestana.titulo) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
            if (pestana == Pestana.HOY) {
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
            Pestana.HOY -> MetricasScreen(
                metricas = state.metricas,
                isLoading = state.isLoading,
                diaDescanso = state.diaDescanso,
                diasSemana = viewModel.diasSemana,
                indiceDia = indiceDia,
                etiquetaDia = etiquetaDia,
                coloresDias = coloresDias,
                diaCompletado = diaCompletado,
                fechaMillis = fechaMillis,
                racha = racha,
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
                onCopiarDiaAnterior = {
                    pendiente = AccionPendiente(
                        titulo = "Copiar día anterior",
                        mensaje = "Se agregarán a $etiquetaDia todos los registros del día anterior.",
                        textoConfirmar = "Copiar"
                    ) { viewModel.copiarDiaAnterior() }
                },
                onVerDetalle = { mostrarDetalle = true },
                onAgregar = viewModel::agregarRegistro,
                onAgregarAlimento = { mostrarAlimento = true },
                onAgregarAgua = viewModel::agregarAgua,
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
                gymHoy = gymHoy,
                onAbrirGym = { pestana = Pestana.GYM },
                suplementos = suplementos,
                suplementosTomados = suplementosTomados,
                onSuplemento = viewModel::marcarSuplemento,
                onGestionarSuplementos = { mostrarSuplementos = true },
                modifier = Modifier.padding(padding)
            )

            Pestana.GYM -> GymScreen(viewModel, Modifier.padding(padding))

            Pestana.PROGRESO -> ProgresoScreen(viewModel, Modifier.padding(padding))

            Pestana.NOTAS -> NotasScreen(
                notas = notas,
                onGuardar = { id, texto -> viewModel.guardarNota(texto, id) },
                onEliminar = viewModel::eliminarNota,
                modifier = Modifier.padding(padding)
            )

            Pestana.MAS -> MasScreen(
                viewModel = viewModel,
                onObjetivos = { mostrarObjetivos = true },
                onPedirConfirmacion = { titulo, mensaje, texto, accion ->
                    pendiente = AccionPendiente(titulo, mensaje, texto, accion)
                },
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
            onAplicar = { alimento, comida, porcion -> viewModel.aplicarAlimentoGuardado(alimento, comida, porcion) },
            onEditarGuardado = { a, nombre, valores -> viewModel.editarAlimentoGuardado(a, nombre, valores) },
            onEliminarGuardado = { viewModel.eliminarAlimentoGuardado(it) },
            onFavorito = viewModel::marcarFavorito,
            onCombinar = viewModel::combinarAlimentos,
            onGuardarNota = { viewModel.guardarNota(it) },
            onBuscar = viewModel::buscarAlimentos,
            onCancelar = { mostrarAlimento = false }
        )
    }

    // Dialogo crear/editar metrica.
    if (mostrarEditor) {
        MetricaEditorDialog(
            metricaExistente = metricaEnEdicion,
            onConfirmar = { nombre, unidad, limite, colorHex, tipoGrafica, limiteDescanso ->
                val enEdicion = metricaEnEdicion
                if (enEdicion == null) {
                    viewModel.crearMetrica(nombre, unidad, limite, colorHex, tipoGrafica, limiteDescanso)
                } else {
                    viewModel.editarMetrica(
                        enEdicion.copy(
                            nombre = nombre.trim(),
                            unidad = unidad,
                            limiteMaximo = limite,
                            colorHex = colorHex,
                            tipoGrafica = tipoGrafica,
                            limiteDescanso = limiteDescanso
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

    if (mostrarDetalle) {
        DetalleDiaDialog(
            etiquetaDia = etiquetaDia,
            registros = registrosDelDia,
            metricas = state.metricas.map { it.config },
            onCerrar = { mostrarDetalle = false }
        )
    }

    if (mostrarSuplementos) {
        SuplementosDialog(
            suplementos = suplementos,
            onAgregar = viewModel::agregarSuplemento,
            onEliminar = viewModel::eliminarSuplemento,
            onCerrar = { mostrarSuplementos = false }
        )
    }

    if (mostrarObjetivos) {
        ObjetivosDialog(
            pesoInicial = pesos.lastOrNull()?.kg,
            perfilGuardado = perfil,
            onAplicar = { objetivos, p ->
                viewModel.guardarPerfil(p)
                viewModel.aplicarObjetivos(objetivos)
            },
            onCerrar = { mostrarObjetivos = false }
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
