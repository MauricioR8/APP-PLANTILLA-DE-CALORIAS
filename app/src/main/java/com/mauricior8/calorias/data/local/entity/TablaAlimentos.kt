package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Modelo relacional para las "Tablas de alimentos" tipo hoja de calculo.
 *
 * - [TablaAlimentos] : la tabla en si.
 * - [ColumnaTabla]   : categorias (max 10 por tabla).
 * - [FilaTabla]      : alimentos (max 100 por tabla).
 * - [CeldaTabla]     : valor en la interseccion fila x columna.
 */
@Entity(tableName = "tablas")
data class TablaAlimentos(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "columnas",
    foreignKeys = [
        ForeignKey(
            entity = TablaAlimentos::class,
            parentColumns = ["id"],
            childColumns = ["tablaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tablaId")]
)
data class ColumnaTabla(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tablaId: Int,
    val nombre: String,
    val orden: Int = 0
)

@Entity(
    tableName = "filas",
    foreignKeys = [
        ForeignKey(
            entity = TablaAlimentos::class,
            parentColumns = ["id"],
            childColumns = ["tablaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tablaId")]
)
data class FilaTabla(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tablaId: Int,
    val nombre: String,
    val orden: Int = 0
)

@Entity(
    tableName = "celdas",
    foreignKeys = [
        ForeignKey(
            entity = FilaTabla::class,
            parentColumns = ["id"],
            childColumns = ["filaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ColumnaTabla::class,
            parentColumns = ["id"],
            childColumns = ["columnaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("filaId"),
        Index("columnaId"),
        Index(value = ["filaId", "columnaId"], unique = true)
    ]
)
data class CeldaTabla(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tablaId: Int,
    val filaId: Int,
    val columnaId: Int,
    val valor: String = ""
)
