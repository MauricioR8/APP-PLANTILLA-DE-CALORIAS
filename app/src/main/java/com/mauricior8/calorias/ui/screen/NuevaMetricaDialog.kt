package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialogo para crear dinamicamente una nueva metrica/categoria.
 */
@Composable
fun NuevaMetricaDialog(
    onConfirmar: (nombre: String, unidad: String, limite: Float?) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("kcal") }
    var limite by remember { mutableStateOf("") }
    val unidades = listOf("kcal", "gr", "mg", "L")

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva metrica") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Grasas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Unidad")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(nombre, unidad, limite.toFloatOrNull()) }
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
