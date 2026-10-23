package com.mauricior8.calorias.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mauricior8.calorias.CaloriasApp
import com.mauricior8.calorias.MainActivity
import com.mauricior8.calorias.R
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.util.Clave
import com.mauricior8.calorias.util.Fechas
import com.mauricior8.calorias.util.claveMetrica
import com.mauricior8.calorias.util.formatoValor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Widget de pantalla de inicio: calorias y proteina de hoy, con un boton para sumar un vaso de
 * agua (250 ml) sin abrir la app. Lee y escribe en la misma base de datos Room de la app.
 */
class ResumenWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        val accion = intent.action
        if (accion == ACCION_AGUA || accion == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            // goAsync mantiene vivo el receptor mientras se lee/escribe en la base de datos.
            val pendiente = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (accion == ACCION_AGUA) sumarAgua(context)
                    actualizarTodos(context)
                } finally {
                    pendiente.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACCION_AGUA = "com.mauricior8.calorias.widget.AGUA"

        /** Vuelve a dibujar todos los widgets colocados (se llama tambien al salir de la app). */
        suspend fun actualizarTodos(context: Context) {
            val gestor = AppWidgetManager.getInstance(context)
            val ids = gestor.getAppWidgetIds(ComponentName(context, ResumenWidget::class.java))
            if (ids.isEmpty()) return
            val textos = textosDeHoy(context)
            ids.forEach { gestor.updateAppWidget(it, vistas(context, textos)) }
        }

        private class Textos(val calorias: String, val proteina: String)

        private suspend fun textosDeHoy(context: Context): Textos {
            val repo = (context.applicationContext as CaloriasApp).repository
            val metricas = repo.metricas.first()
            val inicio = Fechas.inicioDeDia(System.currentTimeMillis())
            val totales = repo.totalesDeDia(inicio, Fechas.desplazarDias(inicio, 1)).first()
                .associate { it.metricaId to it.total }

            fun linea(clave: String, restante: Boolean): String? {
                val m = metricas.firstOrNull { claveMetrica(it.nombre) == clave } ?: return null
                val total = totales[m.id] ?: 0f
                val limite = m.limiteMaximo
                return if (limite == null) {
                    "${formatoValor(total)} ${m.unidad}"
                } else if (restante) {
                    val r = limite - total
                    if (r >= 0) "Quedan ${formatoValor(r)} ${m.unidad}" else "Te pasaste ${formatoValor(-r)} ${m.unidad}"
                } else {
                    "${formatoValor(total)} / ${formatoValor(limite)} ${m.unidad}"
                }
            }
            val cal = linea(Clave.CALORIAS, restante = false)
            val calRest = linea(Clave.CALORIAS, restante = true)
            val prot = linea(Clave.PROTEINAS, restante = true)
            return Textos(
                calorias = cal ?: "Sin métrica de calorías",
                proteina = listOfNotNull(calRest, prot?.let { "Proteína: $it" }).joinToString(" · ")
            )
        }

        private fun vistas(context: Context, t: Textos): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_resumen)
            v.setTextViewText(R.id.widget_calorias, t.calorias)
            v.setTextViewText(R.id.widget_proteina, t.proteina)

            val abrirApp = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.widget_raiz, abrirApp)

            val agua = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, ResumenWidget::class.java).setAction(ACCION_AGUA),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.widget_agua, agua)
            return v
        }

        private suspend fun sumarAgua(context: Context) {
            val repo = (context.applicationContext as CaloriasApp).repository
            val agua = repo.metricas.first().firstOrNull { claveMetrica(it.nombre) == Clave.AGUA } ?: return
            val valor = if (agua.unidad.equals("ml", ignoreCase = true)) 250f else 0.25f
            repo.agregarRegistro(RegistroSuma(metricaId = agua.id, valor = valor, tipo = "manual", detalle = "Vaso de agua"))
        }
    }
}
