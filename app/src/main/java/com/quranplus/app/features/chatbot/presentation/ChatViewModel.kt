package com.quranplus.app.features.chatbot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.features.chatbot.data.AiBlocker
import com.quranplus.app.features.chatbot.data.AiReadiness
import com.quranplus.app.features.chatbot.data.AiReadinessChecker
import com.quranplus.app.features.chatbot.data.ModelAssetRole
import com.quranplus.app.features.chatbot.data.ModelDownloadScheduler
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.chatbot.domain.ChatMessage
import com.quranplus.app.features.chatbot.domain.ChatSession
import com.quranplus.app.features.chatbot.domain.ClearAllChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.ClearChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.GenerateRagAnswerUseCase
import com.quranplus.app.features.chatbot.domain.GetChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.GetChatSessionsUseCase
import com.quranplus.app.features.chatbot.domain.GenerationEvent
import com.quranplus.app.features.chatbot.domain.MessageRole
import com.quranplus.app.features.chatbot.domain.SaveChatMessageUseCase
import com.quranplus.app.features.rag.domain.CitationMarkerValidator
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.GenerationStatus
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

import com.quranplus.app.features.hadith.data.HadithBundleManager
import com.quranplus.app.features.hadith.data.HadithBundleWorkState
import com.quranplus.app.features.hadith.presentation.HadithBundleUiState
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val getChatSessionsUseCase: GetChatSessionsUseCase,
    private val saveChatMessageUseCase: SaveChatMessageUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val clearAllChatHistoryUseCase: ClearAllChatHistoryUseCase,
    private val generateRagAnswerUseCase: GenerateRagAnswerUseCase,
    private val modelRepository: ModelRepository,
    private val modelDownloadScheduler: ModelDownloadScheduler,
    private val preferencesManager: PreferencesManager,
    private val readinessChecker: AiReadinessChecker,
    private val hadithBundleManager: HadithBundleManager
) : ViewModel() {

    private val _hadithBundleState = MutableStateFlow(HadithBundleUiState())
    val hadithBundleState: StateFlow<HadithBundleUiState> = _hadithBundleState.asStateFlow()

    fun isModelInstalled(modelInfo: ModelInfo): Boolean = modelRepository.isModelReady(modelInfo)

    fun refreshHadithBundleStatus() {
        viewModelScope.launch {
            val status = runCatching { hadithBundleManager.status() }.getOrNull()
            if (status != null) {
                _hadithBundleState.update { current ->
                    current.copy(
                        storageLinked = status.storageLinked,
                        localRecordCount = status.localRecordCount,
                        localCollectionCount = status.localCollectionCount
                    )
                }
            }
        }
    }

    fun startHadithBundleDownload() {
        viewModelScope.launch {
            val status = runCatching { hadithBundleManager.status() }.getOrNull()
            if (status?.storageLinked != true) {
                _hadithBundleState.update {
                    it.copy(errorMessage = "Pilih folder SAF sebelum mengunduh bundle Hadist")
                }
                return@launch
            }
            runCatching { hadithBundleManager.enqueueDownload() }
                .onFailure { error ->
                    _hadithBundleState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Unduh Hadist gagal")
                    }
                }
        }
    }

    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = _currentConversationId
        .flatMapLatest { convId -> getChatHistoryUseCase(convId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<ChatSession>> = getChatSessionsUseCase()
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

    private val _streamingCitations = MutableStateFlow<List<RetrievedCitation>>(emptyList())
    val streamingCitations: StateFlow<List<RetrievedCitation>> = _streamingCitations.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadingModel = MutableStateFlow<ModelInfo?>(null)
    val downloadingModel: StateFlow<ModelInfo?> = _downloadingModel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var downloadJob: Job? = null

    init {
        checkModelStatus()
        refreshHadithBundleStatus()
        viewModelScope.launch {
            hadithBundleManager.observeDownload().collect { workState ->
                _hadithBundleState.update { current ->
                    current.copy(
                        workState = workState,
                        errorMessage = (workState as? HadithBundleWorkState.Failed)?.message
                    )
                }
                if (workState is HadithBundleWorkState.Completed) {
                    refreshHadithBundleStatus()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val activeModel = modelDownloadScheduler.findActiveModel(availableModels)
            if (activeModel != null) {
                _downloadingModel.value = activeModel
                observeModelDownload(java.util.UUID(0L, 0L), activeModel)
            }
        }
        viewModelScope.launch {
            val existing = sessions.first()
            if (existing.isNotEmpty()) {
                _currentConversationId.value = existing.first().conversationId
            }
        }
    }

    fun checkModelStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val readiness = readinessChecker.check()
            _readiness.value = readiness
            // Opening chat only requires a verified chatbot model. RAG readiness is
            // tracked separately so index construction cannot hide model selection.
            _isModelReady.value = readiness.isModelReady
        }
    }

    private var streamingJob: Job? = null
    private var generationSequence = 0L
    private var activeGenerationId: Long? = null

    fun createNewSession() {
        activeGenerationId = null
        generationSequence++
        streamingJob?.cancel()
        _isStreaming.value = false
        _streamingContent.value = ""
        _streamingCitations.value = emptyList()
        _currentConversationId.value = UUID.randomUUID().toString()
        _errorMessage.value = null
    }

    fun selectSession(conversationId: String) {
        activeGenerationId = null
        generationSequence++
        streamingJob?.cancel()
        _isStreaming.value = false
        _streamingContent.value = ""
        _streamingCitations.value = emptyList()
        _currentConversationId.value = conversationId
        _errorMessage.value = null
    }

    fun deleteSession(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            clearChatHistoryUseCase(conversationId)
            if (_currentConversationId.value == conversationId) {
                createNewSession()
            }
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            clearAllChatHistoryUseCase()
            createNewSession()
        }
    }

    fun startModelDownload(modelInfo: ModelInfo) {
        if (!modelInfo.isDownloadable || modelInfo.sha256.isNullOrBlank()) {
            _downloadState.value = DownloadState.Failed(
                "Unduhan belum tersedia: ${modelInfo.downloadBlocker}"
            )
            return
        }
        _downloadingModel.value = modelInfo
        downloadJob?.cancel()
        val requestId = runCatching { modelDownloadScheduler.enqueue(modelInfo) }
            .getOrElse { error ->
                _downloadState.value = DownloadState.Failed(
                    error.localizedMessage ?: "Unduhan model tidak dapat dijadwalkan"
                )
                _downloadingModel.value = null
                return
            }
        observeModelDownload(requestId, modelInfo)
    }

    private fun observeModelDownload(requestId: java.util.UUID, modelInfo: ModelInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            modelDownloadScheduler.observe(requestId, modelInfo).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Completed) {
                    val verified = modelRepository.verifyModelSha256Async(modelInfo)
                    if (modelInfo.role == ModelAssetRole.CHATBOT) {
                        _isModelReady.value = verified
                    }
                    _downloadingModel.value = null
                    if (modelInfo.role == ModelAssetRole.EMBEDDING) {
                        if (verified) {
                            preferencesManager.setSelectedEmbeddingModel(modelInfo.id)
                        }
                        _downloadState.value = DownloadState.Idle
                    }
                    checkModelStatus()
                }
            }
        }
    }

    fun cancelModelDownload(modelInfo: ModelInfo? = _downloadingModel.value) {
        val target = modelInfo ?: return
        modelDownloadScheduler.cancel(target)
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
        _downloadingModel.value = null
    }

    fun selectActiveModel(modelInfo: ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!modelRepository.isModelReady(modelInfo)) {
                _errorMessage.value = "Model belum terverifikasi; pilih model yang berstatus siap digunakan."
                return@launch
            }
            preferencesManager.setSelectedModel(modelInfo.id)
            _isModelReady.value = true
            checkModelStatus()
        }
    }

    val availableModels: List<ModelInfo>
        get() = modelRepository.availableModelConfigs

    val selectedModelId: StateFlow<String> = preferencesManager.selectedModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedEmbeddingModelId: StateFlow<String> = preferencesManager.selectedEmbeddingModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "all-minilm-l6-v2-onnx")

    val onlineResearchEnabled: StateFlow<Boolean> = preferencesManager.onlineResearchEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setOnlineResearchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setOnlineResearchEnabled(enabled)
        }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = modelRepository.availableModelConfigs.firstOrNull { it.id == modelId }
            if (model == null || !modelRepository.isModelReady(model)) {
                _errorMessage.value = "Model belum terverifikasi; pilih model yang berstatus siap digunakan."
                return@launch
            }
            preferencesManager.setSelectedModel(modelId)
            _isModelReady.value = true
            checkModelStatus()
        }
    }

    fun selectEmbeddingModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = modelRepository.availableModelConfigs.firstOrNull { it.id == modelId }
            if (model == null || !modelRepository.isModelReady(model)) {
                _errorMessage.value = "Model embedding belum terverifikasi atau tokenizer-nya belum cocok."
                return@launch
            }
            preferencesManager.setSelectedEmbeddingModel(modelId)
            checkModelStatus()
        }
    }

    private fun detectRepetitionLoop(text: String): Pair<String, Boolean> {
        if (text.length < 350) return Pair(text, false)
        for (len in 120 downTo 60) {
            if (text.length < len * 4) continue
            val tail = text.takeLast(len)
            val prev1 = text.substring(text.length - (len * 2), text.length - len)
            val prev2 = text.substring(text.length - (len * 3), text.length - (len * 2))
            val prev3 = text.substring(text.length - (len * 4), text.length - (len * 3))
            if (tail == prev1 && tail == prev2 && tail == prev3) {
                val clean = text.substring(0, text.length - (len * 3)).trimEnd()
                return Pair(clean, true)
            }
        }
        return Pair(text, false)
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isStreaming.value) return
        if (!_isModelReady.value) {
            _errorMessage.value = "Model AI belum siap. Silakan unduh atau pilih model."
            return
        }
        val readinessSnapshot = _readiness.value
        if (!onlineResearchEnabled.value && !readinessSnapshot.isReady) {
            _errorMessage.value = readinessErrorMessage(readinessSnapshot)
            return
        }

        // Set synchronously before launching IO work. This closes the race where
        // two quick taps created two requests and the second waited on LiteRT-LM.
        if (streamingJob?.isActive == true) return
        _isStreaming.value = true
        _streamingContent.value = ""
        _streamingCitations.value = emptyList()

        val targetConvId = _currentConversationId.value
        val generationId = ++generationSequence
        activeGenerationId = generationId

        streamingJob = viewModelScope.launch(Dispatchers.IO) {
            val fullResponse = StringBuilder()
            var visibleResponse = ""
            var ragCitations: List<RetrievedCitation> = emptyList()
            var generationStatus = GenerationStatus.STREAMING

            try {
                _errorMessage.value = null

                // 1. Save user message
                saveChatMessageUseCase(
                    ChatMessage(
                        conversationId = targetConvId,
                        role = MessageRole.USER,
                        content = trimmed
                    )
                )

                // 2. Fetch current active persona
                val persona = preferencesManager.selectedPersona.first()
                val customPrompt = preferencesManager.customSystemPrompt.first()

                // 3. Generate RAG Answer with Ground Truth
                val result = generateRagAnswerUseCase(
                    targetConvId,
                    trimmed,
                    persona,
                    customPrompt
                )
                ragCitations = result.citations
                if (activeGenerationId == generationId) {
                    _streamingCitations.value = ragCitations
                }

                result.tokenStream.collect { event ->
                    if (activeGenerationId != generationId || !_isStreaming.value) {
                        throw CancellationException("Generation is no longer active")
                    }
                    if (event is GenerationEvent.Replace) {
                        fullResponse.clear()
                        fullResponse.append(event.text)
                        visibleResponse = com.quranplus.app.features.chatbot.domain.AiResponsePostProcessor.process(event.text)
                        _streamingContent.value = visibleResponse
                        return@collect
                    }

                    val token = (event as GenerationEvent.Append).text
                    fullResponse.append(token)
                    val rawStr = fullResponse.toString()

                    // Check explicit LLM stop tokens
                    val stopKeywords = listOf(
                        "<end_of_turn>",
                        "<|im_end|>",
                        "<|endoftext|>",
                        "<eos>",
                        "[EOS]",
                        "<|im_start|>",
                        "\n<|im_start|>"
                    )
                    val stopIndex = stopKeywords.map { rawStr.indexOf(it) }
                        .filter { it >= 0 }
                        .minOrNull()

                    if (stopIndex != null) {
                        val cleaned = rawStr.substring(0, stopIndex).trim()
                        visibleResponse = com.quranplus.app.features.chatbot.domain.AiResponsePostProcessor.process(cleaned)
                        _streamingContent.value = visibleResponse
                        throw CancellationException("Stop token detected")
                    }

                    // Check repetition loop
                    val (loopCleaned, hasLoop) = detectRepetitionLoop(rawStr)
                    if (hasLoop) {
                        visibleResponse = com.quranplus.app.features.chatbot.domain.AiResponsePostProcessor.process(loopCleaned)
                        _streamingContent.value = visibleResponse
                        throw CancellationException("Repetition loop detected and truncated")
                    } else {
                        visibleResponse = com.quranplus.app.features.chatbot.domain.AiResponsePostProcessor.process(rawStr)
                        _streamingContent.value = visibleResponse
                    }
                }
                generationStatus = result.generationStatus?.value ?: GenerationStatus.COMPLETE
            } catch (e: CancellationException) {
                // Stop token hit, loop truncated, or user switched session
                generationStatus = GenerationStatus.STOPPED
            } catch (e: Exception) {
                generationStatus = GenerationStatus.UNAVAILABLE
                if (activeGenerationId == generationId) {
                    _errorMessage.value = e.localizedMessage
                        ?.takeIf(String::isNotBlank)
                        ?: "Inferensi lokal gagal tanpa menghasilkan jawaban."
                }
            } finally {
                val rawContent = visibleResponse.ifBlank { fullResponse.toString().trim() }
                val finalContent = com.quranplus.app.features.chatbot.domain.AiResponsePostProcessor.process(rawContent)
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    try {
                        if (finalContent.isNotBlank()) {
                            saveChatMessageUseCase(
                                ChatMessage(
                                    conversationId = targetConvId,
                                    role = MessageRole.ASSISTANT,
                                    content = finalContent,
                                    citations = ragCitations,
                                    generationStatus = generationStatus
                                )
                            )
                        }
                    } catch (e: Exception) {
                        if (activeGenerationId == generationId) {
                            _errorMessage.value = e.localizedMessage
                                ?.takeIf(String::isNotBlank)
                                ?: "Jawaban belum dapat disimpan."
                        }
                    }
                }
                if (activeGenerationId == generationId) {
                    _isStreaming.value = false
                    _streamingContent.value = ""
                    _streamingCitations.value = emptyList()
                    activeGenerationId = null
                }
            }
        }
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        _isStreaming.value = false
    }

    fun clearChat() {
        deleteSession(_currentConversationId.value)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun readinessErrorMessage(readiness: AiReadiness): String = when {
        AiBlocker.MODEL_UNAVAILABLE in readiness.blockers ->
            "Model AI belum siap. Silakan unduh atau pilih model."

        AiBlocker.CORPUS_UNAVAILABLE in readiness.blockers ->
            "Rujukan Quran dan Hadist belum lengkap atau belum diindeks. Unduh/import bundle Hadist lalu bangun index kembali."

        AiBlocker.EMBEDDER_UNAVAILABLE in readiness.blockers ->
            "Model embedding belum siap. Pilih atau unduh model embedding ONNX terlebih dahulu."

        AiBlocker.INDEX_UNAVAILABLE in readiness.blockers ->
            "Index rujukan belum siap. Tunggu proses indexing selesai lalu coba lagi."

        else -> "Rujukan lokal belum siap. Coba perbarui status AI lalu ulangi."
    }
}
