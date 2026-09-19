package com.quranplus.app.features.chatbot.data

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.quranplus.app.features.chatbot.domain.ChatMessage
import com.quranplus.app.features.chatbot.domain.MessageRole
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LlmRunner {
    fun generate(
        conversationId: String,
        prompt: String,
        history: List<ChatMessage> = emptyList()
    ): Flow<String>

    suspend fun clearConversation(conversationId: String)

    suspend fun clearAllConversations()

    fun isAvailable(): Boolean
}

class LiteRtLmRunner(
    private val context: Context,
    private val modelRepository: ModelRepository,
    private val preferencesManager: PreferencesManager
) : LlmRunner {

    private val inferenceMutex = Mutex()
    // LiteRT-LM owns the native KV cache inside each Conversation instance.
    private val conversations = LinkedHashMap<String, Conversation>(
        MAX_CACHED_CONVERSATIONS,
        LOAD_FACTOR,
        true
    )

    private var engine: Engine? = null
    private var loadedModelPath: String? = null

    private val samplerConfig = SamplerConfig(
        topK = 40,
        topP = 0.85,
        temperature = 0.35,
        seed = 0
    )

    private val conversationConfig: ConversationConfig
        get() = ConversationConfig(samplerConfig = samplerConfig)

    private fun getOrInitEngine(preferredModelId: String?): Engine {
        val modelFile = modelRepository.getActiveModelFile(preferredModelId)
        if (!modelFile.exists()) {
            throw IllegalStateException("Model file not found at ${modelFile.absolutePath}")
        }

        if (engine != null && loadedModelPath == modelFile.absolutePath) {
            return engine!!
        }

        closeConversations()
        runCatching { engine?.close() }
            .onFailure { Log.w(TAG, "Error closing previous engine", it) }
        engine = null
        loadedModelPath = null

        Log.i(TAG, "Initializing LiteRT-LM Engine with model: ${modelFile.absolutePath}")
        val newEngine = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                cacheDir = context.cacheDir.absolutePath
            )
        )
        try {
            newEngine.initialize()
        } catch (throwable: Throwable) {
            runCatching { newEngine.close() }
            throw throwable
        }

        engine = newEngine
        loadedModelPath = modelFile.absolutePath
        return newEngine
    }

    override fun isAvailable(): Boolean = modelRepository.isAnyModelReady()

    override fun generate(
        conversationId: String,
        prompt: String,
        history: List<ChatMessage>
    ): Flow<String> = flow {
        if (!modelRepository.isAnyModelReady()) {
            throw IllegalStateException("Model AI belum diunduh. Silakan unduh model melalui ModelGate.")
        }

        inferenceMutex.withLock {
            val preferredModelId = preferencesManager.selectedModel.first()
            val activeEngine = getOrInitEngine(preferredModelId)
            val conversation = getOrCreateConversation(activeEngine, conversationId, history)
            var completed = false

            try {
                conversation.sendMessageAsync(prompt).collect { message ->
                    val chunk = cleanResponseChunk(message.extractText())
                    if (chunk.isNotBlank()) {
                        emit(chunk)
                    }
                }
                completed = true
            } catch (throwable: Throwable) {
                runCatching { conversation.cancelProcess() }
                conversations.remove(conversationId)
                if (throwable is CancellationException) {
                    throw throwable
                }
                throw throwable
            } finally {
                if (!completed) {
                    conversations.remove(conversationId)
                    closeQuietly(conversation)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun clearConversation(conversationId: String) {
        inferenceMutex.withLock {
            conversations.remove(conversationId)?.let(::closeQuietly)
        }
    }

    override suspend fun clearAllConversations() {
        inferenceMutex.withLock { closeConversations() }
    }

    private fun getOrCreateConversation(
        activeEngine: Engine,
        conversationId: String,
        history: List<ChatMessage>
    ): Conversation {
        conversations[conversationId]?.let { return it }

        if (conversations.size >= MAX_CACHED_CONVERSATIONS) {
            val oldest = conversations.entries.iterator().next()
            conversations.remove(oldest.key)
            closeQuietly(oldest.value)
        }

        val initialMessages = history
            .filter { it.content.isNotBlank() }
            .takeLast(MAX_RESTORED_MESSAGES)
            .mapNotNull { it.toLiteRtMessage() }
            .toList()

        return activeEngine.createConversation(
            conversationConfig.copy(initialMessages = initialMessages)
        ).also { conversations[conversationId] = it }
    }

    private fun closeConversations() {
        conversations.values.toList().forEach(::closeQuietly)
        conversations.clear()
    }

    private fun closeQuietly(conversation: Conversation) {
        runCatching { conversation.close() }
            .onFailure { Log.w(TAG, "Error closing LiteRT-LM conversation", it) }
    }

    private fun Message.extractText(): String = contents.contents
        .filterIsInstance<Content.Text>()
        .joinToString(separator = "") { it.text }

    private fun cleanResponseChunk(response: String): String {
        val stopIndex = STOP_TOKENS
            .mapNotNull { token -> response.indexOf(token).takeIf { it >= 0 } }
            .minOrNull()
        val withoutStopToken = stopIndex?.let(response::substring) ?: response
        return STOP_TOKENS.fold(withoutStopToken) { value, token -> value.replace(token, "") }
    }

    fun close() {
        runBlocking(Dispatchers.IO) {
            inferenceMutex.withLock {
                closeConversations()
                runCatching { engine?.close() }
                    .onFailure { Log.w(TAG, "Error closing LiteRT-LM engine", it) }
                engine = null
                loadedModelPath = null
            }
        }
    }

    private fun ChatMessage.toLiteRtMessage(): Message? = when (role) {
        MessageRole.USER -> Message.user(content)
        MessageRole.ASSISTANT -> Message.model(content)
        MessageRole.SYSTEM -> Message.system(content)
    }

    companion object {
        private const val TAG = "LiteRtLmRunner"
        private const val MAX_CACHED_CONVERSATIONS = 2
        private const val MAX_RESTORED_MESSAGES = 8
        private const val LOAD_FACTOR = 0.75f
        private val STOP_TOKENS = listOf(
            "<end_of_turn>",
            "<|im_end|>",
            "<|endoftext|>",
            "<eos>",
            "[EOS]"
        )
    }
}
