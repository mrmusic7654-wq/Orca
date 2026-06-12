package com.orca.agent.execution

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
    private var restartAttempts = 0
    private val maxRestartAttempts = 3

    fun startWatching() {
        scope.launch {
            while (isActive) {
                delay(3000)
                val timeSinceHeartbeat = System.currentTimeMillis() - _lastHeartbeat.value
                _serviceStatus.value = when {
                    timeSinceHeartbeat > 15000 -> ServiceStatus.DEAD
                    timeSinceHeartbeat > 5000 -> ServiceStatus.UNRESPONSIVE
                    else -> ServiceStatus.HEALTHY
                }
                if (_serviceStatus.value == ServiceStatus.DEAD && restartAttempts < maxRestartAttempts) {
                    restartAttempts++
                    notifyUserToReenable()
                }
            }
        }
    }

    fun stopWatching() {}
    fun receivedHeartbeat() { _lastHeartbeat.value = System.currentTimeMillis(); restartAttempts = 0 }
    fun isServiceHealthy(): Boolean = _serviceStatus.value == ServiceStatus.HEALTHY

    private fun notifyUserToReenable() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("orca_watchdog", "Watchdog", NotificationManager.IMPORTANCE_HIGH))
        }
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "orca_watchdog")
            .setContentTitle("Orca Needs Attention")
            .setContentText("Accessibility Service stopped. Tap to re-enable.")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(3001, notification)
    }
}

enum class ServiceStatus { UNKNOWN, HEALTHY, SLOW, UNRESPONSIVE, DEAD, RESTARTING }
