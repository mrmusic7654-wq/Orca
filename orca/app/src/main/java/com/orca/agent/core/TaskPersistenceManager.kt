package com.orca.agent.core

import android.content.Context
import com.orca.agent.data.database.OrcaDatabase
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPersistenceManager @Inject constructor(
    private val database: OrcaDatabase,
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    
    private val _savedTasks = MutableStateFlow<List<SavedTask>>(emptyList())
    val savedTasks: StateFlow<List<SavedTask>> = _savedTasks.asStateFlow()
    
    private val backupFile = "orca_task_backup.json"

    // ============================================================
    // TASK SAVING WITH INTEGRITY CHECK
    // ============================================================
    
    suspend fun saveTask(chain: TaskChain): Boolean {
        return try {
            val savedTask = SavedTask(
                id = chain.id,
                goal = chain.goal,
                currentNodeIndex = chain.currentNodeIndex,
                totalNodes = chain.nodes.size,
                status = chain.status.name,
                chainJson = gson.toJson(chain),
                savedAt = System.currentTimeMillis()
            )
            
            // Save to database
            database.memoryDao().insertMemory(
                com.orca.agent.data.models.MemoryEntity(
                    id = "task_${chain.id}",
                    type = "TASK_STATE",
                    content = gson.toJson(savedTask),
                    timestamp = System.currentTimeMillis(),
                    metadataJson = "{}"
                )
            )
            
            // Also save to file backup
            saveToFileBackup(savedTask)
            
            // Update in-memory list
            val current = _savedTasks.value.toMutableList()
            current.removeAll { it.id == chain.id }
            current.add(0, savedTask)
            _savedTasks.value = current.take(50) // Keep last 50
            
            true
        } catch (e: Exception) {
            // Database might be corrupted - try file backup
            try {
                val savedTask = SavedTask(
                    id = chain.id,
                    goal = chain.goal,
                    currentNodeIndex = chain.currentNodeIndex,
                    totalNodes = chain.nodes.size,
                    status = chain.status.name,
                    chainJson = gson.toJson(chain),
                    savedAt = System.currentTimeMillis()
                )
                saveToFileBackup(savedTask)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    // ============================================================
    // TASK RESTORATION
    // ============================================================
    
    suspend fun restoreTask(taskId: String): TaskChain? {
        // Try database first
        try {
            val memories = database.memoryDao().getRecentMemories(100)
            val taskMemory = memories.find { it.id == "task_$taskId" }
            if (taskMemory != null) {
                val savedTask = gson.fromJson(taskMemory.content, SavedTask::class.java)
                return gson.fromJson(savedTask.chainJson, TaskChain::class.java)
            }
        } catch (e: Exception) {
            // Database read failed
        }
        
        // Try file backup
        return restoreFromFileBackup(taskId)
    }

    // ============================================================
    // FILE-BASED BACKUP (Database Corruption Fallback)
    // ============================================================
    
    private fun saveToFileBackup(task: SavedTask) {
        try {
            val file = java.io.File(context.filesDir, backupFile)
            val existing = if (file.exists()) {
                gson.fromJson(file.readText(), Array<SavedTask>::class.java).toMutableList()
            } else {
                mutableListOf()
            }
            
            existing.removeAll { it.id == task.id }
            existing.add(0, task)
            
            // Keep only last 100 backups
            val trimmed = existing.take(100)
            file.writeText(gson.toJson(trimmed.toTypedArray()))
        } catch (e: Exception) {
            // File backup failed - last resort
        }
    }
    
    private fun restoreFromFileBackup(taskId: String): TaskChain? {
        return try {
            val file = java.io.File(context.filesDir, backupFile)
            if (!file.exists()) return null
            
            val backups = gson.fromJson(file.readText(), Array<SavedTask>::class.java)
            val savedTask = backups.find { it.id == taskId } ?: return null
            
            gson.fromJson(savedTask.chainJson, TaskChain::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ============================================================
    // DATABASE INTEGRITY CHECK
    // ============================================================
    
    suspend fun checkDatabaseIntegrity(): Boolean {
        return try {
            database.memoryDao().getRecentMemories(1)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun repairDatabase(): Boolean {
        return try {
            // Force database rebuild
            database.clearAllTables()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ============================================================
    // AUTO-SAVE DURING EXECUTION
    // ============================================================
    
    fun startAutoSave(chain: TaskChain) {
        scope.launch {
            var currentChain = chain
            while (isActive) {
                delay(30000) // Save every 30 seconds
                saveTask(currentChain)
            }
        }
    }
}

data class SavedTask(
    val id: String,
    val goal: String,
    val currentNodeIndex: Int,
    val totalNodes: Int,
    val status: String,
    val chainJson: String,
    val savedAt: Long
)
