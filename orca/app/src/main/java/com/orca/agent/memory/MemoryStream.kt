// app/src/main/java/com/orca/agent/memory/MemoryStream.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.memory

import com.orca.agent.data.database.OrcaDatabase
import com.orca.agent.data.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryStream @Inject constructor(
    private val database: OrcaDatabase,
    private val deepSave: DeepSave
) {
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()
    
    private val _activeSession = MutableStateFlow<ChatSession?>(null)
    val activeSession: StateFlow<ChatSession?> = _activeSession.asStateFlow()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun initialize() {
        scope.launch {
            database.chatDao().getAllSessions().collect { sessionEntities ->
                _sessions.value = sessionEntities.map { it.toChatSession() }
            }
        }
    }
    
    suspend fun createNewSession(title: String = ""): ChatSession {
        val session = ChatSession(
            id = java.util.UUID.randomUUID().toString(),
            title = title.ifEmpty { "New Session" },
            messages = emptyList(),
            taskChain = null,
            isPaused = false,
            status = SessionStatus.ACTIVE,
            artifacts = emptyList(),
            tags = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        database.chatDao().insertSession(session.toEntity())
        _activeSession.value = session
        return session
    }
    
    suspend fun addMessage(sessionId: String, message: ChatMessage) {
        val updatedSession = _sessions.value.find { it.id == sessionId }?.copy(
            messages = _sessions.value.find { it.id == sessionId }!!.messages + message,
            updatedAt = System.currentTimeMillis()
        ) ?: return
        
        database.chatDao().updateSession(updatedSession.toEntity())
        _activeSession.value = updatedSession
        
        // Auto-generate title from first meaningful exchange
        if (updatedSession.title == "New Session" && updatedSession.messages.size >= 2) {
            val autoTitle = generateTitle(updatedSession.messages)
            database.chatDao().updateTitle(sessionId, autoTitle)
        }
    }
    
    suspend fun loadSession(sessionId: String): ChatSession? {
        val session = database.chatDao().getSession(sessionId)?.toChatSession()
        _activeSession.value = session
        
        // If session has a paused task chain, attempt to restore cognitive state
        if (session?.isPaused == true && session.taskChain != null) {
            deepSave.restoreCognitiveState(session)
        }
        
        return session
    }
    
    suspend fun saveSession(session: ChatSession) {
        deepSave.performDeepSave(session)
        database.chatDao().updateSession(session.toEntity())
    }
    
    suspend fun forkSession(sourceSessionId: String): ChatSession {
        val source = database.chatDao().getSession(sourceSessionId)?.toChatSession()
            ?: throw IllegalStateException("Source session not found")
        
        val forkedSession = source.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${source.title} (Fork)",
            isPaused = false,
            status = SessionStatus.ACTIVE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            parentSessionId = source.id
        )
        
        database.chatDao().insertSession(forkedSession.toEntity())
        return forkedSession
    }
    
    suspend fun mergeSessions(targetId: String, sourceId: String): ChatSession {
        val target = database.chatDao().getSession(targetId)?.toChatSession()!!
        val source = database.chatDao().getSession(sourceId)?.toChatSession()!!
        
        val merged = target.copy(
            messages = (target.messages + source.messages).distinctBy { it.timestamp },
            artifacts = (target.artifacts + source.artifacts).distinct(),
            tags = (target.tags + source.tags).distinct(),
            mergedFrom = (target.mergedFrom + sourceId),
            updatedAt = System.currentTimeMillis()
        )
        
        database.chatDao().updateSession(merged.toEntity())
        return merged
    }
    
    suspend fun semanticSearchSessions(query: String, limit: Int = 10): List<ChatSession> {
        // Delegate to SemanticLake for vector search
        return emptyList() // Placeholder
    }
    
    private fun generateTitle(messages: List<ChatMessage>): String {
        // Use a lightweight on-device model or simple heuristic
        val userMessages = messages.filter { it.role == MessageRole.USER }
        if (userMessages.isNotEmpty()) {
            val firstMsg = userMessages.first().content
            return if (firstMsg.length > 50) firstMsg.take(50) + "..." else firstMsg
        }
        return "Conversation ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
    }
}

data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val taskChain: TaskChain?,
    val isPaused: Boolean,
    val status: SessionStatus,
    val artifacts: List<Artifact>,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val parentSessionId: String? = null,
    val mergedFrom: List<String> = emptyList(),
    val cognitiveCoreDump: String? = null
)

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val attachments: List<Attachment> = emptyList(),
    val actions: List<AgentAction> = emptyList()
)

enum class MessageRole { USER, ORCA, SYSTEM, THOUGHT_STREAM }

enum class SessionStatus { ACTIVE, COMPLETE, PAUSED, ARCHIVED }

data class Artifact(
    val id: String,
    val type: ArtifactType,
    val uri: String,
    val title: String,
    val createdAt: Long
)

enum class ArtifactType { SCREENSHOT, DOCUMENT, IMAGE, VOICE_NOTE, TASK_REPORT }

data class Attachment(
    val type: String,
    val data: String, // Base64 or URI
    val mimeType: String
)
