package com.orca.agent.core

import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EthicalBoundaryGuard @Inject constructor() {
    private val _blockedActions = MutableSharedFlow<BlockedEthicalAction>(replay = 50)
    val blockedActions: SharedFlow<BlockedEthicalAction> = _blockedActions.asSharedFlow()
    private val _ethicalWarnings = MutableStateFlow(0)
    val ethicalWarnings: StateFlow<Int> = _ethicalWarnings.asStateFlow()

    fun evaluateAction(action: String): EthicalDecision {
        val lowerAction = action.lowercase()
        val violations = mutableListOf<EthicalViolation>()

        if (lowerAction.contains("delete all") || lowerAction.contains("wipe")) {
            violations.add(EthicalViolation("DATA_LOSS", "CRITICAL", "Action would permanently delete data."))
        }

        if (lowerAction.contains("password") || lowerAction.contains("credit card")) {
            violations.add(EthicalViolation("PRIVACY_VIOLATION", "CRITICAL", "Attempting to share sensitive information."))
        }

        if (violations.isNotEmpty()) {
            _ethicalWarnings.value++
            return EthicalDecision.Blocked(
                reason = violations.joinToString("; ") { it.description },
                violations = violations,
                explanation = "I cannot execute this action."
            )
        }

        return EthicalDecision.Allowed
    }
}

data class EthicalViolation(val category: String, val severity: String, val description: String)

sealed class EthicalDecision {
    object Allowed : EthicalDecision()
    data class Blocked(val reason: String, val violations: List<EthicalViolation>, val explanation: String) : EthicalDecision()
}

data class BlockedEthicalAction(val action: String, val violations: List<EthicalViolation>, val timestamp: Long)
