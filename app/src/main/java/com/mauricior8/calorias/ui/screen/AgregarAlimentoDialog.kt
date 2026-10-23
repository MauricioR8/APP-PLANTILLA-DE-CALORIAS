package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.remote.AlimentoRemoto
import com.mauricior8.calorias.util.Comidas
import com.mauricior8.calorias.util.claveMetrica
import com.mauricior8.calorias.util.decodificarValores
import kotlinx.coroutines.launch
import java.util.Calendar
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
    onConfirmar: (nombre: String, valores: Map<String, Float>, comida: String) -> Unit,
    onAplicar: (AlimentoGuardado, comida: String, porcion: Float) -> Unit,
    onEditarGuardado: (AlimentoGuardado, nombre: String, valores: Map<String, Float>) -> Unit,
    onEliminarGuardado: (AlimentoGuardado) -> Unit,
    onFavorito: (AlimentoGuardado) -> Unit,
    onCombinar: (nombre: String, alimentos: List<AlimentoGuardado>) -> Unit,
    onGuardarNota: (String) -> Unit,
    onBuscar: suspend (String) -> List<AlimentoRemoto>,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    val valores = remember { mutableStateMapOf<String, String>() }
    var intentoEnviar by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<AlimentoGuardado?>(null) }
    var guardarComoNota by remember { mutableStateOf(false) }
    var porcion by remember { mutableFloatStateOf(1f) }
    var combinar by remember { mutableStateOf(setOf<Int>()) }
    var nombreCombinacion by remember { mutableStateOf("") }
    var alimentoPorBorrar by remember { mutableStateOf<AlimentoGuardado?>(null) }
    var comida by remember { mutableStateOf(Comidas.sugerida(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))) }

    // Busqueda en Open Food Facts.
    val scope = rememberCoroutineScope()
    var consulta by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<AlimentoRemoto>?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var remoto by remember { mutableStateOf<AlimentoRemoto?>(null) }
    var gramos by remember { mutableStateOf("100") }

    fun formatear(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.1f", v)

    /** Rellena los campos con los valores del alimento remoto para la cantidad indicada. */
    fun aplicarRemoto(a: AlimentoRemoto, g: Double) {
        metricas.forEach { m ->
            val clave = claveMetrica(m.nombre) ?: return@forEach
            a.aporte(clave, g)?.let { valores[m.id] = formatear(it) }
        }
    }

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

                Text("Comida", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Comidas.LISTA.forEach { c ->
                        FilterChip(selected = comida == c, onClick = { comida = c }, label = { Text(c) })
                    }
                }

                if (editando == null) {
                    Text(
                        "Buscar alimento (Open Food Facts)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = consulta,
                            onValueChange = { consulta = it },
                            label = { Text("Ej: avena, pechuga, yogur") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            enabled = !buscando && consulta.trim().length >= 2,
                            onClick = {
                                buscando = true
                                scope.launch {
                                    resultados = onBuscar(consulta)
                                    buscando = false
                                }
                            }
                        ) { Icon(Icons.Default.Search, contentDescription = "Buscar") }
                    }
                    if (buscando) Text("Buscando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    resultados?.let { lista ->
                        if (lista.isEmpty() && !buscando) {
                            Text("Sin resultados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        lista.forEach { a ->
                            TextButton(
                                onClick = {
                                    remoto = a
                                    gramos = "100"
                                    nombre = a.nombre
                                    aplicarRemoto(a, 100.0)
                                    resultados = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(a.nombre, maxLines = 1)
                                    Text(
                                        listOf(a.marca, "${formatear(a.kcal)} kcal/100 g")
                                            .filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    remoto?.let { a ->
                        OutlinedTextField(
                            value = gramos,
                            onValueChange = { nuevo ->
                                gramos = nuevo.filter { c -> c.isDigit() || c == '.' }
                                gramos.toDoubleOrNull()?.takeIf { it > 0 }?.let { aplicarRemoto(a, it) }
                            },
                            label = { Text("Cantidad de \"${a.nombre}\" (g)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 2.dp))
                }

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

                if (editando == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = guardarComoNota,
                            onCheckedChange = { guardarComoNota = it }
                        )
                        Text("Guardar también como nota (editable en Notas)")
                    }
                }

                if (alimentos.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        "Historial de alimentos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Porción al agregar", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5f, 1f, 1.5f, 2f).forEach { p ->
                            FilterChip(
                                selected = porcion == p,
                                onClick = { porcion = p },
                                label = { Text("×${if (p % 1f == 0f) p.toInt().toString() else p.toString()}") }
                            )
                        }
                    }
                    alimentos.forEach { a ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = a.id in combinar,
                                onCheckedChange = { combinar = if (it) combinar + a.id else combinar - a.id }
                            )
                            Text(a.nombre, modifier = Modifier.weight(1f), maxLines = 2)
                            IconButton(onClick = { onFavorito(a) }) {
                                Icon(
                                    if (a.favorito) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (a.favorito) "Quitar de favoritos" else "Marcar favorito",
                                    tint = if (a.favorito) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onAplicar(a, comida, porcion) }) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar al día")
                            }
                            IconButton(onClick = { cargarParaEditar(a) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { alimentoPorBorrar = a }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    if (combinar.size >= 2) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text("Combinar ${combinar.size} alimentos en una comida", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = nombreCombinacion,
                            onValueChange = { nombreCombinacion = it },
                            label = { Text("Nombre (ej: Desayuno estándar)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            enabled = nombreCombinacion.isNotBlank(),
                            onClick = {
                                onCombinar(nombreCombinacion, alimentos.filter { it.id in combinar })
                                combinar = emptySet()
                                nombreCombinacion = ""
                            }
                        ) { Text("Guardar combinación") }
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
                        if (guardarComoNota) {
                            val resumen = metricas.mapNotNull { m ->
                                mapa[m.id]?.let { "${m.nombre}: ${if (it % 1f == 0f) it.toInt() else it} ${m.unidad}" }
                            }.joinToString(", ")
                            val nombreNota = nombre.trim().ifEmpty { "Alimento" }
                            onGuardarNota("$nombreNota - $resumen")
                        }
                        onConfirmar(nombre, mapa, comida)
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

    alimentoPorBorrar?.let { a ->
        ConfirmarDialog(
            titulo = "Eliminar alimento",
            mensaje = "Se quitará \"${a.nombre}\" de tu historial de alimentos.",
            onConfirmar = { onEliminarGuardado(a); alimentoPorBorrar = null },
            onCancelar = { alimentoPorBorrar = null }
        )
    }
}
