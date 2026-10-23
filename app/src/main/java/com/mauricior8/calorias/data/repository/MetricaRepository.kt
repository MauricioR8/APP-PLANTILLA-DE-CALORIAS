package com.mauricior8.calorias.data.repository

import androidx.room.withTransaction
import com.mauricior8.calorias.data.backup.DatosRespaldo
import com.mauricior8.calorias.data.local.AppDatabase
import com.mauricior8.calorias.data.local.dao.AjusteDao
import com.mauricior8.calorias.data.local.dao.AlimentoDao
import com.mauricior8.calorias.data.local.dao.CalculoDao
import com.mauricior8.calorias.data.local.dao.EstadoDiaDao
import com.mauricior8.calorias.data.local.dao.GymDao
import com.mauricior8.calorias.data.local.dao.MedidaDao
import com.mauricior8.calorias.data.local.dao.MetricaDao
import com.mauricior8.calorias.data.local.dao.NotaDao
import com.mauricior8.calorias.data.local.dao.PesoDao
import com.mauricior8.calorias.data.local.dao.RespaldoDao
import com.mauricior8.calorias.data.local.dao.SuplementoDao
import com.mauricior8.calorias.data.local.dao.TablaDao
import com.mauricior8.calorias.data.local.dao.TotalDiarioDao
import com.mauricior8.calorias.data.local.entity.Ajuste
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.Medida
import com.mauricior8.calorias.data.local.entity.PesoEjercicio
import com.mauricior8.calorias.data.local.entity.Rutina
import com.mauricior8.calorias.data.local.entity.SerieHecha
import com.mauricior8.calorias.data.local.entity.SesionGym
import com.mauricior8.calorias.data.local.entity.Suplemento
import com.mauricior8.calorias.data.local.entity.SuplementoTomado
import com.mauricior8.calorias.data.local.entity.TotalDiario
import com.mauricior8.calorias.data.seed.RutinaInicial
import com.mauricior8.calorias.util.Fechas
import kotlinx.coroutines.flow.map
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.local.model.MetricaTotal
import com.mauricior8.calorias.data.local.model.TotalComida
import kotlinx.coroutines.flow.Flow

/**
 * Capa intermedia entre los DAO y la capa de presentacion (ViewModel).
 */
