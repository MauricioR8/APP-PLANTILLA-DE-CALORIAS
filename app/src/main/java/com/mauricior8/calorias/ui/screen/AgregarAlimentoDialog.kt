package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.util.decodificarValores
import java.util.Locale

/**
 * Formulario para registrar un alimento (aporta a varias metricas) con un
 * historial reutilizable y editable.
 *
 * - Calorias y Proteinas son OBLIGATORIAS (si esas metricas existen).
 * - El resto de campos son opcionales.
 */
@Composable
fun AgregarAlimentoDialog(
    metricas: List<MetricaConfig>,
    alimentos: List<AlimentoGuardado>,
    onConfirmar: (nombre: String, valores: Map<String, Float>) -> Unit,
    onAplicar: (AlimentoGuardado) -> Unit,
    onEditarGuardado: (AlimentoGuardado, nombre: String, valores: Map<String, Float>) -> Unit,
    onEliminarGuardado: (AlimentoGuardado) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    val valores = remember { mutableStateMapOf<String, String>() }
    var intentoEnviar by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<AlimentoGuardado?>(null) }

    fun esObligatoria(m: MetricaConfig): Boolean {
        val n = m.nombre.lowercase(Locale.getDefault())
        return n.startsWith("calor") || n.startsWith("prote")
    }

    fun cargarParaEditar(a: AlimentoGuardado) {
        editando = a
        nombre = a.nombre
        valores.clear()
        decodificarValores(a.valores).forEach { (id, v) ->
            valores[id] = if (v % 1f == 0f) v.toInt().toString() else v.toString()
        }
    }

    fun limpiarFormulario() {
        editando = null
        nombre = ""
        valores.clear()
        intentoEnviar = false
    }

    val obligatorias = metricas.filter { esObligatoria(it) }
    val faltanObligatorias = obligatorias.any { (valores[it.id]?.toFloatOrNull() ?: 0f) <= 0f }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (editando == null) "Agregar alimento" else "Editar alimento") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
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
                            Text("${m.nombre} (${m.unidad})" + if (obligatoria) " *" else "")
                        },
                        singleLine = true,
                        isError = esError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("* Campos obligatorios", fontWeight = FontWeight.Light)

                if (alimentos.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        "Historial de alimentos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    alimentos.forEach { a ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(a.nombre, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onAplicar(a) }) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar al dia")
                            }
                            IconButton(onClick = { cargarParaEditar(a) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { onEliminarGuardado(a) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
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
                    val enEdicion = editando
                    if (enEdicion == null) {
                        onConfirmar(nombre, mapa)
                    } else {
                        onEditarGuardado(enEdicion, nombre, mapa)
                        limpiarFormulario()
                    }
                }
            ) { Text(if (editando == null) "Guardar" else "Actualizar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cerrar") }
        }
    )
}
