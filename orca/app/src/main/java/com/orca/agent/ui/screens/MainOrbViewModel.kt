package com.orca.agent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orca.agent.core.*
import com.orca.agent.brain.SubconsciousEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainOrbViewModel(
    private val orcaCore: OrcaCore = OrcaCore.getInstance(),
    private val subconsciousEngine: SubconsciousEngine = SubconsciousEngine.getInstance()
) : ViewModel() {

    val cognitiveState: StateFlow<CognitiveState> = orcaCore.cognitiveState
    val thoughtStream: StateFlow<String> = orcaCore.thoughtStream.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val thoughtStreamFlow: SharedFlow<String> = orcaCore.thoughtStream
    private val _autoPilotEnabled = MutableStateFlow(false)
    val autoPilotEnabled: StateFlow<Boolean> = _autoPilotEnabled.asStateFlow()
    val threats = subconsciousEngine.threats
    private val _activeTask = MutableStateFlow<ActiveTaskSummary?>(null)
    val activeTask: StateFlow<ActiveTaskSummary?> = _activeTask.asStateFlow()

    init {
        viewModelScope.launch {
            orcaCore.activeTaskChain.collect { chain ->
                chain?.let { _activeTask.value = ActiveTaskSummary(it.id, it.goal, it.nodes.count { n -> n.status == NodeStatus.VERIFIED }, it.nodes.size) }
            }
        }
    }

    fun processTextInput(text: String) {
        viewModelScope.launch { orcaCore.processUserIntent(UserInput(text = text, type = InputType.TEXT)) }
    }

    fun toggleAutoPilot(enabled: Boolean) {
        _autoPilotEnabled.value = enabled
        if (enabled) orcaCore.enableAutoPilot() else orcaCore.disableAutoPilot()
    }
}

data class ActiveTaskSummary(val id: String, val name: String, val completedSteps: Int, val totalSteps: Int)
