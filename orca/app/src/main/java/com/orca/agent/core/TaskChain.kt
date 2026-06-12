package com.orca.agent.core

data class TaskChain(
    val id: String,
    val goal: String,
    val description: String = "",
    val nodes: List<TaskNode> = emptyList(),
    val currentNodeIndex: Int = 0,
    val status: ChainStatus = ChainStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskNode(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val action: AgentAction,
    val expectedResult: String = "",
    val fallbackAction: AgentAction? = null,
    val status: NodeStatus = NodeStatus.PENDING,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val requiresConfirmation: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

enum class ChainStatus { PENDING, IN_PROGRESS, AWAITING_RECOVERY, AWAITING_CONFIRMATION, COMPLETED, FAILED, ABANDONED }
enum class NodeStatus { PENDING, IN_PROGRESS, VERIFIED, NEEDS_RECOVERY, FAILED, SKIPPED }
