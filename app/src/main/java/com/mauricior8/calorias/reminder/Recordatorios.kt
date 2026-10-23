package com.mauricior8.calorias.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mauricior8.calorias.MainActivity
import com.mauricior8.calorias.util.Horarios

/**
 * Recordatorios a una hora fija: espirometro (2 tandas al dia) y actualizacion mensual de pesos.
 * Cada aviso programa el siguiente al dispararse (alarma inexacta: sin permisos especiales).
 * Los ajustes viven en SharedPreferences para poder leerlos desde el receptor de la alarma.
 */
object Recordatorios {

    enum class Tipo(val id: Int, val titulo: String, val texto: String, val diaDelMes: Int?, val horaDefecto: Int) {
        ESPIRO_1(11, "Espirómetro", "Primera tanda del día: 3 series de 10.", null, 9),
        ESPIRO_2(12, "Espirómetro", "Segunda tanda del día: 3 series de 10.", null, 20),
        PESOS_MES(13, "Actualiza tus pesos", "Revisa tus ejercicios y sube la carga donde ya puedas.", 1, 9)
    }

    data class Config(val activo: Boolean, val hora: Int, val minuto: Int)

    private const val PREFS = "recordatorios"
    private const val CANAL = "gym"
    const val EXTRA_TIPO = "tipo"

    fun config(ctx: Context, tipo: Tipo): Config {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(
            activo = p.getBoolean("${tipo.name}_activo", false),
            hora = p.getInt("${tipo.name}_hora", tipo.horaDefecto),
            minuto = p.getInt("${tipo.name}_min", 0)
        )
    }

    fun guardar(ctx: Context, tipo: Tipo, activo: Boolean, hora: Int, minuto: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("${tipo.name}_activo", activo)
            .putInt("${tipo.name}_hora", hora)
            .putInt("${tipo.name}_min", minuto)
            .apply()
        if (activo) programar(ctx, tipo) else cancelar(ctx, tipo)
    }

    /** Tras reiniciar el telefono las alarmas se pierden: se vuelven a programar las activas. */
    fun reprogramarActivos(ctx: Context) {
        Tipo.entries.forEach { if (config(ctx, it).activo) programar(ctx, it) }
    }

    fun programar(ctx: Context, tipo: Tipo) {
        val c = config(ctx, tipo)
        val cuando = Horarios.proximo(System.currentTimeMillis(), c.hora, c.minuto, tipo.diaDelMes)
        val alarmas = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pendingIntent(ctx, tipo))
    }

    private fun cancelar(ctx: Context, tipo: Tipo) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent(ctx, tipo))
    }

    private fun pendingIntent(ctx: Context, tipo: Tipo): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, tipo.id,
            Intent(ctx, RecordatorioReceiver::class.java).putExtra(EXTRA_TIPO, tipo.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    @SuppressLint("MissingPermission") // se comprueba en RecordatorioAgua.tienePermisoNotificaciones
    fun mostrar(ctx: Context, tipo: Tipo) {
        if (!RecordatorioAgua.tienePermisoNotificaciones(ctx)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Recordatorios de entrenamiento", NotificationManager.IMPORTANCE_DEFAULT)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
        val abrir = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(tipo.titulo)
            .setContentText(tipo.texto)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(tipo.id, notificacion)
    }
}
