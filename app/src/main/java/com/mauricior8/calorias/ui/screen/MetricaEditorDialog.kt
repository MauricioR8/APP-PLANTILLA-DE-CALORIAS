package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.util.ColoresMetrica
import com.mauricior8.calorias.util.colorDesdeHex

/**
 * Dialogo reutilizable para crear o editar una metrica.
 *
 * @param metricaExistente si es null se crea una nueva; si no, se editan sus campos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricaEditorDialog(
    metricaExistente: MetricaConfig? = null,
    onConfirmar: (nombre: String, unidad: String, limite: Float?, colorHex: String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(metricaExistente?.nombre ?: "") }
    var unidad by remember { mutableStateOf(metricaExistente?.unidad ?: "kcal") }
    var limite by remember {
        mutableStateOf(metricaExistente?.limiteMaximo?.let {
            if (it % 1f == 0f) it.toInt().toString() else it.toString()
        } ?: "")
    }
    var colorHex by remember { mutableStateOf(metricaExistente?.colorHex ?: ColoresMetrica.first()) }
    val unidades = listOf("kcal", "gr", "mg", "L")

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (metricaExistente == null) "Nueva metrica" else "Editar metrica") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Unidad")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    unidades.forEach { u ->
                        FilterChip(
                            selected = unidad == u,
                            onClick = { unidad = u },
                            label = { Text(u) }
                        )
                    }
                }

                OutlinedTextField(
                    value = limite,
                    onValueChange = { limite = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Limite diario (opcional)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColoresMetrica.forEach { hex ->
                        val seleccionado = hex == colorHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(colorDesdeHex(hex), CircleShape)
                                .border(
                                    width = if (seleccionado) 3.dp else 0.dp,
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (seleccionado) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(nombre, unidad, limite.toFloatOrNull(), colorHex) }
            ) { Text(if (metricaExistente == null) "Crear" else "Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
