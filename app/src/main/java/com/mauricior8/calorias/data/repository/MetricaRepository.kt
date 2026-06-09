package com.mauricior8.calorias.data.repository

import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.model.MetricaTotal
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Capa intermedia entre el [MetricaDao] y la capa de presentacion (ViewModel).
 * Centraliza el acceso a datos y la logica relacionada con fechas.
 */
class MetricaRepository(
    private val dao: MetricaDao
) {

    val metricas: Flow<List<MetricaConfig>> = dao.observarMetricas()

    /** Totales acumulados del dia actual del dispositivo, agrupados por metrica. */
    val totalesDeHoy: Flow<List<MetricaTotal>> = run {
        val (inicio, fin) = rangoDelDiaActual()
        dao.observarTotalesDelDia(inicio, fin)
    }

    fun historial(metricaId: String): Flow<List<RegistroSuma>> =
        dao.observarHistorial(metricaId)

    suspend fun guardarMetrica(metrica: MetricaConfig) = dao.upsertMetrica(metrica)

    suspend fun agregarRegistro(registro: RegistroSuma): Long = dao.insertRegistro(registro)

    suspend fun hayMetricas(): Boolean = dao.contarMetricas() > 0

    /** Devuelve [inicioDia, finDia) en milisegundos epoch para hoy. */
    private fun rangoDelDiaActual(): Pair<Long, Long> {
        val inicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fin = (inicio.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        return inicio.timeInMillis to fin.timeInMillis
    }
}
