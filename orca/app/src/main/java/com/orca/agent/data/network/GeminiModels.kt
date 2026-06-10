package com.orca.agent.data.network

import com.google.gson.annotations.SerializedName

// Gemini API Request Models
data class GeminiContent(
    @SerializedName("role") val role: String,
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String? = null,
    @SerializedName("inline_data") val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("data") val data: String // Base64 encoded
)

data class GeminiRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("generation_config") val generationConfig: GeminiGenerationConfig? = null,
    @SerializedName("safety_settings") val safetySettings: List<GeminiSafetySetting>? = null
)

data class GeminiGenerationConfig(
    @SerializedName("temperature") val temperature: Float = 0.7f,
    @SerializedName("top_p") val topP: Float = 0.95f,
    @SerializedName("top_k") val topK: Int = 40,
    @SerializedName("max_output_tokens") val maxOutputTokens: Int = 4096,
    @SerializedName("stop_sequences") val stopSequences: List<String>? = null
)

data class GeminiSafetySetting(
    @SerializedName("category") val category: String,
    @SerializedName("threshold") val threshold: String
)

// Gemini API Response Models
data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?,
    @SerializedName("prompt_feedback") val promptFeedback: GeminiPromptFeedback?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent?,
    @SerializedName("finish_reason") val finishReason: String?,
    @SerializedName("safety_ratings") val safetyRatings: List<GeminiSafetyRating>?,
    @SerializedName("index") val index: Int
)

data class GeminiPromptFeedback(
    @SerializedName("safety_ratings") val safetyRatings: List<GeminiSafetyRating>?,
    @SerializedName("block_reason") val blockReason: String?
)

data class GeminiSafetyRating(
    @SerializedName("category") val category: String,
    @SerializedName("probability") val probability: String
)

// Gemini Embedding Models
data class GeminiEmbeddingRequest(
    @SerializedName("model") val model: String = "models/embedding-001",
    @SerializedName("content") val content: GeminiContent
)

data class GeminiEmbeddingResponse(
    @SerializedName("embedding") val embedding: GeminiEmbedding?
)

data class GeminiEmbedding(
    @SerializedName("values") val values: List<Float>
)

// Vision Analysis Models
data class GeminiVisionRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("generation_config") val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig(
        temperature = 0.4f,
        maxOutputTokens = 2048
    )
)

data class GeminiVisionResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?
)

// UI Element Detection Response
data class ScreenAnalysisResponse(
    @SerializedName("app_name") val appName: String?,
    @SerializedName("elements") val elements: List<DetectedElement>?,
    @SerializedName("text_content") val textContent: String?,
    @SerializedName("actionable_count") val actionableCount: Int?
)

data class DetectedElement(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String?,
    @SerializedName("bounds") val bounds: DetectedBounds?,
    @SerializedName("clickable") val clickable: Boolean?,
    @SerializedName("description") val description: String?
)

data class DetectedBounds(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("width") val width: Float,
    @SerializedName("height") val height: Float
)
