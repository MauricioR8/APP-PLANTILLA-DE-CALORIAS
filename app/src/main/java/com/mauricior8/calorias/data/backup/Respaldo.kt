package com.mauricior8.calorias.data.backup

import com.mauricior8.calorias.data.local.entity.Ajuste
import com.mauricior8.calorias.data.local.entity.AlimentoGuardado
import com.mauricior8.calorias.data.local.entity.CalculoHistorial
import com.mauricior8.calorias.data.local.entity.CeldaTabla
import com.mauricior8.calorias.data.local.entity.ColumnaTabla
import com.mauricior8.calorias.data.local.entity.EjercicioRutina
import com.mauricior8.calorias.data.local.entity.EstadoDia
import com.mauricior8.calorias.data.local.entity.FilaTabla
import com.mauricior8.calorias.data.local.entity.Medida
import com.mauricior8.calorias.data.local.entity.MetricaConfig
import com.mauricior8.calorias.data.local.entity.Nota
import com.mauricior8.calorias.data.local.entity.PesoEjercicio
import com.mauricior8.calorias.data.local.entity.PesoRegistro
import com.mauricior8.calorias.data.local.entity.RegistroSuma
import com.mauricior8.calorias.data.local.entity.Rutina
import com.mauricior8.calorias.data.local.entity.SerieHecha
import com.mauricior8.calorias.data.local.entity.SesionGym
import com.mauricior8.calorias.data.local.entity.Suplemento
import com.mauricior8.calorias.data.local.entity.SuplementoTomado
import com.mauricior8.calorias.data.local.entity.TablaAlimentos
import com.mauricior8.calorias.data.local.entity.TotalDiario
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Contenido completo de la base de datos, listo para exportar o importar. */
data class DatosRespaldo(
    val metricas: List<MetricaConfig> = emptyList(),
    val registros: List<RegistroSuma> = emptyList(),
    val notas: List<Nota> = emptyList(),
    val calculos: List<CalculoHistorial> = emptyList(),
    val tablas: List<TablaAlimentos> = emptyList(),
    val columnas: List<ColumnaTabla> = emptyList(),
    val filas: List<FilaTabla> = emptyList(),
    val celdas: List<CeldaTabla> = emptyList(),
    val estadosDia: List<EstadoDia> = emptyList(),
    val alimentos: List<AlimentoGuardado> = emptyList(),
    val pesos: List<PesoRegistro> = emptyList(),
    // Formato 2
    val ajustes: List<Ajuste> = emptyList(),
    val rutinas: List<Rutina> = emptyList(),
    val ejercicios: List<EjercicioRutina> = emptyList(),
    val seriesHechas: List<SerieHecha> = emptyList(),
    val pesosEjercicio: List<PesoEjercicio> = emptyList(),
    val sesiones: List<SesionGym> = emptyList(),
    val suplementos: List<Suplemento> = emptyList(),
    val suplementosTomados: List<SuplementoTomado> = emptyList(),
    val medidas: List<Medida> = emptyList(),
    val totalesDiarios: List<TotalDiario> = emptyList()
)

/**
 * Serializa y lee el respaldo como JSON legible (usa org.json de Android).
 * Formato: {"app":"calorias","formato":2,"fecha":<ms>,"metricas":[...],...}
 * Los respaldos de formato 1 (sin gym ni extras) se siguen pudiendo leer.
 */
object Respaldo {

    const val FORMATO = 2
    private const val APP = "calorias"

