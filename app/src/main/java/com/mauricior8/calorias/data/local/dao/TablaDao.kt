package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import kotlinx.coroutines.flow.Flow

@Dao
interface TablaDao {

    // ---- Tablas ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabla(tabla: TablaAlimentos): Long

    @Query("SELECT * FROM tablas ORDER BY timestamp DESC")
    fun observarTablas(): Flow<List<TablaAlimentos>>

    @Delete
    suspend fun eliminarTabla(tabla: TablaAlimentos)

    // ---- Columnas ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumna(columna: ColumnaTabla): Long

    @Query("SELECT * FROM columnas WHERE tablaId = :tablaId ORDER BY orden ASC, id ASC")
    fun observarColumnas(tablaId: Int): Flow<List<ColumnaTabla>>

    @Query("SELECT COUNT(*) FROM columnas WHERE tablaId = :tablaId")
    suspend fun contarColumnas(tablaId: Int): Int

    @Query("SELECT COALESCE(MAX(orden), -1) FROM columnas WHERE tablaId = :tablaId")
    suspend fun maxOrdenColumna(tablaId: Int): Int

    @Delete
    suspend fun eliminarColumna(columna: ColumnaTabla)

    // ---- Filas ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFila(fila: FilaTabla): Long

    @Query("SELECT * FROM filas WHERE tablaId = :tablaId ORDER BY orden ASC, id ASC")
    fun observarFilas(tablaId: Int): Flow<List<FilaTabla>>

    @Query("SELECT COUNT(*) FROM filas WHERE tablaId = :tablaId")
    suspend fun contarFilas(tablaId: Int): Int

    @Query("SELECT COALESCE(MAX(orden), -1) FROM filas WHERE tablaId = :tablaId")
    suspend fun maxOrdenFila(tablaId: Int): Int

    @Delete
    suspend fun eliminarFila(fila: FilaTabla)

    // ---- Celdas ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCelda(celda: CeldaTabla)

    @Query("SELECT * FROM celdas WHERE tablaId = :tablaId")
    fun observarCeldas(tablaId: Int): Flow<List<CeldaTabla>>

    @Query("SELECT * FROM celdas WHERE filaId = :filaId AND columnaId = :columnaId LIMIT 1")
    suspend fun obtenerCelda(filaId: Int, columnaId: Int): CeldaTabla?
}
