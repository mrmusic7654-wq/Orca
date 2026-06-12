package com.orca.agent.core

import com.orca.agent.brain.ConsciousMind
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryManager @Inject constructor(
    private val consciousMind: ConsciousMind
) {
    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()
    private val recoveryHistory = mutableMapOf<String, MutableSet<String>>()
    private val recoveryCounts = mutableMapOf<String, Int>()
    private val maxRecoveryAttempts = 5

    suspend fun attemptRecovery(taskId: String, nodeId: String, error: String, context: AgentContext): RecoveryResult {
        val totalAttempts = recoveryCounts.getOrDefault(taskId, 0)
        if (totalAttempts >= maxRecoveryAttempts) {
            return RecoveryResult.GiveUp("Max attempts exceeded")
        }

        val recoveryAction = consciousMind.recover(error, context)
        if (recoveryAction == null) {
            return RecoveryResult.GiveUp("No recovery action found")
        }

        recoveryCounts[taskId] = totalAttempts + 1
        return RecoveryResult.Action(recoveryAction, "Recovery attempt ${totalAttempts + 1}")
    }

    fun resetTask(taskId: String) {
        recoveryHistory.remove(taskId)
        recoveryCounts.remove(taskId)
    }
}

sealed class RecoveryState {
    object Idle : RecoveryState()
    data class Attempting(val taskId: String, val attempt: Int) : RecoveryState()
    data class Exhausted(val taskId: String) : RecoveryState()
}

sealed class RecoveryResult {
    data class Action(val action: AgentAction, val description: String) : RecoveryResult()
    data class GiveUp(val reason: String) : RecoveryResult()
}
