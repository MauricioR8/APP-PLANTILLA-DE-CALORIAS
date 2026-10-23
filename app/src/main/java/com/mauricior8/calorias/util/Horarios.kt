package com.mauricior8.calorias.util

import java.util.Calendar

/** Calculo del proximo aviso de un recordatorio (sin Android, probable en la JVM). */
object Horarios {

    /**
     * Primer instante estrictamente posterior a [ahora] a las [hora]:[minuto].
     * Si [diaDelMes] es null el aviso es diario; si no, mensual (un dia mayor que los que tiene
     * el mes se ajusta al ultimo dia).
     */
    fun proximo(ahora: Long, hora: Int, minuto: Int, diaDelMes: Int? = null): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ahora }

        if (diaDelMes == null) {
            c.set(Calendar.HOUR_OF_DAY, hora)
            c.set(Calendar.MINUTE, minuto)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            if (c.timeInMillis <= ahora) c.add(Calendar.DAY_OF_MONTH, 1)
            return c.timeInMillis
        }

        fun enElMesDe(base: Calendar): Calendar {
            val r = base.clone() as Calendar
            r.set(Calendar.DAY_OF_MONTH, 1)
            r.set(Calendar.DAY_OF_MONTH, minOf(diaDelMes, r.getActualMaximum(Calendar.DAY_OF_MONTH)))
            r.set(Calendar.HOUR_OF_DAY, hora)
            r.set(Calendar.MINUTE, minuto)
            r.set(Calendar.SECOND, 0)
            r.set(Calendar.MILLISECOND, 0)
            return r
        }

        var r = enElMesDe(c)
        if (r.timeInMillis <= ahora) {
            val siguiente = c.clone() as Calendar
            siguiente.set(Calendar.DAY_OF_MONTH, 1)
            siguiente.add(Calendar.MONTH, 1)
            r = enElMesDe(siguiente)
        }
        return r.timeInMillis
    }
}
