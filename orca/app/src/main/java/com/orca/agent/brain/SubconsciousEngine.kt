package com.orca.agent.brain

import android.app.Notification
import com.orca.agent.core.ScreenState
import com.orca.agent.memory.MemoryCortex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

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
    
    private val observationBuffer = mutableListOf<ScreenObservation>()
    private val patternBuffer = mutableListOf<BehaviorPattern>()

    fun initialize() {
        intentPredictor.initialize()
        threatDetector.initialize()
    }

    // ============================================================
    // CONTINUOUS MONITORING
    // ============================================================
    
    fun enableContinuousMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        scope.launch {
            while (isActive && isMonitoring) {
                val currentScreen = ScreenState.capture()
                
                if (!currentScreen.isEmpty()) {
                    // Predict user's next action
                    _predictions.value = intentPredictor.predict(currentScreen)
                    
                    // Scan for threats
                    _threats.value = threatDetector.scan(currentScreen)
                    
                    // Record observation
                    recordObservation(currentScreen)
                }
                
                delay(5000) // Check every 5 seconds (battery efficient)
            }
        }
    }

    fun disableContinuousMonitoring() {
        isMonitoring = false
    }

    // ============================================================
    // PASSIVE OBSERVATION
    // ============================================================
    
    fun recordObservation(screen: ScreenState) {
        observationBuffer.add(
            ScreenObservation(
                screen = screen,
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Keep buffer manageable
        if (observationBuffer.size > 1000) {
            observationBuffer.removeAt(0)
        }
    }

    // ============================================================
    // PATTERN EXTRACTION
    // ============================================================
    
    suspend fun extractPatterns(): List<BehaviorPattern> {
        if (observationBuffer.size < 50) return emptyList()
        
        val patterns = mutableListOf<BehaviorPattern>()
        
        // Extract time-based patterns
        val timePatterns = extractTimePatterns()
        patterns.addAll(timePatterns)
        
        // Extract app sequence patterns
        val appPatterns = extractAppSequencePatterns()
        patterns.addAll(appPatterns)
        
        patternBuffer.addAll(patterns)
        return patterns
    }
    
    private fun extractTimePatterns(): List<BehaviorPattern> {
        val patterns = mutableListOf<BehaviorPattern>()
        val hourGroups = observationBuffer.groupBy {
            java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
                .get(java.util.Calendar.HOUR_OF_DAY)
        }
        
        for ((hour, observations) in hourGroups) {
            val appCounts = observations.groupBy { it.screen.currentApp }
                .mapValues { it.value.size }
                .filter { it.value >= 3 } // At least 3 occurrences
            
            for ((app, count) in appCounts) {
                patterns.add(
                    BehaviorPattern(
                        description = "Opens $app at $hour:00",
                        frequency = count,
                        confidence = count.toFloat() / observations.size.toFloat(),
                        timeOfDay = "$hour:00",
                        appPackage = app,
                        actionSequence = emptyList()
                    )
                )
            }
        }
        
        return patterns
    }
    
    private fun extractAppSequencePatterns(): List<BehaviorPattern> {
        val patterns = mutableListOf<BehaviorPattern>()
        val sequences = mutableListOf<List<String>>()
        
        // Build sequences of consecutive app switches
        var currentSequence = mutableListOf<String>()
        for (i in 0 until observationBuffer.size - 1) {
            val current = observationBuffer[i].screen.currentApp
            val next = observationBuffer[i + 1].screen.currentApp
            
            if (current != next && current.isNotEmpty() && next.isNotEmpty()) {
                if (currentSequence.isEmpty()) {
                    currentSequence.add(current)
                }
                currentSequence.add(next)
            } else if (currentSequence.isNotEmpty()) {
                sequences.add(currentSequence.toList())
                currentSequence.clear()
            }
        }
        
        // Find repeated sequences
        val sequenceCounts = sequences.groupBy { it }.mapValues { it.value.size }
        for ((seq, count) in sequenceCounts) {
            if (count >= 3 && seq.size >= 2) {
                patterns.add(
                    BehaviorPattern(
                        description = "App sequence: ${seq.joinToString(" → ")}",
                        frequency = count,
                        confidence = 0.7f,
                        timeOfDay = null,
                        appPackage = seq.last(),
                        actionSequence = seq
                    )
                )
            }
        }
        
        return patterns
    }

    // ============================================================
    // NOTIFICATION HANDLING
    // ============================================================
    
    fun onNotificationPosted(notification: Notification) {
        scope.launch {
            val importance = intentPredictor.evaluateNotificationImportance(notification)
            if (importance > 0.8f) {
                memoryCortex.recordEvent(
                    Event.Notification(
                        title = notification.extras.getString(Notification.EXTRA_TITLE) ?: "",
                        text = notification.extras.getString(Notification.EXTRA_TEXT) ?: "",
                        importance = importance
                    )
                )
            }
        }
    }
}

data class ScreenObservation(
    val screen: ScreenState,
    val timestamp: Long
)

// Event sealed class
sealed class Event {
    abstract val description: String
    abstract val timestamp: Long
    
    data class Notification(
        val title: String,
        val text: String,
        val importance: Float,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Event() {
        override val description: String get() = "Notification: $title"
    }
    
    data class TaskCompleted(
        val taskName: String,
        val duration: Long,
        val success: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Event() {
        override val description: String get() = "Task: $taskName (${if (success) "success" else "failed"})"
    }
}
