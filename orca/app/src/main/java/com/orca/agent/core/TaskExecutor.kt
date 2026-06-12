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
    private val gestureEngine: GestureEngine, private val appNavigator: AppNavigator,
    private val screenParser: ScreenParser, private val consciousMind: ConsciousMind,
    private val memoryCortex: MemoryCortex
) {
    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    suspend fun executeChain(chain: TaskChain): ExecutionResult {
        _executionState.value = ExecutionState.Running(chain.id)
        var current = chain
        for ((i, node) in chain.nodes.withIndex()) {
            val ok = when (node.action) {
                is AgentAction.Tap -> gestureEngine.tapAt(node.action.x, node.action.y)
                is AgentAction.Swipe -> gestureEngine.swipeUp()
                is AgentAction.AppAction -> appNavigator.openApp(node.action.packageName)
                is AgentAction.Wait -> { delay(1000); true }
                else -> false
            }
            if (!ok) break
        }
        _executionState.value = ExecutionState.Completed(current)
        return ExecutionResult.Success("Done")
    }

    fun confirmAction(chainId: String, nodeId: String) {}
    fun cancelChain(chainId: String) { _executionState.value = ExecutionState.Cancelled(chainId) }
}

sealed class ExecutionState {
    object Idle : ExecutionState()
    data class Running(val chainId: String) : ExecutionState()
    data class AwaitingConfirmation(val chainId: String, val nodeId: String, val question: String) : ExecutionState()
    data class Completed(val chain: TaskChain) : ExecutionState()
    data class Cancelled(val chainId: String) : ExecutionState()
}
