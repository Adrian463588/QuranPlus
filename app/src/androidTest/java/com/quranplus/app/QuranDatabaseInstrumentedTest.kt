package com.quranplus.app

import androidx.sqlite.db.SimpleSQLiteQuery
import android.database.sqlite.SQLiteDatabase
import androidx.room.useWriterConnection
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
import java.io.File

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
        assertEquals(0, bundledTableCount(context, "hadiths"))
        assertEquals(0, bundledTableCount(context, "hadith_collections"))
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
        val quranEmbedding = FloatArray(384).apply { this[0] = 1f }
        val hadithEmbedding = FloatArray(384).apply { this[1] = 1f }
        val documentEmbedding = FloatArray(384).apply { this[2] = 1f }
        val records = listOf(
            VectorRecord(
                sourceId = "test-quran-1",
                sourceType = "quran",
                collectionId = "quran",
                chunkIndex = 0,
                text = "Alhamdulillah",
                embedding = quranEmbedding,
                title = "Al-Fatihah",
                reference = "QS. Al-Fatihah:1"
            ),
            VectorRecord(
                sourceId = "test-hadith-1",
                sourceType = "hadith",
                collectionId = "bukhari",
                chunkIndex = 0,
                text = "Niat adalah dasar amal",
                embedding = hadithEmbedding,
                title = "Sahih al-Bukhari",
                reference = "Sahih al-Bukhari no. 1"
            ),
            VectorRecord(
                sourceId = "test-document-1",
                sourceType = "user_document",
                collectionId = "user_document",
                chunkIndex = 0,
                text = "Catatan RAG lokal",
                embedding = documentEmbedding,
                title = "Dokumen lokal",
                reference = "dokumen-lokal"
            )
        )

        try {
            assertEquals(3, index.replace(records))
            val coverage = index.coverage()
            assertEquals(3, coverage.recordCount)
            assertEquals(setOf("quran", "hadith", "user_document"), coverage.sourceTypes)

            val matches = index.search(quranEmbedding, k = 1)

            assertEquals(1, matches.size)
            assertEquals(records.first().sourceId, matches.single().sourceId)
            assertEquals(records.first().reference, matches.single().reference)
        } finally {
            QuranDatabase.getInstance(context).useWriterConnection { connection ->
                connection.usePrepared("DELETE FROM quranplus_vectors") { statement -> statement.step() }
            }
        }
    }

    private fun bundledTableCount(context: android.content.Context, table: String): Int {
        val copy = File.createTempFile("quranplus-asset-", ".db", context.cacheDir)
        return try {
            context.assets.open("databases/quranplus.db").use { input ->
                copy.outputStream().use { output -> input.copyTo(output) }
            }
            SQLiteDatabase.openDatabase(
                copy.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { database ->
                database.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
            }
        } finally {
            copy.delete()
        }
    }
}
