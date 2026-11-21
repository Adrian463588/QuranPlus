package com.quranplus.app.features.hadith.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.hadith.data.CanonicalHadithSynchronizer
import com.quranplus.app.features.hadith.data.HadithBundleManager
import com.quranplus.app.features.hadith.data.HadithBundleWorkState
import com.quranplus.app.features.hadith.domain.GetHadithCollectionsUseCase
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithLookupStatus
import com.quranplus.app.features.hadith.domain.HadithNumberingScheme
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.HadithSearchResult
import com.quranplus.app.features.hadith.domain.SearchHadithUseCase
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.app.core.database.SafUserDataManager
import com.quranplus.shared.features.quran.domain.HadithMarker
import com.quranplus.shared.features.quran.domain.HadithBookmark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    data class LookupIssue(val result: HadithSearchResult) : HadithUiState
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
    private val bundleManager: HadithBundleManager,
    private val preferencesManager: PreferencesManager? = null,
    private val safUserDataManager: SafUserDataManager? = null
) : ViewModel() {
    val collections: StateFlow<List<HadithCollection>> = getHadithCollectionsUseCase()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hadithMarkers: StateFlow<List<HadithMarker>> = preferencesManager?.hadithMarkers
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        ?: MutableStateFlow(emptyList())

    val hadithMarker: StateFlow<HadithMarker?> = preferencesManager?.hadithMarker
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        ?: MutableStateFlow(null)

    fun setHadithMarker(marker: HadithMarker?) {
        viewModelScope.launch {
            if (marker == null) {
                preferencesManager?.setHadithMarkers(emptyList())
            } else {
                preferencesManager?.setHadithMarkers(listOf(marker))
            }
            safUserDataManager?.exportMarkersToSaf()
        }
    }

    fun toggleHadithMarker(
        collectionId: String,
        collectionName: String,
        hadithNumber: Int,
        schemeId: String? = null,
        stableId: String? = null,
        onLimitReached: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = hadithMarkers.value
            val existing = current.find {
                (stableId != null && it.stableId == stableId) ||
                    (it.collectionId == collectionId && it.hadithNumber == hadithNumber && (it.schemeId ?: schemeId) == schemeId)
            }
            if (existing != null) {
                val updated = current.filterNot { it == existing }
                preferencesManager?.setHadithMarkers(updated)
                safUserDataManager?.exportMarkersToSaf()
            } else {
                if (current.size >= com.quranplus.app.core.ui.theme.QuranColors.MAX_READING_MARKERS) {
                    onLimitReached()
                    return@launch
                }
                val usedColors = current.map { it.colorIndex }.toSet()
                val nextColor = (0 until com.quranplus.app.core.ui.theme.QuranColors.MAX_READING_MARKERS).firstOrNull { it !in usedColors } ?: 0
                val newMarker = HadithMarker(
                    collectionId = collectionId,
                    collectionName = collectionName,
                    hadithNumber = hadithNumber,
                    timestamp = System.currentTimeMillis(),
                    colorIndex = nextColor,
                    schemeId = schemeId,
                    stableId = stableId
                )
                preferencesManager?.setHadithMarkers(current + newMarker)
                safUserDataManager?.exportMarkersToSaf()
            }
        }
    }

    val hadithBookmarks: StateFlow<List<HadithBookmark>> = preferencesManager?.hadithBookmarks
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        ?: MutableStateFlow(emptyList())

    fun toggleHadithBookmark(
        record: HadithRecord,
        collectionName: String,
        note: String? = null
    ) {
        viewModelScope.launch {
            val current = hadithBookmarks.value
            val existing = current.find {
                (record.stableId.isNotBlank() && it.stableId == record.stableId) ||
                    (it.collectionId == record.collectionId && it.hadithNumber == record.hadithNumber &&
                        (it.schemeId == null || it.schemeId == record.schemeId))
            }
            if (existing != null) {
                val updated = current.filterNot { it == existing }
                preferencesManager?.setHadithBookmarks(updated)
                safUserDataManager?.exportHadithBookmarksToSaf()
            } else {
                val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
                val newBookmark = HadithBookmark(
                    id = nextId,
                    collectionId = record.collectionId,
                    collectionName = collectionName,
                    hadithNumber = record.hadithNumber,
                    hadithTextArabic = record.textArabic,
                    hadithTranslation = record.translationId.ifBlank { record.translationEn },
                    note = note,
                    timestamp = System.currentTimeMillis(),
                    schemeId = record.schemeId,
                    stableId = record.stableId.ifBlank { null }
                )
                preferencesManager?.setHadithBookmarks(current + newBookmark)
                safUserDataManager?.exportHadithBookmarksToSaf()
            }
        }
    }

    fun deleteHadithBookmark(bookmark: HadithBookmark) {
        viewModelScope.launch {
            val current = hadithBookmarks.value
            val updated = current.filterNot { it.collectionId == bookmark.collectionId && it.hadithNumber == bookmark.hadithNumber }
            preferencesManager?.setHadithBookmarks(updated)
            safUserDataManager?.exportHadithBookmarksToSaf()
        }
    }

    fun restoreHadithBookmark(bookmark: HadithBookmark) {
        viewModelScope.launch {
            val current = hadithBookmarks.value
            if (current.none { it.collectionId == bookmark.collectionId && it.hadithNumber == bookmark.hadithNumber }) {
                preferencesManager?.setHadithBookmarks(current + bookmark)
                safUserDataManager?.exportHadithBookmarksToSaf()
            }
        }
    }

    fun updateHadithBookmarkNote(collectionId: String, hadithNumber: Int, note: String?) {
        viewModelScope.launch {
            val current = hadithBookmarks.value
            val updated = current.map {
                if (it.collectionId == collectionId && it.hadithNumber == hadithNumber) {
                    it.copy(note = note)
                } else it
            }
            preferencesManager?.setHadithBookmarks(updated)
            safUserDataManager?.exportHadithBookmarksToSaf()
        }
    }


    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCollection = MutableStateFlow<String?>(null)
    val selectedCollection: StateFlow<String?> = _selectedCollection.asStateFlow()

    private val _selectedScheme = MutableStateFlow(HadithNumberingScheme.SUNNAH)
    val selectedScheme: StateFlow<HadithNumberingScheme> = _selectedScheme.asStateFlow()

    private val _scrollToIndex = MutableStateFlow<Int?>(null)
    val scrollToIndex: StateFlow<Int?> = _scrollToIndex.asStateFlow()

    private val _highlightedNumber = MutableStateFlow<Int?>(null)
    val highlightedNumber: StateFlow<Int?> = _highlightedNumber.asStateFlow()

    private val _highlightedStableId = MutableStateFlow<String?>(null)
    val highlightedStableId: StateFlow<String?> = _highlightedStableId.asStateFlow()

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
                if (rootUri != null) {
                    bundleManager.restoreFromSaf()
                    safUserDataManager?.restoreFromSaf()
                }
                refreshBundleStatus()
            }
        }
        viewModelScope.launch {
            bundleManager.observeDownload().collect(::handleBundleWorkState)
        }
    }

    fun onScrolledToIndex() {
        _scrollToIndex.value = null
    }

    fun clearHighlight() {
        _highlightedNumber.value = null
        _highlightedStableId.value = null
    }

    fun setQuery(value: String) {
        _query.value = value
        if (isCatalog()) updateCatalogState() else search()
    }

    fun setCollection(value: String?) {
        _selectedCollection.value = value
        _selectedScheme.value = if (value != null) {
            HadithNumberingScheme.defaultForCollection(value)
        } else {
            HadithNumberingScheme.SUNNAH
        }
        _highlightedNumber.value = null
        _highlightedStableId.value = null
        _scrollToIndex.value = null
        if (isCatalog()) updateCatalogState() else search()
    }

    fun setScheme(scheme: HadithNumberingScheme) {
        val allowed = HadithNumberingScheme.schemesForCollection(_selectedCollection.value)
        if (scheme !in allowed || _selectedScheme.value == scheme) return
        _selectedScheme.value = scheme
        _highlightedNumber.value = null
        _highlightedStableId.value = null
        _scrollToIndex.value = null
        if (!isCatalog()) search()
    }

    private var searchJob: Job? = null

    fun retrySync() {
        viewModelScope.launch {
            _state.value = HadithUiState.Loading
            runCatching {
                bundleManager.syncCanonicalIfNeeded()
                refreshBundleStatus()
            }
            if (isCatalog()) updateCatalogState() else search()
        }
    }

    fun openReference(
        collectionId: String,
        hadithNumber: Int,
        schemeId: String? = null
    ) {
        val normalizedCol = collectionId.trim().lowercase()
        if (normalizedCol.isBlank() || hadithNumber <= 0) return
        val allowed = HadithNumberingScheme.schemesForCollection(normalizedCol)
        val parsedScheme = HadithNumberingScheme.fromSchemeId(schemeId)
        val resolvedScheme = parsedScheme?.takeIf { it in allowed }
            ?: HadithNumberingScheme.defaultForCollection(normalizedCol)
        _selectedCollection.value = normalizedCol
        _selectedScheme.value = resolvedScheme
        _query.value = ""
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = HadithUiState.Loading
            try {
                val target = searchHadithUseCase.searchDetailed(normalizedCol, hadithNumber.toString(), resolvedScheme)
                if (target.records.isEmpty()) {
                    _query.value = hadithNumber.toString()
                    _state.value = HadithUiState.LookupIssue(target)
                    return@launch
                }
                val resolvedRecord = target.records.first()
                val windowList = searchHadithUseCase.getReferenceWindow(
                    collectionId = normalizedCol,
                    targetNumber = resolvedRecord.hadithNumber,
                    scheme = resolvedScheme
                ).ifEmpty { target.records }
                val targetIndex = windowList.indexOfFirst { it.id == resolvedRecord.id }
                if (targetIndex >= 0) {
                    _state.value = HadithUiState.Ready(windowList)
                    _highlightedNumber.value = resolvedRecord.hadithNumber
                    _highlightedStableId.value = resolvedRecord.stableId
                    _scrollToIndex.value = targetIndex
                } else {
                    _state.value = HadithUiState.Ready(target.records)
                    _highlightedNumber.value = resolvedRecord.hadithNumber
                    _highlightedStableId.value = resolvedRecord.stableId
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = HadithUiState.Error(error.localizedMessage ?: "Hadist tidak dapat dimuat")
            }
        }
    }

    fun resetToCatalog() {
        searchJob?.cancel()
        _query.value = ""
        _selectedCollection.value = null
        _highlightedNumber.value = null
        _highlightedStableId.value = null
        _scrollToIndex.value = null
        updateCatalogState()
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
            searchJob?.cancel()
            updateCatalogState()
            return
        }
        val collectionId = _selectedCollection.value
        val rawQuery = _query.value.trim()
        val scheme = _selectedScheme.value
        val isNumericRef = extractReferenceNumber(rawQuery) != null

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (collectionId != null && isNumericRef) {
                val targetResult = try {
                    searchHadithUseCase.searchDetailed(collectionId, rawQuery, scheme)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _state.value = HadithUiState.Error(error.localizedMessage ?: "Hadist tidak dapat dimuat")
                    return@launch
                }

                if (targetResult.status != HadithLookupStatus.FOUND || targetResult.records.isEmpty()) {
                    _highlightedNumber.value = null
                    _highlightedStableId.value = null
                    _state.value = HadithUiState.LookupIssue(targetResult)
                    return@launch
                }

                if (targetResult.records.size > 1) {
                    _highlightedNumber.value = null
                    _highlightedStableId.value = null
                    _scrollToIndex.value = null
                    _state.value = HadithUiState.Ready(targetResult.records)
                    return@launch
                }

                val match = targetResult.records.single()
                val effectiveResultScheme = targetResult.scheme
                if (_selectedScheme.value != effectiveResultScheme &&
                    effectiveResultScheme in HadithNumberingScheme.schemesForCollection(collectionId)
                ) {
                    _selectedScheme.value = effectiveResultScheme
                }
                val currentRecords = (_state.value as? HadithUiState.Ready)?.records
                val collectionRecords = if (
                    currentRecords != null &&
                    currentRecords.firstOrNull()?.collectionId == collectionId &&
                    currentRecords.firstOrNull()?.schemeId == effectiveResultScheme.schemeId &&
                    currentRecords.size > 1 &&
                    currentRecords.any { it.id == match.id }
                ) {
                    currentRecords
                } else {
                    _state.value = HadithUiState.Loading
                    try {
                        searchHadithUseCase.getReferenceWindow(
                            collectionId = collectionId,
                            targetNumber = match.hadithNumber,
                            scheme = effectiveResultScheme
                        ).ifEmpty { listOf(match) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                val targetIndex = collectionRecords.indexOfFirst { it.id == match.id }
                if (targetIndex >= 0) {
                    _state.value = HadithUiState.Ready(collectionRecords)
                    _highlightedNumber.value = collectionRecords[targetIndex].hadithNumber
                    _highlightedStableId.value = collectionRecords[targetIndex].stableId
                    _scrollToIndex.value = targetIndex
                } else {
                    _state.value = HadithUiState.Ready(listOf(match))
                    _highlightedNumber.value = match.hadithNumber
                    _highlightedStableId.value = match.stableId
                }
                return@launch
            }

            _highlightedNumber.value = null
            _highlightedStableId.value = null
            _state.value = HadithUiState.Loading
            try {
                val result = searchHadithUseCase.searchDetailed(collectionId, rawQuery, scheme)
                _state.value = if (result.status == HadithLookupStatus.FOUND && result.records.isNotEmpty()) {
                    HadithUiState.Ready(result.records)
                } else {
                    HadithUiState.LookupIssue(result)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = HadithUiState.Error(error.localizedMessage ?: "Hadist tidak dapat dimuat")
            }
        }
    }

    private fun extractReferenceNumber(input: String): Int? {
        val trimmed = input.trim()
        val match = NUMERIC_REFERENCE_REGEX.matchEntire(trimmed)
        if (match != null) {
            return match.groupValues[1].toIntOrNull()?.takeIf { it > 0 }
        }
        val stripped = COLLECTION_LEAD_IN_REGEX.replace(trimmed, "").trim()
        return com.quranplus.app.features.rag.domain.HadithReferenceRequest.findAll(stripped)
            .singleOrNull()
            ?.takeIf { _selectedCollection.value == null || _selectedCollection.value == it.collection }
            ?.takeIf { stripped.split(Regex("\\s+")).filter(String::isNotBlank).size <= 5 }
            ?.number
    }

    private fun isCatalog(): Boolean = _query.value.isBlank() && _selectedCollection.value == null

    private fun updateCatalogState() {
        if (isCatalog()) {
            _state.value = if (collections.value.isEmpty()) HadithUiState.Empty else HadithUiState.Catalog
        }
    }

    private companion object {
        val COLLECTION_LEAD_IN_REGEX = Regex("(?i)^(?:hr\\.?|hadit[hs]?|hadis|hadist)\\s+")
        val NUMERIC_REFERENCE_REGEX = Regex(
            "(?i)^(?:(?:arsip|gading|tafsirq|(?:al[ -]*)?alamiyah|(?:ar[ -]*)?risalah|hadits?)\\s+|(?:no\\.?|nomor|#)(?:\\s+|(?=[\\d])))*(\\d{1,6})(?:\\.(\\d{1,3}|[a-z]{1,3})|([a-z]{1,3}))?(?:\\s+(?:arsip|gading|tafsirq|(?:al[ -]*)?alamiyah|(?:ar[ -]*)?risalah))?$"
        )
    }
}
