package com.orca.agent.brain

import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailurePatternRecognizer @Inject constructor() {
    private val failureHistory = mutableListOf<FailureRecord>()
    private val _detectedPatterns = MutableStateFlow<List<FailurePattern>>(emptyList())
    val detectedPatterns: StateFlow<List<FailurePattern>> = _detectedPatterns.asStateFlow()

    data class FailureRecord(
        val taskId: String, val appPackage: String,
        val errorType: String, val errorMessage: String,
        val timestamp: Long, val wasRecovered: Boolean
    )

    fun recordFailure(taskId: String, appPackage: String, errorType: String, errorMessage: String, wasRecovered: Boolean) {
        failureHistory.add(FailureRecord(taskId, appPackage, errorType, errorMessage, System.currentTimeMillis(), wasRecovered))
        if (failureHistory.size > 200) failureHistory.removeAt(0)
        analyze()
    }

    fun analyze(): Any? {
        val patterns = mutableListOf<FailurePattern>()
        val recent = failureHistory.takeLast(50)
        val networkFailures = recent.filter { it.errorType == "network" }
        if (networkFailures.size >= 5) {
            patterns.add(FailurePattern("NETWORK_ISSUE", networkFailures.size, "Check internet connection"))
        }
        _detectedPatterns.value = patterns
        return patterns
    }

    fun getFailureStats(): String {
        val total = failureHistory.size
        if (total == 0) return "No failures"
        val recovered = failureHistory.count { it.wasRecovered }
        return "Failures: $total, Recovery rate: ${(recovered.toFloat() / total * 100).toInt()}%"
    }
}

data class FailurePattern(val type: String, val count: Int, val suggestion: String)
