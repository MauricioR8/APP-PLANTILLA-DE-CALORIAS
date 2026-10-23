package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.Rutina
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.util.Gym

private val COLORES_RUTINA = listOf(
    "#DC2626", "#2563EB", "#EAB308", "#22C55E", "#A855F7", "#F97316", "#14B8A6", "#EC4899"
)
private val DIAS_CORTOS = listOf("L", "M", "X", "J", "V", "S", "D")

// ---------------------------------------------------------------------------------------
// Lista de rutinas
// ---------------------------------------------------------------------------------------

@Composable
fun RutinasView(viewModel: MainViewModel, onAbrir: (Int) -> Unit, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    val rutinas by viewModel.rutinas.collectAsStateWithLifecycle()
    val ejercicios by viewModel.ejercicios.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text("Mis rutinas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            items(rutinas, key = { it.id }) { r ->
                val n = ejercicios.count { it.rutinaId == r.id }
                Bloque(Modifier.clickable { onAbrir(r.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(colorRutina(r.colorHex)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.nombre, fontWeight = FontWeight.Bold)
                            Text(
                                "${Gym.textoDias(r)} · $n ejercicios",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                Button(onClick = { onAbrir(0) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nueva rutina")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Editor de una rutina (datos + ejercicios)
// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RutinaEditorView(viewModel: MainViewModel, rutinaId: Int, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    val rutinas by viewModel.rutinas.collectAsStateWithLifecycle()
    val ejercicios by viewModel.ejercicios.collectAsStateWithLifecycle()
    val existente = rutinas.firstOrNull { it.id == rutinaId }

    // Si la rutina que se estaba editando desaparece (se borro), se vuelve a la lista. Mientras
    // la lista aun no cargo (existente nunca fue visto) simplemente se espera.
    var vistaAntes by remember(rutinaId) { mutableStateOf(false) }
    if (existente != null) vistaAntes = true
    if (rutinaId != 0 && existente == null) {
        if (vistaAntes) LaunchedEffect(Unit) { onVolver() }
        return
    }

    var nombre by remember(existente?.id) { mutableStateOf(existente?.nombre ?: "") }
    var color by remember(existente?.id) { mutableStateOf(existente?.colorHex ?: COLORES_RUTINA.first()) }
    var dias by remember(existente?.id) { mutableStateOf(existente?.let { Gym.diasDe(it).toSet() } ?: emptySet()) }
    var diaria by remember(existente?.id) { mutableStateOf(existente?.diaria ?: false) }
    var nota by remember(existente?.id) { mutableStateOf(existente?.nota ?: "") }

    var ejercicioEnEdicion by remember { mutableStateOf<EjercicioRutina?>(null) }
    var nuevoEjercicio by remember { mutableStateOf(false) }
    var ejercicioPorBorrar by remember { mutableStateOf<EjercicioRutina?>(null) }
    var confirmarBorrarRutina by remember { mutableStateOf(false) }

    val lista = ejercicios.filter { it.rutinaId == rutinaId }.sortedBy { it.orden }

    fun construir() = Rutina(
        id = existente?.id ?: 0,
        nombre = nombre,
        colorHex = color,
        dias = dias.sorted().joinToString(","),
        diaria = diaria,
        nota = nota.trim().ifEmpty { null },
        orden = existente?.orden ?: 0
    )

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text(
                if (existente == null) "Nueva rutina" else "Editar rutina",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (existente != null) {
                IconButton(onClick = { confirmarBorrarRutina = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar rutina", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            item {
                Bloque(titulo = "Datos") {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        COLORES_RUTINA.forEach { hex ->
                            val elegido = hex == color
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(colorRutina(hex), CircleShape)
                                    .border(if (elegido) 3.dp else 0.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .clickable { color = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (elegido) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bloque diario (todos los días)", modifier = Modifier.weight(1f))
                        Switch(checked = diaria, onCheckedChange = { diaria = it })
                    }
                    if (!diaria) {
                        Text("Días", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DIAS_CORTOS.forEachIndexed { i, letra ->
                                val iso = i + 1
                                FilterChip(
                                    selected = iso in dias,
                                    onClick = { dias = if (iso in dias) dias - iso else dias + iso },
                                    label = { Text(letra) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        label = { Text("Nota (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.guardarRutina(construir())
                            if (existente == null) onVolver()
                        },
                        enabled = nombre.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (existente == null) "Crear rutina" else "Guardar cambios") }
                }
            }

            if (existente != null) {
                item {
                    Bloque(titulo = "Ejercicios (${lista.size})", acento = colorRutina(existente.colorHex)) {
                        if (lista.isEmpty()) {
                            Text("Aún no hay ejercicios.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        lista.forEachIndexed { i, ej ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(ej.nombre, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${ej.grupo} · ${ej.series} × ${ej.reps}${if (ej.esTiempo) " s" else ""}" +
                                            (ej.pesoKg?.let { " · ${Gym.kg(it)} kg" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.moverEjercicio(ej, -1) }, enabled = i > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                                }
                                IconButton(onClick = { viewModel.moverEjercicio(ej, 1) }, enabled = i < lista.lastIndex) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar")
                                }
                                IconButton(onClick = { ejercicioEnEdicion = ej }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { ejercicioPorBorrar = ej }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { nuevoEjercicio = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Agregar ejercicio")
                        }
                    }
                }
            }
        }
    }

    if (nuevoEjercicio && existente != null) {
        EjercicioEditorDialog(
            inicial = null,
            rutinaId = existente.id,
            onGuardar = { viewModel.guardarEjercicio(it); nuevoEjercicio = false },
            onCancelar = { nuevoEjercicio = false }
        )
    }
    ejercicioEnEdicion?.let { ej ->
        EjercicioEditorDialog(
            inicial = ej,
            rutinaId = ej.rutinaId,
            onGuardar = { viewModel.guardarEjercicio(it); ejercicioEnEdicion = null },
            onCancelar = { ejercicioEnEdicion = null }
        )
    }
    ejercicioPorBorrar?.let { ej ->
        ConfirmarDialog(
            titulo = "Eliminar ejercicio",
            mensaje = "Se borrará \"${ej.nombre}\" con su historial de pesos y series.",
            onConfirmar = { viewModel.eliminarEjercicio(ej); ejercicioPorBorrar = null },
            onCancelar = { ejercicioPorBorrar = null }
        )
    }
    if (confirmarBorrarRutina && existente != null) {
        ConfirmarDialog(
            titulo = "Eliminar rutina",
            mensaje = "Se borrará \"${existente.nombre}\" con todos sus ejercicios, pesos y series.",
            onConfirmar = { viewModel.eliminarRutina(existente); confirmarBorrarRutina = false },
            onCancelar = { confirmarBorrarRutina = false }
        )
    }
}

// ---------------------------------------------------------------------------------------
// Editor de un ejercicio
// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EjercicioEditorDialog(
    inicial: EjercicioRutina?,
    rutinaId: Int,
    onGuardar: (EjercicioRutina) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var grupo by remember { mutableStateOf(inicial?.grupo ?: Gym.GRUPOS_FUERZA.first()) }
    var series by remember { mutableStateOf((inicial?.series ?: 3).toString()) }
    var reps by remember { mutableStateOf((inicial?.reps ?: 10).toString()) }
    var esTiempo by remember { mutableStateOf(inicial?.esTiempo ?: false) }
    var descanso by remember { mutableStateOf((inicial?.descansoSeg ?: 90).toString()) }
    var veces by remember { mutableStateOf((inicial?.vecesPorDia ?: 1).toString()) }
    var peso by remember { mutableStateOf(inicial?.pesoKg?.let { Gym.kg(it) } ?: "") }
    var nota by remember { mutableStateOf(inicial?.nota ?: "") }

    fun entero(t: String, min: Int, max: Int, defecto: Int) = (t.toIntOrNull() ?: defecto).coerceIn(min, max)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nuevo ejercicio" else "Editar ejercicio") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Grupo", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Gym.TODOS_LOS_GRUPOS.forEach { g ->
                        FilterChip(selected = grupo == g, onClick = { grupo = g }, label = { Text(g) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumeroCampo("Series", series, Modifier.weight(1f)) { series = it }
                    NumeroCampo(if (esTiempo) "Segundos" else "Reps", reps, Modifier.weight(1f)) { reps = it }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = esTiempo, onCheckedChange = { esTiempo = it })
                    Text("Se mide por tiempo (segundos)")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumeroCampo("Descanso (s)", descanso, Modifier.weight(1f)) { descanso = it }
                    NumeroCampo("Veces al día", veces, Modifier.weight(1f)) { veces = it }
                }
                OutlinedTextField(
                    value = peso,
                    onValueChange = { peso = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Peso de trabajo (kg). Vacío = por definir") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = nombre.isNotBlank(),
                onClick = {
                    onGuardar(
                        (inicial ?: EjercicioRutina(rutinaId = rutinaId, nombre = "")).copy(
                            nombre = nombre,
                            grupo = grupo,
                            series = entero(series, 1, 20, 3),
                            reps = entero(reps, 1, if (esTiempo) 3600 else 200, 10),
                            esTiempo = esTiempo,
                            descansoSeg = entero(descanso, 0, 900, 90),
                            vecesPorDia = entero(veces, 1, 10, 1),
                            pesoKg = peso.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f },
                            nota = nota.trim().ifEmpty { null }
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun NumeroCampo(etiqueta: String, valor: String, modifier: Modifier, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(it.filter(Char::isDigit).take(4)) },
        label = { Text(etiqueta, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
