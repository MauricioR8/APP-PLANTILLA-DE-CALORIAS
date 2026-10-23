package com.mauricior8.calorias.util

import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.Rutina
import java.util.Calendar
import kotlin.math.roundToInt

/** Zona de volumen semanal de un grupo muscular (series por semana). */
enum class ZonaVolumen(val etiqueta: String) {
    BAJO("Bajo"), OPTIMO("Óptimo"), ALTO("Alto")
}

/** Logica pura del modulo de gym (sin Android) para poder probarla en la JVM. */
object Gym {

    val GRUPOS_FUERZA = listOf(
        "Pecho", "Espalda", "Hombro", "Bíceps", "Tríceps",
        "Cuádriceps", "Femoral", "Glúteo", "Gemelo", "Abdomen"
    )

    /** Categorias que no cuentan como volumen de fuerza. */
    val CATEGORIAS_NO_FUERZA = setOf("Movilidad", "Respiración")

    val TODOS_LOS_GRUPOS = GRUPOS_FUERZA + CATEGORIAS_NO_FUERZA + "Otro"

    /** Rango de series semanales por grupo recomendado para hipertrofia. */
    const val SERIES_MIN = 10
    const val SERIES_MAX = 20

    /** Dia ISO: 1 = lunes ... 7 = domingo. */
    fun diaIso(c: Calendar): Int = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1

    fun diasDe(rutina: Rutina): List<Int> =
        rutina.dias.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.sorted()

    fun aplicaEn(rutina: Rutina, diaIso: Int): Boolean = rutina.diaria || diaIso in diasDe(rutina)

    private val NOMBRES_DIA = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    /** "1,4" -> "Lun y Jue"; "1,3,5" -> "Lun, Mié y Vie". */
    fun textoDias(rutina: Rutina): String {
        if (rutina.diaria) return "Todos los días"
        val d = diasDe(rutina).map { NOMBRES_DIA[it - 1] }
        return when (d.size) {
            0 -> "Sin días"
            1 -> d[0]
            else -> d.dropLast(1).joinToString(", ") + " y " + d.last()
        }
    }

    /** Total de series a marcar en un dia (series x veces al dia). */
    fun totalSeries(e: EjercicioRutina): Int = e.series * e.vecesPorDia

    fun hechas(e: EjercicioRutina, indices: Set<Int>): Int =
        (0 until totalSeries(e)).count { it in indices }

    fun completo(e: EjercicioRutina, indices: Set<Int>): Boolean =
        totalSeries(e) > 0 && hechas(e, indices) == totalSeries(e)

    fun zona(seriesSemana: Int): ZonaVolumen = when {
        seriesSemana < SERIES_MIN -> ZonaVolumen.BAJO
        seriesSemana > SERIES_MAX -> ZonaVolumen.ALTO
        else -> ZonaVolumen.OPTIMO
    }

    /**
     * Sugerencia de subir peso: si completaste todas las series en [minimoSesiones] sesiones desde
     * el ultimo cambio de carga, propone +5 % redondeado a 0.5 kg (minimo +0.5 kg).
     * @return el nuevo peso sugerido o null si aun no toca.
     */
    fun sugerirSubida(pesoActual: Float?, sesionesCompletas: Int, minimoSesiones: Int = 6): Float? {
        if (pesoActual == null || pesoActual <= 0f || sesionesCompletas < minimoSesiones) return null
        val incremento = maxOf(0.5f, (pesoActual * 0.05f * 2).roundToInt() / 2f)
        return pesoActual + incremento
    }

    /** "17.5" / "40" sin decimales sobrantes. */
    fun kg(valor: Float): String =
        if (valor % 1f == 0f) valor.toInt().toString() else String.format(java.util.Locale.US, "%.1f", valor)
}
