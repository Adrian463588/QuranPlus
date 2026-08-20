package com.quranplus.app.features.rag.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.rag.data.SafDocumentImporter
import com.quranplus.app.features.rag.data.SafDocumentMetadata
import com.quranplus.app.features.rag.data.SafImportResult
import com.quranplus.app.features.rag.data.SafAssetStore
import com.quranplus.app.features.rag.data.SafStorageStatus
import com.quranplus.app.features.rag.data.RagCorpusIndexer
import com.quranplus.app.features.rag.domain.IndexCorpusResult
import com.quranplus.app.features.hadith.data.HadithReferenceImporter
import com.quranplus.app.features.hadith.data.HadithBundleManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RagImportState {
    data object Idle : RagImportState
    data object Importing : RagImportState
    data object Indexing : RagImportState
    data class Indexed(val count: Int) : RagImportState
    data class IndexBlocked(val reason: String) : RagImportState
    data object LinkingStorage : RagImportState
    data class StorageLinked(val status: SafStorageStatus) : RagImportState
    data class StoredAwaitingEmbedding(val metadata: SafDocumentMetadata) : RagImportState
    data class HadithImported(val title: String, val count: Int) : RagImportState
    data class Unsupported(val reason: String) : RagImportState
    data class Error(val reason: String) : RagImportState
}

class RagDocumentViewModel(
    private val importer: SafDocumentImporter,
    private val assetStore: SafAssetStore,
    private val corpusIndexer: RagCorpusIndexer,
    private val hadithReferenceImporter: HadithReferenceImporter,
    private val hadithBundleManager: HadithBundleManager
) : ViewModel() {
    private val _state = MutableStateFlow<RagImportState>(RagImportState.Idle)
    val state: StateFlow<RagImportState> = _state.asStateFlow()

    private val _storageStatus = MutableStateFlow<SafStorageStatus?>(null)
    val storageStatus: StateFlow<SafStorageStatus?> = _storageStatus.asStateFlow()

    private var indexJob: Job? = null
    private var indexRequested = false
    private var indexCompleted = false

    init {
        refreshStorageStatus()
    }

    fun refreshStorageStatus() {
        viewModelScope.launch {
            val status = runCatching { assetStore.getStatus() }.getOrNull()
            _storageStatus.value = status
            if (status?.isAccessible == true) {
                hadithBundleManager.restoreFromSaf()
                _state.value = RagImportState.StorageLinked(status)
            }
        }
    }

    fun linkStorageTree(uri: Uri, grantFlags: Int) {
        viewModelScope.launch {
            _state.value = RagImportState.LinkingStorage
            val result = runCatching { assetStore.linkTree(uri, grantFlags) }
            result.onSuccess { status ->
                _storageStatus.value = status
                _state.value = RagImportState.StorageLinked(status)
            }.onFailure {
                _storageStatus.value = null
                _state.value = RagImportState.Error(
                    it.localizedMessage ?: "Folder SAF tidak dapat digunakan"
                )
            }
            if (result.isSuccess) {
                hadithBundleManager.restoreFromSaf()
                buildIndex()
            }
        }
    }

    fun importDocument(uri: Uri, grantFlags: Int) {
        viewModelScope.launch {
            _state.value = RagImportState.Importing
            runCatching { importer.persistPermission(uri, grantFlags) }
            val hadithSummary = runCatching { hadithReferenceImporter.import(uri) }.getOrNull()
            if (hadithSummary != null) {
                _state.value = RagImportState.HadithImported(
                    hadithSummary.title,
                    hadithSummary.recordCount
                )
                buildIndex()
                return@launch
            }
            when (val result = importer.import(uri)) {
                is SafImportResult.StoredAwaitingEmbedding -> {
                    _state.value = RagImportState.StoredAwaitingEmbedding(result.metadata)
                    buildIndex()
                }
                is SafImportResult.Unsupported -> _state.value = RagImportState.Unsupported(result.reason)
                is SafImportResult.Error -> _state.value = RagImportState.Error(result.reason)
            }
        }
    }

    fun buildIndex() {
        indexCompleted = false
        requestIndex()
    }

    fun ensureIndex() {
        if (indexCompleted) return
        requestIndex()
    }

    private fun requestIndex() {
        indexRequested = true
        if (indexJob?.isActive == true) return

        indexJob = viewModelScope.launch {
            while (indexRequested) {
                indexRequested = false
                _state.value = RagImportState.Indexing
                val result = runCatching {
                    hadithBundleManager.restoreFromSaf()
                    corpusIndexer.index()
                }.getOrElse { IndexCorpusResult.Blocked("INDEX_UNAVAILABLE") }
                when (result) {
                    is IndexCorpusResult.Indexed -> {
                        indexCompleted = true
                        _state.value = RagImportState.Indexed(result.recordCount)
                    }
                    is IndexCorpusResult.Blocked -> {
                        indexCompleted = false
                        _state.value = RagImportState.IndexBlocked(result.reason)
                    }
                }
            }
            indexJob = null
        }
    }

}
