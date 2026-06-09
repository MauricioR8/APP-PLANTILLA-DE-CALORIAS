package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import kotlinx.coroutines.flow.Flow

@Dao
interface AlimentoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(alimento: AlimentoGuardado): Long

    @Update
    suspend fun actualizar(alimento: AlimentoGuardado)

    @Query("SELECT * FROM alimentos_guardados ORDER BY timestamp DESC")
    fun observar(): Flow<List<AlimentoGuardado>>

    @Delete
    suspend fun eliminar(alimento: AlimentoGuardado)
}
