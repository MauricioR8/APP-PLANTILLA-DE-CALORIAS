package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.util.fechaHora
import com.mauricior8.calorias.util.formatoValor

/**
 * Muestra el historial de registros de una metrica concreta.
 */
@Composable
fun HistorialDialog(
    metrica: MetricaConfig,
    viewModel: MainViewModel,
    onCerrar: () -> Unit
) {
    val registros by viewModel.historial(metrica.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var registroPorEliminar by remember { mutableStateOf<RegistroSuma?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Historial: ${metrica.nombre}") },
        text = {
            if (registros.isEmpty()) {
                Text("Aun no hay registros para esta metrica.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(registros, key = { it.id }) { reg ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "+${formatoValor(reg.valor)} ${metrica.unidad}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    fechaHora(reg.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = { registroPorEliminar = reg }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar registro",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            val detalle = reg.detalle
                            if (!detalle.isNullOrBlank()) {
                                Text(
                                    detalle,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            HorizontalDivider(Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )

    registroPorEliminar?.let { reg ->
        ConfirmarDialog(
            titulo = "Eliminar registro",
            mensaje = "Se quitarán ${formatoValor(reg.valor)} ${metrica.unidad} " +
                "del ${fechaHora(reg.timestamp)}.",
            onConfirmar = { viewModel.eliminarRegistro(reg); registroPorEliminar = null },
            onCancelar = { registroPorEliminar = null }
        )
    }
}
