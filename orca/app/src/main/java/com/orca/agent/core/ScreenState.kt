package com.orca.agent.core

import android.graphics.Rect
import android.util.Base64
import android.view.accessibility.AccessibilityNodeInfo
import com.orca.agent.execution.AccessibilityBridge
import java.io.ByteArrayOutputStream

data class ScreenState(
    val currentApp: String = "",
    val visibleText: String = "",
    val visibleUrls: List<String> = emptyList(),
    val clickableElements: List<ClickableElement> = emptyList(),
    val scrollableElements: List<ClickableElement> = emptyList(),
    val editableElements: List<ClickableElement> = emptyList(),
    val permissionRequests: List<String> = emptyList(),
    val screenshotBase64: String = "",
    val accessibilityTree: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun describe(): String {
        return """
            App: $currentApp
            Visible text: ${visibleText.take(200)}
            Interactive elements: ${clickableElements.size}
            Scrollable: ${scrollableElements.size}
            Editable: ${editableElements.size}
        """.trimIndent()
    }

    fun isEmpty(): Boolean = currentApp.isEmpty() && visibleText.isEmpty()

    companion object {
        fun capture(): ScreenState {
            val bridge = AccessibilityBridge.getInstance()
            val node = bridge?.rootInActiveWindow
            
            if (node != null) {
                return fromAccessibilityNode(node)
            }
            
            return ScreenState()
        }

        fun fromAccessibilityNode(rootNode: AccessibilityNodeInfo): ScreenState {
            val clickableElements = mutableListOf<ClickableElement>()
            val scrollableElements = mutableListOf<ClickableElement>()
            val editableElements = mutableListOf<ClickableElement>()
            val textContent = StringBuilder()
            val urls = mutableListOf<String>()
            val accessibilityTree = StringBuilder()

            extractElements(rootNode, clickableElements, scrollableElements, 
                          editableElements, textContent, urls, accessibilityTree, 0)

            return ScreenState(
                currentApp = rootNode.packageName?.toString() ?: "",
                visibleText = textContent.toString(),
                visibleUrls = urls,
                clickableElements = clickableElements,
                scrollableElements = scrollableElements,
                editableElements = editableElements,
                accessibilityTree = accessibilityTree.toString(),
                screenshotBase64 = captureScreenshotBase64(),
                timestamp = System.currentTimeMillis()
            )
        }

        private fun extractElements(
            node: AccessibilityNodeInfo,
            clickable: MutableList<ClickableElement>,
            scrollable: MutableList<ClickableElement>,
            editable: MutableList<ClickableElement>,
            textContent: StringBuilder,
            urls: MutableList<String>,
            tree: StringBuilder,
            depth: Int
        ) {
            val rect = Rect()
            node.getBoundsInScreen(rect)

            val indent = "  ".repeat(depth)
            tree.append("$indent${node.className}")

            if (rect.width() > 0 && rect.height() > 0) {
                val element = ClickableElement(
                    text = node.text?.toString() ?: "",
                    description = node.contentDescription?.toString() ?: "",
                    bounds = Rect(rect),
                    className = node.className?.toString() ?: "",
                    isEditable = node.isEditable
                )

                if (node.isClickable) {
                    clickable.add(element)
                    tree.append(" [CLICKABLE: ${element.text.take(30)}]")
                }
                if (node.isScrollable) {
                    scrollable.add(element)
                    tree.append(" [SCROLLABLE]")
                }
                if (node.isEditable) {
                    editable.add(element)
                    tree.append(" [EDITABLE]")
                }
            }

            tree.append("\n")

            if (!node.text.isNullOrBlank()) {
                textContent.append(node.text).append(" ")
                
                // Check for URLs in text
                val urlPattern = Regex("https?://[\\w./?=&%-]+")
                urlPattern.findAll(node.text.toString()).forEach { 
                    urls.add(it.value) 
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                extractElements(child, clickable, scrollable, editable, 
                              textContent, urls, tree, depth + 1)
            }
        }

        private fun captureScreenshotBase64(): String {
            // In production, this would use MediaProjection or root access
            // For now, return empty string
            return ""
        }
    }
}

data class ClickableElement(
    val text: String,
    val description: String,
    val bounds: Rect,
    val className: String,
    val isEditable: Boolean = false,
    val confidence: Float = 1.0f
) {
    fun centerX(): Int = bounds.centerX()
    fun centerY(): Int = bounds.centerY()
}
