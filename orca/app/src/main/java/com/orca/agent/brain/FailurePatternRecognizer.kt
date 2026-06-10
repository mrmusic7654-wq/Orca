package com.orca.agent.brain

import com.orca.agent.core.ExecutionResult
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailurePatternRecognizer @Inject constructor() {
    
    private val failureHistory = mutableListOf<FailureRecord>(100)
    private val _detectedPatterns = MutableStateFlow<List<FailurePattern>>(emptyList())
    val detectedPatterns: StateFlow<List<FailurePattern>> = _detectedPatterns.asStateFlow()
    
    data class FailureRecord(
        val taskId: String,
        val appPackage: String,
        val errorType: String,
        val errorMessage: String,
        val timestamp: Long,
        val wasRecovered: Boolean
    )

    // ============================================================
    // SYSTEMIC FAILURE DETECTION
    // ============================================================
    
    fun recordFailure(
        taskId: String,
        appPackage: String,
        errorType: String,
        errorMessage: String,
        wasRecovered: Boolean
    ) {
        failureHistory.add(
            FailureRecord(
                taskId = taskId,
                appPackage = appPackage,
                errorType = errorType,
                errorMessage = errorMessage,
                timestamp = System.currentTimeMillis(),
                wasRecovered = wasRecovered
            )
        )
        
        if (failureHistory.size > 200) {
            failureHistory.removeAt(0)
        }
        
        // Analyze for patterns
        analyzeFailurePatterns()
    }

    // ============================================================
    // PATTERN ANALYSIS
    // ============================================================
    
    private fun analyzeFailurePatterns() {
        val patterns = mutableListOf<FailurePattern>()
        val recent = failureHistory.takeLast(50)
        
        // Pattern 1: Credential expiration
        val loginFailures = recent.filter { 
            it.errorType == "login_wall" || 
            it.errorMessage.contains("login", ignoreCase = true) ||
            it.errorMessage.contains("password", ignoreCase = true) ||
            it.errorMessage.contains("sign in", ignoreCase = true)
        }
        if (loginFailures.size >= 3) {
            val affectedApps = loginFailures.map { it.appPackage }.distinct()
            patterns.add(
                FailurePattern.CredentialExpired(
                    affectedApps = affectedApps,
                    failureCount = loginFailures.size,
                    suggestion = "Credentials may have expired for: ${affectedApps.joinToString()}. " +
                                "Update stored credentials in Orca Settings."
                )
            )
        }
        
        // Pattern 2: App uninstalled
        val appNotFoundFailures = recent.filter {
            it.errorType == "app_not_found" ||
            it.errorMessage.contains("not found", ignoreCase = true) ||
            it.errorMessage.contains("not installed", ignoreCase = true)
        }
        if (appNotFoundFailures.isNotEmpty()) {
            val missingApps = appNotFoundFailures.map { it.appPackage }.distinct()
            patterns.add(
                FailurePattern.AppMissing(
                    missingApps = missingApps,
                    failureCount = appNotFoundFailures.size,
                    suggestion = "Apps no longer installed: ${missingApps.joinToString()}. " +
                                "Update your task configurations."
                )
            )
        }
        
        // Pattern 3: Network dependency failure
        val networkFailures = recent.filter {
            it.errorType == "network" ||
            it.errorMessage.contains("network", ignoreCase = true) ||
            it.errorMessage.contains("timeout", ignoreCase = true) ||
            it.errorMessage.contains("connection", ignoreCase = true)
        }
        if (networkFailures.size >= 5) {
            patterns.add(
                FailurePattern.NetworkIssue(
                    failureCount = networkFailures.size,
                    timespan = networkFailures.last().timestamp - networkFailures.first().timestamp,
                    suggestion = "Frequent network failures. Check internet connection or try offline mode."
                )
            )
        }
        
        // Pattern 4: Same app failing repeatedly
        val appGroups = recent.groupBy { it.appPackage }
        for ((app, failures) in appGroups) {
            if (failures.size >= 5) {
                val recoveryRate = failures.count { it.wasRecovered }.toFloat() / failures.size
                if (recoveryRate < 0.3f) {
                    patterns.add(
                        FailurePattern.UnstableApp(
                            appPackage = app,
                            failureCount = failures.size,
                            recoveryRate = recoveryRate,
                            suggestion = "App '$app' has a ${(recoveryRate * 100).toInt()}% recovery rate. " +
                                        "Consider marking it as unstable or updating its workflows."
                        )
                    )
                }
            }
        }
        
        // Pattern 5: Time-based failure clustering (all failures at specific time)
        val hourGroups = recent.groupBy {
            java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
                .get(java.util.Calendar.HOUR_OF_DAY)
        }
        for ((hour, failures) in hourGroups) {
            if (failures.size >= 4 && recent.size >= 10) {
                patterns.add(
                    FailurePattern.TimeCluster(
                        hour = hour,
                        failureCount = failures.size,
                        suggestion = "Multiple failures occur at $hour:00. " +
                                    "Check if scheduled tasks or network conditions change at this time."
                    )
                )
            }
        }
        
        _detectedPatterns.value = patterns
    }

    // ============================================================
    // ROOT CAUSE ANALYSIS
    // ============================================================
    
    fun findRootCause(recentErrors: List<String>): RootCauseAnalysis {
        val allErrors = recentErrors.joinToString(" ").lowercase()
        
        return when {
            allErrors.contains("login") || allErrors.contains("password") || 
            allErrors.contains("sign in") || allErrors.contains("authenticate") -> {
                RootCauseAnalysis(
                    rootCause = "CREDENTIAL_EXPIRATION",
                    confidence = 0.85f,
                    affectedSystems = listOf("Authentication", "App Login"),
                    solution = "Update stored credentials. Check if passwords were changed recently.",
                    prevention = "Monitor credential age and prompt for updates before expiration."
                )
            }
            allErrors.contains("not found") || allErrors.contains("uninstalled") -> {
                RootCauseAnalysis(
                    rootCause = "APP_REMOVED",
                    confidence = 0.9f,
                    affectedSystems = listOf("App Launch", "Task Execution"),
                    solution = "Reinstall missing apps or update task configurations.",
                    prevention = "Check app existence before planning tasks."
                )
            }
            allErrors.contains("network") || allErrors.contains("timeout") || 
            allErrors.contains("connection") -> {
                RootCauseAnalysis(
                    rootCause = "NETWORK_ISSUE",
                    confidence = 0.75f,
                    affectedSystems = listOf("Cloud API", "Data Sync"),
                    solution = "Check internet connection. Enable offline mode if needed.",
                    prevention = "Pre-cache critical workflows for offline use."
                )
            }
            allErrors.contains("coordinates") || allErrors.contains("position") ||
            allErrors.contains("bounds") -> {
                RootCauseAnalysis(
                    rootCause = "UI_LAYOUT_CHANGED",
                    confidence = 0.8f,
                    affectedSystems = listOf("Touch Input", "Element Detection"),
                    solution = "Run UI recalibration for affected apps.",
                    prevention = "Detect app updates and refresh cached coordinates."
                )
            }
            else -> {
                RootCauseAnalysis(
                    rootCause = "UNKNOWN",
                    confidence = 0.3f,
                    affectedSystems = emptyList(),
                    solution = "Manual investigation required.",
                    prevention = "Improve error categorization."
                )
            }
        }
    }
    
    fun getFailureStats(): FailureStats {
        val total = failureHistory.size
        if (total == 0) return FailureStats(0, 0f, emptyList())
        
        val recovered = failureHistory.count { it.wasRecovered }
        val recoveryRate = recovered.toFloat() / total
        
        val topErrors = failureHistory
            .groupBy { it.errorType }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { Pair(it.key, it.value) }
        
        return FailureStats(
            totalFailures = total,
            recoveryRate = recoveryRate,
            topErrors = topErrors
        )
    }
}

sealed class FailurePattern {
    abstract val suggestion: String
    
    data class CredentialExpired(
        val affectedApps: List<String>,
        val failureCount: Int,
        override val suggestion: String
    ) : FailurePattern()
    
    data class AppMissing(
        val missingApps: List<String>,
        val failureCount: Int,
        override val suggestion: String
    ) : FailurePattern()
    
    data class NetworkIssue(
        val failureCount: Int,
        val timespan: Long,
        override val suggestion: String
    ) : FailurePattern()
    
    data class UnstableApp(
        val appPackage: String,
        val failureCount: Int,
        val recoveryRate: Float,
        override val suggestion: String
    ) : FailurePattern()
    
    data class TimeCluster(
        val hour: Int,
        val failureCount: Int,
        override val suggestion: String
    ) : FailurePattern()
}

data class RootCauseAnalysis(
    val rootCause: String,
    val confidence: Float,
    val affectedSystems: List<String>,
    val solution: String,
    val prevention: String
)

data class FailureStats(
    val totalFailures: Int,
    val recoveryRate: Float,
    val topErrors: List<Pair<String, Int>>
)
