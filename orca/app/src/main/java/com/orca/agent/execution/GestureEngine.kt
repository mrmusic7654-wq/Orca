package com.orca.agent.execution
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class GestureEngine @Inject constructor(private val tc: TouchController) {
    suspend fun swipeUp() = tc.executeGesture(GestureCommand.Swipe(540, 1800, 540, 1300))
    suspend fun tapAt(x: Int, y: Int) = tc.executeGesture(GestureCommand.Tap(x, y))
    suspend fun swipe(sx: Int, sy: Int, ex: Int, ey: Int, d: Long = 300) = tc.executeGesture(GestureCommand.Swipe(sx, sy, ex, ey))
    suspend fun longPress(x: Int, y: Int) = tc.executeGesture(GestureCommand.Swipe(x, y, x, y))
}
