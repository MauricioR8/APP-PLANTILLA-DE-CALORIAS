package com.mauricior8.calorias.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Recibe la alarma periodica y muestra el recordatorio de agua. */
class AguaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RecordatorioAgua.mostrar(context)
    }
}

/** Las alarmas se pierden al reiniciar: se vuelven a programar si el recordatorio estaba activo. */
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            RecordatorioAgua.reprogramarSiActivo(context)
        }
    }
}
