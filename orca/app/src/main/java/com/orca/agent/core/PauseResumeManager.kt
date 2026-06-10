package com.orca.agent.core

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PauseResumeManager @Inject constructor(
    private val context: Context,
    private val notificationManager: PauseResumeNotificationManager,
    private val taskPersistenceManager: TaskPersistenceManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Thread-safe state
    private val stateMutex = Mutex()
    private val _pauseState = MutableStateFlow(PauseState.IDLE)
    val pauseState: StateFlow<PauseState> = _pauseState.asStateFlow()
    
    // Prevents race conditions
    private val isTransitioning = AtomicBoolean(false)
    private val currentResumeRequestId = AtomicInteger(0)
    private val resumeInProgress = AtomicBoolean(false)
    
    // Pending action management
    private val pendingActions = mutableListOf<PendingAction>()
    private var activeAction: PendingAction? = null
    private var currentTaskSnapshot: TaskSnapshot? = null
    
    // Time-sensitive step detection
    private val timeSensitiveKeywords = listOf(
        "payment", "checkout", "confirm", "verify", "otp",
        "2fa", "code", "session", "timeout", "expires"
    )

    // ============================================================
    // PAUSE REQUEST WITH ACTION DRAIN
    // ============================================================
    
    suspend fun requestPause(
        taskName: String,
        currentStep: Int,
        totalSteps: Int,
        currentAction: PendingAction?
    ): PauseResult {
        // Prevent double-triggering
        if (!isTransitioning.compareAndSet(false, true)) {
            return PauseResult.AlreadyPaused
        }
        
        // Check for time-sensitive operation
        val isTimeSensitive = currentAction?.let { action ->
            timeSensitiveKeywords.any { action.description.contains(it, ignoreCase = true) }
        } ?: false
        
        if (isTimeSensitive) {
            isTransitioning.set(false)
            return PauseResult.TimeSensitiveWarning(
                action = currentAction!!.description,
                message = "Current step (${currentAction.description}) is time-sensitive. Pausing may cause it to expire."
            )
        }
        
        return stateMutex.withLock {
            when (_pauseState.value) {
                PauseState.PAUSED -> {
                    isTransitioning.set(false)
                    PauseResult.AlreadyPaused
                }
                PauseState.RESUMING -> {
                    // Cancel the resume countdown
                    cancelResumeCountdown()
                    _pauseState.value = PauseState.PAUSED
                    isTransitioning.set(false)
                    PauseResult.AlreadyPaused
                }
                else -> {
                    // Allow current action to complete (drain)
                    activeAction = currentAction
                    if (currentAction != null && currentAction.isExecuting) {
                        // Wait for action completion (max 5 seconds)
                        withTimeoutOrNull(5000) {
                            while (currentAction.isExecuting) {
                                delay(100)
                            }
                        }
                    }
                    
                    // Save task state to disk (prevents memory leak)
                    saveTaskSnapshot(taskName, currentStep, totalSteps)
                    
                    // Release in-memory resources
                    releaseResources()
                    
                    // Update state
                    _pauseState.value = PauseState.PAUSED
                    
                    // Show notification
                    notificationManager.showPausedNotification(
                        taskName = taskName,
                        currentStep = currentStep,
                        totalSteps = totalSteps,
                        pausedAt = System.currentTimeMillis()
                    )
                    
                    isTransitioning.set(false)
                    PauseResult.Paused
                }
            }
        }
    }

    // ============================================================
    // FORCE PAUSE (No warning, no drain - emergency only)
    // ============================================================
    
    suspend fun forcePause(taskName: String, currentStep: Int, totalSteps: Int) {
        stateMutex.withLock {
            // Cancel everything immediately
            activeAction?.cancel()
            activeAction = null
            pendingActions.clear()
            
            // Emergency save
            saveTaskSnapshot(taskName, currentStep, totalSteps)
            releaseResources()
            
            _pauseState.value = PauseState.PAUSED
            notificationManager.showPausedNotification(taskName, currentStep, totalSteps, System.currentTimeMillis())
        }
    }

    // ============================================================
    // RESUME WITH IDEMPOTENCY
    // ============================================================
    
    suspend fun requestResume(): ResumeResult {
        // Generate unique request ID
        val requestId = currentResumeRequestId.incrementAndGet()
        
        // Prevent duplicate resume requests
        if (!resumeInProgress.compareAndSet(false, true)) {
            return ResumeResult.AlreadyResuming
        }
        
        if (_pauseState.value != PauseState.PAUSED) {
            resumeInProgress.set(false)
            return ResumeResult.NotPaused
        }
        
        // Check if we can resume
        val snapshot = currentTaskSnapshot
        if (snapshot == null) {
            resumeInProgress.set(false)
            return ResumeResult.NoTaskState
        }
        
        return stateMutex.withLock {
            _pauseState.value = PauseState.RESUMING
            
            // Start countdown
            var countdownSeconds = 10
            notificationManager.showResumeCountdown(
                taskName = snapshot.taskName,
                currentStep = snapshot.currentStep,
                totalSteps = snapshot.totalSteps,
                secondsRemaining = countdownSeconds
            )
            
            try {
                while (countdownSeconds > 0) {
                    delay(1000)
                    countdownSeconds--
                    
                    // Check if this request is still the most recent
                    if (requestId != currentResumeRequestId.get()) {
                        // A newer request cancelled this one
                        resumeInProgress.set(false)
                        return ResumeResult.Cancelled
                    }
                    
                    // Check if we were paused again during countdown
                    if (_pauseState.value == PauseState.PAUSED) {
                        resumeInProgress.set(false)
                        return ResumeResult.Cancelled
                    }
                    
                    notificationManager.showResumeCountdown(
                        taskName = snapshot.taskName,
                        currentStep = snapshot.currentStep,
                        totalSteps = snapshot.totalSteps,
                        secondsRemaining = countdownSeconds
                    )
                }
                
                // Countdown complete - verify state one final time
                if (requestId != currentResumeRequestId.get()) {
                    resumeInProgress.set(false)
                    return ResumeResult.Cancelled
                }
                
                // Restore task state
                restoreTaskState(snapshot)
                
                // Navigate back to correct app/screen
                val screenCheck = verifyCurrentScreen(snapshot)
                if (!screenCheck.matches) {
                    // Screen doesn't match - attempt navigation recovery
                    val navigated = navigateToExpectedScreen(snapshot)
                    if (!navigated) {
                        _pauseState.value = PauseState.PAUSED
                        resumeInProgress.set(false)
                        return ResumeResult.ScreenMismatch(
                            expected = screenCheck.expectedApp,
                            actual = screenCheck.actualApp
                        )
                    }
                }
                
                _pauseState.value = PauseState.RUNNING
                notificationManager.showResumedNotification(snapshot.taskName)
                resumeInProgress.set(false)
                ResumeResult.Resumed(snapshot)
                
            } catch (e: CancellationException) {
                resumeInProgress.set(false)
                ResumeResult.Cancelled
            }
        }
    }

    // ============================================================
    // CANCEL RESUME (During Countdown)
    // ============================================================
    
    fun cancelResumeCountdown() {
        currentResumeRequestId.incrementAndGet() // Invalidate current request
        _pauseState.value = PauseState.PAUSED
        resumeInProgress.set(false)
        
        val snapshot = currentTaskSnapshot
        if (snapshot != null) {
            notificationManager.showPausedNotification(
                taskName = snapshot.taskName,
                currentStep = snapshot.currentStep,
                totalSteps = snapshot.totalSteps,
                pausedAt = System.currentTimeMillis()
            )
        }
    }

    // ============================================================
    // MEMORY MANAGEMENT
    // ============================================================
    
    private suspend fun saveTaskSnapshot(taskName: String, currentStep: Int, totalSteps: Int) {
        val snapshot = TaskSnapshot(
            taskName = taskName,
            currentStep = currentStep,
            totalSteps = totalSteps,
            currentScreen = ScreenState.capture(),
            expectedApp = ScreenState.capture().currentApp,
            pausedAt = System.currentTimeMillis(),
            activeChainId = "", // Get from TaskExecutor
            serializedChain = "" // Serialize TaskChain
        )
        currentTaskSnapshot = snapshot
        
        // Persist to disk
        taskPersistenceManager.saveTaskSnapshot(snapshot)
    }
    
    private suspend fun restoreTaskState(snapshot: TaskSnapshot) {
        // Deserialize from disk
        currentTaskSnapshot = taskPersistenceManager.loadTaskSnapshot(snapshot.taskName)
    }
    
    private fun releaseResources() {
        // Clear in-memory screenshots
        // Release context window
        // Garbage collect
        System.gc()
    }

    // ============================================================
    // SCREEN VERIFICATION ON RESUME
    // ============================================================
    
    private suspend fun verifyCurrentScreen(snapshot: TaskSnapshot): ScreenCheckResult {
        val currentScreen = ScreenState.capture()
        val expectedApp = snapshot.expectedApp
        
        return if (currentScreen.currentApp == expectedApp) {
            ScreenCheckResult(matches = true, expectedApp = expectedApp, actualApp = currentScreen.currentApp)
        } else {
            ScreenCheckResult(matches = false, expectedApp = expectedApp, actualApp = currentScreen.currentApp)
        }
    }
    
    private suspend fun navigateToExpectedScreen(snapshot: TaskSnapshot): Boolean {
        // Try to reopen the expected app
        val currentScreen = ScreenState.capture()
        
        if (currentScreen.currentApp != snapshot.expectedApp) {
            // Launch the app
            val intent = context.packageManager.getLaunchIntentForPackage(snapshot.expectedApp)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                delay(1500) // Wait for app to load
                
                // Verify we're in the right app
                val newScreen = ScreenState.capture()
                return newScreen.currentApp == snapshot.expectedApp
            }
        }
        
        return false
    }

    // ============================================================
    // ORPHANED TASK RECOVERY
    // ============================================================
    
    suspend fun recoverOrphanedTask(): TaskSnapshot? {
        // Check for tasks that were paused but lost their notification
        val snapshot = taskPersistenceManager.loadLatestSnapshot()
        if (snapshot != null && snapshot.pausedAt > 0) {
            // Task was paused - restore notification
            notificationManager.showPausedNotification(
                taskName = snapshot.taskName,
                currentStep = snapshot.currentStep,
                totalSteps = snapshot.totalSteps,
                pausedAt = snapshot.pausedAt
            )
            currentTaskSnapshot = snapshot
            _pauseState.value = PauseState.PAUSED
            return snapshot
        }
        return null
    }
}

