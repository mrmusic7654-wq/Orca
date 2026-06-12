package com.orca.agent.core

import android.content.Context
import android.os.Vibrator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyStopHandler @Inject constructor(
    private val context: Context
) {
    private val _emergencyState = MutableStateFlow(EmergencyState.NORMAL)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState.asStateFlow()
    private val _lastIdentityVerification = MutableStateFlow(System.currentTimeMillis())
    val lastIdentityVerification: StateFlow<Long> = _lastIdentityVerification.asStateFlow()

    private val emergencyKeywords = listOf("stop", "abort", "cancel everything", "emergency", "shut down", "halt")

    fun initiateEmergencyStop(reason: String) {
        _emergencyState.value = EmergencyState.EMERGENCY_STOP
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200, 255))
        _emergencyState.value = EmergencyState.SAFE_MODE
    }

    fun detectEmergencyCommand(text: String): Boolean {
        val lowerText = text.lowercase().trim()
        return emergencyKeywords.any { lowerText == it || lowerText.startsWith("$it ") }
    }

    fun enterSafeMode(reason: String) {
        _emergencyState.value = EmergencyState.SAFE_MODE
    }

    fun isInSafeMode(): Boolean {
        return _emergencyState.value == EmergencyState.SAFE_MODE ||
               _emergencyState.value == EmergencyState.EMERGENCY_STOP
    }
}

enum class EmergencyState { NORMAL, SAFE_MODE, EMERGENCY_STOP }
