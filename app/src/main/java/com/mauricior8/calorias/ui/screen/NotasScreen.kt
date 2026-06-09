package com.mauricior8.calorias.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.util.fechaHora

/**
 * Recuadro de notas: el cuadro de escritura crece al escribir, y cada nota
 * guardada se puede abrir para leerla completa, editarla o eliminarla.
 */
@Composable
fun NotasScreen(
    notas: List<Nota>,
    onGuardar: (id: Int, texto: String) -> Unit,
    onEliminar: (Nota) -> Unit,
    modifier: Modifier = Modifier
) {
    var texto by remember { mutableStateOf("") }
    var notaAbierta by remember { mutableStateOf<Nota?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Escribe una nota...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onGuardar(0, texto)
                texto = ""
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Guardar nota")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Mis notas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (notas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aun no tienes notas guardadas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
            ) {
                items(notas, key = { it.id }) { nota ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { notaAbierta = nota }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    nota.texto,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    fechaHora(nota.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onEliminar(nota) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar nota",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    notaAbierta?.let { nota ->
        NotaEditorDialog(
            nota = nota,
            onGuardar = { nuevoTexto ->
                onGuardar(nota.id, nuevoTexto)
                notaAbierta = null
            },
            onEliminar = {
                onEliminar(nota)
                notaAbierta = null
            },
            onCerrar = { notaAbierta = null }
        )
    }
}

/** Dialogo para abrir una nota completa y editarla o eliminarla. */
@Composable
private fun NotaEditorDialog(
    nota: Nota,
    onGuardar: (String) -> Unit,
    onEliminar: () -> Unit,
    onCerrar: () -> Unit
) {
    var texto by remember { mutableStateOf(nota.texto) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nota") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(texto) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onEliminar) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
