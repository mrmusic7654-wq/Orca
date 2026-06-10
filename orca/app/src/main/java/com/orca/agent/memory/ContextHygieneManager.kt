package com.orca.agent.memory

import android.content.Context
import android.content.pm.PackageManager
import com.orca.agent.core.AgentAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextHygieneManager @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Track what we "know" and when we learned it
    private val knowledgeBase = mutableMapOf<String, KnowledgeEntry>()
    private val contaminatedEntries = mutableSetOf<String>()
    
    private val _hygieneReport = MutableStateFlow<HygieneReport?>(null)
    val hygieneReport: StateFlow<HygieneReport?> = _hygieneReport.asStateFlow()
    
    data class KnowledgeEntry(
        val key: String,
        val value: String,
        val confidence: Float,
        val timesVerified: Int,
        val timesFailed: Int,
        val createdAt: Long,
        val lastVerifiedAt: Long,
        val appPackage: String?,
        val appVersion: String?
    )

    // ============================================================
    // CONTEXT POISONING DETECTION
    // ============================================================
    
    fun recordKnowledge(
        key: String,
        value: String,
        appPackage: String?,
        confidence: Float = 0.8f
    ) {
        val existing = knowledgeBase[key]
        
        if (existing != null) {
            // This knowledge already exists - update it
            knowledgeBase[key] = existing.copy(
                value = value,
                confidence = (existing.confidence + confidence) / 2f,
                timesVerified = existing.timesVerified + 1,
                lastVerifiedAt = System.currentTimeMillis()
            )
        } else {
            knowledgeBase[key] = KnowledgeEntry(
                key = key,
                value = value,
                confidence = confidence,
                timesVerified = 1,
                timesFailed = 0,
                createdAt = System.currentTimeMillis(),
                lastVerifiedAt = System.currentTimeMillis(),
                appPackage = appPackage,
                appVersion = getAppVersion(appPackage)
            )
        }
    }
    
    fun markKnowledgeFailed(key: String) {
        val existing = knowledgeBase[key] ?: return
        
        val newConfidence = existing.confidence * 0.7f
        val newTimesFailed = existing.timesFailed + 1
        
        knowledgeBase[key] = existing.copy(
            confidence = newConfidence,
            timesFailed = newTimesFailed
        )
        
        // If confidence drops too low, mark as contaminated
        if (newConfidence < 0.3f || newTimesFailed >= 3) {
            contaminatedEntries.add(key)
        }
    }

    // ============================================================
    // APP UPDATE CASCADE DETECTION
    // ============================================================
    
    suspend fun checkForAppUpdates() {
        val packageManager = context.packageManager
        val staleEntries = mutableListOf<String>()
        
        for ((key, entry) in knowledgeBase) {
            if (entry.appPackage == null) continue
            
            val currentVersion = getAppVersion(entry.appPackage)
            if (currentVersion != null && currentVersion != entry.appVersion) {
                // App has been updated! Our knowledge might be stale.
                staleEntries.add(key)
            }
        }
        
        if (staleEntries.isNotEmpty()) {
            // Mark all affected entries as needing re-verification
            for (key in staleEntries) {
                val entry = knowledgeBase[key] ?: continue
                knowledgeBase[key] = entry.copy(
                    confidence = entry.confidence * 0.5f, // Halve confidence
                    lastVerifiedAt = 0 // Force re-verification
                )
            }
            
            _hygieneReport.value = HygieneReport(
                staleEntries = staleEntries,
                contaminatedEntries = contaminatedEntries.toList(),
                totalEntries = knowledgeBase.size,
                healthyEntries = knowledgeBase.size - staleEntries.size - contaminatedEntries.size
            )
        }
    }

    // ============================================================
    // KNOWLEDGE CLEANUP
    // ============================================================
    
    suspend fun performHygieneCheck(): HygieneReport {
        val staleEntries = mutableListOf<String>()
        val now = System.currentTimeMillis()
        val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days
        
        for ((key, entry) in knowledgeBase) {
            // Check age
            if (now - entry.lastVerifiedAt > maxAge) {
                staleEntries.add(key)
            }
            
            // Check confidence
            if (entry.confidence < 0.5f && entry.timesFailed > 0) {
                contaminatedEntries.add(key)
            }
            
            // Check app version
            if (entry.appPackage != null) {
                val currentVersion = getAppVersion(entry.appPackage)
                if (currentVersion != entry.appVersion) {
                    staleEntries.add(key)
                }
            }
        }
        
        // Remove contaminated entries
        for (key in contaminatedEntries) {
            knowledgeBase.remove(key)
        }
        
        // Reset stale entries confidence
        for (key in staleEntries) {
            knowledgeBase[key]?.let { entry ->
                knowledgeBase[key] = entry.copy(
                    confidence = 0.5f,
                    lastVerifiedAt = 0
                )
            }
        }
        
        val report = HygieneReport(
            staleEntries = staleEntries,
            contaminatedEntries = contaminatedEntries.toList(),
            totalEntries = knowledgeBase.size,
            healthyEntries = knowledgeBase.size - staleEntries.size - contaminatedEntries.size
        )
        
        _hygieneReport.value = report
        return report
    }

    // ============================================================
    // SAFE KNOWLEDGE RETRIEVAL
    // ============================================================
    
    fun getSafeKnowledge(key: String): KnowledgeEntry? {
        val entry = knowledgeBase[key] ?: return null
        
        // Don't return contaminated entries
        if (key in contaminatedEntries) return null
        
        // Don't return low-confidence entries
        if (entry.confidence < 0.5f) return null
        
        // Don't return stale entries
        if (entry.lastVerifiedAt == 0L) return null
        
        return entry
    }
    
    fun getKnowledgeConfidence(key: String): Float {
        return knowledgeBase[key]?.confidence ?: 0f
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun getAppVersion(packageName: String?): String? {
        if (packageName == null) return null
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: packageInfo.longVersionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    // ============================================================
    // SCHEDULED HYGIENE
    // ============================================================
    
    fun startScheduledHygiene() {
        scope.launch {
            while (isActive) {
                delay(6 * 60 * 60 * 1000L) // Every 6 hours
                checkForAppUpdates()
                performHygieneCheck()
            }
        }
    }
}

data class HygieneReport(
    val staleEntries: List<String>,
    val contaminatedEntries: List<String>,
    val totalEntries: Int,
    val healthyEntries: Int
) {
    fun healthPercent(): Float = if (totalEntries > 0) healthyEntries.toFloat() / totalEntries else 1f
}
