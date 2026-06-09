package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mauricior8.calorias.data.local.entity.EstadoDia
import kotlinx.coroutines.flow.Flow

@Dao
interface EstadoDiaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(estado: EstadoDia)

    @Query("SELECT * FROM estados_dia WHERE fecha = :fecha LIMIT 1")
    fun observar(fecha: String): Flow<EstadoDia?>
}
