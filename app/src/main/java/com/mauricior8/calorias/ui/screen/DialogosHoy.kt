package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.Suplemento
import com.mauricior8.calorias.util.Comidas
import com.mauricior8.calorias.util.formatoValor
import com.mauricior8.calorias.util.hora

/**
 * Detalle del dia: de donde salio cada numero. Agrupa los registros por comida y, dentro de
 * cada una, por alimento (un alimento reparte su aporte entre varias metricas).
 */
@Composable
fun DetalleDiaDialog(
    etiquetaDia: String,
    registros: List<RegistroSuma>,
    metricas: List<MetricaConfig>,
    onCerrar: () -> Unit
) {
    val porId = metricas.associateBy { it.id }
    val sinComida = "Sin clasificar"
    val grupos = (Comidas.LISTA + sinComida).mapNotNull { comida ->
        val regs = registros.filter { (it.comida ?: sinComida) == comida }
        if (regs.isEmpty()) null else comida to regs
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Detalle de $etiquetaDia") },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (grupos.isEmpty()) {
                    Text(
                        "No hay detalle para este día. Si activaste el historial semanal, el detalle de " +
                            "semanas anteriores se archiva y solo se conservan los totales.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                grupos.forEach { (comida, regs) ->
                    Text(comida, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    // Mismo alimento y misma hora = un solo renglon con todos sus aportes.
                    regs.groupBy { it.timestamp to (it.detalle ?: "Registro manual") }.forEach { (clave, items) ->
                        Column(Modifier.padding(vertical = 2.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(clave.second, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(hora(clave.first), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                items.joinToString(" · ") { r ->
                                    val m = porId[r.metricaId]
                                    "${m?.nombre ?: r.metricaId} ${formatoValor(r.valor)} ${m?.unidad ?: ""}".trim()
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

/** Lista editable de suplementos del checklist diario. */
@Composable
fun SuplementosDialog(
    suplementos: List<Suplemento>,
    onAgregar: (String) -> Unit,
    onEliminar: (Suplemento) -> Unit,
    onCerrar: () -> Unit
) {
    var nuevo by remember { mutableStateOf("") }
    var porBorrar by remember { mutableStateOf<Suplemento?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Mis suplementos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suplementos.forEach { s ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.nombre, modifier = Modifier.weight(1f))
                        IconButton(onClick = { porBorrar = s }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nuevo,
                        onValueChange = { nuevo = it },
                        label = { Text("Nuevo suplemento") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(enabled = nuevo.isNotBlank(), onClick = { onAgregar(nuevo); nuevo = "" }) { Text("Agregar") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Listo") } }
    )

    porBorrar?.let { s ->
        ConfirmarDialog(
            titulo = "Eliminar suplemento",
            mensaje = "Se quitará \"${s.nombre}\" y su historial de tomas.",
            onConfirmar = { onEliminar(s); porBorrar = null },
            onCancelar = { porBorrar = null }
        )
    }
}
