package com.orca.agent.memory

import com.orca.agent.core.TaskChain
import com.orca.agent.data.database.OrcaDatabase
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryStream @Inject constructor(private val db: OrcaDatabase, private val ds: DeepSave) {
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()
    private val _activeSession = MutableStateFlow<ChatSession?>(null)
    val activeSession: StateFlow<ChatSession?> = _activeSession.asStateFlow()

    companion object {
        @Volatile private var INSTANCE: MemoryStream? = null
        fun getInstance(): MemoryStream = INSTANCE ?: throw IllegalStateException("Not initialized")
        fun setInstance(instance: MemoryStream) { INSTANCE = instance }
    }

    fun initialize() {}
    suspend fun createNewSession(): ChatSession = ChatSession(java.util.UUID.randomUUID().toString(), "New", emptyList(), null, false, SessionStatus.ACTIVE, emptyList(), emptyList(), 0, 0)
    suspend fun addMessage(sid: String, msg: ChatMessage) {}
    suspend fun loadSession(id: String): ChatSession? = null
    suspend fun forkSession(id: String): ChatSession = createNewSession()
}

data class ChatSession(val id: String, val title: String, val messages: List<ChatMessage>, val taskChain: TaskChain?, val isPaused: Boolean, val status: SessionStatus, val artifacts: List<Artifact>, val tags: List<String>, val createdAt: Long, val updatedAt: Long, val parentSessionId: String? = null, val mergedFrom: List<String> = emptyList(), val cognitiveCoreDump: String? = null)
data class ChatMessage(val id: String, val role: MessageRole, val content: String, val timestamp: Long, val attachments: List<Attachment> = emptyList())
enum class MessageRole { USER, ORCA, SYSTEM, THOUGHT_STREAM }
enum class SessionStatus { ACTIVE, COMPLETE, PAUSED, ARCHIVED }
enum class ArtifactType { SCREENSHOT, DOCUMENT, IMAGE, VOICE_NOTE }
data class Artifact(val id: String = java.util.UUID.randomUUID().toString(), val type: ArtifactType, val uri: String, val title: String, val createdAt: Long = System.currentTimeMillis())
data class Attachment(val type: String, val data: String, val mimeType: String)
