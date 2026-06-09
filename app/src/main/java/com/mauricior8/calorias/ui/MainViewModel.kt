package com.mauricior8.calorias.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.repository.MetricaRepository
import com.mauricior8.calorias.util.Calculadora
import com.mauricior8.calorias.ui.state.MetricaConItem
import com.mauricior8.calorias.ui.state.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(
    private val repository: MetricaRepository
) : ViewModel() {

    init {
        sembrarMetricasPorDefecto()
    }

    // ---------------- Estado de Metricas ----------------

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

    fun historial(metricaId: String) = repository.historial(metricaId)

    // ---------------- Estado de Notas ----------------

    val notas: StateFlow<List<Nota>> =
        repository.notas.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ---------------- Estado de Calculadora ----------------

    val historialCalculos: StateFlow<List<CalculoHistorial>> =
        repository.historialCalculos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ---------------- Acciones: Metricas ----------------

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
                    colorHex = colorHex,
                    orden = repository.siguienteOrden()
                )
            )
        }
    }

    /** Edita una metrica existente conservando su id y orden. */
    fun editarMetrica(metrica: MetricaConfig) {
        viewModelScope.launch { repository.guardarMetrica(metrica) }
    }

    fun eliminarMetrica(metrica: MetricaConfig) {
        viewModelScope.launch { repository.eliminarMetrica(metrica) }
    }

    /** Intercambia el orden con el vecino superior. */
    fun moverArriba(metricaId: String) = moverEnDireccion(metricaId, -1)

    /** Intercambia el orden con el vecino inferior. */
    fun moverAbajo(metricaId: String) = moverEnDireccion(metricaId, +1)

    private fun moverEnDireccion(metricaId: String, delta: Int) {
        val items = uiState.value.metricas
        val index = items.indexOfFirst { it.config.id == metricaId }
        if (index < 0) return
        val destino = index + delta
        if (destino < 0 || destino >= items.size) return

        val actual = items[index].config
        val vecino = items[destino].config
        viewModelScope.launch {
            // Intercambiar valores de orden.
            repository.actualizarOrden(actual.id, vecino.orden)
            repository.actualizarOrden(vecino.id, actual.orden)
        }
    }

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

    // ---------------- Acciones: Notas ----------------

    fun guardarNota(texto: String, id: Int = 0) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            repository.guardarNota(Nota(id = id, texto = limpio))
        }
    }

    fun eliminarNota(nota: Nota) {
        viewModelScope.launch { repository.eliminarNota(nota) }
    }

    // ---------------- Acciones: Calculadora ----------------

    /**
     * Evalua la expresion. Si es valida, persiste el calculo y devuelve el
     * resultado formateado; si no, devuelve null.
     */
    fun calcular(expresion: String): String? {
        val resultado = Calculadora.evaluar(expresion) ?: return null
        val texto = formato(resultado)
        viewModelScope.launch {
            repository.guardarCalculo(
                CalculoHistorial(expresion = expresion.trim(), resultado = texto)
            )
        }
        return texto
    }

    fun limpiarHistorialCalculos() {
        viewModelScope.launch { repository.limpiarCalculos() }
    }

    // ---------------- Helpers ----------------

    private fun sembrarMetricasPorDefecto() {
        viewModelScope.launch {
            if (repository.hayMetricas()) return@launch
            val defaults = listOf(
                MetricaConfig("calorias", "Calorias", "kcal", 2000f, "#EF5350", 0),
                MetricaConfig("proteinas", "Proteinas", "gr", 120f, "#66BB6A", 1),
                MetricaConfig("carbohidratos", "Carbohidratos", "gr", 250f, "#42A5F5", 2),
                MetricaConfig("agua", "Agua", "L", 2f, "#26C6DA", 3)
            )
            defaults.forEach { repository.guardarMetrica(it) }
        }
    }

    private fun formato(valor: Float): String =
        if (valor % 1f == 0f) valor.toInt().toString() else "%.2f".format(valor)

    private fun generarId(nombre: String): String {
        val slug = nombre.lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
        return "${slug}_${System.currentTimeMillis()}"
    }

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
