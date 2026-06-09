package com.mauricior8.calorias.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.repository.MetricaRepository
import com.mauricior8.calorias.util.Calculadora
import com.mauricior8.calorias.ui.state.MetricaConItem
import com.mauricior8.calorias.ui.state.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    // ---------------- Seleccion de dia ----------------

    /** Inicio (00:00) del dia seleccionado, en milisegundos epoch. */
    private val _inicioDiaSeleccionado = MutableStateFlow(inicioDeHoy())

    /** Etiquetas de Lunes a Domingo. */
    val diasSemana: List<String> = listOf(
        "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"
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

    // ---------------- Estado de Metricas ----------------

    val uiState: StateFlow<UiState> =
        _inicioDiaSeleccionado.flatMapLatest { inicio ->
            val fin = finDeDia(inicio)
            combine(
                repository.metricas,
                repository.totalesDeDia(inicio, fin)
            ) { metricas, totales ->
                val totalesPorId = totales.associate { it.metricaId to it.total }
                val items = metricas.map { config ->
                    MetricaConItem(
                        config = config,
                        totalHoy = totalesPorId[config.id] ?: 0f
                    )
                }
                UiState(isLoading = false, metricas = items)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(isLoading = true)
        )

    fun historial(metricaId: String) = repository.historial(metricaId)

    fun seleccionarDia(indice: Int) {
        _inicioDiaSeleccionado.value = inicioDeDiaSemana(indice)
    }

    fun limpiarDia() {
        val inicio = _inicioDiaSeleccionado.value
        viewModelScope.launch { repository.limpiarDia(inicio, finDeDia(inicio)) }
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

    // ---------------- Acciones: Metricas ----------------

    fun crearMetrica(
        nombre: String,
        unidad: String,
        limiteMaximo: Float? = null,
        colorHex: String = "#FF6D00"
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
                    orden = repository.siguienteOrden()
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
            // Intercambiar valores de orden.
            repository.actualizarOrden(actual.id, vecino.orden)
            repository.actualizarOrden(vecino.id, actual.orden)
        }
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
     * usando el nombre del alimento como detalle.
     *
     * @param valores mapa metricaId -> valor aportado.
     */
    fun agregarAlimento(nombre: String, valores: Map<String, Float>) {
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
                        detalle = nombreLimpio
                    )
                )
            }
        }
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

    fun agregarColumna(tablaId: Int, nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch { repository.agregarColumna(tablaId, limpio) }
    }

    fun agregarFila(tablaId: Int, nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch { repository.agregarFila(tablaId, limpio) }
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

    // ---------------- Helpers ----------------

    private fun sembrarMetricasPorDefecto() {
        viewModelScope.launch {
            if (repository.hayMetricas()) return@launch
            val defaults = listOf(
                MetricaConfig("calorias", "Calorias", "kcal", 2000f, "#EF5350", 0),
                MetricaConfig("proteinas", "Proteinas", "gr", 120f, "#66BB6A", 1),
                MetricaConfig("carbohidratos", "Carbohidratos", "gr", 250f, "#42A5F5", 2),
                MetricaConfig("azucar", "Azucar", "gr", 30f, "#EC407A", 3),
                MetricaConfig("sodio", "Sodio", "mg", 2000f, "#FFCA28", 4),
                MetricaConfig("agua", "Agua", "L", 2f, "#26C6DA", 5)
            )
            defaults.forEach { repository.guardarMetrica(it) }
        }
    }

    private fun formato(valor: Float): String =
        if (valor % 1f == 0f) valor.toInt().toString() else "%.2f".format(valor)

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
