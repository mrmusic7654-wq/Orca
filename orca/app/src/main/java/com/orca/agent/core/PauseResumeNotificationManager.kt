package com.orca.agent.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.orca.agent.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PauseResumeNotificationManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        const val PAUSE_CHANNEL_ID = "orca_pause_channel"
        const val PAUSE_NOTIFICATION_ID = 2001
        const val RESUME_ACTION = "com.orca.agent.RESUME_TASK"
        const val CANCEL_ACTION = "com.orca.agent.CANCEL_TASK"
        const val CANCEL_RESUME_ACTION = "com.orca.agent.CANCEL_RESUME"
    }
    
    private val _notificationState = MutableStateFlow<PauseNotificationState>(
        PauseNotificationState.HIDDEN
    )
    val notificationState: StateFlow<PauseNotificationState> = _notificationState.asStateFlow()
    
    private var countdownJob: Job? = null

    // ============================================================
    // CHANNEL CREATION
    // ============================================================
    
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PAUSE_CHANNEL_ID,
                "Orca Task Control",
                NotificationManager.IMPORTANCE_HIGH // High importance for persistent control
            ).apply {
                description = "Pause, resume, and control Orca tasks"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ============================================================
    // PAUSE NOTIFICATION
    // ============================================================
    
    fun showPausedNotification(
        taskName: String,
        currentStep: Int,
        totalSteps: Int,
        pausedAt: Long
    ) {
        val resumeIntent = Intent(context, PauseResumeReceiver::class.java).apply {
            action = RESUME_ACTION
            putExtra("task_id", taskName)
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            context, 0, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cancelIntent = Intent(context, PauseResumeReceiver::class.java).apply {
            action = CANCEL_ACTION
            putExtra("task_id", taskName)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val pausedTime = timeFormat.format(java.util.Date(pausedAt))
        
        val notification = NotificationCompat.Builder(context, PAUSE_CHANNEL_ID)
            .setContentTitle("⏸️ Orca PAUSED")
            .setContentText("$taskName — Step $currentStep of $totalSteps")
            .setSubText("Paused at $pausedTime • Touch & hold screen to pause")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Cannot be swiped away
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(Color.parseColor("#FFFF3A2D")) // Neon Red
            .addAction(
                android.R.drawable.ic_media_play,
                "RESUME",
                resumePendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "CANCEL TASK",
                cancelPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Task: $taskName\nProgress: Step $currentStep of $totalSteps\nPaused at: $pausedTime\n\nTap RESUME to continue or CANCEL to stop.")
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(PAUSE_NOTIFICATION_ID, notification)
        _notificationState.value = PauseNotificationState.PAUSED
    }

    // ============================================================
    // RESUME COUNTDOWN NOTIFICATION
    // ============================================================
    
    fun showResumeCountdown(
        taskName: String,
        currentStep: Int,
        totalSteps: Int,
        secondsRemaining: Int
    ) {
        val cancelResumeIntent = Intent(context, PauseResumeReceiver::class.java).apply {
            action = CANCEL_RESUME_ACTION
        }
        val cancelResumePendingIntent = PendingIntent.getBroadcast(
            context, 2, cancelResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, PAUSE_CHANNEL_ID)
            .setContentTitle("🔄 Resuming in $secondsRemaining...")
            .setContentText("$taskName — Step $currentStep of $totalSteps")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setColor(Color.parseColor("#FF00E5FF")) // Cyan
            .setProgress(10, 10 - secondsRemaining, false) // Countdown progress bar
            .addAction(
                android.R.drawable.ic_media_pause,
                "CANCEL RESUME",
                cancelResumePendingIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(PAUSE_NOTIFICATION_ID, notification)
        _notificationState.value = PauseNotificationState.COUNTING_DOWN
    }

    // ============================================================
    // RESUMED NOTIFICATION
    // ============================================================
    
    fun showResumedNotification(taskName: String) {
        val notification = NotificationCompat.Builder(context, PAUSE_CHANNEL_ID)
            .setContentTitle("▶️ Orca RESUMED")
            .setContentText("Continuing: $taskName")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Auto-dismiss after 3 seconds
            .setColor(Color.parseColor("#FF00E676")) // Green
            .build()
        
        NotificationManagerCompat.from(context).notify(PAUSE_NOTIFICATION_ID + 1, notification)
        
        // Remove the persistent notification
        scope.launch {
            delay(3000)
            dismissNotification()
        }
    }

    // ============================================================
    // TASK CANCELLED NOTIFICATION
    // ============================================================
    
    fun showTaskCancelledNotification(taskName: String) {
        val notification = NotificationCompat.Builder(context, PAUSE_CHANNEL_ID)
            .setContentTitle("❌ Task Cancelled")
            .setContentText("$taskName has been cancelled")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setAutoCancel(true)
            .setColor(Color.parseColor("#FFFF1744")) // Error Red
            .build()
        
        NotificationManagerCompat.from(context).notify(PAUSE_NOTIFICATION_ID + 1, notification)
        dismissNotification()
        _notificationState.value = PauseNotificationState.HIDDEN
    }

    // ============================================================
    // DISMISS
    // ============================================================
    
    fun dismissNotification() {
        NotificationManagerCompat.from(context).cancel(PAUSE_NOTIFICATION_ID)
        _notificationState.value = PauseNotificationState.HIDDEN
    }
    
    fun isPauseNotificationActive(): Boolean {
        return _notificationState.value != PauseNotificationState.HIDDEN
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

enum class PauseNotificationState {
    HIDDEN,
    PAUSED,
    COUNTING_DOWN,
    RESUMED
}
