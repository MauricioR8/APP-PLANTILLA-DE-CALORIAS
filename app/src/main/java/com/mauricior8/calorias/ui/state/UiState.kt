package com.mauricior8.calorias.ui.state

import com.mauricior8.calorias.data.local.entity.MetricaConfig

/**
 * Estado inmutable que consume la UI.
 *
 * @param isLoading   Indica carga inicial.
 * @param metricas    Lista de metricas con su total acumulado del dia.
 */
data class UiState(
    val isLoading: Boolean = true,
    val metricas: List<MetricaConItem> = emptyList()
)

/**
 * Une la configuracion de una metrica con su total acumulado de hoy y el
 * porcentaje de progreso respecto al limite (si existe).
 */
data class MetricaConItem(
    val config: MetricaConfig,
    val totalHoy: Float
) {
    val progreso: Float
        get() {
            val limite = config.limiteMaximo ?: return 0f
            if (limite <= 0f) return 0f
            return (totalHoy / limite).coerceIn(0f, 1f)
        }
}
