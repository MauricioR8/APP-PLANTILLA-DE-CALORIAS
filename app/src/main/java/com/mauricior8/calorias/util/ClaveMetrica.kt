package com.mauricior8.calorias.util

import java.text.Normalizer
import java.util.Locale

/** Identificadores canonicos de las metricas nutricionales conocidas. */
object Clave {
    const val CALORIAS = "calorias"
    const val PROTEINAS = "proteinas"
    const val CARBOHIDRATOS = "carbohidratos"
    const val GRASAS = "grasas"
    const val FIBRA = "fibra"
    const val AZUCAR = "azucar"
    const val SODIO = "sodio"
    const val AGUA = "agua"
}

/**
 * Deduce que nutriente representa una metrica por su nombre (sin acentos ni mayusculas),
 * para que funcione tambien con metricas creadas o renombradas por el usuario.
 */
fun claveMetrica(nombre: String): String? {
    val n = Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .trim()
    return when {
        n.startsWith("calor") || n == "kcal" -> Clave.CALORIAS
        n.startsWith("prote") -> Clave.PROTEINAS
        n.startsWith("carbo") -> Clave.CARBOHIDRATOS
        n.startsWith("grasa") || n.startsWith("lipid") -> Clave.GRASAS
        n.startsWith("fibra") -> Clave.FIBRA
        n.startsWith("azucar") -> Clave.AZUCAR
        n.startsWith("sodio") -> Clave.SODIO
        n.startsWith("agua") -> Clave.AGUA
        else -> null
    }
}

/** Comidas del dia que se pueden asignar a un alimento. */
object Comidas {
    val LISTA = listOf("Desayuno", "Comida", "Cena", "Colación")

    /** Comida mas probable segun la hora (0-23). */
    fun sugerida(hora: Int): String = when (hora) {
        in 5..10 -> "Desayuno"
        in 11..16 -> "Comida"
        in 17..21 -> "Cena"
        else -> "Colación"
    }
}