    fun aJson(d: DatosRespaldo, fecha: Long = System.currentTimeMillis()): String {
        val raiz = JSONObject()
            .put("app", APP)
            .put("formato", FORMATO)
            .put("fecha", fecha)
            .put("metricas", lista(d.metricas) { m ->
                JSONObject().put("id", m.id).put("nombre", m.nombre).put("unidad", m.unidad)
                    .put("limiteMaximo", m.limiteMaximo?.toDouble() ?: JSONObject.NULL)
                    .put("colorHex", m.colorHex).put("orden", m.orden)
                    .put("tipoGrafica", m.tipoGrafica)
                    .put("limiteDescanso", m.limiteDescanso?.toDouble() ?: JSONObject.NULL)
            })
            .put("registros", lista(d.registros) { r ->
                JSONObject().put("id", r.id).put("metricaId", r.metricaId)
                    .put("valor", r.valor.toDouble()).put("timestamp", r.timestamp)
                    .put("tipo", r.tipo).put("detalle", r.detalle ?: JSONObject.NULL)
                    .put("comida", r.comida ?: JSONObject.NULL)
            })
            .put("notas", lista(d.notas) { n ->
                JSONObject().put("id", n.id).put("texto", n.texto).put("timestamp", n.timestamp)
            })
            .put("calculos", lista(d.calculos) { c ->
                JSONObject().put("id", c.id).put("expresion", c.expresion)
                    .put("resultado", c.resultado).put("timestamp", c.timestamp)
            })
            .put("tablas", lista(d.tablas) { t ->
                JSONObject().put("id", t.id).put("nombre", t.nombre)
                    .put("tipoGrafica", t.tipoGrafica).put("timestamp", t.timestamp)
            })
            .put("columnas", lista(d.columnas) { c ->
                JSONObject().put("id", c.id).put("tablaId", c.tablaId)
                    .put("nombre", c.nombre).put("orden", c.orden)
            })
            .put("filas", lista(d.filas) { f ->
                JSONObject().put("id", f.id).put("tablaId", f.tablaId)
                    .put("nombre", f.nombre).put("orden", f.orden)
            })
            .put("celdas", lista(d.celdas) { c ->
                JSONObject().put("id", c.id).put("tablaId", c.tablaId).put("filaId", c.filaId)
                    .put("columnaId", c.columnaId).put("valor", c.valor)
            })
            .put("estadosDia", lista(d.estadosDia) { e ->
                JSONObject().put("fecha", e.fecha).put("completado", e.completado)
            })
            .put("alimentos", lista(d.alimentos) { a ->
                JSONObject().put("id", a.id).put("nombre", a.nombre)
                    .put("valores", a.valores).put("timestamp", a.timestamp)
                    .put("favorito", a.favorito)
            })
            .put("pesos", lista(d.pesos) { p ->
                JSONObject().put("id", p.id).put("timestamp", p.timestamp).put("kg", p.kg.toDouble())
            })
            .put("ajustes", lista(d.ajustes) { a ->
                JSONObject().put("clave", a.clave).put("valor", a.valor)
            })
            .put("rutinas", lista(d.rutinas) { r ->
                JSONObject().put("id", r.id).put("nombre", r.nombre).put("colorHex", r.colorHex)
                    .put("dias", r.dias).put("diaria", r.diaria)
                    .put("nota", r.nota ?: JSONObject.NULL).put("orden", r.orden)
            })
            .put("ejercicios", lista(d.ejercicios) { e ->
                JSONObject().put("id", e.id).put("rutinaId", e.rutinaId).put("nombre", e.nombre)
                    .put("grupo", e.grupo).put("series", e.series).put("reps", e.reps)
                    .put("esTiempo", e.esTiempo).put("descansoSeg", e.descansoSeg)
                    .put("vecesPorDia", e.vecesPorDia)
                    .put("pesoKg", e.pesoKg?.toDouble() ?: JSONObject.NULL)
                    .put("nota", e.nota ?: JSONObject.NULL).put("orden", e.orden)
            })
            .put("seriesHechas", lista(d.seriesHechas) { s ->
                JSONObject().put("ejercicioId", s.ejercicioId).put("fecha", s.fecha).put("indice", s.indice)
            })
            .put("pesosEjercicio", lista(d.pesosEjercicio) { p ->
                JSONObject().put("id", p.id).put("ejercicioId", p.ejercicioId)
                    .put("timestamp", p.timestamp).put("kg", p.kg.toDouble())
            })
            .put("sesiones", lista(d.sesiones) { s ->
                JSONObject().put("fecha", s.fecha).put("nota", s.nota)
            })
            .put("suplementos", lista(d.suplementos) { s ->
                JSONObject().put("id", s.id).put("nombre", s.nombre).put("orden", s.orden)
            })
            .put("suplementosTomados", lista(d.suplementosTomados) { s ->
                JSONObject().put("suplementoId", s.suplementoId).put("fecha", s.fecha)
            })
            .put("medidas", lista(d.medidas) { m ->
                JSONObject().put("id", m.id).put("timestamp", m.timestamp)
                    .put("tipo", m.tipo).put("cm", m.cm.toDouble())
            })
            .put("totalesDiarios", lista(d.totalesDiarios) { t ->
                JSONObject().put("fecha", t.fecha).put("metricaId", t.metricaId).put("total", t.total.toDouble())
            })
        return raiz.toString(2)
    }

