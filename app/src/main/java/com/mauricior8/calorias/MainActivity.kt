package com.mauricior8.calorias

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mauricior8.calorias.widget.ResumenWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mauricior8.calorias.seguridad.Bloqueo
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.ui.screen.MainScreen
import com.mauricior8.calorias.ui.screen.PantallaBloqueo
import com.mauricior8.calorias.ui.theme.CaloriasTheme

// FragmentActivity (en vez de ComponentActivity) porque BiometricPrompt lo exige.
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as CaloriasApp
        MainViewModel.Factory(app.repository)
    }

    /** True hasta autenticarse (al abrir la app o tras 30 s en segundo plano). */
    private var bloqueada by mutableStateOf(true)
    private var segundoPlanoDesde = 0L
    private var pidiendoAutenticacion = false

    override fun onStart() {
        super.onStart()
        val bloqueoActivo = viewModel.ajustes.value["bloqueo"] == "1"
        if (bloqueoActivo && segundoPlanoDesde != 0L &&
            SystemClock.elapsedRealtime() - segundoPlanoDesde > TIEMPO_GRACIA_MS
        ) {
            bloqueada = true
        }
    }

    override fun onStop() {
        super.onStop()
        segundoPlanoDesde = SystemClock.elapsedRealtime()
        // Deja el widget de la pantalla de inicio al dia con lo que se acaba de registrar.
        val contexto = applicationContext
        lifecycleScope.launch(Dispatchers.IO) { ResumenWidget.actualizarTodos(contexto) }
    }

    override fun onResume() {
        super.onResume()
        // Si la app quedo abierta y cambio el dia, avanzar a "hoy".
        viewModel.alReanudar()
    }

    private fun intentarDesbloquear() {
        if (pidiendoAutenticacion) return
        if (!Bloqueo.disponible(this)) {
            // El telefono ya no tiene PIN/huella: no dejar al usuario fuera de su propia app.
            viewModel.setAjuste("bloqueo", "0")
            viewModel.avisar("Bloqueo desactivado: el teléfono no tiene PIN, patrón ni huella configurados")
            bloqueada = false
            return
        }
        pidiendoAutenticacion = true
        Bloqueo.pedir(
            this,
            onExito = { pidiendoAutenticacion = false; bloqueada = false },
            onFallo = { pidiendoAutenticacion = false }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
            val listos by viewModel.ajustesListos.collectAsStateWithLifecycle()
            val bloqueoActivo = ajustes["bloqueo"] == "1"

            // Sin bloqueo no hay nada que desbloquear (asi, al activarlo luego, no se bloquea al instante).
            LaunchedEffect(bloqueoActivo) { if (!bloqueoActivo) bloqueada = false }
            LaunchedEffect(listos, bloqueoActivo, bloqueada) {
                if (listos && bloqueoActivo && bloqueada) intentarDesbloquear()
            }

            CaloriasTheme(tema = ajustes["tema"] ?: "sistema") {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        !listos -> Box(Modifier.fillMaxSize())
                        bloqueoActivo && bloqueada -> PantallaBloqueo(onDesbloquear = ::intentarDesbloquear)
                        else -> MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private companion object {
        const val TIEMPO_GRACIA_MS = 30_000L
    }
}
