package com.quranplus.app.features.hadith.domain

data class HadithCollection(
    val id: String,
    val title: String,
    val count: Int,
    val titleArabic: String = "",
    val sourceManifest: HadithSourceManifest? = null
)

data class HadithChapter(
    val collectionId: String,
    val id: String,
    val number: Int,
    val titleArabic: String,
    val titleEnglish: String
)

data class HadithSourceManifest(
    val collectionId: String,
    val titleArabic: String,
    val titleEnglish: String,
    val sourceRevision: String,
    val sourceSha256: String,
    val licenseStatus: String,
    val gradeStatus: String,
    val recordCount: Int,
    val chapterCount: Int,
    val isComplete: Boolean,
    val bundleAllowed: Boolean
)

data class HadithRecord(
    val id: Long,
    val collectionId: String,
    val hadithNumber: Int,
    val title: String,
    val textArabic: String,
    val translationEn: String,
    val reference: String,
    val chapterId: String?,
    val sourceRevision: String,
    val sourceSha256: String,
    val licenseStatus: String,
    val grade: String?,
    val language: String,
    val isComplete: Boolean
)

interface HadithRepository {
    fun getCollections(): kotlinx.coroutines.flow.Flow<List<HadithCollection>>
    fun getChapters(collectionId: String): kotlinx.coroutines.flow.Flow<List<HadithChapter>>
    suspend fun search(collectionId: String?, query: String): List<HadithRecord>
}

class GetHadithCollectionsUseCase(private val repository: HadithRepository) {
    operator fun invoke() = repository.getCollections()
}

class SearchHadithUseCase(private val repository: HadithRepository) {
    suspend operator fun invoke(collectionId: String?, query: String): List<HadithRecord> =
        repository.search(collectionId, query.trim())
}
