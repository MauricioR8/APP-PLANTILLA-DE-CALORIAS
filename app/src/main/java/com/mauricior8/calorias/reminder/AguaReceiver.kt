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

/** Recibe la alarma de un recordatorio a hora fija, lo muestra y programa el siguiente. */
class RecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tipo = Recordatorios.Tipo.entries.firstOrNull {
            it.name == intent.getStringExtra(Recordatorios.EXTRA_TIPO)
        } ?: return
        if (!Recordatorios.config(context, tipo).activo) return
        Recordatorios.mostrar(context, tipo)
        Recordatorios.programar(context, tipo)
    }
}

/** Las alarmas se pierden al reiniciar: se vuelven a programar las que estaban activas. */
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            RecordatorioAgua.reprogramarSiActivo(context)
            Recordatorios.reprogramarActivos(context)
        }
    }
}
