package com.mauricior8.calorias.ui.state

import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.Rutina

/** Rutina que toca el dia seleccionado, con sus ejercicios en orden. */
data class RutinaDelDia(
    val rutina: Rutina,
    val ejercicios: List<EjercicioRutina>
)

/**
 * Resumen de la semana de entrenamiento.
 *
 * @param volumen   grupo muscular -> series hechas esta semana.
 * @param planeadas sesiones de fuerza que tocaban hasta hoy.
 * @param completas sesiones en las que se marcaron todas las series.
 */
data class SemanaGym(
    val volumen: Map<String, Int> = emptyMap(),
    val planeadas: Int = 0,
    val completas: Int = 0
)

/** Gasto energetico estimado con calorias reales y cambio de peso de las ultimas semanas. */
data class GastoReal(
    val kcalPromedio: Int,
    val kgPorSemana: Float,
    val gasto: Int,
    val diasConDatos: Int
)
