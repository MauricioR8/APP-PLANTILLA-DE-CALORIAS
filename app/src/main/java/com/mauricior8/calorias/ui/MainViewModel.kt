package com.mauricior8.calorias.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.Medida
import com.mauricior8.calorias.data.local.entity.PesoEjercicio
import com.mauricior8.calorias.data.local.entity.Rutina
import com.mauricior8.calorias.data.local.entity.SerieHecha
import com.mauricior8.calorias.data.local.entity.SesionGym
import com.mauricior8.calorias.data.local.entity.Suplemento
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.ui.state.GastoReal
import com.mauricior8.calorias.ui.state.RutinaDelDia
import com.mauricior8.calorias.ui.state.SemanaGym
import com.mauricior8.calorias.util.Actividad
import com.mauricior8.calorias.util.Csv
import com.mauricior8.calorias.util.Cuerpo
import com.mauricior8.calorias.util.Fechas
import com.mauricior8.calorias.util.Gym
import com.mauricior8.calorias.util.MetaPeso
import com.mauricior8.calorias.util.Perfil
import com.mauricior8.calorias.util.PerfilBase
import com.mauricior8.calorias.util.Sexo
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.backup.Respaldo
import com.mauricior8.calorias.data.local.model.TotalComida
import com.mauricior8.calorias.data.remote.AlimentoRemoto
import com.mauricior8.calorias.data.remote.OpenFoodFacts
import com.mauricior8.calorias.data.repository.MetricaRepository
import com.mauricior8.calorias.util.Calculadora
import com.mauricior8.calorias.util.Clave
import com.mauricior8.calorias.util.ObjetivosDiarios
import com.mauricior8.calorias.util.claveMetrica
import com.mauricior8.calorias.util.codificarValores
import com.mauricior8.calorias.util.decodificarValores
import com.mauricior8.calorias.ui.state.MetricaConItem
import com.mauricior8.calorias.ui.state.Progreso
import com.mauricior8.calorias.ui.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: MetricaRepository
) : ViewModel() {

    init {
        sembrarMetricasPorDefecto()
    }

    // Formateadores de fecha (declarados arriba para estar listos al construir).
    private val fmtFechaKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val fmtEtiqueta = SimpleDateFormat("dd/MM", Locale.getDefault())

    // ---------------- Mensajes para la UI (Snackbar) ----------------

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Avisos puntuales para mostrar al usuario. */
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    fun avisar(texto: String) {
        _mensajes.tryEmit(texto)
    }

    // ---------------- Seleccion de dia ----------------

    /** Inicio (00:00) del dia seleccionado, en milisegundos epoch. */
    private val _inicioDiaSeleccionado = MutableStateFlow(inicioDeHoy())

    /** True mientras el usuario no haya elegido manualmente otro dia distinto de hoy. */
    private var siguiendoHoy = true

    /** Etiquetas de Lunes a Domingo. */
    val diasSemana: List<String> = listOf(
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    )

    /** Indice (0=Lunes ... 6=Domingo) del dia seleccionado. */
    val indiceDiaSeleccionado: StateFlow<Int> =
        _inicioDiaSeleccionado.map { indiceSemana(it) }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), indiceSemana(inicioDeHoy())
        )

    /** Etiqueta legible del dia, ej. "Lunes 09/06". */
    val etiquetaDiaSeleccionado: StateFlow<String> =
        _inicioDiaSeleccionado.map { etiquetaDia(it) }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), etiquetaDia(inicioDeHoy())
        )

    /** Si el dia seleccionado esta marcado como completado. */
    val diaCompletado: StateFlow<Boolean> =
        _inicioDiaSeleccionado.flatMapLatest { inicio ->
            repository.observarEstadoDia(fechaKey(inicio)).map { it?.completado ?: false }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Inicio del dia seleccionado en millis (para el selector de fecha manual). */
    val fechaSeleccionadaMillis: StateFlow<Long> = _inicioDiaSeleccionado.asStateFlow()

    // ---------------- Estado de Metricas ----------------

    val uiState: StateFlow<UiState> =
        _inicioDiaSeleccionado.flatMapLatest { inicio ->
            val fin = finDeDia(inicio)
            val dia = diaIsoDe(inicio)
            combine(
                repository.metricas,
                repository.totalesDeDia(inicio, fin),
                repository.totalesArchivadosDia(fechaKey(inicio)),
                repository.rutinas
            ) { metricas, totales, archivados, rutinas ->
                // Un dia ya archivado no tiene detalle, pero conserva su total en totales_diarios.
                val sumas = HashMap<String, Float>()
                totales.forEach { sumas[it.metricaId] = (sumas[it.metricaId] ?: 0f) + it.total }
                archivados.forEach { sumas[it.metricaId] = (sumas[it.metricaId] ?: 0f) + it.total }
                // Dia de descanso: hay rutinas de fuerza definidas pero ninguna toca hoy.
                val fuerza = rutinas.filter { !it.diaria }
                val descanso = fuerza.isNotEmpty() && fuerza.none { Gym.aplicaEn(it, dia) }
                val items = metricas.map { config ->
                    MetricaConItem(
                        config = config,
                        totalHoy = sumas[config.id] ?: 0f,
                        diaDescanso = descanso
                    )
                }
                UiState(isLoading = false, metricas = items, diaDescanso = descanso)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(isLoading = true)
        )

    fun historial(metricaId: String) = repository.historial(metricaId)

    fun seleccionarDia(indice: Int) {
        val inicio = inicioDeDiaSemana(indice)
        siguiendoHoy = inicio == inicioDeHoy()
        _inicioDiaSeleccionado.value = inicio
    }

    /** Fija manualmente la fecha (dia/mes/anio) a partir de millis. */
    fun setFechaManual(millis: Long) {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val inicio = aMedianoche(c)
        siguiendoHoy = inicio == inicioDeHoy()
        _inicioDiaSeleccionado.value = inicio
    }

    /**
     * Se llama al volver a la app. Si el usuario seguia viendo "hoy" y ya cambio el dia
     * (paso la medianoche con la app abierta), avanza al dia actual.
     */
    fun alReanudar() {
        _hoy.value = inicioDeHoy()
        if (siguiendoHoy) _inicioDiaSeleccionado.value = inicioDeHoy()
        viewModelScope.launch { archivarSiCorresponde() }
    }

    // ---------------- Comidas del dia seleccionado ----------------

    /** Totales por comida (y metrica) del dia seleccionado. */
    val totalesPorComida: StateFlow<List<TotalComida>> =
        _inicioDiaSeleccionado.flatMapLatest { inicio ->
            repository.totalesPorComida(inicio, finDeDia(inicio))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------------- Progreso semanal / mensual ----------------

    /** Hoy (00:00); se refresca al volver a la app para que la ventana avance de dia. */
    private val _hoy = MutableStateFlow(inicioDeHoy())

    private val _rangoDias = MutableStateFlow(7)
    val rangoDias: StateFlow<Int> = _rangoDias.asStateFlow()

    fun setRangoProgreso(dias: Int) {
        _rangoDias.value = dias
    }

    /** Totales diarios de los ultimos [dias] dias: detalle de registros + totales ya archivados. */
    private fun construirProgreso(dias: Int, hoy: Long): Flow<Progreso> {
        val inicios = (dias - 1 downTo 0).map { atras -> inicioRelativo(hoy, -atras) }
        val claves = inicios.map { fechaKey(it) }
        return combine(
            repository.registrosEnRango(inicios.first(), finDeDia(hoy)),
            repository.totalesArchivadosRango(claves.first(), claves.last())
        ) { registros, archivados ->
            val acumulados = HashMap<String, FloatArray>()
            registros.forEach { r ->
                val i = inicios.indexOfLast { it <= r.timestamp }
                if (i >= 0) acumulados.getOrPut(r.metricaId) { FloatArray(dias) }[i] += r.valor
            }
            archivados.forEach { t ->
                val i = claves.indexOf(t.fecha)
                if (i >= 0) acumulados.getOrPut(t.metricaId) { FloatArray(dias) }[i] += t.total
            }
            Progreso(dias, inicios.map { fmtEtiqueta.format(Date(it)) }, acumulados.mapValues { it.value.toList() })
        }
    }

    val progreso: StateFlow<Progreso> =
        combine(_rangoDias, _hoy) { dias, hoy -> dias to hoy }
            .flatMapLatest { (dias, hoy) -> construirProgreso(dias, hoy) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Progreso())

    /** Ventana fija de 28 dias para el gasto energetico estimado. */
    private val progreso28: StateFlow<Progreso> =
        _hoy.flatMapLatest { hoy -> construirProgreso(28, hoy) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Progreso())

    // ---------------- Peso corporal ----------------

    val pesos: StateFlow<List<PesoRegistro>> =
        repository.pesos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun agregarPeso(kg: Float) {
        if (kg !in 20f..400f) {
            avisar("Escribe un peso entre 20 y 400 kg")
            return
        }
        viewModelScope.launch { repository.agregarPeso(kg) }
    }

    fun eliminarPeso(peso: PesoRegistro) {
        viewModelScope.launch { repository.eliminarPeso(peso.id) }
    }

    // ---------------- Respaldo ----------------

    /** Genera el JSON del respaldo y lo entrega a [escribir] (corre fuera del hilo principal). */
    fun exportar(escribir: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = Respaldo.aJson(repository.leerTodo())
                withContext(Dispatchers.IO) { escribir(json) }
                avisar("Respaldo guardado")
            } catch (e: Exception) {
                avisar("No se pudo guardar el respaldo: ${e.message}")
            }
        }
    }

    /** Reemplaza todos los datos por los del respaldo. Si falla, los datos actuales no cambian. */
    fun importar(texto: String) {
        viewModelScope.launch {
            try {
                val datos = Respaldo.deJson(texto)
                repository.reemplazarTodo(datos)
                // Un respaldo antiguo (formato 1) no trae rutinas: se vuelve a cargar la inicial.
                repository.sembrarRutinaInicial()
                avisar("Respaldo restaurado: ${datos.registros.size} registros, ${datos.notas.size} notas")
            } catch (e: IllegalArgumentException) {
                avisar(e.message ?: "El archivo no es un respaldo válido")
            } catch (e: Exception) {
                avisar("No se pudo restaurar; tus datos no cambiaron")
            }
        }
    }

    // ---------------- Objetivos y busqueda de alimentos ----------------

    /** Actualiza el limite de las metricas reconocidas (calorias, macros, fibra, agua). */
    fun aplicarObjetivos(o: ObjetivosDiarios) {
        viewModelScope.launch {
            val metas = mapOf(
                Clave.CALORIAS to o.kcal.toFloat(),
                Clave.PROTEINAS to o.proteinaG.toFloat(),
                Clave.CARBOHIDRATOS to o.carbohidratosG.toFloat(),
                Clave.GRASAS to o.grasasG.toFloat(),
                Clave.FIBRA to o.fibraG.toFloat(),
                Clave.AGUA to o.aguaL.toFloat()
            )
            var cambiadas = 0
            repository.metricas.first().forEach { config ->
                val nuevo = claveMetrica(config.nombre)?.let { metas[it] } ?: return@forEach
                repository.guardarMetrica(config.copy(limiteMaximo = nuevo))
                cambiadas++
            }
            avisar("Metas actualizadas en $cambiadas métricas")
        }
    }

    /** Busca en Open Food Facts; ante un fallo de red avisa y devuelve una lista vacia. */
    suspend fun buscarAlimentos(texto: String): List<AlimentoRemoto> =
        try {
            OpenFoodFacts.buscar(texto)
        } catch (e: IOException) {
            avisar("No se pudo buscar: revisa tu conexión a internet")
            emptyList()
        }

    fun limpiarDia() {
        val inicio = _inicioDiaSeleccionado.value
        viewModelScope.launch { repository.limpiarDia(inicio, finDeDia(inicio)) }
    }

    /** Limpia (borra) los registros de una metrica en el dia seleccionado. */
    fun limpiarMetrica(metricaId: String) {
        val inicio = _inicioDiaSeleccionado.value
        viewModelScope.launch {
            repository.limpiarMetricaDia(metricaId, inicio, finDeDia(inicio))
        }
    }

    fun marcarDiaCompletado(completado: Boolean) {
        val fecha = fechaKey(_inicioDiaSeleccionado.value)
        viewModelScope.launch { repository.guardarEstadoDia(EstadoDia(fecha, completado)) }
    }

    // ---------------- Estado de Notas ----------------

    val notas: StateFlow<List<Nota>> =
        repository.notas.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ---------------- Estado de Calculadora ----------------

    val historialCalculos: StateFlow<List<CalculoHistorial>> =
        repository.historialCalculos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ---------------- Estado de Tablas de alimentos ----------------

    val tablas: StateFlow<List<TablaAlimentos>> =
        repository.tablas.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun columnas(tablaId: Int): Flow<List<ColumnaTabla>> = repository.columnas(tablaId)
    fun filas(tablaId: Int): Flow<List<FilaTabla>> = repository.filas(tablaId)
    fun celdas(tablaId: Int): Flow<List<CeldaTabla>> = repository.celdas(tablaId)

    // ---------------- Estado de Alimentos guardados (historial) ----------------

    val alimentosGuardados: StateFlow<List<AlimentoGuardado>> =
        repository.alimentosGuardados.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ---------------- Acciones: Metricas ----------------

    fun crearMetrica(
        nombre: String,
        unidad: String,
        limiteMaximo: Float? = null,
        colorHex: String = "#FF6D00",
        tipoGrafica: String = "Anillo",
        limiteDescanso: Float? = null
    ) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return
        viewModelScope.launch {
            repository.guardarMetrica(
                MetricaConfig(
                    id = generarId(nombreLimpio),
                    nombre = nombreLimpio,
                    unidad = unidad,
                    limiteMaximo = limiteMaximo,
                    colorHex = colorHex,
                    orden = repository.siguienteOrden(),
                    tipoGrafica = tipoGrafica,
                    limiteDescanso = limiteDescanso
                )
            )
        }
    }

    /** Edita una metrica existente conservando su id y orden. */
    fun editarMetrica(metrica: MetricaConfig) {
        viewModelScope.launch { repository.guardarMetrica(metrica) }
    }

    fun eliminarMetrica(metrica: MetricaConfig) {
        viewModelScope.launch { repository.eliminarMetrica(metrica) }
    }

    /** Intercambia el orden con el vecino superior. */
    fun moverArriba(metricaId: String) = moverEnDireccion(metricaId, -1)

    /** Intercambia el orden con el vecino inferior. */
    fun moverAbajo(metricaId: String) = moverEnDireccion(metricaId, +1)

    private fun moverEnDireccion(metricaId: String, delta: Int) {
        val items = uiState.value.metricas
        val index = items.indexOfFirst { it.config.id == metricaId }
        if (index < 0) return
        val destino = index + delta
        if (destino < 0 || destino >= items.size) return

        val actual = items[index].config
        val vecino = items[destino].config
        viewModelScope.launch {
            // Si dos metricas comparten orden el intercambio no cambiaria nada.
            // En ese caso se separan usando la posicion en la lista.
            val ordenActual = if (actual.orden != vecino.orden) actual.orden else index
            val ordenVecino = if (actual.orden != vecino.orden) vecino.orden else destino
            repository.intercambiarOrden(actual.id, ordenActual, vecino.id, ordenVecino)
        }
    }

    /** Borra un unico registro del historial de una metrica. */
    fun eliminarRegistro(registro: RegistroSuma) {
        viewModelScope.launch { repository.eliminarRegistro(registro.id) }
    }

    fun agregarRegistro(
        metricaId: String,
        valor: Float,
        tipo: String = "manual",
        detalle: String? = null
    ) {
        if (valor <= 0f) return
        viewModelScope.launch {
            repository.agregarRegistro(
                RegistroSuma(
                    metricaId = metricaId,
                    valor = valor,
                    timestamp = timestampParaRegistro(),
                    tipo = tipo,
                    detalle = detalle
                )
            )
        }
    }

    /**
     * Agrega un alimento que aporta valores a varias metricas a la vez.
     * Inserta un registro (tipo "alimento") por cada metrica con valor > 0,
     * y ademas lo guarda en el historial reutilizable.
     *
     * @param valores mapa metricaId -> valor aportado.
     */
    fun agregarAlimento(nombre: String, valores: Map<String, Float>, comida: String? = null) {
        val nombreLimpio = nombre.trim().ifEmpty { "Alimento" }
        val aportes = valores.filterValues { it > 0f }
        if (aportes.isEmpty()) return
        viewModelScope.launch {
            val ts = timestampParaRegistro()
            aportes.forEach { (metricaId, valor) ->
                repository.agregarRegistro(
                    RegistroSuma(
                        metricaId = metricaId,
                        valor = valor,
                        timestamp = ts,
                        tipo = "alimento",
                        detalle = nombreLimpio,
                        comida = comida
                    )
                )
            }
            // Guardar tambien en el historial reutilizable.
            repository.guardarAlimento(
                AlimentoGuardado(nombre = nombreLimpio, valores = codificarValores(aportes))
            )
        }
    }

    /** Re-aplica un alimento guardado al dia seleccionado (sin duplicar en historial). */
    fun aplicarAlimentoGuardado(alimento: AlimentoGuardado, comida: String? = null, porcion: Float = 1f) {
        val aportes = decodificarValores(alimento.valores)
            .mapValues { it.value * porcion }.filterValues { it > 0f }
        if (aportes.isEmpty()) return
        viewModelScope.launch {
            repository.tocarAlimento(alimento.id)
            val ts = timestampParaRegistro()
            aportes.forEach { (metricaId, valor) ->
                repository.agregarRegistro(
                    RegistroSuma(
                        metricaId = metricaId,
                        valor = valor,
                        timestamp = ts,
                        tipo = "alimento",
                        detalle = alimento.nombre,
                        comida = comida
                    )
                )
            }
        }
    }

    /** Edita un alimento del historial (nombre y/o valores). */
    fun editarAlimentoGuardado(alimento: AlimentoGuardado, nombre: String, valores: Map<String, Float>) {
        viewModelScope.launch {
            repository.actualizarAlimento(
                alimento.copy(
                    nombre = nombre.trim().ifEmpty { alimento.nombre },
                    valores = codificarValores(valores.filterValues { it > 0f })
                )
            )
        }
    }

    fun eliminarAlimentoGuardado(alimento: AlimentoGuardado) {
        viewModelScope.launch { repository.eliminarAlimento(alimento) }
    }

    // ---------------- Acciones: Notas ----------------

    fun guardarNota(texto: String, id: Int = 0) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            repository.guardarNota(Nota(id = id, texto = limpio))
        }
    }

    fun eliminarNota(nota: Nota) {
        viewModelScope.launch { repository.eliminarNota(nota) }
    }

    // ---------------- Acciones: Calculadora ----------------

    /**
     * Evalua la expresion. Si es valida, persiste el calculo y devuelve el
     * resultado formateado; si no, devuelve null.
     */
    fun calcular(expresion: String): String? {
        val resultado = Calculadora.evaluar(expresion) ?: return null
        val texto = formato(resultado)
        viewModelScope.launch {
            repository.guardarCalculo(
                CalculoHistorial(expresion = expresion.trim(), resultado = texto)
            )
        }
        return texto
    }

    fun limpiarHistorialCalculos() {
        viewModelScope.launch { repository.limpiarCalculos() }
    }

    // ---------------- Acciones: Tablas de alimentos ----------------

    fun crearTabla(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch { repository.crearTabla(limpio) }
    }

    fun eliminarTabla(tabla: TablaAlimentos) {
        viewModelScope.launch { repository.eliminarTabla(tabla) }
    }

    fun cambiarTipoGrafica(tabla: TablaAlimentos, tipo: String) {
        viewModelScope.launch { repository.actualizarTabla(tabla.copy(tipoGrafica = tipo)) }
    }

    fun agregarColumna(tablaId: Int, nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            if (!repository.agregarColumna(tablaId, limpio)) {
                avisar("Máximo ${MetricaRepository.MAX_COLUMNAS} columnas por tabla")
            }
        }
    }

    fun agregarFila(tablaId: Int, nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            if (!repository.agregarFila(tablaId, limpio)) {
                avisar("Máximo ${MetricaRepository.MAX_FILAS} filas por tabla")
            }
        }
    }

    fun eliminarColumna(columna: ColumnaTabla) {
        viewModelScope.launch { repository.eliminarColumna(columna) }
    }

    fun eliminarFila(fila: FilaTabla) {
        viewModelScope.launch { repository.eliminarFila(fila) }
    }

    fun setCelda(tablaId: Int, filaId: Int, columnaId: Int, valor: String) {
        viewModelScope.launch { repository.setCelda(tablaId, filaId, columnaId, valor) }
    }

    // ---------------- Ajustes ----------------

    private val _ajustesListos = MutableStateFlow(false)

    /** True cuando ya se leyeron los ajustes (evita mostrar contenido antes de saber si hay bloqueo). */
    val ajustesListos: StateFlow<Boolean> = _ajustesListos.asStateFlow()

    val ajustes: StateFlow<Map<String, String>> =
        repository.ajustes
            .onEach { _ajustesListos.value = true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setAjuste(clave: String, valor: String) {
        viewModelScope.launch { repository.setAjuste(clave, valor) }
    }

    private fun empiezaDomingo(a: Map<String, String>) = a["inicio_semana"] != "lunes"

    /** Cambia la retencion del detalle ("siempre", "semanal" o "30dias") y archiva lo que ya sobra. */
    fun cambiarRetencion(modo: String) {
        viewModelScope.launch {
            repository.setAjuste("retencion", modo)
            archivarSiCorresponde()
        }
    }

    /**
     * Modo historial semanal: borra el DETALLE (nombres, horas) anterior al inicio de la semana
     * pero conserva el total de cada dia, asi Progreso y las metas siguen funcionando.
     */
    private suspend fun archivarSiCorresponde() {
        val ahora = System.currentTimeMillis()
        val limite = when (repository.ajuste("retencion")) {
            "semanal" -> Fechas.inicioDeSemana(ahora, repository.ajuste("inicio_semana") != "lunes")
            "30dias" -> Fechas.desplazarDias(ahora, -30)
            else -> return
        }
        val n = repository.archivarAntesDe(limite)
        if (n > 0) avisar("Historial archivado: $n registros. Los totales por día se conservan.")
    }

    /** Perfil guardado (sin peso: el peso sale del ultimo registro de peso corporal). */
    val perfilBase: StateFlow<PerfilBase?> = ajustes.map { a ->
        val sexo = Sexo.entries.firstOrNull { it.name == a["perfil_sexo"] }
        val edad = a["perfil_edad"]?.toIntOrNull()
        val estatura = a["perfil_estatura"]?.toDoubleOrNull()
        val actividad = Actividad.entries.firstOrNull { it.name == a["perfil_actividad"] }
        val meta = MetaPeso.entries.firstOrNull { it.name == a["perfil_meta"] }
        if (sexo == null || edad == null || estatura == null || actividad == null || meta == null) null
        else PerfilBase(sexo, edad, estatura, actividad, meta)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun guardarPerfil(p: Perfil) {
        viewModelScope.launch {
            repository.setAjuste("perfil_sexo", p.sexo.name)
            repository.setAjuste("perfil_edad", p.edad.toString())
            repository.setAjuste("perfil_estatura", p.estaturaCm.toString())
            repository.setAjuste("perfil_actividad", p.actividad.name)
            repository.setAjuste("perfil_meta", p.meta.name)
        }
    }

    // ---------------- Gym ----------------

    val rutinas: StateFlow<List<Rutina>> =
        repository.rutinas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ejercicios: StateFlow<List<EjercicioRutina>> =
        repository.ejercicios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pesosEjercicio: StateFlow<List<PesoEjercicio>> =
        repository.pesosEjercicio.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Dia ISO (1=lunes..7=domingo) del dia seleccionado. */
    val diaIsoSeleccionado: StateFlow<Int> =
        _inicioDiaSeleccionado.map { diaIsoDe(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), diaIsoDe(inicioDeHoy()))

    /** Rutinas que tocan el dia seleccionado: primero el bloque diario y luego la de fuerza. */
    val rutinasDelDia: StateFlow<List<RutinaDelDia>> =
        combine(rutinas, ejercicios, diaIsoSeleccionado) { rs, ejs, dia ->
            rs.filter { Gym.aplicaEn(it, dia) }
                .sortedWith(compareByDescending<Rutina> { it.diaria }.thenBy { it.orden })
                .map { r -> RutinaDelDia(r, ejs.filter { it.rutinaId == r.id }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Series marcadas como hechas el dia seleccionado. */
    val seriesDelDia: StateFlow<List<SerieHecha>> =
        _inicioDiaSeleccionado.flatMapLatest { repository.seriesDelDia(fechaKey(it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sesionDelDia: StateFlow<SesionGym?> =
        _inicioDiaSeleccionado.flatMapLatest { repository.sesion(fechaKey(it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Volumen por grupo muscular y cumplimiento de la semana en curso. */
    val semanaGym: StateFlow<SemanaGym> =
        combine(_hoy, ajustes) { hoy, a -> hoy to empiezaDomingo(a) }
            .flatMapLatest { (hoy, domingo) ->
                val ini = Fechas.inicioDeSemana(hoy, domingo)
                val dias = (0..6).map { Fechas.desplazarDias(ini, it) }
                combine(
                    repository.rutinas,
                    repository.ejercicios,
                    repository.seriesEnRango(fechaKey(dias.first()), fechaKey(dias.last()))
                ) { rs, ejs, series ->
                    val grupoPorId = ejs.associate { it.id to it.grupo }
                    val volumen = series.mapNotNull { grupoPorId[it.ejercicioId] }
                        .filter { it !in Gym.CATEGORIAS_NO_FUERZA }
                        .groupingBy { it }.eachCount()
                    var planeadas = 0
                    var completas = 0
                    for (dia in dias) {
                        if (dia > hoy) break
                        val iso = diaIsoDe(dia)
                        val fuerza = rs.filter { !it.diaria && Gym.aplicaEn(it, iso) }
                        if (fuerza.isEmpty()) continue
                        planeadas++
                        val ids = fuerza.map { it.id }.toSet()
                        val delDia = series.filter { it.fecha == fechaKey(dia) }.groupBy { it.ejercicioId }
                        val ejsDia = ejs.filter { it.rutinaId in ids }
                        if (ejsDia.isNotEmpty() &&
                            ejsDia.all { Gym.completo(it, delDia[it.id].orEmpty().map { s -> s.indice }.toSet()) }
                        ) completas++
                    }
                    SemanaGym(volumen, planeadas, completas)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SemanaGym())

    /** Ejercicios para los que toca subir peso: ejercicioId -> peso sugerido. */
    val sugerenciasDePeso: StateFlow<Map<Int, Float>> =
        _hoy.flatMapLatest { hoy ->
            combine(
                repository.ejercicios,
                repository.pesosEjercicio,
                repository.seriesEnRango(fechaKey(Fechas.desplazarDias(hoy, -60)), fechaKey(hoy))
            ) { ejs, historial, series ->
                ejs.mapNotNull { ej ->
                    val ultimoCambio = historial.filter { it.ejercicioId == ej.id }.maxOfOrNull { it.timestamp } ?: 0L
                    val desde = fechaKey(ultimoCambio)
                    val sesiones = series.filter { it.ejercicioId == ej.id && it.fecha >= desde }
                        .groupBy { it.fecha }
                        .count { (_, l) -> Gym.completo(ej, l.map { it.indice }.toSet()) }
                    Gym.sugerirSubida(ej.pesoKg, sesiones)?.let { ej.id to it }
                }.toMap()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun marcarSerie(ejercicioId: Int, indice: Int, hecha: Boolean) {
        val fecha = fechaKey(_inicioDiaSeleccionado.value)
        viewModelScope.launch { repository.marcarSerie(ejercicioId, fecha, indice, hecha) }
    }

    fun limpiarSeriesDelDia() {
        val fecha = fechaKey(_inicioDiaSeleccionado.value)
        viewModelScope.launch { repository.limpiarSeriesDelDia(fecha) }
    }

    fun guardarNotaSesion(texto: String) {
        val fecha = fechaKey(_inicioDiaSeleccionado.value)
        viewModelScope.launch { repository.guardarSesion(fecha, texto) }
    }

    fun subirPeso(ejercicio: EjercicioRutina, kg: Float) {
        if (kg <= 0f || kg > 1000f) {
            avisar("Escribe un peso válido en kilos")
            return
        }
        viewModelScope.launch {
            repository.subirPeso(ejercicio.id, kg)
            avisar("${ejercicio.nombre}: ${Gym.kg(kg)} kg")
        }
    }

    fun guardarEjercicio(ejercicio: EjercicioRutina) {
        if (ejercicio.nombre.isBlank()) {
            avisar("El ejercicio necesita un nombre")
            return
        }
        viewModelScope.launch {
            // Si cambia el peso de un ejercicio existente tambien queda en el historial.
            val anterior = ejercicios.value.firstOrNull { it.id == ejercicio.id }
            repository.guardarEjercicio(ejercicio.copy(nombre = ejercicio.nombre.trim()))
            if (anterior != null && ejercicio.pesoKg != null && ejercicio.pesoKg != anterior.pesoKg) {
                repository.subirPeso(ejercicio.id, ejercicio.pesoKg)
            }
        }
    }

    fun eliminarEjercicio(ejercicio: EjercicioRutina) {
        viewModelScope.launch { repository.eliminarEjercicio(ejercicio) }
    }

    fun moverEjercicio(ejercicio: EjercicioRutina, delta: Int) {
        val hermanos = ejercicios.value.filter { it.rutinaId == ejercicio.rutinaId }.sortedBy { it.orden }
        val i = hermanos.indexOfFirst { it.id == ejercicio.id }
        val j = i + delta
        if (i < 0 || j !in hermanos.indices) return
        // Si dos ejercicios comparten orden se separan usando su posicion.
        val a = hermanos[i]
        val b = hermanos[j]
        viewModelScope.launch {
            repository.moverEjercicio(a.copy(orden = i), b.copy(orden = j))
        }
    }

    fun guardarRutina(rutina: Rutina) {
        if (rutina.nombre.isBlank()) {
            avisar("La rutina necesita un nombre")
            return
        }
        viewModelScope.launch {
            if (rutina.id == 0) repository.crearRutina(rutina.copy(nombre = rutina.nombre.trim()))
            else repository.actualizarRutina(rutina.copy(nombre = rutina.nombre.trim()))
        }
    }

    fun eliminarRutina(rutina: Rutina) {
        viewModelScope.launch { repository.eliminarRutina(rutina) }
    }

    // ---------------- Suplementos ----------------

    val suplementos: StateFlow<List<Suplemento>> =
        repository.suplementos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ids de los suplementos tomados el dia seleccionado. */
    val suplementosTomados: StateFlow<Set<Int>> =
        _inicioDiaSeleccionado.flatMapLatest { repository.suplementosTomados(fechaKey(it)) }
            .map { l -> l.map { it.suplementoId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun marcarSuplemento(id: Int, tomado: Boolean) {
        val fecha = fechaKey(_inicioDiaSeleccionado.value)
        viewModelScope.launch { repository.marcarSuplemento(id, fecha, tomado) }
    }

    fun agregarSuplemento(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch { repository.agregarSuplemento(limpio) }
    }

    fun eliminarSuplemento(s: Suplemento) {
        viewModelScope.launch { repository.eliminarSuplemento(s.id) }
    }

    // ---------------- Medidas corporales ----------------

    val medidas: StateFlow<List<Medida>> =
        repository.medidas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun agregarMedida(tipo: String, cm: Float) {
        if (cm !in 5f..300f) {
            avisar("Escribe una medida válida en centímetros")
            return
        }
        viewModelScope.launch { repository.agregarMedida(tipo, cm) }
    }

    fun eliminarMedida(m: Medida) {
        viewModelScope.launch { repository.eliminarMedida(m.id) }
    }

    // ---------------- Racha, gasto real y detalle ----------------

    /** Dias seguidos marcados como completados (hoy aun no completado no rompe la racha). */
    val racha: StateFlow<Int> =
        combine(repository.diasCompletados, _hoy) { l, hoy -> Fechas.racha(l.toSet(), hoy) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Gasto energetico estimado: calorias promedio de los ultimos 28 dias menos lo que "sobra" o
     * "falta" segun el cambio real de peso. Requiere >= 10 dias con calorias y peso en >= 2 semanas.
     */
    val gastoReal: StateFlow<GastoReal?> =
        combine(progreso28, repository.pesos, repository.metricas, _hoy) { p, pesosL, metricas, hoy ->
            val cal = metricas.firstOrNull { claveMetrica(it.nombre) == Clave.CALORIAS } ?: return@combine null
            val dias = (p.totales[cal.id] ?: emptyList()).filter { it > 0f }
            if (dias.size < 10) return@combine null
            val desde = hoy - 28L * 24 * 60 * 60 * 1000
            val puntos = pesosL.filter { it.timestamp >= desde }.map { it.timestamp to it.kg }
            val ritmo = Cuerpo.ritmoKgPorSemana(puntos, dias = 28) ?: return@combine null
            val promedio = dias.average()
            GastoReal(promedio.toInt(), ritmo, Cuerpo.gastoEstimado(promedio, ritmo), dias.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Detalle (alimentos con hora y comida) del dia seleccionado. */
    val registrosDelDia: StateFlow<List<RegistroSuma>> =
        _inicioDiaSeleccionado.flatMapLatest { repository.registrosEnRango(it, finDeDia(it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Aplica la meta de calorias sugerida por el gasto real a la metrica de calorias. */
    fun aplicarCaloriasSugeridas(kcal: Int) {
        viewModelScope.launch {
            val cal = repository.metricas.first().firstOrNull { claveMetrica(it.nombre) == Clave.CALORIAS }
            if (cal == null) {
                avisar("No hay una métrica de calorías")
                return@launch
            }
            repository.guardarMetrica(cal.copy(limiteMaximo = kcal.toFloat()))
            avisar("Meta de calorías: $kcal kcal")
        }
    }

    // ---------------- Atajos de captura ----------------

    /** Suma un vaso de agua (250 ml) a la metrica de agua. */
    fun agregarAgua() {
        val agua = uiState.value.metricas.firstOrNull { claveMetrica(it.config.nombre) == Clave.AGUA }?.config
        if (agua == null) {
            avisar("No hay una métrica de agua")
            return
        }
        agregarRegistro(agua.id, if (agua.unidad.equals("ml", ignoreCase = true)) 250f else 0.25f)
    }

    /** Copia todo lo registrado el dia anterior al dia seleccionado. */
    fun copiarDiaAnterior() {
        val destino = _inicioDiaSeleccionado.value
        val origen = inicioRelativo(destino, -1)
        viewModelScope.launch {
            val n = repository.copiarDia(origen, destino, destino)
            avisar(if (n == 0) "El día anterior no tiene registros" else "Se copiaron $n registros del día anterior")
        }
    }

    fun marcarFavorito(alimento: AlimentoGuardado) {
        viewModelScope.launch { repository.marcarFavorito(alimento.id, !alimento.favorito) }
    }

    /** Une varios alimentos guardados en una comida nueva (suma de sus valores). */
    fun combinarAlimentos(nombre: String, lista: List<AlimentoGuardado>) {
        val limpio = nombre.trim()
        if (limpio.isEmpty() || lista.size < 2) return
        val suma = HashMap<String, Float>()
        lista.forEach { a -> decodificarValores(a.valores).forEach { (id, v) -> suma[id] = (suma[id] ?: 0f) + v } }
        viewModelScope.launch {
            repository.guardarAlimento(AlimentoGuardado(nombre = limpio, valores = codificarValores(suma)))
            avisar("Comida guardada: $limpio")
        }
    }

    // ---------------- Exportar CSV ----------------

    fun exportarCsvNutricion(escribir: (String) -> Unit) = exportarTexto(escribir, "CSV de nutrición guardado") {
        val d = repository.leerTodo()
        val metricas = d.metricas.associateBy { it.id }
        val hora = SimpleDateFormat("HH:mm", Locale.US)
        val filas = mutableListOf<List<Any?>>()
        d.registros.sortedBy { it.timestamp }.forEach { r ->
            val m = metricas[r.metricaId]
            filas += listOf(
                Fechas.clave(r.timestamp), hora.format(Date(r.timestamp)), m?.nombre ?: r.metricaId,
                m?.unidad ?: "", r.valor, r.comida ?: "", r.detalle ?: "", "detalle"
            )
        }
        d.totalesDiarios.sortedBy { it.fecha }.forEach { t ->
            val m = metricas[t.metricaId]
            filas += listOf(t.fecha, "", m?.nombre ?: t.metricaId, m?.unidad ?: "", t.total, "", "", "total archivado")
        }
        d.pesos.sortedBy { it.timestamp }.forEach { p ->
            filas += listOf(Fechas.clave(p.timestamp), hora.format(Date(p.timestamp)), "Peso corporal", "kg", p.kg, "", "", "peso")
        }
        d.medidas.sortedBy { it.timestamp }.forEach { m ->
            filas += listOf(Fechas.clave(m.timestamp), hora.format(Date(m.timestamp)), m.tipo, "cm", m.cm, "", "", "medida")
        }
        Csv.documento(listOf("fecha", "hora", "metrica", "unidad", "valor", "comida", "detalle", "origen"), filas)
    }

    fun exportarCsvGym(escribir: (String) -> Unit) = exportarTexto(escribir, "CSV de gym guardado") {
        val d = repository.leerTodo()
        val ejs = d.ejercicios.associateBy { it.id }
        val rutinas = d.rutinas.associateBy { it.id }
        fun rutinaDe(id: Int) = ejs[id]?.let { rutinas[it.rutinaId]?.nombre } ?: ""
        val filas = mutableListOf<List<Any?>>()
        d.pesosEjercicio.sortedBy { it.timestamp }.forEach { p ->
            val e = ejs[p.ejercicioId]
            filas += listOf(Fechas.clave(p.timestamp), rutinaDe(p.ejercicioId), e?.nombre ?: "", e?.grupo ?: "", "peso de trabajo (kg)", Gym.kg(p.kg))
        }
        d.seriesHechas.groupBy { it.fecha to it.ejercicioId }.toSortedMap(compareBy({ it.first }, { it.second }))
            .forEach { (clave, l) ->
                val e = ejs[clave.second]
                filas += listOf(clave.first, rutinaDe(clave.second), e?.nombre ?: "", e?.grupo ?: "", "series hechas", l.size)
            }
        d.sesiones.sortedBy { it.fecha }.forEach { s -> filas += listOf(s.fecha, "", "", "", "nota de sesión", s.nota) }
        Csv.documento(listOf("fecha", "rutina", "ejercicio", "grupo", "evento", "valor"), filas)
    }

    private fun exportarTexto(escribir: (String) -> Unit, exito: String, generar: suspend () -> String) {
        viewModelScope.launch {
            try {
                val texto = generar()
                withContext(Dispatchers.IO) { escribir(texto) }
                avisar(exito)
            } catch (e: Exception) {
                avisar("No se pudo exportar: ${e.message}")
            }
        }
    }

    // ---------------- Helpers ----------------

    private fun diaIsoDe(inicio: Long): Int =
        Gym.diaIso(Calendar.getInstance().apply { timeInMillis = inicio })

    private fun sembrarMetricasPorDefecto() {
        viewModelScope.launch {
            if (repository.hayMetricas()) return@launch
            val defaults = listOf(
                MetricaConfig("calorias", "Calorías", "kcal", 2000f, "#EF5350", 0),
                MetricaConfig("proteinas", "Proteínas", "gr", 120f, "#66BB6A", 1),
                MetricaConfig("carbohidratos", "Carbohidratos", "gr", 250f, "#42A5F5", 2),
                MetricaConfig("grasas", "Grasas", "gr", 70f, "#FF7043", 3),
                MetricaConfig("fibra", "Fibra", "gr", 30f, "#8D6E63", 4),
                MetricaConfig("azucar", "Azúcar", "gr", 30f, "#EC407A", 5),
                MetricaConfig("sodio", "Sodio", "mg", 2000f, "#FFCA28", 6),
                MetricaConfig("agua", "Agua", "L", 2f, "#26C6DA", 7)
            )
            defaults.forEach { repository.guardarMetrica(it) }
        }
    }

    private fun formato(valor: Double): String =
        if (valor % 1.0 == 0.0 && kotlin.math.abs(valor) < 1e15) valor.toLong().toString()
        else "%.2f".format(valor)

    private fun generarId(nombre: String): String {
        val slug = nombre.lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
        return "${slug}_${System.currentTimeMillis()}"
    }

    // ---------------- Helpers de fecha ----------------

    private fun fechaKey(inicio: Long): String = fmtFechaKey.format(Date(inicio))

    private fun etiquetaDia(inicio: Long): String =
        "${diasSemana[indiceSemana(inicio)]} ${fmtEtiqueta.format(Date(inicio))}"

    /** Devuelve el inicio (00:00) de hoy. */
    private fun inicioDeHoy(): Long = aMedianoche(Calendar.getInstance())

    /** Inicio del dia indicado (0=Lunes..6=Domingo) en la semana actual. */
    private fun inicioDeDiaSemana(indice: Int): Long {
        val c = Calendar.getInstance()
        // Calendar: DOMINGO=1..SABADO=7. Convertimos a Lunes=0..Domingo=6.
        val actual = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
        c.add(Calendar.DAY_OF_MONTH, indice - actual)
        return aMedianoche(c)
    }

    private fun indiceSemana(inicio: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = inicio }
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    /** Inicio (00:00) del dia que esta [delta] dias respecto de [inicio]. */
    private fun inicioRelativo(inicio: Long, delta: Int): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = inicio
            add(Calendar.DAY_OF_MONTH, delta)
        }
        return aMedianoche(c)
    }

    private fun finDeDia(inicio: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = inicio
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.timeInMillis
    }

    private fun aMedianoche(c: Calendar): Long {
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /**
     * Timestamp para un registro nuevo: si el dia seleccionado es hoy usa la
     * hora actual; si no, usa el mediodia de ese dia (para que caiga en rango).
     */
    private fun timestampParaRegistro(): Long {
        val inicio = _inicioDiaSeleccionado.value
        return if (inicio == inicioDeHoy()) System.currentTimeMillis()
        else inicio + 12L * 60 * 60 * 1000
    }

    // Arranque: se declara al final para que todas las propiedades ya existan.
    init {
        viewModelScope.launch {
            repository.sembrarRutinaInicial()
            archivarSiCorresponde()
        }
    }

    class Factory(
        private val repository: MetricaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
                "Unknown ViewModel class ${modelClass.name}"
            }
            return MainViewModel(repository) as T
        }
    }
}
