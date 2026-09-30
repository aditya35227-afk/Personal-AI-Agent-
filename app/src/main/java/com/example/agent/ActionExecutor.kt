package com.example.agent

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import com.example.ai.AiProvider
import com.example.ai.AiResponse
import com.example.domain.model.ActionType
import com.example.domain.model.CommandIntent
import com.example.domain.model.ExecutionResult
import com.example.domain.model.ExecutionStatusCode
import com.example.permissions.AppPermission
import com.example.permissions.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ActionExecutor {
    suspend fun execute(intent: CommandIntent, isConfirmedByUser: Boolean): ExecutionResult
}

class AndroidActionExecutor(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val aiProvider: AiProvider,
    private val privacyFirewall: PrivacyFirewall
) : ActionExecutor {

    private var isTorchOn = false

    override suspend fun execute(intent: CommandIntent, isConfirmedByUser: Boolean): ExecutionResult = withContext(Dispatchers.IO) {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isDeviceLocked == true

        when (intent.actionType) {
            ActionType.TORCH_ON -> setTorch(true)
            ActionType.TORCH_OFF -> setTorch(false)

            ActionType.SET_ALARM -> {
                val hour = intent.parameters["hour"]?.toIntOrNull() ?: 7
                val minute = intent.parameters["minute"]?.toIntOrNull() ?: 0
                val label = intent.parameters["label"] ?: "Student AI Alarm"
                createAlarm(hour, minute, label)
            }

            ActionType.START_TIMER, ActionType.STUDY_TIMER -> {
                val seconds = intent.parameters["seconds"]?.toIntOrNull() ?: (25 * 60)
                val minutes = seconds / 60
                val label = intent.parameters["subject"] ?: "Study Focus"
                startTimer(seconds, label, minutes)
            }

            ActionType.OPEN_APP -> {
                val appName = intent.parameters["appName"] ?: ""
                openAppByName(appName)
            }

            ActionType.OPEN_SETTINGS -> {
                openDeviceSettings()
            }

            ActionType.SET_VOLUME -> {
                val direction = intent.parameters["direction"] ?: "UP"
                adjustVolume(direction == "UP")
            }

            ActionType.CALL_CONTACT -> {
                if (isLocked) {
                    return@withContext ExecutionResult(
                        statusCode = ExecutionStatusCode.FAILED,
                        message = "Phone unlock करने के बाद मैं कॉल कर सकता हूँ.",
                        voiceSpeakableText = "Please unlock your phone to make a call."
                    )
                }

                if (!permissionManager.isGranted(AppPermission.PHONE)) {
                    return@withContext ExecutionResult(
                        statusCode = ExecutionStatusCode.PERMISSION_REQUIRED,
                        message = "Calling requires phone permission.",
                        voiceSpeakableText = "Please grant phone permission to place calls.",
                        intent = intent
                    )
                }

                if (!isConfirmedByUser) {
                    return@withContext ExecutionResult(
                        statusCode = ExecutionStatusCode.USER_CANCELLED,
                        message = "Confirmation required before placing call to ${intent.parameters["contactName"]}.",
                        intent = intent
                    )
                }

                val contactName = intent.parameters["contactName"] ?: "Contact"
                initiateCall(contactName)
            }

            ActionType.SEND_SMS -> {
                if (isLocked) {
                    return@withContext ExecutionResult(
                        statusCode = ExecutionStatusCode.FAILED,
                        message = "Phone unlock करने के बाद मैं संदेश भेज सकता हूँ.",
                        voiceSpeakableText = "Please unlock your phone to send messages."
                    )
                }

                if (!permissionManager.isGranted(AppPermission.SMS)) {
                    return@withContext ExecutionResult(
                        statusCode = ExecutionStatusCode.PERMISSION_REQUIRED,
                        message = "Sending SMS requires SMS permission.",
                        voiceSpeakableText = "Please grant SMS permission.",
                        intent = intent
                    )
                }

                if (!isConfirmedByUser) {
                    return@withContext ExecutionResult(
                        statusCode = ExecutionStatusCode.USER_CANCELLED,
                        message = "Confirmation required before sending message.",
                        intent = intent
                    )
                }

                val contact = intent.parameters["contactName"] ?: ""
                val msg = intent.parameters["message"] ?: ""
                sendSms(contact, msg)
            }

            ActionType.EXPLAIN_CONCEPT, ActionType.HOMEWORK_HELPER -> {
                val prompt = intent.parameters["topic"] ?: intent.rawQuery
                val filteredPrompt = privacyFirewall.filterOutgoingPrompt(prompt)
                val aiResponse = aiProvider.generateResponse(filteredPrompt)
                when (aiResponse) {
                    is AiResponse.Success -> ExecutionResult(
                        statusCode = ExecutionStatusCode.SUCCESS,
                        message = aiResponse.text,
                        voiceSpeakableText = aiResponse.text.take(200)
                    )
                    is AiResponse.Error -> ExecutionResult(
                        statusCode = if (aiResponse.isQuotaLimit) ExecutionStatusCode.AI_LIMIT_REACHED else ExecutionStatusCode.NETWORK_ERROR,
                        message = aiResponse.message,
                        voiceSpeakableText = aiResponse.message
                    )
                    else -> ExecutionResult(ExecutionStatusCode.FAILED, "No response")
                }
            }

            ActionType.CREATE_QUIZ -> {
                val topic = intent.parameters["topic"] ?: intent.rawQuery
                val quizPrompt = "Create a 3-question multiple choice revision quiz for a student about: $topic. Include options A, B, C, D and explain correct answers."
                val aiResponse = aiProvider.generateResponse(quizPrompt)
                when (aiResponse) {
                    is AiResponse.Success -> ExecutionResult(ExecutionStatusCode.SUCCESS, aiResponse.text)
                    is AiResponse.Error -> ExecutionResult(ExecutionStatusCode.NETWORK_ERROR, aiResponse.message)
                    else -> ExecutionResult(ExecutionStatusCode.FAILED, "Could not create quiz")
                }
            }

            ActionType.SOCIAL_CAPTION -> {
                val topic = intent.parameters["topic"] ?: intent.rawQuery
                val captionPrompt = "Create 2 engaging, student-friendly social media captions with relevant hashtags for: $topic."
                val aiResponse = aiProvider.generateResponse(captionPrompt)
                when (aiResponse) {
                    is AiResponse.Success -> ExecutionResult(ExecutionStatusCode.SUCCESS, aiResponse.text)
                    is AiResponse.Error -> ExecutionResult(ExecutionStatusCode.NETWORK_ERROR, aiResponse.message)
                    else -> ExecutionResult(ExecutionStatusCode.FAILED, "Could not generate caption")
                }
            }

            else -> {
                ExecutionResult(
                    statusCode = ExecutionStatusCode.UNSUPPORTED,
                    message = "यह काम अभी मेरे supported actions में नहीं है.",
                    voiceSpeakableText = "Yeh kaam abhi mere supported actions mein nahi hai."
                )
            }
        }
    }

    private fun setTorch(enabled: Boolean): ExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                isTorchOn = enabled
                ExecutionResult(
                    statusCode = ExecutionStatusCode.SUCCESS,
                    message = if (enabled) "Torch turned ON 💡" else "Torch turned OFF 🔦",
                    voiceSpeakableText = if (enabled) "Flashlight turned on" else "Flashlight turned off"
                )
            } else {
                ExecutionResult(
                    statusCode = ExecutionStatusCode.UNSUPPORTED,
                    message = "Torch hardware not available on this device",
                    voiceSpeakableText = "Torch is not available"
                )
            }
        } catch (e: Exception) {
            Log.e("ActionExecutor", "Torch error", e)
            ExecutionResult(
                statusCode = ExecutionStatusCode.FAILED,
                message = "Could not toggle torch: ${e.message}",
                voiceSpeakableText = "Could not control flashlight"
            )
        }
    }

    private fun createAlarm(hour: Int, minute: Int, label: String): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val timeString = "%02d:%02d".format(hour, minute)
            ExecutionResult(
                statusCode = ExecutionStatusCode.SUCCESS,
                message = "Alarm set for $timeString ⏰",
                voiceSpeakableText = "Alarm set for $timeString"
            )
        } catch (e: Exception) {
            Log.e("ActionExecutor", "Alarm error", e)
            ExecutionResult(
                statusCode = ExecutionStatusCode.FAILED,
                message = "Alarm app not available on device",
                voiceSpeakableText = "Could not set alarm"
            )
        }
    }

    private fun startTimer(seconds: Int, label: String, minutes: Int): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ExecutionResult(
                statusCode = ExecutionStatusCode.SUCCESS,
                message = "Timer started for $minutes minutes ⏱️",
                voiceSpeakableText = "Timer started for $minutes minutes"
            )
        } catch (e: Exception) {
            ExecutionResult(
                statusCode = ExecutionStatusCode.FAILED,
                message = "Timer application not available on device",
                voiceSpeakableText = "Could not start timer"
            )
        }
    }

    private fun openAppByName(name: String): ExecutionResult {
        val pm = context.packageManager
        val query = name.lowercase().trim()

        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "camera" to "com.google.android.GoogleCamera",
            "calculator" to "com.google.android.calculator",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "clock" to "com.google.android.deskclock",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm"
        )

        var targetPackage = knownPackages[query]
        if (targetPackage == null) {
            val installedApps = pm.getInstalledApplications(0)
            val match = installedApps.firstOrNull { app ->
                val label = pm.getApplicationLabel(app).toString().lowercase()
                label.contains(query)
            }
            targetPackage = match?.packageName
        }

        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ExecutionResult(
                    statusCode = ExecutionStatusCode.SUCCESS,
                    message = "Opening $name 🚀",
                    voiceSpeakableText = "Opening $name"
                )
            }
        }

        return ExecutionResult(
            statusCode = ExecutionStatusCode.FAILED,
            message = "App '$name' not found on device",
            voiceSpeakableText = "Application $name was not found"
        )
    }

    private fun openDeviceSettings(): ExecutionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ExecutionResult(
                statusCode = ExecutionStatusCode.SUCCESS,
                message = "Opening Android Settings ⚙️",
                voiceSpeakableText = "Opening settings"
            )
        } catch (_: Exception) {
            ExecutionResult(ExecutionStatusCode.FAILED, "Cannot open settings")
        }
    }

    private fun adjustVolume(increase: Boolean): ExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val dir = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
                ExecutionResult(
                    statusCode = ExecutionStatusCode.SUCCESS,
                    message = if (increase) "Volume increased 🔊" else "Volume decreased 🔉",
                    voiceSpeakableText = if (increase) "Volume increased" else "Volume decreased"
                )
            } else {
                ExecutionResult(ExecutionStatusCode.FAILED, "Audio manager not available")
            }
        } catch (e: Exception) {
            ExecutionResult(ExecutionStatusCode.FAILED, "Could not adjust volume: ${e.message}")
        }
    }

    private fun initiateCall(target: String): ExecutionResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${target.filter { it.isDigit() || it == '+' }}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            ExecutionResult(
                statusCode = ExecutionStatusCode.SUCCESS,
                message = "Initiating call to $target 📞",
                voiceSpeakableText = "Calling $target"
            )
        } catch (e: Exception) {
            ExecutionResult(ExecutionStatusCode.FAILED, "Could not place call: ${e.message}")
        }
    }

    private fun sendSms(contact: String, messageText: String): ExecutionResult {
        return try {
            val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${contact.filter { it.isDigit() || it == '+' }}")
                putExtra("sms_body", messageText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(sendIntent)
            ExecutionResult(
                statusCode = ExecutionStatusCode.SUCCESS,
                message = "Prepared SMS for $contact ✉️",
                voiceSpeakableText = "SMS prepared"
            )
        } catch (e: Exception) {
            ExecutionResult(ExecutionStatusCode.FAILED, "Could not send SMS: ${e.message}")
        }
    }
}
