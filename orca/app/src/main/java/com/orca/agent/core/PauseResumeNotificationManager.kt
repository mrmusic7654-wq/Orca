package com.orca.agent.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.orca.agent.R
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PauseResumeNotificationManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "orca_pause_channel"
        const val NOTIFICATION_ID = 2001
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Orca Task Control", NotificationManager.IMPORTANCE_HIGH)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showPausedNotification(taskName: String, currentStep: Int, totalSteps: Int, pausedAt: Long) {
        createChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Orca PAUSED")
            .setContentText("$taskName - Step $currentStep of $totalSteps")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showResumeCountdown(taskName: String, currentStep: Int, totalSteps: Int, secondsRemaining: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Resuming in $secondsRemaining...")
            .setContentText("$taskName - Step $currentStep of $totalSteps")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setOngoing(true)
            .setProgress(10, 10 - secondsRemaining, false)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showResumedNotification(taskName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Orca RESUMED")
            .setContentText("Continuing: $taskName")
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setAutoCancel(true)
            .setTimeoutAfter(3000)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
    }

    fun showTaskCancelledNotification(taskName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Task Cancelled")
            .setContentText(taskName)
            .setSmallIcon(R.drawable.ic_orca_orb)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
        dismissNotification()
    }

    fun dismissNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
