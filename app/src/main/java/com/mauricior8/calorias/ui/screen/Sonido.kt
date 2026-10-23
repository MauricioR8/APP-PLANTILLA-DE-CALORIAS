package com.mauricior8.calorias.ui.screen

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** Vibra y emite un pitido corto: fin de un descanso o de un intervalo. */
fun avisarFin(ctx: Context) {
    try {
        val vibrador = ctx.getSystemService(Vibrator::class.java)
        val patron = longArrayOf(0, 300, 150, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrador?.vibrate(VibrationEffect.createWaveform(patron, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrador?.vibrate(patron, -1)
        }
    } catch (_: Exception) {
        // Sin vibrador o sin permiso: no es critico.
    }
    try {
        val tono = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tono.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
    } catch (_: Exception) {
        // Sin audio disponible: no es critico.
    }
}

/** "1:30" a partir de segundos. */
fun reloj(segundos: Int): String = "%d:%02d".format(segundos / 60, segundos % 60)
