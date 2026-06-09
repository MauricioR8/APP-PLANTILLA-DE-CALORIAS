package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entrada del historial de la calculadora.
 *
 * @param id        Identificador autoincremental.
 * @param expresion Expresion introducida por el usuario (ej: "120+45*2").
 * @param resultado Resultado calculado, ya formateado.
 * @param timestamp Momento del calculo en milisegundos epoch.
 */
@Entity(tableName = "calculos")
data class CalculoHistorial(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val expresion: String,
    val resultado: String,
    val timestamp: Long = System.currentTimeMillis()
)
