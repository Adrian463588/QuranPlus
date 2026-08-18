package com.quranplus.app.features.quran.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.Bookmark
import com.quranplus.app.features.quran.domain.DeleteBookmarkUseCase
import com.quranplus.app.features.quran.domain.GetAyahsBySurahUseCase
import com.quranplus.app.features.quran.domain.GetBookmarksUseCase
import com.quranplus.app.features.quran.domain.GetLastReadUseCase
import com.quranplus.app.features.quran.domain.GetSurahDetailUseCase
import com.quranplus.app.features.quran.domain.GetSurahListUseCase
import com.quranplus.app.features.quran.domain.LastRead
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class QuranViewModel(
    private val getSurahListUseCase: GetSurahListUseCase,
    private val getSurahDetailUseCase: GetSurahDetailUseCase,
    private val getAyahsBySurahUseCase: GetAyahsBySurahUseCase,
    private val searchQuranUseCase: SearchQuranUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val saveLastReadUseCase: SaveLastReadUseCase,
    private val getLastReadUseCase: GetLastReadUseCase
) : ViewModel() {

    val surahListState: StateFlow<UiState<List<Surah>>> = getSurahListUseCase()
        .map<List<Surah>, UiState<List<Surah>>> { list ->
            if (list.isEmpty()) UiState.Loading else UiState.Success(list)
        }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Gagal memuat daftar surah")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val lastReadState: StateFlow<LastRead?> = getLastReadUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookmarksState: StateFlow<UiState<List<Bookmark>>> = getBookmarksUseCase()
        .map<List<Bookmark>, UiState<List<Bookmark>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Gagal memuat bookmark")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _currentSurah = MutableStateFlow<Surah?>(null)
    val currentSurah: StateFlow<Surah?> = _currentSurah.asStateFlow()

    private val _currentAyahsState = MutableStateFlow<UiState<List<Ayah>>>(UiState.Idle)
    val currentAyahsState: StateFlow<UiState<List<Ayah>>> = _currentAyahsState.asStateFlow()

    private val _searchState = MutableStateFlow<UiState<List<Ayah>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<Ayah>>> = _searchState.asStateFlow()

    fun loadSurahDetail(surahNumber: Int) {
        viewModelScope.launch {
            _currentAyahsState.value = UiState.Loading
            val surah = getSurahDetailUseCase(surahNumber)
            _currentSurah.value = surah

            getAyahsBySurahUseCase(surahNumber)
                .catch { _currentAyahsState.value = UiState.Error(it.localizedMessage ?: "Gagal memuat ayat") }
                .collect { ayahs ->
                    _currentAyahsState.value = UiState.Success(ayahs)
                    if (surah != null && ayahs.isNotEmpty()) {
                        saveLastReadUseCase(surahNumber, surah.nameLatin, 1)
                    }
                }
        }
    }

    fun onAyahVisible(surahNumber: Int, surahName: String, ayahNumber: Int) {
        viewModelScope.launch {
            saveLastReadUseCase(surahNumber, surahName, ayahNumber)
        }
    }

    fun toggleBookmark(ayah: Ayah, surahName: String) {
        viewModelScope.launch {
            toggleBookmarkUseCase(
                surahNumber = ayah.surahNumber,
                surahName = surahName,
                ayahNumber = ayah.ayahNumber,
                textArabic = ayah.textArabic,
                translation = ayah.translationId
            )
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            deleteBookmarkUseCase(bookmarkId)
        }
    }

    fun searchQuran(query: String) {
        if (query.isBlank()) {
            _searchState.value = UiState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            try {
                val results = searchQuranUseCase(query)
                _searchState.value = UiState.Success(results)
            } catch (e: Exception) {
                _searchState.value = UiState.Error(e.localizedMessage ?: "Pencarian gagal")
            }
        }
    }

    fun clearSearch() {
        _searchState.value = UiState.Idle
    }
}
