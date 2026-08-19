package com.quranplus.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey
    val id: Int,
    val number: Int,
    @ColumnInfo(name = "name_arabic")
    val nameArabic: String,
    @ColumnInfo(name = "name_latin")
    val nameLatin: String,
    @ColumnInfo(name = "name_english")
    val nameEnglish: String,
    @ColumnInfo(name = "revelation_type")
    val revelationType: String,
    @ColumnInfo(name = "ayah_count")
    val ayahCount: Int
)

@Entity(
    tableName = "ayahs",
    indices = [
        Index(value = ["surah_id", "ayah_number"], name = "idx_ayahs_surah"),
        Index(value = ["juz"], name = "idx_ayahs_juz"),
        Index(value = ["page"], name = "idx_ayahs_page")
    ]
)
data class AyahEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "text_arabic")
    val textArabic: String,
    val transliteration: String,
    @ColumnInfo(name = "translation_id")
    val translationId: String,
    @ColumnInfo(name = "translation_en")
    val translationEn: String,
    val juz: Int = 1,
    val page: Int = 1,
    @ColumnInfo(name = "tajwid_tags")
    val tajwidTags: String? = null
)

@Entity(
    tableName = "word_by_word",
    indices = [
        Index(value = ["surah_id", "ayah_number", "word_index"], unique = true),
        Index(value = ["surah_id", "ayah_number"], name = "idx_word_by_word_ayah")
    ]
)
data class WordByWordEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "word_index")
    val wordIndex: Int,
    @ColumnInfo(name = "text_arabic")
    val textArabic: String,
    val transliteration: String? = null,
    @ColumnInfo(name = "translation_en")
    val translationEn: String,
    @ColumnInfo(name = "translation_id")
    val translationId: String,
    @ColumnInfo(name = "source_revision")
    val sourceRevision: String,
    @ColumnInfo(name = "source_sha256")
    val sourceSha256: String
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "surah_name")
    val surahName: String,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "ayah_text_arabic")
    val ayahTextArabic: String,
    @ColumnInfo(name = "ayah_translation")
    val ayahTranslation: String,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "last_read")
data class LastReadEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "surah_name")
    val surahName: String,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    val juz: Int = 1,
    val page: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tahsin_lessons",
    indices = [
        Index(value = ["category", "order_index"], name = "idx_tahsin_cat")
    ]
)
data class TahsinLessonEntity(
    @PrimaryKey
    val id: Int,
    val category: String, // MAKHARIJ, SIFAT, HUKUM_TAJWID
    val subcategory: String,
    val title: String,
    @ColumnInfo(name = "letter_arabic")
    val letterArabic: String,
    @ColumnInfo(name = "letter_latin")
    val letterLatin: String,
    val description: String,
    @ColumnInfo(name = "articulation_point")
    val articulationPoint: String,
    @ColumnInfo(name = "audio_sample")
    val audioSample: String? = null,
    @ColumnInfo(name = "example_ayah_text")
    val exampleAyahText: String,
    @ColumnInfo(name = "example_ayah_ref")
    val exampleAyahRef: String,
    @ColumnInfo(name = "rule_type")
    val ruleType: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "hadiths",
    indices = [
        Index(value = ["collection_id", "hadith_number"], name = "idx_hadiths_col")
    ]
)
data class HadithEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "hadith_number")
    val hadithNumber: Int,
    val title: String,
    @ColumnInfo(name = "text_arabic")
    val textArabic: String,
    @ColumnInfo(name = "translation_id")
    val translationId: String,
    @ColumnInfo(name = "translation_en")
    val translationEn: String,
    val reference: String,
    @ColumnInfo(name = "chapter_id")
    val chapterId: String? = null,
    @ColumnInfo(name = "source_revision")
    val sourceRevision: String = "",
    @ColumnInfo(name = "source_sha256")
    val sourceSha256: String = "",
    @ColumnInfo(name = "license_status")
    val licenseStatus: String = "unverified",
    val grade: String? = null,
    val language: String = "en",
    @ColumnInfo(name = "is_complete")
    val isComplete: Boolean = false
)

@Entity(tableName = "hadith_collections")
data class HadithCollectionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "title_arabic")
    val titleArabic: String,
    @ColumnInfo(name = "title_english")
    val titleEnglish: String,
    @ColumnInfo(name = "source_revision")
    val sourceRevision: String,
    @ColumnInfo(name = "source_sha256")
    val sourceSha256: String,
    @ColumnInfo(name = "license_status")
    val licenseStatus: String,
    @ColumnInfo(name = "grade_status")
    val gradeStatus: String,
    @ColumnInfo(name = "record_count")
    val recordCount: Int,
    @ColumnInfo(name = "chapter_count")
    val chapterCount: Int,
    @ColumnInfo(name = "is_complete")
    val isComplete: Boolean,
    @ColumnInfo(name = "bundle_allowed")
    val bundleAllowed: Boolean
)

@Entity(
    tableName = "hadith_chapters",
    primaryKeys = ["collection_id", "chapter_id"]
)
data class HadithChapterEntity(
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "chapter_id")
    val chapterId: String,
    @ColumnInfo(name = "chapter_number")
    val chapterNumber: Int,
    @ColumnInfo(name = "title_arabic")
    val titleArabic: String,
    @ColumnInfo(name = "title_english")
    val titleEnglish: String
)

@Entity(
    tableName = "knowledge_chunks",
    indices = [
        Index(value = ["source_type"], name = "idx_chunks_src")
    ]
)
data class KnowledgeChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    val title: String,
    @ColumnInfo(name = "text_content")
    val textContent: String,
    val embedding: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KnowledgeChunkEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    @ColumnInfo(name = "citations_json")
    val citationsJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey
    val id: Int,
    val prompt: String,
    @ColumnInfo(name = "arabic_snippet")
    val arabicSnippet: String,
    val reference: String,
    @ColumnInfo(name = "options_json")
    val optionsJson: String,
    @ColumnInfo(name = "correct_index")
    val correctIndex: Int,
    val explanation: String,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    @ColumnInfo(name = "source_revision")
    val sourceRevision: String
)

@Entity(
    tableName = "quiz_attempts",
    indices = [Index(value = ["question_id"], name = "idx_quiz_attempt_question")]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "question_id")
    val questionId: Int,
    @ColumnInfo(name = "selected_index")
    val selectedIndex: Int,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
