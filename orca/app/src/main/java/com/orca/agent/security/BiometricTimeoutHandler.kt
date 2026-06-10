package com.orca.agent.security

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricTimeoutHandler @Inject constructor() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _timeoutState = MutableStateFlow<TimeoutState>(TimeoutState.Idle)
    val timeoutState: StateFlow<TimeoutState> = _timeoutState.asStateFlow()
    
    private var timeoutJob: Job? = null
    private var onTimeoutAction: (() -> Unit)? = null
    
    companion object {
        const val DEFAULT_TIMEOUT_MS = 30_000L // 30 seconds
        const val EXTENDED_TIMEOUT_MS = 60_000L // 60 seconds for important tasks
        const val FINAL_WARNING_MS = 10_000L // 10 seconds before timeout
    }

    // ============================================================
    // TIMEOUT MANAGEMENT
    // ============================================================
    
    fun startTimeout(
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        onTimeout: () -> Unit
    ) {
        // Cancel any existing timeout
        cancelTimeout()
        
        _timeoutState.value = TimeoutState.Waiting(System.currentTimeMillis() + timeoutMs)
        onTimeoutAction = onTimeout
        
        timeoutJob = scope.launch {
            // Wait for most of the timeout period
            delay(timeoutMs - FINAL_WARNING_MS)
            
            if (isActive) {
                _timeoutState.value = TimeoutState.FinalWarning(
                    "Authentication required. Timing out in ${FINAL_WARNING_MS / 1000} seconds."
                )
                
                // Wait for final warning period
                delay(FINAL_WARNING_MS)
                
                if (isActive) {
                    _timeoutState.value = TimeoutState.TimedOut
                    onTimeoutAction?.invoke()
                }
            }
        }
    }
    
    fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
        onTimeoutAction = null
        _timeoutState.value = TimeoutState.Idle
    }
    
    fun resetTimeout() {
        // User interacted - extend the timeout
        val currentState = _timeoutState.value
        if (currentState is TimeoutState.Waiting) {
            startTimeout(
                timeoutMs = DEFAULT_TIMEOUT_MS,
                onTimeout = onTimeoutAction ?: {}
            )
        }
    }

    // ============================================================
    // TIMEOUT RESPONSE
    // ============================================================
    
    fun onTimeout(): TimeoutResponse {
        return when (_timeoutState.value) {
            is TimeoutState.TimedOut -> {
                TimeoutResponse(
                    shouldPause = true,
                    shouldNotify = true,
                    message = "Authentication timed out. Task has been paused for security.",
                    canResume = true,
                    requiresReAuthentication = true
                )
            }
            else -> {
                TimeoutResponse(
                    shouldPause = false,
                    shouldNotify = false,
                    message = "",
                    canResume = true,
                    requiresReAuthentication = false
                )
            }
        }
    }
}

sealed class TimeoutState {
    object Idle : TimeoutState()
    data class Waiting(val deadline: Long) : TimeoutState()
    data class FinalWarning(val message: String) : TimeoutState()
    object TimedOut : TimeoutState()
}

data class TimeoutResponse(
    val shouldPause: Boolean,
    val shouldNotify: Boolean,
    val message: String,
    val canResume: Boolean,
    val requiresReAuthentication: Boolean
)
