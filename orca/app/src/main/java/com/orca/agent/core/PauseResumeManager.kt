package com.orca.agent.core

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PauseResumeManager @Inject constructor(
    private val context: Context,
    private val notificationManager: PauseResumeNotificationManager
) {
    private val stateMutex = Mutex()
    private val _pauseState = MutableStateFlow(PauseState.IDLE)
    val pauseState: StateFlow<PauseState> = _pauseState.asStateFlow()
    private val isTransitioning = AtomicBoolean(false)

    suspend fun requestPause(taskName: String, currentStep: Int, totalSteps: Int): PauseResult {
        if (!isTransitioning.compareAndSet(false, true)) return PauseResult.AlreadyPaused
        stateMutex.withLock {
            _pauseState.value = PauseState.PAUSED
            notificationManager.showPausedNotification(taskName, currentStep, totalSteps, System.currentTimeMillis())
        }
        isTransitioning.set(false)
        return PauseResult.Paused
    }

    suspend fun requestResume(): ResumeResult {
        stateMutex.withLock {
            _pauseState.value = PauseState.RUNNING
        }
        return ResumeResult.Resumed
    }

    fun cancelResumeCountdown() {
        _pauseState.value = PauseState.PAUSED
    }

    fun startResumeCountdown(onResume: () -> Unit) {
        _pauseState.value = PauseState.RESUMING
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            _pauseState.value = PauseState.RUNNING
            onResume()
        }
    }
}

enum class PauseState { IDLE, RUNNING, PAUSED, RESUMING, CANCELLED }

sealed class PauseResult {
    object Paused : PauseResult()
    object AlreadyPaused : PauseResult()
    data class TimeSensitiveWarning(val action: String, val message: String) : PauseResult()
}

sealed class ResumeResult {
    object Resumed : ResumeResult()
    object AlreadyResuming : ResumeResult()
    object NotPaused : ResumeResult()
    object NoTaskState : ResumeResult()
    object Cancelled : ResumeResult()
    data class ScreenMismatch(val expected: String, val actual: String) : ResumeResult()
}
