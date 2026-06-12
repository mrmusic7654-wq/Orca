package com.orca.agent.execution
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class OrcaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) { super.onNotificationPosted(sbn) }
}
