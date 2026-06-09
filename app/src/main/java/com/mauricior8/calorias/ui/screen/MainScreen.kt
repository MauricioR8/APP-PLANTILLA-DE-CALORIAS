package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.ui.MainViewModel

private enum class Pestana(val titulo: String, val icono: ImageVector) {
    METRICAS("Mi dia", Icons.Filled.Star),
    NOTAS("Notas", Icons.Filled.Edit),
    CALCULADORA("Calculadora", Icons.AutoMirrored.Filled.List),
    TABLAS("Tablas", Icons.Filled.DateRange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notas by viewModel.notas.collectAsStateWithLifecycle()
    val historialCalculos by viewModel.historialCalculos.collectAsStateWithLifecycle()

    var pestana by remember { mutableStateOf(Pestana.METRICAS) }

    // Estado de dialogos.
    var mostrarEditor by remember { mutableStateOf(false) }
    var metricaEnEdicion by remember { mutableStateOf<MetricaConfig?>(null) }
    var metricaHistorial by remember { mutableStateOf<MetricaConfig?>(null) }
    var mostrarAlimento by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pestana.titulo) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
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
                        label = { Text(p.titulo) }
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
                    Icon(Icons.Default.Add, contentDescription = "Nueva metrica")
                }
            }
        }
    ) { padding ->
        when (pestana) {
            Pestana.METRICAS -> MetricasScreen(
                metricas = state.metricas,
                isLoading = state.isLoading,
                onAgregar = viewModel::agregarRegistro,
                onAgregarAlimento = { mostrarAlimento = true },
                onVerHistorial = { metricaHistorial = it },
                onEditar = { metricaEnEdicion = it; mostrarEditor = true },
                onEliminar = viewModel::eliminarMetrica,
                onMoverArriba = viewModel::moverArriba,
                onMoverAbajo = viewModel::moverAbajo,
                modifier = Modifier.padding(padding)
            )

            Pestana.NOTAS -> NotasScreen(
                notas = notas,
                onGuardar = { viewModel.guardarNota(it) },
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
            onConfirmar = { nombre, valores ->
                viewModel.agregarAlimento(nombre, valores)
                mostrarAlimento = false
            },
            onCancelar = { mostrarAlimento = false }
        )
    }

    // Dialogo crear/editar metrica.
    if (mostrarEditor) {
        MetricaEditorDialog(
            metricaExistente = metricaEnEdicion,
            onConfirmar = { nombre, unidad, limite, colorHex ->
                val enEdicion = metricaEnEdicion
                if (enEdicion == null) {
                    viewModel.crearMetrica(nombre, unidad, limite, colorHex)
                } else {
                    viewModel.editarMetrica(
                        enEdicion.copy(
                            nombre = nombre.trim(),
                            unidad = unidad,
                            limiteMaximo = limite,
                            colorHex = colorHex
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
}
