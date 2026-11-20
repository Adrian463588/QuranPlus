package com.quranplus.app.core.database.dao

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.quranplus.app.core.database.entity.AyahEntity
import com.quranplus.app.core.database.entity.BookmarkEntity
import com.quranplus.app.core.database.entity.ChatMessageEntity
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.core.database.entity.HadithCollectionEntity
import com.quranplus.app.core.database.entity.HadithChapterEntity
import com.quranplus.app.core.database.entity.HadithGradeEntity
import com.quranplus.app.core.database.entity.HadithReferenceEntity
import com.quranplus.app.core.database.entity.HadithSchemeStatEntity
import com.quranplus.app.core.database.entity.HadithSourceManifestEntity
import com.quranplus.app.core.database.entity.HadithTranslationEntity
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

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun countAyahs(): Int

    @Query("UPDATE ayahs SET transliteration = :transliteration WHERE id = :id")
    suspend fun updateAyahTransliteration(id: Long, transliteration: String)

    @Query("UPDATE ayahs SET text_arabic = :textArabic WHERE id = :id")
    suspend fun updateAyahTextArabic(id: Long, textArabic: String)

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
    @Query("SELECT COUNT(*) FROM tahsin_lessons")
    suspend fun countLessons(): Int

    @Query("SELECT id FROM tahsin_lessons WHERE is_completed = 1")
    suspend fun getCompletedLessonIds(): List<Int>

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

    @Query("SELECT * FROM hadiths WHERE collection_id = :collectionId ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getHadithsByCollectionPaged(collectionId: String, limit: Int, offset: Int): List<HadithEntity>

    @Query(
        "SELECT h.* FROM hadiths h " +
            "INNER JOIN hadith_references r ON r.hadith_id = h.id " +
            "WHERE r.collection_id = :collectionId " +
            "AND r.scheme_id = :schemeId " +
            "AND r.reference_number BETWEEN :minNumber AND :maxNumber " +
            "AND r.hadith_id IS NOT NULL " +
            "GROUP BY h.id " +
            "ORDER BY MIN(r.reference_number) ASC, MIN(r.variant_suffix) ASC " +
            "LIMIT :limit"
    )
    suspend fun getHadithsBySchemeSlice(
        collectionId: String,
        schemeId: String,
        minNumber: Int,
        maxNumber: Int,
        limit: Int
    ): List<HadithEntity>

    @Query(
        "SELECT h.* FROM hadiths h " +
            "INNER JOIN hadith_references r ON r.hadith_id = h.id " +
            "WHERE r.collection_id = :collectionId " +
            "AND r.scheme_id = :schemeId " +
            "AND r.hadith_id IS NOT NULL " +
            "GROUP BY h.id " +
            "ORDER BY MIN(r.reference_number) ASC, MIN(r.variant_suffix) ASC " +
            "LIMIT :limit OFFSET :offset"
    )
    suspend fun getHadithsBySchemePaged(
        collectionId: String,
        schemeId: String,
        limit: Int,
        offset: Int
    ): List<HadithEntity>

    @Query(
        "SELECT collection_id, COUNT(*) AS record_count FROM hadiths " +
            "WHERE source_revision = :sourceRevision " +
            "AND source_sha256 = :sourceSha256 " +
            "AND license_status = :licenseStatus " +
            "AND is_complete = 1 " +
            "GROUP BY collection_id ORDER BY collection_id ASC"
    )
    suspend fun getVerifiedBundleCollectionCounts(
        sourceRevision: String,
        sourceSha256: String,
        licenseStatus: String
    ): List<HadithBundleCollectionCount>

    @RawQuery(observedEntities = [HadithEntity::class])
    suspend fun searchFts(query: SupportSQLiteQuery): List<HadithEntity>

    @Query(
        "SELECT * FROM hadiths " +
            "WHERE (:collectionId IS NULL OR collection_id = :collectionId) " +
            "AND hadith_number = :number ORDER BY collection_id ASC LIMIT :limit"
    )
    suspend fun searchByNumber(
        collectionId: String?,
        number: Int,
        limit: Int = 100
    ): List<HadithEntity>

    @Query("SELECT COUNT(*) FROM hadiths")
    suspend fun countHadiths(): Int

    @Query("SELECT COUNT(*) FROM hadiths WHERE length(trim(translation_id)) > 0")
    suspend fun countHadithsWithIndonesianTranslation(): Int

    @Query("DELETE FROM hadiths WHERE collection_id = :collectionId")
    suspend fun deleteHadithsForCollection(collectionId: String)

    @Query("DELETE FROM hadith_chapters WHERE collection_id = :collectionId")
    suspend fun deleteChaptersForCollection(collectionId: String)

    @Query("SELECT collection_id FROM hadiths GROUP BY collection_id ORDER BY collection_id ASC")
    fun getCollectionIds(): Flow<List<String>>

    @Query(
        "UPDATE hadiths SET translation_id = :translation " +
            "WHERE id = :id AND license_status = 'reference'"
    )
    suspend fun updateIndonesianTranslationForReference(id: Long, translation: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<HadithCollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<HadithChapterEntity>)

    @Query("SELECT * FROM hadith_collections WHERE id = :collectionId LIMIT 1")
    suspend fun getCollectionById(collectionId: String): HadithCollectionEntity?

    @Query(
        "SELECT h.* FROM hadiths h " +
            "INNER JOIN hadith_references r ON r.hadith_id = h.id " +
            "WHERE (:collectionId IS NULL OR r.collection_id = :collectionId) " +
            "AND r.scheme_id = :schemeId " +
            "AND r.reference_number = :number " +
            "AND (:variant IS NULL OR r.variant_suffix = :variant) " +
            "AND r.hadith_id IS NOT NULL " +
            "ORDER BY r.collection_id ASC, r.reference_number ASC, r.variant_suffix ASC " +
            "LIMIT :limit"
    )
    suspend fun findByReference(
        collectionId: String?,
        schemeId: String,
        number: Int,
        variant: String? = null,
        limit: Int = 50
    ): List<HadithEntity>

    @Query(
        "SELECT * FROM hadith_references " +
            "WHERE (:collectionId IS NULL OR collection_id = :collectionId) " +
            "AND scheme_id = :schemeId " +
            "AND reference_number = :number " +
            "ORDER BY collection_id ASC, variant_suffix ASC"
    )
    suspend fun getReferenceEntries(
        collectionId: String?,
        schemeId: String,
        number: Int
    ): List<HadithReferenceEntity>

    @Query("SELECT * FROM hadith_references WHERE hadith_id IN (:hadithIds) ORDER BY hadith_id ASC, is_primary DESC, scheme_id ASC")
    suspend fun getReferencesForHadiths(hadithIds: List<Long>): List<HadithReferenceEntity>

    @Query("SELECT * FROM hadith_references WHERE stable_id = :stableId ORDER BY is_primary DESC, scheme_id ASC")
    suspend fun getReferencesByStableId(stableId: String): List<HadithReferenceEntity>

    @Query("SELECT * FROM hadiths WHERE stable_id = :stableId LIMIT 1")
    suspend fun getHadithByStableId(stableId: String): HadithEntity?

    @Query("SELECT * FROM hadiths WHERE id IN (:ids)")
    suspend fun getHadithsByIds(ids: List<Long>): List<HadithEntity>

    @Query("SELECT * FROM hadith_grades WHERE hadith_id IN (:hadithIds) ORDER BY id ASC")
    suspend fun getGradesForHadiths(hadithIds: List<Long>): List<HadithGradeEntity>

    @Query("SELECT * FROM hadith_translations WHERE hadith_id IN (:hadithIds) ORDER BY language ASC")
    suspend fun getTranslationsForHadiths(hadithIds: List<Long>): List<HadithTranslationEntity>

    @Query("SELECT * FROM hadith_chapters WHERE collection_id = :collectionId AND chapter_id = :chapterId LIMIT 1")
    suspend fun getChapter(collectionId: String, chapterId: String): HadithChapterEntity?

    @Query("SELECT * FROM hadith_source_manifests WHERE source_id = :sourceId LIMIT 1")
    suspend fun getSourceManifest(sourceId: String): HadithSourceManifestEntity?

    @Query("SELECT * FROM hadith_source_manifests ORDER BY source_id ASC")
    suspend fun getAllSourceManifests(): List<HadithSourceManifestEntity>

    @Query("SELECT * FROM hadith_scheme_stats ORDER BY collection_id ASC, is_default DESC, scheme_id ASC")
    fun getSchemeStats(): Flow<List<HadithSchemeStatEntity>>

    @Query("SELECT * FROM hadith_scheme_stats WHERE collection_id = :collectionId ORDER BY is_default DESC, scheme_id ASC")
    suspend fun getSchemeStatsForCollection(collectionId: String): List<HadithSchemeStatEntity>

    @Query("DELETE FROM hadith_scheme_stats WHERE collection_id = :collectionId")
    suspend fun deleteSchemeStatsForCollection(collectionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchemeStats(stats: List<HadithSchemeStatEntity>)

    @Query(
        "SELECT COUNT(DISTINCT stable_id) FROM hadiths " +
            "WHERE (:collectionId IS NULL OR collection_id = :collectionId) " +
            "AND verification_status IN ('VERIFIED', 'PARTIAL') " +
            "AND (length(trim(text_arabic)) > 0 OR length(trim(translation_en)) > 0 OR length(trim(translation_id)) > 0)"
    )
    suspend fun countActiveDistinctHadiths(collectionId: String? = null): Int

    @Query(
        "SELECT COUNT(DISTINCT stable_id) FROM hadiths " +
            "WHERE length(trim(stable_id)) > 0 " +
            "AND source_revision = :revision " +
            "AND source_sha256 = :sha256"
    )
    suspend fun countCanonicalHadithsByRevisionAndSha(revision: String, sha256: String): Int

    @Query("SELECT COUNT(*) FROM hadiths WHERE collection_id = :collectionId AND license_status = :licenseStatus")
    suspend fun countHadithsByCollectionAndLicense(collectionId: String, licenseStatus: String): Int

    @Query("DELETE FROM hadith_references WHERE collection_id = :collectionId")
    suspend fun deleteReferencesForCollection(collectionId: String)

    @Query("DELETE FROM hadith_translations WHERE collection_id = :collectionId")
    suspend fun deleteTranslationsForCollection(collectionId: String)

    @Query("DELETE FROM hadith_grades WHERE hadith_id IN (SELECT id FROM hadiths WHERE collection_id = :collectionId)")
    suspend fun deleteGradesForCollection(collectionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferences(references: List<HadithReferenceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(translations: List<HadithTranslationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<HadithGradeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceManifest(manifest: HadithSourceManifestEntity)

    @Query("SELECT COUNT(*) FROM hadiths WHERE collection_id = :collectionId AND license_status IN (:licenseStatuses)")
    suspend fun countHadithsByCollectionAndLicenses(collectionId: String, licenseStatuses: List<String>): Int

    @Transaction
    suspend fun clearCollectionData(collectionId: String) {
        deleteGradesForCollection(collectionId)
        deleteTranslationsForCollection(collectionId)
        deleteReferencesForCollection(collectionId)
        deleteSchemeStatsForCollection(collectionId)
        deleteHadithsForCollection(collectionId)
        deleteChaptersForCollection(collectionId)
    }

    @Transaction
    suspend fun runInTransaction(block: suspend () -> Unit) {
        block()
    }

    @Transaction
    suspend fun replaceCollectionAtomically(
        collection: HadithCollectionEntity,
        chapters: List<HadithChapterEntity>,
        hadiths: List<HadithEntity>,
        references: List<HadithReferenceEntity>,
        translations: List<HadithTranslationEntity>,
        grades: List<HadithGradeEntity>,
        schemeStats: List<HadithSchemeStatEntity> = emptyList()
    ) {
        clearCollectionData(collection.id)
        if (chapters.isNotEmpty()) chapters.chunked(500).forEach { insertChapters(it) }
        if (hadiths.isNotEmpty()) hadiths.chunked(500).forEach { insertHadiths(it) }
        if (references.isNotEmpty()) references.chunked(500).forEach { insertReferences(it) }
        if (translations.isNotEmpty()) translations.chunked(500).forEach { insertTranslations(it) }
        if (grades.isNotEmpty()) grades.chunked(500).forEach { insertGrades(it) }
        if (schemeStats.isNotEmpty()) insertSchemeStats(schemeStats)
        insertCollections(listOf(collection))
    }
}

data class HadithBundleCollectionCount(
    @ColumnInfo(name = "collection_id") val collectionId: String,
    @ColumnInfo(name = "record_count") val recordCount: Int
)

@Dao
interface KnowledgeChunkDao {
    @Query("SELECT * FROM knowledge_chunks ORDER BY id ASC")
    suspend fun getAllChunks(): List<KnowledgeChunkEntity>

    @Query("SELECT * FROM knowledge_chunks WHERE source_type = :sourceType")
    suspend fun getChunksBySource(sourceType: String): List<KnowledgeChunkEntity>

    @Query("SELECT COUNT(*) FROM knowledge_chunks")
    suspend fun getChunksCount(): Int

    @RawQuery(observedEntities = [KnowledgeChunkEntity::class])
    suspend fun searchFts(query: SupportSQLiteQuery): List<KnowledgeChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun clearConversation(conversationId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllConversations()
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_questions ORDER BY id ASC")
    fun getQuestions(): Flow<List<QuizQuestionEntity>>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    suspend fun countQuestions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)
}

@Dao
interface TafsirDao {
    @Query("SELECT * FROM tafsirs WHERE surah_id = :surahId AND ayah_number = :ayahNumber LIMIT 1")
    suspend fun getTafsirByAyah(surahId: Int, ayahNumber: Int): com.quranplus.app.core.database.entity.TafsirEntity?

    @Query("SELECT * FROM tafsirs WHERE surah_id = :surahId ORDER BY ayah_number ASC")
    fun getTafsirsForSurah(surahId: Int): Flow<List<com.quranplus.app.core.database.entity.TafsirEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tafsirs: List<com.quranplus.app.core.database.entity.TafsirEntity>)

    @Query("SELECT COUNT(*) FROM tafsirs")
    suspend fun count(): Int

    @Query("DELETE FROM tafsirs")
    suspend fun clear()
}
