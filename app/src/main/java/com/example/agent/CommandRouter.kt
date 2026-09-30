package com.example.agent

import com.example.ai.AiProvider
import com.example.domain.model.ActionType
import com.example.domain.model.CommandIntent
import org.json.JSONObject
import java.util.regex.Pattern

interface CommandRouter {
    suspend fun route(input: String): CommandIntent
}

class LocalFirstCommandRouter(
    private val aiProvider: AiProvider
) : CommandRouter {

    override suspend fun route(input: String): CommandIntent {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // 1. Torch / Flashlight (Local check)
        if (lower.contains("torch on") || lower.contains("flashlight on") ||
            lower.contains("torch chalu") || lower.contains("torch jalao") ||
            lower.contains("टॉर्च ऑन") || lower.contains("टॉर्च जलाओ")
        ) {
            return CommandIntent(
                actionType = ActionType.TORCH_ON,
                rawQuery = trimmed,
                explanationText = "Turning flashlight ON"
            )
        }
        if (lower.contains("torch off") || lower.contains("flashlight off") ||
            lower.contains("torch band") || lower.contains("टॉर्च बंद")
        ) {
            return CommandIntent(
                actionType = ActionType.TORCH_OFF,
                rawQuery = trimmed,
                explanationText = "Turning flashlight OFF"
            )
        }

        // 2. Alarm (Local check)
        // e.g. "set alarm for 7 AM", "alarm for 6:30", "alarm lagao 7 baje"
        val alarmMatcher = Pattern.compile("(?i)(?:set\\s+)?alarm\\s+(?:for\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").matcher(lower)
        if (alarmMatcher.find()) {
            var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
            val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = alarmMatcher.group(3)?.lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            return CommandIntent(
                actionType = ActionType.SET_ALARM,
                parameters = mapOf(
                    "hour" to hour.toString(),
                    "minute" to minute.toString(),
                    "label" to "Student AI Alarm"
                ),
                rawQuery = trimmed,
                explanationText = "Setting alarm for %02d:%02d".format(hour, minute)
            )
        }

        // 3. Timer (Local check)
        // e.g. "timer for 10 minutes", "start a 40 minute study timer", "5 minute timer"
        val timerMatcher = Pattern.compile("(?i)(\\d+)\\s*(?:min|minute|minutes|m)\\s*(?:study\\s*)?timer").matcher(lower)
        val timerMatcherAlt = Pattern.compile("(?i)(?:start|set)\\s*(?:a\\s*)?(\\d+)\\s*(?:min|minute|minutes)\\s*timer").matcher(lower)
        val minutesMatch = when {
            timerMatcher.find() -> timerMatcher.group(1)?.toIntOrNull()
            timerMatcherAlt.find() -> timerMatcherAlt.group(1)?.toIntOrNull()
            lower.contains("study timer") -> 25 // Default Pomodoro
            else -> null
        }
        if (minutesMatch != null) {
            val seconds = minutesMatch * 60
            return CommandIntent(
                actionType = if (lower.contains("study")) ActionType.STUDY_TIMER else ActionType.START_TIMER,
                parameters = mapOf(
                    "seconds" to seconds.toString(),
                    "minutes" to minutesMatch.toString(),
                    "subject" to "Study Session"
                ),
                rawQuery = trimmed,
                explanationText = "Starting timer for $minutesMatch minutes"
            )
        }

        // 4. Open Application (Local check)
        // e.g. "open youtube", "open camera", "open calculator", "open settings"
        val openAppMatcher = Pattern.compile("(?i)^open\\s+([a-zA-Z0-9\\s]+)").matcher(lower)
        if (openAppMatcher.find()) {
            val target = openAppMatcher.group(1)?.trim() ?: ""
            if (target == "settings") {
                return CommandIntent(
                    actionType = ActionType.OPEN_SETTINGS,
                    rawQuery = trimmed,
                    explanationText = "Opening Android Settings"
                )
            }
            return CommandIntent(
                actionType = ActionType.OPEN_APP,
                parameters = mapOf("appName" to target),
                rawQuery = trimmed,
                explanationText = "Opening $target"
            )
        }

        // 5. Volume control (Local check)
        if (lower.contains("volume up") || lower.contains("awaz badhao") || lower.contains("आवाज़ बढ़ाओ")) {
            return CommandIntent(
                actionType = ActionType.SET_VOLUME,
                parameters = mapOf("direction" to "UP"),
                rawQuery = trimmed,
                explanationText = "Increasing volume"
            )
        }
        if (lower.contains("volume down") || lower.contains("awaz kam") || lower.contains("आवाज़ कम करो")) {
            return CommandIntent(
                actionType = ActionType.SET_VOLUME,
                parameters = mapOf("direction" to "DOWN"),
                rawQuery = trimmed,
                explanationText = "Decreasing volume"
            )
        }

        // 6. Sensitive Actions with explicit Confirmation requirement
        // "call Rahul", "Rahul ko call karo"
        val callMatcher = Pattern.compile("(?i)(?:call|dial)\\s+([a-zA-Z0-9\\s]+)").matcher(trimmed)
        if (callMatcher.find() && !lower.contains("what") && !lower.contains("why")) {
            val contactName = callMatcher.group(1)?.trim() ?: ""
            return CommandIntent(
                actionType = ActionType.CALL_CONTACT,
                parameters = mapOf("contactName" to contactName),
                rawQuery = trimmed,
                requiresConfirmation = true,
                requiredPermission = "android.permission.CALL_PHONE",
                explanationText = "Call $contactName?"
            )
        }

        // "send Rahul a message saying I will be late"
        val smsMatcher = Pattern.compile("(?i)(?:send|message)\\s+([a-zA-Z0-9]+)(?:\\s+a message)?\\s+saying\\s+(.+)").matcher(trimmed)
        if (smsMatcher.find()) {
            val contact = smsMatcher.group(1)?.trim() ?: ""
            val messageBody = smsMatcher.group(2)?.trim() ?: ""
            return CommandIntent(
                actionType = ActionType.SEND_SMS,
                parameters = mapOf("contactName" to contact, "message" to messageBody),
                rawQuery = trimmed,
                requiresConfirmation = true,
                requiredPermission = "android.permission.SEND_SMS",
                explanationText = "Send SMS to $contact: \"$messageBody\"?"
            )
        }

        // 7. Academic / Study intents
        if (lower.contains("explain") || lower.contains("what is") || lower.contains("samjhao") || lower.contains("photosynthesis")) {
            return CommandIntent(
                actionType = ActionType.EXPLAIN_CONCEPT,
                parameters = mapOf("topic" to trimmed),
                rawQuery = trimmed,
                explanationText = "Explaining concept"
            )
        }

        if (lower.contains("quiz") || lower.contains("test me")) {
            return CommandIntent(
                actionType = ActionType.CREATE_QUIZ,
                parameters = mapOf("topic" to trimmed),
                rawQuery = trimmed,
                explanationText = "Generating student quiz"
            )
        }

        if (lower.contains("caption") || lower.contains("hashtag") || lower.contains("instagram")) {
            return CommandIntent(
                actionType = ActionType.SOCIAL_CAPTION,
                parameters = mapOf("topic" to trimmed),
                rawQuery = trimmed,
                explanationText = "Generating creative post caption"
            )
        }

        // 8. If complex / conversational, query Gemini for structured intent or answer
        val structuredJson = aiProvider.parseCommandIntent(trimmed)
        if (!structuredJson.isNullOrBlank()) {
            try {
                val json = JSONObject(structuredJson)
                val intentName = json.optString("intent", "UNKNOWN")
                val paramsObj = json.optJSONObject("parameters")
                val params = mutableMapOf<String, String>()
                paramsObj?.keys()?.forEach { k ->
                    params[k] = paramsObj.optString(k, "")
                }
                val reqConfirm = json.optBoolean("requires_confirmation", false)
                val explanation = json.optString("explanation", "")

                val actionType = try {
                    ActionType.valueOf(intentName)
                } catch (_: Exception) {
                    ActionType.UNKNOWN
                }

                if (actionType != ActionType.UNKNOWN) {
                    return CommandIntent(
                        actionType = actionType,
                        parameters = params,
                        rawQuery = trimmed,
                        requiresConfirmation = reqConfirm,
                        explanationText = explanation
                    )
                }
            } catch (_: Exception) {}
        }

        // Fallback: general query / explanation
        return CommandIntent(
            actionType = ActionType.EXPLAIN_CONCEPT,
            parameters = mapOf("topic" to trimmed),
            rawQuery = trimmed,
            explanationText = "Answering academic query"
        )
    }
}
