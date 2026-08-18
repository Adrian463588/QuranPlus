package com.quranplus.app.features.chatbot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.network.ResumableDownloader
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.chatbot.data.ModelRepository
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
import java.io.File

class ChatViewModel(
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val saveChatMessageUseCase: SaveChatMessageUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val generateRagAnswerUseCase: GenerateRagAnswerUseCase,
    private val modelRepository: ModelRepository,
    private val resumableDownloader: ResumableDownloader,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val conversationId = "default_conversation"

    val messages: StateFlow<List<ChatMessage>> = getChatHistoryUseCase(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isModelReady = MutableStateFlow(modelRepository.isAnyModelReady())
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun checkModelStatus() {
        _isModelReady.value = modelRepository.isAnyModelReady()
    }

    fun startModelDownload(modelInfo: ModelInfo) {
        viewModelScope.launch {
            val targetFile = modelRepository.getModelFile(modelInfo.filename)
            resumableDownloader.downloadFile(
                url = modelInfo.downloadUrl,
                targetDestination = targetFile,
                expectedSha256 = modelInfo.sha256
            ).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Completed) {
                    _isModelReady.value = true
                }
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isStreaming.value) return

        viewModelScope.launch {
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

                // 4. Save Assistant Response with citations
                val assistantMessage = ChatMessage(
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = fullResponse.toString(),
                    citations = result.citations
                )
                saveChatMessageUseCase(assistantMessage)

            } catch (e: Exception) {
                // Fallback response if on-device model generation has issue
                val fallbackMessage = ChatMessage(
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = "Maaf, terjadi kendala saat memproses inferensi on-device: ${e.localizedMessage}"
                )
                saveChatMessageUseCase(fallbackMessage)
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
}
