package com.mauricior8.calorias.data.repository

import com.mauricior8.calorias.data.local.dao.AlimentoDao
import com.mauricior8.calorias.data.local.dao.CalculoDao
import com.mauricior8.calorias.data.local.dao.EstadoDiaDao
import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.dao.NotaDao
import com.mauricior8.calorias.data.local.dao.TablaDao
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.local.model.MetricaTotal
import kotlinx.coroutines.flow.Flow

/**
 * Capa intermedia entre los DAO y la capa de presentacion (ViewModel).
 */
class MetricaRepository(
    private val metricaDao: MetricaDao,
    private val notaDao: NotaDao,
    private val calculoDao: CalculoDao,
    private val tablaDao: TablaDao,
    private val estadoDiaDao: EstadoDiaDao,
    private val alimentoDao: AlimentoDao
) {

    // ---- Metricas ----
    val metricas: Flow<List<MetricaConfig>> = metricaDao.observarMetricas()

    /** Totales agregados por metrica para un rango de dia arbitrario. */
    fun totalesDeDia(inicio: Long, fin: Long): Flow<List<MetricaTotal>> =
        metricaDao.observarTotalesDelDia(inicio, fin)

    fun historial(metricaId: String): Flow<List<RegistroSuma>> =
        metricaDao.observarHistorial(metricaId)

    suspend fun guardarMetrica(metrica: MetricaConfig) = metricaDao.upsertMetrica(metrica)

    suspend fun eliminarMetrica(metrica: MetricaConfig) = metricaDao.eliminarMetrica(metrica)

    suspend fun actualizarOrden(id: String, nuevoOrden: Int) =
        metricaDao.actualizarOrden(id, nuevoOrden)

    suspend fun siguienteOrden(): Int = metricaDao.maxOrden() + 1

    suspend fun agregarRegistro(registro: RegistroSuma): Long =
        metricaDao.insertRegistro(registro)

    /** Borra todos los registros de un dia (rango [inicio, fin)). */
    suspend fun limpiarDia(inicio: Long, fin: Long) =
        metricaDao.eliminarRegistrosEnRango(inicio, fin)

    /** Borra los registros de UNA metrica en un dia (rango [inicio, fin)). */
    suspend fun limpiarMetricaDia(metricaId: String, inicio: Long, fin: Long) =
        metricaDao.eliminarRegistrosDeMetricaEnRango(metricaId, inicio, fin)

    suspend fun hayMetricas(): Boolean = metricaDao.contarMetricas() > 0

    // ---- Estado del dia ----
    fun observarEstadoDia(fecha: String): Flow<EstadoDia?> = estadoDiaDao.observar(fecha)
    suspend fun guardarEstadoDia(estado: EstadoDia) = estadoDiaDao.upsert(estado)

    // ---- Historial de alimentos ----
    val alimentosGuardados: Flow<List<AlimentoGuardado>> = alimentoDao.observar()
    suspend fun guardarAlimento(alimento: AlimentoGuardado): Long = alimentoDao.insertar(alimento)
    suspend fun actualizarAlimento(alimento: AlimentoGuardado) = alimentoDao.actualizar(alimento)
    suspend fun eliminarAlimento(alimento: AlimentoGuardado) = alimentoDao.eliminar(alimento)

    // ---- Notas ----
    val notas: Flow<List<Nota>> = notaDao.observarNotas()
    suspend fun guardarNota(nota: Nota) = notaDao.upsert(nota)
    suspend fun eliminarNota(nota: Nota) = notaDao.eliminar(nota)

    // ---- Calculadora ----
    val historialCalculos: Flow<List<CalculoHistorial>> = calculoDao.observarHistorial()
    suspend fun guardarCalculo(calculo: CalculoHistorial) = calculoDao.insertar(calculo)
    suspend fun limpiarCalculos() = calculoDao.limpiar()

    // ---- Tablas de alimentos ----
    val tablas: Flow<List<TablaAlimentos>> = tablaDao.observarTablas()

    fun columnas(tablaId: Int): Flow<List<ColumnaTabla>> = tablaDao.observarColumnas(tablaId)
    fun filas(tablaId: Int): Flow<List<FilaTabla>> = tablaDao.observarFilas(tablaId)
    fun celdas(tablaId: Int): Flow<List<CeldaTabla>> = tablaDao.observarCeldas(tablaId)

    suspend fun crearTabla(nombre: String): Long =
        tablaDao.insertTabla(TablaAlimentos(nombre = nombre))

    suspend fun eliminarTabla(tabla: TablaAlimentos) = tablaDao.eliminarTabla(tabla)

    suspend fun actualizarTabla(tabla: TablaAlimentos) = tablaDao.actualizarTabla(tabla)

    /** Devuelve false si se alcanzo el limite de 10 columnas. */
    suspend fun agregarColumna(tablaId: Int, nombre: String): Boolean {
        if (tablaDao.contarColumnas(tablaId) >= MAX_COLUMNAS) return false
        val orden = tablaDao.maxOrdenColumna(tablaId) + 1
        tablaDao.insertColumna(ColumnaTabla(tablaId = tablaId, nombre = nombre, orden = orden))
        return true
    }

    /** Devuelve false si se alcanzo el limite de 100 filas. */
    suspend fun agregarFila(tablaId: Int, nombre: String): Boolean {
        if (tablaDao.contarFilas(tablaId) >= MAX_FILAS) return false
        val orden = tablaDao.maxOrdenFila(tablaId) + 1
        tablaDao.insertFila(FilaTabla(tablaId = tablaId, nombre = nombre, orden = orden))
        return true
    }

    suspend fun eliminarColumna(columna: ColumnaTabla) = tablaDao.eliminarColumna(columna)
    suspend fun eliminarFila(fila: FilaTabla) = tablaDao.eliminarFila(fila)

    suspend fun setCelda(tablaId: Int, filaId: Int, columnaId: Int, valor: String) {
        val existente = tablaDao.obtenerCelda(filaId, columnaId)
        tablaDao.upsertCelda(
            CeldaTabla(
                id = existente?.id ?: 0,
                tablaId = tablaId,
                filaId = filaId,
                columnaId = columnaId,
                valor = valor
            )
        )
    }

    companion object {
        const val MAX_COLUMNAS = 10
        const val MAX_FILAS = 100
    }
}
