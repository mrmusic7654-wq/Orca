package com.orca.agent.brain

import android.app.Notification
import com.orca.agent.core.ScreenState
import com.orca.agent.memory.MemoryCortex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

data class PredictedIntent(
    val action: String,
    val probability: Float,
    val context: String
)

@Singleton
class SubconsciousEngine @Inject constructor(
    private val intentPredictor: IntentPredictor,
    private val threatDetector: ThreatDetector,
    private val memoryCortex: MemoryCortex
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isMonitoring = false

    private val _predictions = MutableStateFlow<List<PredictedIntent>>(emptyList())
    val predictions: StateFlow<List<PredictedIntent>> = _predictions.asStateFlow()

    private val _threats = MutableStateFlow<List<ThreatAlert>>(emptyList())
    val threats: StateFlow<List<ThreatAlert>> = _threats.asStateFlow()

    companion object {
        @Volatile private var INSTANCE: SubconsciousEngine? = null
        fun getInstance(): SubconsciousEngine = INSTANCE 
            ?: throw IllegalStateException("SubconsciousEngine not initialized")
        fun setInstance(instance: SubconsciousEngine) { INSTANCE = instance }
    }

    fun initialize() {
        intentPredictor.initialize()
        threatDetector.initialize()
    }

    fun enableContinuousMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        scope.launch {
            while (isActive && isMonitoring) {
                val screen = ScreenState.capture()
                _predictions.value = intentPredictor.predict(screen)
                _threats.value = threatDetector.scan(screen)
                delay(5000)
            }
        }
    }

    fun disableContinuousMonitoring() {
        isMonitoring = false
    }

    fun onNotificationPosted(notification: Notification) {
        scope.launch {
            val importance = intentPredictor.evaluateNotificationImportance(notification)
            if (importance > 0.8f) {
                memoryCortex.recordEvent("Important notification: ${notification.extras.getString(Notification.EXTRA_TITLE)}")
            }
        }
    }
}
