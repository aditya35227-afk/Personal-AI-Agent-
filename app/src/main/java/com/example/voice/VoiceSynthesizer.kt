package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface VoiceSynthesizer {
    val isSpeaking: StateFlow<Boolean>
    fun speak(text: String, speechRate: Float = 1.0f, onComplete: (() -> Unit)? = null)
    fun stop()
    fun destroy()
}

class AndroidVoiceSynthesizer(private val context: Context) : VoiceSynthesizer, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var completionCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Prefer Indian English / Hindi for student Hinglish support
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    completionCallback?.invoke()
                    completionCallback = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    completionCallback?.invoke()
                    completionCallback = null
                }
            })
        } else {
            Log.e("VoiceSynthesizer", "TextToSpeech init failed with status: $status")
        }
    }

    override fun speak(text: String, speechRate: Float, onComplete: (() -> Unit)?) {
        if (!isInitialized || tts == null) {
            onComplete?.invoke()
            return
        }

        stop()
        completionCallback = onComplete
        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

        // Clean markdown tags so speech sounds natural
        val cleanText = text
            .replace(Regex("[*#_`~>|\\[\\]]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val utteranceId = "student_agent_${System.currentTimeMillis()}"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        if (isInitialized) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
        }
        _isSpeaking.value = false
    }

    override fun destroy() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
