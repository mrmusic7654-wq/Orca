package com.orca.agent.memory

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextHygieneManager @Inject constructor(
    private val context: Context
) {
    private val knowledgeBase = mutableMapOf<String, KnowledgeEntry>()
    private val contaminatedEntries = mutableSetOf<String>()
    private val _hygieneReport = MutableStateFlow<HygieneReport?>(null)
    val hygieneReport: StateFlow<HygieneReport?> = _hygieneReport.asStateFlow()

    data class KnowledgeEntry(val key: String, val value: String, val confidence: Float, val timesVerified: Int, val timesFailed: Int, val createdAt: Long, val lastVerifiedAt: Long)

    fun recordKnowledge(key: String, value: String, confidence: Float = 0.8f) {
        val existing = knowledgeBase[key]
        knowledgeBase[key] = if (existing != null) {
            existing.copy(value = value, confidence = (existing.confidence + confidence) / 2f, timesVerified = existing.timesVerified + 1, lastVerifiedAt = System.currentTimeMillis())
        } else {
            KnowledgeEntry(key, value, confidence, 1, 0, System.currentTimeMillis(), System.currentTimeMillis())
        }
    }

    fun markKnowledgeFailed(key: String) {
        val existing = knowledgeBase[key] ?: return
        knowledgeBase[key] = existing.copy(confidence = existing.confidence * 0.7f, timesFailed = existing.timesFailed + 1)
        if (knowledgeBase[key]!!.confidence < 0.3f) contaminatedEntries.add(key)
    }

    fun getSafeKnowledge(key: String): KnowledgeEntry? {
        return if (key in contaminatedEntries) null else knowledgeBase[key]
    }

    fun check() {
        val stale = knowledgeBase.filter { System.currentTimeMillis() - it.value.lastVerifiedAt > 7 * 24 * 60 * 60 * 1000L }
        _hygieneReport.value = HygieneReport(stale.keys.toList(), contaminatedEntries.toList(), knowledgeBase.size, knowledgeBase.size - stale.size - contaminatedEntries.size)
    }
}

data class HygieneReport(val staleEntries: List<String>, val contaminatedEntries: List<String>, val totalEntries: Int, val healthyEntries: Int)
