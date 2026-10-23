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

    /**
     * 7 -> 8: gym (rutinas, ejercicios, series, historial de pesos, nota de sesion), ajustes,
     * suplementos, medidas, totales archivados, limite de dia de descanso y favoritos.
     * Cada CREATE se copia de app/schemas/.../8.json; MigracionesTest compara el resultado
     * con el esquema que Room espera.
     */
    val SENTENCIAS_7_8: List<String> = listOf(
        "ALTER TABLE `metricas` ADD COLUMN `limiteDescanso` REAL",
        "ALTER TABLE `alimentos_guardados` ADD COLUMN `favorito` INTEGER NOT NULL DEFAULT 0",
        "CREATE TABLE IF NOT EXISTS `ajustes` (`clave` TEXT NOT NULL, `valor` TEXT NOT NULL, PRIMARY KEY(`clave`))",
        "CREATE TABLE IF NOT EXISTS `rutinas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `dias` TEXT NOT NULL, `diaria` INTEGER NOT NULL, `nota` TEXT, `orden` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `ejercicios_rutina` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rutinaId` INTEGER NOT NULL, `nombre` TEXT NOT NULL, `grupo` TEXT NOT NULL, `series` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `esTiempo` INTEGER NOT NULL, `descansoSeg` INTEGER NOT NULL, `vecesPorDia` INTEGER NOT NULL, `pesoKg` REAL, `nota` TEXT, `orden` INTEGER NOT NULL, FOREIGN KEY(`rutinaId`) REFERENCES `rutinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_ejercicios_rutina_rutinaId` ON `ejercicios_rutina` (`rutinaId`)",
        "CREATE TABLE IF NOT EXISTS `series_hechas` (`ejercicioId` INTEGER NOT NULL, `fecha` TEXT NOT NULL, `indice` INTEGER NOT NULL, PRIMARY KEY(`ejercicioId`, `fecha`, `indice`), FOREIGN KEY(`ejercicioId`) REFERENCES `ejercicios_rutina`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_series_hechas_ejercicioId` ON `series_hechas` (`ejercicioId`)",
        "CREATE INDEX IF NOT EXISTS `index_series_hechas_fecha` ON `series_hechas` (`fecha`)",
        "CREATE TABLE IF NOT EXISTS `pesos_ejercicio` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ejercicioId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `kg` REAL NOT NULL, FOREIGN KEY(`ejercicioId`) REFERENCES `ejercicios_rutina`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_pesos_ejercicio_ejercicioId` ON `pesos_ejercicio` (`ejercicioId`)",
        "CREATE TABLE IF NOT EXISTS `sesiones_gym` (`fecha` TEXT NOT NULL, `nota` TEXT NOT NULL, PRIMARY KEY(`fecha`))",
        "CREATE TABLE IF NOT EXISTS `suplementos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `orden` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `suplementos_tomados` (`suplementoId` INTEGER NOT NULL, `fecha` TEXT NOT NULL, PRIMARY KEY(`suplementoId`, `fecha`), FOREIGN KEY(`suplementoId`) REFERENCES `suplementos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_suplementos_tomados_suplementoId` ON `suplementos_tomados` (`suplementoId`)",
        "CREATE TABLE IF NOT EXISTS `medidas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `tipo` TEXT NOT NULL, `cm` REAL NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `totales_diarios` (`fecha` TEXT NOT NULL, `metricaId` TEXT NOT NULL, `total` REAL NOT NULL, PRIMARY KEY(`fecha`, `metricaId`))"
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
