package com.orca.agent.brain

import com.orca.agent.core.AgentAction
import com.orca.agent.core.TaskNode
import com.orca.agent.memory.MemoryCortex
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFallbackManager @Inject constructor(
    private val memoryCortex: MemoryCortex
) {
    private val cachedWorkflows = mutableMapOf<String, List<TaskNode>>()
    private val cachedRecoveryActions = mutableMapOf<String, List<AgentAction>>()
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun getCachedWorkflow(taskDescription: String): List<TaskNode>? {
        return cachedWorkflows[taskDescription]
            ?: cachedWorkflows.entries.find { taskDescription.contains(it.key, true) }?.value
    }

    fun cacheWorkflow(taskDescription: String, nodes: List<TaskNode>) {
        cachedWorkflows[taskDescription] = nodes
        if (cachedWorkflows.size > 100) {
            val oldest = cachedWorkflows.keys.first()
            cachedWorkflows.remove(oldest)
        }
    }

    fun getCachedRecovery(errorPattern: String): AgentAction? {
        return cachedRecoveryActions.entries
            .find { errorPattern.contains(it.key, true) }?.value?.firstOrNull()
    }

    fun cacheRecoveryAction(errorPattern: String, action: AgentAction) {
        cachedRecoveryActions.getOrPut(errorPattern) { mutableListOf() }.apply { add(0, action) }
    }

    fun enableOfflineMode() { _isOfflineMode.value = true }
    fun disableOfflineMode() { _isOfflineMode.value = false }
    fun canOperateOffline(taskDescription: String): Boolean = cachedWorkflows.containsKey(taskDescription)
}
