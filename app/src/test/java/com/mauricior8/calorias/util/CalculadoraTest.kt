package com.mauricior8.calorias.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculadoraTest {

    private fun ev(s: String) = Calculadora.evaluar(s)

    @Test fun sumaYResta() {
        assertEquals(7.0, ev("3+4")!!, 1e-9)
        assertEquals(-1.0, ev("3-4")!!, 1e-9)
    }

    @Test fun precedenciaDeOperadores() {
        assertEquals(14.0, ev("2+3*4")!!, 1e-9)
        assertEquals(20.0, ev("(2+3)*4")!!, 1e-9)
    }

    @Test fun decimalesConPuntoYComa() {
        assertEquals(3.75, ev("1.5+2.25")!!, 1e-9)
        assertEquals(3.75, ev("1,5+2,25")!!, 1e-9)
    }

    @Test fun sinErrorDePrecisionDeFloat() {
        // Con Float esto daba 0.30000001; con Double el error es ~1e-17.
        assertEquals(0.3, ev("0.1+0.2")!!, 1e-12)
    }

    @Test fun xComoMultiplicacion() {
        assertEquals(12.0, ev("3x4")!!, 1e-9)
        assertEquals(12.0, ev("3X4")!!, 1e-9)
    }

    @Test fun negativosYUnarios() {
        assertEquals(-5.0, ev("-5")!!, 1e-9)
        assertEquals(8.0, ev("3--5")!!, 1e-9)
        assertEquals(6.0, ev("+6")!!, 1e-9)
    }

    @Test fun modulo() {
        assertEquals(1.0, ev("10%3")!!, 1e-9)
    }

    @Test fun divisionPorCeroEsNull() {
        assertNull(ev("5/0"))
        assertNull(ev("5%0"))
    }

    @Test fun expresionesInvalidasSonNull() {
        assertNull(ev(""))
        assertNull(ev("   "))
        assertNull(ev("2+"))
        assertNull(ev("(2+3"))
        assertNull(ev("2+3)"))
        assertNull(ev("1.2.3+1"))
        assertNull(ev("abc"))
    }

    @Test fun espaciosSeIgnoran() {
        assertEquals(9.0, ev(" 2 * ( 3 + 0 ) + 3 ")!!, 1e-9)
    }
}
