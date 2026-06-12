package com.orca.agent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orca.agent.core.*
import com.orca.agent.brain.SubconsciousEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainOrbViewModel @Inject constructor(
    private val orcaCore: OrcaCore,
    private val subconsciousEngine: SubconsciousEngine
) : ViewModel() {

    val cognitiveState: StateFlow<CognitiveState> = orcaCore.cognitiveState

    val thoughtStream: StateFlow<List<String>> = orcaCore.thoughtStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val thoughtStreamFlow: SharedFlow<String> = orcaCore.thoughtStream

    private val _autoPilotEnabled = MutableStateFlow(false)
    val autoPilotEnabled: StateFlow<Boolean> = _autoPilotEnabled.asStateFlow()

    val threats: StateFlow<List<com.orca.agent.brain.ThreatAlert>> = subconsciousEngine.threats

    private val _activeTask = MutableStateFlow<ActiveTaskSummary?>(null)
    val activeTask: StateFlow<ActiveTaskSummary?> = _activeTask.asStateFlow()

    init {
        viewModelScope.launch {
            orcaCore.activeTaskChain.collect { chain ->
                chain?.let {
                    _activeTask.value = ActiveTaskSummary(
                        id = it.id, name = it.goal,
                        completedSteps = it.nodes.count { n -> n.status == NodeStatus.VERIFIED },
                        totalSteps = it.nodes.size
                    )
                } ?: run { _activeTask.value = null }
            }
        }
    }

    fun processTextInput(text: String) {
        viewModelScope.launch {
            orcaCore.processUserIntent(UserInput(text = text, type = InputType.TEXT))
        }
    }

    fun toggleAutoPilot(enabled: Boolean) {
        _autoPilotEnabled.value = enabled
        if (enabled) orcaCore.enableAutoPilot() else orcaCore.disableAutoPilot()
    }

    fun confirmAction(chainId: String, nodeId: String) {
        viewModelScope.launch { orcaCore.taskExecutor.confirmAction(chainId, nodeId) }
    }

    fun cancelChain(chainId: String) {
        viewModelScope.launch { orcaCore.taskExecutor.cancelChain(chainId) }
    }

    override fun onCleared() {
        super.onCleared()
        orcaCore.shutdown()
    }
}

data class ActiveTaskSummary(
    val id: String,
    val name: String,
    val completedSteps: Int,
    val totalSteps: Int
)
