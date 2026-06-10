package com.orca.agent.core

import com.orca.agent.brain.ConsciousMind
import com.orca.agent.brain.SubconsciousEngine
import com.orca.agent.memory.MemoryCortex
import com.orca.agent.agent.DigitalTwin
import com.orca.agent.data.database.OrcaDatabase
import com.orca.agent.agent.GoalManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrcaCore @Inject constructor(
    val consciousMind: ConsciousMind,
    val subconsciousEngine: SubconsciousEngine,
    val memoryCortex: MemoryCortex,
    val taskExecutor: TaskExecutor,
    val digitalTwin: DigitalTwin,
    val goalManager: GoalManager,
    val database: OrcaDatabase
) {
    private val coreScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _cognitiveState = MutableStateFlow(CognitiveState.IDLE)
    val cognitiveState: StateFlow<CognitiveState> = _cognitiveState.asStateFlow()
    
    private val _activeTaskChain = MutableStateFlow<TaskChain?>(null)
    val activeTaskChain: StateFlow<TaskChain?> = _activeTaskChain.asStateFlow()
    
    private val _thoughtStream = MutableSharedFlow<String>(replay = 200)
    val thoughtStream: SharedFlow<String> = _thoughtStream.asSharedFlow()
    
    private val _dailySummary = MutableStateFlow<String?>(null)
    val dailySummary: StateFlow<String?> = _dailySummary.asStateFlow()
    
    private var isInitialized = false
    private var autoPilotLevel = 0 // 0=manual, 1=confirm, 2=semi-auto, 3=full auto

    fun initialize() {
        if (isInitialized) return
        
        coreScope.launch {
            emitThought("🚀 Orca Core initializing...")
            
            consciousMind.initialize()
            subconsciousEngine.initialize()
            memoryCortex.initialize()
            digitalTwin.loadProfile()
            goalManager.initialize()
            
            isInitialized = true
            _cognitiveState.value = CognitiveState.IDLE
            emitThought("✅ Orca is alive. Abyssal Neon v2.0 online.")
            
            // Start passive observation for pattern learning
            startPassiveObservation()
        }
    }

    // ============================================================
    // PILLAR 1: GOAL-DRIVEN EXECUTION
    // ============================================================
    
    suspend fun processUserIntent(input: UserInput): IntentResult {
        _cognitiveState.value = CognitiveState.PLANNING
        emitThought("📋 Processing: ${input.summarize()}")
        
        // Build rich context
        val context = AgentContext(
            currentScreen = ScreenState.capture(),
            memoryItems = memoryCortex.getRelevantMemories(input.text ?: "", 10),
            userProfile = digitalTwin.getProfile()
        )
        
        // Check if this is a known goal
        val existingGoal = goalManager.findSimilarGoal(input.text ?: "")
        if (existingGoal != null) {
            emitThought("🎯 Found existing goal: ${existingGoal.description}")
            context.copy(activeGoal = existingGoal)
        }
        
        // Generate plan via ConsciousMind (Gemini)
        val plan = consciousMind.plan(input.text ?: "", context)
        
        if (plan.nodes.isEmpty()) {
            return IntentResult(
                response = "I couldn't create a plan for that. Could you provide more details?",
                requiresInput = true
            )
        }
        
        // Check for sensitive actions
        val hasSensitiveActions = plan.nodes.any { 
            it.requiresConfirmation || it.riskLevel == RiskLevel.HIGH 
        }
        
        if (hasSensitiveActions && autoPilotLevel < 2) {
            return IntentResult(
                response = "This task contains sensitive actions. I'll need your confirmation.",
                taskChain = plan,
                requiresInput = true
            )
        }
        
        // Execute autonomously
        _activeTaskChain.value = plan
        _cognitiveState.value = CognitiveState.EXECUTING
        
        coreScope.launch {
            val result = taskExecutor.executeChain(plan)
            handleExecutionResult(result, plan)
        }
        
        return IntentResult(
            response = "I've created a ${plan.nodes.size}-step plan. Executing now.",
            taskChain = plan,
            requiresInput = false
        )
    }

    // ============================================================
    // PASSIVE OBSERVATION FOR PATTERN LEARNING
    // ============================================================
    
    private fun startPassiveObservation() {
        coreScope.launch {
            while (isActive) {
                delay(30000) // Check every 30 seconds
                
                if (_cognitiveState.value == CognitiveState.IDLE) {
                    val currentScreen = ScreenState.capture()
                    if (!currentScreen.isEmpty()) {
                        subconsciousEngine.recordObservation(currentScreen)
                    }
                }
            }
        }
        
        // Weekly reflection
        coreScope.launch {
            while (isActive) {
                delay(7 * 24 * 60 * 60 * 1000L) // Every 7 days
                performWeeklyReflection()
            }
        }
    }
    
    private suspend fun performWeeklyReflection() {
        emitThought("📊 Performing weekly reflection...")
        val patterns = subconsciousEngine.extractPatterns()
        goalManager.updateGoalsFromPatterns(patterns)
        
        val summary = consciousMind.generateDailySummary()
        _dailySummary.value = summary
        emitThought("📝 Weekly summary ready")
    }

    // ============================================================
    // EXECUTION RESULT HANDLING
    // ============================================================
    
    private suspend fun handleExecutionResult(result: ExecutionResult, plan: TaskChain) {
        when (result) {
            is ExecutionResult.Success -> {
                _cognitiveState.value = CognitiveState.IDLE
                _activeTaskChain.value = null
                goalManager.markGoalCompleted(plan.goal)
            }
            is ExecutionResult.Failure -> {
                _cognitiveState.value = CognitiveState.ERROR
                if (result.canRecover) {
                    emitThought("🔄 Attempting recovery...")
                    // Re-plan from current state
                    val newPlan = consciousMind.plan(
                        "Recover from failure: ${result.error}",
                        AgentContext(currentScreen = ScreenState.capture())
                    )
                    if (newPlan.nodes.isNotEmpty()) {
                        _activeTaskChain.value = newPlan
                        taskExecutor.executeChain(newPlan)
                    }
                }
            }
            is ExecutionResult.NeedsConfirmation -> {
                _cognitiveState.value = CognitiveState.AWAITING_CONFIRMATION
            }
        }
    }

    // ============================================================
    // AUTOPILOT MANAGEMENT
    // ============================================================
    
    fun enableAutoPilot(level: Int = 1) {
        autoPilotLevel = level
        _cognitiveState.value = when (level) {
            3 -> CognitiveState.AUTONOMOUS
            else -> CognitiveState.IDLE
        }
        subconsciousEngine.enableContinuousMonitoring()
        emitThought("🤖 AutoPilot level $level engaged")
    }
    
    fun disableAutoPilot() {
        autoPilotLevel = 0
        _cognitiveState.value = CognitiveState.IDLE
        subconsciousEngine.disableContinuousMonitoring()
        emitThought("🔒 AutoPilot disengaged")
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private suspend fun emitThought(thought: String) {
        _thoughtStream.emit("[OrcaCore] $thought")
    }
    
    fun shutdown() {
        coreScope.cancel()
        _cognitiveState.value = CognitiveState.SHUTDOWN
    }
}

data class IntentResult(
    val response: String,
    val taskChain: TaskChain? = null,
    val requiresInput: Boolean = false
)
