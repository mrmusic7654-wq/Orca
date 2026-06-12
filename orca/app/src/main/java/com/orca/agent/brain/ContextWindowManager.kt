package com.orca.agent.brain

import com.orca.agent.data.network.GeminiApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextWindowManager @Inject constructor(
    private val geminiApi: GeminiApi
) {
    data class ContextEntry(val type: EntryType, val content: String, val priority: Float, val timestamp: Long)
    enum class EntryType { SYSTEM_PROMPT, USER_COMMAND, SCREENSHOT_DESC, ACTION_TAKEN, VERIFICATION, ERROR_RECOVERY, GEMINI_RESPONSE }

    private val entries = mutableListOf<ContextEntry>()
    private val maxTokens = 900000

    fun addEntry(type: EntryType, content: String, priority: Float = 0.5f) {
        entries.add(ContextEntry(type, content, priority, System.currentTimeMillis()))
        if (entries.size > 100) {
            entries.removeAt(1)
        }
    }

    fun buildContext(): String {
        return entries.takeLast(50).joinToString("\n") { it.content }
    }

    fun getUsageStats(): String = "Entries: ${entries.size}"
    fun clear() { entries.clear() }
}
