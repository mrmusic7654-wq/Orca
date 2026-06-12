package com.orca.agent.security

import android.content.Context
import android.hardware.biometrics.BiometricManager
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

    fun isSensitiveAction(action: String): Boolean {
        return sensitiveActions.any { action.contains(it, ignoreCase = true) }
    }

    fun canAuthenticate(): Boolean {
        return try {
            val bm: BiometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as BiometricManager
            bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun isBiometricAvailable(): Boolean {
        return try {
            val bm: BiometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as BiometricManager
            bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
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
