package com.mauricior8.calorias.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mauricior8.calorias.data.local.dao.CalculoDao
import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.dao.NotaDao
import com.mauricior8.calorias.data.local.dao.TablaDao
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
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
        CeldaTabla::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun metricaDao(): MetricaDao
    abstract fun notaDao(): NotaDao
    abstract fun calculoDao(): CalculoDao
    abstract fun tablaDao(): TablaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorias.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
