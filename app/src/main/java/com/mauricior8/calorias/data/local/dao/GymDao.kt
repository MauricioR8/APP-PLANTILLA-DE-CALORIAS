package com.mauricior8.calorias.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.PesoEjercicio
import com.mauricior8.calorias.data.local.entity.Rutina
import com.mauricior8.calorias.data.local.entity.SerieHecha
import com.mauricior8.calorias.data.local.entity.SesionGym
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {

    // ---- Rutinas ----
    @Query("SELECT * FROM rutinas ORDER BY orden ASC, id ASC")
    fun observarRutinas(): Flow<List<Rutina>>

    @Insert
    suspend fun insertarRutina(rutina: Rutina): Long

    @Update
    suspend fun actualizarRutina(rutina: Rutina)

    @Delete
    suspend fun eliminarRutina(rutina: Rutina)

    @Query("SELECT COALESCE(MAX(orden), -1) FROM rutinas")
    suspend fun maxOrdenRutina(): Int

    @Query("SELECT COUNT(*) FROM rutinas")
    suspend fun contarRutinas(): Int

    // ---- Ejercicios ----
    @Query("SELECT * FROM ejercicios_rutina ORDER BY rutinaId ASC, orden ASC, id ASC")
    fun observarEjercicios(): Flow<List<EjercicioRutina>>

    @Insert
    suspend fun insertarEjercicio(ejercicio: EjercicioRutina): Long

    @Update
    suspend fun actualizarEjercicio(ejercicio: EjercicioRutina)

    @Delete
    suspend fun eliminarEjercicio(ejercicio: EjercicioRutina)

    @Query("UPDATE ejercicios_rutina SET pesoKg = :kg WHERE id = :id")
    suspend fun actualizarPeso(id: Int, kg: Float)

    @Query("SELECT COALESCE(MAX(orden), -1) FROM ejercicios_rutina WHERE rutinaId = :rutinaId")
    suspend fun maxOrdenEjercicio(rutinaId: Int): Int

    @Query("UPDATE ejercicios_rutina SET orden = CASE id WHEN :idA THEN :ordenB WHEN :idB THEN :ordenA END WHERE id IN (:idA, :idB)")
    suspend fun intercambiarOrdenEjercicios(idA: Int, ordenA: Int, idB: Int, ordenB: Int)

    // ---- Series hechas ----
    @Query("SELECT * FROM series_hechas WHERE fecha = :fecha")
    fun observarSeries(fecha: String): Flow<List<SerieHecha>>

    @Query("SELECT * FROM series_hechas WHERE fecha >= :desde AND fecha <= :hasta")
    fun observarSeriesEnRango(desde: String, hasta: String): Flow<List<SerieHecha>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun marcarSerie(serie: SerieHecha)

    @Query("DELETE FROM series_hechas WHERE ejercicioId = :ejercicioId AND fecha = :fecha AND indice = :indice")
    suspend fun desmarcarSerie(ejercicioId: Int, fecha: String, indice: Int)

    @Query("DELETE FROM series_hechas WHERE fecha = :fecha")
    suspend fun limpiarSeriesDelDia(fecha: String)

    // ---- Historial de pesos por ejercicio ----
    @Query("SELECT * FROM pesos_ejercicio ORDER BY timestamp ASC")
    fun observarPesos(): Flow<List<PesoEjercicio>>

    @Insert
    suspend fun insertarPeso(peso: PesoEjercicio)

    // ---- Nota de la sesion ----
    @Query("SELECT * FROM sesiones_gym WHERE fecha = :fecha")
    fun observarSesion(fecha: String): Flow<SesionGym?>

    @Upsert
    suspend fun guardarSesion(sesion: SesionGym)

    @Query("DELETE FROM sesiones_gym WHERE fecha = :fecha")
    suspend fun borrarSesion(fecha: String)
}
