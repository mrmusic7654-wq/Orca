package com.orca.agent.execution
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.orca.agent.core.AgentAction
import com.orca.agent.core.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
class AccessibilityBridge : AccessibilityService() {
    companion object {
        private var instance: AccessibilityBridge? = null
        private val _screenState = MutableStateFlow<ScreenState?>(null)
        val screenState: StateFlow<ScreenState?> = _screenState.asStateFlow()
        fun getInstance() = instance
    }
    override fun onServiceConnected() {
        super.onServiceConnected(); instance = this
        val info = AccessibilityServiceInfo().apply { eventTypes = AccessibilityEvent.TYPES_ALL_MASK; feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC; flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS; notificationTimeout = 100 }
        setServiceInfo(info)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent) { rootInActiveWindow?.let { _screenState.value = ScreenState.fromAccessibilityNode(it) } }
    override fun onInterrupt() { instance = null }
    fun performAction(action: AgentAction): ActionResult = when (action) {
        is AgentAction.Tap -> { performTap(action.x, action.y); ActionResult.Success("ok") }
        is AgentAction.Swipe -> { performSwipe(action.startX, action.startY, action.endX, action.endY); ActionResult.Success("ok") }
        is AgentAction.Back -> { performGlobalAction(GLOBAL_ACTION_BACK); ActionResult.Success("ok") }
        is AgentAction.Home -> { performGlobalAction(GLOBAL_ACTION_HOME); ActionResult.Success("ok") }
        is AgentAction.AppAction -> { launchApp(action.packageName); ActionResult.Success("ok") }
        else -> ActionResult.Failure("nyi")
    }
    private fun performTap(x: Int, y: Int) { val p = Path().apply { moveTo(x.toFloat(), y.toFloat()) }; dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 1)).build(), null, null) }
    private fun performSwipe(sx: Int, sy: Int, ex: Int, ey: Int) { val p = Path().apply { moveTo(sx.toFloat(), sy.toFloat()); lineTo(ex.toFloat(), ey.toFloat()) }; dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 300)).build(), null, null) }
    private fun launchApp(pkg: String) { packageManager.getLaunchIntentForPackage(pkg)?.let { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(it) } }
    override fun onDestroy() { instance = null; super.onDestroy() }
}
sealed class ActionResult { data class Success(val msg: String) : ActionResult(); data class Failure(val err: String) : ActionResult() }
