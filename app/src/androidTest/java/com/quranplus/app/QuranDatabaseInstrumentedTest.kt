package com.quranplus.app

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.database.ReferenceAssetSynchronizer
import com.quranplus.app.features.quran.data.QuranRepositoryImpl
import com.quranplus.app.features.quran.domain.QuranSearchField
import com.quranplus.app.features.quran.domain.QuranSearchFilter
import com.quranplus.app.features.rag.data.SqliteVecVectorIndex
import com.quranplus.app.features.rag.domain.VectorRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuranDatabaseInstrumentedTest {

    @Test
    fun GIVEN_bundledAsset_WHEN_roomMigrates_THEN_quranAndFts5AreAvailable() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = QuranDatabase.getInstance(context)
        ReferenceAssetSynchronizer(context, database).synchronize()

        val surahs = database.quranDao().getAllSurahs().first()
        val ayahs = database.quranDao().getAyahsBySurah(2).first()
        val searchResults = database.quranDao().searchAyahsFts(
            SimpleSQLiteQuery(
                """
                SELECT a.* FROM ayahs AS a
                JOIN ayahs_fts5 ON a.id = ayahs_fts5.rowid
                WHERE ayahs_fts5 MATCH ?
                LIMIT ?
                """.trimIndent(),
                arrayOf<Any>("\"الله\"*", 5)
            )
        )

        assertEquals(114, surahs.size)
        assertFalse(ayahs.isEmpty())
        assertTrue(searchResults.isNotEmpty())
        assertTrue(database.hadithDao().getAllHadiths().isNotEmpty())
        assertEquals(77429, database.wordByWordDao().count())
        assertEquals(77429, database.wordByWordDao().countWithTransliteration())
        assertEquals(16, database.wordByWordDao().getWordsByAyah(10, 20).size)
        assertTrue(database.wordByWordDao().getWordsByAyah(10, 20).all { it.translationEn.isNotBlank() })
        assertTrue(
            database.wordByWordDao().getWordsByAyah(10, 20)
                .all { !it.transliteration.isNullOrBlank() }
        )
        assertTrue(
            database.wordByWordDao().getWordsByAyah(10, 20)
                .all { it.translationId.isNotBlank() }
        )
        assertEquals(17, database.hadithDao().getCollections().first().size)
        assertEquals(54, database.tahsinDao().countLessons())
        assertEquals(12, database.quizDao().getQuestions().first().size)
        assertEquals(0, database.knowledgeChunkDao().getChunksCount())
    }

    @Test
    fun GIVEN_ftsQueryAndSurahFilter_WHEN_repositorySearches_THEN_resultsStayInSelectedSurah() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = QuranDatabase.getInstance(context)
        val repository = QuranRepositoryImpl(
            quranDao = database.quranDao(),
            bookmarkDao = database.bookmarkDao(),
            lastReadDao = database.lastReadDao()
        )

        val results = repository.searchAyahs(
            query = "Allah",
            filter = QuranSearchFilter(surahNumber = 2)
        )

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.surahNumber == 2 })
        assertTrue(results.all { it.surahName.isNotBlank() })
    }

    @Test
    fun GIVEN_englishFieldFilter_WHEN_repositorySearches_THEN_resultsMatchEnglishSource() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = QuranDatabase.getInstance(context)
        ReferenceAssetSynchronizer(context, database).synchronize()
        val sourceAyah = database.quranDao().getAyah(1, 1)
            ?: error("Bundled Al-Faatiha ayah 1 is unavailable")
        val sourceToken = Regex("[\\p{L}]{4,}")
            .find(sourceAyah.translationEn)
            ?.value
            ?: error("Bundled English translation has no searchable token")
        val repository = QuranRepositoryImpl(
            quranDao = database.quranDao(),
            bookmarkDao = database.bookmarkDao(),
            lastReadDao = database.lastReadDao()
        )

        val results = repository.searchAyahs(
            query = sourceToken,
            filter = QuranSearchFilter(field = QuranSearchField.ENGLISH)
        )

        assertTrue(results.any { it.id == sourceAyah.id })
        assertTrue(results.all { it.translationEn.contains(sourceToken, ignoreCase = true) })
    }

    @Test
    fun GIVEN_bundledSqliteVecExtension_WHEN_indexChecksReadiness_THEN_vec0IsAvailable() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = QuranDatabase.getInstance(context)
        assertTrue("sqlite-vec index is not ready", SqliteVecVectorIndex(database).isReady())
    }

    @Test
    fun GIVEN_verifiedVectorIndex_WHEN_replacingAndSearching_THEN_returnsNearestCitation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val index = SqliteVecVectorIndex(QuranDatabase.getInstance(context))
        val embedding = FloatArray(384).apply { this[0] = 1f }
        val record = VectorRecord(
            sourceId = "test-quran-1",
            sourceType = "quran",
            collectionId = "quran",
            chunkIndex = 0,
            text = "Alhamdulillah",
            embedding = embedding,
            title = "Al-Fatihah",
            reference = "QS. Al-Fatihah:1"
        )

        assertEquals(1, index.replace(listOf(record)))
        val matches = index.search(embedding, k = 1)

        assertEquals(1, matches.size)
        assertEquals(record.sourceId, matches.single().sourceId)
        assertEquals(record.reference, matches.single().reference)
    }
}
