package com.mauricior8.calorias.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuerpoCsvTest {

    private val dia = 24L * 60 * 60 * 1000

    // ---------- Cuerpo ----------

    @Test fun mediaMovilPromediaLaVentana() {
        val p = listOf(0L to 80f, dia to 82f, 2 * dia to 81f, 10 * dia to 79f)
        val m = Cuerpo.mediaMovil(p, ventanaDias = 7)
        assertEquals(80f, m[0], 1e-4f)
        assertEquals(81f, m[1], 1e-4f)
        assertEquals(81f, m[2], 1e-4f)
        assertEquals(79f, m[3], 1e-4f)   // los anteriores quedan fuera de la ventana de 7 dias
    }

    @Test fun ritmoDeCambioEnKgPorSemana() {
        // Baja 0.1 kg por dia durante 14 dias = -0.7 kg/semana.
        val p = (0..14).map { it * dia to (80f - 0.1f * it) }
        assertEquals(-0.7f, Cuerpo.ritmoKgPorSemana(p)!!, 1e-3f)
    }

    @Test fun ritmoNecesitaSuficientesDatos() {
        assertNull(Cuerpo.ritmoKgPorSemana(emptyList()))
        assertNull(Cuerpo.ritmoKgPorSemana(listOf(0L to 80f, dia to 80.5f)))                 // < 3 puntos
        assertNull(Cuerpo.ritmoKgPorSemana(listOf(0L to 80f, dia to 80.2f, 2 * dia to 80.4f))) // < 5 dias
    }

    @Test fun gastoEstimadoSegunElCambioDePeso() {
        assertEquals(2470, Cuerpo.gastoEstimado(2800.0, 0.3f))    // subes: gastas menos de lo que comes
        assertEquals(3130, Cuerpo.gastoEstimado(2800.0, -0.3f))   // bajas: gastas mas
        assertEquals(2800, Cuerpo.gastoEstimado(2800.0, 0f))
    }

    // ---------- Csv ----------

    @Test fun camposSimplesNoSeEntrecomillan() {
        assertEquals("hola", Csv.campo("hola"))
        assertEquals("12.5", Csv.campo(12.5))
        assertEquals("", Csv.campo(null))
        assertEquals("-5", Csv.campo(-5))
    }

    @Test fun camposConComasComillasYSaltosSeEscapan() {
        assertEquals("\"a,b\"", Csv.campo("a,b"))
        assertEquals("\"di \"\"hola\"\"\"", Csv.campo("di \"hola\""))
        assertEquals("\"l1\nl2\"", Csv.campo("l1\nl2"))
    }

    @Test fun neutralizaFormulasDeHojaDeCalculo() {
        assertEquals("'=SUMA(A1)", Csv.campo("=SUMA(A1)"))
        assertEquals("'+cmd", Csv.campo("+cmd"))
        assertEquals("'@x", Csv.campo("@x"))
        assertEquals("'-abc", Csv.campo("-abc"))
    }

    @Test fun documentoLlevaBomEncabezadoYFilas() {
        val doc = Csv.documento(listOf("fecha", "valor"), listOf(listOf("2026-10-22", 450.5f), listOf("2026-10-23", 300)))
        assertTrue(doc.startsWith("﻿fecha,valor\r\n"))
        assertTrue(doc.contains("2026-10-22,450.5\r\n"))
        assertTrue(doc.endsWith("2026-10-23,300\r\n"))
    }
}
