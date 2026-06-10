package com.orca.agent.execution

import android.graphics.Rect
import com.orca.agent.core.ClickableElement
import com.orca.agent.core.ScreenState
import kotlin.math.abs
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

    // ============================================================
    // MULTI-LAYER VALIDATION
    // ============================================================
    
    fun validateElement(
        expectedText: String,
        expectedCoordinates: Pair<Int, Int>,
        screen: ScreenState,
        geminiResponse: String
    ): ValidatedElement {
        val warnings = mutableListOf<String>()
        var totalConfidence = 0f
        var matchCount = 0
        
        // Layer 1: Exact text match in accessibility tree
        val textMatch = screen.clickableElements.find { 
            it.text.equals(expectedText, ignoreCase = true) ||
            it.description.equals(expectedText, ignoreCase = true)
        }
        if (textMatch != null) {
            totalConfidence += 1.0f
            matchCount++
        } else {
            warnings.add("No exact text match found for '$expectedText'")
        }
        
        // Layer 2: Partial text match (fuzzy)
        if (textMatch == null) {
            val fuzzyMatch = screen.clickableElements.find {
                it.text.contains(expectedText, ignoreCase = true) ||
                expectedText.contains(it.text, ignoreCase = true) ||
                levenshteinDistance(it.text.lowercase(), expectedText.lowercase()) <= 3
            }
            if (fuzzyMatch != null) {
                totalConfidence += 0.7f
                matchCount++
                warnings.add("Fuzzy match: '${fuzzyMatch.text}' instead of '$expectedText'")
            }
        }
        
        // Layer 3: Coordinate proximity check
        val coordMatch = screen.clickableElements.find {
            val elementCenter = Pair(it.bounds.centerX(), it.bounds.centerY())
            distanceBetween(elementCenter, expectedCoordinates) < 100 // Within 100 pixels
        }
        if (coordMatch != null) {
            totalConfidence += 0.8f
            matchCount++
            if (textMatch == null) {
                warnings.add("Element at expected position but text differs: '${coordMatch.text}'")
            }
        }
        
        // Layer 4: Gemini hallucination check
        val geminiCoords = extractCoordinatesFromGeminiResponse(geminiResponse)
        if (geminiCoords != null) {
            val realElementNearGeminiCoords = screen.clickableElements.find {
                distanceBetween(
                    Pair(it.bounds.centerX(), it.bounds.centerY()),
                    geminiCoords
                ) < 80
            }
            if (realElementNearGeminiCoords == null) {
                totalConfidence -= 0.5f
                warnings.add("GEMINI HALLUCINATION: No element exists at Gemini's suggested coordinates $geminiCoords")
            }
        }
        
        // Layer 5: Visual boundary check
        if (expectedCoordinates.first < 0 || expectedCoordinates.first > 1080 ||
            expectedCoordinates.second < 0 || expectedCoordinates.second > 2400) {
            totalConfidence -= 1.0f
            warnings.add("Coordinates out of screen bounds: $expectedCoordinates")
        }
        
        // Final confidence
        val finalConfidence = if (matchCount > 0) {
            (totalConfidence / matchCount).coerceIn(0f, 1f)
        } else {
            0f
        }
        
        return ValidatedElement(
            element = textMatch ?: coordMatch,
            confidence = finalConfidence,
            validationMethod = "multi-layer: $matchCount layers matched",
            warnings = warnings
        )
    }

    // ============================================================
    // STALE TREE DETECTION
    // ============================================================
    
    fun isAccessibilityTreeStale(screen: ScreenState): Boolean {
        val ageMs = System.currentTimeMillis() - screen.timestamp
        if (ageMs > 2000) return true // Older than 2 seconds
        
        // Check if tree is suspiciously empty (service might have died)
        if (screen.clickableElements.isEmpty() && 
            screen.visibleText.isBlank() &&
            screen.currentApp.isNotEmpty()) {
            return true // App is running but tree is empty - stale
        }
        
        return false
    }
    
    fun forceRefreshAccessibilityTree(): ScreenState {
        // Trigger a refresh by briefly focusing and unfocusing
        val bridge = AccessibilityBridge.getInstance()
        bridge?.rootInActiveWindow?.let { root ->
            root.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            Thread.sleep(50)
            root.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        }
        Thread.sleep(100)
        return ScreenState.capture()
    }

    // ============================================================
    // A/B TESTING & UI VARIATION DETECTION
    // ============================================================
    
    fun detectUIVariation(
        appPackage: String,
        cachedElement: ClickableElement,
        currentScreen: ScreenState
    ): UIVariation {
        // Check if the exact cached element still exists
        val exactMatch = currentScreen.clickableElements.find {
            it.text == cachedElement.text &&
            it.className == cachedElement.className &&
            distanceBetween(
                Pair(it.bounds.centerX(), it.bounds.centerY()),
                Pair(cachedElement.bounds.centerX(), cachedElement.bounds.centerY())
            ) < 50
        }
        
        if (exactMatch != null) {
            return UIVariation.NO_CHANGE
        }
        
        // Check if element moved
        val textMatch = currentScreen.clickableElements.find {
            it.text == cachedElement.text
        }
        if (textMatch != null) {
            return UIVariation.ELEMENT_MOVED(
                oldPosition = Pair(cachedElement.bounds.centerX(), cachedElement.bounds.centerY()),
                newPosition = Pair(textMatch.bounds.centerX(), textMatch.bounds.centerY()),
                element = textMatch
            )
        }
        
        // Check if element was renamed
        val nearbyMatch = currentScreen.clickableElements.find {
            distanceBetween(
                Pair(it.bounds.centerX(), it.bounds.centerY()),
                Pair(cachedElement.bounds.centerX(), cachedElement.bounds.centerY())
            ) < 60
        }
        if (nearbyMatch != null) {
            return UIVariation.ELEMENT_RENAMED(
                oldText = cachedElement.text,
                newText = nearbyMatch.text,
                element = nearbyMatch
            )
        }
        
        // Check for dark mode / theme change
        if (cachedElement.className != "android.widget.Button" &&
            currentScreen.clickableElements.any { 
                it.className == cachedElement.className.replace("Light", "Dark") ||
                it.className == cachedElement.className.replace("Dark", "Light")
            }) {
            return UIVariation.THEME_CHANGED
        }
        
        return UIVariation.ELEMENT_NOT_FOUND
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun extractCoordinatesFromGeminiResponse(response: String): Pair<Int, Int>? {
        val patterns = listOf(
            Regex("""\((\d+)\s*,\s*(\d+)\)"""),
            Regex("""x:\s*(\d+).*?y:\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""at\s*\(?(\d+)[,x]\s*(\d+)\)?""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(response)
            if (match != null) {
                val x = match.groupValues[1].toIntOrNull() ?: continue
                val y = match.groupValues[2].toIntOrNull() ?: continue
                return Pair(x, y)
            }
        }
        return null
    }
    
    private fun distanceBetween(p1: Pair<Int, Int>, p2: Pair<Int, Int>): Float {
        val dx = (p1.first - p2.first).toFloat()
        val dy = (p1.second - p2.second).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i-1] == s2[j-1]) dp[i-1][j-1]
                else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
            }
        }
        return dp[s1.length][s2.length]
    }
}

sealed class UIVariation {
    object NO_CHANGE : UIVariation()
    data class ELEMENT_MOVED(
        val oldPosition: Pair<Int, Int>,
        val newPosition: Pair<Int, Int>,
        val element: ClickableElement
    ) : UIVariation()
    data class ELEMENT_RENAMED(
        val oldText: String,
        val newText: String,
        val element: ClickableElement
    ) : UIVariation()
    object THEME_CHANGED : UIVariation()
    object ELEMENT_NOT_FOUND : UIVariation()
}
