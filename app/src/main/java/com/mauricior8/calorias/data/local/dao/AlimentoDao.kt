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

    // Favoritos primero y luego los mas recientes.
    @Query("SELECT * FROM alimentos_guardados ORDER BY favorito DESC, timestamp DESC")
    fun observar(): Flow<List<AlimentoGuardado>>

    @Query("UPDATE alimentos_guardados SET timestamp = :timestamp WHERE id = :id")
    suspend fun tocar(id: Int, timestamp: Long)

    @Query("UPDATE alimentos_guardados SET favorito = :favorito WHERE id = :id")
    suspend fun marcarFavorito(id: Int, favorito: Boolean)

    @Delete
    suspend fun eliminar(alimento: AlimentoGuardado)
}
