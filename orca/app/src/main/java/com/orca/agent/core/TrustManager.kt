package com.orca.agent.core

import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustManager @Inject constructor() {
    private val _trustScore = MutableStateFlow(0.5f)
    val trustScore: StateFlow<Float> = _trustScore.asStateFlow()
    private val _activityLog = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val activityLog: StateFlow<List<ActivityEntry>> = _activityLog.asStateFlow()

    fun recordAction(outcome: String) {
        val current = _trustScore.value
        val adjustment = when {
            outcome.contains("success") -> 0.02f
            outcome.contains("failure") -> -0.05f
            outcome.contains("corrected") -> -0.03f
            outcome.contains("praised") -> 0.05f
            else -> 0f
        }
        _trustScore.value = (current + adjustment).coerceIn(0f, 1f)
        _activityLog.value = _activityLog.value + ActivityEntry(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            description = outcome,
            trustAdjustment = adjustment
        )
    }

    fun getTrustScore(): Float = _trustScore.value

    fun getAutonomyLevel(): AutonomyLevel {
        return when {
            _trustScore.value > 0.9f -> AutonomyLevel.FULL_AUTONOMY
            _trustScore.value > 0.7f -> AutonomyLevel.SEMI_AUTONOMOUS
            _trustScore.value > 0.5f -> AutonomyLevel.CONFIRM_IMPORTANT
            _trustScore.value > 0.3f -> AutonomyLevel.CONFIRM_ALL
            else -> AutonomyLevel.MANUAL_ONLY
        }
    }
}

data class ActivityEntry(
    val id: String, val timestamp: Long,
    val description: String, val trustAdjustment: Float
)

enum class AutonomyLevel {
    FULL_AUTONOMY, SEMI_AUTONOMOUS, CONFIRM_IMPORTANT, CONFIRM_ALL, MANUAL_ONLY
}
