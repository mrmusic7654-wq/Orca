package com.orca.agent.brain

import com.orca.agent.core.ScreenState
import javax.inject.Inject
import javax.inject.Singleton

data class ThreatAlert(
    val severity: ThreatSeverity, val description: String,
    val source: String, val recommendedAction: String = ""
)

enum class ThreatSeverity { LOW, MEDIUM, HIGH, CRITICAL }

@Singleton
class ThreatDetector @Inject constructor() {
    private val knownPhishingPatterns = listOf("verify.*account.*now", "click.*here.*to.*restore")
    fun initialize() {}
    fun scan(screen: ScreenState): List<ThreatAlert> {
        val alerts = mutableListOf<ThreatAlert>()
        for (pattern in knownPhishingPatterns) {
            if (Regex(pattern).containsMatchIn(screen.visibleText.lowercase())) {
                alerts.add(ThreatAlert(ThreatSeverity.HIGH, "Phishing detected", screen.currentApp, "Close immediately"))
                break
            }
        }
        return alerts
    }
}
