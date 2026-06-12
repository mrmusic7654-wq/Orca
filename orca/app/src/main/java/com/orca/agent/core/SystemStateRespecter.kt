package com.orca.agent.core

import android.content.Context
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
    private val _systemState = MutableStateFlow(SystemState())
    val systemState: StateFlow<SystemState> = _systemState.asStateFlow()

    fun canExecute(): Boolean {
        val state = detectState()
        return !state.isAirplaneMode && !state.isEmergencyCall && state.batteryLevel > 5
    }

    fun isAirplaneModeEnabled(): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }

    fun isBatterySaverEnabled(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isPowerSaveMode
    }

    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "NONE"
        val caps = cm.getNetworkCapabilities(network) ?: return "UNKNOWN"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            else -> "OTHER"
        }
    }

    private fun detectState(): SystemState {
        return SystemState(
            isAirplaneMode = isAirplaneModeEnabled(),
            isBatterySaver = isBatterySaverEnabled(),
            batteryLevel = getBatteryLevel(),
            networkType = getNetworkType()
        )
    }
}

data class SystemState(
    val isAirplaneMode: Boolean = false,
    val isDataSaver: Boolean = false,
    val isBatterySaver: Boolean = false,
    val isEmergencyCall: Boolean = false,
    val isPhoneCall: Boolean = false,
    val isDoNotDisturb: Boolean = false,
    val isSplitScreen: Boolean = false,
    val networkType: String = "UNKNOWN",
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false
)
