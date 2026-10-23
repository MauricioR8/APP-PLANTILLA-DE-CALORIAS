package com.mauricior8.calorias.data.remote

import com.mauricior8.calorias.util.Clave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OpenFoodFactsTest {

    private val respuesta = """
        {"count": 3, "products": [
          {"product_name": "Copos de avena", "brands": "Marca A, Marca B",
           "nutriments": {"energy-kcal_100g": 375, "proteins_100g": 14, "carbohydrates_100g": 59,
                          "fat_100g": 7, "fiber_100g": 10, "sugars_100g": 0.7, "sodium_100g": 0.008}},
          {"product_name": "", "nutriments": {"energy-kcal_100g": 100}},
          {"product_name": "Sin energia", "nutriments": {"proteins_100g": 5}},
          {"product_name": "Sin nutrientes"},
          {"product_name": "Solo energia", "nutriments": {"energy-kcal_100g": 50}}
        ]}
    """.trimIndent()

    @Test fun descartaProductosIncompletos() {
        val r = OpenFoodFacts.parsear(respuesta)
        assertEquals(listOf("Copos de avena", "Solo energia"), r.map { it.nombre })
    }

    @Test fun leeNutrientesPor100g() {
        val a = OpenFoodFacts.parsear(respuesta).first()
        assertEquals("Marca A", a.marca)
        assertEquals(375.0, a.kcal, 0.0)
        assertEquals(14.0, a.proteinas, 0.0)
        assertEquals(8.0, a.sodioMg, 1e-9)   // 0.008 g de sodio = 8 mg
    }

    @Test fun nutrientesAusentesValenCero() {
        val a = OpenFoodFacts.parsear(respuesta).last()
        assertEquals(0.0, a.proteinas, 0.0)
        assertEquals(0.0, a.fibra, 0.0)
    }

    @Test fun aporteEscalaPorGramos() {
        val a = OpenFoodFacts.parsear(respuesta).first()
        assertEquals(187.5, a.aporte(Clave.CALORIAS, 50.0)!!, 1e-9)
        assertEquals(28.0, a.aporte(Clave.PROTEINAS, 200.0)!!, 1e-9)
        assertNull(a.aporte("creatina", 100.0))
    }

    @Test fun sinListaDeProductosDaVacio() {
        assertTrue(OpenFoodFacts.parsear("""{"count": 0}""").isEmpty())
    }

    @Test(expected = IOException::class)
    fun respuestaNoJsonLanzaIOException() {
        OpenFoodFacts.parsear("<html>error</html>")
    }
}
