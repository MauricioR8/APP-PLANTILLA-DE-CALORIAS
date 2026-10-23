package com.mauricior8.calorias.data.backup

import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RespaldoTest {

    private val completo = DatosRespaldo(
        metricas = listOf(
            MetricaConfig("calorias", "Calorías", "kcal", 2000f, "#EF5350", 0, "Anillo"),
            MetricaConfig("agua", "Agua", "L", null, "#26C6DA", 1, "Barras")
        ),
        registros = listOf(
            RegistroSuma(1, "calorias", 0.1f, 1_700_000_000_000L, "alimento", "Avena \"light\"", "Desayuno"),
            RegistroSuma(2, "agua", 250.5f, 1_700_000_100_000L, "manual", null, null)
        ),
        notas = listOf(Nota(1, "línea 1\nlínea 2 ñ 🙂", 5L)),
        calculos = listOf(CalculoHistorial(1, "2+2", "4", 6L)),
        tablas = listOf(TablaAlimentos(1, "Frutas", "Barras", 7L)),
        columnas = listOf(ColumnaTabla(1, 1, "Kcal", 0)),
        filas = listOf(FilaTabla(1, 1, "Manzana", 0)),
        celdas = listOf(CeldaTabla(1, 1, 1, 1, "52")),
        estadosDia = listOf(EstadoDia("2026-10-22", true)),
        alimentos = listOf(AlimentoGuardado(1, "Pollo", "calorias=165.0;proteinas=31.0", 8L)),
        pesos = listOf(PesoRegistro(1, 9L, 78.4f))
    )

    @Test fun idaYVueltaConservaTodo() {
        val json = Respaldo.aJson(completo, fecha = 123L)
        assertEquals(completo, Respaldo.deJson(json))
    }

    @Test fun floatsSeConservanExactos() {
        val leido = Respaldo.deJson(Respaldo.aJson(completo))
        assertEquals(0.1f, leido.registros[0].valor, 0f)
        assertEquals(78.4f, leido.pesos[0].kg, 0f)
    }

    @Test fun nulosSeConservan() {
        val leido = Respaldo.deJson(Respaldo.aJson(completo))
        assertNull(leido.metricas[1].limiteMaximo)
        assertNull(leido.registros[1].detalle)
        assertNull(leido.registros[1].comida)
    }

    @Test fun seccionAusenteSeLeeVacia() {
        val minimo = """{"app":"calorias","formato":1,"fecha":1}"""
        assertEquals(DatosRespaldo(), Respaldo.deJson(minimo))
    }

    @Test fun rechazaArchivosAjenosODanados() {
        listOf(
            "no es json",
            """{"app":"otra","formato":1}""",
            """{"app":"calorias","formato":99}""",
            """{"app":"calorias","formato":1,"metricas":[{"id":"x"}]}"""
        ).forEach { texto ->
            try {
                Respaldo.deJson(texto)
                fail("Debio rechazar: $texto")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.isNotBlank())
            }
        }
    }
}
