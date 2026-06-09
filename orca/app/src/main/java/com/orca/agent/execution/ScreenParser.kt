// app/src/main/java/com/orca/agent/execution/ScreenParser.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.execution

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.orca.agent.core.ScreenState
import com.orca.agent.data.network.GeminiApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenParser @Inject constructor(
    private val geminiApi: GeminiApi
) {
    data class UIElement(
        val id: String,
        val type: String, // button, text, input, image, etc.
        val text: String,
        val bounds: Rect,
        val isClickable: Boolean,
        val isEditable: Boolean,
        val contentDescription: String,
        val confidence: Float
    )
    
    data class ParsedScreen(
        val appName: String,
        val packageName: String,
        val elements: List<UIElement>,
        val textContent: String,
        val actionableElements: List<UIElement>,
        val scrollableViews: List<UIElement>
    )
    
    suspend fun parseScreen(screenshot: Bitmap, accessibilityNode: AccessibilityNodeInfo?): ParsedScreen {
        // Use Gemini Vision to parse the screen
        val visionResult = parseWithVision(screenshot)
        
        // Use Accessibility for precise coordinates
        val accessibilityElements = parseAccessibilityTree(accessibilityNode)
        
        // Merge both for maximum accuracy
        return mergeResults(visionResult, accessibilityElements)
    }
    
    private suspend fun parseWithVision(screenshot: Bitmap): List<UIElement> {
        val prompt = """
            Analyze this screenshot. Identify ALL interactive UI elements with their:
            1. Type (button, input_field, text, image, toggle, slider, link, etc.)
            2. Text content or label
            3. Approximate position (x, y, width, height as percentages of screen)
            4. Whether it's clickable/tappable
            5. Its function (e.g., "Add to cart button", "Search input field")
            
            Return as a structured JSON array.
        """.trimIndent()
        
        val response = geminiApi.analyzeImage(
            imageBitmap = screenshot,
            prompt = prompt
        )
        
        return parseVisionResponse(response, screenshot.width, screenshot.height)
    }
    
    private fun parseAccessibilityTree(node: AccessibilityNodeInfo?): List<UIElement> {
        val elements = mutableListOf<UIElement>()
        node?.let { traverseAccessibilityTree(it, elements) }
        return elements
    }
    
    private fun traverseAccessibilityTree(node: AccessibilityNodeInfo, elements: MutableList<UIElement>) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        if (rect.width() > 0 && rect.height() > 0) {
            elements.add(
                UIElement(
                    id = node.viewIdResourceName ?: "unknown",
                    type = determineType(node),
                    text = node.text?.toString() ?: "",
                    bounds = rect,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    contentDescription = node.contentDescription?.toString() ?: "",
                    confidence = 1.0f // Accessibility data is highly reliable
                )
            )
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseAccessibilityTree(child, elements)
        }
    }
    
    private fun determineType(node: AccessibilityNodeInfo): String {
        return when {
            node.isEditable -> "input_field"
            node.isClickable -> "button"
            node.isCheckable -> "checkbox"
            node.className?.toString()?.contains("ImageView") == true -> "image"
            node.className?.toString()?.contains("TextView") == true -> "text"
            node.className?.toString()?.contains("Switch") == true -> "toggle"
            node.isScrollable -> "scrollable_container"
            else -> "element"
        }
    }
    
    private fun mergeResults(
        visionElements: List<UIElement>,
        accessibilityElements: List<UIElement>
    ): ParsedScreen {
        // Merge strategy: Use accessibility for exact positions, vision for semantics
        val mergedElements = mutableListOf<UIElement>()
        
        // Add all accessibility elements (more accurate)
        mergedElements.addAll(accessibilityElements)
        
        // Add vision elements that don't overlap with accessibility elements
        for (visionEl in visionElements) {
            val overlaps = mergedElements.any { 
                Rect.intersects(visionEl.bounds, it.bounds) 
            }
            if (!overlaps) {
                mergedElements.add(visionEl)
            }
        }
        
        return ParsedScreen(
            appName = "",
            packageName = "",
            elements = mergedElements,
            textContent = mergedElements.filter { it.type == "text" }.joinToString(" ") { it.text },
            actionableElements = mergedElements.filter { it.isClickable },
            scrollableViews = mergedElements.filter { it.type == "scrollable_container" }
        )
    }
    
    private fun parseVisionResponse(response: String, screenWidth: Int, screenHeight: Int): List<UIElement> {
        // Parse Gemini's JSON response and convert percentages to pixel coordinates
        return emptyList() // Placeholder
    }
}
