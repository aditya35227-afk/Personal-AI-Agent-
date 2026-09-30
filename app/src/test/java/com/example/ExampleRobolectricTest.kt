package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.LocalFirstCommandRouter
import com.example.agent.StrictPrivacyFirewall
import com.example.ai.AiMessage
import com.example.ai.AiProvider
import com.example.ai.AiResponse
import com.example.domain.model.ActionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private val fakeAiProvider = object : AiProvider {
        override suspend fun generateResponse(
            prompt: String,
            history: List<AiMessage>,
            imageBase64: String?,
            systemInstruction: String?
        ): AiResponse = AiResponse.Success("Sample AI Response")

        override fun streamResponse(
            prompt: String,
            history: List<AiMessage>,
            systemInstruction: String?
        ): Flow<String> = flowOf("Sample chunk")

        override suspend fun parseCommandIntent(userCommand: String): String? = null
    }

    private val router = LocalFirstCommandRouter(fakeAiProvider)
    private val firewall = StrictPrivacyFirewall()

    @Test
    fun verifyAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Student AI Agent", appName)
    }

    @Test
    fun testTorchCommand() = runBlocking {
        val intent = router.route("torch on")
        assertEquals(ActionType.TORCH_ON, intent.actionType)
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun testAlarmCommand() = runBlocking {
        val intent = router.route("set alarm for 7 AM")
        assertEquals(ActionType.SET_ALARM, intent.actionType)
        assertEquals("7", intent.parameters["hour"])
        assertEquals("0", intent.parameters["minute"])
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun testOpenAppCommand() = runBlocking {
        val intent = router.route("open YouTube")
        assertEquals(ActionType.OPEN_APP, intent.actionType)
        assertEquals("youtube", intent.parameters["appName"]?.lowercase())
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun testStudyTimerCommand() = runBlocking {
        val intent = router.route("start a 40 minute study timer")
        assertEquals(ActionType.STUDY_TIMER, intent.actionType)
        assertEquals("40", intent.parameters["minutes"])
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun testPhotosynthesisAcademicQuery() = runBlocking {
        val intent = router.route("explain photosynthesis")
        assertEquals(ActionType.EXPLAIN_CONCEPT, intent.actionType)
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun testInstagramCaption() = runBlocking {
        val intent = router.route("create an Instagram caption")
        assertEquals(ActionType.SOCIAL_CAPTION, intent.actionType)
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun testCallRequiresConfirmationAndPermission() = runBlocking {
        val intent = router.route("call Rahul")
        assertEquals(ActionType.CALL_CONTACT, intent.actionType)
        assertTrue(intent.requiresConfirmation)
        assertEquals("android.permission.CALL_PHONE", intent.requiredPermission)
        assertEquals("Rahul", intent.parameters["contactName"])
    }

    @Test
    fun testSmsRequiresConfirmationAndPermission() = runBlocking {
        val intent = router.route("send Rahul a message saying I will be late")
        assertEquals(ActionType.SEND_SMS, intent.actionType)
        assertTrue(intent.requiresConfirmation)
        assertEquals("android.permission.SEND_SMS", intent.requiredPermission)
        assertEquals("Rahul", intent.parameters["contactName"])
        assertEquals("I will be late", intent.parameters["message"])
    }

    @Test
    fun testPrivacyFirewallRedactsConfidentialData() {
        val sensitive = "My verification code is OTP: 849302"
        val filtered = firewall.filterOutgoingPrompt(sensitive)
        assertFalse(filtered.contains("849302"))
        assertTrue(filtered.contains("[CONFIDENTIAL_CREDENTIAL_REDACTED]"))
    }
}
