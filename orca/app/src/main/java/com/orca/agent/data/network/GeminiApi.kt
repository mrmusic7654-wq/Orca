package com.orca.agent.data.network
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class GeminiApi @Inject constructor() {
    private val model = GenerativeModel("gemini-2.5-flash", com.orca.agent.BuildConfig.GEMINI_API_KEY)
    suspend fun generateContent(prompt: String, imageBase64: String? = null, maxTokens: Int = 4096): String {
        return try { model.generateContent(content { text(prompt) }).text ?: "No response" } catch (e: Exception) { "Error: ${e.message}" }
    }
    suspend fun embedContent(text: String): FloatArray = FloatArray(768)
}
