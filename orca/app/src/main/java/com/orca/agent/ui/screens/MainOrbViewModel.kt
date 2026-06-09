// app/src/main/java/com/orca/agent/ui/screens/MainOrbViewModel.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orca.agent.core.*
import com.orca.agent.brain.SubconsciousEngine
import com.orca.agent.agent.DigitalTwin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainOrbViewModel @Inject constructor(
    private val orcaCore: OrcaCore,
    private val subconsciousEngine: SubconsciousEngine,
    private val digitalTwin: DigitalTwin
) : ViewModel() {
    
    val cognitiveState = orcaCore.cognitiveState
    val thoughtStream = orcaCore.thoughtStream
        .replay(1)
        .let { flow ->
            flow.onSubscription { }.shareIn(viewModelScope, SharingStarted.Lazily)
        }
    
    val thoughtStreamFlow = orcaCore.thoughtStream
    
    private val _autoPilotEnabled = MutableStateFlow(false)
    val autoPilotEnabled: StateFlow<Boolean> = _autoPilotEnabled.asStateFlow()
    
    val threats = subconsciousEngine.threats
    
    private val _activeTask = MutableStateFlow<ActiveTaskSummary?>(null)
    val activeTask: StateFlow<ActiveTaskSummary?> = _activeTask.asStateFlow()
    
    init {
        // Monitor active task chain
        viewModelScope.launch {
            orcaCore.activeTaskChain.collect { chain ->
                chain?.let {
                    _activeTask.value = ActiveTaskSummary(
                        id = it.id,
                        name = it.name,
                        completedSteps = it.nodes.count { node -> 
                            node.status == com.orca.agent.core.NodeStatus.COMPLETED 
                        },
                        totalSteps = it.nodes.size
                    )
                } ?: run {
                    _activeTask.value = null
                }
            }
        }
    }
    
    fun processTextInput(text: String) {
        viewModelScope.launch {
            orcaCore.processUserIntent(
                UserInput(
                    text = text,
                    imageBase64 = null,
                    voiceInput = null,
                    type = InputType.TEXT
                )
            )
        }
    }
    
    fun toggleAutoPilot(enabled: Boolean) {
        _autoPilotEnabled.value = enabled
        if (enabled) {
            orcaCore.enableAutoPilot()
        } else {
            orcaCore.disableAutoPilot()
        }
    }
    
    fun startVoiceInput() {
        // Initialize voice recognition
    }
    
    fun activateSeeAndAct() {
        // Open camera for SeeAndAct
    }
    
    fun openImagePicker() {
        // Open image picker
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
