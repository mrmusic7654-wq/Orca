package com.orca.agent.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class PauseResumeReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var orcaCore: OrcaCore
    
    @Inject
    lateinit var pauseResumeNotificationManager: PauseResumeNotificationManager
    
    @Inject
    lateinit var touchHoldDetector: com.orca.agent.execution.TouchHoldDetector
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PauseResumeNotificationManager.RESUME_ACTION -> {
                handleResumeAction()
            }
            PauseResumeNotificationManager.CANCEL_ACTION -> {
                handleCancelAction()
            }
            PauseResumeNotificationManager.CANCEL_RESUME_ACTION -> {
                handleCancelResumeAction()
            }
        }
    }
    
    private fun handleResumeAction() {
        // Start the 10-second countdown
        touchHoldDetector.startResumeCountdown {
            // This is called when countdown finishes
            scope.launch {
                orcaCore.taskExecutor.confirmAction(
                    chainId = orcaCore.activeTaskChain.value?.id ?: "",
                    nodeId = ""
                )
                pauseResumeNotificationManager.showResumedNotification(
                    orcaCore.activeTaskChain.value?.goal ?: "Task"
                )
            }
        }
    }
    
    private fun handleCancelAction() {
        // Cancel the entire task
        scope.launch {
            orcaCore.taskExecutor.cancelChain(
                orcaCore.activeTaskChain.value?.id ?: ""
            )
            pauseResumeNotificationManager.showTaskCancelledNotification(
                orcaCore.activeTaskChain.value?.goal ?: "Task"
            )
        }
    }
    
    private fun handleCancelResumeAction() {
        // User changed their mind during countdown
        touchHoldDetector.cancelResumeCountdown()
        pauseResumeNotificationManager.showPausedNotification(
            taskName = orcaCore.activeTaskChain.value?.goal ?: "Task",
            currentStep = orcaCore.activeTaskChain.value?.currentNodeIndex ?: 0,
            totalSteps = orcaCore.activeTaskChain.value?.nodes?.size ?: 0,
            pausedAt = System.currentTimeMillis()
        )
    }
}
