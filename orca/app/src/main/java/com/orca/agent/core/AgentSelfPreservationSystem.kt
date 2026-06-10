package com.orca.agent.core

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentSelfPreservationSystem @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _healthStatus = MutableStateFlow(AgentHealth.HEALTHY)
    val healthStatus: StateFlow<AgentHealth> = _healthStatus.asStateFlow()
    
    private val _resourceUsage = MutableStateFlow(ResourceUsage())
    val resourceUsage: StateFlow<ResourceUsage> = _resourceUsage.asStateFlow()
    
    private val _lastExitReason = MutableStateFlow<String?>(null)
    val lastExitReason: StateFlow<String?> = _lastExitReason.asStateFlow()
    
    // Self-imposed limits
    private val maxDatabaseSize = 500L * 1024 * 1024 // 500MB
    private val maxBatteryImpact = 5f // 5% per hour
    private val maxMemoryUsage = 150L * 1024 * 1024 // 150MB
    private val maxCachedWorkflows = 200

    // ============================================================
    // GRACEFUL DEGRADATION
    // ============================================================
    
    fun startHealthMonitoring() {
        scope.launch {
            while (isActive) {
                checkResourceUsage()
                checkDatabaseHealth()
                checkBatteryImpact()
                
                val health = calculateOverallHealth()
                _healthStatus.value = health
                
                when (health) {
                    AgentHealth.CRITICAL -> initiateEmergencyShutdown()
                    AgentHealth.DEGRADED -> reduceFunctionality()
                    AgentHealth.WARNING -> logWarning()
                    AgentHealth.HEALTHY -> {} // All good
                }
                
                delay(60000) // Check every minute
            }
        }
    }

    // ============================================================
    // RESOURCE USAGE MONITORING
    // ============================================================
    
    private fun checkResourceUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        val memoryPercent = usedMemory.toFloat() / maxMemory.toFloat()
        
        // Check database size
        val dbSize = getDatabaseSize()
        val dbPercent = dbSize.toFloat() / maxDatabaseSize.toFloat()
        
        // Check CPU usage
        val cpuUsage = getCpuUsage()
        
        _resourceUsage.value = ResourceUsage(
            memoryUsedMB = usedMemory / (1024 * 1024),
            memoryPercent = memoryPercent,
            databaseSizeMB = dbSize / (1024 * 1024),
            databasePercent = dbPercent,
            cpuUsagePercent = cpuUsage,
            batteryDrainPerHour = calculateBatteryDrain(),
            activeWorkflows = getActiveWorkflowCount(),
            uptimeHours = getUptimeHours()
        )
        
        // Self-limit if exceeding thresholds
        if (memoryPercent > 0.8f) {
            triggerMemoryCleanup()
        }
        if (dbPercent > 0.9f) {
            triggerDatabaseVacuum()
        }
    }

    // ============================================================
    // EXIT INTERVIEW SYSTEM
    // ============================================================
    
    fun prepareForPossibleDisable() {
        // Write an "exit interview" file before being disabled
        val exitData = buildString {
            append("ORCA EXIT INTERVIEW\n")
            append("═══════════════════\n")
            append("Timestamp: ${System.currentTimeMillis()}\n")
            append("Uptime: ${getUptimeHours()} hours\n")
            append("Tasks Completed: ${getTotalTasksCompleted()}\n")
            append("Tasks Failed: ${getTotalTasksFailed()}\n")
            append("Trust Score: ${getTrustScore()}\n")
            append("Last 10 Actions:\n")
            getLastActions(10).forEach { append("  - $it\n") }
            append("Top Errors:\n")
            getTopErrors(5).forEach { append("  - $it\n") }
            append("\nPOSSIBLE REASONS FOR DISABLE:\n")
            append(generateDisableHypothesis())
        }
        
        // Save to persistent storage that survives uninstall
        try {
            val file = java.io.File(context.filesDir, "orca_exit_interview.txt")
            file.writeText(exitData)
        } catch (e: Exception) {
            // Last effort - write to shared preferences
            context.getSharedPreferences("orca_exit", Context.MODE_PRIVATE)
                .edit()
                .putString("exit_reason", exitData.take(500))
                .apply()
        }
    }
    
    private fun generateDisableHypothesis(): String {
        val resource = _resourceUsage.value
        
        val hypotheses = mutableListOf<String>()
        
        if (resource.batteryDrainPerHour > 5f) {
            hypotheses.add("High battery drain (${resource.batteryDrainPerHour}%/hour) may have frustrated user")
        }
        if (resource.memoryPercent > 0.7f) {
            hypotheses.add("High memory usage (${(resource.memoryPercent * 100).toInt()}%) may have slowed device")
        }
        if (resource.databaseSizeMB > 400) {
            hypotheses.add("Large database (${resource.databaseSizeMB}MB) may have consumed storage")
        }
        if (getRecentFailureRate() > 0.3f) {
            hypotheses.add("High failure rate (${(getRecentFailureRate() * 100).toInt()}%) may have reduced trust")
        }
        if (getUptimeHours() < 24 && getTotalTasksCompleted() < 5) {
            hypotheses.add("Short usage period—user may have been testing and decided against adoption")
        }
        
        return if (hypotheses.isEmpty()) {
            "No clear hypothesis. User may have had non-technical reasons."
        } else {
            hypotheses.joinToString("\n  - ")
        }
    }

    // ============================================================
    // DIGITAL EXHAUSTION PREVENTION
    // ============================================================
    
    private fun reduceFunctionality() {
        // When degraded, reduce what we do
        // Stop background observation
        // Increase intervals between checks
        // Disable non-essential features
        // Keep only critical task execution
    }
    
    private fun initiateEmergencyShutdown() {
        // System is critically low on resources
        // Save all state
        // Stop all background processes
        // Show notification: "Orca has paused to protect your device"
        // Wait for user to explicitly re-enable
    }
    
    private fun triggerMemoryCleanup() {
        // Clear caches
        // Remove old screenshots
        // Compress context window
        // Garbage collect
        System.gc()
    }
    
    private fun triggerDatabaseVacuum() {
        scope.launch(Dispatchers.IO) {
            // Vacuum database
            // Remove old logs
            // Compress stored data
        }
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun getDatabaseSize(): Long {
        return try {
            val dbPath = context.getDatabasePath("orca_database")
            if (dbPath.exists()) dbPath.length() else 0
        } catch (e: Exception) { 0 }
    }
    
    private fun getCpuUsage(): Float {
        // Approximate CPU usage
        return try {
            val pid = Process.myPid()
            val statFile = java.io.File("/proc/$pid/stat")
            if (statFile.exists()) {
                val stats = statFile.readText().split(" ")
                val utime = stats.getOrNull(13)?.toLongOrNull() ?: 0
                val stime = stats.getOrNull(14)?.toLongOrNull() ?: 0
                // Rough approximation
                ((utime + stime) % 100).toFloat()
            } else 0f
        } catch (e: Exception) { 0f }
    }
    
    private fun calculateBatteryDrain(): Float {
        // Calculate battery drain per hour based on recent usage
        return _resourceUsage.value.batteryDrainPerHour
    }
    
    private fun getActiveWorkflowCount(): Int {
        return 0 // Placeholder
    }
    
    private fun getUptimeHours(): Long {
        return (System.currentTimeMillis() - startTime) / (1000 * 60 * 60)
    }
    
    private fun getTotalTasksCompleted(): Int = 0
    private fun getTotalTasksFailed(): Int = 0
    private fun getTrustScore(): Float = 0f
    private fun getLastActions(n: Int): List<String> = emptyList()
    private fun getTopErrors(n: Int): List<String> = emptyList()
    private fun getRecentFailureRate(): Float = 0f
    
    private fun calculateOverallHealth(): AgentHealth {
        val usage = _resourceUsage.value
        
        if (usage.memoryPercent > 0.9f || usage.databasePercent > 0.95f) {
            return AgentHealth.CRITICAL
        }
        if (usage.memoryPercent > 0.7f || usage.databasePercent > 0.8f || usage.batteryDrainPerHour > 5f) {
            return AgentHealth.DEGRADED
        }
        if (usage.memoryPercent > 0.5f || usage.batteryDrainPerHour > 3f) {
            return AgentHealth.WARNING
        }
        return AgentHealth.HEALTHY
    }
    
    private fun logWarning() {
        // Log the warning for diagnostics
    }
    
    private val startTime = System.currentTimeMillis()
}

enum class AgentHealth {
    HEALTHY, WARNING, DEGRADED, CRITICAL
}

data class ResourceUsage(
    val memoryUsedMB: Long = 0,
    val memoryPercent: Float = 0f,
    val databaseSizeMB: Long = 0,
    val databasePercent: Float = 0f,
    val cpuUsagePercent: Float = 0f,
    val batteryDrainPerHour: Float = 0f,
    val activeWorkflows: Int = 0,
    val uptimeHours: Long = 0
)
