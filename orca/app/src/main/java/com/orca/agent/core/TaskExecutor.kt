package com.orca.agent.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.orca.agent.brain.ConsciousMind
import com.orca.agent.execution.*
import com.orca.agent.memory.MemoryCortex
import com.orca.agent.security.SecurityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskExecutor @Inject constructor(
    private val gestureEngine: GestureEngine,
    private val appNavigator: AppNavigator,
    private val screenParser: ScreenParser,
    private val consciousMind: ConsciousMind,
    private val memoryCortex: MemoryCortex,
    private val securityManager: SecurityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()
    
    private val _currentNode = MutableStateFlow<TaskNode?>(null)
    val currentNode: StateFlow<TaskNode?> = _currentNode.asStateFlow()
    
    private val _thoughtStream = MutableSharedFlow<String>(replay = 200)
    val thoughtStream: SharedFlow<String> = _thoughtStream.asSharedFlow()
    
    private val recoveryCount = mutableMapOf<String, Int>()
    private val screenshotHistory = mutableListOf<ScreenState>()

    // ============================================================
    // PILLAR 1: SELF-DIRECTED GOAL EXECUTION
    // ============================================================
    
    suspend fun executeChain(chain: TaskChain): ExecutionResult {
        _executionState.value = ExecutionState.Running(chain.id)
        emitThought("🎯 Starting: ${chain.goal}")
        
        var currentChain = chain.copy(status = ChainStatus.IN_PROGRESS)
        screenshotHistory.clear()
        
        for (i in chain.currentNodeIndex until chain.nodes.size) {
            val node = currentChain.nodes[i]
            _currentNode.value = node
            
            emitThought("📍 Step ${i + 1}/${chain.nodes.size}: ${node.description}")
            
            // Check preconditions
            if (!checkPreconditions(node)) {
                emitThought("⚠️ Preconditions not met for: ${node.description}")
                val recoveryAction = handlePreconditionFailure(node, currentChain)
                if (recoveryAction != null) {
                    executeAction(recoveryAction)
                }
            }
            
            // Execute the node
            val result = executeNodeWithFullRecovery(node, currentChain, i)
            
            when (result) {
                is NodeExecutionResult.Success -> {
                    currentChain = currentChain.copy(
                        nodes = currentChain.nodes.toMutableList().apply {
                            this[i] = node.copy(status = NodeStatus.VERIFIED)
                        },
                        currentNodeIndex = i + 1
                    )
                    emitThought("✅ Done: ${node.description}")
                }
                
                is NodeExecutionResult.Recovered -> {
                    currentChain = currentChain.copy(
                        nodes = currentChain.nodes.toMutableList().apply {
                            this[i] = node.copy(
                                status = NodeStatus.VERIFIED,
                                retryCount = node.retryCount + 1
                            )
                        },
                        currentNodeIndex = i + 1,
                        recoveryAttempts = currentChain.recoveryAttempts + 1
                    )
                    emitThought("🔄 Recovered: ${node.description}")
                }
                
                is NodeExecutionResult.NeedsConfirmation -> {
                    _executionState.value = ExecutionState.AwaitingConfirmation(
                        chainId = chain.id,
                        nodeId = node.id,
                        question = result.question
                    )
                    return ExecutionResult.NeedsConfirmation(result.question, node.riskLevel)
                }
                
                is NodeExecutionResult.FailedPermanently -> {
                    emitThought("❌ Failed: ${node.description} - ${result.reason}")
                    
                    // Try alternative path if available
                    if (currentChain.alternativePaths.containsKey(i)) {
                        emitThought("🔀 Trying alternative path for step $i")
                        val altPath = currentChain.alternativePaths[i]!!
                        val altResult = executeAlternativePath(altPath, currentChain, i)
                        if (altResult is NodeExecutionResult.Success) {
                            currentChain = currentChain.copy(currentNodeIndex = i + 1)
                            continue
                        }
                    }
                    
                    // All paths exhausted - escalate
                    return ExecutionResult.Failure(
                        "Step ${i + 1} failed: ${node.description} - ${result.reason}",
                        canRecover = false
                    )
                }
            }
        }
        
        // All nodes completed
        val finalChain = currentChain.copy(status = ChainStatus.COMPLETED)
        
        // PILLAR 3: Record experience for learning
        scope.launch {
            reflectOnTask(TaskExperience(
                task = finalChain.goal,
                plan = finalChain,
                result = ExecutionResult.Success("Completed ${finalChain.nodes.size} steps"),
                screenshots = screenshotHistory.toList()
            ))
        }
        
        _executionState.value = ExecutionState.Completed(finalChain)
        emitThought("🏆 Task complete: ${chain.goal}")
        return ExecutionResult.Success("Completed ${chain.nodes.size} steps")
    }

    // ============================================================
    // FULL RECOVERY SYSTEM (PILLAR 2)
    // ============================================================
    
    private suspend fun executeNodeWithFullRecovery(
        node: TaskNode,
        chain: TaskChain,
        index: Int
    ): NodeExecutionResult {
        var currentRetries = node.retryCount
        
        while (currentRetries < node.maxRetries) {
            // Check for confirmation
            if (node.requiresConfirmation || securityManager.isSensitiveAction(node.description)) {
                return NodeExecutionResult.NeedsConfirmation(
                    "Orca needs confirmation: ${node.description}",
                    node.riskLevel
                )
            }
            
            // Capture BEFORE screenshot
            val beforeScreen = ScreenState.capture()
            screenshotHistory.add(beforeScreen)
            
            // Execute the action
            val actionSuccess = executeAction(node.action)
            
            if (!actionSuccess) {
                emitThought("⚠️ Action failed: ${node.description}")
                currentRetries++
                if (node.fallbackAction != null) {
                    emitThought("🔄 Trying fallback action")
                    executeAction(node.fallbackAction)
                }
                delay(500)
                continue
            }
            
            // Wait for UI update
            delay(800)
            
            // Capture AFTER screenshot
            val afterScreen = ScreenState.capture()
            screenshotHistory.add(afterScreen)
            
            // ============================================================
            // VERIFICATION LAYER (Level 1: Fast, Level 2: Gemini)
            // ============================================================
            val verification = verifyActionResult(node, beforeScreen, afterScreen)
            
            if (verification.matches && verification.confidence > 0.85f) {
                return NodeExecutionResult.Success
            }
            
            if (verification.confidence > 0.5f) {
                // Try correction
                verification.suggestedCorrection?.let { correction ->
                    emitThought("🔧 Applying correction: ${correction}")
                    executeAction(correction)
                    delay(500)
                    val correctedScreen = ScreenState.capture()
                    val reVerification = verifyActionResult(node, beforeScreen, correctedScreen)
                    if (reVerification.matches) {
                        return NodeExecutionResult.Recovered
                    }
                }
            }
            
            // ============================================================
            // SCREENSHOT-TO-GEMINI FALLBACK (Level 3)
            // ============================================================
            emitThought("🤖 Asking Gemini: What went wrong?")
            val recoveryAction = consciousMind.recover(
                error = "Expected: ${node.expectedResult}\nGot: ${afterScreen.describe()}",
                context = AgentContext(
                    currentScreen = afterScreen,
                    previousScreen = beforeScreen,
                    recentActions = listOf(node.action)
                )
            )
            
            if (recoveryAction != null) {
                executeAction(recoveryAction)
                delay(500)
                val recoveredScreen = ScreenState.capture()
                val reVerification = verifyActionResult(node, beforeScreen, recoveredScreen)
                if (reVerification.matches) {
                    return NodeExecutionResult.Recovered
                }
            }
            
            currentRetries++
        }
        
        return NodeExecutionResult.FailedPermanently(
            "Failed after ${currentRetries} attempts"
        )
    }

    // ============================================================
    // VERIFICATION SYSTEM
    // ============================================================
    
    private suspend fun verifyActionResult(
        node: TaskNode,
        before: ScreenState,
        after: ScreenState
    ): VerificationResult {
        // Level 1: Fast check - did the screen change?
        if (after.visibleText == before.visibleText && 
            after.clickableElements.size == before.clickableElements.size) {
            // Screen didn't change at all - probably a missed tap
            return VerificationResult(
                matches = false,
                confidence = 0.1f,
                description = "Screen unchanged",
                suggestedCorrection = node.action // Retry same action
            )
        }
        
        // Level 2: Text matching
        if (node.expectedResult.isNotBlank()) {
            val expectedLower = node.expectedResult.lowercase()
            val actualLower = after.visibleText.lowercase()
            
            if (actualLower.contains(expectedLower)) {
                return VerificationResult(
                    matches = true,
                    confidence = 0.9f,
                    description = "Expected text found"
                )
            }
            
            // Check clickable elements
            for (element in after.clickableElements) {
                if (element.text.lowercase().contains(expectedLower) ||
                    element.description.lowercase().contains(expectedLower)) {
                    return VerificationResult(
                        matches = true,
                        confidence = 0.85f,
                        description = "Expected element found: ${element.text}"
                    )
                }
            }
        }
        
        // Level 3: Screenshot-to-Gemini verification
        if (after.screenshotBase64.isNotBlank()) {
            return consciousMind.verify(node.expectedResult, after)
        }
        
        return VerificationResult(
            matches = false,
            confidence = 0.3f,
            description = "Verification inconclusive"
        )
    }

    // ============================================================
    // ACTION EXECUTION
    // ============================================================
    
    private suspend fun executeAction(action: AgentAction): Boolean {
        return when (action) {
            is AgentAction.Tap -> gestureEngine.tapAt(action.x, action.y)
            is AgentAction.Swipe -> gestureEngine.swipe(
                action.startX, action.startY,
                action.endX, action.endY,
                action.duration
            )
            is AgentAction.Type -> AccessibilityBridge.executeAction(action) is ActionResult.Success
            is AgentAction.LongPress -> gestureEngine.longPress(action.x, action.y)
            is AgentAction.AppAction -> appNavigator.openApp(action.packageName)
            is AgentAction.Back -> {
                AccessibilityBridge.getInstance()?.performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                )
                true
            }
            is AgentAction.Home -> {
                AccessibilityBridge.getInstance()?.performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
                )
                true
            }
            is AgentAction.Wait -> { delay(1000); true }
            is AgentAction.ScreenshotToGemini -> {
                // Handled by ConsciousMind
                true
            }
            is AgentAction.WaitForUserConfirmation -> {
                // Will pause execution and wait
                false
            }
            is AgentAction.Speak -> {
                // TTS handled by OrcaVoiceEngine
                true
            }
        }
    }

    // ============================================================
    // ALTERNATIVE PATH EXECUTION
    // ============================================================
    
    private suspend fun executeAlternativePath(
        altNodes: List<TaskNode>,
        chain: TaskChain,
        startIndex: Int
    ): NodeExecutionResult {
        for (node in altNodes) {
            val result = executeNodeWithFullRecovery(node, chain, startIndex)
            if (result !is NodeExecutionResult.Success && 
                result !is NodeExecutionResult.Recovered) {
                return result
            }
        }
        return NodeExecutionResult.Success
    }

    // ============================================================
    // PRECONDITION CHECKING
    // ============================================================
    
    private fun checkPreconditions(node: TaskNode): Boolean {
        if (node.preconditions.isEmpty()) return true
        
        val currentScreen = ScreenState.capture()
        val visibleText = currentScreen.visibleText.lowercase()
        
        for (precondition in node.preconditions) {
            if (!visibleText.contains(precondition.lowercase())) {
                return false
            }
        }
        return true
    }
    
    private suspend fun handlePreconditionFailure(
        node: TaskNode,
        chain: TaskChain
    ): AgentAction? {
        // Try to navigate to the right screen
        return consciousMind.recover(
            "Precondition failed: ${node.preconditions}",
            AgentContext(currentScreen = ScreenState.capture())
        )
    }

    // ============================================================
    // PILLAR 3: REFLECTION & LEARNING
    // ============================================================
    
    private suspend fun reflectOnTask(experience: TaskExperience) {
        val lessons = consciousMind.reflect(experience)
        for (lesson in lessons) {
            emitThought("📝 Learned: $lesson")
        }
        
        // Store in memory for future improvement
        memoryCortex.episodicJournal.recordEvent(
            com.orca.agent.brain.Event.TaskCompleted(
                taskName = experience.task,
                duration = System.currentTimeMillis() - experience.timestamp,
                success = experience.result is ExecutionResult.Success
            )
        )
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private suspend fun emitThought(thought: String) {
        _thoughtStream.emit("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $thought")
    }
    
    fun confirmAction(chainId: String, nodeId: String) {
        scope.launch {
            _executionState.value = ExecutionState.Running(chainId)
            // Resume execution - in production, this would continue from the paused node
        }
    }
    
    fun cancelChain(chainId: String) {
        _executionState.value = ExecutionState.Cancelled(chainId)
    }
}

