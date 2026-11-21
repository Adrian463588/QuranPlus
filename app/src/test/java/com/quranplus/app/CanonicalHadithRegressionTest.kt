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
import com.quranplus.app.features.hadith.data.HadithContentCorrector
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

    @Test
    fun GIVEN_nasai5111And5112_WHEN_verifiedAcrossBundleRepoAndRag_THEN_correctsTattooPreservesNamshAndGrades() = runTest {
        val hf6 = loadRecordFromAsset("nasai", 5111, "sunnah")
        val hf6Arch = loadRecordFromAsset("nasai", 5022, "archive_gading")
        assertEquals(hf6.entity.stableId, hf6Arch.entity.stableId)
        assertEquals("sunnah:nasai:5111", hf6.entity.stableId)
        assertTrue(hf6.entity.textArabic.contains("قَالَ بَلَغَنَا"))
        assertFalse(hf6.entity.textArabic.contains("قَالَبَلَغَنَا"))
        assertTrue(hf6.entity.textArabic.contains("الْوَشْرِ وَالْوَشْمِ"))
        assertTrue(hf6.entity.translationId.contains("[Ahmad bin Amru bin As Sarh]"))
        assertTrue(hf6.entity.translationId.contains("mengkikir gigi dan membuat tato."))
        assertFalse(hf6.entity.translationId.contains("bulu alis"))
        assertEquals("https://sunnah.com/nasai:5111", hf6.entity.canonicalUrl)
        assertTrue(hf6.grades.any { it.authority == "Al-Albani" && it.grade == "Sahih" })
        assertTrue(hf6.grades.any { it.authority == "Abu Ghuddah" && it.grade == "Sahih" })
        assertTrue(hf6.grades.any { it.authority == "Zubair Ali Zai" && it.grade == "Daif" })

        val hf6Twin = loadRecordFromAsset("nasai", 5112, "sunnah")
        val hf6TwinArch = loadRecordFromAsset("nasai", 5023, "archive_gading")
        assertEquals(hf6Twin.entity.stableId, hf6TwinArch.entity.stableId)
        assertTrue(hf6Twin.entity.textArabic.contains("قَالَ بَلَغَنَا"))
        assertTrue(hf6Twin.entity.translationId.contains("[Qutaibah]"))
        assertTrue(hf6Twin.entity.translationId.contains("mengkikir gigi dan membuat tato."))
        assertFalse(hf6Twin.entity.translationId.contains("bulu alis"))

        // Preserve valid related hadiths (الْمُتَنَمِّصَاتِ)
        val nasai5109 = loadRecordFromAsset("nasai", 5109, "sunnah")
        assertTrue(nasai5109.entity.textArabic.contains("الْمُتَنَمِّصَاتِ"))
        assertTrue(nasai5109.entity.translationId.contains("mencabut bulu alis"))
        val bukhari5931 = loadRecordFromAsset("bukhari", 5931, "sunnah")
        assertTrue(bukhari5931.entity.translationId.contains("mencukur habis alis"))

        // Exact scheme-scoped lookup for canonical 5111 and archive 5022
        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.countActiveDistinctHadiths("nasai") } returns 5679
        coEvery { dao.findByReference("nasai", "sunnah", 5111, null, any()) } returns listOf(hf6.entity)
        coEvery { dao.findByReference("nasai", "archive_gading", 5022, null, any()) } returns listOf(hf6.entity)
        coEvery { dao.getReferencesForHadiths(any()) } returns hf6.refs
        coEvery { dao.getGradesForHadiths(any()) } returns hf6.grades

        val repo = HadithRepositoryImpl(dao, ioDispatcher = Dispatchers.Unconfined)
        val sunnahRes = repo.searchDetailed("nasai", "5111", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, sunnahRes.status)
        assertEquals("sunnah:nasai:5111", sunnahRes.records.single().stableId)
        assertTrue(sunnahRes.records.single().translationId.contains("mengkikir gigi dan membuat tato."))

        val archiveRes = repo.searchDetailed("nasai", "5022", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.FOUND, archiveRes.status)
        assertEquals("sunnah:nasai:5111", archiveRes.records.single().stableId)
        assertEquals("https://sunnah.com/nasai:5111", archiveRes.records.single().canonicalUrl)

        // Offline RAG grounding resolves corrected Nasai 5111 and arsip 5022 without stale text
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
        val ragCanonical = chatRepo.generateRagResponse("s-nasai-5111", "Jelaskan hadits Nasai 5111 tentang tato", AiPersona.USTADZ, null)
        assertEquals(1, ragCanonical.citations.size)
        assertEquals("https://sunnah.com/nasai:5111", ragCanonical.citations.single().canonicalUrl)
        assertTrue(ragCanonical.citations.single().textSnippet.contains("mengkikir gigi dan membuat tato."))
        assertFalse(ragCanonical.citations.single().textSnippet.contains("bulu alis"))

        val ragArchive = chatRepo.generateRagResponse("s-nasai-5022", "Lihat hadits Nasai arsip 5022", AiPersona.USTADZ, null)
        assertEquals(1, ragArchive.citations.size)
        assertEquals("hadith:nasai:5022", ragArchive.citations.single().deepLinkTarget)
        assertTrue(ragArchive.citations.single().textSnippet.contains("mengkikir gigi dan membuat tato."))
    }

    @Test
    fun GIVEN_hadithContentCorrector_WHEN_applied_THEN_fixesLegacyWhitespaceEnforcesIdempotencyAndRejectsMismatch() {
        val corrector = HadithContentCorrector { assetFile("content-corrections.json").readText(Charsets.UTF_8) }
        val legacyArabic = "أَخْبَرَنَا أَحْمَدُ بْنُ عَمْرِو بْنِ السَّرْحِ قَالَ حَدَّثَنَا ابْنُ وَهْبٍ قَالَ أَخْبَرَنِي اللَّيْثُ عَنْ يَزِيدَ بْنِ أَبِي حَبِيبٍ عَنْ أَبِي الْحُصَيْنِ الْحِمْيَرِيِّ عَنْ أَبِي رَيْحَانَةَ قَالَبَلَغَنَا أَنَّ رَسُولَ اللَّهِ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَهَى عَنْ الْوَشْرِ وَالْوَشْمِ"
        val legacyIndonesian = "Telah mengabarkan kepada kami [Ahmad bin Amru bin As Sarh] ia berkata; telah menceritakan kepada kami [Ibnu Wahb] ia berkata; telah mengabarkan kepadaku [Al Laits] dari [Yazid bin Abu Habib] dari [Abu Al Hushain Al Himyari] dari [Abu Raihanah] ia berkata, \"Telah sampai kabar kepada kami bahwa Rasulullah shallallahu 'alaihi wasallam melarang mengkikir gigi dan mencabut bulu alis.\""

        // First application fixes both translation and missing Arabic space
        val applied = corrector.apply("nasai", 5022, "archive_gading:nasai:5022", legacyArabic, legacyIndonesian)
        assertTrue(applied is HadithContentCorrector.CorrectionOutcome.Applied)
        val resolved = applied as HadithContentCorrector.CorrectionOutcome.Applied
        assertEquals("CORR-NASAI-5111-01", resolved.correctionId)
        assertTrue(resolved.fixedArabic.contains("قَالَ بَلَغَنَا"))
        assertFalse(resolved.fixedArabic.contains("قَالَبَلَغَنَا"))
        assertTrue(resolved.fixedTranslationId.contains("mengkikir gigi dan membuat tato."))
        assertFalse(resolved.fixedTranslationId.contains("bulu alis"))

        // Second application is strictly idempotent (no further mutation)
        val secondPass = corrector.apply(
            "nasai", 5111, "sunnah:nasai:5111", resolved.fixedArabic, resolved.fixedTranslationId
        )
        assertTrue(secondPass is HadithContentCorrector.CorrectionOutcome.AlreadyCorrected)

        // Upstream digest mismatch rejects silent mutation (fail-closed)
        val mismatch = corrector.apply(
            "nasai", 5111, "sunnah:nasai:5111", resolved.fixedArabic, "Teks sumber upstream berubah tidak dikenal"
        )
        assertTrue(mismatch is HadithContentCorrector.CorrectionOutcome.DigestMismatch)

        // Unrelated valid hadith remains completely untouched
        val valid5109 = corrector.apply(
            "nasai", 5109, "sunnah:nasai:5109",
            "لَعَنَ اللَّهُ الْمُتَنَمِّصَاتِ",
            "melaknat wanita yang mencabut bulu alis"
        )
        assertTrue(valid5109 is HadithContentCorrector.CorrectionOutcome.NoRule)
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

    @Test
    fun GIVEN_ahmad4302_WHEN_searchedAcrossEditionsAndRag_THEN_resolvesAuthenticallyWithoutSubstitution() = runTest {
        val risalah4302 = loadRecordFromAsset("ahmad", 4302, "ar_risalah")
        val alamiyah4302 = loadRecordFromAsset("ahmad", 4302, "archive_gading")
        val risalah4526 = loadRecordFromAsset("ahmad", 4526, "ar_risalah")

        // Distinct records per scholarly edition
        assertEquals("mit:ahmad:4075", risalah4302.entity.stableId)
        assertTrue(risalah4302.entity.textArabic.contains("نَسْتَخْصِي"))
        assertEquals("https://hadits.net/ahmad:4302", risalah4302.entity.canonicalUrl)

        assertEquals("mit:ahmad:4302", alamiyah4302.entity.stableId)
        assertTrue(alamiyah4302.entity.textArabic.contains("عَبْدُ الرَّحْمَنِ"))
        assertTrue(alamiyah4302.entity.textArabic.contains("لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ"))
        assertTrue(alamiyah4302.entity.translationId.contains("Janganlah sebagian kalian menjual di atas penjualan sebagian lain"))
        val alamiyahRisNums = alamiyah4302.refs.filter { it.schemeId == "ar_risalah" }.map { it.referenceNumber }.toSet()
        assertTrue("Expected 4531 in alamiyah4302 ar_risalah refs", 4531 in alamiyahRisNums)
        assertFalse("Spurious 4526 must not contaminate mit:ahmad:4302", 4526 in alamiyahRisNums)
        assertEquals("mit:ahmad:4297", risalah4526.entity.stableId)
        assertTrue(risalah4526.entity.textArabic.contains("الشِّغَارِ"))

        val bukhari4302 = loadRecordFromAsset("bukhari", 4302)
        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.countActiveDistinctHadiths("ahmad") } returns 26_363
        coEvery { dao.countActiveDistinctHadiths("bukhari") } returns 7563
        coEvery { dao.findByReference("ahmad", "ar_risalah", 4302, null, any()) } returns listOf(risalah4302.entity)
        coEvery { dao.findByReference("ahmad", "archive_gading", 4302, null, any()) } returns listOf(alamiyah4302.entity)
        coEvery { dao.findByReference(null, "sunnah", 4302, null, any()) } returns listOf(bukhari4302.entity)
        coEvery { dao.findByReference("darimi", "archive_gading", 4302, null, any()) } returns emptyList()
        coEvery { dao.getReferencesForHadiths(any()) } answers {
            val ids = firstArg<List<Long>>().toSet()
            listOf(risalah4302, alamiyah4302, bukhari4302)
                .filter { it.entity.id in ids }
                .flatMap { it.refs }
        }
        coEvery { dao.getGradesForHadiths(any()) } returns emptyList()

        val repo = HadithRepositoryImpl(dao, ioDispatcher = Dispatchers.Unconfined)

        // Exact edition-scoped resolution & natural queries
        val resRis = repo.searchDetailed("ahmad", "4302", HadithNumberingScheme.AR_RISALAH)
        assertEquals(HadithLookupStatus.FOUND, resRis.status)
        assertEquals("mit:ahmad:4075", resRis.records.single().stableId)

        val resNoDot = repo.searchDetailed("ahmad", "No.4302", HadithNumberingScheme.AR_RISALAH)
        assertEquals(HadithLookupStatus.FOUND, resNoDot.status)
        assertEquals("mit:ahmad:4075", resNoDot.records.single().stableId)

        val resArch = repo.searchDetailed("ahmad", "4302", HadithNumberingScheme.ARCHIVE_GADING)
        assertEquals(HadithLookupStatus.FOUND, resArch.status)
        assertEquals("mit:ahmad:4302", resArch.records.single().stableId)
        assertEquals("No. 4302 (Al-Alamiyah)", resArch.records.single().numberLabel)

        val resArsipKeyword = repo.searchDetailed("ahmad", "al-alamiyah 4302", HadithNumberingScheme.AR_RISALAH)
        assertEquals(HadithLookupStatus.FOUND, resArsipKeyword.status)
        assertEquals("mit:ahmad:4302", resArsipKeyword.records.single().stableId)

        val resGlobalExplicit = repo.searchDetailed(null, "ahmad:4302", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, resGlobalExplicit.status)
        assertEquals("mit:ahmad:4075", resGlobalExplicit.records.single().stableId)

        val resGlobalArsip = repo.searchDetailed(null, "Musnad Ahmad arsip 4302", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, resGlobalArsip.status)
        assertEquals("mit:ahmad:4302", resGlobalArsip.records.single().stableId)

        val resGlobalNumeric = repo.searchDetailed(null, "4302", HadithNumberingScheme.SUNNAH)
        assertEquals(HadithLookupStatus.FOUND, resGlobalNumeric.status)
        assertTrue(resGlobalNumeric.records.any { it.collectionId == "ahmad" && it.stableId == "mit:ahmad:4075" })
        assertTrue(resGlobalNumeric.records.any { it.collectionId == "bukhari" && it.hadithNumber == 4302 })

        // AI / RAG grounding for both Ar-Risalah and Al-Alamiyah #4302
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

        val risReply = chatRepo.generateRagResponse("s-ahmad-4302-ris", "Hadis Musnad Ahmad nomor 4302", AiPersona.USTADZ, null)
        assertTrue("Expected Hadits.net URL in reply: ${risReply.citations}", risReply.citations.any { it.canonicalUrl == "https://hadits.net/ahmad:4302" })
        assertTrue(risReply.citations.any { it.textSnippet.contains("نَسْتَخْصِي") })

        val alReply = chatRepo.generateRagResponse("s-ahmad-4302-al", "Hadis Musnad Ahmad arsip 4302", AiPersona.USTADZ, null)
        assertTrue(alReply.citations.any { it.textSnippet.contains("لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ") })
    }

    @Test
    fun GIVEN_completeMusnadAhmadBundle_WHEN_auditingEveryRecordViaProductionRepository_THEN_100PercentDeterministicSearchability() = runTest {
        val bundleFile = assetFile("sunnah-canonical-bundle.zip")
        val ahmadDoc = ZipFile(bundleFile).use { zip ->
            JSONObject(zip.getInputStream(zip.getEntry("collections/ahmad.json")!!).readBytes().toString(Charsets.UTF_8))
        }
        val jsonRecords = ahmadDoc.getJSONArray("records")
        assertEquals(26_363, jsonRecords.length())

        val entitiesById = HashMap<Long, HadithEntity>(jsonRecords.length())
        val refsByHadithId = HashMap<Long, List<HadithReferenceEntity>>(jsonRecords.length())
        val gradesByHadithId = HashMap<Long, List<HadithGradeEntity>>(jsonRecords.length())
        val refsBySchemeNum = HashMap<Pair<String, Int>, MutableList<HadithReferenceEntity>>(55_000)

        for (i in 0 until jsonRecords.length()) {
            val rec = jsonRecords.getJSONObject(i)
            val id = rec.getLong("id")
            val stableId = rec.getString("stable_id")
            val hadithNum = rec.getInt("hadith_number")
            val arabic = rec.getString("arabic")
            val ind = rec.getString("indonesian")
            val gradesArr = rec.getJSONArray("grades")
            val grades = (0 until gradesArr.length()).map { g ->
                val go = gradesArr.getJSONObject(g)
                HadithGradeEntity(hadithId = id, stableId = stableId, authority = go.getString("authority"), grade = go.getString("grade"))
            }
            val refsArr = rec.getJSONArray("references")
            val refs = (0 until refsArr.length()).map { r ->
                val ro = refsArr.getJSONObject(r)
                val refEntity = HadithReferenceEntity(
                    hadithId = id,
                    stableId = stableId,
                    collectionId = "ahmad",
                    schemeId = ro.getString("scheme"),
                    referenceNumber = ro.getInt("number"),
                    variantSuffix = ro.optString("variant"),
                    canonicalUrl = ro.optString("url").takeIf { it.isNotBlank() && it != "null" },
                    isPrimary = ro.getString("scheme") == "ar_risalah"
                )
                refsBySchemeNum.getOrPut(refEntity.schemeId to refEntity.referenceNumber) { mutableListOf() }.add(refEntity)
                refEntity
            }
            val entity = HadithEntity(
                id = id,
                collectionId = "ahmad",
                hadithNumber = hadithNum,
                title = "Musnad Ahmad",
                textArabic = arabic,
                translationId = ind,
                translationEn = "",
                reference = "Musnad Ahmad no. $hadithNum (Ar-Risalah)",
                sourceRevision = ahmadDoc.getString("source_revision"),
                sourceSha256 = "0".repeat(64),
                licenseStatus = "mit",
                isComplete = true,
                stableId = stableId,
                schemeId = "ar_risalah",
                canonicalUrl = rec.optString("canonical_url").takeIf { it.isNotBlank() && it != "null" },
                contentChecksum = rec.getString("content_sha256"),
                verificationStatus = rec.getString("verification_status")
            )
            entitiesById[id] = entity
            refsByHadithId[id] = refs
            if (grades.isNotEmpty()) gradesByHadithId[id] = grades
        }

        val missingSlotsArr = ahmadDoc.getJSONArray("missing_slots")
        val missingArRisalahNumbers = HashSet<Int>(missingSlotsArr.length())
        for (i in 0 until missingSlotsArr.length()) {
            val ms = missingSlotsArr.getJSONObject(i)
            val schemeId = ms.getString("scheme")
            val num = ms.getInt("number")
            missingArRisalahNumbers.add(num)
            refsBySchemeNum.getOrPut(schemeId to num) { mutableListOf() }.add(
                HadithReferenceEntity(
                    hadithId = null,
                    stableId = "missing:ahmad:$schemeId:$num",
                    collectionId = "ahmad",
                    schemeId = schemeId,
                    referenceNumber = num,
                    reviewStatus = ms.getString("reason")
                )
            )
        }

        val collectionMeta = HadithCollectionEntity(
            id = "ahmad",
            titleArabic = "مسند أحمد",
            titleEnglish = "Musnad Ahmad",
            sourceRevision = ahmadDoc.getString("source_revision"),
            sourceSha256 = "0".repeat(64),
            licenseStatus = CanonicalHadithSynchronizer.CANONICAL_MIT_LICENSE_STATUS,
            gradeStatus = "partial",
            recordCount = 26_363,
            chapterCount = 1,
            isComplete = true,
            bundleAllowed = true,
            defaultScheme = "ar_risalah",
            activeRecordCount = 26_363,
            availabilityStatus = CanonicalHadithSynchronizer.STATUS_AVAILABLE
        )
        val ahmadSchemeStats = listOf(
            HadithSchemeStatEntity("ahmad", "ar_risalah", "No. Ar-Risalah", 25_069, 1, 27_647, 2414, true),
            HadithSchemeStatEntity("ahmad", "archive_gading", "No. Arsip", 26_363, 1, 26_363, 0, false)
        )
        val dao = object : HadithDao by mockk(relaxed = true) {
            override suspend fun countActiveDistinctHadiths(collectionId: String?): Int = 26_363
            override suspend fun getCollectionById(collectionId: String): HadithCollectionEntity? =
                collectionMeta.takeIf { collectionId == "ahmad" }
            override suspend fun getSchemeStatsForCollection(collectionId: String): List<HadithSchemeStatEntity> =
                if (collectionId == "ahmad") ahmadSchemeStats else emptyList()
            override suspend fun findByReference(
                collectionId: String?,
                schemeId: String,
                number: Int,
                variant: String?,
                limit: Int
            ): List<HadithEntity> = refsBySchemeNum[schemeId to number]
                .orEmpty()
                .filter { it.hadithId != null && (variant == null || it.variantSuffix == variant) }
                .mapNotNull { entitiesById[it.hadithId] }
            override suspend fun getReferenceEntries(
                collectionId: String?,
                schemeId: String,
                number: Int
            ): List<HadithReferenceEntity> = refsBySchemeNum[schemeId to number].orEmpty()
            override suspend fun getReferencesForHadiths(hadithIds: List<Long>): List<HadithReferenceEntity> =
                hadithIds.flatMap { refsByHadithId[it].orEmpty() }
            override suspend fun getGradesForHadiths(hadithIds: List<Long>): List<HadithGradeEntity> =
                hadithIds.flatMap { gradesByHadithId[it].orEmpty() }
        }

        val repo = HadithRepositoryImpl(dao, ioDispatcher = Dispatchers.Unconfined)

        // 1. Audit 100% of all 26,363 archive_gading (Al-Alamiyah) records through production searchDetailed
        for (n in 1..26_363) {
            val res = repo.searchDetailed("ahmad", n.toString(), HadithNumberingScheme.ARCHIVE_GADING)
            assertEquals("Failed archive_gading lookup for ahmad#$n", HadithLookupStatus.FOUND, res.status)
            assertEquals("Expected 1 record for ahmad#$n", 1, res.records.size)
            val rec = res.records.single()
            assertEquals("mit:ahmad:$n", rec.stableId)
            assertEquals(n, rec.hadithNumber)
            assertTrue("Blank Arabic on ahmad#$n", rec.textArabic.isNotBlank())
            assertTrue("Blank Indonesian on ahmad#$n", rec.translationId.isNotBlank())
        }

        // 2. Audit 100% of all mapped ar_risalah references
        val mappedRisalahNumbers = refsBySchemeNum.keys
            .filter { (schemeId, _) -> schemeId == "ar_risalah" }
            .map { it.second }
            .filter { num -> refsBySchemeNum["ar_risalah" to num].orEmpty().any { it.hadithId != null } }
            .sorted()
        assertTrue("Expected >24,500 mapped ar_risalah numbers", mappedRisalahNumbers.size > 24_500)
        for (r in mappedRisalahNumbers) {
            val res = repo.searchDetailed("ahmad", r.toString(), HadithNumberingScheme.AR_RISALAH)
            assertEquals("Failed ar_risalah lookup for ahmad:$r", HadithLookupStatus.FOUND, res.status)
            assertTrue("Expected match for ar_risalah:$r", res.records.isNotEmpty())
            assertTrue(res.records.all { it.hadithNumber == r && it.textArabic.isNotBlank() && it.translationId.isNotBlank() })
        }

        // 3. Audit all documented unmapped ar_risalah slots -> never silent fallback
        for (m in missingArRisalahNumbers) {
            val res = repo.searchDetailed("ahmad", m.toString(), HadithNumberingScheme.AR_RISALAH)
            assertNotEquals("Unmapped ar_risalah:$m must never silently substitute another record", HadithLookupStatus.FOUND, res.status)
            assertTrue(res.status in setOf(HadithLookupStatus.REFERENCE_UNMAPPED, HadithLookupStatus.RECORD_MISSING))
        }

        // 4. Mandatory checkpoints, repeated idempotency & invalid numbers
        val checkpoints = listOf(1, 10, 100, 1000, 2000, 3000, 3999, 4000, 4001, 4002, 4299, 4300, 4301, 4302, 4303, 4304, 4305, 26363)
        checkpoints.forEach { ckpt ->
            val first = repo.searchDetailed("ahmad", ckpt.toString(), HadithNumberingScheme.ARCHIVE_GADING)
            val second = repo.searchDetailed("ahmad", ckpt.toString(), HadithNumberingScheme.ARCHIVE_GADING)
            assertEquals(first.records.single().stableId, second.records.single().stableId)
        }
        assertEquals(HadithLookupStatus.REFERENCE_UNMAPPED, repo.searchDetailed("ahmad", "4000", HadithNumberingScheme.AR_RISALAH).status)
        assertEquals(HadithLookupStatus.RECORD_MISSING, repo.searchDetailed("ahmad", "999999", HadithNumberingScheme.ARCHIVE_GADING).status)
        assertEquals(HadithLookupStatus.NO_MATCH, repo.searchDetailed("ahmad", "0", HadithNumberingScheme.AR_RISALAH).status)
        assertEquals(HadithLookupStatus.NO_MATCH, repo.searchDetailed("ahmad", "-1", HadithNumberingScheme.AR_RISALAH).status)
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
