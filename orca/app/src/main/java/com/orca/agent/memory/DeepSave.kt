package com.orca.agent.memory

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orca.agent.data.network.GeminiApi
import com.orca.agent.data.database.OrcaDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "deep_saves")
data class DeepSaveEntity(
    @PrimaryKey val sessionId: String,
    val coreDump: String,
    val fullContext: ByteArray,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeepSaveEntity) return false
        return sessionId == other.sessionId
    }

    override fun hashCode(): Int = sessionId.hashCode()
}

@Singleton
class DeepSave @Inject constructor(
    private val geminiApi: GeminiApi,
    private val database: OrcaDatabase
) {
    suspend fun performDeepSave(session: ChatSession) {
        val coreDump = geminiApi.generateContent(
            prompt = "Summarize this conversation into a cognitive core dump:\n${session.messages.takeLast(20).joinToString { it.content.take(200) }}"
        )
        database.memoryDao().insertDeepSave(
            DeepSaveEntity(
                sessionId = session.id,
                coreDump = coreDump,
                fullContext = ByteArray(0),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreCognitiveState(session: ChatSession): RestoredState? {
        val deepSave = database.memoryDao().getDeepSave(session.id) ?: return null
        return RestoredState(
            session = session,
            cognitiveCoreDump = deepSave.coreDump,
            canFullyRestore = deepSave.fullContext.isNotEmpty()
        )
    }
}

data class RestoredState(
    val session: ChatSession,
    val cognitiveCoreDump: String,
    val canFullyRestore: Boolean
)
