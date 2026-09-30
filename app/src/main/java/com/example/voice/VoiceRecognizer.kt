package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Processing(val partial: String = "") : SpeechState()
    data class Result(val text: String) : SpeechState()
    data class Error(val errorMsg: String) : SpeechState()
}

interface VoiceRecognizer {
    val speechState: StateFlow<SpeechState>
    fun startListening(locale: Locale = Locale("hi", "IN"))
    fun stopListening()
    fun destroy()
}

class AndroidVoiceRecognizer(private val context: Context) : VoiceRecognizer {

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    private fun initRecognizer() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechState.value = SpeechState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        _speechState.value = SpeechState.Listening
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _speechState.value = SpeechState.Processing()
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error: $error"
                        }
                        _speechState.value = SpeechState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _speechState.value = SpeechState.Result(text)
                        } else {
                            _speechState.value = SpeechState.Error("Could not hear speech clearly")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _speechState.value = SpeechState.Processing(text)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    override fun startListening(locale: Locale) {
        initRecognizer()
        if (speechRecognizer == null) {
            _speechState.value = SpeechState.Error("Speech recognition not available on this device")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            _speechState.value = SpeechState.Listening
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("Could not start microphone: ${e.message}")
        }
    }

    override fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    override fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        _speechState.value = SpeechState.Idle
    }
}
