package com.orca.agent.core

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import com.orca.agent.execution.AccessibilityBridge
import java.io.ByteArrayOutputStream
import android.util.Base64

data class ScreenState(
    val currentApp: String = "",
    val visibleText: String = "",
    val visibleUrls: List<String> = emptyList(),
    val clickableElements: List<ClickableElement> = emptyList(),
    val permissionRequests: List<String> = emptyList(),
    val screenshotBase64: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun describe(): String {
        return """
            App: $currentApp
            Visible text: ${visibleText.take(200)}
            Elements: ${clickableElements.size} interactive elements
            URLs: $visibleUrls
        """.trimIndent()
    }
    
    companion object {
        fun capture(): ScreenState {
            val bridge = AccessibilityBridge.getInstance()
            val screenState = AccessibilityBridge.screenState.value
            return screenState ?: ScreenState()
        }
        
        fun fromAccessibilityNode(rootNode: AccessibilityNodeInfo): ScreenState {
            val clickableElements = mutableListOf<ClickableElement>()
            val textContent = StringBuilder()
            val urls = mutableListOf<String>()
            
            extractElements(rootNode, clickableElements, textContent, urls)
            
            return ScreenState(
                currentApp = rootNode.packageName?.toString() ?: "",
                visibleText = textContent.toString(),
                visibleUrls = urls,
                clickableElements = clickableElements,
                screenshotBase64 = captureScreenshot()
            )
        }
        
        private fun extractElements(
            node: AccessibilityNodeInfo,
            clickable: MutableList<ClickableElement>,
            textContent: StringBuilder,
            urls: MutableList<String>
        ) {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            
            if (node.isClickable && rect.width() > 0 && rect.height() > 0) {
                clickable.add(
                    ClickableElement(
                        text = node.text?.toString() ?: "",
                        description = node.contentDescription?.toString() ?: "",
                        bounds = rect,
                        className = node.className?.toString() ?: ""
                    )
                )
            }
            
            if (!node.text.isNullOrBlank()) {
                textContent.append(node.text).append(" ")
                
                // Check for URLs
                val text = node.text.toString()
                val urlPattern = Regex("https?://[\\w./]+")
                urlPattern.findAll(text).forEach { urls.add(it.value) }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                extractElements(child, clickable, textContent, urls)
            }
        }
        
        private fun captureScreenshot(): String {
            // Implementation would use MediaProjection or root access
            return "" // Placeholder
        }
    }
}

data class ClickableElement(
    val text: String,
    val description: String,
    val bounds: android.graphics.Rect,
    val className: String
)
