// app/src/main/java/com/orca/agent/voice/OrcaVoiceEngine.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrcaVoiceEngine @Inject constructor(
    private val context: Context
) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private val _speechQueue = MutableStateFlow<List<SpeechItem>>(emptyList())
    val speechQueue: StateFlow<List<SpeechItem>> = _speechQueue.asStateFlow()
    
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    fun initialize() {
        tts = TextToSpeech(context, this)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let {
                it.language = Locale.US
                
                // Set voice for Orca personality - deep, slightly synthetic
                val voice = it.voices.find { v ->
                    v.name.contains("en-us", ignoreCase = true) &&
                    v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS)
                }
                voice?.let { selectedVoice -> it.voice = selectedVoice }
                
                it.setSpeechRate(0.95f) // Slightly slower, deliberate
                it.setPitch(0.9f)       // Slightly deeper
                
                it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        processNextInQueue()
                    }
                    
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        processNextInQueue()
                    }
                })
            }
        }
    }
    
    suspend fun speak(text: String, priority: SpeechPriority = SpeechPriority.NORMAL) {
        val item = SpeechItem(
            id = UUID.randomUUID().toString(),
            text = text,
            priority = priority,
            timestamp = System.currentTimeMillis()
        )
        
        val currentQueue = _speechQueue.value.toMutableList()
        
        // Insert based on priority
        if (priority == SpeechPriority.HIGH) {
            currentQueue.add(0, item)
        } else {
            currentQueue.add(item)
        }
        
        _speechQueue.value = currentQueue
        
        if (!_isSpeaking.value) {
            processNextInQueue()
        }
    }
    
    private fun processNextInQueue() {
        val queue = _speechQueue.value.toMutableList()
        if (queue.isEmpty()) return
        
        // Sort by priority then timestamp
        queue.sortBy { it.priority.ordinal }
        
        val nextItem = queue.removeAt(0)
        _speechQueue.value = queue
        
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()
        
        audioManager.requestAudioFocus(audioFocusRequest)
        
        tts?.speak(
            nextItem.text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            nextItem.id
        )
    }
    
    fun stop() {
        tts?.stop()
        _speechQueue.value = emptyList()
        _isSpeaking.value = false
    }
    
    fun shutdown() {
        tts?.shutdown()
    }
}

data class SpeechItem(
    val id: String,
    val text: String,
    val priority: SpeechPriority,
    val timestamp: Long
)

enum class SpeechPriority {
    LOW,        // Casual observations
    NORMAL,     // Standard responses
    HIGH,       // Important alerts
    CRITICAL    // Security warnings, urgent matters
}
