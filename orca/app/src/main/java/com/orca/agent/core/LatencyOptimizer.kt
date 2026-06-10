package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatencyOptimizer @Inject constructor() {
    
    // Performance tracking
    private val actionTimings = mutableMapOf<String, MutableList<Long>>() // actionType -> durations
    private val _estimatedTimeRemaining = MutableStateFlow(0L)
    val estimatedTimeRemaining: StateFlow<Long> = _estimatedTimeRemaining.asStateFlow()
    
    // Adaptive timing
    private var baseWaitAfterAction = 500L // Start with 500ms
    private var waitAfterScroll = 300L
    private var waitAfterTap = 400L
    private var waitAfterType = 200L
    private var waitAfterAppLaunch = 1500L
    
    // ============================================================
    // PREDICTIVE EXECUTION (Do verification in parallel)
    // ============================================================
    
    suspend fun executeActionOptimized(
        action: AgentAction,
        verificationBlock: suspend () -> VerificationResult
    ): OptimizedResult {
        val startTime = System.currentTimeMillis()
        
        // Execute the action
        val actionSuccess = executeAction(action)
        val actionTime = System.currentTimeMillis() - startTime
        
        // Start verification IMMEDIATELY (don't wait for full UI settle)
        // Most UI changes are visible within 100ms
        delay(minimumWaitForAction(action))
        
        val verifStartTime = System.currentTimeMillis()
        val verification = verificationBlock()
        val verifTime = System.currentTimeMillis() - verifStartTime
        
        val totalTime = System.currentTimeMillis() - startTime
        
        // Record timing for adaptive optimization
        recordTiming(action, actionTime, verifTime)
        
        // If verification passed quickly, we can reduce future waits
        if (verification.matches && verification.confidence > 0.9f) {
            reduceWaitTime(action)
        }
        
        // If verification failed or was uncertain, increase future waits
        if (!verification.matches || verification.confidence < 0.7f) {
            increaseWaitTime(action)
        }
        
        return OptimizedResult(
            success = actionSuccess && verification.matches,
            actionTime = actionTime,
            verificationTime = verifTime,
            totalTime = totalTime,
            verification = verification
        )
    }

    // ============================================================
    // SPECULATIVE EXECUTION
    // ============================================================
    
    suspend fun executeWithSpeculation(
        primaryAction: AgentAction,
        nextLikelyAction: AgentAction?,
        verificationBlock: suspend () -> VerificationResult
    ): OptimizedResult {
        val result = executeActionOptimized(primaryAction, verificationBlock)
        
        // If primary succeeded and we know the next action, pre-compute it
        if (result.success && nextLikelyAction != null) {
            // We can't execute it yet, but we can prepare
            // e.g., pre-load app, pre-calculate coordinates
            prepareNextAction(nextLikelyAction)
        }
        
        return result
    }

    // ============================================================
    // BATCH VERIFICATION
    // ============================================================
    
    suspend fun batchVerify(
        actions: List<AgentAction>,
        batchVerificationBlock: suspend (List<AgentAction>) -> VerificationResult
    ): BatchResult {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<OptimizedResult>()
        
        // Execute all actions quickly
        for (action in actions) {
            executeAction(action)
            delay(50) // Minimal delay between actions
        }
        
        // Single verification for the batch
        val verification = batchVerificationBlock(actions)
        val totalTime = System.currentTimeMillis() - startTime
        
        // If verification fails, we need to identify which action failed
        if (!verification.matches) {
            return BatchResult(
                success = false,
                totalTime = totalTime,
                failedAtIndex = identifyFailedAction(actions),
                verification = verification
            )
        }
        
        return BatchResult(
            success = true,
            totalTime = totalTime,
            failedAtIndex = -1,
            verification = verification
        )
    }

    // ============================================================
    // ADAPTIVE WAIT TIME
    // ============================================================
    
    private fun minimumWaitForAction(action: AgentAction): Long {
        return when (action) {
            is AgentAction.Tap -> waitAfterTap
            is AgentAction.Swipe -> waitAfterScroll
            is AgentAction.Type -> waitAfterType
            is AgentAction.AppAction -> waitAfterAppLaunch
            is AgentAction.Back -> 200L
            is AgentAction.Home -> 300L
            else -> baseWaitAfterAction
        }
    }
    
    private fun reduceWaitTime(action: AgentAction) {
        when (action) {
            is AgentAction.Tap -> waitAfterTap = (waitAfterTap * 0.9).toLong().coerceAtLeast(100L)
            is AgentAction.Swipe -> waitAfterScroll = (waitAfterScroll * 0.9).toLong().coerceAtLeast(150L)
            is AgentAction.Type -> waitAfterType = (waitAfterType * 0.9).toLong().coerceAtLeast(50L)
            else -> baseWaitAfterAction = (baseWaitAfterAction * 0.95).toLong().coerceAtLeast(200L)
        }
    }
    
    private fun increaseWaitTime(action: AgentAction) {
        when (action) {
            is AgentAction.Tap -> waitAfterTap = (waitAfterTap * 1.2).toLong().coerceAtMost(2000L)
            is AgentAction.Swipe -> waitAfterScroll = (waitAfterScroll * 1.2).toLong().coerceAtMost(2000L)
            else -> baseWaitAfterAction = (baseWaitAfterAction * 1.1).toLong().coerceAtMost(3000L)
        }
    }

    // ============================================================
    // TIME ESTIMATION
    // ============================================================
    
    fun estimateRemainingTime(chain: TaskChain): Long {
        var estimated = 0L
        
        for (i in chain.currentNodeIndex until chain.nodes.size) {
            val node = chain.nodes[i]
            estimated += when (node.action) {
                is AgentAction.Tap -> waitAfterTap + 500
                is AgentAction.Swipe -> waitAfterScroll + 300
                is AgentAction.Type -> waitAfterType + 200
                is AgentAction.AppAction -> waitAfterAppLaunch + 1000
                is AgentAction.Wait -> 1000
                else -> baseWaitAfterAction + 500
            }
        }
        
        _estimatedTimeRemaining.value = estimated
        return estimated
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private suspend fun executeAction(action: AgentAction): Boolean {
        // Delegate to gesture engine
        return true // Simplified
    }
    
    private suspend fun prepareNextAction(action: AgentAction) {
        // Pre-compute coordinates, pre-load resources, etc.
    }
    
    private fun recordTiming(action: AgentAction, actionTime: Long, verifTime: Long) {
        val key = action.javaClass.simpleName
        actionTimings.getOrPut(key) { mutableListOf() }.add(actionTime + verifTime)
    }
    
    private fun identifyFailedAction(actions: List<AgentAction>): Int {
        // Binary search to find which action failed
        return -1
    }
}

data class OptimizedResult(
    val success: Boolean,
    val actionTime: Long,
    val verificationTime: Long,
    val totalTime: Long,
    val verification: VerificationResult
)

data class BatchResult(
    val success: Boolean,
    val totalTime: Long,
    val failedAtIndex: Int,
    val verification: VerificationResult
)
