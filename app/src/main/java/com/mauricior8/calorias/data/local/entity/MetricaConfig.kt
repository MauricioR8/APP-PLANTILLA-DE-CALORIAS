package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Configuracion de una metrica de salud (categoria) creada por el usuario.
 *
 * @param id        Identificador unico (PK). Se usa un String para poder generar
 *                  ids deterministas (ej: UUID o slug del nombre).
 * @param nombre    Nombre visible de la metrica (ej: "Calorias", "Proteinas").
 * @param unidad    Unidad de medida: "kcal", "gr", "mg", "L".
 * @param limiteMaximo  Limite/objetivo maximo diario opcional. Si es null no hay limite.
 * @param colorHex  Color asociado para la UI en formato "#RRGGBB".
 */
@Entity(tableName = "metricas")
data class MetricaConfig(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val unidad: String,
    val limiteMaximo: Float? = null,
    val colorHex: String = "#FF6D00"
)
