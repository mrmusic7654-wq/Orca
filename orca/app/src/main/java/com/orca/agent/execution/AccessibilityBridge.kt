// app/src/main/java/com/orca/agent/execution/AccessibilityBridge.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.execution

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.orca.agent.core.AgentAction
import com.orca.agent.core.ScreenState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AccessibilityBridge : AccessibilityService() {
    
    companion object {
        private var instance: AccessibilityBridge? = null
        private val _screenState = MutableStateFlow<ScreenState?>(null)
        val screenState: StateFlow<ScreenState?> = _screenState.asStateFlow()
        
        private val _actionResults = MutableSharedFlow<ActionResult>()
        val actionResults: SharedFlow<ActionResult> = _actionResults.asSharedFlow()
        
        fun getInstance(): AccessibilityBridge? = instance
        
        suspend fun executeAction(action: AgentAction): ActionResult {
            return withContext(Dispatchers.Main) {
                instance?.performAction(action) ?: ActionResult.Failure("Service not running")
            }
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            notificationTimeout = 100
        }
        setServiceInfo(info)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Capture screen state
        val rootNode = rootInActiveWindow ?: return
        
        val screenState = ScreenState.fromAccessibilityNode(rootNode)
        _screenState.value = screenState
    }
    
    override fun onInterrupt() {
        instance = null
    }
    
    private fun performAction(action: AgentAction): ActionResult {
        return when (action) {
            is AgentAction.Tap -> performTap(action.x, action.y)
            is AgentAction.Swipe -> performSwipe(
                action.startX, action.startY,
                action.endX, action.endY,
                action.duration
            )
            is AgentAction.Type -> performType(action.text, action.targetField)
            is AgentAction.LongPress -> performLongPress(action.x, action.y)
            is AgentAction.Back -> performGlobalAction(GLOBAL_ACTION_BACK)
            is AgentAction.Home -> performGlobalAction(GLOBAL_ACTION_HOME)
            is AgentAction.AppAction -> launchApp(action.packageName)
            is AgentAction.Pinch -> ActionResult.Failure("Pinch not yet implemented")
            is AgentAction.Wait -> ActionResult.Success("Waited")
            is AgentAction.Speak -> ActionResult.Success("TTS: ${action.text}")
            is AgentAction.AskUser -> ActionResult.NeedsUserInput(action.question)
        }
    }
    
    private fun performTap(x: Int, y: Int): ActionResult {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1))
            .build()
        
        return try {
            dispatchGesture(gesture, null, null)
            ActionResult.Success("Tapped at ($x, $y)")
        } catch (e: Exception) {
            ActionResult.Failure("Tap failed: ${e.message}")
        }
    }
    
    private fun performSwipe(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        duration: Long
    ): ActionResult {
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        
        return try {
            dispatchGesture(gesture, null, null)
            ActionResult.Success("Swiped from ($startX,$startY) to ($endX,$endY)")
        } catch (e: Exception) {
            ActionResult.Failure("Swipe failed: ${e.message}")
        }
    }
    
    private fun performType(text: String, targetField: String): ActionResult {
        val rootNode = rootInActiveWindow ?: return ActionResult.Failure("No active window")
        
        // Find the target input field
        val inputField = findNodeByText(rootNode, targetField) 
            ?: findEditableNode(rootNode)
            ?: return ActionResult.Failure("Input field not found: $targetField")
        
        return try {
            // Focus the field
            inputField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            
            // Set text
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            ActionResult.Success("Typed '$text' into $targetField")
        } catch (e: Exception) {
            ActionResult.Failure("Type failed: ${e.message}")
        }
    }
    
    private fun performLongPress(x: Int, y: Int): ActionResult {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 800)) // 800ms long press
            .build()
        
        return try {
            dispatchGesture(gesture, null, null)
            ActionResult.Success("Long pressed at ($x, $y)")
        } catch (e: Exception) {
            ActionResult.Failure("Long press failed: ${e.message}")
        }
    }
    
    private fun launchApp(packageName: String): ActionResult {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                ActionResult.Success("Launched $packageName")
            } else {
                ActionResult.Failure("App not found: $packageName")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Launch failed: ${e.message}")
        }
    }
    
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
        }
        
        return null
    }
    
    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableNode(child)
            if (found != null) return found
        }
        
        return null
    }
    
    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(val error: String) : ActionResult()
    data class NeedsUserInput(val question: String) : ActionResult()
}
