package com.mauricior8.calorias.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mauricior8.calorias.data.local.dao.AjusteDao
import com.mauricior8.calorias.data.local.dao.AlimentoDao
import com.mauricior8.calorias.data.local.dao.CalculoDao
import com.mauricior8.calorias.data.local.dao.EstadoDiaDao
import com.mauricior8.calorias.data.local.dao.GymDao
import com.mauricior8.calorias.data.local.dao.MedidaDao
import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.dao.NotaDao
import com.mauricior8.calorias.data.local.dao.PesoDao
import com.mauricior8.calorias.data.local.dao.RespaldoDao
import com.mauricior8.calorias.data.local.dao.SuplementoDao
import com.mauricior8.calorias.data.local.dao.TotalDiarioDao
import com.mauricior8.calorias.data.local.dao.TablaDao
import com.mauricior8.calorias.data.local.entity.Ajuste
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.Medida
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.PesoEjercicio
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.Rutina
import com.mauricior8.calorias.data.local.entity.SerieHecha
import com.mauricior8.calorias.data.local.entity.SesionGym
import com.mauricior8.calorias.data.local.entity.Suplemento
import com.mauricior8.calorias.data.local.entity.SuplementoTomado
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.local.entity.TotalDiario

@Database(
    entities = [
        MetricaConfig::class,
        RegistroSuma::class,
        Nota::class,
        CalculoHistorial::class,
        TablaAlimentos::class,
        ColumnaTabla::class,
        FilaTabla::class,
        CeldaTabla::class,
        EstadoDia::class,
        AlimentoGuardado::class,
        PesoRegistro::class,
        Ajuste::class,
        Rutina::class,
        EjercicioRutina::class,
        SerieHecha::class,
        PesoEjercicio::class,
        SesionGym::class,
        Suplemento::class,
        SuplementoTomado::class,
        Medida::class,
        TotalDiario::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun metricaDao(): MetricaDao
    abstract fun notaDao(): NotaDao
    abstract fun calculoDao(): CalculoDao
    abstract fun tablaDao(): TablaDao
    abstract fun estadoDiaDao(): EstadoDiaDao
    abstract fun alimentoDao(): AlimentoDao
    abstract fun pesoDao(): PesoDao
    abstract fun respaldoDao(): RespaldoDao
    abstract fun gymDao(): GymDao
    abstract fun ajusteDao(): AjusteDao
    abstract fun suplementoDao(): SuplementoDao
    abstract fun medidaDao(): MedidaDao
    abstract fun totalDiarioDao(): TotalDiarioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** 7 -> 8: gym, ajustes, suplementos, medidas y totales archivados. */
        val MIGRACION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Migraciones.SENTENCIAS_7_8.forEach { db.execSQL(it) }
            }
        }

        /** 6 -> 7. Las sentencias viven en [Migraciones] para poder probarlas en la JVM. */
        val MIGRACION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Migraciones.SENTENCIAS_6_7.forEach { db.execSQL(it) }
            }
        }
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorias.db"
                )
                    // Solo las versiones antiguas (sin esquema exportado) se reinician.
                    // De la 6 en adelante cada cambio debe traer su migracion para no
                    // borrar los datos del usuario.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .addMigrations(MIGRACION_6_7, MIGRACION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
