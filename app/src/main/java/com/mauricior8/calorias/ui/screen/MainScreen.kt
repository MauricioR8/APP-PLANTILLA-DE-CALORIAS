package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.ui.state.MetricaConItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mostrarDialogoMetrica by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mi dia - Plantilla Calorias") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoMetrica = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva metrica")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    items(state.metricas, key = { it.config.id }) { item ->
                        MetricaCard(
                            item = item,
                            onAgregar = { valor ->
                                viewModel.agregarRegistro(item.config.id, valor)
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoMetrica) {
        NuevaMetricaDialog(
            onConfirmar = { nombre, unidad, limite ->
                viewModel.crearMetrica(nombre = nombre, unidad = unidad, limiteMaximo = limite)
                mostrarDialogoMetrica = false
            },
            onCancelar = { mostrarDialogoMetrica = false }
        )
    }
}

@Composable
private fun MetricaCard(
    item: MetricaConItem,
    onAgregar: (Float) -> Unit
) {
    var entrada by remember { mutableStateOf("") }
    val color = parseColor(item.config.colorHex)

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.config.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                val limiteTxt = item.config.limiteMaximo?.let { " / ${formato(it)}" } ?: ""
                Text(
                    text = "${formato(item.totalHoy)}$limiteTxt ${item.config.unidad}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (item.config.limiteMaximo != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progreso },
                    modifier = Modifier.fillMaxWidth(),
                    color = color
                )
            }

            Spacer(Modifier.height(12.dp))
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
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    entrada.toFloatOrNull()?.let {
                        onAgregar(it)
                        entrada = ""
                    }
                }) {
                    Text("Agregar")
                }
            }
        }
    }
}

private fun formato(valor: Float): String =
    if (valor % 1f == 0f) valor.toInt().toString() else "%.1f".format(valor)

private fun parseColor(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        Color(0xFFFF6D00)
    }
