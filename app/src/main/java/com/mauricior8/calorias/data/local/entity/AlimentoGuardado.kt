package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Alimento guardado en el historial (reutilizable y editable).
 *
 * @param id      Identificador autoincremental.
 * @param nombre  Nombre del alimento.
 * @param valores Aportes por metrica codificados como "metricaId=valor;metricaId=valor".
 * @param timestamp Momento de creacion/edicion.
 */
@Entity(tableName = "alimentos_guardados")
data class AlimentoGuardado(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val valores: String,
    val timestamp: Long = System.currentTimeMillis()
)
