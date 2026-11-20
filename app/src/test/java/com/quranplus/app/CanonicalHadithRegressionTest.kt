package com.quranplus.app

import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.database.dao.ChatDao
import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.core.database.entity.HadithCollectionEntity
import com.quranplus.app.core.database.entity.HadithGradeEntity
import com.quranplus.app.core.database.entity.HadithReferenceEntity
import com.quranplus.app.core.database.entity.HadithSchemeStatEntity
import com.quranplus.app.core.database.entity.HadithSourceManifestEntity
import com.quranplus.app.features.chatbot.data.ChatRepositoryImpl
import com.quranplus.app.features.chatbot.data.InternetResearcher
import com.quranplus.app.features.chatbot.data.LlmRunner
import com.quranplus.app.features.hadith.data.CanonicalHadithSynchronizer
import com.quranplus.app.features.hadith.data.HadithBundleManager
import com.quranplus.app.features.hadith.data.HadithNumbering
import com.quranplus.app.features.hadith.data.HadithRepositoryImpl
import com.quranplus.app.features.hadith.domain.GetHadithCollectionsUseCase
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithLookupStatus
import com.quranplus.app.features.hadith.domain.HadithNumberingScheme
import com.quranplus.app.features.hadith.domain.SearchHadithUseCase
import com.quranplus.app.features.hadith.presentation.HadithUiState
import com.quranplus.app.features.hadith.presentation.HadithViewModel
import com.quranplus.app.features.rag.data.EmbeddingService
import com.quranplus.app.features.rag.domain.HadithReferenceRequest
import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.RagRuntimeCoordinator
import com.quranplus.app.features.rag.domain.VectorRetriever
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.settings.data.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

@OptIn(ExperimentalCoroutinesApi::class)
class CanonicalHadithRegressionTest {

    private data class LoadedFixture(
        val entity: HadithEntity,
        val refs: List<HadithReferenceEntity>,
        val grades: List<HadithGradeEntity>
    )

