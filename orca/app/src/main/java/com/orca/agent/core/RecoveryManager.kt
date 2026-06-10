package com.orca.agent.core

import com.orca.agent.brain.ConsciousMind
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Singleton
class RecoveryManager @Inject constructor(
    private val consciousMind: ConsciousMind
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Track recovery attempts to prevent loops
    private val recoveryHistory = mutableMapOf<String, MutableSet<String>>() // taskId -> set of tried actions
    private val recoveryCounts = mutableMapOf<String, Int>() // taskId -> total attempts
    private val maxRecoveryAttempts = 5
    private val maxSameActionAttempts = 2
    
    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()

    // ============================================================
    // SMART RECOVERY WITH DEDUPLICATION
    // ============================================================
    
    suspend fun attemptRecovery(
        taskId: String,
        nodeId: String,
        error: String,
        context: AgentContext,
        originalAction: AgentAction
    ): RecoveryResult {
        _recoveryState.value = RecoveryState.Attempting(taskId, 0)
        
        // Check if we've been trying too many times
        val totalAttempts = recoveryCounts.getOrDefault(taskId, 0)
        if (totalAttempts >= maxRecoveryAttempts) {
            _recoveryState.value = RecoveryState.Exhausted(taskId)
            return RecoveryResult.GiveUp(
                "Exceeded maximum recovery attempts ($maxRecoveryAttempts). " +
                "Task requires human intervention."
            )
        }
        
        // Get tried actions for this task
        val triedActions = recoveryHistory.getOrPut(taskId) { mutableSetOf() }
        
        // Generate recovery action
        val recoveryAction = consciousMind.recover(error, context)
        
        if (recoveryAction == null) {
            _recoveryState.value = RecoveryState.NoSolution(taskId)
            return RecoveryResult.GiveUp("Gemini could not suggest a recovery action.")
        }
        
        // Check for duplicate actions (prevent loops)
        val actionFingerprint = actionToFingerprint(recoveryAction)
        val timesTried = triedActions.count { it == actionFingerprint }
        
        if (timesTried >= maxSameActionAttempts) {
            // We've tried this exact action too many times
            // Force Gemini to try something different
            val forcedPrompt = """
                PREVIOUS RECOVERY ATTEMPTS FAILED:
                ${triedActions.joinToString("\n") { "- $it" }}
                
                The action "$actionFingerprint" has been tried $timesTried times and failed each time.
                
                DO NOT suggest that action again. Think of a COMPLETELY DIFFERENT approach.
                
                Original error: $error
                Current screen: ${context.currentScreen.describe()}
            """.trimIndent()
            
            val alternativeAction = consciousMind.recover(forcedPrompt, context)
            if (alternativeAction == null || triedActions.contains(actionToFingerprint(alternativeAction))) {
                _recoveryState.value = RecoveryState.Stuck(taskId)
                return RecoveryResult.GiveUp(
                    "All recovery actions exhausted. Tried: ${triedActions.joinToString()}"
                )
            }
            
            // Use the alternative
            triedActions.add(actionToFingerprint(alternativeAction))
            recoveryCounts[taskId] = totalAttempts + 1
            _recoveryState.value = RecoveryState.Attempting(taskId, totalAttempts + 1)
            return RecoveryResult.Action(alternativeAction, "Alternative recovery after deduplication")
        }
        
        // Validate coordinates before returning
        val validatedAction = validateCoordinates(recoveryAction, context.currentScreen)
        
        // Record this attempt
        triedActions.add(actionToFingerprint(validatedAction))
        recoveryCounts[taskId] = totalAttempts + 1
        _recoveryState.value = RecoveryState.Attempting(taskId, totalAttempts + 1)
        
        return RecoveryResult.Action(validatedAction, "Recovery attempt ${totalAttempts + 1}")
    }

    // ============================================================
    // COORDINATE VALIDATION
    // ============================================================
    
    private fun validateCoordinates(action: AgentAction, screen: ScreenState): AgentAction {
        return when (action) {
            is AgentAction.Tap -> {
                val validatedX = action.x.coerceIn(50, 1030) // Safe area bounds
                val validatedY = action.y.coerceIn(100, 2200) // Avoid status bar and nav
                
                if (validatedX != action.x || validatedY != action.y) {
                    AgentAction.Tap(
                        validatedX, validatedY,
                        "Corrected tap from (${action.x},${action.y}) to ($validatedX, $validatedY)"
                    )
                } else {
                    action
                }
            }
            is AgentAction.Swipe -> {
                val sx = action.startX.coerceIn(50, 1030)
                val sy = action.startY.coerceIn(100, 2200)
                val ex = action.endX.coerceIn(50, 1030)
                val ey = action.endY.coerceIn(100, 2200)
                action.copy(startX = sx, startY = sy, endX = ex, endY = ey)
            }
            else -> action
        }
    }

    // ============================================================
    // GEMINI API RETRY WITH EXPONENTIAL BACKOFF
    // ============================================================
    
    suspend fun callGeminiWithRetry(
        prompt: String,
        screenshot: String?,
        maxRetries: Int = 3
    ): String? {
        var delayMs = 1000L
        var lastError: String? = null
        
        for (attempt in 1..maxRetries) {
            try {
                return consciousMind.geminiApi.generateContent(
                    prompt = prompt,
                    imageBase64 = screenshot,
                    maxTokens = 1000
                )
            } catch (e: Exception) {
                lastError = e.message
                if (attempt < maxRetries) {
                    delay(delayMs)
                    delayMs *= 2 // Exponential backoff: 1s, 2s, 4s
                }
            }
        }
        
        // All retries failed
        _recoveryState.value = RecoveryState.ApiFailed(lastError ?: "Unknown API error")
        return null
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun actionToFingerprint(action: AgentAction): String {
        return when (action) {
            is AgentAction.Tap -> "tap(${action.x},${action.y})"
            is AgentAction.Swipe -> "swipe(${action.startX},${action.startY}->${action.endX},${action.endY})"
            is AgentAction.Type -> "type(${action.targetField})"
            is AgentAction.Back -> "back()"
            is AgentAction.Home -> "home()"
            is AgentAction.AppAction -> "open(${action.packageName})"
            is AgentAction.Wait -> "wait()"
            is AgentAction.LongPress -> "longpress(${action.x},${action.y})"
            else -> action.toString()
        }
    }
    
    fun resetTask(taskId: String) {
        recoveryHistory.remove(taskId)
        recoveryCounts.remove(taskId)
        _recoveryState.value = RecoveryState.Idle
    }
    
    fun getRecoverySummary(taskId: String): String {
        val attempts = recoveryCounts.getOrDefault(taskId, 0)
        val actions = recoveryHistory.getOrDefault(taskId, emptySet())
        return "Recovery attempts: $attempts, Unique actions tried: ${actions.size}"
    }
}

sealed class RecoveryState {
    object Idle : RecoveryState()
    data class Attempting(val taskId: String, val attempt: Int) : RecoveryState()
    data class Stuck(val taskId: String) : RecoveryState()
    data class Exhausted(val taskId: String) : RecoveryState()
    data class NoSolution(val taskId: String) : RecoveryState()
    data class ApiFailed(val error: String) : RecoveryState()
}

sealed class RecoveryResult {
    data class Action(val action: AgentAction, val description: String) : RecoveryResult()
    data class GiveUp(val reason: String) : RecoveryResult()
}
