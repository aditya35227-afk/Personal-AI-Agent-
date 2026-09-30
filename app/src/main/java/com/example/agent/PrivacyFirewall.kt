package com.example.agent

enum class DataClassification {
    PUBLIC_COMMAND,
    USER_ATTACHMENT,
    SENSITIVE_SYSTEM_DATA,
    HIGHLY_SENSITIVE_ACTION
}

interface PrivacyFirewall {
    fun filterOutgoingPrompt(rawPrompt: String): String
    fun validateActionExecution(actionName: String, hasExplicitConsent: Boolean): Boolean
    fun sanitizeLog(details: String): String
}

class StrictPrivacyFirewall : PrivacyFirewall {

    // Regex patterns for sensitive data that should never inadvertently leak in raw prompts
    private val phoneRegex = Regex("\\b\\+?[0-9]{10,13}\\b")
    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val otpRegex = Regex("(?i)\\b(otp|password|pin)\\s*[:=]?\\s*\\d{4,8}\\b")

    override fun filterOutgoingPrompt(rawPrompt: String): String {
        // Redact potential OTP or password occurrences before sending to AI
        var sanitized = rawPrompt
        if (otpRegex.containsMatchIn(sanitized)) {
            sanitized = otpRegex.replace(sanitized, "[CONFIDENTIAL_CREDENTIAL_REDACTED]")
        }
        return sanitized.trim()
    }

    override fun validateActionExecution(actionName: String, hasExplicitConsent: Boolean): Boolean {
        return when (actionName) {
            "SEND_SMS", "CALL_CONTACT", "DELETE_DATA" -> hasExplicitConsent
            else -> true
        }
    }

    override fun sanitizeLog(details: String): String {
        return phoneRegex.replace(details, "XXXXXX****")
    }
}
