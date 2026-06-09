// app/src/main/java/com/orca/agent/core/TaskChain.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class TaskChain(
    val id: String,
    val name: String,
    val description: String,
    val nodes: List<TaskNode>,
    val currentNodeIndex: Int = 0,
    val status: ChainStatus = ChainStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun summarize(): String {
        return "TaskChain[$name]: ${nodes.size} steps, " +
               "current: ${nodes.getOrNull(currentNodeIndex)?.description ?: "none"}, " +
               "completed: ${nodes.count { it.status == NodeStatus.COMPLETED }}/${nodes.size}"
    }
    
    companion object {
        fun fromJson(json: String): TaskChain {
            // Parse Gemini's JSON response into a TaskChain
            return TaskChain(
                id = java.util.UUID.randomUUID().toString(),
                name = "Parsed Task",
                description = "",
                nodes = emptyList()
            )
        }
    }
}

data class TaskNode(
    val id: String,
    val description: String,
    val action: AgentAction,
    val expectedResult: String,
    val fallbackAction: AgentAction? = null,
    val status: NodeStatus = NodeStatus.PENDING,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val requiresConfirmation: Boolean = false
)

enum class ChainStatus { PENDING, IN_PROGRESS, PAUSED, COMPLETED, FAILED }
enum class NodeStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED }
