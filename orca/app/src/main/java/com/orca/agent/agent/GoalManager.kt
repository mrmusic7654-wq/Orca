package com.orca.agent.agent

import com.orca.agent.core.Goal
import com.orca.agent.brain.ConsciousMind
import com.orca.agent.memory.MemoryCortex
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalManager @Inject constructor(
    private val consciousMind: ConsciousMind,
    private val memoryCortex: MemoryCortex
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _activeGoals = MutableStateFlow<List<Goal>>(emptyList())
    val activeGoals: StateFlow<List<Goal>> = _activeGoals.asStateFlow()
    
    private val _completedGoals = MutableStateFlow<List<Goal>>(emptyList())
    val completedGoals: StateFlow<List<Goal>> = _completedGoals.asStateFlow()
    
    private val _pendingSuggestions = MutableStateFlow<List<Goal>>(emptyList())
    val pendingSuggestions: StateFlow<List<Goal>> = _pendingSuggestions.asStateFlow()

    fun initialize() {
        scope.launch {
            // Load saved goals from memory
        }
    }

    // ============================================================
    // GOAL DISCOVERY FROM PATTERNS
    // ============================================================
    
    suspend fun updateGoalsFromPatterns(patterns: List<BehaviorPattern>) {
        val newSuggestions = mutableListOf<Goal>()
        
        for (pattern in patterns) {
            if (pattern.confidence > 0.8f && pattern.frequency >= 3) {
                val goal = Goal(
                    id = java.util.UUID.randomUUID().toString(),
                    description = pattern.suggestedGoal(),
                    priority = pattern.confidence,
                    isActive = false
                )
                newSuggestions.add(goal)
            }
        }
        
        _pendingSuggestions.value = newSuggestions
    }

    // ============================================================
    // GOAL MATCHING
    // ============================================================
    
    fun findSimilarGoal(userInput: String): Goal? {
        val inputLower = userInput.lowercase()
        
        // Check active goals
        for (goal in _activeGoals.value) {
            if (goal.description.lowercase().contains(inputLower) ||
                inputLower.contains(goal.description.lowercase())) {
                return goal
            }
        }
        
        // Check completed goals (for recurring tasks)
        for (goal in _completedGoals.value) {
            if (goal.description.lowercase().contains(inputLower)) {
                return goal.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    completedAt = null
                )
            }
        }
        
        return null
    }

    // ============================================================
    // GOAL LIFECYCLE
    // ============================================================
    
    fun markGoalCompleted(goalDescription: String) {
        val goals = _activeGoals.value.toMutableList()
        val completed = _completedGoals.value.toMutableList()
        
        val index = goals.indexOfFirst { it.description == goalDescription }
        if (index >= 0) {
            val goal = goals.removeAt(index).copy(
                isActive = false,
                completedAt = System.currentTimeMillis()
            )
            completed.add(0, goal)
            
            _activeGoals.value = goals
            _completedGoals.value = completed.take(100) // Keep last 100
        }
    }
    
    fun acceptSuggestion(goalId: String) {
        val suggestions = _pendingSuggestions.value.toMutableList()
        val index = suggestions.indexOfFirst { it.id == goalId }
        if (index >= 0) {
            val goal = suggestions.removeAt(index).copy(isActive = true)
            _activeGoals.value = _activeGoals.value + goal
            _pendingSuggestions.value = suggestions
        }
    }
    
    fun rejectSuggestion(goalId: String) {
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.id != goalId }
    }
}

data class BehaviorPattern(
    val description: String,
    val frequency: Int, // How many times observed
    val confidence: Float, // How confident we are this is a real pattern
    val timeOfDay: String?,
    val appPackage: String?,
    val actionSequence: List<String>
) {
    fun suggestedGoal(): String {
        return when {
            appPackage != null && timeOfDay != null -> 
                "Prepare $appPackage at $timeOfDay"
            actionSequence.isNotEmpty() -> 
                "Automate: ${actionSequence.joinToString(" → ")}"
            else -> description
        }
    }
}
