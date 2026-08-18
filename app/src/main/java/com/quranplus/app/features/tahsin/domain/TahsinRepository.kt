package com.quranplus.app.features.tahsin.domain

import kotlinx.coroutines.flow.Flow

interface TahsinRepository {
    fun getAllLessons(): Flow<List<TahsinLesson>>
    fun getLessonsByCategory(category: TahsinCategory): Flow<List<TahsinLesson>>
    suspend fun getLessonById(id: Int): TahsinLesson?
    suspend fun updateLessonProgress(id: Int, isCompleted: Boolean)
}
