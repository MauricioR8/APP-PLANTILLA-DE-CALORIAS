package com.mauricior8.calorias.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.repository.MetricaRepository
import com.mauricior8.calorias.ui.state.MetricaConItem
import com.mauricior8.calorias.ui.state.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(
    private val repository: MetricaRepository
) : ViewModel() {

    init {
        sembrarMetricasPorDefecto()
    }

    /**
     * Crea un set inicial de metricas la primera vez que se abre la app.
     */
    private fun sembrarMetricasPorDefecto() {
        viewModelScope.launch {
            if (repository.hayMetricas()) return@launch
            val defaults = listOf(
                MetricaConfig("calorias", "Calorias", "kcal", 2000f, "#FF6D00"),
                MetricaConfig("proteinas", "Proteinas", "gr", 120f, "#2E7D32"),
                MetricaConfig("carbohidratos", "Carbohidratos", "gr", 250f, "#1565C0"),
                MetricaConfig("agua", "Agua", "L", 2f, "#0097A7")
            )
            defaults.forEach { repository.guardarMetrica(it) }
        }
    }

    /**
     * UiState derivado: combina la lista de metricas con los totales del dia.
     * Se expone como StateFlow para que Compose lo observe de forma reactiva.
     */
    val uiState: StateFlow<UiState> =
        combine(
            repository.metricas,
            repository.totalesDeHoy
        ) { metricas, totales ->
            val totalesPorId = totales.associate { it.metricaId to it.total }
            val items = metricas.map { config ->
                MetricaConItem(
                    config = config,
                    totalHoy = totalesPorId[config.id] ?: 0f
                )
            }
            UiState(isLoading = false, metricas = items)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(isLoading = true)
        )

    /** Historial reactivo de una metrica concreta. */
    fun historial(metricaId: String) =
        repository.historial(metricaId)

    /** Crea una nueva metrica/categoria definida por el usuario. */
    fun crearMetrica(
        nombre: String,
        unidad: String,
        limiteMaximo: Float? = null,
        colorHex: String = "#FF6D00"
    ) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return
        viewModelScope.launch {
            repository.guardarMetrica(
                MetricaConfig(
                    id = generarId(nombreLimpio),
                    nombre = nombreLimpio,
                    unidad = unidad,
                    limiteMaximo = limiteMaximo,
                    colorHex = colorHex
                )
            )
        }
    }

    /** Inserta un nuevo registro de suma para una metrica. */
    fun agregarRegistro(
        metricaId: String,
        valor: Float,
        tipo: String = "manual",
        detalle: String? = null
    ) {
        if (valor <= 0f) return
        viewModelScope.launch {
            repository.agregarRegistro(
                RegistroSuma(
                    metricaId = metricaId,
                    valor = valor,
                    tipo = tipo,
                    detalle = detalle
                )
            )
        }
    }

    private fun generarId(nombre: String): String {
        val slug = nombre.lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
        return "${slug}_${System.currentTimeMillis()}"
    }

    /**
     * Factory para inyectar el repositorio sin librerias de DI externas.
     */
    class Factory(
        private val repository: MetricaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
                "Unknown ViewModel class ${modelClass.name}"
            }
            return MainViewModel(repository) as T
        }
    }
}
