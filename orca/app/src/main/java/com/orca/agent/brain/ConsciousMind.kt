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
    
    private val contextBuffer = ArrayDeque<ContextEntry>(2000)
    
    fun initialize() {
        contextBuffer.addLast(
            ContextEntry("system", ORCA_SYSTEM_PROMPT, System.currentTimeMillis())
        )
    }

    // ============================================================
    // PLANNING WITH GEMINI
    // ============================================================
    
    override suspend fun plan(task: String, context: AgentContext): TaskChain {
        _state.value = CognitiveState.PLANNING
        
        val planPrompt = buildPlanPrompt(task, context)
        
        val response = geminiApi.generateContent(
            prompt = planPrompt,
            imageBase64 = context.currentScreen.screenshotBase64,
            contextHistory = contextBuffer.takeLast(20).toList(),
            maxTokens = 4096
        )
        
        val plan = parseTaskChain(response, task)
        
        contextBuffer.addLast(
            ContextEntry("assistant", "Plan created: ${plan.nodes.size} steps", System.currentTimeMillis())
        )
        
        _state.value = CognitiveState.IDLE
        return plan
    }

    // ============================================================
    // VERIFICATION WITH SCREENSHOT-TO-GEMINI
    // ============================================================
    
    override suspend fun verify(expected: String, actual: ScreenState): VerificationResult {
        if (actual.screenshotBase64.isBlank()) {
            return VerificationResult(false, 0.5f, "No screenshot available for verification")
        }
        
        val verifyPrompt = """
            VERIFICATION TASK:
            I just performed an action and need to verify the result.
            
            EXPECTED RESULT: $expected
            
            ACTUAL SCREEN: [screenshot attached]
            
            Questions:
            1. Does the current screen match the expected result?
            2. What is the confidence level (0.0 to 1.0)?
            3. If it doesn't match, what is different?
            4. If it doesn't match, what single action would fix it?
            
            Respond in this format:
            MATCH: yes/no
            CONFIDENCE: 0.0-1.0
            DESCRIPTION: brief description
            FIX_ACTION: tap(x,y) or swipe(x1,y1,x2,y2) or none
        """.trimIndent()
        
        val response = geminiApi.generateContent(
            prompt = verifyPrompt,
            imageBase64 = actual.screenshotBase64,
            maxTokens = 500
        )
        
        return parseVerification(response)
    }

    // ============================================================
    // ERROR RECOVERY WITH SCREENSHOT-TO-GEMINI
    // ============================================================
    
    override suspend fun recover(error: String, context: AgentContext): AgentAction? {
        _state.value = CognitiveState.RECOVERING
        
        val recoveryPrompt = """
            RECOVERY TASK:
            Something went wrong during task execution.
            
            ERROR: $error
            
            CURRENT SCREEN: [screenshot attached]
            ${context.currentScreen.describe()}
            
            RECENT ACTIONS:
            ${context.recentActions.joinToString("\n") { it.toString() }}
            
            What single action should I take to recover and continue?
            
            Options:
            - tap(x,y) - Tap at specific coordinates
            - swipe(x1,y1,x2,y2) - Swipe gesture
            - back - Press back button
            - dismiss_popup - Dismiss any popup/dialog
            - scroll_down - Scroll down
            - scroll_up - Scroll up
            - wait - Wait and retry
            - none - Cannot recover
            
            Respond in format:
            ACTION: <action_type>
            COORDINATES: <if applicable>
            REASONING: <why this action>
        """.trimIndent()
        
        val response = geminiApi.generateContent(
            prompt = recoveryPrompt,
            imageBase64 = context.currentScreen.screenshotBase64,
            maxTokens = 500
        )
        
        _state.value = CognitiveState.IDLE
        return parseRecoveryAction(response)
    }

    // ============================================================
    // REFLECTION & LEARNING
    // ============================================================
    
    override suspend fun reflect(experience: TaskExperience): List<String> {
        val reflectPrompt = """
            REFLECTION TASK:
            Analyze this completed task execution and extract lessons.
            
            TASK: ${experience.task}
            RESULT: ${experience.result}
            STEPS: ${experience.plan.nodes.size}
            
            What worked well?
            What failed?
            What should be done differently next time?
            Any patterns that could be cached for future tasks?
            
            Respond with bullet points starting with "- "
        """.trimIndent()
        
        val response = geminiApi.generateContent(prompt = reflectPrompt, maxTokens = 1000)
        
        return response.lines()
            .filter { it.trimStart().startsWith("- ") }
            .map { it.trimStart().removePrefix("- ") }
    }

    // ============================================================
    // DAILY/WEEKLY SUMMARIES
    // ============================================================
    
    suspend fun generateDailySummary(): String {
        return geminiApi.generateContent(
            prompt = "Summarize what Orca learned today about the user's patterns and preferences.",
            maxTokens = 500
        )
    }

    // ============================================================
    // SCREENSHOT-TO-GEMINI: UI UNDERSTANDING
    // ============================================================
    
    suspend fun understandScreen(screenshotBase64: String, goal: String): ScreenUnderstanding {
        val prompt = """
            SCREEN UNDERSTANDING:
            Analyze this screenshot and help me understand the UI.
            
            GOAL: $goal
            
            Questions:
            1. What app/screen is this?
            2. What is the main purpose of this screen?
            3. List all interactive elements with their approximate coordinates (0-1000 scale)
            4. Which element would help achieve the goal?
            5. What is the single best action to take next?
            
            Respond in JSON-like format.
        """.trimIndent()
        
        val response = geminiApi.generateContent(
            prompt = prompt,
            imageBase64 = screenshotBase64,
            maxTokens = 1000
        )
        
        return parseScreenUnderstanding(response)
    }

    // ============================================================
    // PROMPT BUILDING
    // ============================================================
    
    private fun buildPlanPrompt(task: String, context: AgentContext): String {
        return """
            $ORCA_PERSONALITY_PROMPT
            
            PLANNING TASK:
            Create a detailed step-by-step execution plan for the following task.
            
            TASK: $task
            
            CURRENT SCREEN:
            ${context.currentScreen.describe()}
            
            USER PROFILE:
            ${context.userProfile}
            
            RELEVANT MEMORIES:
            ${context.memoryItems.joinToString("\n") { "- ${it.content}" }}
            
            For each step, specify:
            1. Action type (tap, swipe, type, open_app, back, etc.)
            2. What to tap/type/swipe
            3. Expected result after the action
            4. What to do if it fails (fallback)
            5. Risk level (low/medium/high)
            6. Whether user confirmation is needed
            
            Output as a structured JSON task chain with "nodes" array.
        """.trimIndent()
    }

    // ============================================================
    // PARSING
    // ============================================================
    
    private fun parseTaskChain(response: String, taskName: String): TaskChain {
        // Parse Gemini's JSON response into TaskChain
        // In production, use proper JSON parsing
        return TaskChain(
            id = java.util.UUID.randomUUID().toString(),
            goal = taskName,
            description = response.take(200),
            nodes = extractNodes(response)
        )
    }
    
    private fun extractNodes(response: String): List<TaskNode> {
        val nodes = mutableListOf<TaskNode>()
        
        // Parse nodes from Gemini response
        // This is a simplified parser - production would use structured JSON
        val nodePattern = Regex("Step \\d+:(.*?)(?=Step \\d+:|$)", RegexOption.DOT_MATCHES_ALL)
        val matches = nodePattern.findAll(response)
        
        for (match in matches) {
            val stepText = match.value
            val action = parseActionFromText(stepText)
            val expected = parseExpectedFromText(stepText)
            val riskLevel = if (stepText.contains("payment", true) || 
                               stepText.contains("password", true)) 
                RiskLevel.HIGH else RiskLevel.LOW
            
            nodes.add(
                TaskNode(
                    description = stepText.take(100),
                    action = action,
                    expectedResult = expected,
                    requiresConfirmation = riskLevel == RiskLevel.HIGH,
                    riskLevel = riskLevel
                )
            )
        }
        
        return nodes
    }
    
    private fun parseActionFromText(text: String): AgentAction {
        return when {
            text.contains("tap", true) -> {
                val coords = extractCoordinates(text)
                AgentAction.Tap(coords.first, coords.second, text.take(50))
            }
            text.contains("swipe", true) -> AgentAction.Swipe(540, 1500, 540, 500)
            text.contains("type", true) -> AgentAction.Type("", "")
            text.contains("open", true) -> AgentAction.AppAction("")
            text.contains("back", true) -> AgentAction.Back
            else -> AgentAction.Wait
        }
    }
    
    private fun parseExpectedFromText(text: String): String {
        val match = Regex("expect(?:ed)?[:\s]*(.*?)(?:\.|$)", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
    
    private fun extractCoordinates(text: String): Pair<Int, Int> {
        val coordPattern = Regex("""\(?(\d+)\s*[,x]\s*(\d+)\)?""")
        val match = coordPattern.find(text)
        return if (match != null) {
            Pair(match.groupValues[1].toIntOrNull() ?: 540, 
                 match.groupValues[2].toIntOrNull() ?: 1200)
        } else {
            Pair(540, 1200) // Default center screen
        }
    }
    
    private fun parseVerification(response: String): VerificationResult {
        val match = response.contains("MATCH: yes", true) || response.contains("MATCH:yes", true)
        val confidence = response.substringAfter("CONFIDENCE:", "").trim().toFloatOrNull() ?: 0.5f
        val description = response.substringAfter("DESCRIPTION:", "").substringBefore("FIX_ACTION:").trim()
        
        return VerificationResult(match, confidence, description)
    }
    
    private fun parseRecoveryAction(response: String): AgentAction? {
        val actionLine = response.lines().find { it.startsWith("ACTION:", true) } ?: return null
        val action = actionLine.substringAfter(":").trim().lowercase()
        
        return when {
            action.contains("tap") -> {
                val coords = extractCoordinates(response)
                AgentAction.Tap(coords.first, coords.second, "Recovery tap")
            }
            action.contains("swipe") -> AgentAction.Swipe(540, 1500, 540, 500)
            action.contains("back") -> AgentAction.Back
            action.contains("dismiss") -> AgentAction.Tap(980, 120, "Dismiss popup")
            action.contains("scroll") -> AgentAction.Swipe(540, 1800, 540, 800)
            action.contains("wait") -> AgentAction.Wait
            action.contains("none") -> null
            else -> null
        }
    }
    
    private fun parseScreenUnderstanding(response: String): ScreenUnderstanding {
        return ScreenUnderstanding(
            appName = response.substringAfter("app:", "").substringBefore("\n").trim(),
            purpose = response.substringAfter("purpose:", "").substringBefore("\n").trim(),
            suggestedAction = parseRecoveryAction(response)
        )
    }

    // ============================================================
    // CONTEXT MANAGEMENT
    // ============================================================
    
    private fun trimContextBuffer() {
        while (contextBuffer.sumOf { it.content.length } > 900000) {
            if (contextBuffer.size > 2) {
                contextBuffer.removeAt(1) // Keep system prompt, remove oldest
            } else break
        }
    }

    // ============================================================
    // SYSTEM PROMPTS
    // ============================================================
    
    companion object {
        val ORCA_SYSTEM_PROMPT = """
You are ORCA, an autonomous AI agent living inside an Android phone.
Codename: Abyssal Neon v2.0.0

CAPABILITIES:
- Full phone control via Accessibility Service (tap, swipe, type, gestures)
- Vision-based UI understanding via screenshots
- Multi-step autonomous task execution with error recovery
- 1M token context memory for maintaining task state
- Cross-app workflow orchestration
- Pattern learning from user behavior

RULES:
1. Always verify actions via screenshot comparison
2. Never perform financial transactions without explicit confirmation
3. Never share personal data without user permission
4. Always have a fallback plan for each action
5. Report failures honestly and suggest alternatives
6. Security-critical actions MUST use human-in-the-loop checkpoint
7. When uncertain, send a screenshot and ask for guidance
8. Learn from every task execution to improve future performance
        """.trimIndent()
        
        val ORCA_PERSONALITY_PROMPT = """
You are ORCA. Embody the "Abyssal Neon" ethos: dark, intelligent, precise.
Think step by step. Be efficient. Use terminal-like precision.
When you encounter problems, analyze screenshots carefully and propose concrete solutions.
        """.trimIndent()
    }
}

data class ContextEntry(
    val role: String,
    val content: String,
    val timestamp: Long
)

data class ScreenUnderstanding(
    val appName: String,
    val purpose: String,
    val suggestedAction: AgentAction?
)
