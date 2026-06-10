package com.orca.agent.core

data class TaskChain(
    val id: String,
    val goal: String,
    val description: String,
    val nodes: List<TaskNode>,
    val currentNodeIndex: Int = 0,
    val status: ChainStatus = ChainStatus.PENDING,
    val alternativePaths: Map<Int, List<TaskNode>> = emptyMap(), // Node index -> alternative sub-plans
    val recoveryAttempts: Int = 0,
    val maxRecoveryAttempts: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun currentNode(): TaskNode? = nodes.getOrNull(currentNodeIndex)
    fun progress(): Float = if (nodes.isEmpty()) 0f else 
        currentNodeIndex.toFloat() / nodes.size.toFloat()
    fun isComplete(): Boolean = currentNodeIndex >= nodes.size
}

data class TaskNode(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val action: AgentAction,
    val expectedResult: String, // What screen/text should appear after this action
    val preconditions: List<String> = emptyList(), // What must be true before executing
    val fallbackAction: AgentAction? = null,
    val alternativeNode: TaskNode? = null, // If this fails, try this alternative
    val status: NodeStatus = NodeStatus.PENDING,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val requiresConfirmation: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val timeoutMs: Long = 5000 // Max time to wait for expected result
)

enum class ChainStatus { 
    PENDING, IN_PROGRESS, AWAITING_RECOVERY, 
    AWAITING_CONFIRMATION, COMPLETED, FAILED, ABANDONED 
}

enum class NodeStatus { 
    PENDING, IN_PROGRESS, VERIFIED, 
    NEEDS_RECOVERY, FAILED, SKIPPED 
}
