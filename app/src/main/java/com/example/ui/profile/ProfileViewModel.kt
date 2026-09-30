package com.example.ui.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudentAiApp
import com.example.data.local.entity.PermissionLogEntity
import com.example.domain.model.AppSettings
import com.example.domain.model.ConfirmationLevel
import com.example.domain.model.LanguageMode
import com.example.permissions.AppPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val selectedTab: ProfileSubSection = ProfileSubSection.PERMISSIONS,
    val permissionStates: Map<AppPermission, Boolean> = emptyMap(),
    val isEmergencyStopped: Boolean = false,
    val isTestingVoice: Boolean = false,
    val voiceTestResult: String = ""
)

enum class ProfileSubSection(val title: String) {
    PERMISSIONS("Permissions"),
    ACCOUNT("Account"),
    VOICE("Voice Match"),
    DATA_CONTROL("Data & Privacy")
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudentAiApp
    private val settingsRepo = app.settingsRepository
    private val permissionManager = app.permissionManager
    private val voiceRecognizer = app.voiceRecognizer
    private val chatRepo = app.chatRepository
    private val studyRepo = app.studyRepository

    val settings: StateFlow<AppSettings> = settingsRepo.settings
    val permissionLogs: StateFlow<List<PermissionLogEntity>> = permissionManager.getLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionStates()
    }

    fun selectSection(section: ProfileSubSection) {
        _uiState.value = _uiState.value.copy(selectedTab = section)
    }

    fun refreshPermissionStates() {
        val states = AppPermission.values().associateWith { perm ->
            permissionManager.isGranted(perm)
        }
        _uiState.value = _uiState.value.copy(permissionStates = states)
    }

    fun updateAssistantName(name: String) {
        settingsRepo.updateSettings(settings.value.copy(assistantName = name))
    }

    fun updateDisplayName(name: String) {
        settingsRepo.updateSettings(settings.value.copy(userDisplayName = name))
    }

    fun updateLanguage(mode: LanguageMode) {
        settingsRepo.updateSettings(settings.value.copy(languageMode = mode))
    }

    fun updateConfirmationLevel(level: ConfirmationLevel) {
        settingsRepo.updateSettings(settings.value.copy(confirmationLevel = level))
    }

    fun toggleVoiceResponse(enabled: Boolean) {
        settingsRepo.updateSettings(settings.value.copy(voiceResponseEnabled = enabled))
    }

    fun updateVoiceSpeed(speed: Float) {
        settingsRepo.updateSettings(settings.value.copy(voiceSpeed = speed))
    }

    fun toggleRobotAnimation(enabled: Boolean) {
        settingsRepo.updateSettings(settings.value.copy(robotAnimationEnabled = enabled))
    }

    fun toggleAutoListen(enabled: Boolean) {
        settingsRepo.updateSettings(settings.value.copy(autoListenAfterResponse = enabled))
    }

    fun togglePrivacyMode(enabled: Boolean) {
        settingsRepo.updateSettings(settings.value.copy(privacyMode = enabled))
    }

    fun toggleConversationMemory(enabled: Boolean) {
        settingsRepo.updateSettings(settings.value.copy(conversationMemoryEnabled = enabled))
    }

    fun toggleVoiceActivation(enabled: Boolean) {
        settingsRepo.updateSettings(settings.value.copy(voiceActivationEnabled = enabled))
    }

    fun updateWakePhrase(phrase: String) {
        settingsRepo.updateSettings(settings.value.copy(wakePhrase = phrase))
    }

    fun toggleEmergencyStop() {
        val newState = !_uiState.value.isEmergencyStopped
        _uiState.value = _uiState.value.copy(isEmergencyStopped = newState)
        settingsRepo.setEmergencyStop(newState)
        if (newState) {
            app.voiceSynthesizer.stop()
            app.voiceRecognizer.stopListening()
            app.agentStateManager.resetToIdle()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatRepo.clearHistory()
            studyRepo.clearNotes()
            settingsRepo.clearAllData()
            refreshPermissionStates()
        }
    }

    fun openAppSettings(context: Context) {
        permissionManager.openAppSettings(context)
    }

    fun testVoiceRecognition() {
        _uiState.value = _uiState.value.copy(
            isTestingVoice = true,
            voiceTestResult = "Say 'Hey Astra' or speak any study question..."
        )
        voiceRecognizer.startListening()
        viewModelScope.launch {
            voiceRecognizer.speechState.collect { speechState ->
                when (speechState) {
                    is com.example.voice.SpeechState.Result -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = false,
                            voiceTestResult = "Heard: \"${speechState.text}\" (Voice recognition working properly!)"
                        )
                    }
                    is com.example.voice.SpeechState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = false,
                            voiceTestResult = "Voice test failed: ${speechState.errorMsg}"
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
