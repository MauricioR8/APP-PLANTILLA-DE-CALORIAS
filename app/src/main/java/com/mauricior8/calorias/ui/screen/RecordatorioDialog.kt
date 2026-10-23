package com.mauricior8.calorias.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.reminder.RecordatorioAgua

/** Activa o desactiva el recordatorio de agua y elige cada cuantas horas suena. */
@Composable
fun RecordatorioDialog(
    onAviso: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val ctx = LocalContext.current
    val inicial = remember { RecordatorioAgua.ajustes(ctx) }
    var activo by remember { mutableStateOf(inicial.activo) }
    var horas by remember { mutableIntStateOf(inicial.cadaHoras) }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            activo = true
        } else {
            activo = false
            onAviso("Sin permiso de notificaciones no puedo recordarte. Actívalo en Ajustes del teléfono.")
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Recordatorio de agua") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recordarme tomar agua")
                    Switch(
                        checked = activo,
                        onCheckedChange = { quiere ->
                            if (!quiere) {
                                activo = false
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !RecordatorioAgua.tienePermisoNotificaciones(ctx)
                            ) {
                                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                activo = true
                            }
                        }
                    )
                }
                Text("Cada", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 4).forEach { h ->
                        FilterChip(selected = horas == h, onClick = { horas = h }, label = { Text("$h h") })
                    }
                }
                Text(
                    "Solo entre las ${RecordatorioAgua.HORA_INICIO}:00 y las ${RecordatorioAgua.HORA_FIN}:00. " +
                        "Android puede adelantar o retrasar un poco el aviso para ahorrar batería.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                RecordatorioAgua.guardar(ctx, activo, horas)
                onAviso(if (activo) "Recordatorio cada $horas h activado" else "Recordatorio desactivado")
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
