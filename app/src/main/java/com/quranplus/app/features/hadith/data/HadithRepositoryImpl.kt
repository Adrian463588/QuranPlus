package com.quranplus.app.features.hadith.data

import androidx.sqlite.db.SimpleSQLiteQuery
import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.core.database.entity.HadithGradeEntity
import com.quranplus.app.core.database.entity.HadithReferenceEntity
import com.quranplus.app.features.hadith.domain.HadithChapter
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithErrorReport
import com.quranplus.app.features.hadith.domain.HadithGrade
import com.quranplus.app.features.hadith.domain.HadithLookupStatus
import com.quranplus.app.features.hadith.domain.HadithNumberingScheme
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.HadithReferenceAlias
import com.quranplus.app.features.hadith.domain.HadithRepository
import com.quranplus.app.features.hadith.domain.HadithSchemeRange
import com.quranplus.app.features.hadith.domain.HadithSearchResult
import com.quranplus.app.features.rag.domain.HadithReferenceRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HadithRepositoryImpl(
    private val dao: HadithDao,
    private val numbering: HadithNumbering = HadithNumbering(),
    private val canonicalSynchronizer: CanonicalHadithSynchronizer? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HadithRepository {

    override fun getCollections(): Flow<List<HadithCollection>> =
        combine(
            dao.getCollections(),
            dao.getCollectionIds(),
            dao.getSchemeStats()
        ) { collections, idsWithContent, allStats ->
            val statsByCollection = allStats.groupBy { it.collectionId }
            collections.map { collection ->
                val count = if (collection.activeRecordCount > 0) {
                    collection.activeRecordCount
                } else {
                    collection.recordCount
                }
                val isAvailable = collection.id in idsWithContent &&
                    collection.availabilityStatus != CanonicalHadithSynchronizer.STATUS_NOT_INSTALLED
                val defaultScheme = HadithNumberingScheme.fromSchemeId(collection.defaultScheme)
                    ?: HadithNumberingScheme.defaultForCollection(collection.id)
                val ranges = statsByCollection[collection.id]
                    .orEmpty()
                    .mapNotNull { stat ->
                        val schemeEnum = HadithNumberingScheme.fromSchemeId(stat.schemeId)
                            ?: return@mapNotNull null
                        HadithSchemeRange(
                            scheme = schemeEnum,
                            label = schemeEnum.label,
                            recordCount = stat.recordCount,
                            minReferenceNumber = stat.minReferenceNumber,
                            maxReferenceNumber = stat.maxReferenceNumber,
                            missingSlotCount = stat.missingSlotCount
                        )
                    }
                    .sortedBy { range ->
                        when (range.scheme) {
                            defaultScheme -> 0
                            HadithNumberingScheme.SUNNAH -> 1
                            HadithNumberingScheme.AR_RISALAH -> 2
                            HadithNumberingScheme.ARCHIVE_GADING -> 3
                        }
                    }

                HadithCollection(
                    id = collection.id,
                    title = displayTitle(collection.id, collection.titleEnglish),
                    count = count,
                    titleArabic = collection.titleArabic,
                    hasLocalContent = isAvailable,
                    defaultScheme = defaultScheme,
                    availabilityStatus = if (isAvailable) {
                        collection.availabilityStatus.takeIf {
                            it == CanonicalHadithSynchronizer.STATUS_PARTIAL
                        } ?: CanonicalHadithSynchronizer.STATUS_AVAILABLE
                    } else {
                        collection.availabilityStatus
                    },
                    schemeRanges = ranges
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
        searchDetailed(collectionId, query, HadithNumberingScheme.defaultForCollection(collectionId)).records

    override suspend fun getByReference(
        collectionId: String,
        number: Int,
        scheme: HadithNumberingScheme,
        variant: String?
    ): HadithRecord? = withContext(ioDispatcher) {
        canonicalSynchronizer?.ensureSynchronized()
        val effectiveScheme = resolveEffectiveScheme(collectionId, "", scheme)
        val rows = dao.findByReference(collectionId, effectiveScheme.schemeId, number, variant, limit = 10)
            .ifEmpty {
                if (effectiveScheme == HadithNumberingScheme.SUNNAH && collectionId !in CanonicalHadithSynchronizer.CANONICAL_COLLECTION_IDS) {
                    searchReferenceNumberLegacy(collectionId, number)
                } else {
                    emptyList()
                }
            }
        enrich(rows, effectiveScheme).firstOrNull()
    }

    override suspend fun getReferenceWindow(
        collectionId: String,
        targetNumber: Int,
        scheme: HadithNumberingScheme,
        windowRadius: Int
    ): List<HadithRecord> = withContext(ioDispatcher) {
        canonicalSynchronizer?.ensureSynchronized()
        val minNum = (targetNumber - windowRadius).coerceAtLeast(1)
        val maxNum = targetNumber + windowRadius
        val entities = dao.getHadithsBySchemeSlice(
            collectionId = collectionId,
            schemeId = scheme.schemeId,
            minNumber = minNum,
            maxNumber = maxNum,
            limit = windowRadius * 2 + 25
        ).ifEmpty {
            dao.getHadithsByCollection(collectionId).firstOrNull().orEmpty()
        }
        enrich(entities, scheme)
    }

    override suspend fun browseSlice(
        collectionId: String,
        scheme: HadithNumberingScheme,
        limit: Int,
        offset: Int
    ): List<HadithRecord> = withContext(ioDispatcher) {
        canonicalSynchronizer?.ensureSynchronized()
        val entities = dao.getHadithsBySchemePaged(
            collectionId = collectionId,
            schemeId = scheme.schemeId,
            limit = limit,
            offset = offset
        ).ifEmpty {
            if (offset == 0) {
                dao.getHadithsByCollection(collectionId).firstOrNull().orEmpty().take(limit)
            } else {
                emptyList()
            }
        }
        enrich(entities, scheme)
    }

    override suspend fun searchDetailed(
        collectionId: String?,
        query: String,
        scheme: HadithNumberingScheme
    ): HadithSearchResult = withContext(ioDispatcher) {
        canonicalSynchronizer?.ensureSynchronized()

        val trimmed = query.trim()
        val effectiveScheme = resolveEffectiveScheme(collectionId, trimmed, scheme)

        val explicitColRef = parseCollectionPrefixedQuery(trimmed)
        if (explicitColRef != null && (collectionId == null || collectionId == explicitColRef.collection)) {
            val targetScheme = resolveEffectiveScheme(
                collectionId = explicitColRef.collection,
                trimmedQuery = trimmed,
                requestedScheme = HadithNumberingScheme.fromSchemeId(explicitColRef.targetSchemeId()) ?: scheme
            )
            val parsedExplicit = ParsedReferenceQuery(
                number = explicitColRef.number,
                variant = explicitColRef.suffix.ifBlank { null },
                rawDisplay = if (explicitColRef.suffix.isBlank()) {
                    explicitColRef.number.toString()
                } else {
                    "${explicitColRef.number}${explicitColRef.suffix}"
                }
            )
            return@withContext searchNumeric(explicitColRef.collection, parsedExplicit, targetScheme, trimmed)
        }

        val parsedRef = parseReferenceQuery(trimmed)
        if (trimmed.isBlank() && collectionId != null) {
            val records = browseSlice(collectionId, effectiveScheme, limit = INITIAL_BROWSE_LIMIT, offset = 0)
            if (records.isEmpty()) {
                return@withContext unavailableResult(collectionId, effectiveScheme, trimmed)
            }
            return@withContext HadithSearchResult(
                status = HadithLookupStatus.FOUND,
                records = records,
                scheme = effectiveScheme
            )
        }

        if (parsedRef != null) {
            return@withContext searchNumeric(collectionId, parsedRef, effectiveScheme, trimmed)
        }

        val expression = trimmed.toFtsExpression() ?: return@withContext HadithSearchResult(
            status = HadithLookupStatus.NO_MATCH,
            records = emptyList(),
            scheme = effectiveScheme,
            message = "Masukkan minimal 2 karakter atau nomor hadist yang valid."
        )

        val localEntities = dao.searchFts(
            SimpleSQLiteQuery(
                """
                SELECT h.* FROM hadiths AS h
                JOIN hadiths_fts5 ON h.id = hadiths_fts5.rowid
                WHERE (? IS NULL OR h.collection_id = ?)
                  AND hadiths_fts5 MATCH ?
                ORDER BY bm25(hadiths_fts5), h.hadith_number ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf<Any?>(collectionId, collectionId, expression, MAX_RESULTS)
            )
        )
        if (localEntities.isEmpty()) {
            return@withContext missingOrNoMatch(collectionId, effectiveScheme, trimmed)
        }
        HadithSearchResult(
            status = HadithLookupStatus.FOUND,
            records = enrich(localEntities, effectiveScheme),
            scheme = effectiveScheme
        )
    }

    private fun resolveEffectiveScheme(
        collectionId: String?,
        trimmedQuery: String,
        requestedScheme: HadithNumberingScheme
    ): HadithNumberingScheme {
        val allowed = HadithNumberingScheme.schemesForCollection(collectionId)
        return when {
            ARCHIVE_KEYWORD_REGEX.containsMatchIn(trimmedQuery) ->
                HadithNumberingScheme.ARCHIVE_GADING.takeIf { it in allowed } ?: requestedScheme
            RISALAH_KEYWORD_REGEX.containsMatchIn(trimmedQuery) && collectionId == "ahmad" ->
                HadithNumberingScheme.AR_RISALAH
            requestedScheme in allowed -> requestedScheme
            else -> HadithNumberingScheme.defaultForCollection(collectionId)
        }
    }

    private suspend fun searchNumeric(
        collectionId: String?,
        parsedRef: ParsedReferenceQuery,
        scheme: HadithNumberingScheme,
        rawQuery: String
    ): HadithSearchResult {
        if (collectionId != null && !isCollectionAvailable(collectionId)) {
            return unavailableResult(collectionId, scheme, rawQuery)
        }

        val initialMatches = dao.findByReference(
            collectionId = collectionId,
            schemeId = scheme.schemeId,
            number = parsedRef.number,
            variant = parsedRef.variant,
            limit = MAX_RESULTS
        )
        val directMatches = initialMatches.ifEmpty {
            val v = parsedRef.variant
            if (v != null && v.length == 1 && v[0] in 'a'..'z' &&
                scheme == HadithNumberingScheme.SUNNAH &&
                (collectionId == null || collectionId == "muslim")
            ) {
                val numericAlias = (v[0] - 'a' + 1).toString().padStart(2, '0')
                dao.findByReference(
                    collectionId = collectionId,
                    schemeId = scheme.schemeId,
                    number = parsedRef.number,
                    variant = numericAlias,
                    limit = MAX_RESULTS
                )
            } else {
                emptyList()
            }
        }.distinctBy { it.id }

        if (directMatches.isNotEmpty() || (collectionId == null && scheme == HadithNumberingScheme.SUNNAH &&
                !ARCHIVE_KEYWORD_REGEX.containsMatchIn(rawQuery) &&
                !RISALAH_KEYWORD_REGEX.containsMatchIn(rawQuery))
        ) {
            if (collectionId == null && scheme == HadithNumberingScheme.SUNNAH &&
                !ARCHIVE_KEYWORD_REGEX.containsMatchIn(rawQuery) &&
                !RISALAH_KEYWORD_REGEX.containsMatchIn(rawQuery)
            ) {
                val ahmadMatches = dao.findByReference(
                    collectionId = "ahmad",
                    schemeId = HadithNumberingScheme.AR_RISALAH.schemeId,
                    number = parsedRef.number,
                    variant = parsedRef.variant,
                    limit = MAX_RESULTS
                )
                val darimiMatches = dao.findByReference(
                    collectionId = "darimi",
                    schemeId = HadithNumberingScheme.ARCHIVE_GADING.schemeId,
                    number = parsedRef.number,
                    variant = parsedRef.variant,
                    limit = MAX_RESULTS
                )
                val combined = enrich(directMatches, scheme, preferredNumber = parsedRef.number) +
                    enrich(ahmadMatches, HadithNumberingScheme.AR_RISALAH, preferredNumber = parsedRef.number) +
                    enrich(darimiMatches, HadithNumberingScheme.ARCHIVE_GADING, preferredNumber = parsedRef.number)
                val deduped = combined.distinctBy { it.id }.take(MAX_RESULTS)
                if (deduped.isNotEmpty()) {
                    return HadithSearchResult(
                        status = HadithLookupStatus.FOUND,
                        records = deduped,
                        scheme = scheme
                    )
                }
            } else {
                return HadithSearchResult(
                    status = HadithLookupStatus.FOUND,
                    records = enrich(directMatches, scheme, preferredNumber = parsedRef.number),
                    scheme = scheme
                )
            }
        }

        // Check explicitly documented gaps/unmapped edition slots for this scheme
        val slotEntries = if (collectionId != null) {
            dao.getReferenceEntries(
                collectionId = collectionId,
                schemeId = scheme.schemeId,
                number = parsedRef.number
            )
        } else {
            emptyList()
        }
        val missingSlot = slotEntries.firstOrNull { it.hadithId == null }
        val siblingSchemes = findSiblingSchemesWithNumber(collectionId, scheme, parsedRef.number)
        val label = collectionId?.let(HadithReferenceImporter::displayTitle) ?: "Koleksi terpilih"

        if (missingSlot != null) {
            if (missingSlot.reviewStatus == "unmapped_edition_variant" && siblingSchemes.isNotEmpty()) {
                val alt = siblingSchemes.first()
                return HadithSearchResult(
                    status = HadithLookupStatus.REFERENCE_UNMAPPED,
                    records = emptyList(),
                    scheme = scheme,
                    message = "Nomor ${parsedRef.rawDisplay} pada skema ${scheme.label} belum dipetakan pada $label karena perbedaan struktur edisi. Beralih ke mode '${alt.label}' untuk melihat nomor pada edisi tersebut.",
                    errorReport = buildErrorReport(
                        status = HadithLookupStatus.REFERENCE_UNMAPPED,
                        collectionId = collectionId,
                        scheme = scheme,
                        query = rawQuery,
                        detail = "unmapped_edition_variant; alt=${alt.schemeId}"
                    )
                )
            }
            return HadithSearchResult(
                status = HadithLookupStatus.RECORD_MISSING,
                records = emptyList(),
                scheme = scheme,
                message = "Hadist $label no. ${parsedRef.rawDisplay} (${scheme.label}) merupakan celah penomoran/dihapus pada edisi sumber (bukan data rusak).",
                errorReport = buildErrorReport(
                    status = HadithLookupStatus.RECORD_MISSING,
                    collectionId = collectionId,
                    scheme = scheme,
                    query = rawQuery,
                    detail = "${missingSlot.reviewStatus} slot in canonical references"
                )
            )
        }

        if (siblingSchemes.isNotEmpty()) {
            val alt = siblingSchemes.first()
            return HadithSearchResult(
                status = HadithLookupStatus.REFERENCE_UNMAPPED,
                records = emptyList(),
                scheme = scheme,
                message = "Nomor ${parsedRef.rawDisplay} tidak terpetakan di $label pada mode ${scheme.label}. Beralih ke mode '${alt.label}' untuk membuka nomor edisi ${parsedRef.rawDisplay}.",
                errorReport = buildErrorReport(
                    status = HadithLookupStatus.REFERENCE_UNMAPPED,
                    collectionId = collectionId,
                    scheme = scheme,
                    query = rawQuery,
                    detail = "Unmapped in ${scheme.schemeId}; present in ${alt.schemeId}"
                )
            )
        }

        // Distinguish incomplete/partial local bundle from true missing record
        val collectionEntity = collectionId?.let { dao.getCollectionById(it) }
        val activeCount = collectionId?.let { dao.countActiveDistinctHadiths(it) } ?: 0
        val schemeStats = collectionId?.let { dao.getSchemeStatsForCollection(it) }.orEmpty()
        val statForScheme = schemeStats.firstOrNull { it.schemeId == scheme.schemeId }
        val isIncompleteLocalBundle = isCanonicalPartial() ||
            collectionEntity?.availabilityStatus == CanonicalHadithSynchronizer.STATUS_PARTIAL ||
            (collectionEntity != null && !collectionEntity.isComplete &&
                (statForScheme == null || activeCount < statForScheme.maxReferenceNumber))

        if (isIncompleteLocalBundle) {
            val maxKnown = statForScheme?.maxReferenceNumber ?: 0
            val rangeNote = if (maxKnown > 0) " (jangkauan ref no. ${statForScheme?.minReferenceNumber ?: 1}–$maxKnown)" else ""
            return HadithSearchResult(
                status = HadithLookupStatus.PARTIAL,
                records = emptyList(),
                scheme = scheme,
                message = "Bundel lokal $label belum lengkap ($activeCount hadist terpasang$rangeNote). Hadist no. ${parsedRef.rawDisplay} belum tersedia di bundel terpasang.",
                errorReport = buildErrorReport(
                    status = HadithLookupStatus.PARTIAL,
                    collectionId = collectionId,
                    scheme = scheme,
                    query = rawQuery,
                    detail = "Partial bundle ($activeCount active, maxRef=$maxKnown) for (${scheme.schemeId}, ${parsedRef.rawDisplay})"
                )
            )
        }

        return HadithSearchResult(
            status = HadithLookupStatus.RECORD_MISSING,
            records = emptyList(),
            scheme = scheme,
            message = "Hadist no. ${parsedRef.rawDisplay} tidak ditemukan di $label pada skema ${scheme.label}. Periksa nomor atau cari dengan kata kunci.",
            errorReport = buildErrorReport(
                status = HadithLookupStatus.RECORD_MISSING,
                collectionId = collectionId,
                scheme = scheme,
                query = rawQuery,
                detail = "No row for ($collectionId, ${scheme.schemeId}, ${parsedRef.rawDisplay})"
            )
        )
    }

    private suspend fun findSiblingSchemesWithNumber(
        collectionId: String?,
        excludedScheme: HadithNumberingScheme,
        number: Int
    ): List<HadithNumberingScheme> {
        val candidates = HadithNumberingScheme.schemesForCollection(collectionId)
            .filter { it != excludedScheme }
        val matches = mutableListOf<HadithNumberingScheme>()
        for (candidate in candidates) {
            val rows = dao.getReferenceEntries(collectionId, candidate.schemeId, number)
            if (rows.any { it.hadithId != null }) {
                matches.add(candidate)
            }
        }
        return matches
    }

    private suspend fun isCollectionAvailable(collectionId: String): Boolean {
        val count = dao.countActiveDistinctHadiths(collectionId)
        return count > 0
    }

    private suspend fun isCanonicalPartial(): Boolean {
        val manifest = dao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID)
        return manifest?.status == CanonicalHadithSynchronizer.STATUS_PARTIAL
    }

    private suspend fun unavailableResult(
        collectionId: String,
        scheme: HadithNumberingScheme,
        query: String
    ): HadithSearchResult {
        val canonicalManifest = dao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID)
        val archiveManifest = dao.getSourceManifest(HadithBundleImporter.BUNDLE_ID)
        val manifest = when {
            canonicalManifest?.status == CanonicalHadithSynchronizer.STATUS_CORRUPT ||
                canonicalManifest?.status == CanonicalHadithSynchronizer.STATUS_PARTIAL -> canonicalManifest
            archiveManifest?.status == CanonicalHadithSynchronizer.STATUS_CORRUPT ||
                archiveManifest?.status == CanonicalHadithSynchronizer.STATUS_PARTIAL -> archiveManifest
            else -> canonicalManifest ?: archiveManifest
        }
        val status = when (manifest?.status) {
            CanonicalHadithSynchronizer.STATUS_CORRUPT -> HadithLookupStatus.CORRUPT
            CanonicalHadithSynchronizer.STATUS_PARTIAL -> HadithLookupStatus.PARTIAL
            else -> HadithLookupStatus.NOT_INSTALLED
        }
        val title = HadithReferenceImporter.displayTitle(collectionId)
        val message = when (status) {
            HadithLookupStatus.CORRUPT ->
                "Data korpus $title gagal verifikasi SHA-256. Sinkronkan ulang korpus sebelum membaca."
            HadithLookupStatus.PARTIAL ->
                "Sinkronisasi $title baru selesai sebagian. Muat ulang korpus lokal."
            else ->
                "Korpus offline $title belum terpasang atau belum disinkronkan di perangkat."
        }
        return HadithSearchResult(
            status = status,
            records = emptyList(),
            scheme = scheme,
            message = message,
            errorReport = buildErrorReport(
                status = status,
                collectionId = collectionId,
                scheme = scheme,
                query = query,
                detail = "manifest=${manifest?.sourceId ?: CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID} status=${manifest?.status ?: "NONE"} active=${manifest?.activeRecordCount ?: 0}"
            )
        )
    }

    private suspend fun missingOrNoMatch(
        collectionId: String?,
        scheme: HadithNumberingScheme,
        query: String
    ): HadithSearchResult {
        if (collectionId != null && !isCollectionAvailable(collectionId)) {
            return unavailableResult(collectionId, scheme, query)
        }
        val manifest = dao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID)
        val total = dao.countActiveDistinctHadiths(collectionId)
        if (manifest?.status == CanonicalHadithSynchronizer.STATUS_CORRUPT && total == 0) {
            return HadithSearchResult(
                status = HadithLookupStatus.CORRUPT,
                records = emptyList(),
                scheme = scheme,
                message = "Data hadist gagal verifikasi checksum SHA-256.",
                errorReport = buildErrorReport(
                    status = HadithLookupStatus.CORRUPT,
                    collectionId = collectionId,
                    scheme = scheme,
                    query = query,
                    detail = "Canonical manifest marked CORRUPT"
                )
            )
        }
        return HadithSearchResult(
            status = HadithLookupStatus.NO_MATCH,
            records = emptyList(),
            scheme = scheme,
            message = "Tidak ada hadist yang cocok dengan \"$query\"."
        )
    }

    private suspend fun searchReferenceNumberLegacy(
        collectionId: String,
        number: Int
    ): List<HadithEntity> =
        if (numbering.hasReferenceNumbers(collectionId)) {
            numbering.archiveNumbers(collectionId, number).mapNotNull { archiveNumber ->
                dao.getHadithByNumber(collectionId, archiveNumber)
                    ?.takeIf { numbering.referenceNumber(it) == number }
            }
        } else {
            dao.searchByNumber(collectionId, number)
        }

    private suspend fun enrich(
        entities: List<HadithEntity>,
        requestedScheme: HadithNumberingScheme,
        preferredNumber: Int? = null
    ): List<HadithRecord> {
        if (entities.isEmpty()) return emptyList()
        val ids = entities.map { it.id }.distinct()
        val refsByHadith = LinkedHashMap<Long, MutableList<HadithReferenceEntity>>(ids.size)
        val gradesByHadith = LinkedHashMap<Long, MutableList<HadithGradeEntity>>(ids.size)
        ids.chunked(BATCH_QUERY_LIMIT).forEach { chunk ->
            dao.getReferencesForHadiths(chunk).forEach { ref ->
                val hid = ref.hadithId ?: return@forEach
                refsByHadith.getOrPut(hid) { mutableListOf() }.add(ref)
            }
            dao.getGradesForHadiths(chunk).forEach { grade ->
                gradesByHadith.getOrPut(grade.hadithId) { mutableListOf() }.add(grade)
            }
        }

        return entities.map { entity ->
            val refs = refsByHadith[entity.id].orEmpty()
            val grades = gradesByHadith[entity.id].orEmpty().map { HadithGrade(it.authority, it.grade) }
            entity.toRecord(requestedScheme, refs, grades, preferredNumber)
        }
    }

    private fun HadithEntity.toRecord(
        requestedScheme: HadithNumberingScheme,
        refs: List<HadithReferenceEntity>,
        gradesList: List<HadithGrade>,
        preferredNumber: Int? = null
    ): HadithRecord {
        val activeRef = preferredNumber?.let { target ->
            refs.firstOrNull { it.schemeId == requestedScheme.schemeId && it.referenceNumber == target }
        } ?: refs.firstOrNull { it.schemeId == requestedScheme.schemeId }
        val sunnahRef = refs.firstOrNull { it.schemeId == CanonicalHadithSynchronizer.SCHEME_SUNNAH }
        val risalahRef = refs.firstOrNull { it.schemeId == CanonicalHadithSynchronizer.SCHEME_AR_RISALAH }
        val archiveRef = refs.firstOrNull { it.schemeId == CanonicalHadithSynchronizer.SCHEME_ARCHIVE_GADING }
        val primaryRef = activeRef ?: sunnahRef ?: risalahRef ?: archiveRef

        val activeNum = activeRef?.referenceNumber ?: when {
            requestedScheme == HadithNumberingScheme.ARCHIVE_GADING && archiveRef != null -> archiveRef.referenceNumber
            requestedScheme == HadithNumberingScheme.AR_RISALAH && risalahRef != null -> risalahRef.referenceNumber
            requestedScheme == HadithNumberingScheme.SUNNAH && sunnahRef != null -> sunnahRef.referenceNumber
            else -> hadithNumber
        }
        val activeVariant = activeRef?.variantSuffix.orEmpty()
        val formattedActive = formatWithVariant(activeNum, activeVariant)

        val canonicalNum = sunnahRef?.referenceNumber
            ?: risalahRef?.referenceNumber
            ?: if (licenseStatus in CanonicalHadithSynchronizer.CANONICAL_ACCEPTED_LICENSES) hadithNumber
            else numbering.referenceNumber(this)
        val canonVariant = sunnahRef?.variantSuffix ?: risalahRef?.variantSuffix.orEmpty()
        val formattedCanon = canonicalNum?.let { num -> formatWithVariant(num, canonVariant) }

        val schemeBadgeText = when (requestedScheme) {
            HadithNumberingScheme.SUNNAH -> "Sunnah.com"
            HadithNumberingScheme.AR_RISALAH -> "Ar-Risalah"
            HadithNumberingScheme.ARCHIVE_GADING ->
                if (collectionId in setOf("ahmad", "darimi")) "Al-Alamiyah" else "Arsip"
        }

        val isBundled = licenseStatus in CanonicalHadithSynchronizer.CANONICAL_ACCEPTED_LICENSES
        val displayNumberLabel = if (refs.isEmpty() && !isBundled) {
            numbering.numberLabel(this)
        } else {
            "No. $formattedActive ($schemeBadgeText)"
        }

        val resolvedCanonicalUrl = when {
            requestedScheme == HadithNumberingScheme.ARCHIVE_GADING -> sunnahRef?.canonicalUrl
            activeRef?.canonicalUrl != null -> activeRef.canonicalUrl
            schemeId == requestedScheme.schemeId -> canonicalUrl
            sunnahRef?.canonicalUrl != null && requestedScheme == HadithNumberingScheme.SUNNAH -> sunnahRef.canonicalUrl
            !isBundled -> numbering.canonicalUrl(this)
            else -> null
        }

        val bookNo = primaryRef?.bookNumber ?: 0
        val inBookNo = primaryRef?.hadithInBook ?: 0
        val aliases = refs.mapNotNull { ref ->
            val schemeEnum = HadithNumberingScheme.fromSchemeId(ref.schemeId) ?: return@mapNotNull null
            HadithReferenceAlias(
                scheme = schemeEnum,
                number = ref.referenceNumber,
                variantSuffix = ref.variantSuffix,
                bookNumber = ref.bookNumber,
                hadithInBook = ref.hadithInBook,
                canonicalUrl = ref.canonicalUrl
            )
        }

        val resolvedGrades = gradesList.ifEmpty {
            grade?.split(";")?.mapNotNull { segment ->
                val parts = segment.split(":", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    HadithGrade(parts[0].trim(), parts[1].trim())
                } else null
            }.orEmpty()
        }

        val indonesianText = if (isBundled) {
            translationId
        } else {
            numbering.indonesianTranslation(this)
        }
        val formattedReference = buildString {
            val collectionTitle = HadithReferenceImporter.displayTitle(collectionId)
            append(collectionTitle)
            append(" No. ").append(formattedActive)
            append(" (").append(schemeBadgeText)
            if (bookNo > 0 && inBookNo > 0) {
                append(" • Kitab ").append(bookNo).append(", Hadis ").append(inBookNo)
            }
            append(')')
            aliases.filter { it.scheme != requestedScheme }.forEach { alias ->
                val altTag = when (alias.scheme) {
                    HadithNumberingScheme.ARCHIVE_GADING ->
                        if (collectionId in setOf("ahmad", "darimi")) "Al-Alamiyah" else "Arsip"
                    HadithNumberingScheme.AR_RISALAH -> "Ar-Risalah"
                    HadithNumberingScheme.SUNNAH -> "Sunnah.com"
                }
                append(" • ").append(altTag).append(" no. ").append(formatWithVariant(alias.number, alias.variantSuffix))
            }
        }

        return HadithRecord(
            id = id,
            collectionId = collectionId,
            hadithNumber = activeNum,
            title = title,
            textArabic = textArabic,
            translationId = indonesianText,
            translationEn = translationEn,
            reference = if (refs.isEmpty() && !isBundled) {
                numbering.reference(this)
            } else {
                formattedReference
            },
            chapterId = chapterId,
            referenceNumber = sunnahRef?.referenceNumber ?: activeRef?.referenceNumber ?: canonicalNum,
            numberLabel = displayNumberLabel,
            stableId = stableId.ifBlank { "${requestedScheme.schemeId}:$collectionId:$activeNum" },
            schemeId = requestedScheme.schemeId,
            variantSuffix = activeVariant,
            bookNumber = bookNo,
            hadithInBook = inBookNo,
            chapterTitle = title,
            canonicalUrl = resolvedCanonicalUrl,
            grades = resolvedGrades,
            alternateReferences = aliases,
            verificationStatus = verificationStatus,
            indonesianAvailable = indonesianText.isNotBlank()
        )
    }

    private fun formatWithVariant(number: Int, variant: String): String =
        when {
            variant.isBlank() -> number.toString()
            variant.first().isDigit() -> "$number.$variant"
            else -> "$number$variant"
        }

    private fun parseCollectionPrefixedQuery(trimmed: String): HadithReferenceRequest? {
        val strippedPrefix = COLLECTION_LEAD_IN_REGEX.replace(trimmed, "").trim()
        val match = HadithReferenceRequest.findAll(strippedPrefix).singleOrNull() ?: return null
        val tailCheck = COLLECTION_SUFFIX_NOISE_REGEX.replace(strippedPrefix, "").trim()
        val tokens = tailCheck.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        // Only treat as pure reference query when the input doesn't append extra prose/keywords
        return match.takeIf { tokens.size <= 5 }
    }

    private fun parseReferenceQuery(trimmed: String): ParsedReferenceQuery? {
        val match = NUMERIC_REFERENCE_REGEX.matchEntire(trimmed) ?: return null
        val number = match.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val variant = (match.groupValues[2].ifBlank { match.groupValues[3] }).lowercase().ifBlank { null }
        val display = variant?.let { formatWithVariant(number, it) } ?: number.toString()
        return ParsedReferenceQuery(number = number, variant = variant, rawDisplay = display)
    }

    private fun buildErrorReport(
        status: HadithLookupStatus,
        collectionId: String?,
        scheme: HadithNumberingScheme,
        query: String,
        detail: String
    ): HadithErrorReport = HadithErrorReport(
        status = status,
        collectionId = collectionId,
        scheme = scheme,
        query = query,
        diagnostics = "status=${status.name}; collection=${collectionId ?: "all"}; scheme=${scheme.schemeId}; query=$query; detail=$detail"
    )

    private fun String.toFtsExpression(): String? {
        val normalized = normalizeArabicForQuery(lowercase())
        val tokens = Regex("[\\p{L}\\p{N}]+")
            .findAll(normalized)
            .map { it.value }
            .filter { it.length >= 2 }
            .distinct()
            .take(MAX_QUERY_TOKENS)
            .toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { token ->
            "\"${token.replace("\"", "\"\"")}\"*"
        }
    }

    private fun normalizeArabicForQuery(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val code = ch.code
            when {
                code == 1600 -> Unit
                code in 1611..1648 || code in 1750..1773 -> Unit
                code == 1649 || code == 1570 || code == 1571 || code == 1573 -> sb.append('\u0627')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun displayTitle(id: String, sourceTitle: String): String = when (id) {
        "abudawud" -> "Sunan Abu Dawud"
        else -> sourceTitle
    }

    private data class ParsedReferenceQuery(
        val number: Int,
        val variant: String?,
        val rawDisplay: String
    )

    private companion object {
        const val INITIAL_BROWSE_LIMIT = 200
        const val MAX_RESULTS = 100
        const val MAX_QUERY_TOKENS = 12
        const val BATCH_QUERY_LIMIT = 450
        val ARCHIVE_KEYWORD_REGEX = Regex("(?i)\\b(arsip|gading|tafsirq|(?:al[ -]*)?alamiyah)\\b")
        val RISALAH_KEYWORD_REGEX = Regex("(?i)\\b((?:ar[ -]*)?risalah|arna.?ut)\\b")
        val COLLECTION_LEAD_IN_REGEX = Regex("(?i)^(?:hr\\.?|hadit[hs]?|hadis|hadist)\\s+")
        val COLLECTION_SUFFIX_NOISE_REGEX = Regex("[():#]")
        val WHITESPACE_REGEX = Regex("\\s+")
        val NUMERIC_REFERENCE_REGEX = Regex(
            "(?i)^(?:(?:arsip|gading|tafsirq|(?:al[ -]*)?alamiyah|(?:ar[ -]*)?risalah|hadits?)\\s+|(?:no\\.?|nomor|#)(?:\\s+|(?=[\\d])))*(\\d{1,6})(?:\\.(\\d{1,3}|[a-z]{1,3})|([a-z]{1,3}))?(?:\\s+(?:arsip|gading|tafsirq|(?:al[ -]*)?alamiyah|(?:ar[ -]*)?risalah))?$"
        )
    }
}
