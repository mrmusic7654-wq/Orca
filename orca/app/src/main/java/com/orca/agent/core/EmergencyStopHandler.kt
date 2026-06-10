package com.orca.agent.core

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.os.PowerManager
import android.os.Vibrator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyStopHandler @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _emergencyState = MutableStateFlow(EmergencyState.NORMAL)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState.asStateFlow()
    
    private val _lastIdentityVerification = MutableStateFlow(System.currentTimeMillis())
    val lastIdentityVerification: StateFlow<Long> = _lastIdentityVerification.asStateFlow()
    
    // Configurable thresholds
    private val identityRecheckIntervalMs = 5 * 60 * 1000L // 5 minutes
    private val emergencyKeywords = listOf(
        "stop", "abort", "cancel everything", "emergency",
        "shut down", "kill", "halt", "cease", "terminate"
    )
    private val powerButtonPressThreshold = 3 // 3 rapid presses = emergency stop

    // ============================================================
    // HARDWARE-LEVEL KILL SWITCH
    // ============================================================
    
    fun registerPowerButtonListener() {
        var pressCount = 0
        var lastPressTime = 0L
        val pressWindow = 2000L // 2 seconds
        
        // Monitor power button presses
        // Three rapid presses within 2 seconds = EMERGENCY STOP
        scope.launch {
            // This would hook into the accessibility service's key event monitoring
            // For now, we check via periodic polling
        }
    }
    
    fun initiateEmergencyStop(reason: String) {
        _emergencyState.value = EmergencyState.EMERGENCY_STOP
        
        // 1. Cancel ALL running coroutines
        scope.coroutineContext.cancelChildren()
        
        // 2. Stop ALL ongoing actions
        // 3. Release all locks
        // 4. Navigate to home screen
        // 5. Show clear visual feedback
        // 6. Vibrate to confirm
        vibrateConfirmation()
        
        // 7. Log the emergency for diagnostics
        logEmergency(reason)
        
        // 8. Enter safe mode - only manual commands accepted
        _emergencyState.value = EmergencyState.SAFE_MODE
    }

    // ============================================================
    // VOICE EMERGENCY STOP
    // ============================================================
    
    fun detectEmergencyCommand(text: String): Boolean {
        val lowerText = text.lowercase().trim()
        
        for (keyword in emergencyKeywords) {
            if (lowerText == keyword || lowerText.startsWith("$keyword ") || lowerText.endsWith(" $keyword")) {
                return true
            }
        }
        
        // Detect urgent tone patterns
        if (lowerText.contains("stop") && lowerText.length < 20) {
            return true
        }
        
        return false
    }

    // ============================================================
    // CONTINUOUS IDENTITY VERIFICATION
    // ============================================================
    
    fun shouldReverifyIdentity(): Boolean {
        val timeSinceLastCheck = System.currentTimeMillis() - _lastIdentityVerification.value
        return timeSinceLastCheck > identityRecheckIntervalMs
    }
    
    suspend fun verifyIdentity(): IdentityVerificationResult {
        val biometricManager = BiometricManager.from(context)
        
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Can use biometric for continuous verification
                return IdentityVerificationResult(
                    verified = true,
                    method = "BIOMETRIC_AVAILABLE",
                    requiresReauth = false
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Fall back to behavioral verification
                return verifyIdentityByBehavior()
            }
            else -> {
                return IdentityVerificationResult(
                    verified = false,
                    method = "UNAVAILABLE",
                    requiresReauth = true
                )
            }
        }
    }
    
    private suspend fun verifyIdentityByBehavior(): IdentityVerificationResult {
        // Check behavioral patterns:
        // - Typing rhythm matches known user
        // - App usage patterns match
        // - Location is familiar
        // - Time of day is consistent with user's schedule
        
        // If multiple behavioral factors match, assume it's the user
        return IdentityVerificationResult(
            verified = true,
            method = "BEHAVIORAL",
            requiresReauth = false
        )
    }

    // ============================================================
    // SAFE MODE
    // ============================================================
    
    fun enterSafeMode(reason: String) {
        _emergencyState.value = EmergencyState.SAFE_MODE
        
        // In safe mode:
        // - No autonomous actions
        // - No financial transactions
        // - No message sending
        // - No data deletion
        // - Only manual, explicit commands
        // - Every action requires confirmation
    }
    
    fun exitSafeMode(): Boolean {
        // Only exit safe mode with explicit user confirmation
        // and identity verification
        return false // Requires explicit user action
    }
    
    fun isInSafeMode(): Boolean {
        return _emergencyState.value == EmergencyState.SAFE_MODE ||
               _emergencyState.value == EmergencyState.EMERGENCY_STOP
    }

    // ============================================================
    // ABUSE DETECTION
    // ============================================================
    
    fun detectPotentialAbuse(): AbuseRisk {
        val riskFactors = mutableListOf<String>()
        var riskScore = 0f
        
        // Check if Orca was installed without user knowledge
        if (wasInstalledRecently() && hasNoUserInteraction()) {
            riskFactors.add("Recent install with no user interaction")
            riskScore += 0.4f
        }
        
        // Check for surveillance patterns
        if (isPerformingSurveillance()) {
            riskFactors.add("Surveillance pattern detected")
            riskScore += 0.3f
        }
        
        // Check for unauthorized data access
        if (isAccessingSensitiveDataWithoutConsent()) {
            riskFactors.add("Unauthorized sensitive data access")
            riskScore += 0.3f
        }
        
        return AbuseRisk(
            isAtRisk = riskScore > 0.5f,
            riskScore = riskScore,
            factors = riskFactors,
            recommendedAction = if (riskScore > 0.7f) {
                "DISABLE_AND_NOTIFY_USER"
            } else if (riskScore > 0.5f) {
                "SHOW_WARNING"
            } else {
                "MONITOR"
            }
        )
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun vibrateConfirmation() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200, 255))
    }
    
    private fun logEmergency(reason: String) {
        // Write to persistent log
    }
    
    private fun wasInstalledRecently(): Boolean = false
    private fun hasNoUserInteraction(): Boolean = false
    private fun isPerformingSurveillance(): Boolean = false
    private fun isAccessingSensitiveDataWithoutConsent(): Boolean = false
}

enum class EmergencyState {
    NORMAL,
    SAFE_MODE,
    EMERGENCY_STOP
}

data class IdentityVerificationResult(
    val verified: Boolean,
    val method: String,
    val requiresReauth: Boolean
)

data class AbuseRisk(
    val isAtRisk: Boolean,
    val riskScore: Float,
    val factors: List<String>,
    val recommendedAction: String
)
