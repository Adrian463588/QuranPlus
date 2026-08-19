package com.quranplus.app.features.tahsin.domain

class GetQuizQuestionsUseCase(private val repository: QuizRepository) {
    operator fun invoke() = repository.getQuestions()
}

class RecordQuizAttemptUseCase(private val repository: QuizRepository) {
    suspend operator fun invoke(questionId: Int, selectedIndex: Int, isCorrect: Boolean) {
        repository.recordAttempt(questionId, selectedIndex, isCorrect)
    }
}
