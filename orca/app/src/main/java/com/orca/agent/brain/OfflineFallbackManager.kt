package com.orca.agent.brain

import com.orca.agent.core.*
import com.orca.agent.memory.MemoryCortex
import com.orca.agent.memory.MemoryType
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFallbackManager @Inject constructor(
    private val memoryCortex: MemoryCortex,
    private val contextWindowManager: ContextWindowManager
) {
    // Cached knowledge that works offline
    private val cachedWorkflows = mutableMapOf<String, List<TaskNode>>()
    private val cachedUIKnowledge = mutableMapOf<String, UICache>()
    private val cachedRecoveryActions = mutableMapOf<String, List<AgentAction>>()
    
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // ============================================================
    // OFFLINE WORKFLOW EXECUTION
    // ============================================================
    
    fun getCachedWorkflow(taskDescription: String): List<TaskNode>? {
        // Look for exact match
        cachedWorkflows[taskDescription]?.let { return it }
        
        // Look for similar tasks using keyword matching
        val keywords = taskDescription.lowercase().split(" ")
        for ((cachedTask, workflow) in cachedWorkflows) {
            val cachedKeywords = cachedTask.lowercase().split(" ")
            val overlap = keywords.count { it in cachedKeywords }
            if (overlap.toFloat() / keywords.size > 0.6f) {
                return workflow // Return closest match
            }
        }
        
        return null
    }
    
    fun cacheWorkflow(taskDescription: String, nodes: List<TaskNode>) {
        cachedWorkflows[taskDescription] = nodes
        // Keep cache bounded
        if (cachedWorkflows.size > 100) {
            val oldest = cachedWorkflows.keys.first()
            cachedWorkflows.remove(oldest)
        }
    }

    // ============================================================
    // OFFLINE UI KNOWLEDGE
    // ============================================================
    
    fun getUIElementLocation(appPackage: String, elementDescription: String): Pair<Int, Int>? {
        val cache = cachedUIKnowledge[appPackage] ?: return null
        return cache.elements[elementDescription.lowercase()]
    }
    
    fun cacheUIElement(appPackage: String, elementDescription: String, x: Int, y: Int) {
        val cache = cachedUIKnowledge.getOrPut(appPackage) { UICache() }
        cache.elements[elementDescription.lowercase()] = Pair(x, y)
        cache.lastUpdated = System.currentTimeMillis()
    }
    
    fun isUICacheStale(appPackage: String): Boolean {
        val cache = cachedUIKnowledge[appPackage] ?: return true
        // Cache expires after 7 days (apps might update)
        return System.currentTimeMillis() - cache.lastUpdated > 7 * 24 * 60 * 60 * 1000L
    }

    // ============================================================
    // OFFLINE RECOVERY
    // ============================================================
    
    fun getCachedRecovery(errorPattern: String): AgentAction? {
        for ((pattern, actions) in cachedRecoveryActions) {
            if (errorPattern.contains(pattern, ignoreCase = true)) {
                return actions.firstOrNull()
            }
        }
        return null
    }
    
    fun cacheRecoveryAction(errorPattern: String, action: AgentAction) {
        val actions = cachedRecoveryActions.getOrPut(errorPattern) { mutableListOf() }
        if (actions.size < 3) { // Keep top 3
            actions.add(0, action)
        }
    }

    // ============================================================
    // OFFLINE MODE MANAGEMENT
    // ============================================================
    
    fun enableOfflineMode() {
        _isOfflineMode.value = true
    }
    
    fun disableOfflineMode() {
        _isOfflineMode.value = false
    }
    
    fun canOperateOffline(taskDescription: String): Boolean {
        // Check if we have cached knowledge for this task
        return getCachedWorkflow(taskDescription) != null
    }

    // ============================================================
    // COMMON OFFLINE FALLBACKS
    // ============================================================
    
    fun getCommonRecoveryActions(): List<AgentAction> {
        return listOf(
            AgentAction.Back,
            AgentAction.Wait,
            AgentAction.Tap(980, 120, "Dismiss potential popup"), // Top-right corner
            AgentAction.Swipe(540, 1800, 540, 800, 300), // Scroll up
            AgentAction.Swipe(540, 800, 540, 1800, 300), // Scroll down
        )
    }
    
    fun getAppRestartAction(packageName: String): List<AgentAction> {
        return listOf(
            AgentAction.Home,
            AgentAction.Wait,
            AgentAction.AppAction(packageName)
        )
    }
}

data class UICache(
    val elements: MutableMap<String, Pair<Int, Int>> = mutableMapOf(),
    var lastUpdated: Long = System.currentTimeMillis()
)
