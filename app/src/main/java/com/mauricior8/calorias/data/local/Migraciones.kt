package com.mauricior8.calorias.data.local

/**
 * Sentencias SQL de las migraciones de Room, sin dependencias de Android para poder
 * probarlas en la JVM contra SQLite real (ver MigracionesTest).
 */
object Migraciones {

    /**
     * 6 -> 7: comida por registro, tabla de peso corporal y las metricas Grasas y Fibra
     * para quien aun no las tenga. Conserva todos los datos existentes.
     */
    val SENTENCIAS_6_7: List<String> = listOf(
        "ALTER TABLE `registros` ADD COLUMN `comida` TEXT",
        "CREATE TABLE IF NOT EXISTS `peso` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`timestamp` INTEGER NOT NULL, `kg` REAL NOT NULL)",
        metricaSiFalta("grasas", "Grasas", 70.0, "#FF7043", "'grasas','grasa'"),
        metricaSiFalta("fibra", "Fibra", 30.0, "#8D6E63", "'fibra'")
    )

    // Sin agregados en el SELECT principal: un MAX() sin GROUP BY devuelve siempre una
    // fila aunque el WHERE filtre todo. Solo se agrega si la tabla ya tiene metricas
    // (si el usuario las borro todas, no se tocan) y si no existe ya una equivalente.
    private fun metricaSiFalta(
        id: String,
        nombre: String,
        limite: Double,
        color: String,
        nombresExistentes: String
    ): String =
        "INSERT INTO `metricas` " +
            "(`id`, `nombre`, `unidad`, `limiteMaximo`, `colorHex`, `orden`, `tipoGrafica`) " +
            "SELECT '$id', '$nombre', 'gr', $limite, '$color', " +
            "(SELECT COALESCE(MAX(`orden`), -1) + 1 FROM `metricas`), 'Anillo' " +
            "WHERE EXISTS (SELECT 1 FROM `metricas`) " +
            "AND NOT EXISTS (SELECT 1 FROM `metricas` " +
            "WHERE lower(`nombre`) IN ($nombresExistentes) OR `id` = '$id')"
}
