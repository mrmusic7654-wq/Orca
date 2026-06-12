package com.orca.agent.data.database

import android.content.Context
import androidx.room.*
import com.orca.agent.data.models.ChatSessionEntity
import com.orca.agent.memory.DeepSaveEntity
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        ChatSessionEntity::class,
        DeepSaveEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OrcaDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: OrcaDatabase? = null

        fun getInstance(context: Context): OrcaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrcaDatabase::class.java,
                    "orca_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :sessionId")
    suspend fun updateTitle(sessionId: String, title: String)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE status = 'ACTIVE'")
    suspend fun getActiveGoals(): List<ChatSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(entity: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeepSave(deepSave: DeepSaveEntity)

    @Query("SELECT * FROM deep_saves WHERE sessionId = :sessionId")
    suspend fun getDeepSave(sessionId: String): DeepSaveEntity?
}
