package com.quranplus.app.features.rag.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.rag.data.SafDocumentImporter
import com.quranplus.app.features.rag.data.SafDocumentMetadata
import com.quranplus.app.features.rag.data.SafImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RagImportState {
    data object Idle : RagImportState
    data object Importing : RagImportState
    data class StoredAwaitingEmbedding(val metadata: SafDocumentMetadata) : RagImportState
    data class Unsupported(val reason: String) : RagImportState
    data class Error(val reason: String) : RagImportState
}

class RagDocumentViewModel(
    private val importer: SafDocumentImporter
) : ViewModel() {
    private val _state = MutableStateFlow<RagImportState>(RagImportState.Idle)
    val state: StateFlow<RagImportState> = _state.asStateFlow()

    fun importDocument(uri: Uri, grantFlags: Int) {
        viewModelScope.launch {
            _state.value = RagImportState.Importing
            runCatching { importer.persistPermission(uri, grantFlags) }
            when (val result = importer.import(uri)) {
                is SafImportResult.StoredAwaitingEmbedding -> {
                    _state.value = RagImportState.StoredAwaitingEmbedding(result.metadata)
                }
                is SafImportResult.Unsupported -> _state.value = RagImportState.Unsupported(result.reason)
                is SafImportResult.Error -> _state.value = RagImportState.Error(result.reason)
            }
        }
    }
}
