package com.mauricior8.calorias.data.backup

import com.mauricior8.calorias.data.local.entity.Ajuste
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.Medida
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.PesoEjercicio
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.Rutina
import com.mauricior8.calorias.data.local.entity.SerieHecha
import com.mauricior8.calorias.data.local.entity.SesionGym
import com.mauricior8.calorias.data.local.entity.Suplemento
import com.mauricior8.calorias.data.local.entity.SuplementoTomado
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.local.entity.TotalDiario
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
        pesos = listOf(PesoRegistro(1, 9L, 78.4f)),
        ajustes = listOf(Ajuste("tema", "oscuro")),
        rutinas = listOf(Rutina(1, "Lunes y Jueves", "#DC2626", "1,4", false, "nota ñ", 0)),
        ejercicios = listOf(
            EjercicioRutina(1, 1, "Press inclinado", "Pecho", 3, 10, false, 90, 1, 17.5f, "kg por lado", 0),
            EjercicioRutina(2, 1, "Slant board", "Movilidad", 3, 80, true, 15, 1, null, null, 1)
        ),
        seriesHechas = listOf(SerieHecha(1, "2026-10-22", 0)),
        pesosEjercicio = listOf(PesoEjercicio(1, 1, 10L, 17.5f)),
        sesiones = listOf(SesionGym("2026-10-22", "hombro molesto")),
        suplementos = listOf(Suplemento(1, "Creatina", 0)),
        suplementosTomados = listOf(SuplementoTomado(1, "2026-10-22")),
        medidas = listOf(Medida(1, 11L, "Cintura", 82.5f)),
        totalesDiarios = listOf(TotalDiario("2026-10-15", "calorias", 2450.5f))
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

    @Test fun respaldoDeFormato1SigueSiendoLegible() {
        val viejo = """{"app":"calorias","formato":1,"fecha":1,
            "metricas":[{"id":"calorias","nombre":"Calorías","unidad":"kcal","limiteMaximo":2000.0,
                         "colorHex":"#EF5350","orden":0,"tipoGrafica":"Anillo"}],
            "alimentos":[{"id":1,"nombre":"Pollo","valores":"calorias=165.0","timestamp":5}]}"""
        val d = Respaldo.deJson(viejo)
        assertNull(d.metricas[0].limiteDescanso)
        assertEquals(false, d.alimentos[0].favorito)
        assertTrue(d.rutinas.isEmpty() && d.ajustes.isEmpty())
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
