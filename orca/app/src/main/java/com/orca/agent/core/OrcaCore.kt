package com.orca.agent.core

import com.orca.agent.brain.ConsciousMind
import com.orca.agent.brain.SubconsciousEngine
import com.orca.agent.memory.MemoryCortex
import com.orca.agent.agent.DigitalTwin
import com.orca.agent.data.database.OrcaDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrcaCore @Inject constructor(
    val consciousMind: ConsciousMind, val subconsciousEngine: SubconsciousEngine,
    val memoryCortex: MemoryCortex, val taskExecutor: TaskExecutor,
    val digitalTwin: DigitalTwin, val database: OrcaDatabase
) {
    private val coreScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _cognitiveState = MutableStateFlow(CognitiveState.IDLE)
    val cognitiveState: StateFlow<CognitiveState> = _cognitiveState.asStateFlow()
    private val _activeTaskChain = MutableStateFlow<TaskChain?>(null)
    val activeTaskChain: StateFlow<TaskChain?> = _activeTaskChain.asStateFlow()
    private val _thoughtStream = MutableSharedFlow<String>(replay = 200)
    val thoughtStream: SharedFlow<String> = _thoughtStream.asSharedFlow()
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        coreScope.launch {
            consciousMind.initialize(); subconsciousEngine.initialize()
            memoryCortex.initialize(); digitalTwin.loadProfile()
            isInitialized = true; _cognitiveState.value = CognitiveState.IDLE
        }
    }

    suspend fun processUserIntent(input: UserInput): IntentResult {
        _cognitiveState.value = CognitiveState.PLANNING
        val context = AgentContext(currentScreen = ScreenState.capture())
        val plan = consciousMind.plan(input.text ?: "", context)
        if (plan.nodes.isNotEmpty()) {
            _activeTaskChain.value = plan
            _cognitiveState.value = CognitiveState.EXECUTING
            taskExecutor.executeChain(plan)
        }
        return IntentResult(response = "Plan: ${plan.nodes.size} steps", taskChain = plan)
    }

    fun enableAutoPilot() { _cognitiveState.value = CognitiveState.AUTONOMOUS }
    fun disableAutoPilot() { _cognitiveState.value = CognitiveState.IDLE }
    fun shutdown() { coreScope.cancel() }
}

data class IntentResult(val response: String, val taskChain: TaskChain? = null)
