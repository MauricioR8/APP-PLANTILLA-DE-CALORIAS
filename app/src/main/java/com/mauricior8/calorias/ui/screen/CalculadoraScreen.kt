package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.util.hora

/**
 * Mini calculadora con teclado propio e historial persistente.
 *
 * @param onCalcular evalua la expresion; devuelve el resultado formateado o null.
 */
@Composable
fun CalculadoraScreen(
    historial: List<CalculoHistorial>,
    onCalcular: (String) -> String?,
    onLimpiarHistorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expresion by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf(false) }

    fun pulsar(s: String) {
        error = false
        expresion += s
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Pantalla.
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expresion.ifEmpty { " " },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (error) "Error" else resultado,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (error) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Teclado.
        val filas = listOf(
            listOf("C", "(", ")", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "<", "=")
        )
        filas.forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fila.forEach { tecla ->
                    FilledTonalButton(
                        onClick = {
                            when (tecla) {
                                "C" -> { expresion = ""; resultado = "0"; error = false }
                                "<" -> { if (expresion.isNotEmpty()) expresion = expresion.dropLast(1) }
                                "=" -> {
                                    val r = onCalcular(expresion)
                                    if (r == null) { error = true } else { resultado = r; error = false }
                                }
                                else -> pulsar(tecla)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(tecla, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Historial.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Historial",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (historial.isNotEmpty()) {
                TextButton(onClick = onLimpiarHistorial) { Text("Limpiar") }
            }
        }

        if (historial.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Sin calculos aun.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(historial, key = { it.id }) { calc ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${calc.expresion} = ${calc.resultado}")
                            Text(
                                hora(calc.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