class MetricaRepository(
    private val db: AppDatabase,
    private val metricaDao: MetricaDao,
    private val notaDao: NotaDao,
    private val calculoDao: CalculoDao,
    private val tablaDao: TablaDao,
    private val estadoDiaDao: EstadoDiaDao,
    private val alimentoDao: AlimentoDao,
    private val pesoDao: PesoDao,
    private val respaldoDao: RespaldoDao,
    private val gymDao: GymDao,
    private val ajusteDao: AjusteDao,
    private val suplementoDao: SuplementoDao,
    private val medidaDao: MedidaDao,
    private val totalDiarioDao: TotalDiarioDao
) {

    // ---- Metricas ----
    val metricas: Flow<List<MetricaConfig>> = metricaDao.observarMetricas()

    /** Totales del dia desglosados por comida. */
    fun totalesPorComida(inicio: Long, fin: Long): Flow<List<TotalComida>> =
        metricaDao.observarTotalesPorComida(inicio, fin)

    /** Registros de todas las metricas en [inicio, fin). */
    fun registrosEnRango(inicio: Long, fin: Long): Flow<List<RegistroSuma>> =
        metricaDao.observarRegistrosEnRango(inicio, fin)

    /** Totales agregados por metrica para un rango de dia arbitrario. */
    fun totalesDeDia(inicio: Long, fin: Long): Flow<List<MetricaTotal>> =
        metricaDao.observarTotalesDelDia(inicio, fin)

    fun historial(metricaId: String): Flow<List<RegistroSuma>> =
        metricaDao.observarHistorial(metricaId)

    suspend fun guardarMetrica(metrica: MetricaConfig) = metricaDao.upsertMetrica(metrica)

    suspend fun eliminarMetrica(metrica: MetricaConfig) = db.withTransaction {
        metricaDao.eliminarMetrica(metrica)
        totalDiarioDao.eliminarDeMetrica(metrica.id)
    }

    suspend fun actualizarOrden(id: String, nuevoOrden: Int) =
        metricaDao.actualizarOrden(id, nuevoOrden)

    suspend fun intercambiarOrden(idA: String, ordenA: Int, idB: String, ordenB: Int) =
        metricaDao.intercambiarOrden(idA, ordenA, idB, ordenB)

    suspend fun eliminarRegistro(id: Int) = metricaDao.eliminarRegistro(id)

    suspend fun siguienteOrden(): Int = metricaDao.maxOrden() + 1

    suspend fun agregarRegistro(registro: RegistroSuma): Long =
        metricaDao.insertRegistro(registro)

    /** Borra todos los registros de un dia (rango [inicio, fin)). */
    suspend fun limpiarDia(inicio: Long, fin: Long) =
        metricaDao.eliminarRegistrosEnRango(inicio, fin)

    /** Borra los registros de UNA metrica en un dia (rango [inicio, fin)). */
    suspend fun limpiarMetricaDia(metricaId: String, inicio: Long, fin: Long) =
        metricaDao.eliminarRegistrosDeMetricaEnRango(metricaId, inicio, fin)

    suspend fun hayMetricas(): Boolean = metricaDao.contarMetricas() > 0

    // ---- Estado del dia ----
    fun observarEstadoDia(fecha: String): Flow<EstadoDia?> = estadoDiaDao.observar(fecha)
    val diasCompletados: Flow<List<String>> = estadoDiaDao.observarCompletados()
    suspend fun guardarEstadoDia(estado: EstadoDia) = estadoDiaDao.upsert(estado)

    // ---- Historial de alimentos ----
    val alimentosGuardados: Flow<List<AlimentoGuardado>> = alimentoDao.observar()
    suspend fun guardarAlimento(alimento: AlimentoGuardado): Long = alimentoDao.insertar(alimento)
    suspend fun actualizarAlimento(alimento: AlimentoGuardado) = alimentoDao.actualizar(alimento)
    suspend fun eliminarAlimento(alimento: AlimentoGuardado) = alimentoDao.eliminar(alimento)

    // ---- Peso corporal ----
    val pesos: Flow<List<PesoRegistro>> = pesoDao.observar()
    suspend fun agregarPeso(kg: Float, timestamp: Long = System.currentTimeMillis()) {
        pesoDao.insertar(PesoRegistro(timestamp = timestamp, kg = kg))
    }
    suspend fun eliminarPeso(id: Int) = pesoDao.eliminar(id)

    // ---- Ajustes ----
    val ajustes: Flow<Map<String, String>> = ajusteDao.observar().map { l -> l.associate { it.clave to it.valor } }
    suspend fun ajuste(clave: String): String? = ajusteDao.valor(clave)
    suspend fun setAjuste(clave: String, valor: String) = ajusteDao.guardar(Ajuste(clave, valor))

    // ---- Totales archivados (modo historial semanal) ----
    fun totalesArchivadosDia(fecha: String): Flow<List<TotalDiario>> = totalDiarioDao.observarDia(fecha)
    fun totalesArchivadosRango(desde: String, hasta: String): Flow<List<TotalDiario>> =
        totalDiarioDao.observarRango(desde, hasta)

    /**
     * Archiva el detalle anterior a [limite]: suma cada dia y metrica en totales_diarios y borra
     * los registros. Todo en una transaccion, asi nunca se pierde un total.
     * @return cuantos registros se archivaron.
     */
    suspend fun archivarAntesDe(limite: Long): Int = db.withTransaction {
        val viejos = metricaDao.registrosAntesDe(limite)
        if (viejos.isEmpty()) return@withTransaction 0
        viejos.groupBy { Fechas.clave(it.timestamp) to it.metricaId }.forEach { (clave, lista) ->
            val (fecha, metricaId) = clave
            val previo = totalDiarioDao.obtener(fecha, metricaId)?.total ?: 0f
            totalDiarioDao.guardar(TotalDiario(fecha, metricaId, previo + lista.sumOf { it.valor.toDouble() }.toFloat()))
        }
        metricaDao.eliminarRegistrosAntesDe(limite)
        viejos.size
    }

    /** Copia los registros de [origenInicio, origenFin) al dia que empieza en [destinoInicio]. */
    suspend fun copiarDia(origenInicio: Long, origenFin: Long, destinoInicio: Long): Int = db.withTransaction {
        val origen = metricaDao.registrosEnRangoUnaVez(origenInicio, origenFin)
        if (origen.isEmpty()) return@withTransaction 0
        val delta = destinoInicio - origenInicio
        metricaDao.insertarRegistros(origen.map { it.copy(id = 0, timestamp = it.timestamp + delta) })
        origen.size
    }

    suspend fun registrosEnRangoUnaVez(inicio: Long, fin: Long): List<RegistroSuma> =
        metricaDao.registrosEnRangoUnaVez(inicio, fin)

    // ---- Alimentos: uso y favoritos ----
    suspend fun tocarAlimento(id: Int) = alimentoDao.tocar(id, System.currentTimeMillis())
    suspend fun marcarFavorito(id: Int, favorito: Boolean) = alimentoDao.marcarFavorito(id, favorito)

    // ---- Suplementos ----
    val suplementos: Flow<List<Suplemento>> = suplementoDao.observar()
    fun suplementosTomados(fecha: String): Flow<List<SuplementoTomado>> = suplementoDao.observarTomados(fecha)
    suspend fun agregarSuplemento(nombre: String) {
        suplementoDao.insertar(Suplemento(nombre = nombre, orden = suplementoDao.maxOrden() + 1))
    }
    suspend fun eliminarSuplemento(id: Int) = suplementoDao.eliminar(id)
    suspend fun marcarSuplemento(id: Int, fecha: String, tomado: Boolean) {
        if (tomado) suplementoDao.marcarTomado(SuplementoTomado(id, fecha)) else suplementoDao.desmarcarTomado(id, fecha)
    }

    // ---- Medidas corporales ----
    val medidas: Flow<List<Medida>> = medidaDao.observar()
    suspend fun agregarMedida(tipo: String, cm: Float) = medidaDao.insertar(Medida(tipo = tipo, cm = cm))
    suspend fun eliminarMedida(id: Int) = medidaDao.eliminar(id)

    // ---- Gym ----
    val rutinas: Flow<List<Rutina>> = gymDao.observarRutinas()
    val ejercicios: Flow<List<EjercicioRutina>> = gymDao.observarEjercicios()
    val pesosEjercicio: Flow<List<PesoEjercicio>> = gymDao.observarPesos()
    fun seriesDelDia(fecha: String): Flow<List<SerieHecha>> = gymDao.observarSeries(fecha)
    fun seriesEnRango(desde: String, hasta: String): Flow<List<SerieHecha>> = gymDao.observarSeriesEnRango(desde, hasta)
    fun sesion(fecha: String): Flow<SesionGym?> = gymDao.observarSesion(fecha)

    suspend fun crearRutina(rutina: Rutina): Long =
        gymDao.insertarRutina(rutina.copy(id = 0, orden = gymDao.maxOrdenRutina() + 1))
    suspend fun actualizarRutina(rutina: Rutina) = gymDao.actualizarRutina(rutina)
    suspend fun eliminarRutina(rutina: Rutina) = gymDao.eliminarRutina(rutina)

    suspend fun guardarEjercicio(ejercicio: EjercicioRutina) {
        if (ejercicio.id == 0) {
            val id = gymDao.insertarEjercicio(
                ejercicio.copy(orden = gymDao.maxOrdenEjercicio(ejercicio.rutinaId) + 1)
            )
            // El peso inicial cuenta como primer punto del historial.
            ejercicio.pesoKg?.let { gymDao.insertarPeso(PesoEjercicio(ejercicioId = id.toInt(), kg = it)) }
        } else {
            gymDao.actualizarEjercicio(ejercicio)
        }
    }
    suspend fun eliminarEjercicio(ejercicio: EjercicioRutina) = gymDao.eliminarEjercicio(ejercicio)

    /** Cambia el peso de trabajo y deja constancia en el historial (para la grafica mensual). */
    suspend fun subirPeso(ejercicioId: Int, kg: Float) = db.withTransaction {
        gymDao.actualizarPeso(ejercicioId, kg)
        gymDao.insertarPeso(PesoEjercicio(ejercicioId = ejercicioId, kg = kg))
    }

    suspend fun moverEjercicio(a: EjercicioRutina, b: EjercicioRutina) =
        gymDao.intercambiarOrdenEjercicios(a.id, a.orden, b.id, b.orden)

    suspend fun marcarSerie(ejercicioId: Int, fecha: String, indice: Int, hecha: Boolean) {
        if (hecha) gymDao.marcarSerie(SerieHecha(ejercicioId, fecha, indice))
        else gymDao.desmarcarSerie(ejercicioId, fecha, indice)
    }
    suspend fun limpiarSeriesDelDia(fecha: String) = gymDao.limpiarSeriesDelDia(fecha)
    suspend fun guardarSesion(fecha: String, nota: String) {
        if (nota.isBlank()) gymDao.borrarSesion(fecha) else gymDao.guardarSesion(SesionGym(fecha, nota.trim()))
    }

    /**
     * Carga la rutina inicial (de la nota del usuario) una sola vez. Una marca en ajustes evita
     * volver a crearla si el usuario la borra o la edita despues.
     */
    suspend fun sembrarRutinaInicial() = db.withTransaction {
        if (ajusteDao.valor(AJUSTE_RUTINA_SEMBRADA) != null) return@withTransaction
        if (gymDao.contarRutinas() == 0) {
            RutinaInicial.bloques.forEach { b ->
                val rutinaId = gymDao.insertarRutina(b.rutina).toInt()
                b.ejercicios.forEachIndexed { i, e ->
                    val id = gymDao.insertarEjercicio(e.copy(rutinaId = rutinaId, orden = i)).toInt()
                    e.pesoKg?.let { gymDao.insertarPeso(PesoEjercicio(ejercicioId = id, kg = it)) }
                }
            }
        }
        if (suplementoDao.maxOrden() < 0) {
            listOf("Creatina", "Proteína en polvo").forEachIndexed { i, n ->
                suplementoDao.insertar(Suplemento(nombre = n, orden = i))
            }
        }
        ajusteDao.guardar(Ajuste(AJUSTE_RUTINA_SEMBRADA, "1"))
    }

    // ---- Respaldo ----
    suspend fun leerTodo(): DatosRespaldo = db.withTransaction {
        DatosRespaldo(
            ajustes = respaldoDao.ajustes(),
            rutinas = respaldoDao.rutinas(),
            ejercicios = respaldoDao.ejercicios(),
            seriesHechas = respaldoDao.seriesHechas(),
            pesosEjercicio = respaldoDao.pesosEjercicio(),
            sesiones = respaldoDao.sesiones(),
            suplementos = respaldoDao.suplementos(),
            suplementosTomados = respaldoDao.suplementosTomados(),
            medidas = respaldoDao.medidas(),
            totalesDiarios = respaldoDao.totalesDiarios(),
            metricas = respaldoDao.metricas(),
            registros = respaldoDao.registros(),
            notas = respaldoDao.notas(),
            calculos = respaldoDao.calculos(),
            tablas = respaldoDao.tablas(),
            columnas = respaldoDao.columnas(),
            filas = respaldoDao.filas(),
            celdas = respaldoDao.celdas(),
            estadosDia = respaldoDao.estadosDia(),
            alimentos = respaldoDao.alimentos(),
            pesos = respaldoDao.pesos()
        )
    }

    /**
     * Reemplaza TODO el contenido por el del respaldo, en una sola transaccion: si algo
     * falla (por ejemplo un registro de una metrica inexistente) se revierte y los datos
     * actuales quedan intactos.
     */
    suspend fun reemplazarTodo(d: DatosRespaldo) = db.withTransaction {
        // Borrar padres: las tablas hijas (registros, columnas, filas, celdas) caen en cascada.
        respaldoDao.borrarMetricas()
        respaldoDao.borrarNotas()
        respaldoDao.borrarCalculos()
        respaldoDao.borrarTablas()
        respaldoDao.borrarEstadosDia()
        respaldoDao.borrarAlimentos()
        respaldoDao.borrarPesos()
        respaldoDao.borrarAjustes()
        respaldoDao.borrarRutinas()
        respaldoDao.borrarSesiones()
        respaldoDao.borrarSuplementos()
        respaldoDao.borrarMedidas()
        respaldoDao.borrarTotalesDiarios()

        respaldoDao.insertarAjustes(d.ajustes)
        respaldoDao.insertarRutinas(d.rutinas)
        respaldoDao.insertarEjercicios(d.ejercicios)
        respaldoDao.insertarSeriesHechas(d.seriesHechas)
        respaldoDao.insertarPesosEjercicio(d.pesosEjercicio)
        respaldoDao.insertarSesiones(d.sesiones)
        respaldoDao.insertarSuplementos(d.suplementos)
        respaldoDao.insertarSuplementosTomados(d.suplementosTomados)
        respaldoDao.insertarMedidas(d.medidas)
        respaldoDao.insertarTotalesDiarios(d.totalesDiarios)
        respaldoDao.insertarMetricas(d.metricas)
        respaldoDao.insertarRegistros(d.registros)
        respaldoDao.insertarNotas(d.notas)
        respaldoDao.insertarCalculos(d.calculos)
        respaldoDao.insertarTablas(d.tablas)
        respaldoDao.insertarColumnas(d.columnas)
        respaldoDao.insertarFilas(d.filas)
        respaldoDao.insertarCeldas(d.celdas)
        respaldoDao.insertarEstadosDia(d.estadosDia)
        respaldoDao.insertarAlimentos(d.alimentos)
        respaldoDao.insertarPesos(d.pesos)
    }

    // ---- Notas ----
    val notas: Flow<List<Nota>> = notaDao.observarNotas()
    suspend fun guardarNota(nota: Nota) = notaDao.upsert(nota)
    suspend fun eliminarNota(nota: Nota) = notaDao.eliminar(nota)

    // ---- Calculadora ----
    val historialCalculos: Flow<List<CalculoHistorial>> = calculoDao.observarHistorial()
    suspend fun guardarCalculo(calculo: CalculoHistorial) = calculoDao.insertar(calculo)
    suspend fun limpiarCalculos() = calculoDao.limpiar()

    // ---- Tablas de alimentos ----
    val tablas: Flow<List<TablaAlimentos>> = tablaDao.observarTablas()

    fun columnas(tablaId: Int): Flow<List<ColumnaTabla>> = tablaDao.observarColumnas(tablaId)
    fun filas(tablaId: Int): Flow<List<FilaTabla>> = tablaDao.observarFilas(tablaId)
    fun celdas(tablaId: Int): Flow<List<CeldaTabla>> = tablaDao.observarCeldas(tablaId)

    suspend fun crearTabla(nombre: String): Long =
        tablaDao.insertTabla(TablaAlimentos(nombre = nombre))

    suspend fun eliminarTabla(tabla: TablaAlimentos) = tablaDao.eliminarTabla(tabla)

    suspend fun actualizarTabla(tabla: TablaAlimentos) = tablaDao.actualizarTabla(tabla)

    /** Devuelve false si se alcanzo el limite de 10 columnas. */
    suspend fun agregarColumna(tablaId: Int, nombre: String): Boolean {
        if (tablaDao.contarColumnas(tablaId) >= MAX_COLUMNAS) return false
        val orden = tablaDao.maxOrdenColumna(tablaId) + 1
        tablaDao.insertColumna(ColumnaTabla(tablaId = tablaId, nombre = nombre, orden = orden))
        return true
    }

    /** Devuelve false si se alcanzo el limite de 100 filas. */
    suspend fun agregarFila(tablaId: Int, nombre: String): Boolean {
        if (tablaDao.contarFilas(tablaId) >= MAX_FILAS) return false
        val orden = tablaDao.maxOrdenFila(tablaId) + 1
        tablaDao.insertFila(FilaTabla(tablaId = tablaId, nombre = nombre, orden = orden))
        return true
    }

    suspend fun eliminarColumna(columna: ColumnaTabla) = tablaDao.eliminarColumna(columna)
    suspend fun eliminarFila(fila: FilaTabla) = tablaDao.eliminarFila(fila)

    suspend fun setCelda(tablaId: Int, filaId: Int, columnaId: Int, valor: String) {
        val existente = tablaDao.obtenerCelda(filaId, columnaId)
        tablaDao.upsertCelda(
            CeldaTabla(
                id = existente?.id ?: 0,
                tablaId = tablaId,
                filaId = filaId,
                columnaId = columnaId,
                valor = valor
            )
        )
    }

    companion object {
        const val MAX_COLUMNAS = 10
        const val MAX_FILAS = 100
        const val AJUSTE_RUTINA_SEMBRADA = "rutina_sembrada"
    }
}
