package com.mauricior8.calorias.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mauricior8.calorias.MainActivity
import java.util.Calendar

/**
 * Recordatorio periodico para tomar agua. Usa alarmas inexactas (no exigen permisos
 * especiales y gastan poca bateria) y solo notifica entre [HORA_INICIO] y [HORA_FIN].
 * Los ajustes se guardan en SharedPreferences.
 */
object RecordatorioAgua {

    const val HORA_INICIO = 8
    const val HORA_FIN = 22

    private const val PREFS = "recordatorio_agua"
    private const val CLAVE_ACTIVO = "activo"
    private const val CLAVE_HORAS = "cada_horas"
    private const val CANAL = "agua"
    private const val ID_NOTIFICACION = 1001

    data class Ajustes(val activo: Boolean, val cadaHoras: Int)

    fun ajustes(ctx: Context): Ajustes {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Ajustes(p.getBoolean(CLAVE_ACTIVO, false), p.getInt(CLAVE_HORAS, 2))
    }

    fun guardar(ctx: Context, activo: Boolean, cadaHoras: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(CLAVE_ACTIVO, activo)
            .putInt(CLAVE_HORAS, cadaHoras)
            .apply()
        if (activo) programar(ctx, cadaHoras) else cancelar(ctx)
    }

    /** Vuelve a programar la alarma si estaba activa (tras reiniciar el telefono). */
    fun reprogramarSiActivo(ctx: Context) {
        val a = ajustes(ctx)
        if (a.activo) programar(ctx, a.cadaHoras)
    }

    private fun programar(ctx: Context, cadaHoras: Int) {
        val intervalo = cadaHoras * AlarmManager.INTERVAL_HOUR
        alarmas(ctx).setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + intervalo,
            intervalo,
            pendingIntent(ctx)
        )
    }

    private fun cancelar(ctx: Context) {
        alarmas(ctx).cancel(pendingIntent(ctx))
    }

    private fun alarmas(ctx: Context) = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, AguaReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun tienePermisoNotificaciones(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Muestra la notificacion si estamos dentro del horario y hay permiso. */
    @SuppressLint("MissingPermission") // se comprueba en tienePermisoNotificaciones
    fun mostrar(ctx: Context) {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hora < HORA_INICIO || hora >= HORA_FIN) return
        if (!tienePermisoNotificaciones(ctx)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Recordatorio de agua", NotificationManager.IMPORTANCE_DEFAULT)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
        val abrir = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("💧 Hora de tomar agua")
            .setContentText("Registra tu vaso en la app para llevar tu meta del día.")
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID_NOTIFICACION, notificacion)
    }
}