    /** @throws IllegalArgumentException si el texto no es un respaldo valido. */
    fun deJson(texto: String): DatosRespaldo {
        try {
            val raiz = JSONObject(texto)
            require(raiz.optString("app") == APP) { "El archivo no es un respaldo de esta app" }
            val formato = raiz.optInt("formato", -1)
            require(formato in 1..FORMATO) { "Versión de respaldo no compatible ($formato)" }
            return DatosRespaldo(
                metricas = leer(raiz, "metricas") { o ->
                    MetricaConfig(
                        id = o.getString("id"), nombre = o.getString("nombre"),
                        unidad = o.getString("unidad"),
                        limiteMaximo = if (o.isNull("limiteMaximo")) null
                        else o.getDouble("limiteMaximo").toFloat(),
                        colorHex = o.getString("colorHex"), orden = o.getInt("orden"),
                        tipoGrafica = o.getString("tipoGrafica"),
                        limiteDescanso = if (!o.has("limiteDescanso") || o.isNull("limiteDescanso")) null
                        else o.getDouble("limiteDescanso").toFloat()
                    )
                },
                registros = leer(raiz, "registros") { o ->
                    RegistroSuma(
                        id = o.getInt("id"), metricaId = o.getString("metricaId"),
                        valor = o.getDouble("valor").toFloat(), timestamp = o.getLong("timestamp"),
                        tipo = o.getString("tipo"),
                        detalle = if (o.isNull("detalle")) null else o.getString("detalle"),
                        comida = if (o.isNull("comida")) null else o.getString("comida")
                    )
                },
                notas = leer(raiz, "notas") { o ->
                    Nota(o.getInt("id"), o.getString("texto"), o.getLong("timestamp"))
                },
                calculos = leer(raiz, "calculos") { o ->
                    CalculoHistorial(
                        o.getInt("id"), o.getString("expresion"),
                        o.getString("resultado"), o.getLong("timestamp")
                    )
                },
                tablas = leer(raiz, "tablas") { o ->
                    TablaAlimentos(
                        o.getInt("id"), o.getString("nombre"),
                        o.getString("tipoGrafica"), o.getLong("timestamp")
                    )
                },
                columnas = leer(raiz, "columnas") { o ->
                    ColumnaTabla(o.getInt("id"), o.getInt("tablaId"), o.getString("nombre"), o.getInt("orden"))
                },
                filas = leer(raiz, "filas") { o ->
                    FilaTabla(o.getInt("id"), o.getInt("tablaId"), o.getString("nombre"), o.getInt("orden"))
                },
                celdas = leer(raiz, "celdas") { o ->
                    CeldaTabla(
                        o.getInt("id"), o.getInt("tablaId"), o.getInt("filaId"),
                        o.getInt("columnaId"), o.getString("valor")
                    )
                },
                estadosDia = leer(raiz, "estadosDia") { o ->
                    EstadoDia(o.getString("fecha"), o.getBoolean("completado"))
                },
                alimentos = leer(raiz, "alimentos") { o ->
                    AlimentoGuardado(
                        o.getInt("id"), o.getString("nombre"),
                        o.getString("valores"), o.getLong("timestamp"),
                        favorito = o.optBoolean("favorito", false)
                    )
                },
                pesos = leer(raiz, "pesos") { o ->
                    PesoRegistro(o.getInt("id"), o.getLong("timestamp"), o.getDouble("kg").toFloat())
                },
                ajustes = leer(raiz, "ajustes") { o -> Ajuste(o.getString("clave"), o.getString("valor")) },
                rutinas = leer(raiz, "rutinas") { o ->
                    Rutina(
                        id = o.getInt("id"), nombre = o.getString("nombre"), colorHex = o.getString("colorHex"),
                        dias = o.getString("dias"), diaria = o.getBoolean("diaria"),
                        nota = if (o.isNull("nota")) null else o.getString("nota"),
                        orden = o.getInt("orden")
                    )
                },
                ejercicios = leer(raiz, "ejercicios") { o ->
                    EjercicioRutina(
                        id = o.getInt("id"), rutinaId = o.getInt("rutinaId"), nombre = o.getString("nombre"),
                        grupo = o.getString("grupo"), series = o.getInt("series"), reps = o.getInt("reps"),
                        esTiempo = o.getBoolean("esTiempo"), descansoSeg = o.getInt("descansoSeg"),
                        vecesPorDia = o.getInt("vecesPorDia"),
                        pesoKg = if (o.isNull("pesoKg")) null else o.getDouble("pesoKg").toFloat(),
                        nota = if (o.isNull("nota")) null else o.getString("nota"),
                        orden = o.getInt("orden")
                    )
                },
                seriesHechas = leer(raiz, "seriesHechas") { o ->
                    SerieHecha(o.getInt("ejercicioId"), o.getString("fecha"), o.getInt("indice"))
                },
                pesosEjercicio = leer(raiz, "pesosEjercicio") { o ->
                    PesoEjercicio(
                        o.getInt("id"), o.getInt("ejercicioId"),
                        o.getLong("timestamp"), o.getDouble("kg").toFloat()
                    )
                },
                sesiones = leer(raiz, "sesiones") { o -> SesionGym(o.getString("fecha"), o.getString("nota")) },
                suplementos = leer(raiz, "suplementos") { o ->
                    Suplemento(o.getInt("id"), o.getString("nombre"), o.getInt("orden"))
                },
                suplementosTomados = leer(raiz, "suplementosTomados") { o ->
                    SuplementoTomado(o.getInt("suplementoId"), o.getString("fecha"))
                },
                medidas = leer(raiz, "medidas") { o ->
                    Medida(o.getInt("id"), o.getLong("timestamp"), o.getString("tipo"), o.getDouble("cm").toFloat())
                },
                totalesDiarios = leer(raiz, "totalesDiarios") { o ->
                    TotalDiario(o.getString("fecha"), o.getString("metricaId"), o.getDouble("total").toFloat())
                }
            )
        } catch (e: JSONException) {
            throw IllegalArgumentException("Archivo de respaldo dañado: ${e.message}", e)
        }
    }

    private fun <T> lista(items: List<T>, aObjeto: (T) -> JSONObject): JSONArray =
        JSONArray().also { arr -> items.forEach { arr.put(aObjeto(it)) } }

    // Una seccion ausente se interpreta como vacia (respaldos de versiones anteriores).
    private fun <T> leer(raiz: JSONObject, clave: String, desdeObjeto: (JSONObject) -> T): List<T> {
        val arr = raiz.optJSONArray(clave) ?: return emptyList()
        return (0 until arr.length()).map { desdeObjeto(arr.getJSONObject(it)) }
    }
}
