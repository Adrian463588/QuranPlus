package com.quranplus.app

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.ui.theme.QuranPlusTheme
import com.quranplus.app.features.chatbot.domain.ChatRepository
import com.quranplus.app.features.hadith.data.CanonicalHadithSynchronizer
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.hadith.data.CanonicalSyncResult
import com.quranplus.app.features.hadith.data.HadithBundleManager
import com.quranplus.app.features.hadith.data.HadithNumbering
import com.quranplus.app.features.hadith.data.HadithRepositoryImpl
import com.quranplus.app.features.hadith.domain.GetHadithCollectionsUseCase
import com.quranplus.app.features.hadith.domain.HadithLookupStatus
import com.quranplus.app.features.hadith.domain.HadithNumberingScheme
import com.quranplus.app.features.hadith.domain.SearchHadithUseCase
import com.quranplus.app.features.hadith.presentation.HadithScreen
import com.quranplus.app.features.hadith.presentation.HadithUiState
import com.quranplus.app.features.hadith.presentation.HadithViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HadithMigrationAndIntegrityInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun GIVEN_v14BundledDatabaseWithUserData_WHEN_migratingToV16_THEN_preservesDataAndSyncsAll65716Hadiths() = runBlocking {
        val tempDbFile = File(context.cacheDir, "sprint3_migration_v14_to_v16.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        File("${tempDbFile.absolutePath}-wal").delete()
        File("${tempDbFile.absolutePath}-shm").delete()

        try {
            // 1. Copy bundled DB (v1), advance through v14, seed authentic user data & legacy custom table row.
            context.assets.open("databases/quranplus.db").use { input ->
                tempDbFile.outputStream().use { output -> input.copyTo(output) }
            }

            val driver = BundledSQLiteDriver()
            driver.open(tempDbFile.absolutePath).use { conn ->
                QuranDatabase.migrationsThrough14().forEach { migration ->
                    migration.migrate(conn)
                }
                conn.prepare("PRAGMA user_version = 14").use { it.step() }
                conn.prepare("INSERT INTO bookmarks(surah_id, surah_name, ayah_number, ayah_text_arabic, ayah_translation, note, timestamp) VALUES (2, 'Al-Baqarah', 255, 'الله لا إله إلا هو', 'Ayat Kursi', 'Keep me', 1700000000)").use { it.step() }
                conn.prepare("INSERT INTO last_read(id, surah_id, surah_name, ayah_number, juz, page, timestamp) VALUES (1, 18, 'Al-Kahf', 10, 15, 294, 1700000001)").use { it.step() }
                conn.prepare("INSERT INTO chat_messages(conversation_id, role, content, citations_json, timestamp, completion_status) VALUES ('c1', 'user', 'Dalil', '[]', 1700000002, 'complete')").use { it.step() }
                // Seed legacy v14 custom non-bundled collection row that must survive upgrade cleanly
                conn.prepare("INSERT INTO hadith_collections(id, title_arabic, title_english, source_revision, source_sha256, license_status, grade_status, record_count, chapter_count, is_complete, bundle_allowed) VALUES ('custom_note', 'فوائد', 'Fawaid Custom', 'local-1', '', 'licensed', 'not_provided', 1, 0, 1, 1)").use { it.step() }
                conn.prepare("INSERT INTO hadiths(id, collection_id, hadith_number, title, text_arabic, translation_id, translation_en, reference, source_revision, source_sha256, license_status, language, is_complete) VALUES (9900001, 'custom_note', 1, 'Fawaid Custom', 'إنما الأعمال', 'Amal tergantung niat', '', 'Fawaid Custom no. 1', 'local-1', '', 'licensed', 'id', 1)").use { it.step() }
                // Seed stale pre-upgrade nasai:5111 and stale rag_index_metadata to prove replacement & invalidation
                conn.prepare("INSERT INTO hadiths(id, collection_id, hadith_number, title, text_arabic, translation_id, translation_en, reference, source_revision, source_sha256, license_status, language, is_complete) VALUES (5105117, 'nasai', 5111, 'Sunan al-Nasa''i', 'قَالَبَلَغَنَا', 'mengkikir gigi dan mencabut bulu alis.', '', 'Sunan al-Nasa''i no. 5111', 'old-rev', '', 'licensed', 'id', 1)").use { it.step() }
                conn.prepare("CREATE TABLE IF NOT EXISTS rag_index_metadata(id INTEGER PRIMARY KEY CHECK(id = 1), fingerprint TEXT NOT NULL, model_id TEXT NOT NULL, model_revision TEXT NOT NULL, tokenizer_sha256 TEXT NOT NULL, embedding_dimension INTEGER NOT NULL, normalized INTEGER NOT NULL, pooling TEXT NOT NULL, max_sequence_length INTEGER NOT NULL, chunk_token_count INTEGER NOT NULL, chunk_overlap_tokens INTEGER NOT NULL, corpus_record_count INTEGER NOT NULL, corpus_fingerprint TEXT NOT NULL, updated_at INTEGER NOT NULL)").use { it.step() }
                conn.prepare("INSERT INTO rag_index_metadata(id, fingerprint, model_id, model_revision, tokenizer_sha256, embedding_dimension, normalized, pooling, max_sequence_length, chunk_token_count, chunk_overlap_tokens, corpus_record_count, corpus_fingerprint, updated_at) VALUES (1, 'stale-fp', 'm', 'r', 't', 384, 1, 'mean', 256, 510, 50, 1, 'stale-cfp', 1700000000)").use { it.step() }
            }

            // 2. Open with Room v16 + MIGRATION_14_15 + MIGRATION_15_16
            val db = Room.databaseBuilder(
                context,
                QuranDatabase::class.java,
                tempDbFile.absolutePath
            )
                .setDriver(BundledSQLiteDriver())
                .addMigrations(QuranDatabase.MIGRATION_14_15, QuranDatabase.MIGRATION_15_16)
                .build()

            try {
                val bookmarks = db.bookmarkDao().getAllBookmarks().first()
                assertEquals(1, bookmarks.size)
                assertEquals("Keep me", bookmarks.single().note)

                val lastRead = db.lastReadDao().getLastRead().first()
                assertEquals(18, lastRead?.surahId)

                val chat = db.chatDao().getAllMessages().first()
                assertEquals(1, chat.size)

                val customStats = db.hadithDao().getSchemeStatsForCollection("custom_note")
                assertEquals(1, customStats.single().recordCount)

                // 3. Synchronize offline 9-collection corpus (65,716 records) & verify custom row survival
                val sync = CanonicalHadithSynchronizer(context, db)
                val syncRes = sync.ensureSynchronized()
                assertTrue("Expected Synchronized but got $syncRes", syncRes is CanonicalSyncResult.Synchronized)
                assertEquals(CanonicalSyncResult.AlreadyCurrent, sync.ensureSynchronized())
                val staleMetaRemaining = db.useReaderConnection { conn ->
                    conn.usePrepared("SELECT COUNT(*) FROM rag_index_metadata") { st ->
                        st.step()
                        st.getInt(0)
                    }
                }
                assertEquals(0, staleMetaRemaining)
                assertEquals(65_716, (syncRes as CanonicalSyncResult.Synchronized).activeRecords)
                assertEquals(9, syncRes.collectionCount)
                assertEquals(1, db.hadithDao().countActiveDistinctHadiths("custom_note"))
                assertEquals(26_363, db.hadithDao().countActiveDistinctHadiths("ahmad"))
                assertEquals(3_367, db.hadithDao().countActiveDistinctHadiths("darimi"))

                val ahmadStats = db.hadithDao().getSchemeStatsForCollection("ahmad")
                assertEquals(27_647, ahmadStats.first { it.schemeId == "ar_risalah" }.maxReferenceNumber)
                assertEquals(26_363, ahmadStats.first { it.schemeId == "archive_gading" }.maxReferenceNumber)

                // 4. Verify FTS5 & exact reference lookup across Kutubut Tis'ah including Ahmad 4001
                val repo = HadithRepositoryImpl(db.hadithDao(), HadithNumbering(), sync)
                val ahmadRis = repo.searchDetailed("ahmad", "4001", HadithNumberingScheme.AR_RISALAH)
                assertEquals(HadithLookupStatus.FOUND, ahmadRis.status)
                assertTrue(ahmadRis.records.single().textArabic.contains("آيَةُ اللِّعَانِ"))
                assertEquals("https://hadits.net/ahmad:4001", ahmadRis.records.single().canonicalUrl)

                val ahmadAl = repo.searchDetailed("ahmad", "4001", HadithNumberingScheme.ARCHIVE_GADING)
                assertEquals(HadithLookupStatus.FOUND, ahmadAl.status)
                assertTrue(ahmadAl.records.single().textArabic.contains("مَنْ نَزَلَتْ بِهِ فَاقَةٌ"))

                val khawarijFts = repo.searchDetailed("tirmidhi", "ابو امامة", HadithNumberingScheme.SUNNAH)
                assertEquals(HadithLookupStatus.FOUND, khawarijFts.status)
                assertTrue(khawarijFts.records.any { it.referenceNumber == 3000 })

                val christianFts = repo.searchDetailed("bukhari", "earth thrown", HadithNumberingScheme.SUNNAH)
                assertEquals(HadithLookupStatus.FOUND, christianFts.status)
                assertTrue(christianFts.records.any { it.referenceNumber == 3617 })

                val ahmadLianFts = repo.searchDetailed("ahmad", "عشية الجمعة اللعان", HadithNumberingScheme.AR_RISALAH)
                assertEquals(HadithLookupStatus.FOUND, ahmadLianFts.status)
                assertTrue(ahmadLianFts.records.any { it.textArabic.contains("آيَةُ اللِّعَانِ") })

                val muslimVariants = repo.searchDetailed("muslim", "2723", HadithNumberingScheme.SUNNAH)
                assertTrue(muslimVariants.records.size >= 2)
                val muslim2723b = repo.searchDetailed("muslim", "2723b", HadithNumberingScheme.SUNNAH)
                assertEquals(1, muslim2723b.records.size)
                assertEquals("02", muslim2723b.records.single().variantSuffix)

                // HF-006: Sunan an-Nasa'i 5111 / 5022 + 5112 / 5023 + FTS5 + preserved 5109
                val nasai5111 = repo.searchDetailed("nasai", "5111", HadithNumberingScheme.SUNNAH)
                assertEquals(HadithLookupStatus.FOUND, nasai5111.status)
                assertTrue(nasai5111.records.single().translationId.contains("mengkikir gigi dan membuat tato."))
                assertFalse(nasai5111.records.single().translationId.contains("bulu alis"))
                val nasai5022 = repo.searchDetailed("nasai", "5022", HadithNumberingScheme.ARCHIVE_GADING)
                assertEquals(nasai5111.records.single().stableId, nasai5022.records.single().stableId)
                val nasaiTatoFts = repo.searchDetailed("nasai", "membuat tato", HadithNumberingScheme.SUNNAH)
                assertTrue(nasaiTatoFts.records.any { it.stableId == "sunnah:nasai:5111" })
                assertTrue(repo.searchDetailed("nasai", "5109", HadithNumberingScheme.SUNNAH).records.single().translationId.contains("mencabut bulu alis"))
            } finally {
                db.close()
            }
        } finally {
            tempDbFile.delete()
            File("${tempDbFile.absolutePath}-wal").delete()
            File("${tempDbFile.absolutePath}-shm").delete()
        }
    }

    @Test
    fun GIVEN_hadithScreenCompose_WHEN_displayingAhmad4001AndAbuDawud4000_THEN_rendersBothSchemesAndTouchTargetsCleanly() {
        val db = QuranDatabase.getInstance(context)
        val sync = CanonicalHadithSynchronizer(context, db)
        runBlocking { sync.ensureSynchronized() }

        val repo = HadithRepositoryImpl(db.hadithDao(), HadithNumbering(), sync)
        val bundleManager = org.koin.java.KoinJavaComponent.getKoin().get<HadithBundleManager>()
        val vm = HadithViewModel(
            GetHadithCollectionsUseCase(repo),
            SearchHadithUseCase(repo),
            bundleManager
        )

        composeRule.setContent {
            QuranPlusTheme(darkTheme = true) {
                HadithScreen(
                    viewModel = vm,
                    onRequestStorage = {},
                    onBundleReadyForAi = {}
                )
            }
        }

        // Open Musnad Ahmad 4001 (defaults cleanly to Ar-Risalah scheme -> li'an)
        vm.openReference("ahmad", 4001, "ar_risalah")
        composeRule.waitUntil(timeoutMillis = 15_000) {
            vm.state.value is HadithUiState.Ready
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No. Ar-Risalah").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("No. Arsip").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("No. Arsip").performClick()
        composeRule.waitForIdle()

        vm.openReference("abudawud", 4000, "sunnah")
        composeRule.waitUntil(timeoutMillis = 15_000) {
            vm.state.value is HadithUiState.Ready
        }
        composeRule.waitForIdle()

        // Verify Lompat button & scheme toggle are displayed without clipping
        composeRule.onNodeWithText("Lompat").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("No. Sunnah.com").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("No. Arsip").assertIsDisplayed().assertHeightIsAtLeast(48.dp)

        // Verify all 4 action icons enforce >= 48dp minimum touch target
        composeRule.onAllNodesWithContentDescription("Salin Hadist").onFirst()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onAllNodesWithContentDescription("Bagikan Hadist").onFirst()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onAllNodesWithContentDescription("Simpan Bookmark Hadist").onFirst()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onAllNodesWithContentDescription("Tandai Berhenti Baca").onFirst()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Switch scheme to No. Arsip & search 7563 in bukhari (exists in Sunnah.com, unmapped in Arsip) -> verify UNMAPPED recovery UI
        vm.setCollection("bukhari")
        vm.setScheme(HadithNumberingScheme.ARCHIVE_GADING)
        vm.setQuery("7563")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            vm.state.value is HadithUiState.LookupIssue
        }
        composeRule.onNodeWithText("Gunakan No. Sunnah.com").assertIsDisplayed().performClick()

        // Open Sunan an-Nasa'i 5111 offline -> verify corrected translation rendered onscreen (phone or tablet viewport)
        vm.openReference("nasai", 5111, "sunnah")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            vm.state.value.let { st ->
                st is HadithUiState.Ready && st.records.any { it.stableId == "sunnah:nasai:5111" }
            }
        }
        composeRule.onAllNodesWithText("mengkikir gigi dan membuat tato.", substring = true).onFirst().assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("mengkikir gigi dan mencabut bulu alis.", substring = true)
                .fetchSemanticsNodes().size
        )
    }

    @Test
    fun GIVEN_ahmad4302AndFullCheckpointsOnRoomSql_WHEN_searchedAndRendered_THEN_passesOfflineAndAiGrounding() {
        val db = QuranDatabase.getInstance(context)
        val sync = CanonicalHadithSynchronizer(context, db)
        runBlocking { sync.ensureSynchronized() }

        val repo = HadithRepositoryImpl(db.hadithDao(), HadithNumbering(), sync)
        val chatRepo = org.koin.java.KoinJavaComponent.getKoin().get<ChatRepository>()
        val bundleManager = org.koin.java.KoinJavaComponent.getKoin().get<HadithBundleManager>()

        runBlocking {
            val ahmadCount = db.hadithDao().countActiveDistinctHadiths("ahmad")
            assertEquals(26_363, ahmadCount)

            // Every mandatory archive checkpoint & boundary works end-to-end against real Room SQLite
            val checkpoints = listOf(1, 10, 100, 1000, 2000, 3000, 3999, 4000, 4001, 4002, 4299, 4300, 4301, 4302, 4303, 4304, 4305, 26_363)
            checkpoints.forEach { n ->
                val res = repo.searchDetailed("ahmad", n.toString(), HadithNumberingScheme.ARCHIVE_GADING)
                assertEquals("Checkpoint archive_gading#$n failed", HadithLookupStatus.FOUND, res.status)
                assertEquals("mit:ahmad:$n", res.records.single().stableId)
            }
            assertEquals(HadithLookupStatus.FOUND, repo.searchDetailed("ahmad", "27647", HadithNumberingScheme.AR_RISALAH).status)

            // Exact distinction: ar_risalah:4302 vs archive_gading:4302 + clean crosswalk
            val ris4302 = repo.searchDetailed("ahmad", "4302", HadithNumberingScheme.AR_RISALAH).records.single()
            assertEquals("mit:ahmad:4075", ris4302.stableId)
            assertTrue(ris4302.textArabic.contains("نَسْتَخْصِي"))

            val arch4302 = repo.searchDetailed("ahmad", "4302", HadithNumberingScheme.ARCHIVE_GADING).records.single()
            assertEquals("mit:ahmad:4302", arch4302.stableId)
            assertTrue(arch4302.textArabic.contains("لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ"))
            assertTrue(arch4302.translationId.contains("Janganlah sebagian kalian menjual di atas penjualan sebagian lain"))

            assertEquals("mit:ahmad:4297", repo.searchDetailed("ahmad", "4526", HadithNumberingScheme.AR_RISALAH).records.single().stableId)
            assertEquals("mit:ahmad:4302", repo.searchDetailed("ahmad", "4531", HadithNumberingScheme.AR_RISALAH).records.single().stableId)
            assertEquals("mit:ahmad:4075", repo.searchDetailed(null, "ahmad:4302", HadithNumberingScheme.SUNNAH).records.single().stableId)
            assertEquals("mit:ahmad:4302", repo.searchDetailed(null, "Musnad Ahmad arsip 4302", HadithNumberingScheme.SUNNAH).records.single().stableId)
            assertEquals(HadithLookupStatus.REFERENCE_UNMAPPED, repo.searchDetailed("ahmad", "4000", HadithNumberingScheme.AR_RISALAH).status)
            assertEquals(HadithLookupStatus.RECORD_MISSING, repo.searchDetailed("ahmad", "999999", HadithNumberingScheme.ARCHIVE_GADING).status)

            // AI/RAG real Room citations
            val aiRis = chatRepo.generateRagResponse("dev-ahmad-4302-ris", "Hadis Musnad Ahmad nomor 4302", AiPersona.USTADZ, null)
            assertTrue(aiRis.citations.any { it.canonicalUrl == "https://hadits.net/ahmad:4302" })
            val aiArch = chatRepo.generateRagResponse("dev-ahmad-4302-al", "Hadis Musnad Ahmad arsip 4302", AiPersona.USTADZ, null)
            assertTrue(aiArch.citations.any { it.textSnippet.contains("لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ") })
        }

        val vm = HadithViewModel(GetHadithCollectionsUseCase(repo), SearchHadithUseCase(repo), bundleManager)
        composeRule.setContent {
            QuranPlusTheme(darkTheme = true) {
                HadithScreen(viewModel = vm, onRequestStorage = {}, onBundleReadyForAi = {})
            }
        }

        vm.openReference("ahmad", 4302, "archive_gading")
        composeRule.waitUntil(timeoutMillis = 15_000) {
            vm.state.value.let { it is HadithUiState.Ready && it.records.any { r -> r.stableId == "mit:ahmad:4302" } }
        }
        composeRule.onAllNodesWithText("Janganlah sebagian kalian menjual", substring = true).onFirst().assertIsDisplayed()

        vm.setScheme(HadithNumberingScheme.AR_RISALAH)
        vm.setQuery("4302")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            vm.state.value.let { it is HadithUiState.Ready && it.records.any { r -> r.stableId == "mit:ahmad:4075" } }
        }

        vm.setQuery("999999")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            vm.state.value is HadithUiState.LookupIssue
        }
        composeRule.onNodeWithText("Hadist Tidak Tersedia").assertIsDisplayed()
    }

    @Test
    fun GIVEN_darimi2841AndAllCheckpointsOnRoomSql_WHEN_searchedAndRendered_THEN_passesOfflineAndAiGrounding() {
        val db = QuranDatabase.getInstance(context)
        val sync = CanonicalHadithSynchronizer(context, db)
        runBlocking { sync.ensureSynchronized() }

        val repo = HadithRepositoryImpl(db.hadithDao(), HadithNumbering(), sync)
        val chatRepo = org.koin.java.KoinJavaComponent.getKoin().get<ChatRepository>()
        val bundleManager = org.koin.java.KoinJavaComponent.getKoin().get<HadithBundleManager>()

        runBlocking {
            assertEquals(3_367, db.hadithDao().countActiveDistinctHadiths("darimi"))
            val checkpoints = listOf(1, 10, 100, 500, 1000, 1500, 2000, 2500, 2805, 2839, 2840, 2841, 2842, 2843, 2948, 2949, 2975, 3023, 3027, 3_367)
            checkpoints.forEach { n ->
                val res = repo.searchDetailed("darimi", n.toString(), HadithNumberingScheme.ARCHIVE_GADING)
                assertEquals("Checkpoint darimi#$n failed", HadithLookupStatus.FOUND, res.status)
                val rec = res.records.single()
                assertEquals("mit:darimi:$n", rec.stableId)
                assertEquals(null, rec.canonicalUrl)
            }

            val d2841 = repo.searchDetailed("darimi", "No. 2841", HadithNumberingScheme.ARCHIVE_GADING).records.single()
            assertTrue(d2841.textArabic.contains("إِلَى عَلِيٍّ رَضِيَ اللَّهُ عَنْهُ"))
            assertTrue(d2841.translationId.contains("[Ali] radlyallahu 'anhu"))
            assertFalse(d2841.translationId.contains("Rasulullah"))

            assertEquals("mit:darimi:2841", repo.searchDetailed(null, "Sunan al-Darimi 2841", HadithNumberingScheme.SUNNAH).records.single().stableId)
            assertEquals(HadithLookupStatus.RECORD_MISSING, repo.searchDetailed("darimi", "999999", HadithNumberingScheme.ARCHIVE_GADING).status)

            val aiReply = chatRepo.generateRagResponse("dev-darimi-2841", "Hadis Sunan al-Darimi nomor 2841", AiPersona.USTADZ, null)
            assertEquals(1, aiReply.citations.size)
            assertEquals("hadith:darimi:2841", aiReply.citations.single().deepLinkTarget)
            assertEquals(null, aiReply.citations.single().canonicalUrl)
            assertTrue(aiReply.citations.single().textSnippet.contains("[Ali] radlyallahu 'anhu"))
        }

        val vm = HadithViewModel(GetHadithCollectionsUseCase(repo), SearchHadithUseCase(repo), bundleManager)
        composeRule.setContent {
            QuranPlusTheme(darkTheme = true) {
                HadithScreen(viewModel = vm, onRequestStorage = {}, onBundleReadyForAi = {})
            }
        }

        vm.openReference("darimi", 2841, "archive_gading")
        composeRule.waitUntil(timeoutMillis = 15_000) {
            vm.state.value.let { it is HadithUiState.Ready && it.records.any { r -> r.stableId == "mit:darimi:2841" } }
        }
        composeRule.onAllNodesWithText("harta warisan anak itu untuk ibunya", substring = true).onFirst().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("sunnah.com/darimi", substring = true).fetchSemanticsNodes().size)

        vm.setQuery("999999")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            vm.state.value is HadithUiState.LookupIssue
        }
        composeRule.onNodeWithText("Hadist Tidak Tersedia").assertIsDisplayed()
    }
}
