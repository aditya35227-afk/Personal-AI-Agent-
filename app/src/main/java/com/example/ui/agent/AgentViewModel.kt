package com.example.ui.agent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudentAiApp
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RoutineEntity
import com.example.domain.model.CommandIntent
import com.example.domain.model.ExecutionResult
import com.example.domain.model.ExecutionState
import com.example.domain.model.FeatureCapability
import com.example.domain.model.FeatureId
import com.example.permissions.AppPermission
import com.example.voice.SpeechState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AgentUiState(
    val isAvatarVisible: Boolean = false,
    val selectedSubCategory: AgentSubCategory = AgentSubCategory.OVERVIEW,
    val isListening: Boolean = false,
    val activeTimerSecondsRemaining: Int = 0,
    val isTimerActive: Boolean = false,
    val pendingConfirmationIntent: CommandIntent? = null,
    val requiredPermissionToPrompt: AppPermission? = null
)

enum class AgentSubCategory(val title: String) {
    OVERVIEW("Assistant"),
    STUDY_AGENT("Study Agent"),
    PHONE_AGENT("Phone Agent"),
    SOCIAL_MEDIA("Social Creator"),
    AUTOMATIONS("Routines")
}

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudentAiApp
    private val agentStateManager = app.agentStateManager
    private val voiceRecognizer = app.voiceRecognizer
    private val studyRepo = app.studyRepository
    private val permissionManager = app.permissionManager
    private val settingsRepo = app.settingsRepository

    val agentState: StateFlow<ExecutionState> = agentStateManager.state
    val currentQuery: StateFlow<String> = agentStateManager.currentQuery
    val lastResult: StateFlow<ExecutionResult?> = agentStateManager.lastResult

    val notes: StateFlow<List<NoteEntity>> = studyRepo.notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val routines: StateFlow<List<RoutineEntity>> = studyRepo.routines.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val capabilities: List<FeatureCapability> = FeatureCapability.getAllCapabilities()

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            agentStateManager.state.collect { state ->
                when (state) {
                    ExecutionState.LISTENING,
                    ExecutionState.THINKING,
                    ExecutionState.EXECUTING,
                    ExecutionState.CONFIRMATION_REQUIRED,
                    ExecutionState.PERMISSION_REQUIRED -> {
                        _uiState.value = _uiState.value.copy(isAvatarVisible = true)
                    }
                    ExecutionState.SUCCESS, ExecutionState.ERROR -> {
                        _uiState.value = _uiState.value.copy(isAvatarVisible = true)
                    }
                    ExecutionState.IDLE -> {
                        // Keep visible if user manually expanded or return to minimized
                    }
                }
            }
        }

        viewModelScope.launch {
            agentStateManager.pendingConfirmationIntent.collect { intent ->
                _uiState.value = _uiState.value.copy(pendingConfirmationIntent = intent)
            }
        }

        viewModelScope.launch {
            voiceRecognizer.speechState.collect { speechState ->
                when (speechState) {
                    is SpeechState.Listening -> {
                        _uiState.value = _uiState.value.copy(isListening = true, isAvatarVisible = true)
                    }
                    is SpeechState.Processing -> {
                        _uiState.value = _uiState.value.copy(isListening = true)
                    }
                    is SpeechState.Result -> {
                        _uiState.value = _uiState.value.copy(isListening = false)
                        runCommand(speechState.text, isVoice = true)
                    }
                    is SpeechState.Error -> {
                        _uiState.value = _uiState.value.copy(isListening = false)
                    }
                    SpeechState.Idle -> {
                        _uiState.value = _uiState.value.copy(isListening = false)
                    }
                }
            }
        }
    }

    fun selectCategory(category: AgentSubCategory) {
        _uiState.value = _uiState.value.copy(selectedSubCategory = category)
    }

    fun startListening() {
        if (!permissionManager.isGranted(AppPermission.MICROPHONE)) {
            _uiState.value = _uiState.value.copy(
                requiredPermissionToPrompt = AppPermission.MICROPHONE,
                isAvatarVisible = true
            )
            return
        }
        _uiState.value = _uiState.value.copy(isAvatarVisible = true, isListening = true)
        voiceRecognizer.startListening()
    }

    fun stopListening() {
        voiceRecognizer.stopListening()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun runCommand(commandText: String, isVoice: Boolean = false) {
        _uiState.value = _uiState.value.copy(isAvatarVisible = true)
        agentStateManager.processCommand(commandText, isVoice)
    }

    fun confirmPendingAction() {
        _uiState.value = _uiState.value.copy(pendingConfirmationIntent = null)
        agentStateManager.confirmPendingAction()
    }

    fun cancelPendingAction() {
        _uiState.value = _uiState.value.copy(pendingConfirmationIntent = null)
        agentStateManager.cancelPendingAction()
    }

    fun dismissAvatar() {
        _uiState.value = _uiState.value.copy(isAvatarVisible = false)
        agentStateManager.resetToIdle()
    }

    fun dismissPermissionPrompt() {
        _uiState.value = _uiState.value.copy(requiredPermissionToPrompt = null)
    }

    fun startStudyTimer(minutes: Int) {
        timerJob?.cancel()
        val totalSeconds = minutes * 60
        _uiState.value = _uiState.value.copy(
            activeTimerSecondsRemaining = totalSeconds,
            isTimerActive = true
        )

        // Also trigger system timer
        runCommand("start a $minutes minute study timer")

        timerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(activeTimerSecondsRemaining = remaining)
            }
            _uiState.value = _uiState.value.copy(isTimerActive = false)
        }
    }

    fun stopStudyTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isTimerActive = false, activeTimerSecondsRemaining = 0)
    }

    fun saveQuickNote(title: String, content: String, tag: String = "GENERAL") {
        viewModelScope.launch {
            studyRepo.saveNote(title, content, tag)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            studyRepo.deleteNote(id)
        }
    }

    fun addRoutine(title: String, phrase: String, actionType: String, params: String) {
        viewModelScope.launch {
            studyRepo.saveRoutine(title, phrase, actionType, params)
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch {
            studyRepo.deleteRoutine(id)
        }
    }

    fun toggleRoutine(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            studyRepo.setRoutineEnabled(id, enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
