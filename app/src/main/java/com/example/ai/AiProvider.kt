package com.example.ai

import kotlinx.coroutines.flow.Flow

data class AiMessage(
    val role: String, // "user" or "model"
    val text: String,
    val imageBase64: String? = null
)

sealed class AiResponse {
    data class Success(val text: String) : AiResponse()
    data class StreamChunk(val chunk: String) : AiResponse()
    data class Error(val message: String, val isQuotaLimit: Boolean = false) : AiResponse()
}

interface AiProvider {
    suspend fun generateResponse(
        prompt: String,
        history: List<AiMessage> = emptyList(),
        imageBase64: String? = null,
        systemInstruction: String? = null
    ): AiResponse

    fun streamResponse(
        prompt: String,
        history: List<AiMessage> = emptyList(),
        systemInstruction: String? = null
    ): Flow<String>

    suspend fun parseCommandIntent(userCommand: String): String?
}
