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
    private val gson = Gson()
    private val backupFile = "orca_task_backup.json"

    suspend fun saveTask(chain: TaskChain): Boolean {
        return try {
            val json = gson.toJson(chain)
            val file = java.io.File(context.filesDir, backupFile)
            file.writeText(json)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreTask(taskId: String): TaskChain? {
        return try {
            val file = java.io.File(context.filesDir, backupFile)
            if (!file.exists()) return null
            gson.fromJson(file.readText(), TaskChain::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveTaskSnapshot(snapshot: String) {
        val file = java.io.File(context.filesDir, "orca_snapshot.json")
        file.writeText(snapshot)
    }

    suspend fun loadTaskSnapshot(taskName: String): String? {
        val file = java.io.File(context.filesDir, "orca_snapshot.json")
        return if (file.exists()) file.readText() else null
    }

    suspend fun loadLatestSnapshot(): String? {
        val file = java.io.File(context.filesDir, "orca_snapshot.json")
        return if (file.exists()) file.readText() else null
    }
}
