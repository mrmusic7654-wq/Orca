package com.orca.agent.execution
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import com.orca.agent.data.network.GeminiApi
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ScreenParser @Inject constructor(private val api: GeminiApi) {
    data class UIElement(val text: String, val desc: String, val clickable: Boolean)
    suspend fun parse(screenshot: Bitmap, node: AccessibilityNodeInfo?): List<UIElement> = emptyList()
}