// ============================================================
// EXECUTION STATE & RESULTS
// ============================================================

sealed class ExecutionState {
    object Idle : ExecutionState()
    data class Running(val chainId: String) : ExecutionState()
    data class AwaitingConfirmation(val chainId: String, val nodeId: String, val question: String) : ExecutionState()
    data class Completed(val chain: TaskChain) : ExecutionState()
    data class Failed(val chainId: String, val error: String) : ExecutionState()
    data class Cancelled(val chainId: String) : ExecutionState()
}

sealed class NodeExecutionResult {
    object Success : NodeExecutionResult()
    object Recovered : NodeExecutionResult()
    data class NeedsConfirmation(val question: String, val riskLevel: RiskLevel) : NodeExecutionResult()
    data class FailedPermanently(val reason: String) : NodeExecutionResult()
}
// Add this to the existing TaskExecutor class

// ============================================================
// PAUSE/RESUME SUPPORT
// ============================================================

private var isPaused = false
private var pauseLock = java.util.concurrent.locks.ReentrantLock()
private var pauseCondition = pauseLock.newCondition()

suspend fun checkPauseState() {
    if (isPaused) {
        // Wait until resumed
        withContext(Dispatchers.IO) {
            pauseLock.lock()
            try {
                while (isPaused) {
                    pauseCondition.await()
                }
            } finally {
                pauseLock.unlock()
            }
        }
    }
}

fun pauseExecution(taskName: String, currentStep: Int, totalSteps: Int) {
    isPaused = true
    pauseResumeNotificationManager.showPausedNotification(
        taskName = taskName,
        currentStep = currentStep,
        totalSteps = totalSteps,
        pausedAt = System.currentTimeMillis()
    )
}

fun resumeExecution() {
    pauseLock.lock()
    try {
        isPaused = false
        pauseCondition.signalAll()
    } finally {
        pauseLock.unlock()
    }
}

// Modified executeNode method to check pause state
private suspend fun executeNodeWithFullRecovery(
    node: TaskNode,
    chain: TaskChain,
    index: Int
): NodeExecutionResult {
    // CHECK PAUSE STATE before each action
    checkPauseState()
    
    // ... rest of existing implementation
    
    // Also check after recovery
    checkPauseState()
}
