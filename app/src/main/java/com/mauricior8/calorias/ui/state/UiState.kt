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
    val metricas: List<MetricaConItem> = emptyList(),
    val diaDescanso: Boolean = false
)

/**
 * Totales diarios de los ultimos [dias] dias (el ultimo es hoy).
 *
 * @param etiquetas Fecha corta (dd/MM) de cada dia, del mas antiguo al mas reciente.
 * @param totales   metricaId -> total de cada dia (misma longitud que [etiquetas]).
 */
data class Progreso(
    val dias: Int = 7,
    val etiquetas: List<String> = emptyList(),
    val totales: Map<String, List<Float>> = emptyMap()
)

/**
 * Une la configuracion de una metrica con su total acumulado de hoy y el
 * porcentaje de progreso respecto al limite (si existe).
 */
data class MetricaConItem(
    val config: MetricaConfig,
    val totalHoy: Float,
    val diaDescanso: Boolean = false
) {
    /** Limite que aplica hoy: el de descanso (si existe) en dias sin entrenamiento. */
    val limite: Float?
        get() = if (diaDescanso) config.limiteDescanso ?: config.limiteMaximo else config.limiteMaximo

    val progreso: Float
        get() {
            val limite = limite ?: return 0f
            if (limite <= 0f) return 0f
            return (totalHoy / limite).coerceIn(0f, 1f)
        }
}
