// app/src/main/java/com/orca/agent/data/database/OrcaDatabase.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.data.database

import android.content.Context
import androidx.room.*
import com.orca.agent.data.models.*

@Database(
    entities = [
        ChatSessionEntity::class,
        MemoryEntity::class,
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
    fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<ChatSessionEntity>>
    
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
    @Query("SELECT * FROM memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<MemoryEntry>
    
    @Query("SELECT * FROM memories WHERE type = 'GOAL' AND metadataJson LIKE '%active%'")
    suspend fun getActiveGoals(): List<Goal>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: MemoryEntity)
    
    @Query("SELECT * FROM memories WHERE type = 'PATTERN' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPatterns(limit: Int): List<MemoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MemoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeepSave(deepSave: DeepSaveEntity)
    
    @Query("SELECT * FROM deep_saves WHERE sessionId = :sessionId")
    suspend fun getDeepSave(sessionId: String): DeepSaveEntity?
}
