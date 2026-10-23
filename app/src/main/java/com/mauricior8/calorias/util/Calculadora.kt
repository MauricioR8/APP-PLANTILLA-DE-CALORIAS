package com.mauricior8.calorias.util

/**
 * Evaluador de expresiones aritmeticas sencillo (recursive descent).
 * Soporta + - * / %, parentesis y decimales. Devuelve null si la expresion
 * es invalida o hay division por cero.
 *
 * Gramatica:
 *   expr   = term { ("+" | "-") term }
 *   term   = factor { ("*" | "/" | "%") factor }
 *   factor = number | "(" expr ")" | "-" factor
 */
object Calculadora {

    fun evaluar(entrada: String): Double? {
        if (entrada.isBlank()) return null
        return try {
            val parser = Parser(entrada)
            val resultado = parser.parseExpresion()
            if (!parser.finalizado()) return null
            if (resultado.isNaN() || resultado.isInfinite()) null else resultado
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(texto: String) {
        // Normaliza separadores comunes y elimina espacios.
        private val s = texto.replace(',', '.').replace("x", "*").replace("X", "*")
        private var pos = 0

        fun finalizado(): Boolean {
            saltarEspacios()
            return pos >= s.length
        }

        fun parseExpresion(): Double {
            var valor = parseTermino()
            while (true) {
                saltarEspacios()
                when (peek()) {
                    '+' -> { pos++; valor += parseTermino() }
                    '-' -> { pos++; valor -= parseTermino() }
                    else -> return valor
                }
            }
        }

        private fun parseTermino(): Double {
            var valor = parseFactor()
            while (true) {
                saltarEspacios()
                when (peek()) {
                    '*' -> { pos++; valor *= parseFactor() }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("div/0")
                        valor /= divisor
                    }
                    '%' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("mod/0")
                        valor %= divisor
                    }
                    else -> return valor
                }
            }
        }

        private fun parseFactor(): Double {
            saltarEspacios()
            return when (peek()) {
                '(' -> {
                    pos++
                    val valor = parseExpresion()
                    saltarEspacios()
                    if (peek() != ')') throw IllegalStateException("falta )")
                    pos++
                    valor
                }
                '-' -> { pos++; -parseFactor() }
                '+' -> { pos++; parseFactor() }
                else -> parseNumero()
            }
        }

        private fun parseNumero(): Double {
            saltarEspacios()
            val inicio = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (pos == inicio) throw IllegalStateException("numero esperado")
            return s.substring(inicio, pos).toDouble()
        }

        private fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'

        private fun saltarEspacios() {
            while (pos < s.length && s[pos] == ' ') pos++
        }
    }
}
