package com.mauricior8.calorias.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mauricior8.calorias.data.local.dao.AlimentoDao
import com.mauricior8.calorias.data.local.dao.CalculoDao
import com.mauricior8.calorias.data.local.dao.EstadoDiaDao
import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.dao.NotaDao
import com.mauricior8.calorias.data.local.dao.PesoDao
import com.mauricior8.calorias.data.local.dao.RespaldoDao
import com.mauricior8.calorias.data.local.dao.TablaDao
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.TablaAlimentos

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
        PesoRegistro::class
    ],
    version = 7,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                    .addMigrations(MIGRACION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
