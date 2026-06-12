package com.orca.agent.execution

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TouchHoldDetector @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _pauseState = MutableStateFlow(PauseState.IDLE)
    val pauseState: StateFlow<PauseState> = _pauseState.asStateFlow()
    private val _countdownValue = MutableStateFlow(0)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()
    private var touchDownTime = 0L
    private var isTouching = false
    private var holdJob: Job? = null
    private var resumeJob: Job? = null

    companion object {
        const val HOLD_DURATION_MS = 3000L
        const val RESUME_COUNTDOWN_SECONDS = 10
        const val MAX_TOUCH_MOVEMENT = 50f
    }

    fun onTouchDown(x: Float, y: Float) {
        touchDownTime = System.currentTimeMillis()
        isTouching = true
        if (_pauseState.value == PauseState.RUNNING) {
            holdJob = scope.launch {
                delay(HOLD_DURATION_MS)
                if (isTouching) {
                    _pauseState.value = PauseState.PAUSED
                }
            }
        }
    }

    fun onTouchMove(x: Float, y: Float): Boolean {
        return isTouching
    }

    fun onTouchUp() {
        isTouching = false
        holdJob?.cancel()
    }

    fun startResumeCountdown(onResume: () -> Unit) {
        _pauseState.value = PauseState.RESUMING
        resumeJob = scope.launch {
            for (i in RESUME_COUNTDOWN_SECONDS downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            _pauseState.value = PauseState.RUNNING
            onResume()
        }
    }

    fun cancelResumeCountdown() {
        resumeJob?.cancel()
        _pauseState.value = PauseState.PAUSED
        _countdownValue.value = 0
    }

    fun start() {}
    fun stop() {}
}

enum class PauseState { IDLE, RUNNING, PAUSED, RESUMING }
