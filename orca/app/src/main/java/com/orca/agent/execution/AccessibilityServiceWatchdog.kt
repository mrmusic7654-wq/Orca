package com.orca.agent.execution

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.orca.agent.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityServiceWatchdog @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _serviceStatus = MutableStateFlow(ServiceStatus.UNKNOWN)
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()
    
    private val _lastHeartbeat = MutableStateFlow(0L)
    val lastHeartbeat: StateFlow<Long> = _lastHeartbeat.asStateFlow()
    
    private var watchdogJob: Job? = null
    private var restartAttempts = 0
    private val maxRestartAttempts = 3
    
    companion object {
        const val HEARTBEAT_TIMEOUT_MS = 5000L
        const val WATCHDOG_INTERVAL_MS = 3000L
        const val CHANNEL_ID = "orca_watchdog"
    }

    // ============================================================
    // HEARTBEAT MONITORING
    // ============================================================
    
    fun startWatching() {
        createNotificationChannel()
        startForegroundNotification()
        
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                checkServiceHealth()
            }
        }
    }
    
    fun stopWatching() {
        watchdogJob?.cancel()
    }
    
    fun receivedHeartbeat() {
        _lastHeartbeat.value = System.currentTimeMillis()
        _serviceStatus.value = ServiceStatus.HEALTHY
        restartAttempts = 0 // Reset on successful heartbeat
    }
    
    private suspend fun checkServiceHealth() {
        val timeSinceHeartbeat = System.currentTimeMillis() - _lastHeartbeat.value
        
        when {
            timeSinceHeartbeat > HEARTBEAT_TIMEOUT_MS * 3 -> {
                // Service has been dead for a while
                _serviceStatus.value = ServiceStatus.DEAD
                attemptRestart()
            }
            timeSinceHeartbeat > HEARTBEAT_TIMEOUT_MS -> {
                // Service might be stuck
                _serviceStatus.value = ServiceStatus.UNRESPONSIVE
                attemptGentleRestart()
            }
            timeSinceHeartbeat > HEARTBEAT_TIMEOUT_MS / 2 -> {
                // Service is slow but alive
                _serviceStatus.value = ServiceStatus.SLOW
            }
            else -> {
                _serviceStatus.value = ServiceStatus.HEALTHY
            }
        }
    }

    // ============================================================
    // SERVICE RESTART
    // ============================================================
    
    private suspend fun attemptGentleRestart() {
        // Try to restart without disabling/enabling
        val bridge = AccessibilityBridge.getInstance()
        if (bridge == null) {
            _serviceStatus.value = ServiceStatus.RESTARTING
            // The service might restart on its own
            delay(2000)
        }
    }
    
    private suspend fun attemptRestart() {
        if (restartAttempts >= maxRestartAttempts) {
            _serviceStatus.value = ServiceStatus.FAILED_PERMANENTLY
            notifyUserToReenable()
            return
        }
        
        restartAttempts++
        _serviceStatus.value = ServiceStatus.RESTARTING
        
        // Toggle accessibility service off and on
        // This requires the user to have granted permission
        val serviceName = "${context.packageName}/.execution.AccessibilityBridge"
        
        try {
            // Disable
            Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                ""
            )
            delay(500)
            
            // Re-enable
            Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                serviceName
            )
            Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                "1"
            )
            
            delay(1000)
            
            if (AccessibilityBridge.getInstance() != null) {
                _serviceStatus.value = ServiceStatus.HEALTHY
                restartAttempts = 0
            }
        } catch (e: Exception) {
            // Cannot restart programmatically - need user intervention
            _serviceStatus.value = ServiceStatus.NEEDS_USER_INTERVENTION
            notifyUserToReenable()
        }
    }

    // ============================================================
    // USER NOTIFICATION
    // ============================================================
    
    private fun notifyUserToReenable() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Orca Needs Attention")
            .setContentText("Accessibility Service stopped. Tap to re-enable.")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2001, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Orca Watchdog",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Orca Watchdog")
            .setContentText("Monitoring accessibility service health")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        
        // This would be used with a foreground service
    }
    
    fun isServiceHealthy(): Boolean {
        return _serviceStatus.value == ServiceStatus.HEALTHY ||
               _serviceStatus.value == ServiceStatus.SLOW
    }
}

enum class ServiceStatus {
    UNKNOWN,
    HEALTHY,
    SLOW,
    UNRESPONSIVE,
    RESTARTING,
    DEAD,
    FAILED_PERMANENTLY,
    NEEDS_USER_INTERVENTION
}
