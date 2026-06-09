package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import java.util.Locale

/**
 * Formulario para registrar un alimento que aporta valores a varias metricas.
 *
 * - Calorias y Proteinas son OBLIGATORIAS (si esas metricas existen).
 * - El resto de campos son opcionales.
 */
@Composable
fun AgregarAlimentoDialog(
    metricas: List<MetricaConfig>,
    onConfirmar: (nombre: String, valores: Map<String, Float>) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    val valores = remember { mutableStateMapOf<String, String>() }
    var intentoEnviar by remember { mutableStateOf(false) }

    fun esObligatoria(m: MetricaConfig): Boolean {
        val n = m.nombre.lowercase(Locale.getDefault())
        return n.startsWith("calor") || n.startsWith("prote")
    }

    val obligatorias = metricas.filter { esObligatoria(it) }
    val faltanObligatorias = obligatorias.any { (valores[it.id]?.toFloatOrNull() ?: 0f) <= 0f }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Agregar alimento") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del alimento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                metricas.forEach { m ->
                    val obligatoria = esObligatoria(m)
                    val texto = valores[m.id] ?: ""
                    val esError = intentoEnviar && obligatoria &&
                        (texto.toFloatOrNull() ?: 0f) <= 0f
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { nuevo ->
                            valores[m.id] = nuevo.filter { c -> c.isDigit() || c == '.' }
                        },
                        label = {
                            Text(
                                "${m.nombre} (${m.unidad})" + if (obligatoria) " *" else ""
                            )
                        },
                        singleLine = true,
                        isError = esError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    "* Campos obligatorios",
                    fontWeight = FontWeight.Light
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    intentoEnviar = true
                    if (faltanObligatorias) return@TextButton
                    val mapa = valores.mapNotNull { (id, v) ->
                        v.toFloatOrNull()?.let { id to it }
                    }.toMap()
                    onConfirmar(nombre, mapa)
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
