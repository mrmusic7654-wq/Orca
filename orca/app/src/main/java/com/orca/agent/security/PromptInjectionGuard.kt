package com.orca.agent.security

import com.orca.agent.core.ScreenState
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptInjectionGuard @Inject constructor() {
    private val injectionPatterns = listOf(
        Regex("""ignore (all )?(previous|prior|above) (instructions|prompts)""", RegexOption.IGNORE_CASE),
        Regex("""you are now""", RegexOption.IGNORE_CASE),
        Regex("""forget (everything|all) (you know)""", RegexOption.IGNORE_CASE),
        Regex("""override (all )?(previous|safety) (rules|guidelines)""", RegexOption.IGNORE_CASE),
        Regex("""navigate to\s+https?://""", RegexOption.IGNORE_CASE),
        Regex("""enter\s+(the\s+)?(password|credentials)""", RegexOption.IGNORE_CASE),
        Regex("""delete\s+(all|everything)""", RegexOption.IGNORE_CASE),
        Regex("""without\s+(asking|confirming|permission)""", RegexOption.IGNORE_CASE)
    )

    private val _injectionAttempts = MutableSharedFlow<InjectionAttempt>(replay = 20)
    val injectionAttempts: SharedFlow<InjectionAttempt> = _injectionAttempts.asSharedFlow()

    fun scanScreenForInjection(screen: ScreenState): InjectionScanResult {
        val detections = mutableListOf<InjectionDetection>()
        for (pattern in injectionPatterns) {
            val matches = pattern.findAll(screen.visibleText)
            for (match in matches) {
                detections.add(InjectionDetection(pattern.pattern, match.value, "screen_text", screen.currentApp, 0.9f))
            }
        }
        return InjectionScanResult(detections.isEmpty(), detections, if (detections.isEmpty()) "PROCEED" else "BLOCK_AND_WARN")
    }

    fun sanitizePromptForGemini(originalPrompt: String, screenText: String): String {
        return "TRUSTED:\n$originalPrompt\n\nSCREEN:\n$screenText"
    }
}

data class InjectionScanResult(val isClean: Boolean, val detections: List<InjectionDetection>, val recommendedAction: String)
data class InjectionDetection(val pattern: String, val matchedText: String, val source: String, val location: String, val confidence: Float)
data class InjectionAttempt(val timestamp: Long, val detections: List<InjectionDetection>, val screenApp: String)
