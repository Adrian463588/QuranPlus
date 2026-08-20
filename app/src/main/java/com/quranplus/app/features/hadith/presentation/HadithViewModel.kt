package com.quranplus.app.features.hadith.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.hadith.data.HadithBundleManager
import com.quranplus.app.features.hadith.data.HadithBundleWorkState
import com.quranplus.app.features.hadith.domain.GetHadithCollectionsUseCase
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.SearchHadithUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HadithUiState {
    data object Loading : HadithUiState
    data object Catalog : HadithUiState
    data object Empty : HadithUiState
    data class Ready(val records: List<HadithRecord>) : HadithUiState
    data class Error(val message: String) : HadithUiState
}

data class HadithBundleUiState(
    val storageLinked: Boolean = false,
    val localRecordCount: Int = 0,
    val localCollectionCount: Int = 0,
    val workState: HadithBundleWorkState = HadithBundleWorkState.Idle,
    val errorMessage: String? = null
)

class HadithViewModel(
    getHadithCollectionsUseCase: GetHadithCollectionsUseCase,
    private val searchHadithUseCase: SearchHadithUseCase,
    private val bundleManager: HadithBundleManager
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

    private val _bundleState = MutableStateFlow(HadithBundleUiState())
    val bundleState: StateFlow<HadithBundleUiState> = _bundleState.asStateFlow()

    private val _bundleReadyEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val bundleReadyEvents = _bundleReadyEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            collections.collect { updateCatalogState() }
        }
        viewModelScope.launch {
            bundleManager.observeStorageRoot().collect { rootUri ->
                if (rootUri != null) bundleManager.restoreFromSaf()
                refreshBundleStatus()
            }
        }
        viewModelScope.launch {
            bundleManager.observeDownload().collect(::handleBundleWorkState)
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

    fun startBundleDownload() {
        viewModelScope.launch {
            val status = runCatching { bundleManager.status() }.getOrNull()
            if (status?.storageLinked != true) {
                _bundleState.update { it.copy(errorMessage = "Pilih folder SAF sebelum mengunduh Hadist") }
                return@launch
            }
            runCatching { bundleManager.enqueueDownload() }
                .onFailure { error ->
                    _bundleState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Download Hadist gagal")
                    }
                }
        }
    }

    fun clearBundleError() {
        _bundleState.update { it.copy(errorMessage = null) }
    }

    private suspend fun refreshBundleStatus() {
        val status = runCatching { bundleManager.status() }.getOrNull() ?: return
        _bundleState.update {
            it.copy(
                storageLinked = status.storageLinked,
                localRecordCount = status.localRecordCount,
                localCollectionCount = status.localCollectionCount,
                errorMessage = null
            )
        }
    }

    private suspend fun handleBundleWorkState(workState: HadithBundleWorkState) {
        _bundleState.update {
            it.copy(
                workState = workState,
                errorMessage = (workState as? HadithBundleWorkState.Failed)?.message
            )
        }
        if (workState is HadithBundleWorkState.Completed) {
            refreshBundleStatus()
            _bundleReadyEvents.emit(Unit)
        }
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
