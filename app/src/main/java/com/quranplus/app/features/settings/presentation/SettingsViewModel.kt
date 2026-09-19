package com.quranplus.app.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.features.chatbot.data.ModelDownloadScheduler
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val modelRepository: ModelRepository? = null,
    private val modelDownloadScheduler: ModelDownloadScheduler? = null
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = preferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val arabicFontSize: StateFlow<Float> = preferencesManager.arabicFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28f)

    val showTransliteration: StateFlow<Boolean> = preferencesManager.showTransliteration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showTranslation: StateFlow<Boolean> = preferencesManager.showTranslation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val enableTajwid: StateFlow<Boolean> = preferencesManager.enableTajwid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val selectedPersona: StateFlow<AiPersona> = preferencesManager.selectedPersona
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiPersona.USTADZ)

    val customSystemPrompt: StateFlow<String> = preferencesManager.customSystemPrompt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiPersona.CUSTOM.defaultPrompt)

    val selectedModel: StateFlow<String> = preferencesManager.selectedModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedEmbeddingModel: StateFlow<String> = preferencesManager.selectedEmbeddingModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "all-minilm-l6-v2-onnx")

    val availableEmbeddingModels: List<ModelInfo>
        get() = modelRepository?.availableEmbeddingModels ?: emptyList()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadingModel = MutableStateFlow<ModelInfo?>(null)
    val downloadingModel: StateFlow<ModelInfo?> = _downloadingModel.asStateFlow()

    private var downloadJob: Job? = null

    fun isModelInstalled(modelInfo: ModelInfo): Boolean =
        modelRepository?.isModelReady(modelInfo) == true

    fun startEmbeddingModelDownload(modelInfo: ModelInfo) {
        val scheduler = modelDownloadScheduler ?: return
        if (!modelInfo.isDownloadable || modelInfo.sha256.isNullOrBlank()) {
            _downloadState.value = DownloadState.Failed(
                "Unduhan belum tersedia: ${modelInfo.downloadBlocker}"
            )
            return
        }
        _downloadingModel.value = modelInfo
        downloadJob?.cancel()
        val requestId = runCatching { scheduler.enqueue(modelInfo) }
            .getOrElse { error ->
                _downloadState.value = DownloadState.Failed(
                    error.localizedMessage ?: "Unduhan model tidak dapat dijadwalkan"
                )
                _downloadingModel.value = null
                return
            }
        downloadJob = viewModelScope.launch {
            scheduler.observe(requestId, modelInfo).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Completed) {
                    _downloadingModel.value = null
                } else if (state is DownloadState.Failed || state is DownloadState.ChecksumError) {
                    _downloadingModel.value = null
                }
            }
        }
    }

    fun cancelEmbeddingModelDownload(modelInfo: ModelInfo? = _downloadingModel.value) {
        val target = modelInfo ?: return
        modelDownloadScheduler?.cancel(target)
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
        _downloadingModel.value = null
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDarkMode(enabled) }
    }

    fun setArabicFontSize(size: Float) {
        viewModelScope.launch { preferencesManager.setArabicFontSize(size) }
    }

    fun setShowTransliteration(show: Boolean) {
        viewModelScope.launch { preferencesManager.setShowTransliteration(show) }
    }

    fun setShowTranslation(show: Boolean) {
        viewModelScope.launch { preferencesManager.setShowTranslation(show) }
    }

    fun setEnableTajwid(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setEnableTajwid(enabled) }
    }

    fun setSelectedPersona(persona: AiPersona) {
        viewModelScope.launch { preferencesManager.setSelectedPersona(persona) }
    }

    fun setCustomSystemPrompt(prompt: String) {
        viewModelScope.launch { preferencesManager.setCustomSystemPrompt(prompt) }
    }

    fun setSelectedModel(modelName: String) {
        viewModelScope.launch { preferencesManager.setSelectedModel(modelName) }
    }

    fun setSelectedEmbeddingModel(modelId: String) {
        viewModelScope.launch { preferencesManager.setSelectedEmbeddingModel(modelId) }
    }
}
