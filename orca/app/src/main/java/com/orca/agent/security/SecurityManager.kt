package com.orca.agent.security

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    private val context: Context
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val sensitiveActions = listOf(
        "payment", "password", "credit card", "banking",
        "transfer", "checkout", "confirm purchase",
        "sign in", "login", "credential"
    )

    fun isSensitiveAction(actionDescription: String): Boolean {
        return sensitiveActions.any { actionDescription.contains(it, ignoreCase = true) }
    }

    fun requestAuthentication(
        activity: FragmentActivity,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        _authState.value = AuthState.Authenticating(reason)

        val biometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                authenticateWithBiometric(activity, reason, onSuccess, onFailure)
            }
            else -> {
                _authState.value = AuthState.RequiresFallback(reason)
                onFailure()
            }
        }
    }

    private fun authenticateWithBiometric(
        activity: FragmentActivity,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor: Executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                _authState.value = AuthState.Authenticated
                onSuccess()
            }
            override fun onAuthenticationFailed() {
                _authState.value = AuthState.Failed("Authentication failed")
                onFailure()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                _authState.value = AuthState.Error(errString.toString())
                onFailure()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Orca Security Checkpoint")
            .setSubtitle(reason)
            .setDescription("Verify your identity to proceed")
            .setNegativeButtonText("Cancel")
            .build()

        prompt.authenticate(promptInfo)
    }
}

sealed class AuthState {
    object Idle : AuthState()
    data class Authenticating(val reason: String) : AuthState()
    object Authenticated : AuthState()
    data class Failed(val error: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data class RequiresFallback(val reason: String) : AuthState()
}
