package com.mauricior8.calorias.data.repository

import com.mauricior8.calorias.data.local.dao.CalculoDao
import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.dao.NotaDao
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.model.MetricaTotal
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Capa intermedia entre los DAO y la capa de presentacion (ViewModel).
 */
class MetricaRepository(
    private val metricaDao: MetricaDao,
    private val notaDao: NotaDao,
    private val calculoDao: CalculoDao
) {

    // ---- Metricas ----
    val metricas: Flow<List<MetricaConfig>> = metricaDao.observarMetricas()

    val totalesDeHoy: Flow<List<MetricaTotal>> = run {
        val (inicio, fin) = rangoDelDiaActual()
        metricaDao.observarTotalesDelDia(inicio, fin)
    }

    fun historial(metricaId: String): Flow<List<RegistroSuma>> =
        metricaDao.observarHistorial(metricaId)

    suspend fun guardarMetrica(metrica: MetricaConfig) = metricaDao.upsertMetrica(metrica)

    suspend fun eliminarMetrica(metrica: MetricaConfig) = metricaDao.eliminarMetrica(metrica)

    suspend fun actualizarOrden(id: String, nuevoOrden: Int) =
        metricaDao.actualizarOrden(id, nuevoOrden)

    suspend fun siguienteOrden(): Int = metricaDao.maxOrden() + 1

    suspend fun agregarRegistro(registro: RegistroSuma): Long =
        metricaDao.insertRegistro(registro)

    suspend fun hayMetricas(): Boolean = metricaDao.contarMetricas() > 0

    // ---- Notas ----
    val notas: Flow<List<Nota>> = notaDao.observarNotas()
    suspend fun guardarNota(nota: Nota) = notaDao.upsert(nota)
    suspend fun eliminarNota(nota: Nota) = notaDao.eliminar(nota)

    // ---- Calculadora ----
    val historialCalculos: Flow<List<CalculoHistorial>> = calculoDao.observarHistorial()
    suspend fun guardarCalculo(calculo: CalculoHistorial) = calculoDao.insertar(calculo)
    suspend fun limpiarCalculos() = calculoDao.limpiar()

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
