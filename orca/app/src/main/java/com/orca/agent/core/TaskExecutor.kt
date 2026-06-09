// app/src/main/java/com/orca/agent/core/TaskExecutor.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.core

import com.orca.agent.execution.*
import com.orca.agent.brain.ConsciousMind
import com.orca.agent.memory.MemoryCortex
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
    private val memoryCortex: MemoryCortex
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()
    
    private val _currentNode = MutableStateFlow<TaskNode?>(null)
    val currentNode: StateFlow<TaskNode?> = _currentNode.asStateFlow()
    
    suspend fun executeChain(chain: TaskChain): TaskChain {
        _executionState.value = ExecutionState.Running(chain.id)
        
        var currentChain = chain.copy(status = ChainStatus.IN_PROGRESS)
        
        for (i in chain.currentNodeIndex until chain.nodes.size) {
            val node = chain.nodes[i]
            _currentNode.value = node
            
            val result = executeNode(node, currentChain)
            
            currentChain = when (result) {
                is NodeResult.Success -> {
                    currentChain.copy(
                        nodes = currentChain.nodes.toMutableList().apply {
                            this[i] = node.copy(status = NodeStatus.COMPLETED)
                        },
                        currentNodeIndex = i + 1
                    )
                }
                is NodeResult.NeedsConfirmation -> {
                    _executionState.value = ExecutionState.AwaitingConfirmation(
                        chainId = chain.id,
                        nodeId = node.id,
                        question = result.question
                    )
                    return currentChain.copy(
                        status = ChainStatus.PAUSED,
                        currentNodeIndex = i
                    )
                }
                is NodeResult.Failure -> {
                    if (node.retryCount < node.maxRetries && node.fallbackAction != null) {
                        // Retry with fallback
                        val fallbackNode = node.copy(
                            action = node.fallbackAction,
                            retryCount = node.retryCount + 1
                        )
                        val fallbackResult = executeNode(fallbackNode, currentChain)
                        if (fallbackResult is NodeResult.Success) {
                            currentChain.copy(
                                nodes = currentChain.nodes.toMutableList().apply {
                                    this[i] = fallbackNode.copy(status = NodeStatus.COMPLETED)
                                },
                                currentNodeIndex = i + 1
                            )
                        } else {
                            currentChain.copy(
                                nodes = currentChain.nodes.toMutableList().apply {
                                    this[i] = node.copy(status = NodeStatus.FAILED)
                                },
                                status = ChainStatus.FAILED
                            )
                        }
                    } else {
                        currentChain.copy(
                            nodes = currentChain.nodes.toMutableList().apply {
                                this[i] = node.copy(status = NodeStatus.FAILED)
                            },
                            status = ChainStatus.FAILED
                        )
                    }
                }
            }
        }
        
        // All nodes completed
        val finalChain = currentChain.copy(
            status = if (currentChain.status != ChainStatus.FAILED) 
                ChainStatus.COMPLETED 
            else 
                ChainStatus.FAILED
        )
        
        _executionState.value = ExecutionState.Completed(finalChain)
        return finalChain
    }
    
    private suspend fun executeNode(node: TaskNode, chain: TaskChain): NodeResult {
        // Pre-action: Check if confirmation needed
        if (node.requiresConfirmation) {
            return NodeResult.NeedsConfirmation(
                "Orca needs confirmation to: ${node.description}"
            )
        }
        
        // Execute the action
        val actionResult = when (node.action) {
            is AgentAction.Tap -> gestureEngine.tapAt(node.action.x, node.action.y)
            is AgentAction.Swipe -> gestureEngine.swipeUp()
            is AgentAction.Type -> AccessibilityBridge.executeAction(node.action) is ActionResult.Success
            is AgentAction.Back -> AccessibilityBridge.executeAction(node.action) is ActionResult.Success
            is AgentAction.Home -> AccessibilityBridge.executeAction(node.action) is ActionResult.Success
            is AgentAction.AppAction -> appNavigator.openApp(node.action.packageName)
            is AgentAction.Wait -> {
                delay(1000)
                true
            }
            else -> false
        }
        
        if (!actionResult) {
            return NodeResult.Failure("Action failed: ${node.description}")
        }
        
        // Post-action: Verify expected screen change
        delay(500) // Wait for UI to update
        val verification = verifyActionResult(node)
        
        return if (verification) {
            NodeResult.Success
        } else {
            NodeResult.Failure("Verification failed: ${node.expectedResult}")
        }
    }
    
    private suspend fun verifyActionResult(node: TaskNode): Boolean {
        val currentScreen = AccessibilityBridge.screenState.first() ?: return false
        
        // Check if the expected result matches current screen state
        return currentScreen.visibleText.contains(node.expectedResult, ignoreCase = true) ||
               currentScreen.clickableElements.any { 
                   it.description.contains(node.expectedResult, ignoreCase = true) 
               }
    }
    
    fun confirmAction(chainId: String, nodeId: String) {
        scope.launch {
            // Resume execution after confirmation
        }
    }
    
    fun cancelChain(chainId: String) {
        _executionState.value = ExecutionState.Cancelled(chainId)
    }
}

sealed class ExecutionState {
    object Idle : ExecutionState()
    data class Running(val chainId: String) : ExecutionState()
    data class AwaitingConfirmation(
        val chainId: String,
        val nodeId: String,
        val question: String
    ) : ExecutionState()
    data class Completed(val chain: TaskChain) : ExecutionState()
    data class Failed(val chainId: String, val error: String) : ExecutionState()
    data class Cancelled(val chainId: String) : ExecutionState()
}

sealed class NodeResult {
    object Success : NodeResult()
    data class Failure(val error: String) : NodeResult()
    data class NeedsConfirmation(val question: String) : NodeResult()
}
