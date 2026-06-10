package com.orca.agent.brain

import com.orca.agent.core.ScreenState
import com.orca.agent.data.network.GeminiApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmotionalContextDetector @Inject constructor(
    private val geminiApi: GeminiApi
) {
    private val _userMood = MutableStateFlow(UserMood.NEUTRAL)
    val userMood: StateFlow<UserMood> = _userMood.asStateFlow()
    
    private val moodHistory = mutableListOf<MoodRecord>(100)
    
    private val frustrationIndicators = listOf(
        "rapid tapping",
        "repeated back button",
        "app switching",
        "force close",
        "keyboard smashing"
    )
    
    // ============================================================
    // MOOD DETECTION FROM BEHAVIOR
    // ============================================================
    
    fun detectMoodFromBehavior(
        recentActions: List<String>,
        typingSpeed: Float, // characters per second
        backPressCount: Int,
        appSwitchCount: Int,
        timeOfDay: Int // hour
    ): UserMood {
        var frustrationScore = 0f
        var urgencyScore = 0f
        
        // Rapid back button = frustration
        if (backPressCount > 5) {
            frustrationScore += 0.4f
        }
        
        // Rapid app switching = frustration or urgency
        if (appSwitchCount > 10) {
            frustrationScore += 0.3f
            urgencyScore += 0.3f
        }
        
        // Very fast typing = urgency or anger
        if (typingSpeed > 5f) {
            urgencyScore += 0.3f
            if (typingSpeed > 8f) {
                frustrationScore += 0.2f
            }
        }
        
        // Late night = tired
        if (timeOfDay in 23..24 || timeOfDay in 0..4) {
            frustrationScore += 0.1f // More easily frustrated when tired
        }
        
        return when {
            frustrationScore > 0.6f -> UserMood.FRUSTRATED
            urgencyScore > 0.6f -> UserMood.URGENT
            frustrationScore > 0.3f -> UserMood.SLIGHTLY_FRUSTRATED
            timeOfDay in 23..24 || timeOfDay in 0..5 -> UserMood.TIRED
            else -> UserMood.NEUTRAL
        }
    }

    // ============================================================
    // ADAPTIVE BEHAVIOR BASED ON MOOD
    // ============================================================
    
    fun getAdaptiveResponse(mood: UserMood, originalMessage: String): AdaptiveResponse {
        return when (mood) {
            UserMood.FRUSTRATED -> AdaptiveResponse(
                shouldSimplify = true,
                shouldBeQuiet = true,
                shouldAvoidQuestions = true,
                modifiedTone = "calm and minimal",
                suggestedResponse = "Got it. $originalMessage - I'll handle this quietly.",
                maxStepsWithoutConfirmation = 10 // Don't bother user
            )
            UserMood.URGENT -> AdaptiveResponse(
                shouldSimplify = true,
                shouldBeQuiet = false,
                shouldAvoidQuestions = false,
                modifiedTone = "fast and direct",
                suggestedResponse = "On it. $originalMessage - I'll be quick.",
                maxStepsWithoutConfirmation = 15 // Speed over caution
            )
            UserMood.TIRED -> AdaptiveResponse(
                shouldSimplify = true,
                shouldBeQuiet = true,
                shouldAvoidQuestions = true,
                modifiedTone = "gentle and supportive",
                suggestedResponse = "I've got this. Rest while I handle $originalMessage.",
                maxStepsWithoutConfirmation = 8
            )
            UserMood.NEUTRAL -> AdaptiveResponse(
                shouldSimplify = false,
                shouldBeQuiet = false,
                shouldAvoidQuestions = false,
                modifiedTone = "normal",
                suggestedResponse = null,
                maxStepsWithoutConfirmation = 3
            )
            UserMood.SLIGHTLY_FRUSTRATED -> AdaptiveResponse(
                shouldSimplify = true,
                shouldBeQuiet = false,
                shouldAvoidQuestions = false,
                modifiedTone = "patient and helpful",
                suggestedResponse = null,
                maxStepsWithoutConfirmation = 5
            )
        }
    }

    // ============================================================
    // MOOD TRACKING
    // ============================================================
    
    fun recordMood(mood: UserMood, context: String) {
        moodHistory.add(
            MoodRecord(
                mood = mood,
                timestamp = System.currentTimeMillis(),
                context = context
            )
        )
        _userMood.value = mood
    }
    
    fun getMoodTrend(): String {
        if (moodHistory.size < 10) return "Not enough data"
        
        val recent = moodHistory.takeLast(20)
        val frustratedCount = recent.count { it.mood == UserMood.FRUSTRATED }
        
        return when {
            frustratedCount > 8 -> "User seems frequently frustrated. Consider simplifying interactions."
            frustratedCount > 4 -> "User shows occasional frustration. Monitor closely."
            recent.all { it.mood == UserMood.NEUTRAL } -> "User appears satisfied with interactions."
            else -> "Mood varies normally."
        }
    }
}

enum class UserMood {
    NEUTRAL,
    SLIGHTLY_FRUSTRATED,
    FRUSTRATED,
    URGENT,
    TIRED,
    HAPPY
}

data class MoodRecord(
    val mood: UserMood,
    val timestamp: Long,
    val context: String
)

data class AdaptiveResponse(
    val shouldSimplify: Boolean,
    val shouldBeQuiet: Boolean,
    val shouldAvoidQuestions: Boolean,
    val modifiedTone: String,
    val suggestedResponse: String?,
    val maxStepsWithoutConfirmation: Int
)
