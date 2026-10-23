package com.mauricior8.calorias.data.backup

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
    val pesos: List<PesoRegistro> = emptyList()
)

/**
 * Serializa y lee el respaldo como JSON legible (usa org.json de Android).
 * Formato: {"app":"calorias","formato":1,"fecha":<ms>,"metricas":[...],...}
 */
object Respaldo {

    const val FORMATO = 1
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
            })
            .put("pesos", lista(d.pesos) { p ->
                JSONObject().put("id", p.id).put("timestamp", p.timestamp).put("kg", p.kg.toDouble())
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
                        tipoGrafica = o.getString("tipoGrafica")
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
                        o.getString("valores"), o.getLong("timestamp")
                    )
                },
                pesos = leer(raiz, "pesos") { o ->
                    PesoRegistro(o.getInt("id"), o.getLong("timestamp"), o.getDouble("kg").toFloat())
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
