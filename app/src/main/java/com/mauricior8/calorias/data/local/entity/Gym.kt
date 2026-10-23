package com.mauricior8.calorias.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rutina de entrenamiento (ej: "Lunes y Jueves - Empuje").
 *
 * @param dias   Dias en que se hace, como lista ISO separada por comas (1=lunes ... 7=domingo).
 * @param diaria Si es true es un bloque que aplica a TODOS los dias (movilidad, respiracion...).
 */
@Entity(tableName = "rutinas")
data class Rutina(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val colorHex: String = "#DC2626",
    val dias: String = "",
    val diaria: Boolean = false,
    val nota: String? = null,
    val orden: Int = 0
)

/**
 * Ejercicio de una rutina. Todo el peso se expresa en kilos.
 *
 * @param grupo      Grupo muscular ("Pecho", "Espalda"...) o categoria ("Movilidad", "Respiración").
 * @param reps       Repeticiones por serie, o segundos si [esTiempo].
 * @param vecesPorDia Cuantas veces al dia se repite el bloque de series (ej: espirometro = 2).
 * @param pesoKg     Peso de trabajo actual; null = aun por definir.
 */
@Entity(
    tableName = "ejercicios_rutina",
    foreignKeys = [
        ForeignKey(
            entity = Rutina::class,
            parentColumns = ["id"],
            childColumns = ["rutinaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rutinaId")]
)
data class EjercicioRutina(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val rutinaId: Int,
    val nombre: String,
    val grupo: String = "Otro",
    val series: Int = 3,
    val reps: Int = 10,
    val esTiempo: Boolean = false,
    val descansoSeg: Int = 90,
    val vecesPorDia: Int = 1,
    val pesoKg: Float? = null,
    val nota: String? = null,
    val orden: Int = 0
)

/** Serie marcada como hecha un dia concreto (fecha "yyyy-MM-dd"; indice = 0..series*vecesPorDia-1). */
@Entity(
    tableName = "series_hechas",
    primaryKeys = ["ejercicioId", "fecha", "indice"],
    foreignKeys = [
        ForeignKey(
            entity = EjercicioRutina::class,
            parentColumns = ["id"],
            childColumns = ["ejercicioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ejercicioId"), Index("fecha")]
)
data class SerieHecha(
    val ejercicioId: Int,
    val fecha: String,
    val indice: Int
)

/** Cada vez que subes el peso de trabajo de un ejercicio se guarda aqui (historial y graficas). */
@Entity(
    tableName = "pesos_ejercicio",
    foreignKeys = [
        ForeignKey(
            entity = EjercicioRutina::class,
            parentColumns = ["id"],
            childColumns = ["ejercicioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ejercicioId")]
)
data class PesoEjercicio(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ejercicioId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val kg: Float
)

/** Nota libre de la sesion de un dia (molestias, sensaciones). */
@Entity(tableName = "sesiones_gym")
data class SesionGym(
    @PrimaryKey
    val fecha: String,
    val nota: String
)
