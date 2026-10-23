package com.mauricior8.calorias.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClaveMetricaTest {

    @Test fun reconoceNombresConYSinAcentos() {
        assertEquals(Clave.CALORIAS, claveMetrica("Calorías"))
        assertEquals(Clave.CALORIAS, claveMetrica("calorias"))
        assertEquals(Clave.PROTEINAS, claveMetrica("PROTEÍNAS"))
        assertEquals(Clave.AZUCAR, claveMetrica("Azúcar"))
        assertEquals(Clave.GRASAS, claveMetrica("Grasa total"))
        assertEquals(Clave.FIBRA, claveMetrica(" Fibra "))
    }

    @Test fun nombreDesconocidoDaNull() {
        assertNull(claveMetrica("Creatina"))
        assertNull(claveMetrica(""))
    }

    @Test fun comidaSugeridaSegunLaHora() {
        assertEquals("Desayuno", Comidas.sugerida(8))
        assertEquals("Comida", Comidas.sugerida(14))
        assertEquals("Cena", Comidas.sugerida(20))
        assertEquals("Colación", Comidas.sugerida(23))
        assertEquals("Colación", Comidas.sugerida(3))
    }
}
