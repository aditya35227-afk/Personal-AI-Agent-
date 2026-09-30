package com.example.domain.model

enum class ActionType {
    OPEN_APP,
    TORCH_ON,
    TORCH_OFF,
    SET_ALARM,
    START_TIMER,
    SET_VOLUME,
    MEDIA_CONTROL,
    OPEN_SETTINGS,
    APP_NAVIGATE,
    CALL_CONTACT,
    SEND_SMS,
    STUDY_TIMER,
    EXPLAIN_CONCEPT,
    CREATE_QUIZ,
    HOMEWORK_HELPER,
    SOCIAL_CAPTION,
    AUTOMATION_ROUTINE,
    UNKNOWN
}

data class CommandIntent(
    val actionType: ActionType,
    val parameters: Map<String, String> = emptyMap(),
    val rawQuery: String,
    val requiresConfirmation: Boolean = false,
    val requiredPermission: String? = null,
    val explanationText: String = ""
)

enum class ExecutionState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SUCCESS,
    ERROR,
    PERMISSION_REQUIRED,
    CONFIRMATION_REQUIRED
}

enum class ExecutionStatusCode {
    SUCCESS,
    FAILED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    NETWORK_ERROR,
    AI_LIMIT_REACHED,
    USER_CANCELLED
}

data class ExecutionResult(
    val statusCode: ExecutionStatusCode,
    val message: String,
    val voiceSpeakableText: String = message,
    val intent: CommandIntent? = null,
    val payload: Any? = null
)
