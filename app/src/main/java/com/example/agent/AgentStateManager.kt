package com.example.agent

import com.example.domain.model.CommandIntent
import com.example.domain.model.ExecutionResult
import com.example.domain.model.ExecutionState
import com.example.domain.model.ExecutionStatusCode
import com.example.voice.VoiceSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface AgentStateManager {
    val state: StateFlow<ExecutionState>
    val currentQuery: StateFlow<String>
    val lastResult: StateFlow<ExecutionResult?>
    val pendingConfirmationIntent: StateFlow<CommandIntent?>

    fun processCommand(commandText: String, isVoice: Boolean = false)
    fun confirmPendingAction()
    fun cancelPendingAction()
    fun resetToIdle()
}

class DefaultAgentStateManager(
    private val commandRouter: CommandRouter,
    private val actionExecutor: ActionExecutor,
    private val voiceSynthesizer: VoiceSynthesizer,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : AgentStateManager {

    private val _state = MutableStateFlow(ExecutionState.IDLE)
    override val state: StateFlow<ExecutionState> = _state.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    override val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    private val _lastResult = MutableStateFlow<ExecutionResult?>(null)
    override val lastResult: StateFlow<ExecutionResult?> = _lastResult.asStateFlow()

    private val _pendingConfirmationIntent = MutableStateFlow<CommandIntent?>(null)
    override val pendingConfirmationIntent: StateFlow<CommandIntent?> = _pendingConfirmationIntent.asStateFlow()

    private var currentJob: Job? = null

    override fun processCommand(commandText: String, isVoice: Boolean) {
        currentJob?.cancel()
        currentJob = scope.launch {
            _currentQuery.value = commandText
            _state.value = ExecutionState.THINKING
            _lastResult.value = null

            // 1. Route command (Local-first or Gemini)
            val intent = commandRouter.route(commandText)

            // 2. Check if confirmation is required
            if (intent.requiresConfirmation) {
                _pendingConfirmationIntent.value = intent
                _state.value = ExecutionState.CONFIRMATION_REQUIRED
                return@launch
            }

            // 3. Execute
            _state.value = ExecutionState.EXECUTING
            val result = actionExecutor.execute(intent, isConfirmedByUser = true)
            _lastResult.value = result

            when (result.statusCode) {
                ExecutionStatusCode.SUCCESS -> {
                    _state.value = ExecutionState.SUCCESS
                    if (isVoice && result.voiceSpeakableText.isNotBlank()) {
                        voiceSynthesizer.speak(result.voiceSpeakableText)
                    }
                    // Auto-hide avatar after success delay
                    delay(4000)
                    if (_state.value == ExecutionState.SUCCESS) {
                        _state.value = ExecutionState.IDLE
                    }
                }
                ExecutionStatusCode.PERMISSION_REQUIRED -> {
                    _state.value = ExecutionState.PERMISSION_REQUIRED
                }
                else -> {
                    _state.value = ExecutionState.ERROR
                    if (isVoice && result.voiceSpeakableText.isNotBlank()) {
                        voiceSynthesizer.speak(result.voiceSpeakableText)
                    }
                    delay(4500)
                    if (_state.value == ExecutionState.ERROR) {
                        _state.value = ExecutionState.IDLE
                    }
                }
            }
        }
    }

    override fun confirmPendingAction() {
        val intent = _pendingConfirmationIntent.value ?: return
        _pendingConfirmationIntent.value = null
        currentJob = scope.launch {
            _state.value = ExecutionState.EXECUTING
            val result = actionExecutor.execute(intent, isConfirmedByUser = true)
            _lastResult.value = result

            if (result.statusCode == ExecutionStatusCode.SUCCESS) {
                _state.value = ExecutionState.SUCCESS
                voiceSynthesizer.speak(result.voiceSpeakableText)
                delay(3500)
                if (_state.value == ExecutionState.SUCCESS) {
                    _state.value = ExecutionState.IDLE
                }
            } else {
                _state.value = ExecutionState.ERROR
                delay(3500)
                if (_state.value == ExecutionState.ERROR) {
                    _state.value = ExecutionState.IDLE
                }
            }
        }
    }

    override fun cancelPendingAction() {
        _pendingConfirmationIntent.value = null
        _lastResult.value = ExecutionResult(
            statusCode = ExecutionStatusCode.USER_CANCELLED,
            message = "Action cancelled by user."
        )
        _state.value = ExecutionState.IDLE
    }

    override fun resetToIdle() {
        currentJob?.cancel()
        voiceSynthesizer.stop()
        _state.value = ExecutionState.IDLE
        _pendingConfirmationIntent.value = null
    }
}
