package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiAiProvider : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val defaultSystemInstruction = """
        You are "Astra", an intelligent, privacy-first, friendly personal AI assistant created specifically for students in school and college.
        Guidelines:
        1. Always be concise, encouraging, and academically clear.
        2. Seamlessly understand and converse in English, Hindi, and Hinglish (natural mix).
        3. Explain homework questions step-by-step, not just giving dry answers.
        4. When explaining science, math, or coding, use intuitive analogies and bullet points.
        5. Support study revisions, flashcards, concept breakdowns, and short quizzes.
        6. When asked about actions (alarms, timers, apps, device controls), guide the user or format structured intent.
        7. Keep responses polite, safe, student-friendly, and free from fluff.
    """.trimIndent()

    override suspend fun generateResponse(
        prompt: String,
        history: List<AiMessage>,
        imageBase64: String?,
        systemInstruction: String?
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineFallback(prompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val jsonBody = buildRequestBody(prompt, history, imageBase64, systemInstruction ?: defaultSystemInstruction)

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                if (response.code == 429) {
                    return@withContext AiResponse.Error(
                        "AI quota limit temporarily reached. You can still use all local voice actions, study timer, and offline tools!",
                        isQuotaLimit = true
                    )
                }
                Log.e("GeminiAiProvider", "API error: ${response.code} $responseBody")
                return@withContext AiResponse.Error("API request error (${response.code}). Using offline assistant mode.")
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return@withContext AiResponse.Success(text)
                }
            }
            AiResponse.Success("No clear response generated. Please rephrase your query.")
        } catch (e: Exception) {
            Log.e("GeminiAiProvider", "Network error calling Gemini", e)
            AiResponse.Error("Network error: ${e.localizedMessage ?: "Unable to connect"}. Offline mode active.")
        }
    }

    override fun streamResponse(
        prompt: String,
        history: List<AiMessage>,
        systemInstruction: String?
    ): Flow<String> = flow {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val fallback = generateOfflineFallback(prompt)
            if (fallback is AiResponse.Success) {
                emit(fallback.text)
            } else if (fallback is AiResponse.Error) {
                emit(fallback.message)
            }
            return@flow
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:streamGenerateContent?alt=sse&key=$apiKey"
        val jsonBody = buildRequestBody(prompt, history, null, systemInstruction ?: defaultSystemInstruction)

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit("Error connecting to Gemini API (${response.code}).")
                return@flow
            }

            val inputStream = response.body?.byteStream()
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val rawLine = line?.trim() ?: continue
                    if (!rawLine.startsWith("data:")) continue
                    val payload = rawLine.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue
                    try {
                        val chunkObj = JSONObject(payload)
                        val candidates = chunkObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                if (text.isNotEmpty()) {
                                    emit(text)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            emit("Connection interrupted: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun parseCommandIntent(userCommand: String): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        val structuredPrompt = """
            You are a strict action parser for a student assistant app.
            Analyze the following user command:
            "$userCommand"
            
            Supported intents allowlist:
            - SET_ALARM (params: hour [0-23], minute [0-59], label)
            - START_TIMER (params: seconds)
            - OPEN_APP (params: appName)
            - TORCH_ON / TORCH_OFF
            - SET_VOLUME (params: level [0-100])
            - CALL_CONTACT (params: contactName, requiresConfirmation: true)
            - SEND_SMS (params: contactName, message, requiresConfirmation: true)
            - STUDY_TIMER (params: minutes, subject)
            - CREATE_QUIZ (params: topic, questionCount)
            - EXPLAIN_CONCEPT (params: topic)
            - SOCIAL_CAPTION (params: topic, platform)
            - UNKNOWN
            
            Return ONLY a raw JSON object with:
            {
              "intent": "<INTENT_NAME>",
              "parameters": { ... },
              "requires_confirmation": <true/false>,
              "explanation": "<short Hindi/English explanation of intent>"
            }
            Do not include Markdown backticks or extra commentary.
        """.trimIndent()

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val json = JSONObject().apply {
                val contentsArr = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply { put("text", structuredPrompt) })
                    }
                    put("parts", partsArr)
                }
                contentsArr.put(contentObj)
                put("contents", contentsArr)
            }

            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val jsonResponse = JSONObject(body)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val text = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.getJSONObject(0)
                    ?.optString("text")
                return@withContext text?.trim()
            }
        } catch (_: Exception) {}
        null
    }

    private fun buildRequestBody(
        prompt: String,
        history: List<AiMessage>,
        imageBase64: String?,
        systemInstructionText: String
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val sysInstructionObj = JSONObject().apply {
            val parts = JSONArray().apply {
                put(JSONObject().apply { put("text", systemInstructionText) })
            }
            put("parts", parts)
        }
        root.put("systemInstruction", sysInstructionObj)

        val contents = JSONArray()
        // Conversation history
        for (msg in history) {
            val role = if (msg.role == "user") "user" else "model"
            val cObj = JSONObject().apply {
                put("role", role)
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", msg.text) })
                }
                put("parts", parts)
            }
            contents.put(cObj)
        }

        // Current turn
        val currentTurn = JSONObject().apply {
            put("role", "user")
            val parts = JSONArray()
            parts.put(JSONObject().apply { put("text", prompt) })
            if (!imageBase64.isNullOrBlank()) {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", imageBase64)
                }
                parts.put(JSONObject().apply { put("inlineData", inlineData) })
            }
            put("parts", parts)
        }
        contents.put(currentTurn)
        root.put("contents", contents)

        // Config
        val config = JSONObject().apply {
            put("temperature", 0.7)
            put("topP", 0.95)
        }
        root.put("generationConfig", config)

        return root
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
    }

    private fun generateOfflineFallback(prompt: String): AiResponse {
        val lower = prompt.lowercase()
        return when {
            lower.contains("photosynthesis") -> AiResponse.Success(
                "🌱 **Photosynthesis (प्रकाश संश्लेषण) Step-by-Step:**\n" +
                "1. **Light Reaction:** Chlorophyll absorbs sunlight and splits water (H2O) into Oxygen (O2) and Hydrogen.\n" +
                "2. **Dark Reaction (Calvin Cycle):** Carbon dioxide (CO2) is converted into Glucose (C6H12O6) for plant energy.\n" +
                "Equation: 6CO2 + 6H2O + Sunlight -> C6H12O6 + 6O2"
            )
            lower.contains("newton") || lower.contains("gravity") -> AiResponse.Success(
                "🍎 **Newton's Laws of Motion:**\n" +
                "1. **Inertia:** An object stays at rest or motion unless an external force acts on it.\n" +
                "2. **Force:** Force = mass x acceleration (F = m * a).\n" +
                "3. **Action-Reaction:** Every action has an equal and opposite reaction."
            )
            lower.contains("quiz") -> AiResponse.Success(
                "📝 **Quick Student Quiz:**\n" +
                "Q1: What is the powerhouse of the cell?\n" +
                "A) Nucleus  B) Mitochondria  C) Ribosome\n\n" +
                "Q2: In Python, which keyword defines a function?\n" +
                "A) func  B) def  C) function\n\n" +
                "Reply with your answers!"
            )
            lower.contains("caption") || lower.contains("instagram") -> AiResponse.Success(
                "📸 **Student Social Media Caption:**\n" +
                "\"Late nights, big dreams, and endless cups of chai. ☕📚 Building my future one chapter at a time!\"\n\n" +
                "Hashtags: #StudentLife #StudyGram #FutureEngineers #FocusMode #Hustle"
            )
            else -> AiResponse.Success(
                "Namaste! I am your student assistant Astra. I am ready to explain concepts, help with homework, and run study timers. (Configure your Gemini API key in Secrets for full live AI responses)."
            )
        }
    }
}
