package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.repository.MetricaRepository
import com.mauricior8.calorias.ui.MainViewModel

/**
 * Pestaña "Tablas de alimentos". Permite crear tablas tipo hoja de calculo con
 * hasta 10 columnas (categorias) y 100 filas (alimentos), con celdas editables.
 */
@Composable
fun TablasScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tablas by viewModel.tablas.collectAsStateWithLifecycle()
    var tablaAbierta by remember { mutableStateOf<TablaAlimentos?>(null) }

    // Mantener referencia actualizada de la tabla abierta.
    val abierta = tablaAbierta?.let { sel -> tablas.firstOrNull { it.id == sel.id } }

    if (abierta == null) {
        ListaTablas(
            tablas = tablas,
            onAbrir = { tablaAbierta = it },
            onCrear = { viewModel.crearTabla(it) },
            onEliminar = { viewModel.eliminarTabla(it) },
            modifier = modifier
        )
    } else {
        DetalleTabla(
            tabla = abierta,
            viewModel = viewModel,
            onVolver = { tablaAbierta = null },
            modifier = modifier
        )
    }
}

@Composable
private fun ListaTablas(
    tablas: List<TablaAlimentos>,
    onAbrir: (TablaAlimentos) -> Unit,
    onCrear: (String) -> Unit,
    onEliminar: (TablaAlimentos) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        FilledTonalButton(
            onClick = { mostrarDialogo = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Crear tabla")
        }
        Spacer(Modifier.height(12.dp))

        if (tablas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No hay tablas. Crea una para empezar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(tablas, key = { it.id }) { tabla ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAbrir(tabla) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tabla.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onEliminar(tabla) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar tabla",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoTexto(
            titulo = "Nueva tabla",
            etiqueta = "Nombre de la tabla",
            onConfirmar = { onCrear(it); mostrarDialogo = false },
            onCancelar = { mostrarDialogo = false }
        )
    }
}

@Composable
private fun DetalleTabla(
    tabla: TablaAlimentos,
    viewModel: MainViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val columnas by remember(tabla.id) { viewModel.columnas(tabla.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val filas by remember(tabla.id) { viewModel.filas(tabla.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val celdas by remember(tabla.id) { viewModel.celdas(tabla.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val celdaPorClave = remember(celdas) {
        celdas.associateBy { it.filaId to it.columnaId }
    }

    var dialogoColumna by remember { mutableStateOf(false) }
    var dialogoFila by remember { mutableStateOf(false) }
    var celdaEditando by remember { mutableStateOf<Triple<FilaTabla, ColumnaTabla, String>?>(null) }
    var mostrarGrafica by remember { mutableStateOf(false) }

    val anchoCelda = 110.dp
    val anchoFilaNombre = 130.dp

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        // Cabecera.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                tabla.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { dialogoColumna = true },
                enabled = columnas.size < MetricaRepository.MAX_COLUMNAS,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Columna (${columnas.size}/${MetricaRepository.MAX_COLUMNAS})")
            }
            OutlinedButton(
                onClick = { dialogoFila = true },
                enabled = filas.size < MetricaRepository.MAX_FILAS,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Fila (${filas.size}/${MetricaRepository.MAX_FILAS})")
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { mostrarGrafica = true },
            enabled = columnas.isNotEmpty() && filas.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Ver / editar grafica")
        }

        Spacer(Modifier.height(10.dp))

        if (columnas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Agrega columnas (categorias) y filas (alimentos).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val scrollH = rememberScrollState()
            Column(Modifier.fillMaxSize()) {
                // Fila de encabezados.
                Row(Modifier.horizontalScroll(scrollH)) {
                    CeldaCabecera("Alimento", anchoFilaNombre)
                    columnas.forEach { col ->
                        Box(
                            modifier = Modifier
                                .width(anchoCelda)
                                .height(48.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { viewModel.eliminarColumna(col) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                col.nombre,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }

                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(filas, key = { it.id }) { fila ->
                        Row(Modifier.horizontalScroll(scrollH)) {
                            // Nombre de fila (eliminar con click largo via boton).
                            Box(
                                modifier = Modifier
                                    .width(anchoFilaNombre)
                                    .height(48.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.eliminarFila(fila) },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    fila.nombre,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            columnas.forEach { col ->
                                val valor = celdaPorClave[fila.id to col.id]?.valor ?: ""
                                Box(
                                    modifier = Modifier
                                        .width(anchoCelda)
                                        .height(48.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .clickable {
                                            celdaEditando = Triple(fila, col, valor)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(valor, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogoColumna) {
        DialogoTexto(
            titulo = "Nueva columna",
            etiqueta = "Nombre de la categoria",
            onConfirmar = { viewModel.agregarColumna(tabla.id, it); dialogoColumna = false },
            onCancelar = { dialogoColumna = false }
        )
    }
    if (dialogoFila) {
        DialogoTexto(
            titulo = "Nuevo alimento",
            etiqueta = "Nombre del alimento",
            onConfirmar = { viewModel.agregarFila(tabla.id, it); dialogoFila = false },
            onCancelar = { dialogoFila = false }
        )
    }
    celdaEditando?.let { (fila, col, valorActual) ->
        DialogoTexto(
            titulo = "${fila.nombre} - ${col.nombre}",
            etiqueta = "Valor",
            valorInicial = valorActual,
            onConfirmar = {
                viewModel.setCelda(tabla.id, fila.id, col.id, it)
                celdaEditando = null
            },
            onCancelar = { celdaEditando = null }
        )
    }

    if (mostrarGrafica) {
        GraficaDialog(
            tabla = tabla,
            columnas = columnas,
            filas = filas,
            celdaPorClave = celdaPorClave,
            onCambiarTipo = { viewModel.cambiarTipoGrafica(tabla, it) },
            onCerrar = { mostrarGrafica = false }
        )
    }
}

@Composable
private fun GraficaDialog(
    tabla: TablaAlimentos,
    columnas: List<ColumnaTabla>,
    filas: List<FilaTabla>,
    celdaPorClave: Map<Pair<Int, Int>, CeldaTabla>,
    onCambiarTipo: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val tipos = listOf("Barras", "Histograma", "Pastel", "Picos")
    var columnaSel by remember(tabla.id) { mutableStateOf(columnas.firstOrNull()?.id) }

    val datos: List<Pair<String, Float>> = remember(columnaSel, celdaPorClave, filas) {
        val colId = columnaSel
        if (colId == null) emptyList()
        else filas.map { fila ->
            val v = celdaPorClave[fila.id to colId]?.valor?.toFloatOrNull() ?: 0f
            fila.nombre to v
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Grafica: ${tabla.nombre}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Tipo de grafica", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tipos.forEach { t ->
                        FilterChip(
                            selected = tabla.tipoGrafica == t,
                            onClick = { onCambiarTipo(t) },
                            label = { Text(t) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Columna a graficar", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    columnas.forEach { col ->
                        FilterChip(
                            selected = columnaSel == col.id,
                            onClick = { columnaSel = col.id },
                            label = { Text(col.nombre) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                GraficaTabla(datos = datos, tipo = tabla.tipoGrafica)
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

@Composable
private fun CeldaCabecera(texto: String, ancho: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(ancho)
            .height(48.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun DialogoTexto(
    titulo: String,
    etiqueta: String,
    valorInicial: String = "",
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf(valorInicial) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text(etiqueta) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(texto) }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
