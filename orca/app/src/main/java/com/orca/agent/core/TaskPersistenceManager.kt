package com.orca.agent.core

import android.content.Context
import com.orca.agent.data.database.OrcaDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPersistenceManager @Inject constructor(
    private val database: OrcaDatabase,
    private val context: Context
) {
    private val backupFile = "orca_task_backup.json"

    suspend fun saveTask(chain: TaskChain): Boolean {
        return try {
            val file = java.io.File(context.filesDir, backupFile)
            file.writeText(chain.toString())
            true
        } catch (e: Exception) { false }
    }

    suspend fun restoreTask(taskId: String): TaskChain? = null

    suspend fun saveTaskSnapshot(snapshot: String) {
        java.io.File(context.filesDir, "orca_snapshot.json").writeText(snapshot)
    }

    suspend fun loadTaskSnapshot(taskName: String): String? {
        val file = java.io.File(context.filesDir, "orca_snapshot.json")
        return if (file.exists()) file.readText() else null
    }

    suspend fun loadLatestSnapshot(): String? = loadTaskSnapshot("")
}
