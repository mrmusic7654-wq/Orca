// app/src/main/java/com/orca/agent/ui/screens/ChatViewModel.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orca.agent.core.OrcaCore
import com.orca.agent.core.UserInput
import com.orca.agent.core.InputType
import com.orca.agent.memory.*
import com.orca.agent.memory.MemoryStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val memoryStream: MemoryStream,
    private val orcaCore: OrcaCore
) : ViewModel() {
    
    val activeSession: StateFlow<ChatSession?> = memoryStream.activeSession
    
    val thoughtStream = orcaCore.thoughtStream
        .replay(1)
        .let { flow ->
            flow.onSubscription { }.shareIn(viewModelScope, SharingStarted.Lazily)
        }
    
    val thoughtStreamFlow = orcaCore.thoughtStream
    
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()
    
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
            
            val session = activeSession.value ?: return@launch
            
            // Add user message
            memoryStream.addMessage(
                sessionId = session.id,
                message = ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
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
                    id = java.util.UUID.randomUUID().toString(),
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
        // Navigate to War Room
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
}
