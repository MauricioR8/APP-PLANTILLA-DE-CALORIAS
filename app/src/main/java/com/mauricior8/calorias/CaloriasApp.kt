package com.mauricior8.calorias

import android.app.Application
import com.mauricior8.calorias.data.local.AppDatabase
import com.mauricior8.calorias.data.repository.MetricaRepository

/**
 * Application personalizada que actua como contenedor simple de dependencias
 * (Service Locator) para evitar librerias de DI en esta plantilla base.
 */
class CaloriasApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: MetricaRepository by lazy {
        MetricaRepository(
            db = database,
            pesoDao = database.pesoDao(),
            respaldoDao = database.respaldoDao(),
            gymDao = database.gymDao(),
            ajusteDao = database.ajusteDao(),
            suplementoDao = database.suplementoDao(),
            medidaDao = database.medidaDao(),
            totalDiarioDao = database.totalDiarioDao(),
            metricaDao = database.metricaDao(),
            notaDao = database.notaDao(),
            calculoDao = database.calculoDao(),
            tablaDao = database.tablaDao(),
            estadoDiaDao = database.estadoDiaDao(),
            alimentoDao = database.alimentoDao()
        )
    }
}
