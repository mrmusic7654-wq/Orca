package com.orca.agent.data.network

data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(val role: String, val parts: List<GeminiPart>)
data class GeminiPart(val text: String? = null)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContent?, val finishReason: String?)
