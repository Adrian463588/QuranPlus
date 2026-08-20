package com.quranplus.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.quranplus.app.core.database.entity.HadithChapterEntity
import com.quranplus.app.core.database.entity.HadithCollectionEntity
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.core.database.entity.QuizQuestionEntity
import com.quranplus.app.core.database.entity.TahsinLessonEntity
import com.quranplus.app.core.database.entity.WordByWordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reconciles immutable reference tables when an installed app is upgraded.
 * Room's pre-packaged asset is copied only on first install, so migrations
 * must explicitly import newly shipped reference rows without touching user data.
 */
class ReferenceAssetSynchronizer(
    private val context: Context,
    private val database: QuranDatabase
) {
    private val wordByWordRevision =
        "quran.com-api:wbw-id:025540d4ba76c5f0e29db120d8997051b6870b6d3f4d3f7264474a8d6ef2769a"
    private val hadithSourceRevision = "hadith-json-1.3.0"

    suspend fun synchronize() = withContext(Dispatchers.IO) {
        val temporaryAsset = copyAssetToCache()
        try {
            SQLiteDatabase.openDatabase(
                temporaryAsset.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { source ->
                synchronizeWordByWord(source)
                synchronizeHadithCollections(source)
                synchronizeHadithContent(source)
                synchronizeTahsin(source)
                synchronizeQuiz(source)
            }
        } finally {
            temporaryAsset.delete()
        }
    }

    private suspend fun synchronizeWordByWord(source: SQLiteDatabase) {
        val sourceCount = source.queryCount("word_by_word")
        if (sourceCount == 0) return

        if (source.querySingleText(
                table = "word_by_word",
                column = "source_sha256"
            ).isNullOrBlank()
        ) return
        val targetCount = database.wordByWordDao().countBySourceRevision(wordByWordRevision)
        if (targetCount >= sourceCount) return

        database.wordByWordDao().clear()

        val batch = ArrayList<WordByWordEntity>(BATCH_SIZE)
        source.rawQuery(
            "SELECT id, surah_id, ayah_number, word_index, text_arabic, " +
                "transliteration, translation_en, translation_id, source_revision, source_sha256 " +
                "FROM word_by_word ORDER BY id ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                batch += WordByWordEntity(
                    id = cursor.getLong(0),
                    surahId = cursor.getInt(1),
                    ayahNumber = cursor.getInt(2),
                    wordIndex = cursor.getInt(3),
                    textArabic = cursor.getString(4),
                    transliteration = cursor.getString(5),
                    translationEn = cursor.getString(6),
                    translationId = cursor.getString(7),
                    sourceRevision = wordByWordRevision,
                    sourceSha256 = cursor.getString(9)
                )
                if (batch.size == BATCH_SIZE) {
                    database.wordByWordDao().insertAll(batch)
                    batch.clear()
                }
            }
        }
        if (batch.isNotEmpty()) {
            database.wordByWordDao().insertAll(batch)
        }
    }

    private suspend fun synchronizeHadithCollections(source: SQLiteDatabase) {
        val sourceCount = source.queryCount("hadith_collections")
        if (sourceCount == 0) return

        val collections = ArrayList<HadithCollectionEntity>(sourceCount)
        source.rawQuery(
            "SELECT id, title_arabic, title_english, source_revision, source_sha256, " +
                "license_status, grade_status, record_count, chapter_count, " +
                "is_complete, bundle_allowed FROM hadith_collections ORDER BY id ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                collections += HadithCollectionEntity(
                    id = cursor.getString(0),
                    titleArabic = cursor.getString(1),
                    titleEnglish = cursor.getString(2),
                    sourceRevision = cursor.getString(3),
                    sourceSha256 = cursor.getString(4),
                    licenseStatus = cursor.getString(5),
                    gradeStatus = cursor.getString(6),
                    recordCount = cursor.getInt(7),
                    chapterCount = cursor.getInt(8),
                    isComplete = cursor.getInt(9) != 0,
                    bundleAllowed = cursor.getInt(10) != 0
                )
            }
        }
        database.hadithDao().insertCollections(collections)
    }

    private suspend fun synchronizeHadithContent(source: SQLiteDatabase) {
        val sourceCount = source.queryCount("hadiths")
        if (sourceCount > 0 && database.hadithDao().countHadiths() < sourceCount) {
            database.withTransaction {
                synchronizeHadithChapters(source)
                val batch = ArrayList<HadithEntity>(BATCH_SIZE)
                source.rawQuery(
                    "SELECT id, collection_id, hadith_number, title, text_arabic, " +
                        "translation_id, translation_en, reference " +
                        "FROM hadiths ORDER BY id ASC",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val translation = cursor.getString(6)
                        batch += HadithEntity(
                            id = cursor.getLong(0),
                            collectionId = cursor.getString(1),
                            hadithNumber = cursor.getInt(2),
                            title = cursor.getString(3),
                            textArabic = cursor.getString(4),
                            translationId = cursor.getString(5),
                            translationEn = translation,
                            reference = cursor.getString(7),
                            sourceRevision = hadithSourceRevision,
                            licenseStatus = "reference",
                            language = "en",
                            isComplete = translation.isNotBlank()
                        )
                        if (batch.size == BATCH_SIZE) {
                            database.hadithDao().insertHadiths(batch)
                            batch.clear()
                        }
                    }
                }
                if (batch.isNotEmpty()) database.hadithDao().insertHadiths(batch)
            }
        }
        synchronizeHadithTranslations(source)
    }

    private suspend fun synchronizeHadithTranslations(source: SQLiteDatabase) {
        val sourceTranslationCount = source.rawQuery(
            "SELECT COUNT(*) FROM hadiths WHERE length(trim(translation_id)) > 0",
            null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        if (sourceTranslationCount == 0 ||
            database.hadithDao().countHadithsWithIndonesianTranslation() >= sourceTranslationCount
        ) return

        source.rawQuery(
            "SELECT id, translation_id FROM hadiths " +
                "WHERE length(trim(translation_id)) > 0",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                database.hadithDao().updateIndonesianTranslation(
                    id = cursor.getLong(0),
                    translation = cursor.getString(1)
                )
            }
        }
    }

    private suspend fun synchronizeHadithChapters(source: SQLiteDatabase) {
        val chapterBatch = ArrayList<HadithChapterEntity>(BATCH_SIZE)
        source.rawQuery(
            "SELECT collection_id, chapter_id, chapter_number, title_arabic, title_english " +
                "FROM hadith_chapters ORDER BY collection_id, chapter_number ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                chapterBatch += HadithChapterEntity(
                    collectionId = cursor.getString(0),
                    chapterId = cursor.getString(1),
                    chapterNumber = cursor.getInt(2),
                    titleArabic = cursor.getString(3),
                    titleEnglish = cursor.getString(4)
                )
                if (chapterBatch.size == BATCH_SIZE) {
                    database.hadithDao().insertChapters(chapterBatch)
                    chapterBatch.clear()
                }
            }
        }
        if (chapterBatch.isNotEmpty()) database.hadithDao().insertChapters(chapterBatch)
    }

    private suspend fun synchronizeTahsin(source: SQLiteDatabase) {
        if (!source.hasTable("tahsin_lessons")) return
        val sourceCount = source.queryCount("tahsin_lessons")
        if (sourceCount == 0 || database.tahsinDao().countLessons() >= sourceCount) return

        val completedIds = database.tahsinDao().getCompletedLessonIds().toSet()
        val lessons = ArrayList<TahsinLessonEntity>(sourceCount)
        source.rawQuery(
            "SELECT id, category, subcategory, title, letter_arabic, letter_latin, " +
                "description, articulation_point, audio_sample, example_ayah_text, " +
                "example_ayah_ref, rule_type, order_index FROM tahsin_lessons " +
                "ORDER BY order_index ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                lessons += TahsinLessonEntity(
                    id = id,
                    category = cursor.getString(1),
                    subcategory = cursor.getString(2),
                    title = cursor.getString(3),
                    letterArabic = cursor.getString(4),
                    letterLatin = cursor.getString(5),
                    description = cursor.getString(6),
                    articulationPoint = cursor.getString(7),
                    audioSample = cursor.getStringOrNull(8),
                    exampleAyahText = cursor.getString(9),
                    exampleAyahRef = cursor.getString(10),
                    ruleType = cursor.getString(11),
                    orderIndex = cursor.getInt(12),
                    isCompleted = id in completedIds
                )
            }
        }
        database.tahsinDao().insertLessons(lessons)
    }

    private suspend fun synchronizeQuiz(source: SQLiteDatabase) {
        if (!source.hasTable("quiz_questions")) return
        val sourceCount = source.queryCount("quiz_questions")
        if (sourceCount == 0 || database.quizDao().countQuestions() >= sourceCount) return

        val questions = ArrayList<QuizQuestionEntity>(sourceCount)
        source.rawQuery(
            "SELECT id, prompt, arabic_snippet, reference, options_json, correct_index, " +
                "explanation, source_id, source_revision FROM quiz_questions ORDER BY id ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                questions += QuizQuestionEntity(
                    id = cursor.getInt(0),
                    prompt = cursor.getString(1),
                    arabicSnippet = cursor.getString(2),
                    reference = cursor.getString(3),
                    optionsJson = cursor.getString(4),
                    correctIndex = cursor.getInt(5),
                    explanation = cursor.getString(6),
                    sourceId = cursor.getString(7),
                    sourceRevision = cursor.getString(8)
                )
            }
        }
        database.quizDao().insertQuestions(questions)
    }

    private fun copyAssetToCache(): File {
        val target = File.createTempFile("quranplus-reference-", ".db", context.cacheDir)
        context.assets.open("databases/quranplus.db").use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }

    private fun SQLiteDatabase.queryCount(table: String): Int {
        rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun SQLiteDatabase.querySingleText(table: String, column: String): String? {
        rawQuery("SELECT $column FROM $table LIMIT 1", null).use { cursor ->
            return if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
        }
    }

    private fun SQLiteDatabase.hasTable(table: String): Boolean {
        rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(table)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private companion object {
        const val BATCH_SIZE = 500
    }
}
