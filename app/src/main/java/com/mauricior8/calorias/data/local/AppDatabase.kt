package com.mauricior8.calorias.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.RegistroSuma

@Database(
    entities = [MetricaConfig::class, RegistroSuma::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun metricaDao(): MetricaDao

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
