package com.mauricior8.calorias.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Utilidades de fecha sin dependencias de Android (probables en la JVM). */
object Fechas {

    /** Clave "yyyy-MM-dd" de un instante, en la zona horaria del telefono. */
    fun clave(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(ms))

    /** Inicio (00:00) del dia que contiene [ms]. */
    fun inicioDeDia(ms: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /**
     * Inicio (00:00) de la semana que contiene [ms]. Con [empiezaDomingo] la semana va de
     * domingo a sabado; si no, de lunes a domingo.
     */
    fun inicioDeSemana(ms: Long, empiezaDomingo: Boolean): Long {
        val c = Calendar.getInstance().apply { timeInMillis = inicioDeDia(ms) }
        val dow = c.get(Calendar.DAY_OF_WEEK) // domingo=1 ... sabado=7
        val atras = if (empiezaDomingo) dow - 1 else (dow + 5) % 7
        c.add(Calendar.DAY_OF_MONTH, -atras)
        return c.timeInMillis
    }

    /**
     * Racha de dias completados consecutivos. Cuenta hacia atras desde hoy; si hoy aun no esta
     * completado la racha no se rompe (se cuenta desde ayer).
     */
    fun racha(completados: Set<String>, hoy: Long): Int {
        var dia = if (clave(hoy) in completados) hoy else desplazarDias(hoy, -1)
        var n = 0
        while (clave(dia) in completados) {
            n++
            dia = desplazarDias(dia, -1)
        }
        return n
    }

    /** [delta] dias antes o despues de [ms], a las 00:00. */
    fun desplazarDias(ms: Long, delta: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = inicioDeDia(ms) }
        c.add(Calendar.DAY_OF_MONTH, delta)
        return c.timeInMillis
    }
}
