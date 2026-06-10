package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustManager @Inject constructor() {
    
    private val _trustScore = MutableStateFlow(TrustScore())
    val trustScore: StateFlow<TrustScore> = _trustScore.asStateFlow()
    
    private val _activityLog = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val activityLog: StateFlow<List<ActivityEntry>> = _activityLog.asStateFlow()
    
    private val _pendingUndoActions = MutableStateFlow<List<UndoAction>>(emptyList())
    val pendingUndoActions: StateFlow<List<UndoAction>> = _pendingUndoActions.asStateFlow()
    
    // Trust calculation weights
    private val successWeight = 0.4f
    private val userCorrectionWeight = -0.3f
    private val costMistakeWeight = -0.6f
    private val consistencyWeight = 0.2f
    private val transparencyWeight = 0.1f

    // ============================================================
    // TRUST SCORE CALCULATION
    // ============================================================
    
    fun recordAction(outcome: ActionOutcome) {
        val current = _trustScore.value
        var adjustment = 0f
        var reason = ""
        
        when (outcome) {
            is ActionOutcome.Success -> {
                adjustment = 0.02f
                reason = "Task completed successfully"
            }
            is ActionOutcome.SuccessWithCorrection -> {
                adjustment = 0.01f
                reason = "Task completed with minor correction"
            }
            is ActionOutcome.UserCorrected -> {
                adjustment = -0.05f
                reason = "User had to correct the action"
            }
            is ActionOutcome.UserCancelled -> {
                adjustment = -0.08f
                reason = "User cancelled the task"
            }
            is ActionOutcome.CostlyMistake -> {
                adjustment = -0.15f
                reason = "Action resulted in an undesirable outcome (${outcome.description})"
            }
            is ActionOutcome.RepeatedMistake -> {
                adjustment = -0.10f
                reason = "Repeated the same mistake (${outcome.count} times)"
            }
            is ActionOutcome.UserPraised -> {
                adjustment = 0.05f
                reason = "User explicitly praised the action"
            }
        }
        
        val newScore = (current.overallScore + adjustment).coerceIn(0f, 1f)
        
        // Calculate sub-scores
        val recentOutcomes = getRecentOutcomes(50)
        val reliabilityScore = calculateReliability(recentOutcomes)
        val consistencyScore = calculateConsistency(recentOutcomes)
        val transparencyScore = calculateTransparency()
        
        _trustScore.value = TrustScore(
            overallScore = newScore,
            reliabilityScore = reliabilityScore,
            consistencyScore = consistencyScore,
            transparencyScore = transparencyScore,
            totalActions = current.totalActions + 1,
            successfulActions = current.successfulActions + if (adjustment > 0) 1 else 0,
            lastUpdated = System.currentTimeMillis()
        )
        
        // Log the activity
        logActivity(outcome, adjustment, reason)
        
        // Track for undo
        if (outcome is ActionOutcome.CostlyMistake) {
            addUndoAction(outcome)
        }
    }

    // ============================================================
    // REPEATED MISTAKE DETECTION
    // ============================================================
    
    fun detectRepeatedMistakes(): List<RepeatedMistake> {
        val recentFailures = getRecentOutcomes(100)
            .filter { it is ActionOutcome.UserCorrected || it is ActionOutcome.CostlyMistake }
        
        val grouped = recentFailures.groupBy { it.description }
        
        return grouped
            .filter { it.value.size >= 3 } // Same mistake 3+ times
            .map { (description, outcomes) ->
                RepeatedMistake(
                    description = description,
                    count = outcomes.size,
                    lastOccurrence = outcomes.last().timestamp,
                    recommendedFix = generateFix(description)
                )
            }
    }
    
    private fun generateFix(description: String): String {
        return when {
            description.contains("button", ignoreCase = true) -> 
                "Update cached button coordinates or add visual verification"
            description.contains("login", ignoreCase = true) -> 
                "Check if stored credentials are still valid"
            description.contains("payment", ignoreCase = true) -> 
                "Add additional confirmation step before payment"
            else -> "Review and update the workflow for this task"
        }
    }

    // ============================================================
    // TRANSPARENCY SYSTEM
    // ============================================================
    
    fun generateActivitySummary(): String {
        val recent = _activityLog.value.takeLast(20)
        
        if (recent.isEmpty()) return "No recent activity."
        
        val summary = buildString {
            append("ORCA ACTIVITY SUMMARY\n")
            append("═══════════════════\n\n")
            
            for (entry in recent.reversed()) {
                val icon = when {
                    entry.outcome is ActionOutcome.Success -> "✅"
                    entry.outcome is ActionOutcome.UserCorrected -> "⚠️"
                    entry.outcome is ActionOutcome.CostlyMistake -> "❌"
                    entry.outcome is ActionOutcome.UserPraised -> "🌟"
                    else -> "ℹ️"
                }
                append("$icon ${formatTimestamp(entry.timestamp)}: ${entry.description}\n")
            }
            
            append("\nTrust Score: ${(_trustScore.value.overallScore * 100).toInt()}%\n")
        }
        
        return summary
    }
    
    fun explainLastAction(): String {
        val lastEntry = _activityLog.value.lastOrNull()
            ?: return "No actions have been performed yet."
        
        return """
            Last Action: ${lastEntry.description}
            Outcome: ${lastEntry.outcome.javaClass.simpleName}
            Time: ${formatTimestamp(lastEntry.timestamp)}
            Trust Impact: ${if (lastEntry.trustAdjustment > 0) "+" else ""}${lastEntry.trustAdjustment}
            Reason: ${lastEntry.reason}
        """.trimIndent()
    }

    // ============================================================
    // UNDO SYSTEM
    // ============================================================
    
    private fun addUndoAction(outcome: ActionOutcome.CostlyMistake) {
        val undoActions = _pendingUndoActions.value.toMutableList()
        undoActions.add(
            UndoAction(
                id = java.util.UUID.randomUUID().toString(),
                description = outcome.description,
                timestamp = outcome.timestamp,
                canUndo = outcome.reversible,
                undoSteps = outcome.undoSteps
            )
        )
        _pendingUndoActions.value = undoActions.take(50)
    }
    
    fun getUndoActions(): List<UndoAction> {
        return _pendingUndoActions.value.filter { it.canUndo }
    }
    
    fun markUndone(actionId: String) {
        _pendingUndoActions.value = _pendingUndoActions.value.map {
            if (it.id == actionId) it.copy(isUndone = true) else it
        }
    }

    // ============================================================
    // TRUST-BASED BEHAVIOR
    // ============================================================
    
    fun getAutonomyLevel(): AutonomyLevel {
        val score = _trustScore.value.overallScore
        return when {
            score > 0.9f -> AutonomyLevel.FULL_AUTONOMY
            score > 0.7f -> AutonomyLevel.SEMI_AUTONOMOUS
            score > 0.5f -> AutonomyLevel.CONFIRM_IMPORTANT
            score > 0.3f -> AutonomyLevel.CONFIRM_ALL
            else -> AutonomyLevel.MANUAL_ONLY
        }
    }
    
    fun shouldSkipConfirmation(riskLevel: RiskLevel): Boolean {
        val level = getAutonomyLevel()
        return when (level) {
            AutonomyLevel.FULL_AUTONOMY -> riskLevel != RiskLevel.CRITICAL
            AutonomyLevel.SEMI_AUTONOMOUS -> riskLevel == RiskLevel.LOW
            AutonomyLevel.CONFIRM_IMPORTANT -> false
            else -> false
        }
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun logActivity(outcome: ActionOutcome, adjustment: Float, reason: String) {
        val log = _activityLog.value.toMutableList()
        log.add(
            ActivityEntry(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                description = outcome.description,
                outcome = outcome,
                trustAdjustment = adjustment,
                reason = reason
            )
        )
        _activityLog.value = log.takeLast(500)
    }
    
    private fun getRecentOutcomes(n: Int): List<ActionOutcome> {
        return _activityLog.value.takeLast(n).map { it.outcome }
    }
    
    private fun calculateReliability(outcomes: List<ActionOutcome>): Float {
        if (outcomes.isEmpty()) return 0.5f
        val successes = outcomes.count { it is ActionOutcome.Success || it is ActionOutcome.SuccessWithCorrection }
        return successes.toFloat() / outcomes.size.toFloat()
    }
    
    private fun calculateConsistency(outcomes: List<ActionOutcome>): Float {
        if (outcomes.size < 10) return 0.5f
        // Check if outcomes are consistent or erratic
        val changes = outcomes.windowed(2).count { 
            it[0].javaClass != it[1].javaClass 
        }
        return 1f - (changes.toFloat() / outcomes.size.toFloat())
    }
    
    private fun calculateTransparency(): Float {
        val recentLogs = _activityLog.value.size
        return if (recentLogs > 10) 1.0f else recentLogs / 10f
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}

// ============================================================
// DATA CLASSES
// ============================================================

data class TrustScore(
    val overallScore: Float = 0.5f,
    val reliabilityScore: Float = 0.5f,
    val consistencyScore: Float = 0.5f,
    val transparencyScore: Float = 0.5f,
    val totalActions: Int = 0,
    val successfulActions: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

sealed class ActionOutcome {
    abstract val description: String
    abstract val timestamp: Long
    
    data class Success(
        override val description: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ActionOutcome()
    
    data class SuccessWithCorrection(
        override val description: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ActionOutcome()
    
    data class UserCorrected(
        override val description: String,
        val userAction: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ActionOutcome()
    
    data class UserCancelled(
        override val description: String,
        val reason: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ActionOutcome()
    
    data class CostlyMistake(
        override val description: String,
        val reversible: Boolean,
        val undoSteps: List<String>,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ActionOutcome()
    
    data class RepeatedMistake(
        override val description: String,
        val count: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ActionOutcome()
    
    data class UserPraised(
        override val description: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ActionOutcome()
}

data class ActivityEntry(
    val id: String,
    val timestamp: Long,
    val description: String,
    val outcome: ActionOutcome,
    val trustAdjustment: Float,
    val reason: String
)

data class UndoAction(
    val id: String,
    val description: String,
    val timestamp: Long,
    val canUndo: Boolean,
    val undoSteps: List<String>,
    val isUndone: Boolean = false
)

data class RepeatedMistake(
    val description: String,
    val count: Int,
    val lastOccurrence: Long,
    val recommendedFix: String
)

enum class AutonomyLevel {
    FULL_AUTONOMY,
    SEMI_AUTONOMOUS,
    CONFIRM_IMPORTANT,
    CONFIRM_ALL,
    MANUAL_ONLY
}
