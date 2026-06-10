package com.orca.agent.execution

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.orca.agent.R
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
    
    // Touch tracking
    private var touchDownTime = 0L
    private var touchX = 0f
    private var touchY = 0f
    private var isTouching = false
    private var holdTimer: CountDownTimer? = null
    private var resumeTimer: CountDownTimer? = null
    
    // Pause overlay
    private var pauseOverlay: View? = null
    private var windowManager: WindowManager? = null
    
    // Duration thresholds
    companion object {
        const val HOLD_DURATION_MS = 3000L // 3 seconds to pause
        const val RESUME_COUNTDOWN_SECONDS = 10
        const val MAX_TOUCH_MOVEMENT = 50f // Max pixels finger can move (prevents scroll from triggering)
    }

    // ============================================================
    // TOUCH EVENT PROCESSING
    // ============================================================
    
    fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = System.currentTimeMillis()
                touchX = event.rawX
                touchY = event.rawY
                isTouching = true
                
                // Start hold detection if Orca is currently executing
                if (_pauseState.value == PauseState.RUNNING) {
                    startHoldDetection()
                }
                
                // If already paused, check for resume gesture
                if (_pauseState.value == PauseState.PAUSED) {
                    // Double-tap to resume (alternative to notification)
                    detectDoubleTapForResume()
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Check if finger moved too much (user is scrolling, not holding)
                val dx = kotlin.math.abs(event.rawX - touchX)
                val dy = kotlin.math.abs(event.rawY - touchY)
                
                if (dx > MAX_TOUCH_MOVEMENT || dy > MAX_TOUCH_MOVEMENT) {
                    cancelHoldDetection()
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                cancelHoldDetection()
            }
        }
    }

    // ============================================================
    // HOLD DETECTION FOR PAUSE
    // ============================================================
    
    private fun startHoldDetection() {
        cancelHoldDetection()
        
        holdTimer = object : CountDownTimer(HOLD_DURATION_MS, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                _countdownValue.value = secondsRemaining
                
                // Update overlay opacity based on progress
                val progress = 1f - (millisUntilFinished.toFloat() / HOLD_DURATION_MS.toFloat())
                updatePauseOverlay(progress, secondsRemaining)
            }
            
            override fun onFinish() {
                if (isTouching) {
                    triggerPause()
                }
            }
        }.start()
        
        // Show initial overlay
        showPauseOverlay()
    }
    
    private fun cancelHoldDetection() {
        holdTimer?.cancel()
        holdTimer = null
        _countdownValue.value = 0
        hidePauseOverlay()
    }

    // ============================================================
    // PAUSE TRIGGER
    // ============================================================
    
    private fun triggerPause() {
        _pauseState.value = PauseState.PAUSED
        _countdownValue.value = 0
        
        // Flash red overlay
        showPauseConfirmationOverlay()
        
        // Vibrate to confirm
        vibratePauseConfirmation()
        
        // Update notification
        updatePauseNotification()
        
        // Cancel hold timer
        cancelHoldDetection()
        
        // Auto-hide overlay after 2 seconds
        scope.launch {
            delay(2000)
            hidePauseOverlay()
        }
    }

    // ============================================================
    // RESUME FROM NOTIFICATION
    // ============================================================
    
    fun startResumeCountdown(onResume: () -> Unit) {
        if (_pauseState.value != PauseState.PAUSED) return
        
        _pauseState.value = PauseState.RESUMING
        
        resumeTimer = object : CountDownTimer(RESUME_COUNTDOWN_SECONDS * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                _countdownValue.value = seconds
                updateResumeCountdownNotification(seconds)
            }
            
            override fun onFinish() {
                _pauseState.value = PauseState.RUNNING
                _countdownValue.value = 0
                updateResumeCompleteNotification()
                onResume()
            }
        }.start()
    }
    
    fun cancelResumeCountdown() {
        resumeTimer?.cancel()
        resumeTimer = null
        _pauseState.value = PauseState.PAUSED
        _countdownValue.value = 0
        updatePauseNotification()
    }

    // ============================================================
    // TWO-USER MODE
    // ============================================================
    
    fun enableTwoUserMode() {
        // In two-user mode:
        // - Pause is more sensitive (2 second hold instead of 3)
        // - Resume requires explicit notification tap (no double-tap)
        // - Tasks are saved with user-specific tags
        // - Each user has their own notification channel
    }
    
    fun isSecondUserActive(): Boolean {
        return _pauseState.value == PauseState.PAUSED
    }
    
    fun getEstimatedPauseDuration(): Long {
        return System.currentTimeMillis() - pauseStartTime
    }
    
    private var pauseStartTime = 0L

    // ============================================================
    // OVERLAY MANAGEMENT
    // ============================================================
    
    private fun showPauseOverlay() {
        // Red translucent overlay that intensifies as hold continues
        if (pauseOverlay == null) {
            createOverlayView()
        }
        pauseOverlay?.visibility = View.VISIBLE
        pauseOverlay?.alpha = 0.2f
    }
    
    private fun updatePauseOverlay(progress: Float, countdown: Int) {
        pauseOverlay?.alpha = 0.2f + (progress * 0.5f) // 0.2 to 0.7
        val countdownText = pauseOverlay?.findViewById<TextView>(R.id.countdown_text)
        countdownText?.text = "$countdown"
        countdownText?.visibility = View.VISIBLE
    }
    
    private fun showPauseConfirmationOverlay() {
        pauseOverlay?.alpha = 0.8f
        pauseOverlay?.setBackgroundColor(0xCCFF0000.toInt()) // Solid red
        val countdownText = pauseOverlay?.findViewById<TextView>(R.id.countdown_text)
        countdownText?.text = "PAUSED"
        countdownText?.textSize = 32f
    }
    
    private fun hidePauseOverlay() {
        pauseOverlay?.visibility = View.GONE
        pauseOverlay?.alpha = 0f
    }
    
    private fun createOverlayView() {
        val overlay = FrameLayout(context).apply {
            setBackgroundColor(0x33FF0000.toInt()) // Transparent red
            isClickable = false
            isFocusable = false
        }
        
        val countdownText = TextView(context).apply {
            id = R.id.countdown_text
            text = ""
            textSize = 48f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        
        overlay.addView(countdownText, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        windowManager?.addView(overlay, params)
        pauseOverlay = overlay
    }

    // ============================================================
    // NOTIFICATION MANAGEMENT
    // ============================================================
    
    private fun updatePauseNotification() {
        // Show persistent notification:
        // "Orca PAUSED"
        // "Task: Booking flight to Tokyo"
        // "Progress: Step 7 of 20"
        // "Paused at: 2:35 PM"
        // [RESUME] [CANCEL TASK]
    }
    
    private fun updateResumeCountdownNotification(seconds: Int) {
        // Update notification:
        // "Resuming in $seconds..."
        // Progress bar showing countdown
        // [CANCEL RESUME]
    }
    
    private fun updateResumeCompleteNotification() {
        // Brief notification:
        // "Orca Resumed"
        // "Continuing: Booking flight to Tokyo"
        // Auto-dismiss after 2 seconds
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun vibratePauseConfirmation() {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        // Double pulse: pause
        vibrator?.vibrate(android.os.VibrationEffect.createWaveform(
            longArrayOf(0, 100, 100, 200),
            intArrayOf(0, 255, 0, 255),
            -1
        ))
    }
    
    private fun detectDoubleTapForResume() {
        // Track tap timing
        // If two taps within 500ms, trigger resume countdown
    }
    
    private val context: android.content.Context
        get() = com.orca.agent.OrcaApplication.instance
}

enum class PauseState {
    IDLE,       // Orca is not running any task
    RUNNING,    // Task is executing
    PAUSED,     // Task is paused by user
    RESUMING,   // Countdown to resume
    CANCELLED   // Task was cancelled during pause
}