// ============================================================
// SUPPORTING DATA CLASSES
// ============================================================

data class TaskSnapshot(
    val taskName: String,
    val currentStep: Int,
    val totalSteps: Int,
    val currentScreen: ScreenState,
    val expectedApp: String,
    val pausedAt: Long,
    val activeChainId: String,
    val serializedChain: String
)

data class PendingAction(
    val id: String,
    val description: String,
    val isExecuting: Boolean,
    val startedAt: Long
) {
    fun cancel() {
        // Cancel the action
    }
}

data class ScreenCheckResult(
    val matches: Boolean,
    val expectedApp: String,
    val actualApp: String
)

sealed class PauseResult {
    object Paused : PauseResult()
    object AlreadyPaused : PauseResult()
    data class TimeSensitiveWarning(val action: String, val message: String) : PauseResult()
}

sealed class ResumeResult {
    data class Resumed(val snapshot: TaskSnapshot) : ResumeResult()
    object AlreadyResuming : ResumeResult()
    object NotPaused : ResumeResult()
    object NoTaskState : ResumeResult()
    object Cancelled : ResumeResult()
    data class ScreenMismatch(val expected: String, val actual: String) : ResumeResult()
}

enum class PauseState {
    IDLE,
    RUNNING,
    PAUSED,
    RESUMING
}
