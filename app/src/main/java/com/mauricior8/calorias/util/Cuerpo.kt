package com.mauricior8.calorias.util

import kotlin.math.roundToInt

/** Estadistica de peso corporal y gasto energetico (sin Android, para probar en la JVM). */
object Cuerpo {

    private const val DIA_MS = 24L * 60 * 60 * 1000

    /** Kilocalorias por kg de peso corporal ganado o perdido (aproximacion habitual). */
    const val KCAL_POR_KG = 7700.0

    /** Media movil: para cada medicion, promedio de las de los [ventanaDias] dias anteriores. */
    fun mediaMovil(puntos: List<Pair<Long, Float>>, ventanaDias: Int = 7): List<Float> =
        puntos.map { (t, _) ->
            val ventana = puntos.filter { it.first <= t && it.first > t - ventanaDias * DIA_MS }
            ventana.map { it.second }.average().toFloat()
        }

    /**
     * Ritmo de cambio en kg por semana con regresion lineal sobre los ultimos [dias] dias.
     * Devuelve null si hay menos de 3 mediciones o abarcan menos de 5 dias.
     */
    fun ritmoKgPorSemana(puntos: List<Pair<Long, Float>>, dias: Int = 14): Float? {
        if (puntos.isEmpty()) return null
        val fin = puntos.maxOf { it.first }
        val recientes = puntos.filter { it.first >= fin - dias * DIA_MS }
        if (recientes.size < 3) return null
        val t0 = recientes.minOf { it.first }
        val x = recientes.map { (it.first - t0).toDouble() / DIA_MS }
        if (x.max() < 5.0) return null
        val y = recientes.map { it.second.toDouble() }
        val mx = x.average()
        val my = y.average()
        val den = x.sumOf { (it - mx) * (it - mx) }
        if (den == 0.0) return null
        val pendiente = x.indices.sumOf { (x[it] - mx) * (y[it] - my) } / den   // kg por dia
        return (pendiente * 7).toFloat()
    }

    /**
     * Gasto energetico total estimado: lo que comiste de media menos lo que "sobra" o "falta"
     * segun el cambio real de peso. Ej: comes 2800 y subes 0.3 kg/sem -> gasto ~ 2800 - 330 = 2470.
     */
    fun gastoEstimado(kcalPromedioDia: Double, kgPorSemana: Float): Int =
        (kcalPromedioDia - kgPorSemana / 7.0 * KCAL_POR_KG).roundToInt()
}
