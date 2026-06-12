package com.orca.agent.security
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SecurityManager @Inject constructor(private val context: Context) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    private val sensitiveActions = listOf("payment","password","credit card","banking","transfer","checkout","confirm purchase","sign in","login","credential")
    fun isSensitiveAction(action: String): Boolean = sensitiveActions.any { action.contains(it,true) }
    fun requestAuthentication(activity: FragmentActivity, reason: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        _authState.value = AuthState.Authenticating(reason)
        val bm = BiometricManager.from(context)
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor: Executor = ContextCompat.getMainExecutor(context)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) { _authState.value = AuthState.Authenticated; onSuccess() }
                override fun onAuthenticationFailed() { _authState.value = AuthState.Failed("Failed"); onFailure() }
                override fun onAuthenticationError(c: Int, s: CharSequence) { _authState.value = AuthState.Error(s.toString()); onFailure() }
            }
            val prompt = BiometricPrompt(activity, executor, callback)
            val info = BiometricPrompt.PromptInfo.Builder().setTitle("Orca Security").setSubtitle(reason).setNegativeButtonText("Cancel").build()
            prompt.authenticate(info)
        } else { _authState.value = AuthState.RequiresFallback(reason); onFailure() }
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
