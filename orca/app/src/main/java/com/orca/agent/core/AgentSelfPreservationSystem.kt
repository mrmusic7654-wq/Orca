package com.orca.agent.core

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentSelfPreservationSystem @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _healthStatus = MutableStateFlow<AgentHealth>(AgentHealth.HEALTHY)
    val healthStatus: StateFlow<AgentHealth> = _healthStatus.asStateFlow()
    private val _resourceUsage = MutableStateFlow(ResourceUsage())
    val resourceUsage: StateFlow<ResourceUsage> = _resourceUsage.asStateFlow()

    fun startHealthMonitoring() {
        scope.launch {
            while (isActive) {
                checkResources()
                delay(60000)
            }
        }
    }

    private fun checkResources() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryPercent = usedMemory.toFloat() / maxMemory.toFloat()

        _resourceUsage.value = ResourceUsage(
            memoryUsedMB = usedMemory / (1024 * 1024),
            memoryPercent = memoryPercent
        )

        _healthStatus.value = when {
            memoryPercent > 0.9f -> AgentHealth.CRITICAL
            memoryPercent > 0.7f -> AgentHealth.DEGRADED
            memoryPercent > 0.5f -> AgentHealth.WARNING
            else -> AgentHealth.HEALTHY
        }

        if (memoryPercent > 0.7f) {
            System.gc()
        }
    }

    fun getUptimeHours(): Long {
        return (System.currentTimeMillis() - startTime) / (1000 * 60 * 60)
    }

    private val startTime = System.currentTimeMillis()
}

enum class AgentHealth { HEALTHY, WARNING, DEGRADED, CRITICAL }

data class ResourceUsage(
    val memoryUsedMB: Long = 0,
    val memoryPercent: Float = 0f,
    val databaseSizeMB: Long = 0,
    val batteryDrainPerHour: Float = 0f
)
