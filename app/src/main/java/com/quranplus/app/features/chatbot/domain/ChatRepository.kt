package com.quranplus.app.features.chatbot.domain

import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.settings.data.AiPersona
import kotlinx.coroutines.flow.Flow

data class RagGenerationResult(
    val tokenStream: Flow<String>,
    val citations: List<RetrievedCitation>
)

interface ChatRepository {
    fun getChatHistory(conversationId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage): Long
    suspend fun clearHistory(conversationId: String)
    suspend fun generateRagResponse(userQuery: String, persona: AiPersona, customPrompt: String?): RagGenerationResult
}
