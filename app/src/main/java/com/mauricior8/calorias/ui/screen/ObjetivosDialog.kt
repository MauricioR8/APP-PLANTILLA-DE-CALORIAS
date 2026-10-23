package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mauricior8.calorias.util.Actividad
import com.mauricior8.calorias.util.MetaPeso
import com.mauricior8.calorias.util.Objetivos
import com.mauricior8.calorias.util.ObjetivosDiarios
import com.mauricior8.calorias.util.Perfil
import com.mauricior8.calorias.util.PerfilBase
import com.mauricior8.calorias.util.Sexo
import java.util.Locale

/**
 * Calcula calorias y macros diarios (Mifflin-St Jeor) y permite aplicarlos como
 * limite de las metricas correspondientes.
 *
 * @param pesoInicial ultimo peso registrado (kg), si existe.
 */
@Composable
fun ObjetivosDialog(
    pesoInicial: Float?,
    perfilGuardado: PerfilBase?,
    onAplicar: (ObjetivosDiarios, Perfil) -> Unit,
    onCerrar: () -> Unit
) {
    var sexo by rememberSaveable { mutableStateOf(perfilGuardado?.sexo ?: Sexo.HOMBRE) }
    var actividad by rememberSaveable { mutableStateOf(perfilGuardado?.actividad ?: Actividad.MODERADA) }
    var meta by rememberSaveable { mutableStateOf(perfilGuardado?.meta ?: MetaPeso.MANTENER) }
    var edad by rememberSaveable { mutableStateOf(perfilGuardado?.edad?.toString() ?: "") }
    var peso by rememberSaveable {
        mutableStateOf(pesoInicial?.let { String.format(Locale.US, "%.1f", it) } ?: "")
    }
    var estatura by rememberSaveable {
        mutableStateOf(
            perfilGuardado?.estaturaCm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        )
    }
    var proteinaPorKg by remember { mutableFloatStateOf(meta.proteinaPorKg.toFloat()) }

    val perfil = remember(sexo, actividad, meta, edad, peso, estatura) {
        val e = edad.toIntOrNull()
        val p = peso.replace(',', '.').toDoubleOrNull()
        val h = estatura.replace(',', '.').toDoubleOrNull()
        if (e == null || p == null || h == null) null else Perfil(sexo, e, p, h, actividad, meta)
    }
    val error = perfil?.let { Objetivos.validar(it) }
    val resultado = if (perfil != null && error == null) Objetivos.calcular(perfil, proteinaPorKg.toDouble()) else null

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Calcular objetivos") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Etiqueta("Sexo")
                Fila { Sexo.entries.forEach { s -> FilterChip(sexo == s, { sexo = s }, { Text(s.etiqueta) }) } }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Campo("Edad", edad, Modifier.weight(1f)) { edad = it.filter(Char::isDigit).take(3) }
                    Campo("Peso (kg)", peso, Modifier.weight(1f)) { peso = limpiarDecimal(it) }
                    Campo("Estatura (cm)", estatura, Modifier.weight(1f)) { estatura = limpiarDecimal(it) }
                }

                Etiqueta("Actividad física")
                Fila { Actividad.entries.forEach { a -> FilterChip(actividad == a, { actividad = a }, { Text(a.etiqueta) }) } }

                Etiqueta("Meta")
                Fila {
                    MetaPeso.entries.forEach { m ->
                        FilterChip(meta == m, { meta = m; proteinaPorKg = m.proteinaPorKg.toFloat() }, { Text(m.etiqueta) })
                    }
                }

                Etiqueta("Proteína: ${String.format(Locale.getDefault(), "%.1f", proteinaPorKg)} g por kg")
                Slider(
                    value = proteinaPorKg,
                    onValueChange = { proteinaPorKg = Math.round(it * 10) / 10f },
                    valueRange = 1.2f..3.0f,
                    steps = 17
                )

                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                } else if (resultado == null) {
                    Text(
                        "Completa edad, peso y estatura para ver el resultado.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Resultado(resultado)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = resultado != null,
                onClick = {
                    if (resultado != null && perfil != null) onAplicar(resultado, perfil)
                    onCerrar()
                }
            ) { Text("Aplicar a mis metas") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

@Composable
private fun Resultado(o: ObjetivosDiarios) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${o.kcal} kcal al día", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Metabolismo basal ${o.tmb} kcal · gasto total ${o.gastoTotal} kcal",
                style = MaterialTheme.typography.bodySmall)
            Text("Proteínas ${o.proteinaG} g · Carbohidratos ${o.carbohidratosG} g · Grasas ${o.grasasG} g")
            Text("Fibra ${o.fibraG} g · Agua ${String.format(Locale.getDefault(), "%.1f", o.aguaL)} L")
            Text(
                "Es una estimación de partida: ajústala según cómo evolucione tu peso.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun Etiqueta(texto: String) =
    Text(texto, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)

@Composable
private fun Fila(contenido: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { contenido() }
}

@Composable
private fun Campo(etiqueta: String, valor: String, modifier: Modifier, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

private fun limpiarDecimal(texto: String) = texto.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
