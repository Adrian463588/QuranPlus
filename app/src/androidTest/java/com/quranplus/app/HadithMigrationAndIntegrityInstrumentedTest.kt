package com.quranplus.app

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.ui.theme.QuranPlusTheme
import com.quranplus.app.features.hadith.data.CanonicalHadithSynchronizer
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
    }
}
