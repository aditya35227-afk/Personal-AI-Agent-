package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val messages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    suspend fun saveMessage(
        role: String,
        content: String,
        isVoicePrompt: Boolean = false,
        attachedImageUri: String? = null
    ): Long {
        val entity = ChatMessageEntity(
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            isVoicePrompt = isVoicePrompt,
            attachedImageUri = attachedImageUri
        )
        return chatDao.insertMessage(entity)
    }

    suspend fun clearHistory() {
        chatDao.clearAll()
    }

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteById(id)
    }
}
