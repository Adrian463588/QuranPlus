package com.quranplus.app.features.hadith.data

import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithChapter
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.HadithRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class HadithRepositoryImpl(
    private val dao: HadithDao
) : HadithRepository {
    override fun getCollections(): Flow<List<HadithCollection>> =
        combine(dao.getCollections(), dao.getCollectionIds()) { collections, idsWithContent ->
            collections.map { collection ->
                HadithCollection(
                    id = collection.id,
                    title = displayTitle(collection.id, collection.titleEnglish),
                    count = collection.recordCount,
                    titleArabic = collection.titleArabic,
                    hasLocalContent = collection.id in idsWithContent
                )
            }
        }

    override fun getChapters(collectionId: String): Flow<List<HadithChapter>> =
        dao.getChapters(collectionId).map { chapters ->
            chapters.map { chapter ->
                HadithChapter(
                    collectionId = chapter.collectionId,
                    id = chapter.chapterId,
                    number = chapter.chapterNumber,
                    titleArabic = chapter.titleArabic,
                    titleEnglish = chapter.titleEnglish
                )
            }
        }

    override suspend fun search(collectionId: String?, query: String): List<HadithRecord> =
        dao.search(collectionId, query).map { hadith ->
            HadithRecord(
                id = hadith.id,
                collectionId = hadith.collectionId,
                hadithNumber = hadith.hadithNumber,
                title = hadith.title,
                textArabic = hadith.textArabic,
                translationId = hadith.translationId,
                translationEn = hadith.translationEn,
                reference = hadith.reference,
                chapterId = hadith.chapterId
            )
        }

    private fun displayTitle(id: String, sourceTitle: String): String = when (id) {
        "abudawud" -> "Sunan Abu Dawud"
        else -> sourceTitle
    }
}
