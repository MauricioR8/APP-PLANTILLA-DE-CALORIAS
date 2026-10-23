package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Medicion de peso corporal.
 *
 * @param id        Identificador autoincremental.
 * @param timestamp Momento de la medicion en milisegundos epoch.
 * @param kg        Peso en kilogramos.
 */
@Entity(tableName = "peso")
data class PesoRegistro(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val kg: Float
)
