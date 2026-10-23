package com.mauricior8.calorias.data.remote

import com.mauricior8.calorias.util.Clave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Alimento de Open Food Facts con sus nutrientes por cada 100 g. */
data class AlimentoRemoto(
    val nombre: String,
    val marca: String,
    val kcal: Double,
    val proteinas: Double,
    val carbohidratos: Double,
    val grasas: Double,
    val fibra: Double,
    val azucar: Double,
    val sodioMg: Double
) {
    /** Aporte a una metrica (por clave canonica) para la cantidad indicada en gramos. */
    fun aporte(clave: String, gramos: Double): Double? {
        val por100 = when (clave) {
            Clave.CALORIAS -> kcal
            Clave.PROTEINAS -> proteinas
            Clave.CARBOHIDRATOS -> carbohidratos
            Clave.GRASAS -> grasas
            Clave.FIBRA -> fibra
            Clave.AZUCAR -> azucar
            Clave.SODIO -> sodioMg
            else -> return null
        }
        return por100 * gramos / 100.0
    }
}

/**
 * Busqueda de alimentos en la base publica Open Food Facts (https://openfoodfacts.org).
 * Solo se envia el texto buscado; no se envia ningun dato personal ni del usuario.
 */
object OpenFoodFacts {

    private const val BASE = "https://world.openfoodfacts.org/cgi/search.pl"
    private const val AGENTE = "CaloriasApp/7.0 (https://github.com/MauricioR8/APP-PLANTILLA-DE-CALORIAS)"

    /** @throws IOException si no hay conexion o el servicio falla. */
    suspend fun buscar(texto: String, maximo: Int = 15): List<AlimentoRemoto> {
        val consulta = texto.trim()
        if (consulta.length < 2) return emptyList()
        return withContext(Dispatchers.IO) {
            val url = URL(
                "$BASE?search_terms=${URLEncoder.encode(consulta, "UTF-8")}" +
                    "&search_simple=1&action=process&json=1&page_size=$maximo" +
                    "&fields=product_name,brands,nutriments"
            )
            val conexion = url.openConnection() as HttpURLConnection
            try {
                conexion.connectTimeout = 10_000
                conexion.readTimeout = 15_000
                conexion.setRequestProperty("User-Agent", AGENTE)
                if (conexion.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Servicio no disponible (${conexion.responseCode})")
                }
                val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
                parsear(cuerpo)
            } finally {
                conexion.disconnect()
            }
        }
    }

    /** Interpreta la respuesta JSON; ignora productos sin nombre o sin datos de energia. */
    fun parsear(json: String): List<AlimentoRemoto> {
        val productos = try {
            JSONObject(json).optJSONArray("products")
        } catch (e: JSONException) {
            throw IOException("Respuesta inesperada del servicio", e)
        } ?: return emptyList()

        return (0 until productos.length()).mapNotNull { i ->
            val p = productos.optJSONObject(i) ?: return@mapNotNull null
            val nombre = p.optString("product_name").trim()
            val n = p.optJSONObject("nutriments") ?: return@mapNotNull null
            if (nombre.isEmpty() || !n.has("energy-kcal_100g")) return@mapNotNull null
            fun v(clave: String) = n.optDouble(clave, 0.0).takeIf { !it.isNaN() } ?: 0.0
            AlimentoRemoto(
                nombre = nombre,
                marca = p.optString("brands").substringBefore(',').trim(),
                kcal = v("energy-kcal_100g"),
                proteinas = v("proteins_100g"),
                carbohidratos = v("carbohydrates_100g"),
                grasas = v("fat_100g"),
                fibra = v("fiber_100g"),
                azucar = v("sugars_100g"),
                sodioMg = v("sodium_100g") * 1000.0
            )
        }
    }
}
