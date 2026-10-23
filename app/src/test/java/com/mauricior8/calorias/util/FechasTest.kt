package com.mauricior8.calorias.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class FechasTest {

    private fun ms(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    @Test fun claveDeFecha() {
        assertEquals("2026-10-22", Fechas.clave(ms(2026, 10, 22, 21, 50)))
        assertEquals("2026-01-05", Fechas.clave(ms(2026, 1, 5)))
    }

    @Test fun inicioDeDiaQuitaLaHora() {
        assertEquals(ms(2026, 10, 22), Fechas.inicioDeDia(ms(2026, 10, 22, 23, 59)))
    }

    @Test fun semanaEmpezandoEnDomingo() {
        // 2026-10-22 es jueves; el domingo anterior es el 18.
        assertEquals(ms(2026, 10, 18), Fechas.inicioDeSemana(ms(2026, 10, 22, 14), empiezaDomingo = true))
        // Un domingo es el inicio de su propia semana.
        assertEquals(ms(2026, 10, 25), Fechas.inicioDeSemana(ms(2026, 10, 25, 8), empiezaDomingo = true))
        // Un sabado pertenece a la semana que empezo el domingo anterior.
        assertEquals(ms(2026, 10, 18), Fechas.inicioDeSemana(ms(2026, 10, 24, 23), empiezaDomingo = true))
    }

    @Test fun semanaEmpezandoEnLunes() {
        assertEquals(ms(2026, 10, 19), Fechas.inicioDeSemana(ms(2026, 10, 22, 14), empiezaDomingo = false))
        // Un domingo es el ULTIMO dia de la semana que empezo el lunes.
        assertEquals(ms(2026, 10, 19), Fechas.inicioDeSemana(ms(2026, 10, 25, 8), empiezaDomingo = false))
        assertEquals(ms(2026, 10, 19), Fechas.inicioDeSemana(ms(2026, 10, 19), empiezaDomingo = false))
    }

    @Test fun cruzaMesYAnio() {
        // 2026-01-01 es jueves: el domingo anterior es 2025-12-28.
        assertEquals(ms(2025, 12, 28), Fechas.inicioDeSemana(ms(2026, 1, 1, 10), empiezaDomingo = true))
    }

    @Test fun rachaDeDiasCompletados() {
        val hoy = ms(2026, 10, 22, 20)
        val seguidos = setOf("2026-10-22", "2026-10-21", "2026-10-20", "2026-10-18")
        assertEquals(3, Fechas.racha(seguidos, hoy))                       // el 19 rompe la racha
        // Hoy sin completar: no se rompe, cuenta desde ayer.
        assertEquals(2, Fechas.racha(setOf("2026-10-21", "2026-10-20"), hoy))
        // Ayer sin completar y hoy tampoco: racha 0.
        assertEquals(0, Fechas.racha(setOf("2026-10-20"), hoy))
        assertEquals(0, Fechas.racha(emptySet(), hoy))
    }

    @Test fun desplazarDias() {
        assertEquals(ms(2026, 10, 15), Fechas.desplazarDias(ms(2026, 10, 22, 18), -7))
        assertEquals(ms(2026, 11, 1), Fechas.desplazarDias(ms(2026, 10, 31, 5), 1))
    }
}
