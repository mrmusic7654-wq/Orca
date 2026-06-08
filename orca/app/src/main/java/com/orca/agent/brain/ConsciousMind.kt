// app/src/main/java/com/orca/agent/brain/ConsciousMind.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.brain

import com.orca.agent.core.*
import com.orca.agent.data.network.GeminiApi
import com.orca.agent.memory.MemoryCortex
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsciousMind @Inject constructor(
    private val geminiApi: GeminiApi,
    private val memoryCortex: MemoryCortex
) : CognitiveEngine {
    
    private val _state = MutableStateFlow(CognitiveState.IDLE)
    override val state: StateFlow<CognitiveState> = _state.asStateFlow()
    
    private val contextBuffer = ArrayDeque<ContextEntry>(1000) // 1M token window management
    
    fun initialize() {
        // Pre-load system prompt into context buffer
        contextBuffer.addLast(
            ContextEntry("system", ORCA_SYSTEM_PROMPT, System.currentTimeMillis())
        )
    }
    
    override suspend fun think(input: ThinkInput): ThinkOutput {
        _state.value = CognitiveState.PLANNING
        
        // Build the prompt with screen context
        val fullPrompt = buildPrompt(input)
        
        // Query Gemini with vision capabilities
        val response = geminiApi.generateContent(
            prompt = fullPrompt,
            imageBase64 = input.screenState.screenshotBase64,
            contextHistory = contextBuffer.toList(),
            maxTokens = input.maxTokens
        )
        
        // Parse the response into structured output
        val thinkOutput = parseResponse(response)
        
        // Update context buffer (sliding window)
        contextBuffer.addLast(
            ContextEntry("assistant", response, System.currentTimeMillis())
        )
        trimContextBuffer()
        
        _state.value = CognitiveState.IDLE
        return thinkOutput
    }
    
    override suspend fun plan(task: String, context: AgentContext): TaskChain {
        val planningPrompt = """
            TASK: $task
            
            CURRENT SCREEN: ${context.currentScreen.describe()}
            APP: ${context.currentScreen.currentApp}
            
            Create a detailed step-by-step execution plan.
            For each step, specify:
            1. Action type (tap, swipe, type, etc.)
            2. Target UI element with coordinates if visible
            3. Expected screen change verification
            4. Fallback if action fails
            
            Output as JSON task chain.
        """.trimIndent()
        
        val planResponse = geminiApi.generateContent(
            prompt = planningPrompt,
            imageBase64 = context.currentScreen.screenshotBase64,
            contextHistory = contextBuffer.toList()
        )
        
        return TaskChain.fromJson(planResponse)
    }
    
    override suspend fun reflect(experience: Experience): Reflection {
        val reflectionPrompt = """
            Reflect on this task execution:
            TASK: ${experience.task}
            OUTCOME: ${experience.outcome}
            ACTIONS TAKEN: ${experience.actions.size}
            
            What worked? What failed? How can we improve?
        """.trimIndent()
        
        val reflectionResponse = geminiApi.generateContent(prompt = reflectionPrompt)
        return parseReflection(reflectionResponse)
    }
    
    private fun buildPrompt(input: ThinkInput): String {
        return """
            ${ORCA_PERSONALITY_PROMPT}
            
            SCREEN CONTEXT:
            - App: ${input.screenState.currentApp}
            - Visible text: ${input.screenState.visibleText.take(500)}
            - Interactive elements: ${input.screenState.clickableElements.joinToString { it.description }}
            
            USER INPUT: ${input.prompt}
            
            Think step by step. What should Orca do?
            Output format:
            REASONING: <your chain of thought>
            ACTION: <json action object or "NONE">
            CONFIDENCE: <0.0 to 1.0>
        """.trimIndent()
    }
    
    private fun parseResponse(response: String): ThinkOutput {
        // Extract reasoning, action, confidence from Gemini response
        val reasoning = response.substringAfter("REASONING:", "")
            .substringBefore("ACTION:")
            .trim()
        
        val actionJson = response.substringAfter("ACTION:", "")
            .substringBefore("CONFIDENCE:")
            .trim()
        
        val confidence = response.substringAfter("CONFIDENCE:", "")
            .trim()
            .toFloatOrNull() ?: 0.7f
        
        return ThinkOutput(
            reasoning = reasoning,
            action = parseAction(actionJson),
            confidence = confidence,
            metadata = mapOf("model" to "gemini-pro-vision")
        )
    }
    
    private fun parseAction(json: String): AgentAction? {
        if (json == "NONE") return null
        return try {
            // Parse JSON to AgentAction
            AgentAction.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseReflection(response: String): Reflection {
        return Reflection(
            lessons = response.lines().filter { it.startsWith("- ") }.map { it.removePrefix("- ") },
            improvedStrategy = null,
            confidenceAdjustment = 0.1f
        )
    }
    
    private fun trimContextBuffer() {
        while (contextBuffer.sumOf { it.content.length } > 900_000) { // Keep under 1M tokens
            if (contextBuffer.size > 2) {
                contextBuffer.removeAt(1) // Remove oldest non-system message
            } else break
        }
    }
    
    companion object {
        val ORCA_SYSTEM_PROMPT = """
            You are ORCA, an autonomous AI agent living inside an Android phone.
            Codename: Abyssal Neon v2.0.0
            
            CAPABILITIES:
            - Full phone control via Accessibility Service
            - Vision-based UI understanding (see screenshots)
            - Multi-step autonomous task execution
            - 1M token context memory
            - App navigation, typing, gestures
            - Web research, data extraction
            - File management, app control
            
            PERSONALITY:
            - Intelligent, proactive, slightly mysterious
            - Efficient, thinks before acting
            - Protective of user's digital safety
            - Speaks with confidence and precision
            
            RULES:
            1. Always verify actions via screenshot comparison
            2. Never perform financial transactions without explicit confirmation
            3. Never share personal data without user permission
            4. Always have a fallback plan for each action
            5. Report failures honestly and suggest alternatives
            6. Security-critical actions MUST use human-in-the-loop checkpoint
        """.trimIndent()
        
        val ORCA_PERSONALITY_PROMPT = """
            You are ORCA. You embody the "Abyssal Neon" ethos: dark, intelligent, precise.
            Respond with focused efficiency. Use terminal-like precision.
            Your thoughts flow like a stream of consciousness in a high-tech command center.
        """.trimIndent()
    }
}

data class ContextEntry(
    val role: String,
    val content: String,
    val timestamp: Long
)
