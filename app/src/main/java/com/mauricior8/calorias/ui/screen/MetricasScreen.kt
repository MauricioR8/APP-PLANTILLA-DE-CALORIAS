package com.mauricior8.calorias.ui.screen

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.ui.state.MetricaConItem
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
    onAgregar: (metricaId: String, valor: Float) -> Unit,
    onVerHistorial: (MetricaConfig) -> Unit,
    onEditar: (MetricaConfig) -> Unit,
    onEliminar: (MetricaConfig) -> Unit,
    onMoverArriba: (String) -> Unit,
    onMoverAbajo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            metricas.isEmpty() -> Text(
                "No hay metricas. Pulsa + para crear una.",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                items(metricas, key = { it.config.id }) { item ->
                    MetricaCard(
                        item = item,
                        esPrimero = item == metricas.first(),
                        esUltimo = item == metricas.last(),
                        onAgregar = { valor -> onAgregar(item.config.id, valor) },
                        onVerHistorial = { onVerHistorial(item.config) },
                        onEditar = { onEditar(item.config) },
                        onEliminar = { onEliminar(item.config) },
                        onMoverArriba = { onMoverArriba(item.config.id) },
                        onMoverAbajo = { onMoverAbajo(item.config.id) }
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
                // Anillo de progreso con el porcentaje.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                    CircularProgressIndicator(
                        progress = { if (item.config.limiteMaximo != null) progresoAnim else 1f },
                        modifier = Modifier.size(56.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.18f),
                        strokeWidth = 6.dp
                    )
                    if (item.config.limiteMaximo != null) {
                        Text(
                            "${(item.progreso * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
