// app/src/main/java/com/orca/agent/data/models/ChatSession.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.data.models

import androidx.room.*
import com.orca.agent.core.TaskChain
import com.orca.agent.memory.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Room Entities
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val messagesJson: String, // JSON serialized messages
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
        val gson = Gson()
        return ChatSession(
            id = id,
            title = title,
            messages = gson.fromJson(messagesJson, object : TypeToken<List<ChatMessage>>() {}.type),
            taskChain = taskChainJson?.let { gson.fromJson(it, TaskChain::class.java) },
            isPaused = isPaused,
            status = SessionStatus.valueOf(status),
            artifacts = gson.fromJson(artifactsJson, object : TypeToken<List<Artifact>>() {}.type),
            tags = gson.fromJson(tagsJson, object : TypeToken<List<String>>() {}.type),
            createdAt = createdAt,
            updatedAt = updatedAt,
            parentSessionId = parentSessionId,
            mergedFrom = gson.fromJson(mergedFromJson, object : TypeToken<List<String>>() {}.type),
            cognitiveCoreDump = cognitiveCoreDump
        )
    }
}

fun ChatSession.toEntity(): ChatSessionEntity {
    val gson = Gson()
    return ChatSessionEntity(
        id = id,
        title = title,
        messagesJson = gson.toJson(messages),
        taskChainJson = taskChain?.let { gson.toJson(it) },
        isPaused = isPaused,
        status = status.name,
        artifactsJson = gson.toJson(artifacts),
        tagsJson = gson.toJson(tags),
        createdAt = createdAt,
        updatedAt = updatedAt,
        parentSessionId = parentSessionId,
        mergedFromJson = gson.toJson(mergedFrom),
        cognitiveCoreDump = cognitiveCoreDump
    )
}

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val content: String,
    val timestamp: Long,
    val metadataJson: String
) {
    fun toMemoryEntry(): MemoryEntry {
        val gson = Gson()
        return MemoryEntry(
            id = id,
            type = MemoryType.valueOf(type),
            content = content,
            embedding = null,
            timestamp = timestamp,
            metadata = gson.fromJson(metadataJson, object : TypeToken<Map<String, String>>() {}.type)
        )
    }
}

@Entity(tableName = "deep_saves")
data class DeepSaveEntity(
    @PrimaryKey val sessionId: String,
    val coreDump: String,
    val fullContext: ByteArray,
    val timestamp: Long
)
