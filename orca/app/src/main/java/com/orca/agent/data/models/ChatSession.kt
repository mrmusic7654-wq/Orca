package com.orca.agent.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orca.agent.memory.ChatSession
import com.orca.agent.memory.SessionStatus

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val messagesJson: String,
    val taskChainJson: String?,
    val isPaused: Boolean,
    val status: String,
    val artifactsJson: String,
    val tagsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val parentSessionId: String?,
    val mergedFromJson: String,
    val cognitiveCoreDump: String?
) {
    fun toChatSession(): ChatSession {
        return ChatSession(
            id = id, title = title, messages = emptyList(), taskChain = null,
            isPaused = isPaused,
            status = try { SessionStatus.valueOf(status) } catch (e: Exception) { SessionStatus.ACTIVE },
            artifacts = emptyList(), tags = emptyList(),
            createdAt = createdAt, updatedAt = updatedAt,
            parentSessionId = parentSessionId, mergedFrom = emptyList(),
            cognitiveCoreDump = cognitiveCoreDump
        )
    }
}

fun ChatSession.toEntity(): ChatSessionEntity {
    return ChatSessionEntity(
        id = id, title = title, messagesJson = "[]", taskChainJson = null,
        isPaused = isPaused, status = status.name, artifactsJson = "[]", tagsJson = "[]",
        createdAt = createdAt, updatedAt = updatedAt,
        parentSessionId = parentSessionId, mergedFromJson = "[]", cognitiveCoreDump = cognitiveCoreDump
    )
}
