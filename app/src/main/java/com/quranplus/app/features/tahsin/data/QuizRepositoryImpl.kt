package com.quranplus.app.features.tahsin.data

import com.quranplus.app.core.database.dao.QuizDao
import com.quranplus.app.core.database.entity.QuizAttemptEntity
import com.quranplus.app.features.tahsin.domain.QuizQuestion
import com.quranplus.app.features.tahsin.domain.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class QuizRepositoryImpl(private val quizDao: QuizDao) : QuizRepository {
    override fun getQuestions(): Flow<List<QuizQuestion>> = quizDao.getQuestions().map { entities ->
        entities.mapNotNull { entity ->
            val options = runCatching {
                val json = JSONArray(entity.optionsJson)
                List(json.length()) { index -> json.getString(index) }
            }.getOrNull()
            if (options.isNullOrEmpty() || entity.correctIndex !in options.indices) {
                null
            } else {
                QuizQuestion(
                    id = entity.id,
                    prompt = entity.prompt,
                    arabicSnippet = entity.arabicSnippet,
                    reference = entity.reference,
                    options = options,
                    correctIndex = entity.correctIndex,
                    explanation = entity.explanation,
                    sourceId = entity.sourceId,
                    sourceRevision = entity.sourceRevision
                )
            }
        }
    }

    override suspend fun recordAttempt(questionId: Int, selectedIndex: Int, isCorrect: Boolean) {
        quizDao.insertAttempt(
            QuizAttemptEntity(
                questionId = questionId,
                selectedIndex = selectedIndex,
                isCorrect = isCorrect
            )
        )
    }
}
