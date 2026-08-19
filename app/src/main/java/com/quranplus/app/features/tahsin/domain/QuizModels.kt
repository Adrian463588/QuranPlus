package com.quranplus.app.features.tahsin.domain

data class QuizQuestion(
    val id: Int,
    val prompt: String,
    val arabicSnippet: String,
    val reference: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sourceId: String,
    val sourceRevision: String
)

interface QuizRepository {
    fun getQuestions(): kotlinx.coroutines.flow.Flow<List<QuizQuestion>>
    suspend fun recordAttempt(questionId: Int, selectedIndex: Int, isCorrect: Boolean)
}
