package com.orca.agent.core

import kotlinx.coroutines.flow.StateFlow

enum class CognitiveState {
    IDLE, OBSERVING, PLANNING, EXECUTING, VERIFYING,
    RECOVERING, AWAITING_CONFIRMATION, LEARNING,
    AUTONOMOUS, ERROR, SHUTDOWN
}

enum class InputType { TEXT, IMAGE, VOICE, SEE_AND_ACT }
enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class UserInput(
    val text: String?,
    val imageBase64: String? = null,
    val voiceInput: ByteArray? = null,
    val type: InputType,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

sealed class AgentAction {
    data class Tap(val x: Int, val y: Int, val description: String = "") : AgentAction()
    data class Swipe(val startX: Int, val startY: Int, val endX: Int, val endY: Int, val duration: Long = 300) : AgentAction()
    data class Type(val text: String, val targetField: String = "") : AgentAction()
    data class LongPress(val x: Int, val y: Int) : AgentAction()
    data class AppAction(val packageName: String, val action: String = "launch") : AgentAction()
    data class ScreenshotToGemini(val prompt: String, val screenshotBase64: String) : AgentAction()
    data class WaitForUserConfirmation(val question: String, val riskLevel: RiskLevel) : AgentAction()
    object Back : AgentAction()
    object Home : AgentAction()
    object Wait : AgentAction()
    data class Speak(val text: String) : AgentAction()
}

data class ScreenState(
    val currentApp: String = "",
    val visibleText: String = "",
    val clickableElements: List<ClickableElement> = emptyList(),
    val screenshotBase64: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun describe(): String = "App: $currentApp"
    fun isEmpty(): Boolean = currentApp.isEmpty()
    companion object {
        fun capture(): ScreenState = ScreenState()
        fun fromAccessibilityNode(node: Any): ScreenState = ScreenState()
    }
}

data class ClickableElement(
    val text: String,
    val description: String,
    val bounds: android.graphics.Rect,
    val className: String,
    val isEditable: Boolean = false
)

data class AgentContext(
    val currentScreen: ScreenState = ScreenState(),
    val previousScreen: ScreenState? = null,
    val recentActions: List<AgentAction> = emptyList()
)

data class VerificationResult(
    val matches: Boolean,
    val confidence: Float,
    val description: String,
    val suggestedCorrection: AgentAction? = null
)

sealed class ExecutionResult {
    data class Success(val description: String) : ExecutionResult()
    data class NeedsRecovery(val error: String, val screenshot: ScreenState) : ExecutionResult()
    data class NeedsConfirmation(val question: String, val riskLevel: RiskLevel) : ExecutionResult()
    data class Failure(val error: String, val canRecover: Boolean) : ExecutionResult()
}

interface CognitiveEngine {
    val state: StateFlow<CognitiveState>
    suspend fun plan(task: String, context: AgentContext): TaskChain
    suspend fun verify(expected: String, actual: ScreenState): VerificationResult
    suspend fun recover(error: String, context: AgentContext): AgentAction?
}

data class TaskExperience(
    val task: String,
    val plan: TaskChain,
    val result: ExecutionResult,
    val screenshots: List<ScreenState> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
