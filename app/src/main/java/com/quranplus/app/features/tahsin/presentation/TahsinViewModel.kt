package com.quranplus.app.features.tahsin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.quran.presentation.UiState
import com.quranplus.app.features.tahsin.domain.GetTahsinLessonByIdUseCase
import com.quranplus.app.features.tahsin.domain.GetTahsinLessonsUseCase
import com.quranplus.app.features.tahsin.domain.TahsinCategory
import com.quranplus.app.features.tahsin.domain.TahsinLesson
import com.quranplus.app.features.tahsin.domain.UpdateTahsinProgressUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TahsinViewModel(
    private val getTahsinLessonsUseCase: GetTahsinLessonsUseCase,
    private val getTahsinLessonByIdUseCase: GetTahsinLessonByIdUseCase,
    private val updateTahsinProgressUseCase: UpdateTahsinProgressUseCase
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(TahsinCategory.MAKHARIJ)
    val selectedCategory: StateFlow<TahsinCategory> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val lessonsState: StateFlow<UiState<List<TahsinLesson>>> = _selectedCategory
        .flatMapLatest { category ->
            getTahsinLessonsUseCase(category)
                .map<List<TahsinLesson>, UiState<List<TahsinLesson>>> { list -> UiState.Success(list) }
                .catch { emit(UiState.Error(it.localizedMessage ?: "Gagal memuat materi tahsin")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _selectedLesson = MutableStateFlow<TahsinLesson?>(null)
    val selectedLesson: StateFlow<TahsinLesson?> = _selectedLesson.asStateFlow()

    fun selectCategory(category: TahsinCategory) {
        _selectedCategory.value = category
    }

    fun loadLessonDetail(lessonId: Int) {
        viewModelScope.launch {
            _selectedLesson.value = getTahsinLessonByIdUseCase(lessonId)
        }
    }

    fun toggleLessonCompleted(lesson: TahsinLesson) {
        viewModelScope.launch {
            val newStatus = !lesson.isCompleted
            updateTahsinProgressUseCase(lesson.id, newStatus)
            _selectedLesson.value = _selectedLesson.value?.copy(isCompleted = newStatus)
        }
    }
}
