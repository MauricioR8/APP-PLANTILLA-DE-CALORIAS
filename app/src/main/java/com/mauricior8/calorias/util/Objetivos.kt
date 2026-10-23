package com.mauricior8.calorias.util

import kotlin.math.max
import kotlin.math.roundToInt

enum class Sexo(val etiqueta: String) { HOMBRE("Hombre"), MUJER("Mujer") }

enum class Actividad(val etiqueta: String, val factor: Double) {
    SEDENTARIO("Sedentario", 1.2),
    LIGERA("Ligera (1-3 días/sem)", 1.375),
    MODERADA("Moderada (3-5 días/sem)", 1.55),
    ALTA("Alta (6-7 días/sem)", 1.725),
    MUY_ALTA("Muy alta (doble sesión)", 1.9)
}

/** Meta de composicion corporal: ajuste calorico y proteina sugerida (g por kg). */
enum class MetaPeso(val etiqueta: String, val ajuste: Double, val proteinaPorKg: Double) {
    DEFINICION("Definición (−20 %)", -0.20, 2.2),
    MANTENER("Mantener", 0.0, 1.8),
    HIPERTROFIA("Hipertrofia (+10 %)", 0.10, 2.0)
}

data class Perfil(
    val sexo: Sexo,
    val edad: Int,
    val pesoKg: Double,
    val estaturaCm: Double,
    val actividad: Actividad,
    val meta: MetaPeso
)

/** Perfil sin el peso (que sale del ultimo registro de peso corporal), para guardarlo en ajustes. */
data class PerfilBase(
    val sexo: Sexo,
    val edad: Int,
    val estaturaCm: Double,
    val actividad: Actividad,
    val meta: MetaPeso
) {
    fun conPeso(pesoKg: Double) = Perfil(sexo, edad, pesoKg, estaturaCm, actividad, meta)
}

data class ObjetivosDiarios(
    val tmb: Int,
    val gastoTotal: Int,
    val kcal: Int,
    val proteinaG: Int,
    val carbohidratosG: Int,
    val grasasG: Int,
    val fibraG: Int,
    val aguaL: Double
)

/**
 * Objetivos diarios con Mifflin-St Jeor (TMB), factor de actividad y ajuste segun la meta.
 * Es una estimacion de partida: conviene ajustarla segun la evolucion real del peso.
 */
object Objetivos {

    /** Devuelve un mensaje de error si el perfil tiene valores fuera de rango, o null. */
    fun validar(p: Perfil): String? = when {
        p.edad !in 14..100 -> "La edad debe estar entre 14 y 100 años"
        p.pesoKg !in 30.0..300.0 -> "El peso debe estar entre 30 y 300 kg"
        p.estaturaCm !in 100.0..250.0 -> "La estatura debe estar entre 100 y 250 cm"
        else -> null
    }

    fun calcular(p: Perfil, proteinaPorKg: Double = p.meta.proteinaPorKg): ObjetivosDiarios {
        val base = 10 * p.pesoKg + 6.25 * p.estaturaCm - 5 * p.edad
        val tmb = base + if (p.sexo == Sexo.HOMBRE) 5 else -161
        val gasto = tmb * p.actividad.factor
        val kcal = gasto * (1 + p.meta.ajuste)

        val proteina = proteinaPorKg * p.pesoKg
        // Grasas: 25 % de las kcal, sin bajar de 0.8 g/kg (minimo para salud hormonal).
        val grasas = max(kcal * 0.25 / 9, 0.8 * p.pesoKg)
        val carbos = max((kcal - proteina * 4 - grasas * 9) / 4, 0.0)

        return ObjetivosDiarios(
            tmb = tmb.roundToInt(),
            gastoTotal = gasto.roundToInt(),
            kcal = kcal.roundToInt(),
            proteinaG = proteina.roundToInt(),
            carbohidratosG = carbos.roundToInt(),
            grasasG = grasas.roundToInt(),
            fibraG = (kcal / 1000 * 14).roundToInt(),
            aguaL = (p.pesoKg * 0.035 * 10).roundToInt() / 10.0
        )
    }
}
