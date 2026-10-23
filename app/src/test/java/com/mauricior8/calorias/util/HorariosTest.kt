package com.mauricior8.calorias.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HorariosTest {

    private fun ms(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    @Test fun diarioAntesDeLaHoraEsHoy() {
        assertEquals(ms(2026, 10, 22, 9, 0), Horarios.proximo(ms(2026, 10, 22, 8, 30), 9, 0))
    }

    @Test fun diarioDespuesDeLaHoraEsManana() {
        assertEquals(ms(2026, 10, 23, 9, 0), Horarios.proximo(ms(2026, 10, 22, 9, 1), 9, 0))
    }

    @Test fun diarioEnLaHoraExactaEsManana() {
        // Estrictamente posterior: si ya es la hora, el siguiente es el dia siguiente.
        assertEquals(ms(2026, 10, 23, 9, 0), Horarios.proximo(ms(2026, 10, 22, 9, 0), 9, 0))
    }

    @Test fun diarioCruzaFinDeMes() {
        assertEquals(ms(2026, 11, 1, 20, 0), Horarios.proximo(ms(2026, 10, 31, 21, 0), 20, 0))
    }

    @Test fun mensualAntesDelDiaEsEsteMes() {
        assertEquals(ms(2026, 10, 25, 9, 0), Horarios.proximo(ms(2026, 10, 22, 12, 0), 9, 0, diaDelMes = 25))
    }

    @Test fun mensualDespuesDelDiaEsElMesSiguiente() {
        assertEquals(ms(2026, 11, 1, 9, 0), Horarios.proximo(ms(2026, 10, 22, 12, 0), 9, 0, diaDelMes = 1))
    }

    @Test fun mensualCruzaElAnio() {
        assertEquals(ms(2027, 1, 1, 9, 0), Horarios.proximo(ms(2026, 12, 15, 12, 0), 9, 0, diaDelMes = 1))
    }

    @Test fun mensualAjustaAlUltimoDiaDelMesCorto() {
        // 2027 no es bisiesto: febrero tiene 28 dias.
        assertEquals(ms(2027, 2, 28, 9, 0), Horarios.proximo(ms(2027, 2, 10, 12, 0), 9, 0, diaDelMes = 31))
    }

    @Test fun siempreEsPosteriorAAhora() {
        val ahora = ms(2026, 10, 22, 13, 37)
        listOf(null, 1, 15, 31).forEach { dia ->
            assertTrue(Horarios.proximo(ahora, 8, 15, dia) > ahora)
        }
    }
}
