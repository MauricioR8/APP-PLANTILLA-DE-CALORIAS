package com.mauricior8.calorias.data.seed

import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.Rutina

/**
 * Rutina inicial tomada de la nota "PESOS GYM" del usuario. Todo es editable desde la app.
 *
 * Los valores que la nota daba en "placas" de la maquina se guardan tal cual en el campo de
 * kilos y se marcan en la nota del ejercicio, para no perder el dato original. Los "[¿?]" de
 * la nota quedan sin peso (pendientes de definir).
 */
object RutinaInicial {

    /** Rutina con sus ejercicios (el rutinaId real se asigna al insertar). */
    class Bloque(val rutina: Rutina, val ejercicios: List<EjercicioRutina>)

    private const val PLACAS = "Valor original de la nota en placas de la máquina"

    private fun e(
        nombre: String, grupo: String, series: Int, reps: Int, peso: Float? = null,
        nota: String? = null, descanso: Int = 90, tiempo: Boolean = false, veces: Int = 1
    ) = EjercicioRutina(
        rutinaId = 0, nombre = nombre, grupo = grupo, series = series, reps = reps,
        esTiempo = tiempo, descansoSeg = descanso, vecesPorDia = veces, pesoKg = peso, nota = nota
    )

    val bloques: List<Bloque> = listOf(
        Bloque(
            Rutina(
                nombre = "Diario", colorHex = "#22C55E", dias = "", diaria = true,
                nota = "Solo respirar por la nariz en el gym", orden = 0
            ),
            listOf(
                e("Slant board", "Movilidad", series = 3, reps = 80, descanso = 15, tiempo = true,
                    nota = "80 segundos por serie, 15 s de descanso"),
                e("Espirómetro de flujo", "Respiración", series = 3, reps = 10, descanso = 90, veces = 2,
                    nota = "Respiración. 2 veces al día. Descanso entre series 1:30")
            )
        ),
        Bloque(
            Rutina(nombre = "Lunes y Jueves · Empuje", colorHex = "#DC2626", dias = "1,4", orden = 1),
            listOf(
                e("Press inclinado", "Pecho", 3, 10, 17.5f, "kg por lado"),
                e("Press sentado en máquina", "Pecho", 3, 10, 40f, "kg por lado"),
                e("Copa de tríceps en polea", "Tríceps", 3, 8, 8f, PLACAS),
                e("Press francés en polea", "Tríceps", 3, 8, 8f, PLACAS),
                e("Elevaciones en polea", "Hombro", 3, 10, 3f, PLACAS)
            )
        ),
        Bloque(
            Rutina(nombre = "Martes y Viernes · Jalón", colorHex = "#2563EB", dias = "2,5", orden = 2),
            listOf(
                e("Jalón al pecho", "Espalda", 3, 10, 65f),
                e("Remo con agarre largo", "Espalda", 3, 10, 8f, PLACAS),
                e("Pull over", "Espalda", 3, 10, null, "Peso por definir"),
                e("Pájaros en máquina", "Hombro", 3, 8, 3f, PLACAS),
                e("Curl de bíceps parado", "Bíceps", 3, 10, 16f, "Mancuerna"),
                e("Curl bayesian en polea", "Bíceps", 3, 8, 4f, PLACAS)
            )
        ),
        Bloque(
            Rutina(nombre = "Miércoles y Sábado · Pierna", colorHex = "#EAB308", dias = "3,6", orden = 3),
            listOf(
                e("Sentadilla sissy", "Cuádriceps", 4, 10, null, "Profundidad: baja"),
                e("Extensión de cuádriceps", "Cuádriceps", 3, 10, null, "Peso por definir"),
                e("Femoral sentado", "Femoral", 3, 10, null, "Peso por definir"),
                e("Glúteo en abductores", "Glúteo", 3, 10, null, "Peso por definir"),
                e("Glúteo en máquina", "Glúteo", 3, 8, null, "Peso por definir"),
                e("Gemelo en burro", "Gemelo", 3, 10, null, "Kg totales. Peso por definir"),
                e("Elevaciones de pierna", "Abdomen", 3, 12, null, "Calidad (sin peso)")
            )
        )
    )
}
