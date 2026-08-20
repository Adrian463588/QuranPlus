package com.quranplus.app.features.hadith.domain

data class HadithCollection(
    val id: String,
    val title: String,
    val count: Int,
    val titleArabic: String = "",
    val hasLocalContent: Boolean = true
)

enum class HadithCollectionSection(val title: String) {
    KUTUBUS_SITTAH("Kutubus Sittah"),
    OTHER("Hadis Lainnya")
}

private val kutubusSittahIds = listOf(
    "bukhari",
    "muslim",
    "abudawud",
    "tirmidhi",
    "nasai",
    "ibnmajah"
)

fun HadithCollection.section(): HadithCollectionSection =
    if (id in kutubusSittahIds) HadithCollectionSection.KUTUBUS_SITTAH
    else HadithCollectionSection.OTHER

fun sectionedHadithCollections(
    collections: List<HadithCollection>
): List<Pair<HadithCollectionSection, List<HadithCollection>>> =
    HadithCollectionSection.entries.mapNotNull { section ->
        val items = collections
            .filter { it.section() == section }
            .sortedWith(
                compareBy(
                    { kutubusSittahIds.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE },
                    { it.title }
                )
            )
        section.takeIf { items.isNotEmpty() }?.let { it to items }
    }

data class HadithChapter(
    val collectionId: String,
    val id: String,
    val number: Int,
    val titleArabic: String,
    val titleEnglish: String
)

data class HadithRecord(
    val id: Long,
    val collectionId: String,
    val hadithNumber: Int,
    val title: String,
    val textArabic: String,
    val translationId: String,
    val translationEn: String,
    val reference: String,
    val chapterId: String?
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
