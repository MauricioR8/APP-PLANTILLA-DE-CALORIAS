package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.mauricior8.calorias.data.local.entity.Ajuste
import com.mauricior8.calorias.data.local.entity.Medida
import com.mauricior8.calorias.data.local.entity.Suplemento
import com.mauricior8.calorias.data.local.entity.SuplementoTomado
import com.mauricior8.calorias.data.local.entity.TotalDiario
import kotlinx.coroutines.flow.Flow

@Dao
interface AjusteDao {
    @Query("SELECT * FROM ajustes")
    fun observar(): Flow<List<Ajuste>>

    @Query("SELECT valor FROM ajustes WHERE clave = :clave")
    suspend fun valor(clave: String): String?

    @Upsert
    suspend fun guardar(ajuste: Ajuste)
}

@Dao
interface SuplementoDao {
    @Query("SELECT * FROM suplementos ORDER BY orden ASC, id ASC")
    fun observar(): Flow<List<Suplemento>>

    @Insert
    suspend fun insertar(suplemento: Suplemento): Long

    @Update
    suspend fun actualizar(suplemento: Suplemento)

    @Query("DELETE FROM suplementos WHERE id = :id")
    suspend fun eliminar(id: Int)

    @Query("SELECT COALESCE(MAX(orden), -1) FROM suplementos")
    suspend fun maxOrden(): Int

    @Query("SELECT * FROM suplementos_tomados WHERE fecha = :fecha")
    fun observarTomados(fecha: String): Flow<List<SuplementoTomado>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun marcarTomado(tomado: SuplementoTomado)

    @Query("DELETE FROM suplementos_tomados WHERE suplementoId = :id AND fecha = :fecha")
    suspend fun desmarcarTomado(id: Int, fecha: String)
}

@Dao
interface MedidaDao {
    @Query("SELECT * FROM medidas ORDER BY timestamp ASC")
    fun observar(): Flow<List<Medida>>

    @Insert
    suspend fun insertar(medida: Medida)

    @Query("DELETE FROM medidas WHERE id = :id")
    suspend fun eliminar(id: Int)
}

@Dao
interface TotalDiarioDao {
    @Query("SELECT * FROM totales_diarios WHERE fecha = :fecha")
    fun observarDia(fecha: String): Flow<List<TotalDiario>>

    @Query("SELECT * FROM totales_diarios WHERE fecha >= :desde AND fecha <= :hasta")
    fun observarRango(desde: String, hasta: String): Flow<List<TotalDiario>>

    @Query("SELECT * FROM totales_diarios WHERE fecha = :fecha AND metricaId = :metricaId")
    suspend fun obtener(fecha: String, metricaId: String): TotalDiario?

    @Upsert
    suspend fun guardar(total: TotalDiario)

    @Query("DELETE FROM totales_diarios WHERE metricaId = :metricaId")
    suspend fun eliminarDeMetrica(metricaId: String)
}
