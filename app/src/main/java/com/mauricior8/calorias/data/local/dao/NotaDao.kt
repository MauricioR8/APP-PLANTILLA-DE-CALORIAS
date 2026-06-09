package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mauricior8.calorias.data.local.entity.Nota
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(nota: Nota): Long

    @Query("SELECT * FROM notas ORDER BY timestamp DESC")
    fun observarNotas(): Flow<List<Nota>>

    @Delete
    suspend fun eliminar(nota: Nota)
}
