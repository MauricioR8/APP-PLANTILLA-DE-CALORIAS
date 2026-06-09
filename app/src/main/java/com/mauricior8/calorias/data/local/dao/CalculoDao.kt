package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(calculo: CalculoHistorial): Long

    @Query("SELECT * FROM calculos ORDER BY timestamp DESC LIMIT 100")
    fun observarHistorial(): Flow<List<CalculoHistorial>>

    @Query("DELETE FROM calculos")
    suspend fun limpiar()
}
