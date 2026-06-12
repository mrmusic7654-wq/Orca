package com.orca.agent.brain

import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiResponseValidator @Inject constructor() {
    private val responseHistory = mutableListOf<ResponseRecord>()

    data class ResponseRecord(val promptType: String, val parseSuccess: Boolean, val actionExecuted: Boolean, val timestamp: Long)

    fun validate(response: String): Boolean {
        return response.isNotBlank() && !response.contains("I cannot", true)
    }

    fun isSafetyFilterBlocking(response: String): Boolean {
        return response.contains("I cannot", true) || response.contains("I'm not able", true)
    }

    fun attemptRepair(response: String): String? {
        var repaired = response
        repaired = repaired.replace("```json", "").replace("```", "")
        repaired = repaired.replace('\u201C', '"').replace('\u201D', '"')
        return if (repaired != response) repaired else null
    }

    fun recordResponse(promptType: String, parseSuccess: Boolean, actionExecuted: Boolean) {
        responseHistory.add(ResponseRecord(promptType, parseSuccess, actionExecuted, System.currentTimeMillis()))
        if (responseHistory.size > 100) responseHistory.removeAt(0)
    }

    fun getReliabilityStats(): String {
        val total = responseHistory.size
        if (total == 0) return "No data"
        val successRate = responseHistory.count { it.parseSuccess }.toFloat() / total
        return "Success rate: ${(successRate * 100).toInt()}%"
    }
}
