package com.quranplus.app.features.tahsin.data

import com.quranplus.app.core.database.dao.TahsinDao
import com.quranplus.app.core.database.entity.TahsinLessonEntity
import com.quranplus.app.features.tahsin.domain.TahsinCategory
import com.quranplus.app.features.tahsin.domain.TahsinLesson
import com.quranplus.app.features.tahsin.domain.TahsinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TahsinRepositoryImpl(
    private val tahsinDao: TahsinDao
) : TahsinRepository {

    override fun getAllLessons(): Flow<List<TahsinLesson>> {
        return tahsinDao.getAllLessons().map { it.map { entity -> entity.toDomain() } }
    }

    override fun getLessonsByCategory(category: TahsinCategory): Flow<List<TahsinLesson>> {
        return tahsinDao.getLessonsByCategory(category.id).map { it.map { entity -> entity.toDomain() } }
    }

    override suspend fun getLessonById(id: Int): TahsinLesson? {
        return tahsinDao.getLessonById(id)?.toDomain()
    }

    override suspend fun updateLessonProgress(id: Int, isCompleted: Boolean) {
        tahsinDao.updateLessonProgress(id, isCompleted)
    }

    private fun TahsinLessonEntity.toDomain(): TahsinLesson {
        val cat = runCatching { TahsinCategory.valueOf(category) }.getOrElse {
            throw IllegalStateException("Kategori Tahsin tidak dikenal: $category", it)
        }
        return TahsinLesson(
            id = id,
            category = cat,
            subcategory = subcategory,
            title = title,
            letterArabic = letterArabic,
            letterLatin = letterLatin,
            description = description,
            articulationPoint = articulationPoint,
            audioSample = audioSample,
            exampleAyahText = exampleAyahText,
            exampleAyahRef = exampleAyahRef,
            ruleType = ruleType,
            orderIndex = orderIndex,
            isCompleted = isCompleted
        )
    }
}
