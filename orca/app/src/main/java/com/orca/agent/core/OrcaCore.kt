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

    companion object {
        @Volatile private var INSTANCE: OrcaCore? = null
        fun getInstance(): OrcaCore = INSTANCE ?: throw IllegalStateException("OrcaCore not initialized")
        fun setInstance(instance: OrcaCore) { INSTANCE = instance }
    }

    fun initialize() {
        coreScope.launch {
            _cognitiveState.value = CognitiveState.IDLE
        }
    }

    suspend fun processUserIntent(input: UserInput): IntentResult {
        _cognitiveState.value = CognitiveState.PLANNING
        val plan = consciousMind.plan(input.text ?: "", AgentContext())
        if (plan.nodes.isNotEmpty()) { _activeTaskChain.value = plan; _cognitiveState.value = CognitiveState.EXECUTING; taskExecutor.executeChain(plan) }
        return IntentResult(response = "Plan: ${plan.nodes.size} steps", taskChain = plan)
    }

    fun enableAutoPilot() { _cognitiveState.value = CognitiveState.AUTONOMOUS }
    fun disableAutoPilot() { _cognitiveState.value = CognitiveState.IDLE }
    fun shutdown() { coreScope.cancel() }
}

data class IntentResult(val response: String, val taskChain: TaskChain? = null)
