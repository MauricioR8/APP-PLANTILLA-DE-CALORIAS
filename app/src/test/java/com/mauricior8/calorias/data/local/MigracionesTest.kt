package com.mauricior8.calorias.data.local

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Prueba las migraciones contra SQLite real usando los esquemas que Room exporta a
 * app/schemas. Si cambias las entidades sin migracion, o la migracion no deja el
 * esquema exactamente como lo espera Room, estas pruebas fallan (y la app se caeria
 * al abrir la base de datos en el telefono).
 */
class MigracionesTest {

    private val carpeta = File("schemas/com.mauricior8.calorias.data.local.AppDatabase")
    private lateinit var conn: Connection

    @Before fun abrir() {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
    }

    @After fun cerrar() = conn.close()

    // ---------- utilidades ----------

    private fun esquema(version: Int): JSONObject =
        JSONObject(File(carpeta, "$version.json").readText()).getJSONObject("database")

    /** Crea las tablas e indices tal como los crearia Room para esa version. */
    private fun crear(c: Connection, version: Int) {
        val entidades = esquema(version).getJSONArray("entities")
        for (i in 0 until entidades.length()) {
            val e = entidades.getJSONObject(i)
            val tabla = e.getString("tableName")
            c.createStatement().use { it.execute(e.getString("createSql").replace("\${TABLE_NAME}", tabla)) }
            val indices = e.getJSONArray("indices")
            for (j in 0 until indices.length()) {
                val sql = indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tabla)
                c.createStatement().use { it.execute(sql) }
            }
        }
    }

    private fun exec(sql: String) = conn.createStatement().use { it.execute(sql) }

    private fun <T> consulta(c: Connection, sql: String, fila: (java.sql.ResultSet) -> T): List<T> =
        c.createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                val out = mutableListOf<T>()
                while (rs.next()) out += fila(rs)
                out
            }
        }

    /** Descripcion comparable del esquema: columnas, claves foraneas e indices por tabla. */
    private fun descripcion(c: Connection): Map<String, List<String>> {
        val tablas = consulta(c, "SELECT name FROM sqlite_master WHERE type='table' " +
            "AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata','room_master_table') " +
            "ORDER BY name") { it.getString(1) }
        return tablas.associateWith { t ->
            val cols = consulta(c, "PRAGMA table_info(`$t`)") {
                "col ${it.getString("name")} ${it.getString("type")} nn=${it.getInt("notnull")} " +
                    "def=${it.getString("dflt_value")} pk=${it.getInt("pk")}"
            }
            val fks = consulta(c, "PRAGMA foreign_key_list(`$t`)") {
                "fk ${it.getString("from")}->${it.getString("table")}.${it.getString("to")} " +
                    "del=${it.getString("on_delete")}"
            }.sorted()
            val idx = consulta(c, "PRAGMA index_list(`$t`)") { it.getString("name") to it.getInt("unique") }
                .filter { !it.first.startsWith("sqlite_autoindex") }
                .map { (n, u) ->
                    val cols2 = consulta(c, "PRAGMA index_info(`$n`)") { it.getString("name") }
                    "idx $n unique=$u $cols2"
                }.sorted()
            cols + fks + idx
        }
    }

    /** Base v6 como la dejaria la app: metricas por defecto + datos de ejemplo. */
    private fun cargarBaseV6(conMetricas: Boolean = true) {
        crear(conn, 6)
        if (conMetricas) {
            exec("INSERT INTO metricas VALUES ('calorias','Calorías','kcal',2000.0,'#EF5350',0,'Anillo')")
            exec("INSERT INTO metricas VALUES ('proteinas','Proteínas','gr',120.0,'#66BB6A',1,'Anillo')")
            exec("INSERT INTO metricas VALUES ('carbohidratos','Carbohidratos','gr',250.0,'#42A5F5',2,'Anillo')")
            exec("INSERT INTO metricas VALUES ('azucar','Azúcar','gr',30.0,'#EC407A',3,'Pastel')")
            exec("INSERT INTO metricas VALUES ('sodio','Sodio','mg',2000.0,'#FFCA28',4,'Barras')")
            exec("INSERT INTO metricas VALUES ('agua','Agua','L',2.0,'#26C6DA',5,'Anillo')")
            exec("INSERT INTO registros (metricaId, valor, timestamp, tipo, detalle) " +
                "VALUES ('calorias', 450.5, 1700000000000, 'alimento', 'Avena')")
            exec("INSERT INTO registros (metricaId, valor, timestamp, tipo, detalle) " +
                "VALUES ('agua', 0.5, 1700000100000, 'manual', NULL)")
        }
        exec("INSERT INTO notas (texto, timestamp) VALUES ('mi nota', 1)")
        exec("INSERT INTO tablas (nombre, tipoGrafica, timestamp) VALUES ('Frutas', 'barras', 2)")
        exec("INSERT INTO columnas (tablaId, nombre, orden) VALUES (1, 'Kcal', 0)")
        exec("INSERT INTO filas (tablaId, nombre, orden) VALUES (1, 'Manzana', 0)")
        exec("INSERT INTO celdas (tablaId, filaId, columnaId, valor) VALUES (1, 1, 1, '52')")
        exec("INSERT INTO estados_dia VALUES ('2026-10-22', 1)")
    }

    private fun migrar() = Migraciones.SENTENCIAS_6_7.forEach { exec(it) }

    private fun migrar7a8() = Migraciones.SENTENCIAS_7_8.forEach { exec(it) }

    private fun nombresMetricas() =
        consulta(conn, "SELECT id || ':' || orden FROM metricas ORDER BY orden") { it.getString(1) }

    // ---------- pruebas ----------

    @Test fun esquemaMigradoEsIgualAlDeRoomV7() {
        cargarBaseV6()
        migrar()
        val limpia = DriverManager.getConnection("jdbc:sqlite::memory:")
        try {
            crear(limpia, 7)
            assertEquals(descripcion(limpia), descripcion(conn))
        } finally {
            limpia.close()
        }
    }

    // ---------- 7 -> 8 ----------

    @Test fun esquemaMigradoDe7a8EsIgualAlDeRoomV8() {
        crear(conn, 7)
        exec("INSERT INTO metricas VALUES ('calorias','Calorías','kcal',2000.0,'#EF5350',0,'Anillo')")
        migrar7a8()
        val limpia = DriverManager.getConnection("jdbc:sqlite::memory:")
        try {
            crear(limpia, 8)
            assertEquals(descripcion(limpia), descripcion(conn))
        } finally {
            limpia.close()
        }
    }

    @Test fun cadenaCompletaDe6a8EsIgualAlDeRoomV8() {
        cargarBaseV6()
        migrar()
        migrar7a8()
        val limpia = DriverManager.getConnection("jdbc:sqlite::memory:")
        try {
            crear(limpia, 8)
            assertEquals(descripcion(limpia), descripcion(conn))
        } finally {
            limpia.close()
        }
    }

    @Test fun migracion7a8ConservaDatosYDaValoresPorDefecto() {
        cargarBaseV6()
        migrar()
        exec("INSERT INTO alimentos_guardados (nombre, valores, timestamp) VALUES ('Pollo','calorias=165.0',5)")
        migrar7a8()
        assertEquals(2, consulta(conn, "SELECT COUNT(*) FROM registros") { it.getInt(1) }.single())
        assertEquals(0, consulta(conn, "SELECT favorito FROM alimentos_guardados") { it.getInt(1) }.single())
        assertEquals(listOf<Double?>(null, null, null, null, null, null, null, null),
            consulta(conn, "SELECT limiteDescanso FROM metricas") { rs -> rs.getDouble(1).takeIf { !rs.wasNull() } })
    }

    @Test fun elGymSeBorraEnCascadaConSuRutina() {
        crear(conn, 8)
        exec("INSERT INTO rutinas (nombre, colorHex, dias, diaria, orden) VALUES ('A','#DC2626','1,4',0,0)")
        exec("INSERT INTO ejercicios_rutina (rutinaId, nombre, grupo, series, reps, esTiempo, descansoSeg, vecesPorDia, pesoKg, orden) " +
            "VALUES (1,'Press','Pecho',3,10,0,90,1,17.5,0)")
        exec("INSERT INTO series_hechas VALUES (1,'2026-10-22',0)")
        exec("INSERT INTO pesos_ejercicio (ejercicioId, timestamp, kg) VALUES (1, 1, 17.5)")
        exec("DELETE FROM rutinas WHERE id = 1")
        assertEquals(0, consulta(conn, "SELECT COUNT(*) FROM ejercicios_rutina") { it.getInt(1) }.single())
        assertEquals(0, consulta(conn, "SELECT COUNT(*) FROM series_hechas") { it.getInt(1) }.single())
        assertEquals(0, consulta(conn, "SELECT COUNT(*) FROM pesos_ejercicio") { it.getInt(1) }.single())
    }

    @Test fun conservaTodosLosDatosExistentes() {
        cargarBaseV6()
        migrar()
        assertEquals(2, consulta(conn, "SELECT COUNT(*) FROM registros") { it.getInt(1) }.single())
        assertEquals(450.5, consulta(conn, "SELECT valor FROM registros WHERE id=1") { it.getDouble(1) }.single(), 0.0)
        assertEquals("Avena", consulta(conn, "SELECT detalle FROM registros WHERE id=1") { it.getString(1) }.single())
        assertEquals("mi nota", consulta(conn, "SELECT texto FROM notas") { it.getString(1) }.single())
        assertEquals("52", consulta(conn, "SELECT valor FROM celdas") { it.getString(1) }.single())
        assertEquals(1, consulta(conn, "SELECT completado FROM estados_dia") { it.getInt(1) }.single())
    }

    @Test fun registrosAntiguosQuedanSinComida() {
        cargarBaseV6()
        migrar()
        val comidas = consulta(conn, "SELECT comida FROM registros") { it.getString(1) }
        assertEquals(listOf(null, null), comidas)
    }

    @Test fun agregaGrasasYFibraAlFinal() {
        cargarBaseV6()
        migrar()
        assertEquals(
            listOf("calorias:0", "proteinas:1", "carbohidratos:2", "azucar:3", "sodio:4",
                "agua:5", "grasas:6", "fibra:7"),
            nombresMetricas()
        )
        val fila = consulta(conn, "SELECT unidad, limiteMaximo, tipoGrafica FROM metricas WHERE id='grasas'") {
            Triple(it.getString(1), it.getDouble(2), it.getString(3))
        }.single()
        assertEquals(Triple("gr", 70.0, "Anillo"), fila)
    }

    @Test fun noDuplicaSiElUsuarioYaTeniaUnaMetricaGrasas() {
        cargarBaseV6()
        exec("INSERT INTO metricas VALUES ('grasas_1699999999999','Grasas','gr',60.0,'#000000',6,'Anillo')")
        migrar()
        val grasas = consulta(conn, "SELECT id FROM metricas WHERE lower(nombre) IN ('grasas','grasa')") { it.getString(1) }
        assertEquals(listOf("grasas_1699999999999"), grasas)
        // Fibra si faltaba y se agrega al final.
        assertTrue(nombresMetricas().last().startsWith("fibra:"))
    }

    @Test fun noAgregaNadaSiElUsuarioBorroTodasLasMetricas() {
        cargarBaseV6(conMetricas = false)
        migrar()
        assertEquals(0, consulta(conn, "SELECT COUNT(*) FROM metricas") { it.getInt(1) }.single())
    }

    @Test fun enBaseNuevaV7SePuedeGuardarComidaYPeso() {
        crear(conn, 7)
        exec("INSERT INTO metricas VALUES ('calorias','Calorías','kcal',2000.0,'#EF5350',0,'Anillo')")
        exec("INSERT INTO registros (metricaId, valor, timestamp, tipo, detalle, comida) " +
            "VALUES ('calorias', 300.0, 1, 'alimento', 'Huevo', 'Desayuno')")
        exec("INSERT INTO peso (timestamp, kg) VALUES (1, 78.4)")
        assertEquals("Desayuno", consulta(conn, "SELECT comida FROM registros") { it.getString(1) }.single())
        assertEquals(78.4, consulta(conn, "SELECT kg FROM peso") { it.getDouble(1) }.single(), 1e-9)
    }

    @Test fun elEsquemaV6ExportadoExiste() {
        assertTrue(File(carpeta, "6.json").exists())
        assertTrue(File(carpeta, "7.json").exists())
    }
}
