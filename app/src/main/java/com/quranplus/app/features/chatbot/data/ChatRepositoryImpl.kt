package com.quranplus.app.features.chatbot.data

import com.quranplus.app.core.database.dao.ChatDao
import com.quranplus.app.core.database.entity.ChatMessageEntity
import com.quranplus.app.features.chatbot.domain.ChatMessage
import com.quranplus.app.features.chatbot.domain.ChatRepository
import com.quranplus.app.features.chatbot.domain.MessageRole
import com.quranplus.app.features.chatbot.domain.RagGenerationResult
import com.quranplus.app.features.rag.data.TfLiteEmbeddingService
import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.VectorRetriever
import com.quranplus.app.features.settings.data.AiPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val embeddingService: TfLiteEmbeddingService,
    private val vectorRetriever: VectorRetriever,
    private val ragPipeline: RagPipeline,
    private val llmRunner: LiteRtLmRunner
) : ChatRepository {

    override fun getChatHistory(conversationId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessages(conversationId).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    conversationId = entity.conversationId,
                    role = when (entity.role.lowercase()) {
                        "user" -> MessageRole.USER
                        "assistant" -> MessageRole.ASSISTANT
                        else -> MessageRole.SYSTEM
                    },
                    content = entity.content,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    override suspend fun saveMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                id = message.id,
                conversationId = message.conversationId,
                role = message.role.name.lowercase(),
                content = message.content,
                timestamp = message.timestamp
            )
        )
    }

    override suspend fun clearHistory(conversationId: String) {
        chatDao.clearConversation(conversationId)
    }

    override suspend fun generateRagResponse(
        userQuery: String,
        persona: AiPersona,
        customPrompt: String?
    ): RagGenerationResult {
        // 1. Generate query embedding
        val queryVector = embeddingService.embed(userQuery)

        // 2. Retrieve Top-5 citations from Quran & Hadith
        val citations = vectorRetriever.retrieveTopK(userQuery, queryVector, k = 5)

        // 3. Build Augmented Prompt with Ground Truth Dalil
        val augmentedPrompt = ragPipeline.buildAugmentedPrompt(
            question = userQuery,
            persona = persona,
            customPrompt = customPrompt,
            citations = citations
        )

        // 4. Generate stream with LiteRT-LM
        val stream = llmRunner.generate(augmentedPrompt)

        return RagGenerationResult(
            tokenStream = stream,
            citations = citations
        )
    }
}
