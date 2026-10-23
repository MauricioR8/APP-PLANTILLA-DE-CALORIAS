package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import kotlinx.coroutines.flow.Flow

@Dao
interface PesoDao {

    @Insert
    suspend fun insertar(peso: PesoRegistro): Long

    // Del mas antiguo al mas reciente (orden natural para graficar).
    @Query("SELECT * FROM peso ORDER BY timestamp ASC")
    fun observar(): Flow<List<PesoRegistro>>

    @Query("DELETE FROM peso WHERE id = :id")
    suspend fun eliminar(id: Int)
}
