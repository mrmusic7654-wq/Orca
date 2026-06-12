package com.orca.agent.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orca.agent.memory.ChatSession
import com.orca.agent.memory.ChatMessage
import com.orca.agent.memory.SessionStatus
import com.orca.agent.memory.Artifact
import com.orca.agent.core.TaskChain
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        val gson = Gson()
        return ChatSession(
            id = id,
            title = title,
            messages = try {
                gson.fromJson(messagesJson, object : TypeToken<List<ChatMessage>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            },
            taskChain = try {
                taskChainJson?.let { gson.fromJson(it, TaskChain::class.java) }
            } catch (e: Exception) {
                null
            },
            isPaused = isPaused,
            status = try {
                SessionStatus.valueOf(status)
            } catch (e: Exception) {
                SessionStatus.ACTIVE
            },
            artifacts = try {
                gson.fromJson(artifactsJson, object : TypeToken<List<Artifact>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            },
            tags = try {
                gson.fromJson(tagsJson, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            },
            createdAt = createdAt,
            updatedAt = updatedAt,
            parentSessionId = parentSessionId,
            mergedFrom = try {
                gson.fromJson(mergedFromJson, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            },
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
