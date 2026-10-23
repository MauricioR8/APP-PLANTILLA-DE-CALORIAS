package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.model.MetricaTotal
import com.mauricior8.calorias.data.local.model.TotalComida
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricaDao {

    // a) Insertar / actualizar metricas (categorias).
    // @Upsert actualiza la fila en su lugar. Con REPLACE SQLite borraba la fila vieja y el
    // ON DELETE CASCADE eliminaba todos los registros de la metrica al editarla.
    @Upsert
    suspend fun upsertMetrica(metrica: MetricaConfig)

    // b) Insertar un registro de suma (ingreso diario).
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegistro(registro: RegistroSuma): Long

    // c) Obtener todas las metricas como Flow reactivo, ordenadas por posicion.
    @Query("SELECT * FROM metricas ORDER BY orden ASC, nombre ASC")
    fun observarMetricas(): Flow<List<MetricaConfig>>

    /**
     * d) Suma total de los valores ingresados HOY, agrupados por metricaId.
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

    // Totales del dia desglosados por comida (solo registros con comida asignada).
    @Query(
        """
        SELECT metricaId AS metricaId, comida AS comida, SUM(valor) AS total
        FROM registros
        WHERE timestamp >= :inicioDia AND timestamp < :finDia AND comida IS NOT NULL
        GROUP BY metricaId, comida
        """
    )
    fun observarTotalesPorComida(inicioDia: Long, finDia: Long): Flow<List<TotalComida>>

    // Registros en un rango (para las graficas de progreso semanal/mensual).
    @Query("SELECT * FROM registros WHERE timestamp >= :inicio AND timestamp < :fin")
    fun observarRegistrosEnRango(inicio: Long, fin: Long): Flow<List<RegistroSuma>>

    // e) Historial completo de una metrica, mas reciente primero.
    @Query(
        """
        SELECT * FROM registros
        WHERE metricaId = :metricaId
        ORDER BY timestamp DESC
        """
    )
    fun observarHistorial(metricaId: String): Flow<List<RegistroSuma>>

    // Eliminar una metrica (los registros se borran en cascada).
    @Delete
    suspend fun eliminarMetrica(metrica: MetricaConfig)

    // Eliminar todos los registros dentro de un rango (para "limpiar" un dia).
    @Query("DELETE FROM registros WHERE timestamp >= :inicio AND timestamp < :fin")
    suspend fun eliminarRegistrosEnRango(inicio: Long, fin: Long)

    // Eliminar los registros de UNA metrica dentro de un rango (limpiar esa tarjeta).
    @Query("DELETE FROM registros WHERE metricaId = :metricaId AND timestamp >= :inicio AND timestamp < :fin")
    suspend fun eliminarRegistrosDeMetricaEnRango(metricaId: String, inicio: Long, fin: Long)

    // Eliminar un unico registro (para corregir una captura equivocada).
    @Query("DELETE FROM registros WHERE id = :id")
    suspend fun eliminarRegistro(id: Int)

    // Actualizar el orden de una metrica concreta.
    @Query("UPDATE metricas SET orden = :nuevoOrden WHERE id = :id")
    suspend fun actualizarOrden(id: String, nuevoOrden: Int)

    // Intercambia el orden de dos metricas en una sola sentencia (atomico).
    @Query(
        """
        UPDATE metricas
        SET orden = CASE id WHEN :idA THEN :ordenB WHEN :idB THEN :ordenA END
        WHERE id IN (:idA, :idB)
        """
    )
    suspend fun intercambiarOrden(idA: String, ordenA: Int, idB: String, ordenB: Int)

    // Maximo orden actual (para colocar nuevas metricas al final).
    @Query("SELECT COALESCE(MAX(orden), -1) FROM metricas")
    suspend fun maxOrden(): Int

    // Util: contar metricas existentes (para sembrar datos por defecto la 1a vez).
    @Query("SELECT COUNT(*) FROM metricas")
    suspend fun contarMetricas(): Int
}
