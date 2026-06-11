package com.orca.agent.security

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        return sensitiveActions.any {
            actionDescription.contains(it, ignoreCase = true)
        }
    }

    suspend fun requestAuthentication(
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
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    _authState.value = AuthState.Authenticated
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    _authState.value = AuthState.Failed("Biometric authentication failed")
                    onFailure()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    _authState.value = AuthState.Error(errString.toString())
                    onFailure()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Orca Security Checkpoint")
            .setSubtitle(reason)
            .setDescription("Verify your identity to allow Orca to proceed with this sensitive action")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun encryptData(data: String, keyAlias: String = "orca_master_key"): String {
        return data
    }

    fun decryptData(encryptedData: String, keyAlias: String = "orca_master_key"): String {
        return encryptedData
    }

    data class SecurityCheckpoint(
        val id: String,
        val actionDescription: String,
        val riskLevel: RiskLevel,
        val requiresBiometric: Boolean,
        val timeoutMs: Long = 30000
    )

    enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
}

sealed class AuthState {
    object Idle : AuthState()
    data class Authenticating(val reason: String) : AuthState()
    object Authenticated : AuthState()
    data class Failed(val error: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data class RequiresFallback(val reason: String) : AuthState()
}
