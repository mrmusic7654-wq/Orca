package com.orca.agent.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentGuardian @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _warnings = MutableSharedFlow<EnvironmentWarning>(replay = 10)
    val warnings: SharedFlow<EnvironmentWarning> = _warnings.asSharedFlow()
    
    private val _isSafeToExecute = MutableStateFlow(true)
    val isSafeToExecute: StateFlow<Boolean> = _isSafeToExecute.asStateFlow()
    
    // Thresholds
    companion object {
        const val CRITICAL_BATTERY = 5
        const val LOW_BATTERY = 15
        const val WARNING_BATTERY = 25
        const val MIN_BATTERY_FOR_TASKS = 20
    }

    // ============================================================
    // COMPREHENSIVE PRE-FLIGHT CHECK
    // ============================================================
    
    fun preFlightCheck(taskChain: TaskChain): PreFlightResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var canProceed = true
        
        // Battery check
        val batteryLevel = getBatteryLevel()
        if (batteryLevel < CRITICAL_BATTERY) {
            issues.add("Battery critically low (${batteryLevel}%). Cannot execute tasks.")
            canProceed = false
        } else if (batteryLevel < LOW_BATTERY) {
            if (taskChain.nodes.size > 5) {
                issues.add("Battery low (${batteryLevel}%). Task may not complete.")
            }
        } else if (batteryLevel < WARNING_BATTERY) {
            warnings.add("Battery at ${batteryLevel}%. Consider charging.")
        }
        
        // Charging state
        val isCharging = isDeviceCharging()
        if (!isCharging && taskChain.nodes.size > 20) {
            warnings.add("Long task on battery power. May drain significantly.")
        }
        
        // Accessibility permission
        if (!isAccessibilityEnabled()) {
            issues.add("Accessibility Service not enabled. Cannot control device.")
            canProceed = false
        }
        
        // Overlay permission
        if (!Settings.canDrawOverlays(context)) {
            warnings.add("Overlay permission not granted. Some features limited.")
        }
        
        // Screen recording detection
        if (isScreenBeingRecorded()) {
            issues.add("SCREEN RECORDING DETECTED. Privacy risk: actions will be visible.")
            canProceed = false
        }
        
        // Do Not Disturb
        val isDND = isDoNotDisturbEnabled()
        if (isDND && taskChain.nodes.any { it.action is AgentAction.Speak }) {
            warnings.add("Do Not Disturb is active. Voice output suppressed.")
        }
        
        // Power saving mode
        if (isPowerSavingEnabled() && taskChain.nodes.size > 10) {
            warnings.add("Power saving mode active. Performance may be reduced.")
        }
        
        // Storage space
        val freeSpace = getFreeStorageSpace()
        if (freeSpace < 100 * 1024 * 1024) { // Less than 100MB
            warnings.add("Low storage space (${freeSpace / (1024*1024)}MB). Screenshot storage may fail.")
        }
        
        return PreFlightResult(
            canProceed = canProceed,
            issues = issues,
            warnings = warnings,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            estimatedTaskDuration = estimateTaskDuration(taskChain)
        )
    }

    // ============================================================
    // CONTINUOUS MONITORING
    // ============================================================
    
    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val batteryLevel = getBatteryLevel()
                
                if (batteryLevel <= CRITICAL_BATTERY) {
                    _warnings.emit(
                        EnvironmentWarning.BatteryCritical(
                            level = batteryLevel,
                            message = "Battery critically low. Suspending all tasks."
                        )
                    )
                    _isSafeToExecute.value = false
                }
                
                if (isScreenBeingRecorded()) {
                    _warnings.emit(
                        EnvironmentWarning.ScreenRecordingDetected(
                            "Screen recording/casting active. Pausing visible actions."
                        )
                    )
                    _isSafeToExecute.value = false
                } else if (batteryLevel > CRITICAL_BATTERY) {
                    _isSafeToExecute.value = true
                }
                
                delay(10000) // Check every 10 seconds
            }
        }
    }

    // ============================================================
    // SYSTEM STATE CHECKS
    // ============================================================
    
    fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    fun isDeviceCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
    
    fun isScreenBeingRecorded(): Boolean {
        // Check if MediaProjection is active (screen recording/casting)
        return try {
            val mediaProjectionManager = context.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as? MediaProjectionManager
            // This is a heuristic - actual detection requires Android 14+ APIs
            false
        } catch (e: Exception) {
            false
        }
    }
    
    fun isAccessibilityEnabled(): Boolean {
        val service = "${context.packageName}/com.orca.agent.execution.AccessibilityBridge"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    fun isDoNotDisturbEnabled(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter == 
                android.app.NotificationManager.INTERRUPTION_FILTER_NONE
        } else false
    }
    
    fun isPowerSavingEnabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }
    
    fun getFreeStorageSpace(): Long {
        val stat = android.os.StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    // ============================================================
    // TASK DURATION ESTIMATION
    // ============================================================
    
    fun estimateTaskDuration(chain: TaskChain): Long {
        // Rough estimate: 2 seconds per action + 1 second per verification
        val actionTime = chain.nodes.size * 2000L
        val verificationTime = chain.nodes.size * 1000L
        val cloudCalls = chain.nodes.count { 
            it.action is AgentAction.ScreenshotToGemini || 
            it.requiresConfirmation 
        } * 3000L
        return actionTime + verificationTime + cloudCalls
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

sealed class EnvironmentWarning {
    data class BatteryCritical(val level: Int, val message: String) : EnvironmentWarning()
    data class BatteryLow(val level: Int, val message: String) : EnvironmentWarning()
    data class ScreenRecordingDetected(val message: String) : EnvironmentWarning()
    data class AccessibilityRevoked(val message: String) : EnvironmentWarning()
    data class StorageLow(val freeMB: Long, val message: String) : EnvironmentWarning()
    data class NetworkLost(val message: String) : EnvironmentWarning()
}