    @Test
    fun GIVEN_canonicalAssetBundle_WHEN_verified_THEN_sha256CountsAndZeroCollisionsHold() {
        val manifestFile = assetFile("corpus-manifest.json")
        val bundleFile = assetFile("sunnah-canonical-bundle.zip")
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))

        assertEquals("Unlicense+MIT", manifest.getString("license_id"))
        assertEquals(bundleFile.length(), manifest.getLong("archive_size_bytes"))
        assertEquals(manifest.getString("archive_sha256"), sha256Hex(bundleFile.readBytes()))

        val collections = manifest.getJSONObject("collections")
        assertEquals(
            setOf("bukhari", "muslim", "abudawud", "tirmidhi", "nasai", "ibnmajah", "malik", "ahmad", "darimi"),
            collections.keys().asSequence().toSet()
        )
        assertEquals(26_363, collections.getJSONObject("ahmad").getInt("active_record_count"))
        assertEquals(3_367, collections.getJSONObject("darimi").getInt("active_record_count"))

        var totalActive = 0
        ZipFile(bundleFile).use { zip ->
            for (colId in collections.keys()) {
                val meta = collections.getJSONObject(colId)
                val entryBytes = zip.getInputStream(zip.getEntry(meta.getString("file"))!!).readBytes()
                assertEquals(meta.getString("sha256"), sha256Hex(entryBytes))
                val doc = JSONObject(entryBytes.toString(Charsets.UTF_8))
                val records = doc.getJSONArray("records")
                assertEquals(meta.getInt("active_record_count"), records.length())
                totalActive += records.length()

                val refKeys = HashSet<String>()
                for (i in 0 until records.length()) {
                    val rec = records.getJSONObject(i)
                    val refs = rec.getJSONArray("references")
                    for (r in 0 until refs.length()) {
                        val refObj = refs.getJSONObject(r)
                        val key = "$colId|${refObj.getString("scheme")}|${refObj.getInt("number")}|${refObj.optString("variant")}"
                        assertTrue("Duplicate key $key", refKeys.add(key))
                    }
                }
            }
        }
        assertEquals(manifest.getInt("active_record_count"), totalActive)
    }

    @Test
    fun GIVEN_goldenFixtures_WHEN_queriedFromCanonicalBundle_THEN_allFourMatchVerifiedSources() = runTest {
        val hf1 = loadRecordFromAsset("bukhari", 3617)
        val hf2 = loadRecordFromAsset("abudawud", 4000)
        val hf3 = loadRecordFromAsset("tirmidhi", 3000)
        val hf4 = loadRecordFromAsset("tirmidhi", 813)

        // HF-001: Sahih al-Bukhari 3617
        assertTrue(hf1.entity.textArabic.contains("نَصْرَانِيًّا"))
        assertTrue(hf1.entity.textArabic.contains("لَفَظَتْهُ الأَرْضُ"))
        assertTrue(hf1.entity.translationEn.contains("Christian"))
        assertEquals("https://sunnah.com/bukhari:3617", hf1.entity.canonicalUrl)

        // HF-002: Sunan Abi Dawud 4000 preserves مَالِكِ vs مَلِكِ diacritic distinction + grades
        assertTrue(hf2.entity.textArabic.contains("مَالِكِ يَوْمِ الدِّينِ"))
        assertTrue(hf2.entity.textArabic.contains("مَلِكِ يَوْمِ الدِّينِ"))
        assertTrue(hf2.entity.translationEn.contains("AbuBakr, Umar and Uthman"))
        assertTrue(hf2.grades.any { it.authority == "Al-Albani" && it.grade.contains("Daif") })
        assertEquals("https://sunnah.com/abudawud:4000", hf2.entity.canonicalUrl)

        // HF-003: Jami' at-Tirmidhi 3000 (Abu Umamah & Khawarij, NOT Hajj)
        assertTrue(hf3.entity.textArabic.contains("أَبُو أُمَامَةَ"))
        assertTrue(hf3.entity.textArabic.contains("كِلاَبُ النَّارِ"))
        assertTrue(hf3.entity.translationEn.contains("Khawarij"))
        assertFalse(hf3.entity.translationEn.contains("Hajj"))
        assertEquals("https://sunnah.com/tirmidhi:3000", hf3.entity.canonicalUrl)

        // HF-004: Jami' at-Tirmidhi 813 (Hajj provisions & mount)
        assertTrue(hf4.entity.textArabic.contains("الزَّادُ وَالرَّاحِلَةُ"))
        assertTrue(hf4.entity.translationEn.contains("provisions"))
        assertTrue(hf4.entity.translationEn.contains("Hajj"))
        assertEquals("https://sunnah.com/tirmidhi:813", hf4.entity.canonicalUrl)
        assertNotEquals(hf3.entity.stableId, hf4.entity.stableId)

        // Verify Repository searchDetailed resolves all four in SUNNAH scheme
        listOf(hf1, hf2, hf3, hf4).forEach { fixture ->
            val dao = mockDaoForFixture(fixture)
            val result = HadithRepositoryImpl(dao, ioDispatcher = Dispatchers.Unconfined).searchDetailed(
                collectionId = fixture.entity.collectionId,
                query = fixture.entity.hadithNumber.toString(),
                scheme = HadithNumberingScheme.SUNNAH
            )
            assertEquals(HadithLookupStatus.FOUND, result.status)
            assertEquals(1, result.records.size)
            val rec = result.records.single()
            assertEquals(fixture.entity.canonicalUrl, rec.canonicalUrl)
            assertEquals(fixture.grades.size, rec.grades.size)
        }
    }

    @Test
    fun GIVEN_muslim2723Variants_WHEN_loadedFromBundle_THEN_baseReturnsMultipleAndVariantReturnsSingle() = runTest {
        val variants = loadRecordsByBaseNumberFromAsset("muslim", 2723)
        assertTrue("Expected >= 2 variants for Muslim 2723", variants.size >= 2)
        assertTrue(variants.any { it.refs.any { r -> r.variantSuffix == "01" } })
        assertTrue(variants.any { it.refs.any { r -> r.variantSuffix == "02" } })

        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.countActiveDistinctHadiths("muslim") } returns 7360
        coEvery { dao.findByReference("muslim", "sunnah", 2723, null, any()) } returns variants.map { it.entity }
        coEvery { dao.findByReference("muslim", "sunnah", 2723, "02", any()) } returns variants
            .filter { f -> f.refs.any { it.variantSuffix == "02" } }.map { it.entity }
        coEvery { dao.getReferencesForHadiths(any()) } returns variants.flatMap { it.refs }

        val repo = HadithRepositoryImpl(dao, ioDispatcher = Dispatchers.Unconfined)
        val allRes = repo.searchDetailed("muslim", "2723", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, allRes.status)
        assertTrue(allRes.records.size >= 2)

        val dotRes = repo.searchDetailed("muslim", "2723.02", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, dotRes.status)
        assertEquals(1, dotRes.records.size)
        assertEquals("02", dotRes.records.single().variantSuffix)

        val letterRes = repo.searchDetailed("muslim", "2723b", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, letterRes.status)
        assertEquals(1, letterRes.records.size)
        assertEquals("02", letterRes.records.single().variantSuffix)
    }

    @Test
    fun GIVEN_lookupFailureTaxonomy_WHEN_searching_THEN_distinguishesAllSixErrorStatesCleanly() = runTest {
        val repoDao = mockk<HadithDao>(relaxed = true)
        val repo = HadithRepositoryImpl(repoDao, ioDispatcher = Dispatchers.Unconfined)

        // 1. RECORD_MISSING (known missing_in_source slot)
        coEvery { repoDao.countActiveDistinctHadiths("bukhari") } returns 7580
        coEvery { repoDao.findByReference("bukhari", "sunnah", 99999, null, any()) } returns emptyList()
        coEvery { repoDao.getReferenceEntries("bukhari", "sunnah", 99999) } returns listOf(
            HadithReferenceEntity(
                hadithId = null, stableId = "missing:bukhari:sunnah:99999",
                collectionId = "bukhari", schemeId = "sunnah",
                referenceNumber = 99999, reviewStatus = CanonicalHadithSynchronizer.REVIEW_MISSING_IN_SOURCE
            )
        )
        val missing = repo.searchDetailed("bukhari", "99999", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.RECORD_MISSING, missing.status)
        assertNotNull(missing.errorReport)

        // 2. REFERENCE_UNMAPPED (unmapped in archive_gading while sunnah exists)
        coEvery { repoDao.findByReference("bukhari", "archive_gading", 3617, null, any()) } returns emptyList()
        coEvery { repoDao.getReferenceEntries("bukhari", "sunnah", 3617) } returns listOf(
            HadithReferenceEntity(
                hadithId = 1103617L, stableId = "sunnah:bukhari:3617",
                collectionId = "bukhari", schemeId = "sunnah", referenceNumber = 3617
            )
        )
        val unmapped = repo.searchDetailed("bukhari", "3617", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.REFERENCE_UNMAPPED, unmapped.status)
        assertTrue(unmapped.message!!.contains("No. Sunnah.com"))

        // 3. NOT_INSTALLED (no canonical or archive manifest)
        coEvery { repoDao.countActiveDistinctHadiths("ahmad") } returns 0
        coEvery { repoDao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID) } returns null
        coEvery { repoDao.getSourceManifest("gadingnst-hadith-api") } returns null
        val notInstalled = repo.searchDetailed("ahmad", "1", HadithNumberingScheme.AR_RISALAH)
        assertEquals(HadithLookupStatus.NOT_INSTALLED, notInstalled.status)

        // 4. CORRUPT (failed SHA-256 verification on canonical or archive bundle)
        coEvery { repoDao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID) } returns manifestEntity("CORRUPT")
        val corruptCanonical = repo.searchDetailed("ahmad", "1", HadithNumberingScheme.AR_RISALAH)
        assertEquals(HadithLookupStatus.CORRUPT, corruptCanonical.status)
        coEvery { repoDao.getSourceManifest(CanonicalHadithSynchronizer.CANONICAL_SOURCE_ID) } returns null
        coEvery { repoDao.getSourceManifest("gadingnst-hadith-api") } returns manifestEntity("CORRUPT")
        val corrupt = repo.searchDetailed("ahmad", "1", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.CORRUPT, corrupt.status)

        // 5. PARTIAL (incomplete sync)
        coEvery { repoDao.getSourceManifest("gadingnst-hadith-api") } returns manifestEntity("PARTIAL")
        val partial = repo.searchDetailed("ahmad", "1", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.PARTIAL, partial.status)

        // 6. NO_MATCH (FTS text search finds nothing on healthy collection)
        coEvery { repoDao.countActiveDistinctHadiths("bukhari") } returns 7580
        coEvery { repoDao.searchFts(any()) } returns emptyList()
        val noMatch = repo.searchDetailed("bukhari", "xyznonexistenttoken", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.NO_MATCH, noMatch.status)
    }

    @Test
    fun GIVEN_musnadAhmad4001_WHEN_verifiedAcrossEditions_THEN_arRisalahAndAlAlamiyahStayDistinct() = runTest {
        val bundleFile = assetFile("sunnah-canonical-bundle.zip")
        ZipFile(bundleFile).use { zip ->
            val entry = zip.getEntry("collections/ahmad.json")
            assertNotNull("Expected complete MIT ahmad.json inside offline bundle", entry)
            val doc = JSONObject(zip.getInputStream(entry!!).readBytes().toString(Charsets.UTF_8))
            val records = doc.getJSONArray("records")
            assertEquals(26_363, records.length())

            var arRisalah4001: JSONObject? = null
            var alamiyah4001: JSONObject? = null
            var arRisalah4027: JSONObject? = null
            for (i in 0 until records.length()) {
                val rec = records.getJSONObject(i)
                val refs = rec.getJSONArray("references")
                for (r in 0 until refs.length()) {
                    val refObj = refs.getJSONObject(r)
                    when (refObj.getString("scheme") to refObj.getInt("number")) {
                        "ar_risalah" to 4001 -> arRisalah4001 = rec
                        "archive_gading" to 4001 -> alamiyah4001 = rec
                        "ar_risalah" to 4027 -> arRisalah4027 = rec
                    }
                }
            }
            assertNotNull("Expected Ar-Risalah ahmad:4001 (li'an)", arRisalah4001)
            assertTrue(arRisalah4001!!.getString("arabic").contains("عَشِيَّةَ الْجُمُعَةِ"))
            assertTrue(arRisalah4001.getString("arabic").contains("آيَةُ اللِّعَانِ"))
            assertTrue(arRisalah4001.getString("indonesian").contains("ayat li'an"))
            assertFalse(arRisalah4001.getString("indonesian").contains("kupu-kupu"))

            assertNotNull("Expected Al-Alamiyah/Arsip ahmad:4001 (faqah)", alamiyah4001)
            assertTrue(alamiyah4001!!.getString("arabic").contains("مَنْ نَزَلَتْ بِهِ فَاقَةٌ"))
            assertNotEquals(arRisalah4001.getString("stable_id"), alamiyah4001.getString("stable_id"))

            assertNotNull("Expected Ar-Risalah ahmad:4027 distinct from 4001", arRisalah4027)
            assertNotEquals(arRisalah4001.getString("stable_id"), arRisalah4027!!.getString("stable_id"))
        }
    }

    @Test
    fun GIVEN_hadithViewModel_WHEN_switchingSchemeOrOpeningLegacyReference_THEN_updatesUiStateDeterministically() {
        val mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
        try {
            runTest(mainDispatcher) {
                val hf1 = loadRecordFromAsset("bukhari", 3617)
                val dao = mockk<HadithDao>(relaxed = true)
                coEvery { dao.countActiveDistinctHadiths("bukhari") } returns 7580
                every { dao.getHadithsByCollection("bukhari") } returns flowOf(listOf(hf1.entity))
                coEvery { dao.findByReference("bukhari", "sunnah", 3617, null, any()) } returns listOf(hf1.entity)
                coEvery { dao.findByReference("bukhari", "archive_gading", 3617, null, any()) } returns emptyList()
                coEvery { dao.getReferenceEntries("bukhari", "sunnah", 3617) } returns hf1.refs
                coEvery { dao.getReferencesForHadiths(any()) } returns hf1.refs
                coEvery { dao.getGradesForHadiths(any()) } returns hf1.grades

                val repo = HadithRepositoryImpl(dao, ioDispatcher = mainDispatcher)
                val getCollections = mockk<GetHadithCollectionsUseCase>()
                every { getCollections.invoke() } returns flowOf(
                    listOf(HadithCollection("bukhari", "Sahih al-Bukhari", 7580))
                )
                val bundleManager = mockk<HadithBundleManager>(relaxed = true)
                every { bundleManager.observeStorageRoot() } returns flowOf(null)
                every { bundleManager.observeDownload() } returns flowOf()

                val vm = HadithViewModel(getCollections, SearchHadithUseCase(repo), bundleManager)
                vm.openReference("bukhari", 3617, "sunnah")
                advanceUntilIdle()
                assertTrue("Expected Ready but got ${vm.state.value}", vm.state.value is HadithUiState.Ready)
                val readyState = vm.state.value as HadithUiState.Ready
                assertEquals(3617, readyState.records.single().hadithNumber)

                vm.setQuery("3617")
                vm.setScheme(HadithNumberingScheme.ARCHIVE_GADING)
                advanceUntilIdle()
                val issueState = vm.state.value as HadithUiState.LookupIssue
                assertEquals(HadithLookupStatus.REFERENCE_UNMAPPED, issueState.result.status)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun GIVEN_ragChatRepository_WHEN_askingSpecificGoldenHadithsOffline_THEN_groundsAllFourWithoutInternet() = runTest {
        val fixtures = listOf(
            "Bukhari 3617" to loadRecordFromAsset("bukhari", 3617),
            "Abu Dawud 4000" to loadRecordFromAsset("abudawud", 4000),
            "Tirmidhi 3000" to loadRecordFromAsset("tirmidhi", 3000),
            "Tirmidhi 813" to loadRecordFromAsset("tirmidhi", 813)
        )

        val internet = mockk<InternetResearcher>()
        coEvery { internet.search(any(), any()) } returns emptyList()
        val prefs = mockk<PreferencesManager>()
        every { prefs.onlineResearchEnabled } returns flowOf(true)
        val runner = mockk<LlmRunner>(relaxed = true)
        every { runner.isAvailable() } returns false

        fixtures.forEach { (query, fixture) ->
            val database = mockk<QuranDatabase>(relaxed = true)
            val dao = mockk<HadithDao>(relaxed = true)
            every { database.hadithDao() } returns dao
            coEvery {
                dao.findByReference(fixture.entity.collectionId, "sunnah", fixture.entity.hadithNumber, null, any())
            } returns listOf(fixture.entity)

            val repo = ChatRepositoryImpl(
                mockk<ChatDao>(relaxed = true),
                mockk<EmbeddingService>(relaxed = true),
                mockk<VectorRetriever>(relaxed = true),
                RagPipeline(),
                runner,
                database,
                RagRuntimeCoordinator(),
                internet,
                prefs,
                HadithNumbering { "{}" }
            )
            val res = repo.generateRagResponse("s1", "Jelaskan hadits $query", AiPersona.USTADZ, null)
            assertEquals(1, res.citations.size)
            assertEquals(fixture.entity.canonicalUrl, res.citations.single().canonicalUrl)
            assertTrue(res.citations.single().textSnippet.contains(fixture.entity.textArabic.take(24)))
            coVerify(exactly = 0) { internet.search(any(), any()) }
        }
    }

    @Test
    fun GIVEN_ahmad4001AndPartialCollection_WHEN_searchingAndCheckingRanges_THEN_tracksMaxRefSeparatelyNeverSubstitutesAndReportsPartial() = runTest {
        val risalah4001 = loadRecordFromAsset("ahmad", 4001, "ar_risalah")
        val alamiyah4001 = loadRecordFromAsset("ahmad", 4001, "archive_gading")

        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.countActiveDistinctHadiths("ahmad") } returns 26_363
        coEvery { dao.findByReference("ahmad", "ar_risalah", 4001, null, any()) } returns listOf(risalah4001.entity)
        coEvery { dao.findByReference("ahmad", "archive_gading", 4001, null, any()) } returns listOf(alamiyah4001.entity)
        coEvery { dao.findByReference("ahmad", "sunnah", 4001, null, any()) } returns emptyList()
        coEvery { dao.getReferenceEntries("ahmad", "ar_risalah", 4001) } returns risalah4001.refs
        coEvery { dao.getReferenceEntries("ahmad", "archive_gading", 4001) } returns alamiyah4001.refs
        coEvery { dao.getReferencesForHadiths(listOf(risalah4001.entity.id)) } returns risalah4001.refs
        coEvery { dao.getReferencesForHadiths(listOf(alamiyah4001.entity.id)) } returns alamiyah4001.refs
        coEvery { dao.getGradesForHadiths(listOf(risalah4001.entity.id)) } returns risalah4001.grades
        coEvery { dao.getGradesForHadiths(listOf(alamiyah4001.entity.id)) } returns alamiyah4001.grades
        every { dao.getCollections() } returns flowOf(
            listOf(
                HadithCollectionEntity(
                    id = "ahmad", titleArabic = "مسند أحمد", titleEnglish = "Musnad Ahmad",
                    sourceRevision = "rev", sourceSha256 = "0".repeat(64), licenseStatus = "canonical_mit",
                    gradeStatus = "verified_multi_authority", recordCount = 4305, chapterCount = 1,
                    isComplete = false, bundleAllowed = true, defaultScheme = "ar_risalah",
                    activeRecordCount = 4305, availabilityStatus = CanonicalHadithSynchronizer.STATUS_PARTIAL
                )
            )
        )
        every { dao.getCollectionIds() } returns flowOf(listOf("ahmad"))
        every { dao.getSchemeStats() } returns flowOf(
            listOf(
                HadithSchemeStatEntity("ahmad", "ar_risalah", "No. Ar-Risalah", 24_372, 1, 27_647, 2616, true),
                HadithSchemeStatEntity("ahmad", "archive_gading", "No. Arsip", 4305, 1, 26_363, 22_058, false)
            )
        )

        val repo = HadithRepositoryImpl(dao, ioDispatcher = Dispatchers.Unconfined)

        // Separate count vs highest reference number tracking
        val collections = repo.getCollections().first()
        val ahmadCol = collections.single()
        assertEquals(4305, ahmadCol.count)
        assertEquals(27_647, ahmadCol.maxReferenceNumber(HadithNumberingScheme.AR_RISALAH))
        assertEquals(26_363, ahmadCol.maxReferenceNumber(HadithNumberingScheme.ARCHIVE_GADING))

        // Exact scheme-scoped resolution for Musnad Ahmad 4001
        val resRis = repo.searchDetailed("ahmad", "4001", HadithNumberingScheme.AR_RISALAH)
        assertEquals(HadithLookupStatus.FOUND, resRis.status)
        assertTrue(resRis.records.single().textArabic.contains("آيَةُ اللِّعَانِ"))
        assertEquals("https://hadits.net/ahmad:4001", resRis.records.single().canonicalUrl)

        val resArch = repo.searchDetailed("ahmad", "4001", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.FOUND, resArch.status)
        assertTrue(resArch.records.single().textArabic.contains("مَنْ نَزَلَتْ بِهِ فَاقَةٌ"))
        assertEquals(null, resArch.records.single().canonicalUrl)

        // Negative: requesting scheme without 99999 on partial bundle reports PARTIAL instead of NOT_FOUND
        coEvery { dao.getCollectionById("ahmad") } returns HadithCollectionEntity(
            id = "ahmad", titleArabic = "مسند أحمد", titleEnglish = "Musnad Ahmad",
            sourceRevision = "rev", sourceSha256 = "0".repeat(64), licenseStatus = "licensed",
            gradeStatus = "not_provided", recordCount = 4305, chapterCount = 0,
            isComplete = false, bundleAllowed = true, defaultScheme = "archive_gading",
            activeRecordCount = 4305, availabilityStatus = CanonicalHadithSynchronizer.STATUS_PARTIAL
        )
        coEvery { dao.getSchemeStatsForCollection("ahmad") } returns listOf(
            HadithSchemeStatEntity("ahmad", "archive_gading", "No. Arsip", 4305, 1, 26_363, 22_058, true)
        )
        coEvery { dao.findByReference("ahmad", "archive_gading", 18000, null, any()) } returns emptyList()
        val partialMissing = repo.searchDetailed("ahmad", "18000", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.PARTIAL, partialMissing.status)

        // Offline RAG grounding for "Jelaskan hadits Ahmad 4001"
        val reqRis = HadithReferenceRequest.findAll("Jelaskan hadits Ahmad 4001").single()
        assertEquals("ar_risalah", reqRis.targetSchemeId())
        val reqArch = HadithReferenceRequest.findAll("Jelaskan hadits Ahmad arsip 4001").single()
        assertEquals("archive_gading", reqArch.targetSchemeId())

        val database = mockk<QuranDatabase>(relaxed = true)
        every { database.hadithDao() } returns dao
        val chatRepo = ChatRepositoryImpl(
            mockk<ChatDao>(relaxed = true),
            mockk<EmbeddingService>(relaxed = true),
            mockk<VectorRetriever>(relaxed = true),
            RagPipeline(),
            mockk<LlmRunner>(relaxed = true).apply { every { isAvailable() } returns false },
            database,
            RagRuntimeCoordinator(),
            mockk<InternetResearcher>().apply { coEvery { search(any(), any()) } returns emptyList() },
            mockk<PreferencesManager>().apply { every { onlineResearchEnabled } returns flowOf(true) },
            HadithNumbering { "{}" }
        )
        val chatRes = chatRepo.generateRagResponse("s-ahmad", "Jelaskan hadits Musnad Ahmad 4001", AiPersona.USTADZ, null)
        assertEquals(1, chatRes.citations.size)
        assertEquals("https://hadits.net/ahmad:4001", chatRes.citations.single().canonicalUrl)
        assertTrue(chatRes.citations.single().textSnippet.contains("آيَةُ اللِّعَانِ"))
    }

    private fun mockDaoForFixture(fixture: LoadedFixture): HadithDao {
        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.countActiveDistinctHadiths(fixture.entity.collectionId) } returns 5000
        coEvery {
            dao.findByReference(fixture.entity.collectionId, "sunnah", fixture.entity.hadithNumber, null, any())
        } returns listOf(fixture.entity)
        coEvery { dao.getReferencesForHadiths(any()) } returns fixture.refs
        coEvery { dao.getGradesForHadiths(any()) } returns fixture.grades
        return dao
    }

    private fun loadRecordFromAsset(collectionId: String, targetNumber: Int, scheme: String = "sunnah"): LoadedFixture =
        loadRecordsByBaseNumberFromAsset(collectionId, targetNumber, scheme).single()

    private fun loadRecordsByBaseNumberFromAsset(
        collectionId: String,
        targetNumber: Int,
        scheme: String = "sunnah"
    ): List<LoadedFixture> {
        val bundleFile = assetFile("sunnah-canonical-bundle.zip")
        val results = mutableListOf<LoadedFixture>()
        ZipFile(bundleFile).use { zip ->
            val doc = JSONObject(zip.getInputStream(zip.getEntry("collections/$collectionId.json")!!).readBytes().toString(Charsets.UTF_8))
            val records = doc.getJSONArray("records")
            val titleEn = doc.getString("title_english")
            val licenseName = doc.optString("license", "Unlicense")
            val licenseStatus = if (licenseName.equals("MIT", ignoreCase = true)) {
                CanonicalHadithSynchronizer.CANONICAL_MIT_LICENSE_STATUS
            } else {
                CanonicalHadithSynchronizer.CANONICAL_LICENSE_STATUS
            }
            val defaultScheme = doc.optString("default_scheme", "sunnah")
            for (i in 0 until records.length()) {
                val rec = records.getJSONObject(i)
                val refsArr = rec.getJSONArray("references")
                val matchesTarget = (0 until refsArr.length()).any { idx ->
                    val r = refsArr.getJSONObject(idx)
                    r.getString("scheme") == scheme && r.getInt("number") == targetNumber
                }
                if (!matchesTarget) continue

                val id = rec.getLong("id")
                val stableId = rec.getString("stable_id")
                val hadithNum = rec.getInt("hadith_number")
                val gradesArr = rec.getJSONArray("grades")
                val grades = (0 until gradesArr.length()).map { g ->
                    val go = gradesArr.getJSONObject(g)
                    HadithGradeEntity(
                        hadithId = id, stableId = stableId,
                        authority = go.getString("authority"), grade = go.getString("grade")
                    )
                }
                val refs = (0 until refsArr.length()).map { r ->
                    val ro = refsArr.getJSONObject(r)
                    HadithReferenceEntity(
                        hadithId = id, stableId = stableId, collectionId = collectionId,
                        schemeId = ro.getString("scheme"), referenceNumber = ro.getInt("number"),
                        variantSuffix = ro.optString("variant"),
                        bookNumber = ro.optInt("book", 0), hadithInBook = ro.optInt("hadith", 0),
                        canonicalUrl = ro.optString("url").takeIf { it.isNotBlank() && it != "null" },
                        isPrimary = ro.getString("scheme") == defaultScheme
                    )
                }
                val entity = HadithEntity(
                    id = id,
                    collectionId = collectionId,
                    hadithNumber = hadithNum,
                    title = rec.optString("chapter_title").ifBlank { titleEn },
                    textArabic = rec.optString("arabic"),
                    translationId = rec.optString("indonesian"),
                    translationEn = rec.optString("english"),
                    reference = "$titleEn no. $hadithNum (${if (defaultScheme == "ar_risalah") "Ar-Risalah" else "Sunnah.com"})",
                    chapterId = rec.optString("chapter_id").takeIf { it.isNotBlank() && it != "0" },
                    sourceRevision = doc.getString("source_revision"),
                    sourceSha256 = "0".repeat(64),
                    licenseStatus = licenseStatus,
                    grade = grades.joinToString("; ") { "${it.authority}: ${it.grade}" }.ifBlank { null },
                    isComplete = rec.getBoolean("is_complete"),
                    stableId = stableId,
                    schemeId = defaultScheme,
                    canonicalUrl = rec.optString("canonical_url").takeIf { it.isNotBlank() && it != "null" },
                    contentChecksum = rec.getString("content_sha256"),
                    verificationStatus = rec.getString("verification_status")
                )
                results += LoadedFixture(entity, refs, grades)
            }
        }
        return results
    }

    private fun manifestEntity(status: String) = HadithSourceManifestEntity(
        sourceId = "gadingnst-hadith-api",
        sourceUri = "https://github.com/gadingnst/hadith-api",
        revision = "rev",
        archiveSha256 = "a".repeat(64),
        licenseId = "MIT",
        activeRecordCount = 0,
        missingSlotCount = 0,
        status = status,
        updatedAtEpochMs = 0L
    )

    private fun assetFile(name: String): File = listOf(
        File("src/main/assets/hadith/$name"),
        File("app/src/main/assets/hadith/$name")
    ).first(File::isFile)

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
