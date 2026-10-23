package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.TablaAlimentos

/**
 * Lectura completa de todas las tablas (exportar) y reemplazo total (importar).
 * Los inserts usan la estrategia por defecto (ABORT): si el respaldo es inconsistente
 * la transaccion falla y la base de datos queda intacta.
 */
@Dao
interface RespaldoDao {

    // ---- Lectura ----
    @Query("SELECT * FROM metricas ORDER BY orden ASC") suspend fun metricas(): List<MetricaConfig>
    @Query("SELECT * FROM registros ORDER BY id ASC") suspend fun registros(): List<RegistroSuma>
    @Query("SELECT * FROM notas ORDER BY id ASC") suspend fun notas(): List<Nota>
    @Query("SELECT * FROM calculos ORDER BY id ASC") suspend fun calculos(): List<CalculoHistorial>
    @Query("SELECT * FROM tablas ORDER BY id ASC") suspend fun tablas(): List<TablaAlimentos>
    @Query("SELECT * FROM columnas ORDER BY id ASC") suspend fun columnas(): List<ColumnaTabla>
    @Query("SELECT * FROM filas ORDER BY id ASC") suspend fun filas(): List<FilaTabla>
    @Query("SELECT * FROM celdas ORDER BY id ASC") suspend fun celdas(): List<CeldaTabla>
    @Query("SELECT * FROM estados_dia") suspend fun estadosDia(): List<EstadoDia>
    @Query("SELECT * FROM alimentos_guardados ORDER BY id ASC") suspend fun alimentos(): List<AlimentoGuardado>
    @Query("SELECT * FROM peso ORDER BY id ASC") suspend fun pesos(): List<PesoRegistro>

    // ---- Borrado (las tablas hijas caen por ON DELETE CASCADE) ----
    @Query("DELETE FROM metricas") suspend fun borrarMetricas()
    @Query("DELETE FROM notas") suspend fun borrarNotas()
    @Query("DELETE FROM calculos") suspend fun borrarCalculos()
    @Query("DELETE FROM tablas") suspend fun borrarTablas()
    @Query("DELETE FROM estados_dia") suspend fun borrarEstadosDia()
    @Query("DELETE FROM alimentos_guardados") suspend fun borrarAlimentos()
    @Query("DELETE FROM peso") suspend fun borrarPesos()

    // ---- Inserccion (padres antes que hijos) ----
    @Insert suspend fun insertarMetricas(lista: List<MetricaConfig>)
    @Insert suspend fun insertarRegistros(lista: List<RegistroSuma>)
    @Insert suspend fun insertarNotas(lista: List<Nota>)
    @Insert suspend fun insertarCalculos(lista: List<CalculoHistorial>)
    @Insert suspend fun insertarTablas(lista: List<TablaAlimentos>)
    @Insert suspend fun insertarColumnas(lista: List<ColumnaTabla>)
    @Insert suspend fun insertarFilas(lista: List<FilaTabla>)
    @Insert suspend fun insertarCeldas(lista: List<CeldaTabla>)
    @Insert suspend fun insertarEstadosDia(lista: List<EstadoDia>)
    @Insert suspend fun insertarAlimentos(lista: List<AlimentoGuardado>)
    @Insert suspend fun insertarPesos(lista: List<PesoRegistro>)
}
