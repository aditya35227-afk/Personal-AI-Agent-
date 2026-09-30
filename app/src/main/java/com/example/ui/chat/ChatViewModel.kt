package com.example.ui.chat

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudentAiApp
import com.example.ai.AiMessage
import com.example.ai.AiResponse
import com.example.data.local.entity.ChatMessageEntity
import com.example.voice.SpeechState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class ChatUiState(
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val streamingChunk: String = "",
    val selectedImageUri: Uri? = null,
    val errorMessage: String? = null,
    val isMicActive: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudentAiApp
    private val chatRepo = app.chatRepository
    private val aiProvider = app.aiProvider
    private val voiceRecognizer = app.voiceRecognizer
    private val voiceSynthesizer = app.voiceSynthesizer
    private val settingsRepo = app.settingsRepository

    val messages: StateFlow<List<ChatMessageEntity>> = chatRepo.messages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeGenerationJob: Job? = null

    init {
        viewModelScope.launch {
            voiceRecognizer.speechState.collect { speechState ->
                when (speechState) {
                    is SpeechState.Listening -> {
                        _uiState.value = _uiState.value.copy(isMicActive = true)
                    }
                    is SpeechState.Processing -> {
                        _uiState.value = _uiState.value.copy(
                            isMicActive = true,
                            inputText = speechState.partial
                        )
                    }
                    is SpeechState.Result -> {
                        _uiState.value = _uiState.value.copy(
                            isMicActive = false,
                            inputText = speechState.text
                        )
                        sendMessage(isVoice = true)
                    }
                    is SpeechState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isMicActive = false,
                            errorMessage = speechState.errorMsg
                        )
                    }
                    SpeechState.Idle -> {
                        _uiState.value = _uiState.value.copy(isMicActive = false)
                    }
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText, errorMessage = null)
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun toggleVoiceInput() {
        if (_uiState.value.isMicActive) {
            voiceRecognizer.stopListening()
            _uiState.value = _uiState.value.copy(isMicActive = false)
        } else {
            voiceSynthesizer.stop()
            voiceRecognizer.startListening()
        }
    }

    fun sendMessage(isVoice: Boolean = false) {
        val query = _uiState.value.inputText.trim()
        val imageUri = _uiState.value.selectedImageUri
        if (query.isBlank() && imageUri == null) return

        val userText = if (query.isNotBlank()) query else "Analyze this study image"
        _uiState.value = _uiState.value.copy(
            inputText = "",
            selectedImageUri = null,
            isGenerating = true,
            streamingChunk = "",
            errorMessage = null
        )

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            // Save user message to Room
            chatRepo.saveMessage(
                role = "user",
                content = userText,
                isVoicePrompt = isVoice,
                attachedImageUri = imageUri?.toString()
            )

            // Convert image to base64 if present
            var base64Image: String? = null
            if (imageUri != null) {
                base64Image = uriToBase64(imageUri)
            }

            // Prepare conversation history
            val history = messages.value.takeLast(10).map { msg ->
                AiMessage(role = msg.role, text = msg.content)
            }

            val fullResponse = StringBuilder()
            try {
                aiProvider.streamResponse(userText, history).collect { chunk ->
                    fullResponse.append(chunk)
                    _uiState.value = _uiState.value.copy(
                        streamingChunk = fullResponse.toString()
                    )
                }

                val finalContent = fullResponse.toString().ifBlank {
                    // Fallback to non-streaming if stream was empty
                    when (val res = aiProvider.generateResponse(userText, history, base64Image)) {
                        is AiResponse.Success -> res.text
                        is AiResponse.Error -> res.message
                        else -> "No response generated."
                    }
                }

                // Save assistant response to Room
                chatRepo.saveMessage(
                    role = "assistant",
                    content = finalContent
                )

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    streamingChunk = ""
                )

                // Optional speech
                if (settingsRepo.settings.value.voiceResponseEnabled && isVoice) {
                    voiceSynthesizer.speak(finalContent.take(250))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "Error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        if (_uiState.value.streamingChunk.isNotBlank()) {
            val partial = _uiState.value.streamingChunk
            viewModelScope.launch {
                chatRepo.saveMessage(role = "assistant", content = "$partial [Stopped]")
            }
        }
        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            streamingChunk = ""
        )
    }

    fun regenerateLastResponse() {
        val lastUserMessage = messages.value.lastOrNull { it.role == "user" } ?: return
        _uiState.value = _uiState.value.copy(inputText = lastUserMessage.content)
        sendMessage()
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepo.clearHistory()
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            chatRepo.deleteMessage(id)
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognizer.destroy()
        voiceSynthesizer.stop()
    }
}
