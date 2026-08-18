package com.quranplus.app.features.chatbot.domain

import com.quranplus.app.features.settings.data.AiPersona
import kotlinx.coroutines.flow.Flow

class GetChatHistoryUseCase(private val repository: ChatRepository) {
    operator fun invoke(conversationId: String): Flow<List<ChatMessage>> =
        repository.getChatHistory(conversationId)
}

class SaveChatMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(message: ChatMessage): Long = repository.saveMessage(message)
}

class ClearChatHistoryUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(conversationId: String) = repository.clearHistory(conversationId)
}

class GenerateRagAnswerUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(
        userQuery: String,
        persona: AiPersona,
        customPrompt: String? = null
    ): RagGenerationResult = repository.generateRagResponse(userQuery, persona, customPrompt)
}
