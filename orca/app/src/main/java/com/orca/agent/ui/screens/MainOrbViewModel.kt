// ============================================================
// MainOrbViewModel.kt - FULL FEATURES
// Path: app/src/main/java/com/orca/agent/ui/screens/MainOrbViewModel.kt
// ============================================================

package com.orca.agent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orca.agent.core.*
import com.orca.agent.brain.SubconsciousEngine
import com.orca.agent.brain.ThreatAlert
import com.orca.agent.brain.ThreatSeverity
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
        .map { "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $it" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val thoughtStreamFlow: SharedFlow<String> = orcaCore.thoughtStream

    private val _autoPilotEnabled = MutableStateFlow(false)
    val autoPilotEnabled: StateFlow<Boolean> = _autoPilotEnabled.asStateFlow()

    val threats: StateFlow<List<ThreatAlert>> = subconsciousEngine.threats

    private val _activeTask = MutableStateFlow<ActiveTaskSummary?>(null)
    val activeTask: StateFlow<ActiveTaskSummary?> = _activeTask.asStateFlow()

    private val _predictions = MutableStateFlow<List<String>>(emptyList())
    val predictions: StateFlow<List<String>> = _predictions.asStateFlow()

    init {
        viewModelScope.launch {
            orcaCore.activeTaskChain.collect { chain ->
                chain?.let {
                    _activeTask.value = ActiveTaskSummary(
                        id = it.id,
                        name = it.goal,
                        completedSteps = it.nodes.count { n -> n.status == NodeStatus.VERIFIED },
                        totalSteps = it.nodes.size
                    )
                } ?: run {
                    _activeTask.value = null
                }
            }
        }

        viewModelScope.launch {
            subconsciousEngine.predictions.collect { preds ->
                _predictions.value = preds.map { it.action }
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

    fun processVoiceInput(audioData: ByteArray) {
        viewModelScope.launch {
            orcaCore.processUserIntent(
                UserInput(
                    text = null,
                    imageBase64 = null,
                    voiceInput = audioData,
                    type = InputType.VOICE
                )
            )
        }
    }

    fun processImageInput(imageBase64: String) {
        viewModelScope.launch {
            orcaCore.processUserIntent(
                UserInput(
                    text = null,
                    imageBase64 = imageBase64,
                    voiceInput = null,
                    type = InputType.IMAGE
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
        viewModelScope.launch {
            orcaCore.emitThought("Voice input activated")
        }
    }

    fun activateSeeAndAct() {
        viewModelScope.launch {
            orcaCore.emitThought("SeeAndAct camera activated")
        }
    }

    fun openImagePicker() {
        viewModelScope.launch {
            orcaCore.emitThought("Image picker opened")
        }
    }

    fun confirmAction(chainId: String, nodeId: String) {
        viewModelScope.launch {
            orcaCore.taskExecutor.confirmAction(chainId, nodeId)
        }
    }

    fun cancelChain(chainId: String) {
        viewModelScope.launch {
            orcaCore.taskExecutor.cancelChain(chainId)
        }
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
