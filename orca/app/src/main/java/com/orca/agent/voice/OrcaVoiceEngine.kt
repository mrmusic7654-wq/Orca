package com.orca.agent.voice
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class OrcaVoiceEngine @Inject constructor(private val ctx: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    fun initialize() { tts = TextToSpeech(ctx, this) }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US }
    fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "orca") }
    fun shutdown() { tts?.shutdown() }
}
