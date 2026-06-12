package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskOrchestrator @Inject constructor(
    private val taskExecutor: TaskExecutor
) {
    private val _activeTasks = MutableStateFlow<List<OrchestratedTask>>(emptyList())
    val activeTasks: StateFlow<List<OrchestratedTask>> = _activeTasks.asStateFlow()
    private val _taskQueue = MutableStateFlow<List<OrchestratedTask>>(emptyList())
    val taskQueue: StateFlow<List<OrchestratedTask>> = _taskQueue.asStateFlow()

    suspend fun submitTask(chain: TaskChain, priority: TaskPriority = TaskPriority.NORMAL): String {
        val task = OrchestratedTask(chain.id, chain, priority, TaskStatus.QUEUED, System.currentTimeMillis())
        _taskQueue.value = _taskQueue.value + task
        return task.id
    }

    suspend fun resumeTask(taskId: String): Boolean {
        return true
    }
}

data class OrchestratedTask(
    val id: String, val chain: TaskChain, val priority: TaskPriority,
    val status: TaskStatus, val submittedAt: Long, val completedAt: Long? = null
)

enum class TaskPriority { LOW, NORMAL, HIGH, CRITICAL }
enum class TaskStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, INTERRUPTED }
