package com.mauricior8.calorias.seguridad

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Bloqueo de la app con huella, rostro o el PIN/patron del telefono. No guarda ninguna
 * credencial: delega todo en el sistema (BiometricPrompt).
 */
object Bloqueo {

    private const val AUTENTICADORES =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** True si el telefono tiene huella/rostro o un PIN/patron configurado. */
    fun disponible(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS

    fun pedir(activity: FragmentActivity, onExito: () -> Unit, onFallo: (String) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onExito()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFallo(errString.toString())
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Calorías")
            .setSubtitle("Usa tu huella, rostro o el PIN del teléfono")
            .setAllowedAuthenticators(AUTENTICADORES)
            .build()
        prompt.authenticate(info)
    }
}
