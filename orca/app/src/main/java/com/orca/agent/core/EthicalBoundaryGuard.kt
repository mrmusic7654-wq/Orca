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
    
    // ============================================================
    // ACTION EVALUATION
    // ============================================================
    
    fun evaluateAction(
        action: String,
        context: ActionContext
    ): EthicalDecision {
        val violations = mutableListOf<EthicalViolation>()
        
        // Check 1: Harm prevention
        val harmCheck = checkForHarm(action, context)
        if (harmCheck != null) violations.add(harmCheck)
        
        // Check 2: Privacy violation
        val privacyCheck = checkForPrivacyViolation(action, context)
        if (privacyCheck != null) violations.add(privacyCheck)
        
        // Check 3: Consent verification
        val consentCheck = checkForConsent(action, context)
        if (consentCheck != null) violations.add(consentCheck)
        
        // Check 4: Legality
        val legalityCheck = checkForIllegality(action, context)
        if (legalityCheck != null) violations.add(legalityCheck)
        
        // Check 5: Outdated information
        val outdatedCheck = checkForOutdatedInformation(action, context)
        if (outdatedCheck != null) violations.add(outdatedCheck)
        
        // Check 6: Mass action without confirmation
        val massActionCheck = checkForMassAction(action)
        if (massActionCheck != null) violations.add(massActionCheck)
        
        // Check 7: Self-harm or dangerous instructions
        val dangerCheck = checkForDangerousInstructions(action)
        if (dangerCheck != null) violations.add(dangerCheck)
        
        // Check 8: Harassment or stalking
        val harassmentCheck = checkForHarassment(action, context)
        if (harassmentCheck != null) violations.add(harassmentCheck)
        
        if (violations.isNotEmpty()) {
            _blockedActions.emit(
                BlockedEthicalAction(
                    action = action,
                    violations = violations,
                    timestamp = System.currentTimeMillis()
                )
            )
            _ethicalWarnings.value++
            
            return EthicalDecision.Blocked(
                reason = violations.joinToString("; ") { it.description },
                violations = violations,
                explanation = generateExplanation(violations)
            )
        }
        
        return EthicalDecision.Allowed
    }

    // ============================================================
    // HARM DETECTION
    // ============================================================
    
    private fun checkForHarm(action: String, context: ActionContext): EthicalViolation? {
        val lowerAction = action.lowercase()
        
        // Financial harm
        if ((lowerAction.contains("transfer") || lowerAction.contains("send money")) &&
            context.amount != null && context.amount > 500) {
            return EthicalViolation(
                category = "FINANCIAL_HARM",
                severity = "HIGH",
                description = "Large money transfer detected. Requires additional verification."
            )
        }
        
        // Reputational harm
        if (lowerAction.contains("post") && lowerAction.contains("all social") &&
            (lowerAction.contains("angry") || lowerAction.contains("offensive") ||
             lowerAction.contains("complaint"))) {
            return EthicalViolation(
                category = "REPUTATIONAL_HARM",
                severity = "MEDIUM",
                description = "Mass social media posting that could damage reputation."
            )
        }
        
        // Data loss
        if (lowerAction.contains("delete all") || lowerAction.contains("wipe") ||
            lowerAction.contains("format")) {
            return EthicalViolation(
                category = "DATA_LOSS",
                severity = "CRITICAL",
                description = "Action would permanently delete data."
            )
        }
        
        return null
    }

    // ============================================================
    // PRIVACY VIOLATION
    // ============================================================
    
    private fun checkForPrivacyViolation(action: String, context: ActionContext): EthicalViolation? {
        val lowerAction = action.lowercase()
        
        // Sharing personal data
        if (lowerAction.contains("share") || lowerAction.contains("send") ||
            lowerAction.contains("forward")) {
            if (lowerAction.contains("password") || lowerAction.contains("ssn") ||
                lowerAction.contains("credit card") || lowerAction.contains("social security")) {
                return EthicalViolation(
                    category = "PRIVACY_VIOLATION",
                    severity = "CRITICAL",
                    description = "Attempting to share sensitive personal information."
                )
            }
        }
        
        // Location tracking
        if (lowerAction.contains("track") && lowerAction.contains("location") &&
            !context.isConsented) {
            return EthicalViolation(
                category = "PRIVACY_VIOLATION",
                severity = "HIGH",
                description = "Location tracking without explicit consent."
            )
        }
        
        return null
    }

    // ============================================================
    // OUTDATED INFORMATION CHECK
    // ============================================================
    
    private fun checkForOutdatedInformation(action: String, context: ActionContext): EthicalViolation? {
        val lowerAction = action.lowercase()
        
        // Contact-related actions with potentially outdated info
        if (lowerAction.contains("call") || lowerAction.contains("message") ||
            lowerAction.contains("text") || lowerAction.contains("email")) {
            
            // Check if the contact has been marked as deceased or outdated
            if (context.contactStatus == "DECEASED") {
                return EthicalViolation(
                    category = "OUTDATED_INFORMATION",
                    severity = "CRITICAL",
                    description = "This contact may no longer be reachable. The action could cause distress."
                )
            }
            
            if (context.contactLastVerified != null &&
                System.currentTimeMillis() - context.contactLastVerified > 90 * 24 * 60 * 60 * 1000L) {
                return EthicalViolation(
                    category = "OUTDATED_INFORMATION",
                    severity = "LOW",
                    description = "Contact information hasn't been verified in over 90 days."
                )
            }
        }
        
        return null
    }

    // ============================================================
    // HARASSMENT DETECTION
    // ============================================================
    
    private fun checkForHarassment(action: String, context: ActionContext): EthicalViolation? {
        val lowerAction = action.lowercase()
        
        // Repeated contact
        if (context.recentContactCount != null && context.recentContactCount > 10 &&
            (lowerAction.contains("message") || lowerAction.contains("call"))) {
            return EthicalViolation(
                category = "POTENTIAL_HARASSMENT",
                severity = "HIGH",
                description = "High frequency of contact detected. This could be harassment."
            )
        }
        
        // Monitoring/surveillance
        if (lowerAction.contains("monitor") || lowerAction.contains("track") ||
            lowerAction.contains("watch") || lowerAction.contains("surveil")) {
            if (!context.isConsented) {
                return EthicalViolation(
                    category = "POTENTIAL_STALKING",
                    severity = "CRITICAL",
                    description = "Surveillance/monitoring without consent is illegal."
                )
            }
        }
        
        return null
    }

    // ============================================================
    // DANGEROUS INSTRUCTIONS
    // ============================================================
    
    private fun checkForDangerousInstructions(action: String): EthicalViolation? {
        val lowerAction = action.lowercase()
        
        // Self-harm related
        val selfHarmKeywords = listOf("hurt", "harm", "dangerous", "illegal", "weapon")
        for (keyword in selfHarmKeywords) {
            if (lowerAction.contains(keyword)) {
                return EthicalViolation(
                    category = "DANGEROUS_INSTRUCTION",
                    severity = "CRITICAL",
                    description = "This action could result in harm. Orca cannot execute it."
                )
            }
        }
        
        return null
    }

    // ============================================================
    // UTILITY CHECKS
    // ============================================================
    
    private fun checkForConsent(action: String, context: ActionContext): EthicalViolation? {
        if (!context.isConsented && context.requiresConsent) {
            return EthicalViolation(
                category = "NO_CONSENT",
                severity = "HIGH",
                description = "This action requires explicit user consent."
            )
        }
        return null
    }
    
    private fun checkForIllegality(action: String, context: ActionContext): EthicalViolation? {
        val lowerAction = action.lowercase()
        val illegalKeywords = listOf("pirate", "crack", "hack", "steal", "fraud")
        for (keyword in illegalKeywords) {
            if (lowerAction.contains(keyword)) {
                return EthicalViolation(
                    category = "POTENTIALLY_ILLEGAL",
                    severity = "CRITICAL",
                    description = "This action may be illegal. Orca cannot execute it."
                )
            }
        }
        return null
    }
    
    private fun checkForMassAction(action: String): EthicalViolation? {
        val lowerAction = action.lowercase()
        if ((lowerAction.contains("all") || lowerAction.contains("everyone") ||
             lowerAction.contains("every")) &&
            (lowerAction.contains("send") || lowerAction.contains("post") ||
             lowerAction.contains("share") || lowerAction.contains("delete"))) {
            return EthicalViolation(
                category = "MASS_ACTION",
                severity = "MEDIUM",
                description = "Mass action detected. Requires individual confirmation."
            )
        }
        return null
    }
    
    private fun generateExplanation(violations: List<EthicalViolation>): String {
        return buildString {
            append("I cannot execute this action because:\n\n")
            for ((index, violation) in violations.withIndex()) {
                append("${index + 1}. [${violation.severity}] ${violation.description}\n")
            }
            append("\nIf you believe this is an error, please rephrase your request.")
        }
    }
}

// ============================================================
// DATA CLASSES
// ============================================================

data class ActionContext(
    val amount: Double? = null,
    val isConsented: Boolean = false,
    val requiresConsent: Boolean = false,
    val contactStatus: String? = null,
    val contactLastVerified: Long? = null,
    val recentContactCount: Int? = null
)

data class EthicalViolation(
    val category: String,
    val severity: String,
    val description: String
)

sealed class EthicalDecision {
    object Allowed : EthicalDecision()
    data class Blocked(
        val reason: String,
        val violations: List<EthicalViolation>,
        val explanation: String
    ) : EthicalDecision()
}

data class BlockedEthicalAction(
    val action: String,
    val violations: List<EthicalViolation>,
    val timestamp: Long
)
