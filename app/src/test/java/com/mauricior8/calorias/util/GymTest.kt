package com.mauricior8.calorias.util

import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.Rutina
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GymTest {

    private fun cal(y: Int, m: Int, d: Int) = Calendar.getInstance().apply { clear(); set(y, m - 1, d) }

    @Test fun diaIsoDeCadaDiaDeLaSemana() {
        // 2026-10-19 es lunes, 2026-10-25 es domingo.
        assertEquals(1, Gym.diaIso(cal(2026, 10, 19)))
        assertEquals(4, Gym.diaIso(cal(2026, 10, 22)))
        assertEquals(6, Gym.diaIso(cal(2026, 10, 24)))
        assertEquals(7, Gym.diaIso(cal(2026, 10, 25)))
    }

    @Test fun rutinaAplicaEnSusDias() {
        val a = Rutina(1, "A", dias = "1,4")
        assertTrue(Gym.aplicaEn(a, 1))
        assertTrue(Gym.aplicaEn(a, 4))
        assertFalse(Gym.aplicaEn(a, 2))
        assertFalse(Gym.aplicaEn(a, 7))
    }

    @Test fun bloqueDiarioAplicaTodosLosDias() {
        val diario = Rutina(2, "Diario", dias = "", diaria = true)
        (1..7).forEach { assertTrue(Gym.aplicaEn(diario, it)) }
    }

    @Test fun diasInvalidosSeIgnoran() {
        assertEquals(listOf(1, 4), Gym.diasDe(Rutina(1, "x", dias = "4, 1,9,x,0")))
    }

    @Test fun textoDeDias() {
        assertEquals("Lun y Jue", Gym.textoDias(Rutina(1, "A", dias = "1,4")))
        assertEquals("Lun, Mié y Vie", Gym.textoDias(Rutina(1, "A", dias = "1,3,5")))
        assertEquals("Sáb", Gym.textoDias(Rutina(1, "A", dias = "6")))
        assertEquals("Todos los días", Gym.textoDias(Rutina(1, "A", diaria = true)))
        assertEquals("Sin días", Gym.textoDias(Rutina(1, "A", dias = "")))
    }

    @Test fun seriesTotalesYCompletado() {
        val espirometro = EjercicioRutina(1, 1, "Espirómetro", series = 3, vecesPorDia = 2)
        assertEquals(6, Gym.totalSeries(espirometro))
        assertFalse(Gym.completo(espirometro, setOf(0, 1, 2)))
        assertEquals(3, Gym.hechas(espirometro, setOf(0, 1, 2)))
        assertTrue(Gym.completo(espirometro, setOf(0, 1, 2, 3, 4, 5)))
        // Indices fuera de rango (por ejemplo tras reducir las series) no cuentan.
        assertEquals(2, Gym.hechas(espirometro.copy(series = 1), setOf(0, 1, 5)))
    }

    @Test fun zonasDeVolumenSemanal() {
        assertEquals(ZonaVolumen.BAJO, Gym.zona(9))
        assertEquals(ZonaVolumen.OPTIMO, Gym.zona(10))
        assertEquals(ZonaVolumen.OPTIMO, Gym.zona(20))
        assertEquals(ZonaVolumen.ALTO, Gym.zona(21))
    }

    @Test fun sugerenciaDeSubidaDePeso() {
        assertNull(Gym.sugerirSubida(17.5f, sesionesCompletas = 5))
        assertEquals(18.5f, Gym.sugerirSubida(17.5f, 6)!!, 1e-6f)   // +5 % = 0.875 -> 1.0
        assertEquals(68.5f, Gym.sugerirSubida(65f, 8)!!, 1e-6f)     // +5 % = 3.25 -> 3.5 (redondeo a 0.5)
        assertNull(Gym.sugerirSubida(null, 10))
        assertEquals(8.5f, Gym.sugerirSubida(8f, 6)!!, 1e-6f)       // minimo +0.5
    }

    @Test fun formatoDeKilos() {
        assertEquals("40", Gym.kg(40f))
        assertEquals("17.5", Gym.kg(17.5f))
    }
}
