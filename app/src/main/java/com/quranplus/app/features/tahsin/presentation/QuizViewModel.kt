package com.quranplus.app.features.tahsin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.tahsin.domain.GetQuizQuestionsUseCase
import com.quranplus.app.features.tahsin.domain.QuizQuestion
import com.quranplus.app.features.tahsin.domain.RecordQuizAttemptUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data object Empty : QuizUiState
    data class Success(val questions: List<QuizQuestion>) : QuizUiState
    data class Error(val message: String) : QuizUiState
}

class QuizViewModel(
    getQuizQuestionsUseCase: GetQuizQuestionsUseCase,
    private val recordQuizAttemptUseCase: RecordQuizAttemptUseCase
) : ViewModel() {

    val uiState: StateFlow<QuizUiState> = getQuizQuestionsUseCase()
        .map { questions ->
            if (questions.isEmpty()) QuizUiState.Empty else QuizUiState.Success(questions)
        }
        .catch { emit(QuizUiState.Error(it.localizedMessage ?: "Bank soal tidak dapat dimuat")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizUiState.Loading)

    fun recordAttempt(questionId: Int, selectedIndex: Int, isCorrect: Boolean) {
        viewModelScope.launch {
            recordQuizAttemptUseCase(questionId, selectedIndex, isCorrect)
        }
    }
}
