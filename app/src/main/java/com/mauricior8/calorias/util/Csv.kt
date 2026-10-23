package com.mauricior8.calorias.util

/** Construccion de CSV compatible con Excel/Sheets (separador coma, comillas dobles, UTF-8). */
object Csv {

    /** Escapa un campo: lo entrecomilla si lleva coma, comillas o saltos de linea. */
    fun campo(valor: Any?): String {
        val s = valor?.toString() ?: ""
        // Evita "inyeccion de formulas" al abrir el CSV en una hoja de calculo.
        val seguro = if (s.isNotEmpty() && s[0] in "=+@\t\r" || (s.startsWith("-") && s.length > 1 && !s[1].isDigit())) "'$s" else s
        return if (seguro.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + seguro.replace("\"", "\"\"") + "\""
        } else seguro
    }

    fun fila(vararg valores: Any?): String = valores.joinToString(",") { campo(it) }

    fun documento(encabezado: List<String>, filas: List<List<Any?>>): String =
        buildString {
            append('﻿') // BOM: Excel reconoce UTF-8 y los acentos
            append(fila(*encabezado.toTypedArray())).append("\r\n")
            filas.forEach { append(fila(*it.toTypedArray())).append("\r\n") }
        }
}
