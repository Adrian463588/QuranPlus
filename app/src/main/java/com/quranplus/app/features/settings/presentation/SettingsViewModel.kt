package com.quranplus.app.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemma-3-1b-it.litertlm")

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
}
