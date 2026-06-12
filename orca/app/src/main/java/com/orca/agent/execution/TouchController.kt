package com.orca.agent.execution
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class TouchController @Inject constructor() {
    suspend fun executeGesture(g: GestureCommand): Boolean {
        val b = AccessibilityBridge.getInstance() ?: return false
        return when (g) {
            is GestureCommand.Tap -> b.performAction(com.orca.agent.core.AgentAction.Tap(g.x, g.y)) is ActionResult.Success
            is GestureCommand.Swipe -> b.performAction(com.orca.agent.core.AgentAction.Swipe(g.sx, g.sy, g.ex, g.ey)) is ActionResult.Success
        }
    }
}
sealed class GestureCommand {
    data class Tap(val x: Int, val y: Int) : GestureCommand()
    data class Swipe(val sx: Int, val sy: Int, val ex: Int, val ey: Int) : GestureCommand()
}
