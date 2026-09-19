package com.quranplus.app.features.chatbot.domain

import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.GenerationStatus
import com.quranplus.app.features.settings.data.AiPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface GenerationEvent {
    data class Append(val text: String) : GenerationEvent
    data class Replace(val text: String) : GenerationEvent
}

data class RagGenerationResult(
    val tokenStream: Flow<GenerationEvent>,
    val citations: List<RetrievedCitation>,
    val generationStatus: StateFlow<GenerationStatus>? = null
)

data class ChatSession(
    val conversationId: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Long
)

interface ChatRepository {
    fun getChatHistory(conversationId: String): Flow<List<ChatMessage>>
    fun getChatSessions(): Flow<List<ChatSession>>
    suspend fun saveMessage(message: ChatMessage): Long
    suspend fun clearHistory(conversationId: String)
    suspend fun clearAllHistory()
    suspend fun generateRagResponse(
        conversationId: String,
        userQuery: String,
        persona: AiPersona,
        customPrompt: String?
    ): RagGenerationResult
}
