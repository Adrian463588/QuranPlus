package com.quranplus.app

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quranplus.app.core.database.QuranDatabase
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
        assertEquals(0, database.hadithDao().getAllHadiths().size)
        assertEquals(0, database.knowledgeChunkDao().getChunksCount())
    }
}
