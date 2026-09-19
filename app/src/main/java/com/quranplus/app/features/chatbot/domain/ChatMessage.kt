package com.quranplus.app.features.chatbot.domain

import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.GenerationStatus

data class ChatMessage(
    val id: Long = 0,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val citations: List<RetrievedCitation> = emptyList(),
    val isStreaming: Boolean = false,
    val generationStatus: GenerationStatus = GenerationStatus.COMPLETE,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
