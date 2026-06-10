package com.orca.agent.core

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.orca.agent.execution.AccessibilityBridge
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
    private var lastUserInteraction = 0L
    private val userInteractionThreshold = 3000L // 3 seconds

    // ============================================================
    // USER INTERRUPTION DETECTION
    // ============================================================
    
    fun startMonitoring(appPackage: String) {
        monitoredApp = appPackage
        isTaskRunning = true
        lastUserInteraction = System.currentTimeMillis()
        
        scope.launch {
            while (isActive && isTaskRunning) {
                checkForUserInterruption()
                checkAppHealth(appPackage)
                checkNetworkConnectivity()
                delay(2000) // Check every 2 seconds
            }
        }
    }
    
    fun stopMonitoring() {
        isTaskRunning = false
        monitoredApp = null
    }
    
    private suspend fun checkForUserInterruption() {
        val currentScreen = ScreenState.capture()
        val bridge = AccessibilityBridge.getInstance() ?: return
        
        // Detect if user is actively touching the screen
        val recentAccessibilityEvent = bridge.lastEventTime
        val timeSinceLastEvent = System.currentTimeMillis() - recentAccessibilityEvent
        
        // If user touched screen recently and we're mid-task
        if (timeSinceLastEvent < 1000 && isTaskRunning) {
            // Check if the screen changed unexpectedly (not by our action)
            if (currentScreen.currentApp != monitoredApp) {
                _warnings.emit(
                    GuardianWarning.UserSwitchedApp(
                        from = monitoredApp ?: "unknown",
                        to = currentScreen.currentApp
                    )
                )
            }
            
            lastUserInteraction = System.currentTimeMillis()
        }
        
        // If user has been interacting for a while, pause our task
        if (System.currentTimeMillis() - lastUserInteraction < userInteractionThreshold) {
            _warnings.emit(
                GuardianWarning.UserIsActive(
                    "User is currently using the device. Task should pause."
                )
            )
        }
    }

    // ============================================================
    // APP HEALTH MONITORING
    // ============================================================
    
    private suspend fun checkAppHealth(packageName: String) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return
        
        val isAppRunning = runningProcesses.any { 
            it.processName == packageName || 
            it.processName.startsWith("$packageName:") 
        }
        
        if (!isAppRunning && isTaskRunning) {
            _warnings.emit(
                GuardianWarning.AppCrashed(
                    packageName = packageName,
                    message = "Target app is no longer running. It may have crashed."
                )
            )
        }
        
        // Check if app is in foreground
        val currentScreen = ScreenState.capture()
        if (currentScreen.currentApp != packageName && isTaskRunning) {
            // App might be in background - check if it's still alive
            if (!isAppRunning) {
                _warnings.emit(
                    GuardianWarning.AppNotForeground(
                        expected = packageName,
                        actual = currentScreen.currentApp
                    )
                )
            }
        }
    }

    // ============================================================
    // NETWORK CONNECTIVITY
    // ============================================================
    
    private suspend fun checkNetworkConnectivity() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: run {
            _warnings.emit(
                GuardianWarning.NetworkLost("No network connection available. Cloud-dependent actions will fail.")
            )
            return
        }
        
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        
        if (!hasInternet) {
            _warnings.emit(
                GuardianWarning.NetworkLost("Network connected but no internet access.")
            )
        }
        
        if (isMetered) {
            _warnings.emit(
                GuardianWarning.MeteredNetwork(
                    "On metered connection. Large screenshot uploads may use significant data."
                )
            )
        }
    }

    // ============================================================
    // PROACTIVE PROTECTION
    // ============================================================
    
    fun shouldPauseForUser(): Boolean {
        return System.currentTimeMillis() - lastUserInteraction < userInteractionThreshold
    }
    
    fun isAppHealthy(packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any { it.processName.contains(packageName) }
    }
    
    fun hasInternetConnectivity(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

sealed class GuardianWarning {
    data class UserSwitchedApp(val from: String, val to: String) : GuardianWarning()
    data class UserIsActive(val message: String) : GuardianWarning()
    data class AppCrashed(val packageName: String, val message: String) : GuardianWarning()
    data class AppNotForeground(val expected: String, val actual: String) : GuardianWarning()
    data class NetworkLost(val message: String) : GuardianWarning()
    data class MeteredNetwork(val message: String) : GuardianWarning()
    data class BatteryLow(val level: Int) : GuardianWarning()
}
