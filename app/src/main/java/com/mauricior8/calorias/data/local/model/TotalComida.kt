package com.mauricior8.calorias.data.local.model

/** Suma de una metrica en una comida concreta (comida null = sin clasificar). */
data class TotalComida(
    val metricaId: String,
    val comida: String?,
    val total: Float
)
