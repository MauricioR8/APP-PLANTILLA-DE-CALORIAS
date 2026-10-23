package com.mauricior8.calorias.ui.screen

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.reminder.RecordatorioAgua
import com.mauricior8.calorias.reminder.Recordatorios
import com.mauricior8.calorias.seguridad.Bloqueo
import com.mauricior8.calorias.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pestana "Mas": herramientas y ajustes. */
@Composable
fun MasScreen(
    viewModel: MainViewModel,
    onObjetivos: () -> Unit,
    onPedirConfirmacion: (titulo: String, mensaje: String, texto: String, accion: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var seccion by rememberSaveable { mutableStateOf("menu") }
    BackHandler(enabled = seccion != "menu") { seccion = "menu" }

    when (seccion) {
        "calculadora" -> ConAtras("Calculadora", { seccion = "menu" }, modifier) {
            val historial by viewModel.historialCalculos.collectAsStateWithLifecycle()
            CalculadoraScreen(
                historial = historial,
                onCalcular = viewModel::calcular,
                onLimpiarHistorial = viewModel::limpiarHistorialCalculos
            )
        }
        "tablas" -> ConAtras("Tablas de alimentos", { seccion = "menu" }, modifier) { TablasScreen(viewModel) }
        "ajustes" -> ConAtras("Ajustes", { seccion = "menu" }, modifier) {
            AjustesScreen(viewModel, onPedirConfirmacion)
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            item { Fila("Calculadora", "Suma tus porciones con historial") { seccion = "calculadora" } }
            item { Fila("Tablas de alimentos", "Tus tablas tipo hoja de cálculo") { seccion = "tablas" } }
            item { Fila("Calcular objetivos", "Calorías y macros según tu meta", onObjetivos) }
            item { Fila("Ajustes", "Tema, historial, bloqueo, recordatorios y respaldo") { seccion = "ajustes" } }
        }
    }
}

@Composable
private fun Fila(titulo: String, descripcion: String, onClick: () -> Unit) {
    Bloque(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold)
                Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun ConAtras(titulo: String, onAtras: () -> Unit, modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        contenido()
    }
}

// ---------------------------------------------------------------------------------------
// Ajustes
// ---------------------------------------------------------------------------------------

@Composable
private fun AjustesScreen(
    vm: MainViewModel,
    onPedirConfirmacion: (titulo: String, mensaje: String, texto: String, accion: () -> Unit) -> Unit
) {
    val ctx = LocalContext.current
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    var mostrarAgua by remember { mutableStateOf(false) }

    // Respaldo y CSV: el usuario elige donde guardar o de donde leer (Storage Access Framework).
    val guardarRespaldo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportar { json -> escribir(ctx, uri, json) }
    }
    val guardarCsvNutricion = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportarCsvNutricion { texto -> escribir(ctx, uri, texto) }
    }
    val guardarCsvGym = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportarCsvGym { texto -> escribir(ctx, uri, texto) }
    }
    val abrirRespaldo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val texto = try {
                ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
            if (texto == null) vm.avisar("No se pudo leer el archivo")
            else onPedirConfirmacion(
                "Restaurar respaldo",
                "Se reemplazarán TODOS tus datos actuales por los del archivo. Esta acción no se puede deshacer.",
                "Restaurar"
            ) { vm.importar(texto) }
        }
    }
    val fecha = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        item {
            Bloque(titulo = "Apariencia") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sistema" to "Sistema", "claro" to "Claro", "oscuro" to "Oscuro").forEach { (valor, texto) ->
                        FilterChip(
                            selected = (ajustes["tema"] ?: "sistema") == valor,
                            onClick = { vm.setAjuste("tema", valor) },
                            label = { Text(texto) }
                        )
                    }
                }
            }
        }
        item {
            Bloque(titulo = "La semana empieza el") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("domingo" to "Domingo", "lunes" to "Lunes").forEach { (valor, texto) ->
                        FilterChip(
                            selected = (ajustes["inicio_semana"] ?: "domingo") == valor,
                            onClick = { vm.setAjuste("inicio_semana", valor) },
                            label = { Text(texto) }
                        )
                    }
                }
                Text(
                    "Define el reinicio del historial semanal y la semana de entrenamiento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        item {
            Bloque(titulo = "Historial de comidas") {
                val actual = ajustes["retencion"] ?: "siempre"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("siempre" to "Siempre", "semanal" to "Semanal", "30dias" to "30 días").forEach { (valor, texto) ->
                        FilterChip(
                            selected = actual == valor,
                            onClick = {
                                if (valor == actual) return@FilterChip
                                if (valor == "siempre") vm.cambiarRetencion(valor)
                                else onPedirConfirmacion(
                                    "Activar historial ${if (valor == "semanal") "semanal" else "de 30 días"}",
                                    "Se borrará el detalle (nombres y horas de cada alimento) anterior a " +
                                        (if (valor == "semanal") "esta semana" else "los últimos 30 días") +
                                        ". Los totales de cada día se conservan, así que Progreso y tus gráficas " +
                                        "siguen funcionando. Conviene guardar antes un respaldo.",
                                    "Activar"
                                ) { vm.cambiarRetencion(valor) }
                            },
                            label = { Text(texto) }
                        )
                    }
                }
                Text(
                    when (actual) {
                        "semanal" -> "Cada inicio de semana se archiva el detalle de la semana anterior (solo quedan los totales por día)."
                        "30dias" -> "Se archiva el detalle con más de 30 días (solo quedan los totales por día)."
                        else -> "Se guarda todo el detalle de cada alimento para ver de dónde salió cada número."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        item {
            Bloque(titulo = "Seguridad") {
                val activo = ajustes["bloqueo"] == "1"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bloquear con huella o PIN")
                        Text(
                            "Se pide al abrir la app y tras 30 segundos en segundo plano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = activo,
                        onCheckedChange = { quiere ->
                            if (quiere && !Bloqueo.disponible(ctx)) {
                                vm.avisar("Configura primero una huella, un PIN o un patrón en el teléfono")
                            } else {
                                vm.setAjuste("bloqueo", if (quiere) "1" else "0")
                            }
                        }
                    )
                }
            }
        }
        item {
            Bloque(titulo = "Recordatorios") {
                FilaRecordatorio(Recordatorios.Tipo.ESPIRO_1, "Espirómetro · primera tanda", vm::avisar)
                FilaRecordatorio(Recordatorios.Tipo.ESPIRO_2, "Espirómetro · segunda tanda", vm::avisar)
                FilaRecordatorio(Recordatorios.Tipo.PESOS_MES, "Actualizar pesos (día 1 de cada mes)", vm::avisar)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { mostrarAgua = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Recordatorio de agua")
                }
            }
        }
        item {
            Bloque(titulo = "Tus datos") {
                OutlinedButton(onClick = { guardarRespaldo.launch("calorias-respaldo-$fecha.json") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Guardar respaldo")
                }
                OutlinedButton(
                    onClick = { abrirRespaldo.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Restaurar respaldo") }
                OutlinedButton(onClick = { guardarCsvNutricion.launch("calorias-nutricion-$fecha.csv") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Exportar CSV de nutrición") }
                OutlinedButton(onClick = { guardarCsvGym.launch("calorias-gym-$fecha.csv") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Exportar CSV de gym") }
                Text(
                    "Los archivos se guardan donde tú elijas; la app no los sube a ningún servidor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (mostrarAgua) RecordatorioDialog(onAviso = vm::avisar, onCerrar = { mostrarAgua = false })
}

private fun escribir(ctx: android.content.Context, uri: android.net.Uri, texto: String) {
    val salida = ctx.contentResolver.openOutputStream(uri, "wt") ?: throw java.io.IOException("No se pudo abrir el archivo")
    salida.use { it.write(texto.toByteArray(Charsets.UTF_8)) }
}

/** Interruptor + hora de un recordatorio a hora fija. */
@Composable
private fun FilaRecordatorio(tipo: Recordatorios.Tipo, etiqueta: String, onAviso: (String) -> Unit) {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(Recordatorios.config(ctx, tipo)) }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            Recordatorios.guardar(ctx, tipo, true, cfg.hora, cfg.minuto)
            cfg = cfg.copy(activo = true)
        } else {
            onAviso("Sin permiso de notificaciones no puedo avisarte. Actívalo en Ajustes del teléfono.")
        }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta)
            TextButton(onClick = {
                TimePickerDialog(ctx, { _, h, m ->
                    Recordatorios.guardar(ctx, tipo, cfg.activo, h, m)
                    cfg = cfg.copy(hora = h, minuto = m)
                }, cfg.hora, cfg.minuto, true).show()
            }) { Text("%02d:%02d".format(cfg.hora, cfg.minuto)) }
        }
        Switch(
            checked = cfg.activo,
            onCheckedChange = { quiere ->
                when {
                    !quiere -> {
                        Recordatorios.guardar(ctx, tipo, false, cfg.hora, cfg.minuto)
                        cfg = cfg.copy(activo = false)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !RecordatorioAgua.tienePermisoNotificaciones(ctx) ->
                        pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else -> {
                        Recordatorios.guardar(ctx, tipo, true, cfg.hora, cfg.minuto)
                        cfg = cfg.copy(activo = true)
                    }
                }
            }
        )
    }
}
