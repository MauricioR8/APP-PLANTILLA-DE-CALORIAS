package com.mauricior8.calorias.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ObjetivosTest {

    // Valores calculados a mano con Mifflin-St Jeor.
    @Test fun hombreHipertrofia() {
        val p = Perfil(Sexo.HOMBRE, 30, 80.0, 180.0, Actividad.MODERADA, MetaPeso.HIPERTROFIA)
        val o = Objetivos.calcular(p)
        assertEquals(1780, o.tmb)          // 10*80 + 6.25*180 - 5*30 + 5
        assertEquals(2759, o.gastoTotal)   // 1780 * 1.55
        assertEquals(3035, o.kcal)         // +10 %
        assertEquals(160, o.proteinaG)     // 2.0 g/kg
        assertEquals(84, o.grasasG)        // 25 % de las kcal
        assertEquals(409, o.carbohidratosG)
        assertEquals(42, o.fibraG)         // 14 g por 1000 kcal
        assertEquals(2.8, o.aguaL, 1e-9)   // 35 ml/kg
    }

    @Test fun mujerMantenimientoUsaMinimoDeGrasas() {
        val p = Perfil(Sexo.MUJER, 25, 60.0, 165.0, Actividad.SEDENTARIO, MetaPeso.MANTENER)
        val o = Objetivos.calcular(p)
        assertEquals(1345, o.tmb)          // ... - 161
        assertEquals(1614, o.kcal)
        assertEquals(108, o.proteinaG)
        assertEquals(48, o.grasasG)        // 25 % daria 45; se aplica el minimo de 0.8 g/kg
        assertEquals(188, o.carbohidratosG)
    }

    @Test fun definicionReduceCalorias() {
        val base = Perfil(Sexo.HOMBRE, 30, 80.0, 180.0, Actividad.MODERADA, MetaPeso.MANTENER)
        val def = base.copy(meta = MetaPeso.DEFINICION)
        assertEquals(Math.round(2759 * 0.8).toInt(), Objetivos.calcular(def).kcal)
    }

    @Test fun proteinaPersonalizada() {
        val p = Perfil(Sexo.HOMBRE, 30, 80.0, 180.0, Actividad.MODERADA, MetaPeso.MANTENER)
        assertEquals(200, Objetivos.calcular(p, proteinaPorKg = 2.5).proteinaG)
    }

    @Test fun carbohidratosNuncaSonNegativos() {
        val p = Perfil(Sexo.MUJER, 60, 40.0, 150.0, Actividad.SEDENTARIO, MetaPeso.DEFINICION)
        assertEquals(true, Objetivos.calcular(p, proteinaPorKg = 5.0).carbohidratosG >= 0)
    }

    @Test fun validacionDeRangos() {
        val ok = Perfil(Sexo.HOMBRE, 30, 80.0, 180.0, Actividad.MODERADA, MetaPeso.MANTENER)
        assertNull(Objetivos.validar(ok))
        assertNotNull(Objetivos.validar(ok.copy(edad = 5)))
        assertNotNull(Objetivos.validar(ok.copy(pesoKg = 10.0)))
        assertNotNull(Objetivos.validar(ok.copy(estaturaCm = 20.0)))
    }
}
