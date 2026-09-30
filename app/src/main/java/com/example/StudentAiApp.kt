package com.example

import android.app.Application
import com.example.agent.ActionExecutor
import com.example.agent.AgentStateManager
import com.example.agent.AndroidActionExecutor
import com.example.agent.CommandRouter
import com.example.agent.DefaultAgentStateManager
import com.example.agent.LocalFirstCommandRouter
import com.example.agent.PrivacyFirewall
import com.example.agent.StrictPrivacyFirewall
import com.example.ai.AiProvider
import com.example.ai.GeminiAiProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.StudyRepository
import com.example.permissions.AndroidPermissionManager
import com.example.permissions.PermissionManager
import com.example.voice.AndroidVoiceRecognizer
import com.example.voice.AndroidVoiceSynthesizer
import com.example.voice.VoiceRecognizer
import com.example.voice.VoiceSynthesizer

class StudentAiApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var studyRepository: StudyRepository
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var privacyFirewall: PrivacyFirewall
        private set
    lateinit var aiProvider: AiProvider
        private set
    lateinit var voiceRecognizer: VoiceRecognizer
        private set
    lateinit var voiceSynthesizer: VoiceSynthesizer
        private set
    lateinit var commandRouter: CommandRouter
        private set
    lateinit var actionExecutor: ActionExecutor
        private set
    lateinit var agentStateManager: AgentStateManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        chatRepository = ChatRepository(database.chatDao())
        settingsRepository = SettingsRepository(this)
        studyRepository = StudyRepository(database.noteDao(), database.routineDao())

        permissionManager = AndroidPermissionManager(this, database.permissionLogDao())
        privacyFirewall = StrictPrivacyFirewall()
        aiProvider = GeminiAiProvider()

        voiceRecognizer = AndroidVoiceRecognizer(this)
        voiceSynthesizer = AndroidVoiceSynthesizer(this)

        commandRouter = LocalFirstCommandRouter(aiProvider)
        actionExecutor = AndroidActionExecutor(this, permissionManager, aiProvider, privacyFirewall)
        agentStateManager = DefaultAgentStateManager(commandRouter, actionExecutor, voiceSynthesizer)
    }
}
