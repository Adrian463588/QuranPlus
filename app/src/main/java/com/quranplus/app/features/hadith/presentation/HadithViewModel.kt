package com.quranplus.app.features.hadith.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.hadith.domain.GetHadithCollectionsUseCase
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.SearchHadithUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HadithUiState {
    data object Loading : HadithUiState
    data object Catalog : HadithUiState
    data object Empty : HadithUiState
    data class Ready(val records: List<HadithRecord>) : HadithUiState
    data class Error(val message: String) : HadithUiState
}

class HadithViewModel(
    getHadithCollectionsUseCase: GetHadithCollectionsUseCase,
    private val searchHadithUseCase: SearchHadithUseCase
) : ViewModel() {
    val collections: StateFlow<List<HadithCollection>> = getHadithCollectionsUseCase()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCollection = MutableStateFlow<String?>(null)
    val selectedCollection: StateFlow<String?> = _selectedCollection.asStateFlow()

    private val _state = MutableStateFlow<HadithUiState>(HadithUiState.Loading)
    val state: StateFlow<HadithUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            collections.collect { updateCatalogState() }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
        if (isCatalog()) updateCatalogState() else search()
    }

    fun setCollection(value: String?) {
        _selectedCollection.value = value
        if (isCatalog()) updateCatalogState() else search()
    }

    private fun search() {
        if (isCatalog()) {
            updateCatalogState()
            return
        }
        viewModelScope.launch {
            _state.value = HadithUiState.Loading
            runCatching {
                searchHadithUseCase(_selectedCollection.value, _query.value)
            }.onSuccess { records ->
                _state.value = if (records.isEmpty()) HadithUiState.Empty else HadithUiState.Ready(records)
            }.onFailure { error ->
                _state.value = HadithUiState.Error(error.localizedMessage ?: "Hadist tidak dapat dimuat")
            }
        }
    }

    private fun isCatalog(): Boolean = _query.value.isBlank() && _selectedCollection.value == null

    private fun updateCatalogState() {
        if (isCatalog()) {
            _state.value = if (collections.value.isEmpty()) HadithUiState.Empty else HadithUiState.Catalog
        }
    }
}
