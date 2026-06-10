package com.orca.agent.brain

import com.orca.agent.core.ScreenState
import com.orca.agent.core.AgentAction
import com.orca.agent.data.network.GeminiApi
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextWindowManager @Inject constructor(
    private val geminiApi: GeminiApi
) {
    private val maxContextTokens = 900000 // Leave 100K buffer from 1M
    private val estimatedTokensPerChar = 0.25 // Rough estimate
    
    data class ContextEntry(
        val type: EntryType,
        val content: String,
        val tokenEstimate: Int,
        val priority: Float, // 0.0 = can discard, 1.0 = must keep
        val timestamp: Long
    )
    
    enum class EntryType {
        SYSTEM_PROMPT,      // Must always keep
        USER_COMMAND,       // High priority
        SCREENSHOT_DESC,    // Medium - older ones can be summarized
        ACTION_TAKEN,       // Medium
        VERIFICATION,       // Medium
        ERROR_RECOVERY,     // High - important for learning
        GEMINI_RESPONSE,    // Medium
        MEMORY_RETRIEVED    // Low - can be re-retrieved
    }
    
    private val entries = mutableListOf<ContextEntry>()
    private var totalTokens = 0

    // ============================================================
    // ADDING TO CONTEXT
    // ============================================================
    
    fun addEntry(type: EntryType, content: String, priority: Float = 0.5f) {
        val tokenEstimate = (content.length * estimatedTokensPerChar).toInt()
        val entry = ContextEntry(type, content, tokenEstimate, priority, System.currentTimeMillis())
        
        entries.add(entry)
        totalTokens += tokenEstimate
        
        // Check if we need to prune
        if (totalTokens > maxContextTokens) {
            pruneContext()
        }
    }

    // ============================================================
    // INTELLIGENT PRUNING
    // ============================================================
    
    private fun pruneContext() {
        // Calculate how many tokens we need to free
        val targetTokens = (maxContextTokens * 0.7).toInt() // Prune to 70% capacity
        val tokensToFree = totalTokens - targetTokens
        
        if (tokensToFree <= 0) return
        
        var freedTokens = 0
        
        // First pass: Remove low-priority entries that are old
        val toRemove = mutableListOf<ContextEntry>()
        
        // Sort by: priority ascending, then age descending
        val sortedEntries = entries
            .filter { it.type != EntryType.SYSTEM_PROMPT } // Never remove system prompt
            .sortedWith(compareBy<ContextEntry> { it.priority }.thenByDescending { it.timestamp })
        
        for (entry in sortedEntries) {
            if (freedTokens >= tokensToFree) break
            
            // Don't remove error recovery entries (important for learning)
            if (entry.type == EntryType.ERROR_RECOVERY && entry.priority > 0.7f) continue
            
            // Don't remove recent user commands
            if (entry.type == EntryType.USER_COMMAND && 
                System.currentTimeMillis() - entry.timestamp < 300000) continue // 5 minutes
            
            toRemove.add(entry)
            freedTokens += entry.tokenEstimate
        }
        
        // If still not enough, summarize instead of removing
        if (freedTokens < tokensToFree) {
            summarizeOldestScreenDescriptions()
        }
        
        // Remove marked entries
        entries.removeAll(toRemove)
        totalTokens = entries.sumOf { it.tokenEstimate }
    }

    // ============================================================
    // SUMMARIZATION
    // ============================================================
    
    private fun summarizeOldestScreenDescriptions() {
        val screenEntries = entries.filter { it.type == EntryType.SCREENSHOT_DESC }
        if (screenEntries.size < 10) return // Not enough to summarize
        
        // Take the 20 oldest screen descriptions
        val toSummarize = screenEntries
            .sortedBy { it.timestamp }
            .take(20)
        
        if (toSummarize.isEmpty()) return
        
        // Create a summary
        val summary = buildString {
            append("SUMMARY OF ${toSummarize.size} PREVIOUS SCREENS:\n")
            append("Apps visited: ")
            append(toSummarize.joinToString(" → ") { 
                it.content.take(30) 
            })
            append("\nTimeline: ")
            append("${toSummarize.first().timestamp} to ${toSummarize.last().timestamp}")
        }
        
        // Remove the detailed entries
        entries.removeAll(toSummarize)
        
        // Add the summary
        val summaryEntry = ContextEntry(
            type = EntryType.SCREENSHOT_DESC,
            content = summary,
            tokenEstimate = (summary.length * estimatedTokensPerChar).toInt(),
            priority = 0.4f,
            timestamp = System.currentTimeMillis()
        )
        entries.add(summaryEntry)
        
        // Recalculate
        totalTokens = entries.sumOf { it.tokenEstimate }
    }

    // ============================================================
    // CONTEXT RETRIEVAL FOR GEMINI
    // ============================================================
    
    fun buildContextString(limit: Int = 50): String {
        // Return the most recent N entries as a formatted string
        return entries
            .takeLast(limit)
            .joinToString("\n") { entry ->
                when (entry.type) {
                    EntryType.SYSTEM_PROMPT -> entry.content
                    EntryType.USER_COMMAND -> "USER: ${entry.content}"
                    EntryType.SCREENSHOT_DESC -> "SCREEN: ${entry.content}"
                    EntryType.ACTION_TAKEN -> "ACTION: ${entry.content}"
                    EntryType.VERIFICATION -> "VERIFY: ${entry.content}"
                    EntryType.ERROR_RECOVERY -> "RECOVERY: ${entry.content}"
                    EntryType.GEMINI_RESPONSE -> "ORCA: ${entry.content}"
                    EntryType.MEMORY_RETRIEVED -> "MEMORY: ${entry.content}"
                }
            }
    }

    // ============================================================
    // STATISTICS
    // ============================================================
    
    fun getUsageStats(): ContextStats {
        return ContextStats(
            totalEntries = entries.size,
            totalTokens = totalTokens,
            maxTokens = maxContextTokens,
            utilizationPercent = (totalTokens.toFloat() / maxContextTokens * 100),
            entriesByType = entries.groupBy { it.type }.mapValues { it.value.size },
            oldestEntry = entries.minOfOrNull { it.timestamp } ?: 0,
            newestEntry = entries.maxOfOrNull { it.timestamp } ?: 0
        )
    }
    
    fun clear() {
        entries.clear()
        totalTokens = 0
    }
}

data class ContextStats(
    val totalEntries: Int,
    val totalTokens: Int,
    val maxTokens: Int,
    val utilizationPercent: Float,
    val entriesByType: Map<ContextWindowManager.EntryType, Int>,
    val oldestEntry: Long,
    val newestEntry: Long
)
