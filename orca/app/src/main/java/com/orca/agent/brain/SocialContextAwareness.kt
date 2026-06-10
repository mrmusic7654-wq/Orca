package com.orca.agent.brain

import android.content.Context
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialContextAwareness @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _socialContext = MutableStateFlow(SocialContext())
    val socialContext: StateFlow<SocialContext> = _socialContext.asStateFlow()
    
    private val _privacyMode = MutableStateFlow(PrivacyMode.NORMAL)
    val privacyMode: StateFlow<PrivacyMode> = _privacyMode.asStateFlow()
    
    private val _deadManSwitch = MutableStateFlow(DeadManSwitchState())
    val deadManSwitch: StateFlow<DeadManSwitchState> = _deadManSwitch.asStateFlow()
    
    // User activity tracking for life detection
    private var lastUserInteraction = System.currentTimeMillis()
    private var consecutiveNoInteractionDays = 0
    private var silentCorrectionDetected = false

    // ============================================================
    // SOCIAL CONTEXT DETECTION
    // ============================================================
    
    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val context = detectSocialContext()
                _socialContext.value = context
                
                // Adjust privacy mode based on context
                _privacyMode.value = when {
                    context.isInMeeting -> PrivacyMode.SILENT
                    context.isInPublic -> PrivacyMode.DISCREET
                    context.screenIsVisible -> PrivacyMode.VISIBLE_SCREEN
                    context.isAlone -> PrivacyMode.NORMAL
                    else -> PrivacyMode.NORMAL
                }
                
                // Check dead man's switch
                checkUserAlive()
                
                // Check for silent corrections
                checkForSilentCorrections()
                
                delay(30000) // Every 30 seconds
            }
        }
    }

    // ============================================================
    // CONTEXT DETECTION
    // ============================================================
    
    private fun detectSocialContext(): SocialContext {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        
        // Detect meeting context
        val isInMeeting = isLikelyInMeeting(calendar)
        
        // Detect public context
        val isInPublic = isLikelyInPublic()
        
        // Detect if screen is visible to others
        val screenIsVisible = isScreenLikelyVisible()
        
        // Detect if alone
        val isAlone = isLikelyAlone(hour)
        
        // Detect sensitive location
        val isSensitiveLocation = isSensitiveLocation()
        
        return SocialContext(
            isInMeeting = isInMeeting,
            isInPublic = isInPublic,
            screenIsVisible = screenIsVisible,
            isAlone = isAlone,
            isSensitiveLocation = isSensitiveLocation,
            hourOfDay = hour,
            dayOfWeek = dayOfWeek,
            ambientNoise = detectAmbientNoise()
        )
    }

    // ============================================================
    // DEAD MAN'S SWITCH
    // ============================================================
    
    fun recordUserInteraction() {
        lastUserInteraction = System.currentTimeMillis()
        consecutiveNoInteractionDays = 0
    }
    
    private fun checkUserAlive() {
        val hoursSinceInteraction = 
            (System.currentTimeMillis() - lastUserInteraction) / (1000 * 60 * 60)
        
        val daysSinceInteraction = hoursSinceInteraction / 24
        
        if (daysSinceInteraction > consecutiveNoInteractionDays) {
            consecutiveNoInteractionDays = daysSinceInteraction.toInt()
        }
        
        val state = when {
            consecutiveNoInteractionDays >= 14 -> DeadManSwitchState.EMERGENCY_SHUTDOWN
            consecutiveNoInteractionDays >= 7 -> DeadManSwitchState.CRITICAL_WARNING
            consecutiveNoInteractionDays >= 3 -> DeadManSwitchState.WARNING
            consecutiveNoInteractionDays >= 1 -> DeadManSwitchState.NO_RECENT_ACTIVITY
            else -> DeadManSwitchState.ACTIVE
        }
        
        if (state != _deadManSwitch.value.state) {
            _deadManSwitch.value = DeadManSwitchState(state)
            
            when (state) {
                DeadManSwitchState.CRITICAL_WARNING -> {
                    // Show notification: "Orca hasn't detected activity in 7 days."
                }
                DeadManSwitchState.EMERGENCY_SHUTDOWN -> {
                    // STOP ALL ACTIONS
                    // Save state
                    // Show final notification
                    // Enter dormant mode
                    initiateDeadManShutdown()
                }
                else -> {}
            }
        }
    }
    
    private fun initiateDeadManShutdown() {
        // Stop all autonomous actions
        // Stop all scheduled tasks
        // Stop all background processing
        // Show persistent notification explaining how to reactivate
        // Wait for explicit user reactivation
    }

    // ============================================================
    // SILENT CORRECTION DETECTION
    // ============================================================
    
    private fun checkForSilentCorrections() {
        // Monitor for patterns like:
        // 1. Orca books flight → User cancels flight → User rebooks different flight
        // 2. Orca sends message → User deletes message → User sends different message
        // 3. Orca adds item to cart → User removes item → User adds different item
        
        // These patterns indicate silent corrections
        // The agent should notice and say: "I noticed you changed the flight I booked.
        // Would you like me to learn your preference for next time?"
    }

    // ============================================================
    // PRIVACY-AWARE BEHAVIOR
    // ============================================================
    
    fun shouldSpeakAloud(): Boolean {
        return when (_privacyMode.value) {
            PrivacyMode.SILENT -> false
            PrivacyMode.DISCREET -> false
            PrivacyMode.VISIBLE_SCREEN -> false
            PrivacyMode.NORMAL -> true
        }
    }
    
    fun shouldShowNotification(): Boolean {
        return when (_privacyMode.value) {
            PrivacyMode.SILENT -> false
            PrivacyMode.DISCREET -> true // Use silent notification
            PrivacyMode.VISIBLE_SCREEN -> false // Don't show on visible screen
            PrivacyMode.NORMAL -> true
        }
    }
    
    fun getAppropriateVolume(): Float {
        return when (_privacyMode.value) {
            PrivacyMode.SILENT -> 0f
            PrivacyMode.DISCREET -> 0.3f
            PrivacyMode.NORMAL -> 0.7f
            PrivacyMode.VISIBLE_SCREEN -> 0f
        }
    }

    // ============================================================
    // CROSS-PURPOSE PREVENTION
    // ============================================================
    
    fun validateGoalSafety(goal: String): GoalSafetyAssessment {
        val dangerousPatterns = listOf(
            "cancel all" to "This could cancel important services",
            "delete all" to "This would permanently delete data",
            "send to everyone" to "This would share with all contacts",
            "buy the cheapest" to "This might compromise quality",
            "save money" to "This is vague—could have unintended consequences",
            "optimize" to "Optimizing without constraints can be destructive"
        )
        
        val warnings = mutableListOf<String>()
        for ((pattern, warning) in dangerousPatterns) {
            if (goal.contains(pattern, ignoreCase = true)) {
                warnings.add(warning)
            }
        }
        
        return GoalSafetyAssessment(
            isSafe = warnings.isEmpty(),
            warnings = warnings,
            requiresConfirmation = warnings.isNotEmpty(),
            suggestedClarification = if (warnings.isNotEmpty()) {
                "This goal could have unintended consequences. Could you be more specific?"
            } else null
        )
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun isLikelyInMeeting(calendar: java.util.Calendar): Boolean {
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val isWeekday = dayOfWeek in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
        val isBusinessHours = hour in 9..17
        return isWeekday && isBusinessHours
    }
    
    private fun isLikelyInPublic(): Boolean {
        // Check if connected to public WiFi
        // Check location context
        // Check ambient noise
        return false
    }
    
    private fun isScreenLikelyVisible(): Boolean {
        // Check if phone is face-up on a table
        // Check ambient light
        // Check if casting/screen sharing
        return false
    }
    
    private fun isLikelyAlone(hour: Int): Boolean {
        return hour in 22..23 || hour in 0..5
    }
    
    private fun isSensitiveLocation(): Boolean {
        // Check if at doctor's office, bank, etc.
        return false
    }
    
    private fun detectAmbientNoise(): Float {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // This requires RECORD_AUDIO permission and is approximate
            0.5f
        } catch (e: Exception) { 0.5f }
    }
}

data class SocialContext(
    val isInMeeting: Boolean = false,
    val isInPublic: Boolean = false,
    val screenIsVisible: Boolean = false,
    val isAlone: Boolean = true,
    val isSensitiveLocation: Boolean = false,
    val hourOfDay: Int = 0,
    val dayOfWeek: Int = 0,
    val ambientNoise: Float = 0f
)

enum class PrivacyMode {
    NORMAL,
    DISCREET,
    SILENT,
    VISIBLE_SCREEN
}

data class DeadManSwitchState(
    val state: String = "ACTIVE"
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val NO_RECENT_ACTIVITY = "NO_RECENT_ACTIVITY"
        const val WARNING = "WARNING"
        const val CRITICAL_WARNING = "CRITICAL_WARNING"
        const val EMERGENCY_SHUTDOWN = "EMERGENCY_SHUTDOWN"
    }
}

data class GoalSafetyAssessment(
    val isSafe: Boolean,
    val warnings: List<String>,
    val requiresConfirmation: Boolean,
    val suggestedClarification: String?
)
