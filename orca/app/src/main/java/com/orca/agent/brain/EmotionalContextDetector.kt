package com.orca.agent.brain

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
    private val moodHistory = mutableListOf<MoodRecord>()

    fun detectMoodFromBehavior(
        recentActions: List<String>,
        typingSpeed: Float,
        backPressCount: Int,
        appSwitchCount: Int,
        timeOfDay: Int
    ): UserMood {
        var frustrationScore = 0f
        if (backPressCount > 5) frustrationScore += 0.4f
        if (appSwitchCount > 10) frustrationScore += 0.3f
        if (typingSpeed > 8f) frustrationScore += 0.2f
        if (timeOfDay in 23..24 || timeOfDay in 0..4) frustrationScore += 0.1f

        return when {
            frustrationScore > 0.6f -> UserMood.FRUSTRATED
            frustrationScore > 0.3f -> UserMood.SLIGHTLY_FRUSTRATED
            timeOfDay in 0..5 -> UserMood.TIRED
            else -> UserMood.NEUTRAL
        }
    }

    fun detect(): String {
        return _userMood.value.name
    }

    fun recordMood(mood: UserMood, context: String) {
        moodHistory.add(MoodRecord(mood, System.currentTimeMillis(), context))
        _userMood.value = mood
        if (moodHistory.size > 100) moodHistory.removeAt(0)
    }

    fun getMoodTrend(): String {
        if (moodHistory.size < 10) return "Not enough data"
        val recent = moodHistory.takeLast(20)
        val frustratedCount = recent.count { it.mood == UserMood.FRUSTRATED }
        return when {
            frustratedCount > 8 -> "User seems frequently frustrated"
            frustratedCount > 4 -> "User shows occasional frustration"
            else -> "Mood varies normally"
        }
    }
}

enum class UserMood { NEUTRAL, SLIGHTLY_FRUSTRATED, FRUSTRATED, URGENT, TIRED, HAPPY }
data class MoodRecord(val mood: UserMood, val timestamp: Long, val context: String)
