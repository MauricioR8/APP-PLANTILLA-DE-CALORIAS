package com.mauricior8.calorias.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatoTest {

    @Test fun codificarYDecodificarValores() {
        val original = mapOf("calorias_1" to 250f, "proteinas_2" to 12.5f)
        assertEquals(original, decodificarValores(codificarValores(original)))
    }

    @Test fun decodificarTextoVacioODanado() {
        assertTrue(decodificarValores("").isEmpty())
        assertTrue(decodificarValores("   ").isEmpty())
        assertEquals(mapOf("a" to 1f), decodificarValores("a=1;roto;b=x;c=1=2"))
    }
}
