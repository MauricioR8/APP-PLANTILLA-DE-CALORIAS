package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estado de un dia concreto (clave = fecha "yyyy-MM-dd").
 *
 * @param fecha      Fecha en formato ISO corto, sirve de PK.
 * @param completado Si el usuario marco el dia como completado.
 */
@Entity(tableName = "estados_dia")
data class EstadoDia(
    @PrimaryKey
    val fecha: String,
    val completado: Boolean = false
)
