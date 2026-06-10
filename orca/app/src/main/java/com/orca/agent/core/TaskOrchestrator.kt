package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskOrchestrator @Inject constructor(
    private val taskExecutor: TaskExecutor
) {
    // Only ONE task can control the phone at a time
    private val phoneLock = Mutex()
    
    private val _activeTasks = MutableStateFlow<List<OrchestratedTask>>(emptyList())
    val activeTasks: StateFlow<List<OrchestratedTask>> = _activeTasks.asStateFlow()
    
    private val _taskQueue = MutableStateFlow<List<OrchestratedTask>>(emptyList())
    val taskQueue: StateFlow<List<OrchestratedTask>> = _taskQueue.asStateFlow()
    
    private val _interruptionRequests = MutableSharedFlow<InterruptionRequest>(replay = 5)
    val interruptionRequests: SharedFlow<InterruptionRequest> = _interruptionRequests.asSharedFlow()
    
    private var currentTaskId: String? = null
    private var currentTaskJob: Job? = null

    // ============================================================
    // TASK QUEUING WITH PRIORITY
    // ============================================================
    
    suspend fun submitTask(chain: TaskChain, priority: TaskPriority = TaskPriority.NORMAL): String {
        val task = OrchestratedTask(
            id = chain.id,
            chain = chain,
            priority = priority,
            status = TaskStatus.QUEUED,
            submittedAt = System.currentTimeMillis()
        )
        
        val queue = _taskQueue.value.toMutableList()
        
        // Insert by priority
        val insertIndex = queue.indexOfFirst { it.priority.ordinal > priority.ordinal }
        if (insertIndex >= 0) {
            queue.add(insertIndex, task)
        } else {
            queue.add(task)
        }
        
        _taskQueue.value = queue
        
        // If no task is running, start this one
        if (currentTaskId == null) {
            processNextTask()
        }
        
        return task.id
    }

    // ============================================================
    // TASK INTERRUPTION
    // ============================================================
    
    suspend fun requestInterruption(
        taskId: String,
        reason: String,
        newGoal: String? = null
    ): Boolean {
        if (currentTaskId == taskId) {
            // This is the currently running task - interrupt it
            _interruptionRequests.emit(
                InterruptionRequest(
                    taskId = taskId,
                    reason = reason,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            // Cancel current execution
            currentTaskJob?.cancel()
            
            // Save partial progress
            val currentChain = taskExecutor.executionState.value
            if (currentChain is ExecutionState.Running) {
                // Task was mid-execution - save state
                savePartialProgress(taskId)
            }
            
            // If new goal provided, create new task
            if (newGoal != null) {
                val newChain = TaskChain(
                    id = java.util.UUID.randomUUID().toString(),
                    goal = newGoal,
                    description = "Interrupted from: $reason",
                    nodes = emptyList()
                )
                submitTask(newChain, TaskPriority.HIGH)
            }
            
            currentTaskId = null
            processNextTask()
            return true
        }
        
        // Task is queued - just remove it
        _taskQueue.value = _taskQueue.value.filter { it.id != taskId }
        return true
    }

    // ============================================================
    // TASK DRIFT DETECTION
    // ============================================================
    
    suspend fun detectTaskDrift(
        originalGoal: String,
        currentScreen: ScreenState,
        completedSteps: Int
    ): DriftAssessment {
        val driftIndicators = mutableListOf<String>()
        var driftScore = 0f
        
        // Check if we're in the wrong app
        val expectedApp = extractExpectedApp(originalGoal)
        if (expectedApp != null && currentScreen.currentApp != expectedApp) {
            driftIndicators.add("Expected app: $expectedApp, Actual: ${currentScreen.currentApp}")
            driftScore += 0.3f
        }
        
        // Check if visible text is completely unrelated to goal
        val goalKeywords = originalGoal.lowercase().split(" ")
        val screenKeywords = currentScreen.visibleText.lowercase().split(" ")
        val keywordOverlap = goalKeywords.count { it in screenKeywords }
        if (keywordOverlap.toFloat() / goalKeywords.size < 0.1f && completedSteps > 5) {
            driftIndicators.add("Screen content unrelated to goal after $completedSteps steps")
            driftScore += 0.4f
        }
        
        // Check for repetitive actions (sign of stuck loop)
        // This would need action history from TaskExecutor
        
        return DriftAssessment(
            isDrifting = driftScore > 0.5f,
            driftScore = driftScore,
            indicators = driftIndicators,
            recommendedAction = if (driftScore > 0.7f) {
                "ABANDON_AND_REPLAN"
            } else if (driftScore > 0.4f) {
                "PAUSE_AND_VERIFY"
            } else {
                "CONTINUE"
            }
        )
    }

    // ============================================================
    // STATE RECOVERY
    // ============================================================
    
    private suspend fun savePartialProgress(taskId: String) {
        // Save current progress so we can resume later
        val chain = _activeTasks.value.find { it.id == taskId }?.chain
        if (chain != null) {
            // Save to persistence
        }
    }
    
    suspend fun resumeTask(taskId: String): Boolean {
        // Find the task and resume from where it left off
        val task = _activeTasks.value.find { it.id == taskId } ?: return false
        return submitTask(task.chain, task.priority).isNotEmpty()
    }

    // ============================================================
    // QUEUE PROCESSING
    // ============================================================
    
    private suspend fun processNextTask() {
        phoneLock.withLock {
            val queue = _taskQueue.value
            if (queue.isEmpty()) {
                currentTaskId = null
                return
            }
            
            val nextTask = queue.first()
            _taskQueue.value = queue.drop(1)
            _activeTasks.value = _activeTasks.value + nextTask.copy(status = TaskStatus.RUNNING)
            
            currentTaskId = nextTask.id
            
            currentTaskJob = CoroutineScope(Dispatchers.Default).launch {
                val result = taskExecutor.executeChain(nextTask.chain)
                
                val updatedTasks = _activeTasks.value.map {
                    if (it.id == nextTask.id) {
                        it.copy(
                            status = if (result is ExecutionResult.Success) TaskStatus.COMPLETED 
                                    else TaskStatus.FAILED,
                            completedAt = System.currentTimeMillis()
                        )
                    } else it
                }
                _activeTasks.value = updatedTasks
                
                currentTaskId = null
                processNextTask() // Process next in queue
            }
        }
    }

    private fun extractExpectedApp(goal: String): String? {
        val appMap = mapOf(
            "uber" to "com.ubercab",
            "amazon" to "com.amazon.mShop.android.shopping",
            "youtube" to "com.google.android.youtube",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "settings" to "com.android.settings"
        )
        
        for ((keyword, packageName) in appMap) {
            if (goal.contains(keyword, ignoreCase = true)) {
                return packageName
            }
        }
        return null
    }
}

data class OrchestratedTask(
    val id: String,
    val chain: TaskChain,
    val priority: TaskPriority,
    val status: TaskStatus,
    val submittedAt: Long,
    val completedAt: Long? = null
)

enum class TaskPriority { LOW, NORMAL, HIGH, CRITICAL }
enum class TaskStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, INTERRUPTED }

data class InterruptionRequest(
    val taskId: String,
    val reason: String,
    val timestamp: Long
)

data class DriftAssessment(
    val isDrifting: Boolean,
    val driftScore: Float,
    val indicators: List<String>,
    val recommendedAction: String // "CONTINUE", "PAUSE_AND_VERIFY", "ABANDON_AND_REPLAN"
)
