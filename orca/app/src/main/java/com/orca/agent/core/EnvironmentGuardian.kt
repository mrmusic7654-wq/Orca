package com.orca.agent.core

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentGuardian @Inject constructor(
    private val context: Context
) {
    private val _isSafeToExecute = MutableStateFlow(true)
    val isSafeToExecute: StateFlow<Boolean> = _isSafeToExecute.asStateFlow()

    fun preFlightCheck(chain: TaskChain): PreFlightResult {
        val batteryLevel = getBatteryLevel()
        val isCharging = isCharging()
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (batteryLevel < 5) {
            issues.add("Battery critically low")
            _isSafeToExecute.value = false
        } else if (batteryLevel < 15) {
            warnings.add("Battery low: ${batteryLevel}%")
        }

        if (isPowerSavingEnabled() && chain.nodes.size > 10) {
            warnings.add("Power saving mode active")
        }

        return PreFlightResult(
            canProceed = issues.isEmpty(),
            issues = issues,
            warnings = warnings,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            estimatedTaskDuration = chain.nodes.size * 2000L
        )
    }

    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun isPowerSavingEnabled(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isPowerSaveMode
    }

    fun isAccessibilityEnabled(): Boolean {
        val service = "${context.packageName}/com.orca.agent.execution.AccessibilityBridge"
        val enabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabled?.contains(service) == true
    }

    fun getFreeStorageSpace(): Long {
        val stat = android.os.StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }
}

data class PreFlightResult(
    val canProceed: Boolean,
    val issues: List<String>,
    val warnings: List<String>,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val estimatedTaskDuration: Long
)
