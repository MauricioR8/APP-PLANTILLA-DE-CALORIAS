package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Nota libre escrita por el usuario.
 *
 * @param id        Identificador autoincremental.
 * @param texto     Contenido de la nota.
 * @param timestamp Momento de creacion en milisegundos epoch.
 */
@Entity(tableName = "notas")
data class Nota(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val texto: String,
    val timestamp: Long = System.currentTimeMillis()
)
