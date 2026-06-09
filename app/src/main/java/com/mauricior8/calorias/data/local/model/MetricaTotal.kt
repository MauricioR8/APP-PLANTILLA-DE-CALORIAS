package com.mauricior8.calorias.data.local.model

/**
 * Resultado de la consulta agregada: suma total de una metrica en un periodo.
 *
 * @param metricaId  Id de la metrica agrupada.
 * @param total      Suma de todos los valores registrados.
 */
data class MetricaTotal(
    val metricaId: String,
    val total: Float
)
