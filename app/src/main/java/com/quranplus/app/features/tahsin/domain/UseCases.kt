package com.quranplus.app.features.tahsin.domain

import kotlinx.coroutines.flow.Flow

class GetTahsinLessonsUseCase(private val repository: TahsinRepository) {
    operator fun invoke(category: TahsinCategory? = null): Flow<List<TahsinLesson>> {
        return if (category != null) {
            repository.getLessonsByCategory(category)
        } else {
            repository.getAllLessons()
        }
    }
}

class GetTahsinLessonByIdUseCase(private val repository: TahsinRepository) {
    suspend operator fun invoke(id: Int): TahsinLesson? = repository.getLessonById(id)
}

class UpdateTahsinProgressUseCase(private val repository: TahsinRepository) {
    suspend operator fun invoke(id: Int, isCompleted: Boolean) = repository.updateLessonProgress(id, isCompleted)
}
