package com.orca.agent.execution

import com.orca.agent.core.ClickableElement
import com.orca.agent.core.ScreenState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UIElementValidator @Inject constructor() {
    data class ValidatedElement(
        val element: ClickableElement?,
        val confidence: Float,
        val validationMethod: String,
        val warnings: List<String> = emptyList()
    )

    fun validateElement(expectedText: String, expectedCoordinates: Pair<Int, Int>, screen: ScreenState): ValidatedElement {
        val textMatch = screen.clickableElements.find { it.text.equals(expectedText, ignoreCase = true) }
        if (textMatch != null) {
            return ValidatedElement(textMatch, 1.0f, "exact_text_match")
        }
        val fuzzyMatch = screen.clickableElements.find { it.text.contains(expectedText, ignoreCase = true) }
        if (fuzzyMatch != null) {
            return ValidatedElement(fuzzyMatch, 0.7f, "fuzzy_text_match", listOf("Fuzzy match"))
        }
        return ValidatedElement(null, 0f, "no_match", listOf("No element found"))
    }

    fun isAccessibilityTreeStale(screen: ScreenState): Boolean {
        return System.currentTimeMillis() - screen.timestamp > 2000
    }

    fun forceRefreshAccessibilityTree(): ScreenState = ScreenState.capture()
}
