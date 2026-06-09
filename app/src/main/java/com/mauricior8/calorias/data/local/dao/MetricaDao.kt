package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.model.MetricaTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricaDao {

    // a) Insertar / actualizar metricas (categorias).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetrica(metrica: MetricaConfig)

    // b) Insertar un registro de suma (ingreso diario).
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegistro(registro: RegistroSuma): Long

    // c) Obtener todas las metricas como Flow reactivo.
    @Query("SELECT * FROM metricas ORDER BY nombre ASC")
    fun observarMetricas(): Flow<List<MetricaConfig>>

    /**
     * d) Suma total de los valores ingresados HOY, agrupados por metricaId.
     *
     * Se filtra por el rango [inicioDia, finDia) en milisegundos epoch, que el
     * repositorio calcula para el dia actual del dispositivo.
     */
    @Query(
        """
        SELECT metricaId AS metricaId, SUM(valor) AS total
        FROM registros
        WHERE timestamp >= :inicioDia AND timestamp < :finDia
        GROUP BY metricaId
        """
    )
    fun observarTotalesDelDia(inicioDia: Long, finDia: Long): Flow<List<MetricaTotal>>

    // e) Historial completo de una metrica, mas reciente primero.
    @Query(
        """
        SELECT * FROM registros
        WHERE metricaId = :metricaId
        ORDER BY timestamp DESC
        """
    )
    fun observarHistorial(metricaId: String): Flow<List<RegistroSuma>>

    // Util: contar metricas existentes (para sembrar datos por defecto la 1a vez).
    @Query("SELECT COUNT(*) FROM metricas")
    suspend fun contarMetricas(): Int
}
