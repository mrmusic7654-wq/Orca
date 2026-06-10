package com.orca.agent.core

import android.app.usage.UsageStatsManager
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemStateRespecter @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _systemState = MutableStateFlow(SystemState())
    val systemState: StateFlow<SystemState> = _systemState.asStateFlow()
    
    private val _blockedActions = MutableSharedFlow<BlockedAction>(replay = 20)
    val blockedActions: SharedFlow<BlockedAction> = _blockedActions.asSharedFlow()
    
    data class SystemState(
        val isAirplaneMode: Boolean = false,
        val isDataSaver: Boolean = false,
        val isBatterySaver: Boolean = false,
        val isEmergencyCall: Boolean = false,
        val isPhoneCall: Boolean = false,
        val isDoNotDisturb: Boolean = false,
        val isDrivingMode: Boolean = false,
        val isKidsMode: Boolean = false,
        val isGuestMode: Boolean = false,
        val isSplitScreen: Boolean = false,
        val networkType: NetworkType = NetworkType.UNKNOWN,
        val batteryLevel: Int = 100,
        val isCharging: Boolean = false
    )
    
    enum class NetworkType { WIFI, CELLULAR, METERED, NONE, UNKNOWN }

    // ============================================================
    // SYSTEM STATE DETECTION
    // ============================================================
    
    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val state = detectCurrentState()
                _systemState.value = state
                
                // Check for actions that should be blocked
                if (state.isAirplaneMode) {
                    _blockedActions.emit(BlockedAction.AllNetworkActions("Airplane mode active"))
                }
                if (state.isDataSaver) {
                    _blockedActions.emit(BlockedAction.LargeDataTransfers("Data Saver active"))
                }
                if (state.isEmergencyCall) {
                    _blockedActions.emit(BlockedAction.AllActions("Emergency call in progress"))
                }
                if (state.isBatterySaver && state.batteryLevel < 10) {
                    _blockedActions.emit(BlockedAction.NonEssentialActions("Critical battery with saver"))
                }
                if (state.isKidsMode || state.isGuestMode) {
                    _blockedActions.emit(BlockedAction.AllActions("Device in restricted mode"))
                }
                
                delay(5000)
            }
        }
    }

    // ============================================================
    // PERMISSION TO ACT
    // ============================================================
    
    fun canExecuteAction(action: AgentAction): ActionPermission {
        val state = _systemState.value
        
        // NEVER act during emergency calls
        if (state.isEmergencyCall) {
            return ActionPermission.Denied(
                "EMERGENCY CALL IN PROGRESS. All actions suspended for safety.",
                canRetryAfter = null
            )
        }
        
        // NEVER act in kids/guest mode
        if (state.isKidsMode || state.isGuestMode) {
            return ActionPermission.Denied(
                "Device is in restricted mode. Actions suspended.",
                canRetryAfter = null
            )
        }
        
        // Cloud-dependent actions during airplane mode
        if (state.isAirplaneMode && action is AgentAction.ScreenshotToGemini) {
            return ActionPermission.Delayed(
                "Airplane mode active. Will retry when network available.",
                canRetryAfter = null // Retry when airplane mode ends
            )
        }
        
        // Large screenshots on data saver
        if (state.isDataSaver && action is AgentAction.ScreenshotToGemini) {
            return ActionPermission.NeedsUserApproval(
                "Data Saver is active. Screenshot upload will use ~100KB of mobile data."
            )
        }
        
        // Non-essential during critical battery
        if (state.isBatterySaver && state.batteryLevel < 10) {
            val isEssential = action is AgentAction.Tap || 
                             action is AgentAction.Back || 
                             action is AgentAction.Home
            if (!isEssential) {
                return ActionPermission.Delayed(
                    "Battery critically low (${state.batteryLevel}%). Only essential actions allowed.",
                    canRetryAfter = null // Retry when charging
                )
            }
        }
        
        // Phone call - pause but allow essential navigation
        if (state.isPhoneCall) {
            val isDisruptive = action is AgentAction.AppAction || 
                              action is AgentAction.Speak
            if (isDisruptive) {
                return ActionPermission.Delayed(
                    "Phone call in progress. Disruptive actions paused.",
                    canRetryAfter = null
                )
            }
        }
        
        // Split screen - warn about coordinate issues
        if (state.isSplitScreen) {
            return ActionPermission.AllowedWithWarning(
                "Split screen detected. UI coordinates may be inaccurate."
            )
        }
        
        return ActionPermission.Allowed
    }

    // ============================================================
    // EMERGENCY DETECTION
    // ============================================================
    
    private fun isEmergencyCallActive(): Boolean {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.callState == TelephonyManager.CALL_STATE_OFFHOOK
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isPhoneCallActive(): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode == AudioManager.MODE_IN_CALL
        } catch (e: Exception) {
            false
        }
    }

    // ============================================================
    // STATE DETECTION
    // ============================================================
    
    private fun detectCurrentState(): SystemState {
        return SystemState(
            isAirplaneMode = isAirplaneModeEnabled(),
            isDataSaver = isDataSaverEnabled(),
            isBatterySaver = isBatterySaverEnabled(),
            isEmergencyCall = isEmergencyCallActive(),
            isPhoneCall = isPhoneCallActive(),
            isDoNotDisturb = isDoNotDisturbEnabled(),
            isDrivingMode = isDrivingModeEnabled(),
            isSplitScreen = isSplitScreenActive(),
            networkType = getNetworkType(),
            batteryLevel = getBatteryLevel(),
            isCharging = isCharging()
        )
    }
    
    private fun isAirplaneModeEnabled(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        } else false
    }
    
    private fun isDataSaverEnabled(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            connectivityManager.isActiveNetworkMetered && connectivityManager.restrictBackgroundStatus == 
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        } else false
    }
    
    private fun isBatterySaverEnabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }
    
    private fun isDoNotDisturbEnabled(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter != 
                android.app.NotificationManager.INTERRUPTION_FILTER_ALL
        } else false
    }
    
    private fun isDrivingModeEnabled(): Boolean {
        // Check for Android Auto or driving mode
        return false // Requires additional detection
    }
    
    private fun isSplitScreenActive(): Boolean {
        // Detect multi-window mode
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.isInMultiWindowMode
        } else false
    }
    
    private fun getNetworkType(): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    NetworkType.METERED
                } else {
                    NetworkType.CELLULAR
                }
            }
            else -> NetworkType.UNKNOWN
        }
    }
    
    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    private fun isCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
}

sealed class ActionPermission {
    object Allowed : ActionPermission()
    data class AllowedWithWarning(val warning: String) : ActionPermission()
    data class Delayed(val reason: String, val canRetryAfter: Long?) : ActionPermission()
    data class NeedsUserApproval(val reason: String) : ActionPermission()
    data class Denied(val reason: String, val canRetryAfter: Long?) : ActionPermission()
}

sealed class BlockedAction {
    abstract val reason: String
    data class AllActions(override val reason: String) : BlockedAction()
    data class AllNetworkActions(override val reason: String) : BlockedAction()
    data class LargeDataTransfers(override val reason: String) : BlockedAction()
    data class NonEssentialActions(override val reason: String) : BlockedAction()
}
