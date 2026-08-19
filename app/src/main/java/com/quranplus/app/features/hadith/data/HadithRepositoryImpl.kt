package com.quranplus.app.features.hadith.data

import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithChapter
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.HadithSourceManifest
import com.quranplus.app.features.hadith.domain.HadithRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HadithRepositoryImpl(
    private val dao: HadithDao
) : HadithRepository {
    override fun getCollections(): Flow<List<HadithCollection>> =
        dao.getCollections().map { collections ->
            collections.map { collection ->
                HadithCollection(
                    id = collection.id,
                    title = collection.titleEnglish,
                    count = collection.recordCount,
                    titleArabic = collection.titleArabic,
                    sourceManifest = HadithSourceManifest(
                        collectionId = collection.id,
                        titleArabic = collection.titleArabic,
                        titleEnglish = collection.titleEnglish,
                        sourceRevision = collection.sourceRevision,
                        sourceSha256 = collection.sourceSha256,
                        licenseStatus = collection.licenseStatus,
                        gradeStatus = collection.gradeStatus,
                        recordCount = collection.recordCount,
                        chapterCount = collection.chapterCount,
                        isComplete = collection.isComplete,
                        bundleAllowed = collection.bundleAllowed
                    )
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
                translationEn = hadith.translationEn,
                reference = hadith.reference,
                chapterId = hadith.chapterId,
                sourceRevision = hadith.sourceRevision,
                sourceSha256 = hadith.sourceSha256,
                licenseStatus = hadith.licenseStatus,
                grade = hadith.grade,
                language = hadith.language,
                isComplete = hadith.isComplete
            )
        }
}
