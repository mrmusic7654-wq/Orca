package com.orca.agent.core

import kotlinx.coroutines.flow.StateFlow

// ============================================================
// CORE TYPES - AGENTIC AI
// ============================================================

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
) {
    fun summarize(): String = when (type) {
        InputType.TEXT -> text?.take(100) ?: "Text input"
        InputType.IMAGE -> "Image input"
        InputType.VOICE -> "Voice input"
        InputType.SEE_AND_ACT -> "SeeAndAct"
    }
}

// Agent Actions
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

// Screen State
data class ScreenState(
    val currentApp: String = "",
    val visibleText: String = "",
    val clickableElements: List<ClickableElement> = emptyList(),
    val screenshotBase64: String = "",
    val accessibilityTree: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun describe(): String = "App: $currentApp | Elements: ${clickableElements.size} | Text: ${visibleText.take(200)}"
    fun isEmpty(): Boolean = currentApp.isEmpty() && visibleText.isEmpty()
    
    companion object {
        fun capture(): ScreenState {
            return com.orca.agent.execution.AccessibilityBridge.screenState.value 
                ?: ScreenState()
        }
    }
}

data class ClickableElement(
    val text: String,
    val description: String,
    val bounds: android.graphics.Rect,
    val className: String,
    val isEditable: Boolean = false
)

// Agent Context
data class AgentContext(
    val currentScreen: ScreenState,
    val previousScreen: ScreenState? = null,
    val activeGoal: Goal? = null,
    val recentActions: List<AgentAction> = emptyList(),
    val memoryItems: List<MemoryItem> = emptyList(),
    val userProfile: UserProfile = UserProfile()
)

data class Goal(
    val id: String,
    val description: String,
    val priority: Float,
    val isActive: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class MemoryItem(
    val id: String,
    val content: String,
    val type: MemoryType,
    val relevance: Float,
    val timestamp: Long
)

enum class MemoryType { EPISODIC, SEMANTIC, PROCEDURAL, PATTERN }

data class UserProfile(
    val name: String = "",
    val preferences: Map<String, String> = emptyMap(),
    val frequentApps: List<String> = emptyList(),
    val importantContacts: List<String> = emptyList()
)

// Verification Result
data class VerificationResult(
    val matches: Boolean,
    val confidence: Float,
    val description: String,
    val suggestedCorrection: AgentAction? = null
)

// Execution Result
sealed class ExecutionResult {
    data class Success(val description: String) : ExecutionResult()
    data class NeedsRecovery(val error: String, val screenshot: ScreenState) : ExecutionResult()
    data class NeedsConfirmation(val question: String, val riskLevel: RiskLevel) : ExecutionResult()
    data class Failure(val error: String, val canRecover: Boolean) : ExecutionResult()
}

// Cognitive Engine Interface
interface CognitiveEngine {
    val state: StateFlow<CognitiveState>
    suspend fun plan(task: String, context: AgentContext): TaskChain
    suspend fun verify(expected: String, actual: ScreenState): VerificationResult
    suspend fun recover(error: String, context: AgentContext): AgentAction?
    suspend fun reflect(experience: TaskExperience): List<String>
}

data class TaskExperience(
    val task: String,
    val plan: TaskChain,
    val result: ExecutionResult,
    val screenshots: List<ScreenState>,
    val timestamp: Long = System.currentTimeMillis()
)
