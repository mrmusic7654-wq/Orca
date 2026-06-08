// app/src/main/java/com/orca/agent/core/CognitiveEngine.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.core

import kotlinx.coroutines.flow.StateFlow

interface CognitiveEngine {
    val state: StateFlow<CognitiveState>
    
    suspend fun think(input: ThinkInput): ThinkOutput
    suspend fun plan(task: String, context: AgentContext): TaskChain
    suspend fun reflect(experience: Experience): Reflection
}

data class ThinkInput(
    val prompt: String,
    val screenState: ScreenState,
    val maxTokens: Int = 4096
)

data class ThinkOutput(
    val reasoning: String,
    val action: AgentAction?,
    val confidence: Float,
    val metadata: Map<String, Any>
)

sealed class AgentAction {
    data class Tap(val x: Int, val y: Int, val description: String) : AgentAction()
    data class Swipe(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int,
        val duration: Long
    ) : AgentAction()
    data class Type(val text: String, val targetField: String) : AgentAction()
    data class LongPress(val x: Int, val y: Int) : AgentAction()
    data class Pinch(val scale: Float, val centerX: Int, val centerY: Int) : AgentAction()
    data class AppAction(val packageName: String, val action: String) : AgentAction()
    object Back : AgentAction()
    object Home : AgentAction()
    object Wait : AgentAction()
    data class Speak(val text: String) : AgentAction()
    data class AskUser(val question: String, val context: String) : AgentAction()
}

data class Experience(
    val task: String,
    val actions: List<AgentAction>,
    val outcome: Outcome,
    val screenTransitions: List<ScreenState>
)

enum class Outcome { SUCCESS, PARTIAL_SUCCESS, FAILURE, TIMEOUT }

data class Reflection(
    val lessons: List<String>,
    val improvedStrategy: String?,
    val confidenceAdjustment: Float
)
