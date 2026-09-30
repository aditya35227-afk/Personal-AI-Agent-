package com.example.domain.model

enum class LanguageMode(val displayName: String, val speechLocale: String) {
    HINGLISH("Hinglish (हिन्दी + Eng)", "hi_IN"),
    HINDI("हिन्दी (Hindi)", "hi_IN"),
    ENGLISH("English (India)", "en_IN")
}

enum class ConfirmationLevel(val displayName: String, val description: String) {
    LOW("Low", "Confirm only critical financial/deletion actions"),
    NORMAL("Normal", "Confirm calls, SMS, deletions, and major changes (Recommended)"),
    STRICT("Strict", "Confirm all external actions before execution")
}

data class AppSettings(
    val assistantName: String = "Astra",
    val userDisplayName: String = "Student",
    val languageMode: LanguageMode = LanguageMode.HINGLISH,
    val voiceResponseEnabled: Boolean = true,
    val voiceSpeed: Float = 1.0f,
    val robotAnimationEnabled: Boolean = true,
    val autoListenAfterResponse: Boolean = false,
    val confirmationLevel: ConfirmationLevel = ConfirmationLevel.NORMAL,
    val privacyMode: Boolean = true,
    val conversationMemoryEnabled: Boolean = true,
    val voiceActivationEnabled: Boolean = false,
    val wakePhrase: String = "Hey Astra"
)

enum class FeatureId {
    STUDY_AGENT,
    PHONE_AGENT,
    SOCIAL_ASSISTANT,
    AUTOMATION_ROUTINES,
    MULTIMODAL_STUDY
}

data class FeatureCapability(
    val id: FeatureId,
    val title: String,
    val description: String,
    val isFree: Boolean = true,
    val isEnabled: Boolean = true
) {
    companion object {
        fun getAllCapabilities(): List<FeatureCapability> = listOf(
            FeatureCapability(
                id = FeatureId.STUDY_AGENT,
                title = "Study Agent",
                description = "Study & break timers, concept explainer, homework assistance, quiz generator",
                isFree = true,
                isEnabled = true
            ),
            FeatureCapability(
                id = FeatureId.PHONE_AGENT,
                title = "Phone Agent",
                description = "Open apps, alarms, timers, torch, media, volume, settings controls",
                isFree = true,
                isEnabled = true
            ),
            FeatureCapability(
                id = FeatureId.SOCIAL_ASSISTANT,
                title = "Social Media Assistant",
                description = "Student project captions, hashtag generators, content schedules",
                isFree = true,
                isEnabled = true
            ),
            FeatureCapability(
                id = FeatureId.AUTOMATION_ROUTINES,
                title = "Automations & Routines",
                description = "Custom voice shortcuts and recurring student routines",
                isFree = true,
                isEnabled = true
            ),
            FeatureCapability(
                id = FeatureId.MULTIMODAL_STUDY,
                title = "Photo / Document Analysis",
                description = "Explain uploaded diagrams, math formulas, and handwritten notes securely",
                isFree = true,
                isEnabled = true
            )
        )
    }
}
