package com.quranplus.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.quranplus.app.core.database.entity.AyahEntity
import com.quranplus.app.core.database.entity.BookmarkEntity
import com.quranplus.app.core.database.entity.ChatMessageEntity
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.core.database.entity.HadithCollectionEntity
import com.quranplus.app.core.database.entity.HadithChapterEntity
import com.quranplus.app.core.database.entity.KnowledgeChunkEntity
import com.quranplus.app.core.database.entity.LastReadEntity
import com.quranplus.app.core.database.entity.SurahEntity
import com.quranplus.app.core.database.entity.TahsinLessonEntity
import com.quranplus.app.core.database.entity.WordByWordEntity
import com.quranplus.app.core.database.entity.QuizAttemptEntity
import com.quranplus.app.core.database.entity.QuizQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs ORDER BY number ASC")
    suspend fun getAllSurahsOnce(): List<SurahEntity>

    @Query("SELECT * FROM surahs WHERE number = :surahNumber LIMIT 1")
    suspend fun getSurahByNumber(surahNumber: Int): SurahEntity?

    @Query("SELECT * FROM ayahs WHERE surah_id = :surahNumber ORDER BY ayah_number ASC")
    fun getAyahsBySurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE page = :page ORDER BY surah_id ASC, ayah_number ASC LIMIT 1")
    suspend fun getFirstAyahByPage(page: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE juz = :juz ORDER BY surah_id ASC, ayah_number ASC LIMIT 1")
    suspend fun getFirstAyahByJuz(juz: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE surah_id = :surahNumber AND ayah_number = :ayahNumber LIMIT 1")
    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): AyahEntity?

    @Query("SELECT * FROM ayahs ORDER BY surah_id ASC, ayah_number ASC")
    suspend fun getAllAyahs(): List<AyahEntity>

    @RawQuery(observedEntities = [AyahEntity::class])
    suspend fun searchAyahsFts(query: SupportSQLiteQuery): List<AyahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)
}

@Dao
interface WordByWordDao {
    @Query(
        "SELECT * FROM word_by_word " +
            "WHERE surah_id = :surahNumber " +
            "ORDER BY ayah_number ASC, word_index ASC"
    )
    fun getWordsBySurah(surahNumber: Int): Flow<List<WordByWordEntity>>

    @Query(
        "SELECT * FROM word_by_word " +
            "WHERE surah_id = :surahNumber AND ayah_number = :ayahNumber " +
            "ORDER BY word_index ASC"
    )
    suspend fun getWordsByAyah(surahNumber: Int, ayahNumber: Int): List<WordByWordEntity>

    @Query("SELECT COUNT(*) FROM word_by_word")
    suspend fun count(): Int

    @Query(
        "SELECT COUNT(*) FROM word_by_word " +
            "WHERE transliteration IS NOT NULL AND length(trim(transliteration)) > 0"
    )
    suspend fun countWithTransliteration(): Int

    @Query("SELECT COUNT(*) FROM word_by_word WHERE source_revision = :sourceRevision")
    suspend fun countBySourceRevision(sourceRevision: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordByWordEntity>)

    @Query("DELETE FROM word_by_word")
    suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY surah_id ASC, ayah_number ASC")
    fun getAllBookmarksBySurah(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE surah_id = :surahNumber AND ayah_number = :ayahNumber)")
    fun isBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean>

    @Query("SELECT * FROM bookmarks WHERE surah_id = :surahNumber AND ayah_number = :ayahNumber LIMIT 1")
    suspend fun getBookmark(surahNumber: Int, ayahNumber: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surah_id = :surahNumber AND ayah_number = :ayahNumber")
    suspend fun deleteBookmarkByAyah(surahNumber: Int, ayahNumber: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("UPDATE bookmarks SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)
}

@Dao
interface LastReadDao {
    @Query("SELECT * FROM last_read WHERE id = 1 LIMIT 1")
    fun getLastRead(): Flow<LastReadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastRead(lastRead: LastReadEntity)
}

@Dao
interface TahsinDao {
    @Query("SELECT * FROM tahsin_lessons ORDER BY order_index ASC")
    fun getAllLessons(): Flow<List<TahsinLessonEntity>>

    @Query("SELECT * FROM tahsin_lessons WHERE category = :category ORDER BY order_index ASC")
    fun getLessonsByCategory(category: String): Flow<List<TahsinLessonEntity>>

    @Query("SELECT * FROM tahsin_lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: Int): TahsinLessonEntity?

    @Query("UPDATE tahsin_lessons SET is_completed = :isCompleted WHERE id = :lessonId")
    suspend fun updateLessonProgress(lessonId: Int, isCompleted: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<TahsinLessonEntity>)
}

@Dao
interface HadithDao {
    @Query("SELECT * FROM hadith_collections ORDER BY title_english ASC")
    fun getCollections(): Flow<List<HadithCollectionEntity>>

    @Query("SELECT COUNT(*) FROM hadith_collections")
    suspend fun countCollections(): Int

    @Query("SELECT * FROM hadith_chapters WHERE collection_id = :collectionId ORDER BY chapter_number ASC")
    fun getChapters(collectionId: String): Flow<List<HadithChapterEntity>>

    @Query("SELECT * FROM hadiths WHERE collection_id = :collectionId ORDER BY hadith_number ASC")
    fun getHadithsByCollection(collectionId: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE collection_id = :collectionId AND hadith_number = :number LIMIT 1")
    suspend fun getHadithByNumber(collectionId: String, number: Int): HadithEntity?

    @Query("SELECT * FROM hadiths ORDER BY collection_id ASC, hadith_number ASC")
    suspend fun getAllHadiths(): List<HadithEntity>

    @Query("SELECT collection_id FROM hadiths GROUP BY collection_id ORDER BY collection_id ASC")
    fun getCollectionIds(): Flow<List<String>>

    @Query(
        "SELECT * FROM hadiths " +
            "WHERE (:collectionId IS NULL OR collection_id = :collectionId) AND " +
            "(text_arabic LIKE '%' || :query || '%' OR translation_en LIKE '%' || :query || '%') " +
            "ORDER BY hadith_number ASC LIMIT :limit"
    )
    suspend fun search(collectionId: String?, query: String, limit: Int = 100): List<HadithEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<HadithCollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<HadithChapterEntity>)
}

@Dao
interface KnowledgeChunkDao {
    @Query("SELECT * FROM knowledge_chunks")
    suspend fun getAllChunks(): List<KnowledgeChunkEntity>

    @Query("SELECT * FROM knowledge_chunks WHERE source_type = :sourceType")
    suspend fun getChunksBySource(sourceType: String): List<KnowledgeChunkEntity>

    @Query("SELECT COUNT(*) FROM knowledge_chunks")
    suspend fun getChunksCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun clearConversation(conversationId: String)
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_questions ORDER BY id ASC")
    fun getQuestions(): Flow<List<QuizQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)
}
