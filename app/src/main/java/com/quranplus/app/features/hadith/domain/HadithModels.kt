package com.quranplus.app.features.hadith.domain

import kotlinx.coroutines.flow.Flow

enum class HadithNumberingScheme(
    val schemeId: String,
    val label: String
) {
    SUNNAH("sunnah", "No. Sunnah.com"),
    AR_RISALAH("ar_risalah", "No. Ar-Risalah"),
    ARCHIVE_GADING("archive_gading", "No. Arsip");

    companion object {
        fun fromSchemeId(schemeId: String?): HadithNumberingScheme? =
            entries.firstOrNull { it.schemeId.equals(schemeId?.trim(), ignoreCase = true) }

        fun defaultForCollection(collectionId: String?): HadithNumberingScheme = when (collectionId?.lowercase()) {
            "ahmad" -> AR_RISALAH
            "darimi" -> ARCHIVE_GADING
            else -> SUNNAH
        }

        fun schemesForCollection(collectionId: String?): List<HadithNumberingScheme> = when (collectionId?.lowercase()) {
            null -> listOf(SUNNAH, AR_RISALAH, ARCHIVE_GADING)
            "ahmad" -> listOf(AR_RISALAH, ARCHIVE_GADING)
            "darimi" -> listOf(ARCHIVE_GADING)
            else -> listOf(SUNNAH, ARCHIVE_GADING)
        }
    }
}

data class HadithSchemeRange(
    val scheme: HadithNumberingScheme,
    val label: String,
    val recordCount: Int,
    val minReferenceNumber: Int,
    val maxReferenceNumber: Int,
    val missingSlotCount: Int = 0
)

enum class HadithLookupStatus {
    FOUND,
    RECORD_MISSING,
    REFERENCE_UNMAPPED,
    CORRUPT,
    PARTIAL,
    NOT_INSTALLED,
    NO_MATCH
}

data class HadithGrade(
    val authority: String,
    val grade: String
)

data class HadithReferenceAlias(
    val scheme: HadithNumberingScheme,
    val number: Int,
    val variantSuffix: String = "",
    val bookNumber: Int = 0,
    val hadithInBook: Int = 0,
    val canonicalUrl: String? = null
)

data class HadithErrorReport(
    val status: HadithLookupStatus,
    val collectionId: String?,
    val scheme: HadithNumberingScheme,
    val query: String,
    val diagnostics: String
)

data class HadithSearchResult(
    val status: HadithLookupStatus,
    val records: List<HadithRecord>,
    val scheme: HadithNumberingScheme,
    val message: String? = null,
    val errorReport: HadithErrorReport? = null
)

data class HadithCollection(
    val id: String,
    val title: String,
    val count: Int,
    val titleArabic: String = "",
    val hasLocalContent: Boolean = true,
    val defaultScheme: HadithNumberingScheme = HadithNumberingScheme.defaultForCollection(id),
    val availabilityStatus: String = "AVAILABLE",
    val schemeRanges: List<HadithSchemeRange> = emptyList()
) {
    fun availableSchemes(): List<HadithNumberingScheme> =
        if (schemeRanges.isNotEmpty()) {
            schemeRanges.map { it.scheme }.distinct()
        } else {
            HadithNumberingScheme.schemesForCollection(id)
        }

    fun rangeFor(scheme: HadithNumberingScheme): HadithSchemeRange? =
        schemeRanges.firstOrNull { it.scheme == scheme }
            ?: schemeRanges.firstOrNull { it.scheme == defaultScheme }

    fun minReferenceNumber(scheme: HadithNumberingScheme = defaultScheme): Int =
        rangeFor(scheme)?.minReferenceNumber ?: if (count > 0) 1 else 0

    fun maxReferenceNumber(scheme: HadithNumberingScheme = defaultScheme): Int =
        rangeFor(scheme)?.maxReferenceNumber ?: count

    fun missingSlotCount(scheme: HadithNumberingScheme = defaultScheme): Int =
        rangeFor(scheme)?.missingSlotCount ?: 0

    fun activeCountForScheme(scheme: HadithNumberingScheme = defaultScheme): Int =
        rangeFor(scheme)?.recordCount ?: count
}

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
    val chapterId: String?,
    val referenceNumber: Int? = null,
    val numberLabel: String = "No. $hadithNumber (nomor arsip)",
    val stableId: String = "",
    val schemeId: String = "sunnah",
    val variantSuffix: String = "",
    val bookNumber: Int = 0,
    val hadithInBook: Int = 0,
    val chapterTitle: String = "",
    val canonicalUrl: String? = null,
    val grades: List<HadithGrade> = emptyList(),
    val alternateReferences: List<HadithReferenceAlias> = emptyList(),
    val verificationStatus: String = "VERIFIED",
    val indonesianAvailable: Boolean = translationId.isNotBlank()
)

interface HadithRepository {
    fun getCollections(): Flow<List<HadithCollection>>
    fun getChapters(collectionId: String): Flow<List<HadithChapter>>
    suspend fun search(collectionId: String?, query: String): List<HadithRecord>
    suspend fun searchDetailed(
        collectionId: String?,
        query: String,
        scheme: HadithNumberingScheme = HadithNumberingScheme.SUNNAH
    ): HadithSearchResult = HadithSearchResult(
        status = HadithLookupStatus.NO_MATCH,
        records = emptyList(),
        scheme = scheme
    )

    suspend fun getByReference(
        collectionId: String,
        number: Int,
        scheme: HadithNumberingScheme = HadithNumberingScheme.SUNNAH,
        variant: String? = null
    ): HadithRecord? = null

    suspend fun getReferenceWindow(
        collectionId: String,
        targetNumber: Int,
        scheme: HadithNumberingScheme = HadithNumberingScheme.defaultForCollection(collectionId),
        windowRadius: Int = 35
    ): List<HadithRecord> = emptyList()

    suspend fun browseSlice(
        collectionId: String,
        scheme: HadithNumberingScheme = HadithNumberingScheme.defaultForCollection(collectionId),
        limit: Int = 200,
        offset: Int = 0
    ): List<HadithRecord> = search(collectionId, "").drop(offset).take(limit)
}

class GetHadithCollectionsUseCase(private val repository: HadithRepository) {
    operator fun invoke() = repository.getCollections()
}

class SearchHadithUseCase(private val repository: HadithRepository) {
    suspend operator fun invoke(collectionId: String?, query: String): List<HadithRecord> =
        repository.search(collectionId, query.trim())

    suspend fun searchDetailed(
        collectionId: String?,
        query: String,
        scheme: HadithNumberingScheme = HadithNumberingScheme.SUNNAH
    ): HadithSearchResult = repository.searchDetailed(collectionId, query.trim(), scheme)

    suspend fun getReferenceWindow(
        collectionId: String,
        targetNumber: Int,
        scheme: HadithNumberingScheme,
        windowRadius: Int = 35
    ): List<HadithRecord> = repository.getReferenceWindow(collectionId, targetNumber, scheme, windowRadius)

    suspend fun browseSlice(
        collectionId: String,
        scheme: HadithNumberingScheme,
        limit: Int = 200,
        offset: Int = 0
    ): List<HadithRecord> = repository.browseSlice(collectionId, scheme, limit, offset)
}
