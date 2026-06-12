package com.orca.agent.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orca.agent.core.*
import com.orca.agent.memory.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val memoryStream: MemoryStream = MemoryStream.getInstance()
    private val orcaCore: OrcaCore = OrcaCore.getInstance()

    val activeSession: StateFlow<ChatSession?> = memoryStream.activeSession
    val thoughtStream: StateFlow<List<String>> = orcaCore.thoughtStream.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    fun sendMessage(text: String) {
        viewModelScope.launch {
            _isThinking.value = true
            val session = activeSession.value ?: return@launch
            memoryStream.addMessage(session.id, ChatMessage(UUID.randomUUID().toString(), MessageRole.USER, text, System.currentTimeMillis()))
            val result = orcaCore.processUserIntent(UserInput(text = text, type = InputType.TEXT))
            memoryStream.addMessage(session.id, ChatMessage(UUID.randomUUID().toString(), MessageRole.ORCA, result.response, System.currentTimeMillis()))
            _isThinking.value = false
        }
    }

    fun forkSession() {
        viewModelScope.launch { activeSession.value?.let { memoryStream.forkSession(it.id) } }
    }
}
