package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Ajuste de la app como par clave/valor (tema, retencion del historial, perfil...). */
@Entity(tableName = "ajustes")
data class Ajuste(
    @PrimaryKey
    val clave: String,
    val valor: String
)

/** Suplemento del checklist diario (creatina, proteina...). */
@Entity(tableName = "suplementos")
data class Suplemento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val orden: Int = 0
)

/** Suplemento marcado como tomado un dia concreto ("yyyy-MM-dd"). */
@Entity(
    tableName = "suplementos_tomados",
    primaryKeys = ["suplementoId", "fecha"],
    foreignKeys = [
        ForeignKey(
            entity = Suplemento::class,
            parentColumns = ["id"],
            childColumns = ["suplementoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("suplementoId")]
)
data class SuplementoTomado(
    val suplementoId: Int,
    val fecha: String
)

/** Medida corporal en centimetros ("Cintura", "Brazo", "Pierna", "Pecho", "Cadera"). */
@Entity(tableName = "medidas")
data class Medida(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tipo: String,
    val cm: Float
)

/**
 * Total de una metrica en un dia cuyo detalle ya se archivo (modo historial semanal).
 * Conserva los totales para que Progreso siga funcionando aunque se borre el detalle.
 */
@Entity(tableName = "totales_diarios", primaryKeys = ["fecha", "metricaId"])
data class TotalDiario(
    val fecha: String,
    val metricaId: String,
    val total: Float
)
