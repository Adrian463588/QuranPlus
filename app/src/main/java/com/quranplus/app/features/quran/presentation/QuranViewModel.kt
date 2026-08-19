package com.quranplus.app.features.quran.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.Bookmark
import com.quranplus.app.features.quran.domain.DeleteBookmarkUseCase
import com.quranplus.app.features.quran.domain.RestoreBookmarkUseCase
import com.quranplus.app.features.quran.domain.UpdateBookmarkNoteUseCase
import com.quranplus.app.features.quran.domain.GetAyahsBySurahUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByJuzUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByPageUseCase
import com.quranplus.app.features.quran.domain.GetBookmarksUseCase
import com.quranplus.app.features.quran.domain.GetLastReadUseCase
import com.quranplus.app.features.quran.domain.GetSurahDetailUseCase
import com.quranplus.app.features.quran.domain.GetSurahListUseCase
import com.quranplus.app.features.quran.domain.LastRead
import com.quranplus.app.features.quran.domain.BookmarkSort
import com.quranplus.app.features.quran.domain.SaveLastReadUseCase
import com.quranplus.app.features.quran.domain.SearchQuranUseCase
import com.quranplus.app.features.quran.domain.Surah
import com.quranplus.app.features.quran.domain.ToggleBookmarkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModel(
    private val getSurahListUseCase: GetSurahListUseCase,
    private val getSurahDetailUseCase: GetSurahDetailUseCase,
    private val getAyahsBySurahUseCase: GetAyahsBySurahUseCase,
    private val getFirstAyahByPageUseCase: GetFirstAyahByPageUseCase,
    private val getFirstAyahByJuzUseCase: GetFirstAyahByJuzUseCase,
    private val searchQuranUseCase: SearchQuranUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val restoreBookmarkUseCase: RestoreBookmarkUseCase,
    private val updateBookmarkNoteUseCase: UpdateBookmarkNoteUseCase,
    private val saveLastReadUseCase: SaveLastReadUseCase,
    private val getLastReadUseCase: GetLastReadUseCase
) : ViewModel() {

    val surahListState: StateFlow<UiState<List<Surah>>> = getSurahListUseCase()
        .map<List<Surah>, UiState<List<Surah>>> { list ->
            if (list.isEmpty()) UiState.Empty else UiState.Success(list)
        }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Gagal memuat daftar surah")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val lastReadState: StateFlow<LastRead?> = getLastReadUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _bookmarkSort = MutableStateFlow(BookmarkSort.NEWEST)
    val bookmarkSort: StateFlow<BookmarkSort> = _bookmarkSort.asStateFlow()

    val bookmarksState: StateFlow<UiState<List<Bookmark>>> = _bookmarkSort
        .flatMapLatest { sort -> getBookmarksUseCase(sort) }
        .map<List<Bookmark>, UiState<List<Bookmark>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Gagal memuat bookmark")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _currentSurah = MutableStateFlow<Surah?>(null)
    val currentSurah: StateFlow<Surah?> = _currentSurah.asStateFlow()

    private val _currentAyahsState = MutableStateFlow<UiState<List<Ayah>>>(UiState.Idle)
    val currentAyahsState: StateFlow<UiState<List<Ayah>>> = _currentAyahsState.asStateFlow()

    private val _searchState = MutableStateFlow<UiState<List<Ayah>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<Ayah>>> = _searchState.asStateFlow()

    private val _searchSurahFilter = MutableStateFlow<Int?>(null)
    val searchSurahFilter: StateFlow<Int?> = _searchSurahFilter.asStateFlow()

    private var detailJob: Job? = null
    private var searchJob: Job? = null
    private var lastSavedRead: String? = null
    private var lastSearchQuery = ""

    fun loadSurahDetail(surahNumber: Int) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _currentAyahsState.value = UiState.Loading
            val surah = getSurahDetailUseCase(surahNumber)
            if (surah == null) {
                _currentSurah.value = null
                _currentAyahsState.value = UiState.Error("Surah tidak ditemukan")
                return@launch
            }
            _currentSurah.value = surah

            getAyahsBySurahUseCase(surahNumber)
                .catch { _currentAyahsState.value = UiState.Error(it.localizedMessage ?: "Gagal memuat ayat") }
                .collect { ayahs ->
                    _currentAyahsState.value = if (ayahs.isEmpty()) UiState.Empty else UiState.Success(ayahs)
                }
        }
    }

    fun findFirstAyahByPage(page: Int, onResolved: (Ayah?) -> Unit) {
        if (page !in 1..604) {
            onResolved(null)
            return
        }
        viewModelScope.launch {
            onResolved(getFirstAyahByPageUseCase(page))
        }
    }

    fun findFirstAyahByJuz(juz: Int, onResolved: (Ayah?) -> Unit) {
        if (juz !in 1..30) {
            onResolved(null)
            return
        }
        viewModelScope.launch {
            onResolved(getFirstAyahByJuzUseCase(juz))
        }
    }

    fun onAyahVisible(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        juz: Int,
        page: Int
    ) {
        if (ayahNumber < 1 || lastSavedRead == "$surahNumber:$ayahNumber") return
        lastSavedRead = "$surahNumber:$ayahNumber"
        viewModelScope.launch {
            saveLastReadUseCase(surahNumber, surahName, ayahNumber, juz, page)
        }
    }

    fun toggleBookmark(ayah: Ayah, surahName: String, note: String? = null) {
        viewModelScope.launch {
            toggleBookmarkUseCase(
                surahNumber = ayah.surahNumber,
                surahName = surahName,
                ayahNumber = ayah.ayahNumber,
                textArabic = ayah.textArabic,
                translation = ayah.translationId,
                note = note
            )
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            deleteBookmarkUseCase(bookmark)
        }
    }

    fun restoreBookmark(bookmark: Bookmark) {
        viewModelScope.launch { restoreBookmarkUseCase(bookmark) }
    }

    fun updateBookmarkNote(bookmarkId: Long, note: String?) {
        viewModelScope.launch { updateBookmarkNoteUseCase(bookmarkId, note) }
    }

    fun setBookmarkSort(sort: BookmarkSort) {
        _bookmarkSort.value = sort
    }

    fun searchQuran(query: String) {
        searchJob?.cancel()
        lastSearchQuery = query
        if (query.isBlank()) {
            _searchState.value = UiState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            _searchState.value = UiState.Loading
            try {
                val results = searchQuranUseCase(query, _searchSurahFilter.value)
                _searchState.value = if (results.isEmpty()) UiState.Empty else UiState.Success(results)
            } catch (e: Exception) {
                _searchState.value = UiState.Error(e.localizedMessage ?: "Pencarian gagal")
            }
        }
    }

    fun clearSearch() {
        lastSearchQuery = ""
        _searchState.value = UiState.Idle
    }

    fun setSearchSurahFilter(surahNumber: Int?) {
        require(surahNumber == null || surahNumber in 1..114)
        _searchSurahFilter.value = surahNumber
        if (lastSearchQuery.isNotBlank()) searchQuran(lastSearchQuery)
    }
}
