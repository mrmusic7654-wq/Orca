package com.orca.agent.core

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskGuardian @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _warnings = MutableSharedFlow<GuardianWarning>(replay = 10)
    val warnings: SharedFlow<GuardianWarning> = _warnings.asSharedFlow()
    private var monitoredApp: String? = null
    private var isTaskRunning = false

    fun startMonitoring(appPackage: String) {
        monitoredApp = appPackage
        isTaskRunning = true
    }

    fun stopMonitoring() {
        isTaskRunning = false
        monitoredApp = null
    }

    fun isAppHealthy(packageName: String): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = am.runningAppProcesses ?: return false
        return processes.any { it.processName.contains(packageName) }
    }

    fun hasInternetConnectivity(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

sealed class GuardianWarning {
    data class UserSwitchedApp(val from: String, val to: String) : GuardianWarning()
    data class UserIsActive(val message: String) : GuardianWarning()
    data class AppCrashed(val packageName: String, val message: String) : GuardianWarning()
    data class NetworkLost(val message: String) : GuardianWarning()
    data class MeteredNetwork(val message: String) : GuardianWarning()
    data class BatteryLow(val level: Int) : GuardianWarning()
}
