package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa un ingreso diario de valor para una metrica concreta.
 *
 * @param id        Identificador autoincremental (PK).
 * @param metricaId FK hacia [MetricaConfig.id].
 * @param valor     Valor numerico ingresado.
 * @param timestamp Momento del registro en milisegundos (epoch).
 * @param tipo      "manual" o "automatico".
 * @param detalle   Texto opcional (ej: "Desayuno", "Vaso de agua").
 */
@Entity(
    tableName = "registros",
    foreignKeys = [
        ForeignKey(
            entity = MetricaConfig::class,
            parentColumns = ["id"],
            childColumns = ["metricaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("metricaId"), Index("timestamp")]
)
data class RegistroSuma(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val metricaId: String,
    val valor: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val tipo: String = "manual",
    val detalle: String? = null
)
