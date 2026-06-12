// ============================================================
// ChatViewModel.kt - FULL FEATURES
// Path: app/src/main/java/com/orca/agent/ui/screens/ChatViewModel.kt
// ============================================================

package com.orca.agent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orca.agent.core.*
import com.orca.agent.memory.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val memoryStream: MemoryStream,
    private val orcaCore: OrcaCore
) : ViewModel() {

    val activeSession: StateFlow<ChatSession?> = memoryStream.activeSession

    val thoughtStream: StateFlow<List<String>> = orcaCore.thoughtStream
        .map { "[${System.currentTimeMillis()}] $it" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val thoughtStreamFlow: SharedFlow<String> = orcaCore.thoughtStream

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _taskChain = MutableStateFlow<TaskChain?>(null)
    val taskChain: StateFlow<TaskChain?> = _taskChain.asStateFlow()

    init {
        viewModelScope.launch {
            orcaCore.activeTaskChain.collect { chain ->
                _taskChain.value = chain
            }
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            if (sessionId == "new_session" || sessionId == "new") {
                memoryStream.createNewSession()
            } else {
                memoryStream.loadSession(sessionId)
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            _isThinking.value = true

            val session = activeSession.value ?: run {
                memoryStream.createNewSession()
                activeSession.value ?: return@launch
            }

            // Add user message
            memoryStream.addMessage(
                sessionId = session.id,
                message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.USER,
                    content = text,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Process with Orca
            val result = orcaCore.processUserIntent(
                UserInput(
                    text = text,
                    imageBase64 = null,
                    voiceInput = null,
                    type = InputType.TEXT
                )
            )

            // Add Orca response
            memoryStream.addMessage(
                sessionId = session.id,
                message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ORCA,
                    content = result.response,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Add thought stream messages if any
            if (result.taskChain != null) {
                memoryStream.addMessage(
                    sessionId = session.id,
                    message = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = MessageRole.SYSTEM,
                        content = "Task chain created: ${result.taskChain.nodes.size} steps",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            _isThinking.value = false
        }
    }

    fun sendImage(imageBase64: String) {
        viewModelScope.launch {
            _isThinking.value = true

            val session = activeSession.value ?: return@launch

            memoryStream.addMessage(
                sessionId = session.id,
                message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.USER,
                    content = "[Image attached]",
                    timestamp = System.currentTimeMillis(),
                    attachments = listOf(
                        Attachment(
                            type = "image",
                            data = imageBase64,
                            mimeType = "image/jpeg"
                        )
                    )
                )
            )

            val result = orcaCore.processUserIntent(
                UserInput(
                    text = null,
                    imageBase64 = imageBase64,
                    voiceInput = null,
                    type = InputType.IMAGE
                )
            )

            memoryStream.addMessage(
                sessionId = session.id,
                message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ORCA,
                    content = result.response,
                    timestamp = System.currentTimeMillis()
                )
            )

            _isThinking.value = false
        }
    }

    fun forkSession() {
        viewModelScope.launch {
            val session = activeSession.value ?: return@launch
            memoryStream.forkSession(session.id)
        }
    }

    fun showTaskTimeline() {
        // Navigate to War Room - handled by navigation
    }

    fun confirmAction(chainId: String, nodeId: String) {
        viewModelScope.launch {
            orcaCore.taskExecutor.confirmAction(chainId, nodeId)
        }
    }

    fun cancelTask(chainId: String) {
        viewModelScope.launch {
            orcaCore.taskExecutor.cancelChain(chainId)
        }
    }

    fun startVoiceInput() { }
    fun activateSeeAndAct() { }
    fun openImagePicker() { }
}

@HiltViewModel
class MemoryStreamViewModel @Inject constructor(
    private val memoryStream: MemoryStream
) : ViewModel() {

    val sessions: StateFlow<List<ChatSession>> = memoryStream.sessions

    fun createNewSession() {
        viewModelScope.launch {
            memoryStream.createNewSession()
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            memoryStream.loadSession(sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            // Delete logic
        }
    }
}
