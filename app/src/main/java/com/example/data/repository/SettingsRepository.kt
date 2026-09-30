package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.AppSettings
import com.example.domain.model.ConfirmationLevel
import com.example.domain.model.LanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("student_ai_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    private fun loadSettings(): AppSettings {
        val langStr = prefs.getString("language_mode", LanguageMode.HINGLISH.name) ?: LanguageMode.HINGLISH.name
        val confirmStr = prefs.getString("confirmation_level", ConfirmationLevel.NORMAL.name) ?: ConfirmationLevel.NORMAL.name

        val languageMode = try {
            LanguageMode.valueOf(langStr)
        } catch (_: Exception) {
            LanguageMode.HINGLISH
        }

        val confirmationLevel = try {
            ConfirmationLevel.valueOf(confirmStr)
        } catch (_: Exception) {
            ConfirmationLevel.NORMAL
        }

        return AppSettings(
            assistantName = prefs.getString("assistant_name", "Astra") ?: "Astra",
            userDisplayName = prefs.getString("user_display_name", "Student") ?: "Student",
            languageMode = languageMode,
            voiceResponseEnabled = prefs.getBoolean("voice_response_enabled", true),
            voiceSpeed = prefs.getFloat("voice_speed", 1.0f),
            robotAnimationEnabled = prefs.getBoolean("robot_animation_enabled", true),
            autoListenAfterResponse = prefs.getBoolean("auto_listen_after_response", false),
            confirmationLevel = confirmationLevel,
            privacyMode = prefs.getBoolean("privacy_mode", true),
            conversationMemoryEnabled = prefs.getBoolean("conversation_memory_enabled", true),
            voiceActivationEnabled = prefs.getBoolean("voice_activation_enabled", false),
            wakePhrase = prefs.getString("wake_phrase", "Hey Astra") ?: "Hey Astra"
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().apply {
            putString("assistant_name", newSettings.assistantName)
            putString("user_display_name", newSettings.userDisplayName)
            putString("language_mode", newSettings.languageMode.name)
            putBoolean("voice_response_enabled", newSettings.voiceResponseEnabled)
            putFloat("voice_speed", newSettings.voiceSpeed)
            putBoolean("robot_animation_enabled", newSettings.robotAnimationEnabled)
            putBoolean("auto_listen_after_response", newSettings.autoListenAfterResponse)
            putString("confirmation_level", newSettings.confirmationLevel.name)
            putBoolean("privacy_mode", newSettings.privacyMode)
            putBoolean("conversation_memory_enabled", newSettings.conversationMemoryEnabled)
            putBoolean("voice_activation_enabled", newSettings.voiceActivationEnabled)
            putString("wake_phrase", newSettings.wakePhrase)
            apply()
        }
        _settings.value = newSettings
    }

    fun setEmergencyStop(stopped: Boolean) {
        _isEmergencyStopped.value = stopped
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        _settings.value = loadSettings()
    }
}
