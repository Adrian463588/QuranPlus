package com.quranplus.app.features.chatbot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.chatbot.data.ModelDownloadScheduler
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.chatbot.data.AiBlocker
import com.quranplus.app.features.chatbot.data.AiReadiness
import com.quranplus.app.features.chatbot.data.AiReadinessChecker
import com.quranplus.app.features.chatbot.domain.ChatMessage
import com.quranplus.app.features.chatbot.domain.ClearChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.GenerateRagAnswerUseCase
import com.quranplus.app.features.chatbot.domain.GetChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.MessageRole
import com.quranplus.app.features.chatbot.domain.SaveChatMessageUseCase
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val saveChatMessageUseCase: SaveChatMessageUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val generateRagAnswerUseCase: GenerateRagAnswerUseCase,
    private val modelRepository: ModelRepository,
    private val modelDownloadScheduler: ModelDownloadScheduler,
    private val preferencesManager: PreferencesManager,
    private val readinessChecker: AiReadinessChecker
) : ViewModel() {

    private val conversationId = "default_conversation"

    val messages: StateFlow<List<ChatMessage>> = getChatHistoryUseCase(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _readiness = MutableStateFlow(
        AiReadiness(isReady = false, blockers = AiBlocker.entries.toSet())
    )
    val readiness: StateFlow<AiReadiness> = _readiness.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var downloadJob: kotlinx.coroutines.Job? = null

    init {
        checkModelStatus()
    }

    fun checkModelStatus() {
        viewModelScope.launch {
            val readiness = readinessChecker.check()
            _readiness.value = readiness
            _isModelReady.value = readiness.isReady
        }
    }

    fun startModelDownload(modelInfo: ModelInfo) {
        if (!modelInfo.isDownloadable || modelInfo.sha256.isNullOrBlank()) {
            _downloadState.value = DownloadState.Failed(
                "Unduhan belum tersedia: ${modelInfo.downloadBlocker}"
            )
            return
        }
        downloadJob?.cancel()
        val requestId = runCatching { modelDownloadScheduler.enqueue(modelInfo) }
            .getOrElse { error ->
                _downloadState.value = DownloadState.Failed(
                    error.localizedMessage ?: "Unduhan model tidak dapat dijadwalkan"
                )
                return
            }
        downloadJob = viewModelScope.launch {
            modelDownloadScheduler.observe(requestId, modelInfo).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Completed) {
                    _isModelReady.value = modelRepository.isModelReady(modelInfo)
                    checkModelStatus()
                }
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isStreaming.value) return
        if (!_isModelReady.value) {
            _errorMessage.value = _readiness.value.blockers.joinToString(", ") { it.name }
            return
        }

        viewModelScope.launch {
            _errorMessage.value = null
            // 1. Save user message
            val userMessage = ChatMessage(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = userText
            )
            saveChatMessageUseCase(userMessage)

            // 2. Fetch current active persona
            val persona = preferencesManager.selectedPersona.first()
            val customPrompt = preferencesManager.customSystemPrompt.first()

            _isStreaming.value = true
            _streamingContent.value = ""

            try {
                // 3. Generate RAG Answer with Ground Truth
                val result = generateRagAnswerUseCase(userText, persona, customPrompt)
                val fullResponse = StringBuilder()

                result.tokenStream.collect { token ->
                    fullResponse.append(token)
                    _streamingContent.value = fullResponse.toString()
                }

                if (fullResponse.isEmpty()) {
                    throw IllegalStateException("Model tidak mengembalikan token jawaban")
                }

                // 4. Save Assistant Response with citations
                val assistantMessage = ChatMessage(
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = fullResponse.toString(),
                    citations = result.citations
                )
                saveChatMessageUseCase(assistantMessage)

            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                    ?.takeIf(String::isNotBlank)
                    ?: "Inferensi lokal gagal tanpa menghasilkan jawaban."
            } finally {
                _isStreaming.value = false
                _streamingContent.value = ""
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            clearChatHistoryUseCase(conversationId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
