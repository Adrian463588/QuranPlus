package com.quranplus.app.core.database

import android.content.Context
import android.os.Build
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.dao.ChatDao
import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.core.database.dao.KnowledgeChunkDao
import com.quranplus.app.core.database.dao.LastReadDao
import com.quranplus.app.core.database.dao.QuranDao
import com.quranplus.app.core.database.dao.TahsinDao
import com.quranplus.app.core.database.dao.WordByWordDao
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
import com.quranplus.app.core.database.entity.QuizAttemptEntity
import com.quranplus.app.core.database.entity.QuizQuestionEntity
import com.quranplus.app.core.database.entity.TafsirEntity
import com.quranplus.app.core.database.entity.WordByWordEntity

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        WordByWordEntity::class,
        BookmarkEntity::class,
        LastReadEntity::class,
        TahsinLessonEntity::class,
        HadithEntity::class,
        HadithCollectionEntity::class,
        HadithChapterEntity::class,
        HadithReferenceEntity::class,
        HadithTranslationEntity::class,
        HadithGradeEntity::class,
        HadithSourceManifestEntity::class,
        HadithSchemeStatEntity::class,
        KnowledgeChunkEntity::class,
        ChatMessageEntity::class,
        QuizQuestionEntity::class,
        QuizAttemptEntity::class,
        TafsirEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun wordByWordDao(): WordByWordDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun lastReadDao(): LastReadDao
    abstract fun tahsinDao(): TahsinDao
    abstract fun hadithDao(): HadithDao
    abstract fun knowledgeChunkDao(): KnowledgeChunkDao
    abstract fun chatDao(): ChatDao
    abstract fun quizDao(): com.quranplus.app.core.database.dao.QuizDao
    abstract fun tafsirDao(): com.quranplus.app.core.database.dao.TafsirDao

    companion object {
        private const val DB_NAME = "quranplus.db"
        private const val SQLITE_VEC_LIBRARY = "libvec0.so"

        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getInstance(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): QuranDatabase {
            copyPrepackagedAsset(context)
            val driver = BundledSQLiteDriver()
            materializeSqliteVecExtension(context)
                ?.let { extension ->
                    driver.addExtension(extension.absolutePath, "sqlite3_vec_init")
                }
            val builder = Room.databaseBuilder(
                context.applicationContext,
                QuranDatabase::class.java,
                DB_NAME
            )

            return builder
                .setDriver(driver)
                .addMigrations(*allMigrations())
                .addCallback(FTS_CALLBACK)
                .build()
        }


        private fun materializeSqliteVecExtension(context: Context): File? {
            val nativeLibrary = File(context.applicationInfo.nativeLibraryDir, SQLITE_VEC_LIBRARY)
            if (nativeLibrary.isFile) return nativeLibrary

            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return null
            val extensionDirectory = File(context.codeCacheDir, "sqlite-extensions")
            if (!extensionDirectory.exists() && !extensionDirectory.mkdirs()) return null

            val target = File(extensionDirectory, SQLITE_VEC_LIBRARY)
            if (target.isFile) return target

            return runCatching {
                ZipFile(context.applicationInfo.sourceDir).use { archive ->
                    val entry = archive.getEntry("lib/$abi/$SQLITE_VEC_LIBRARY")
                        ?: return@runCatching null
                    val temporary = File(extensionDirectory, "$SQLITE_VEC_LIBRARY.part")
                    try {
                        archive.getInputStream(entry).use { input ->
                            temporary.outputStream().use { output -> input.copyTo(output) }
                        }
                        try {
                            Files.move(
                                temporary.toPath(),
                                target.toPath(),
                                StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        } catch (_: AtomicMoveNotSupportedException) {
                            Files.move(
                                temporary.toPath(),
                                target.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                    } finally {
                        temporary.delete()
                    }
                    target.takeIf(File::isFile)
                }
            }.getOrNull()
        }

        private fun copyPrepackagedAsset(context: Context) {
            val target = context.getDatabasePath(DB_NAME)
            if (target.isFile) return
            val parent = target.parentFile ?: error("Database directory is unavailable")
            if (!parent.exists() && !parent.mkdirs()) {
                error("Database directory cannot be created")
            }
            val temporary = File(parent, "$DB_NAME.asset.tmp")
            try {
                context.assets.open("databases/$DB_NAME").use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                java.nio.file.Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
                )
            } finally {
                temporary.delete()
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SQLiteConnection) {
                createFts5(database)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS quiz_questions (
                        id INTEGER NOT NULL PRIMARY KEY,
                        prompt TEXT NOT NULL,
                        arabic_snippet TEXT NOT NULL,
                        reference TEXT NOT NULL,
                        options_json TEXT NOT NULL,
                        correct_index INTEGER NOT NULL,
                        explanation TEXT NOT NULL,
                        source_id TEXT NOT NULL,
                        source_revision TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS quiz_attempts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        question_id INTEGER NOT NULL,
                        selected_index INTEGER NOT NULL,
                        isCorrect INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_quiz_attempt_question ON quiz_attempts(question_id)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_insert")
                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_delete")
                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_update")
                database.executeSql("DROP TABLE IF EXISTS ayahs_fts5")
                createFts5(database)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql("ALTER TABLE last_read ADD COLUMN juz INTEGER NOT NULL DEFAULT 1")
                database.executeSql("ALTER TABLE last_read ADD COLUMN page INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS word_by_word (
                        id INTEGER NOT NULL PRIMARY KEY,
                        surah_id INTEGER NOT NULL,
                        ayah_number INTEGER NOT NULL,
                        word_index INTEGER NOT NULL,
                        text_arabic TEXT NOT NULL,
                        translation_en TEXT NOT NULL,
                        translation_id TEXT NOT NULL,
                        source_revision TEXT NOT NULL,
                        source_sha256 TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_word_by_word_surah_id_ayah_number_word_index " +
                        "ON word_by_word(surah_id, ayah_number, word_index)"
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_word_by_word_ayah " +
                        "ON word_by_word(surah_id, ayah_number)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql("ALTER TABLE hadiths ADD COLUMN chapter_id TEXT")
                database.executeSql("ALTER TABLE hadiths ADD COLUMN source_revision TEXT NOT NULL DEFAULT ''")
                database.executeSql("ALTER TABLE hadiths ADD COLUMN source_sha256 TEXT NOT NULL DEFAULT ''")
                database.executeSql("ALTER TABLE hadiths ADD COLUMN license_status TEXT NOT NULL DEFAULT 'unverified'")
                database.executeSql("ALTER TABLE hadiths ADD COLUMN grade TEXT")
                database.executeSql("ALTER TABLE hadiths ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
                database.executeSql("ALTER TABLE hadiths ADD COLUMN is_complete INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS hadith_collections (
                        id TEXT NOT NULL PRIMARY KEY,
                        title_arabic TEXT NOT NULL,
                        title_english TEXT NOT NULL,
                        source_revision TEXT NOT NULL,
                        source_sha256 TEXT NOT NULL,
                        license_status TEXT NOT NULL,
                        grade_status TEXT NOT NULL,
                        record_count INTEGER NOT NULL,
                        chapter_count INTEGER NOT NULL,
                        is_complete INTEGER NOT NULL,
                        bundle_allowed INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS hadith_chapters (
                        collection_id TEXT NOT NULL,
                        chapter_id TEXT NOT NULL,
                        chapter_number INTEGER NOT NULL,
                        title_arabic TEXT NOT NULL,
                        title_english TEXT NOT NULL,
                        PRIMARY KEY(collection_id, chapter_id)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SQLiteConnection) {
                if (!hasColumn(database, "word_by_word", "transliteration")) {
                    database.executeSql(
                        "ALTER TABLE word_by_word ADD COLUMN transliteration TEXT"
                    )
                }
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS tafsirs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        surah_id INTEGER NOT NULL,
                        ayah_number INTEGER NOT NULL,
                        tafsir_text TEXT NOT NULL,
                        source TEXT NOT NULL DEFAULT 'Kemenag RI'
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_tafsirs_surah_ayah ON tafsirs(surah_id, ayah_number)"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SQLiteConnection) {
                createRetrievalFts5(database)
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql(
                    "ALTER TABLE chat_messages ADD COLUMN completion_status TEXT NOT NULL DEFAULT 'complete'"
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_insert")
                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_delete")
                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_update")
                database.executeSql("DROP TABLE IF EXISTS hadiths_fts5")
                createRetrievalFts5(database)
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_delete")
                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_update")
                database.executeSql(
                    """
                    CREATE TRIGGER IF NOT EXISTS ayahs_fts5_after_delete
                    AFTER DELETE ON ayahs BEGIN
                        DELETE FROM ayahs_fts5 WHERE rowid = old.id;
                    END
                    """.trimIndent()
                )
                val newArabicText = "new.text_arabic"
                database.executeSql(
                    """
                    CREATE TRIGGER IF NOT EXISTS ayahs_fts5_after_update
                    AFTER UPDATE ON ayahs BEGIN
                        DELETE FROM ayahs_fts5 WHERE rowid = old.id;
                        INSERT INTO ayahs_fts5(rowid, translation_id, translation_en, transliteration, text_arabic, text_arabic_normalized)
                        VALUES (new.id, new.translation_id, new.translation_en, new.transliteration, new.text_arabic, ${arabicSearchExpression(newArabicText)});
                    END
                    """.trimIndent()
                )

                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_delete")
                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_update")
                database.executeSql(
                    """
                    CREATE TRIGGER IF NOT EXISTS hadiths_fts5_after_delete
                    AFTER DELETE ON hadiths BEGIN
                        DELETE FROM hadiths_fts5 WHERE rowid = old.id;
                    END
                    """.trimIndent()
                )
                database.executeSql(
                    """
                    CREATE TRIGGER IF NOT EXISTS hadiths_fts5_after_update
                    AFTER UPDATE ON hadiths BEGIN
                        DELETE FROM hadiths_fts5 WHERE rowid = old.id;
                        INSERT INTO hadiths_fts5(
                            rowid, collection_id, title, text_arabic, text_arabic_normalized,
                            translation_id, translation_en, reference
                        ) VALUES (
                            new.id, new.collection_id, new.title, new.text_arabic,
                            ${arabicSearchExpression("new.text_arabic")},
                            new.translation_id, new.translation_en, new.reference
                        );
                    END
                    """.trimIndent()
                )

                database.executeSql("DROP TRIGGER IF EXISTS knowledge_chunks_fts5_after_delete")
                database.executeSql("DROP TRIGGER IF EXISTS knowledge_chunks_fts5_after_update")
                database.executeSql(
                    """
                    CREATE TRIGGER IF NOT EXISTS knowledge_chunks_fts5_after_delete
                    AFTER DELETE ON knowledge_chunks BEGIN
                        DELETE FROM knowledge_chunks_fts5 WHERE rowid = old.id;
                    END
                    """.trimIndent()
                )
                database.executeSql(
                    """
                    CREATE TRIGGER IF NOT EXISTS knowledge_chunks_fts5_after_update
                    AFTER UPDATE ON knowledge_chunks BEGIN
                        DELETE FROM knowledge_chunks_fts5 WHERE rowid = old.id;
                        INSERT INTO knowledge_chunks_fts5(
                            rowid, source_type, source_id, title, text_content
                        ) VALUES (new.id, new.source_type, new.source_id, new.title, new.text_content);
                    END
                    """.trimIndent()
                )
            }
        }

        internal val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SQLiteConnection) {
                if (!hasColumn(database, "hadiths", "stable_id")) {
                    database.executeSql("ALTER TABLE hadiths ADD COLUMN stable_id TEXT NOT NULL DEFAULT ''")
                }
                if (!hasColumn(database, "hadiths", "scheme_id")) {
                    database.executeSql("ALTER TABLE hadiths ADD COLUMN scheme_id TEXT NOT NULL DEFAULT 'sunnah'")
                }
                if (!hasColumn(database, "hadiths", "canonical_url")) {
                    database.executeSql("ALTER TABLE hadiths ADD COLUMN canonical_url TEXT")
                }
                if (!hasColumn(database, "hadiths", "content_checksum")) {
                    database.executeSql("ALTER TABLE hadiths ADD COLUMN content_checksum TEXT NOT NULL DEFAULT ''")
                }
                if (!hasColumn(database, "hadiths", "verification_status")) {
                    database.executeSql("ALTER TABLE hadiths ADD COLUMN verification_status TEXT NOT NULL DEFAULT 'VERIFIED'")
                }
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_hadiths_stable_id ON hadiths(stable_id)"
                )

                if (!hasColumn(database, "hadith_collections", "default_scheme")) {
                    database.executeSql(
                        "ALTER TABLE hadith_collections ADD COLUMN default_scheme TEXT NOT NULL DEFAULT 'sunnah'"
                    )
                }
                if (!hasColumn(database, "hadith_collections", "active_record_count")) {
                    database.executeSql(
                        "ALTER TABLE hadith_collections ADD COLUMN active_record_count INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if (!hasColumn(database, "hadith_collections", "availability_status")) {
                    database.executeSql(
                        "ALTER TABLE hadith_collections ADD COLUMN availability_status TEXT NOT NULL DEFAULT 'AVAILABLE'"
                    )
                }

                database.executeSql(
                    """
                    UPDATE hadiths SET
                        stable_id = CASE
                            WHEN collection_id IN ('ahmad', 'darimi') THEN 'archive_gading:' || collection_id || ':' || hadith_number
                            ELSE 'sunnah:' || collection_id || ':' || hadith_number
                        END,
                        scheme_id = CASE
                            WHEN collection_id IN ('ahmad', 'darimi') THEN 'archive_gading'
                            ELSE 'sunnah'
                        END
                    WHERE stable_id = ''
                    """.trimIndent()
                )

                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS hadith_references (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hadith_id INTEGER,
                        stable_id TEXT NOT NULL,
                        collection_id TEXT NOT NULL,
                        scheme_id TEXT NOT NULL,
                        reference_number INTEGER NOT NULL,
                        variant_suffix TEXT NOT NULL,
                        book_number INTEGER NOT NULL,
                        hadith_in_book INTEGER NOT NULL,
                        canonical_url TEXT,
                        is_primary INTEGER NOT NULL,
                        review_status TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_hadith_ref_unique " +
                        "ON hadith_references(collection_id, scheme_id, reference_number, variant_suffix)"
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_hadith_ref_hadith_id ON hadith_references(hadith_id)"
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_hadith_ref_stable_id ON hadith_references(stable_id)"
                )

                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS hadith_translations (
                        hadith_id INTEGER NOT NULL,
                        stable_id TEXT NOT NULL,
                        collection_id TEXT NOT NULL,
                        language TEXT NOT NULL,
                        edition_id TEXT NOT NULL,
                        text TEXT NOT NULL,
                        source_revision TEXT NOT NULL,
                        license_status TEXT NOT NULL,
                        PRIMARY KEY(stable_id, language)
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_hadith_trans_hadith_id ON hadith_translations(hadith_id)"
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_hadith_trans_col_lang ON hadith_translations(collection_id, language)"
                )

                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS hadith_grades (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hadith_id INTEGER NOT NULL,
                        stable_id TEXT NOT NULL,
                        authority TEXT NOT NULL,
                        grade TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_hadith_grades_hadith_id ON hadith_grades(hadith_id)"
                )
                database.executeSql(
                    "CREATE INDEX IF NOT EXISTS idx_hadith_grades_stable_id ON hadith_grades(stable_id)"
                )

                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS hadith_source_manifests (
                        source_id TEXT NOT NULL PRIMARY KEY,
                        source_uri TEXT NOT NULL,
                        revision TEXT NOT NULL,
                        archive_sha256 TEXT NOT NULL,
                        license_id TEXT NOT NULL,
                        active_record_count INTEGER NOT NULL,
                        missing_slot_count INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        updated_at_epoch_ms INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                database.executeSql(
                    """
                    INSERT OR IGNORE INTO hadith_references(
                        hadith_id, stable_id, collection_id, scheme_id,
                        reference_number, variant_suffix, book_number, hadith_in_book,
                        canonical_url, is_primary, review_status
                    )
                    SELECT
                        id, stable_id, collection_id, scheme_id,
                        hadith_number, '', 0, 0,
                        canonical_url, 1, 'verified'
                    FROM hadiths
                    WHERE length(trim(stable_id)) > 0
                    """.trimIndent()
                )

                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_insert")
                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_delete")
                database.executeSql("DROP TRIGGER IF EXISTS ayahs_fts5_after_update")
                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_insert")
                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_delete")
                database.executeSql("DROP TRIGGER IF EXISTS hadiths_fts5_after_update")
                createFts5(database)
                createRetrievalFts5(database)
            }
        }


        private fun createFts5(database: SupportSQLiteDatabase) {
            fts5Statements().forEach(database::execSQL)
            retrievalFts5Statements().forEach(database::execSQL)
        }

        private fun createFts5(database: SQLiteConnection) {
            fts5Statements().forEach { sql -> database.executeSql(sql) }
        }

        private fun createRetrievalFts5(database: SQLiteConnection) {
            retrievalFts5Statements().forEach { sql -> database.executeSql(sql) }
        }

        private fun retrievalFts5Statements(): List<String> = listOf(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS hadiths_fts5 USING fts5(
                collection_id,
                title,
                text_arabic,
                text_arabic_normalized,
                translation_id,
                translation_en,
                reference,
                tokenize = 'unicode61 remove_diacritics 2'
            )
            """.trimIndent(),
            "DELETE FROM hadiths_fts5",
            """
            INSERT INTO hadiths_fts5(
                rowid, collection_id, title, text_arabic, text_arabic_normalized,
                translation_id, translation_en, reference
            )
            SELECT id, collection_id, title, text_arabic,
                ${arabicSearchExpression("text_arabic")}, translation_id, translation_en, reference
            FROM hadiths
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS hadiths_fts5_after_insert
            AFTER INSERT ON hadiths BEGIN
                INSERT INTO hadiths_fts5(
                    rowid, collection_id, title, text_arabic, text_arabic_normalized,
                    translation_id, translation_en, reference
                ) VALUES (
                    new.id, new.collection_id, new.title, new.text_arabic,
                    ${arabicSearchExpression("new.text_arabic")},
                    new.translation_id, new.translation_en, new.reference
                );
            END
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS hadiths_fts5_after_delete
            AFTER DELETE ON hadiths BEGIN
                DELETE FROM hadiths_fts5 WHERE rowid = old.id;
            END
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS hadiths_fts5_after_update
            AFTER UPDATE ON hadiths BEGIN
                DELETE FROM hadiths_fts5 WHERE rowid = old.id;
                INSERT INTO hadiths_fts5(
                    rowid, collection_id, title, text_arabic, text_arabic_normalized,
                    translation_id, translation_en, reference
                ) VALUES (
                    new.id, new.collection_id, new.title, new.text_arabic,
                    ${arabicSearchExpression("new.text_arabic")},
                    new.translation_id, new.translation_en, new.reference
                );
            END
            """.trimIndent(),
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS knowledge_chunks_fts5 USING fts5(
                source_type,
                source_id,
                title,
                text_content,
                tokenize = 'unicode61 remove_diacritics 2'
            )
            """.trimIndent(),
            "DELETE FROM knowledge_chunks_fts5",
            """
            INSERT INTO knowledge_chunks_fts5(rowid, source_type, source_id, title, text_content)
            SELECT id, source_type, source_id, title, text_content
            FROM knowledge_chunks
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS knowledge_chunks_fts5_after_insert
            AFTER INSERT ON knowledge_chunks BEGIN
                INSERT INTO knowledge_chunks_fts5(
                    rowid, source_type, source_id, title, text_content
                ) VALUES (new.id, new.source_type, new.source_id, new.title, new.text_content);
            END
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS knowledge_chunks_fts5_after_delete
            AFTER DELETE ON knowledge_chunks BEGIN
                DELETE FROM knowledge_chunks_fts5 WHERE rowid = old.id;
            END
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS knowledge_chunks_fts5_after_update
            AFTER UPDATE ON knowledge_chunks BEGIN
                DELETE FROM knowledge_chunks_fts5 WHERE rowid = old.id;
                INSERT INTO knowledge_chunks_fts5(
                    rowid, source_type, source_id, title, text_content
                ) VALUES (new.id, new.source_type, new.source_id, new.title, new.text_content);
            END
            """.trimIndent()
        )

        private fun SQLiteConnection.executeSql(sql: String) {
            prepare(sql).use { it.step() }
        }

        private fun hasColumn(
            database: SQLiteConnection,
            tableName: String,
            columnName: String
        ): Boolean {
            database.prepare("PRAGMA table_info($tableName)").use { statement ->
                while (statement.step()) {
                    if (statement.getText(1) == columnName) return true
                }
            }
            return false
        }

        private fun fts5Statements(): List<String> {
            val arabicText = "text_arabic"
            val newArabicText = "new.text_arabic"
            val oldArabicText = "old.text_arabic"
            return listOf(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS ayahs_fts5 USING fts5(
                    translation_id,
                    translation_en,
                    transliteration,
                    text_arabic,
                    text_arabic_normalized
                )
                """.trimIndent(),
            "DELETE FROM ayahs_fts5",
                """
                INSERT INTO ayahs_fts5(rowid, translation_id, translation_en, transliteration, text_arabic, text_arabic_normalized)
                SELECT id, translation_id, translation_en, transliteration, text_arabic, ${arabicSearchExpression(arabicText)} FROM ayahs
                """.trimIndent(),
                """
                CREATE TRIGGER IF NOT EXISTS ayahs_fts5_after_insert
                AFTER INSERT ON ayahs BEGIN
                    INSERT INTO ayahs_fts5(rowid, translation_id, translation_en, transliteration, text_arabic, text_arabic_normalized)
                    VALUES (new.id, new.translation_id, new.translation_en, new.transliteration, new.text_arabic, ${arabicSearchExpression(newArabicText)});
                END
                """.trimIndent(),
                """
                CREATE TRIGGER IF NOT EXISTS ayahs_fts5_after_delete
                AFTER DELETE ON ayahs BEGIN
                    DELETE FROM ayahs_fts5 WHERE rowid = old.id;
                END
                """.trimIndent(),
                """
                CREATE TRIGGER IF NOT EXISTS ayahs_fts5_after_update
                AFTER UPDATE ON ayahs BEGIN
                    DELETE FROM ayahs_fts5 WHERE rowid = old.id;
                    INSERT INTO ayahs_fts5(rowid, translation_id, translation_en, transliteration, text_arabic, text_arabic_normalized)
                    VALUES (new.id, new.translation_id, new.translation_en, new.transliteration, new.text_arabic, ${arabicSearchExpression(newArabicText)});
                END
                """.trimIndent()
            )
        }

        private fun arabicSearchExpression(column: String): String {
            var expression = column
            expression = "replace($expression, char(1600), '')"
            expression = "replace($expression, char(1649), char(1575))"
            expression = "replace($expression, char(1570), char(1575))"
            expression = "replace($expression, char(1571), char(1575))"
            expression = "replace($expression, char(1573), char(1575))"
            for (code in 1611..1648) expression = "replace($expression, char($code), '')"
            for (code in 1750..1773) expression = "replace($expression, char($code), '')"
            return expression
        }

        internal fun migrationsThrough14(): Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
            MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            MIGRATION_13_14
        )

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SQLiteConnection) {
                database.executeSql(
                    """
                    CREATE TABLE IF NOT EXISTS hadith_scheme_stats (
                        collection_id TEXT NOT NULL,
                        scheme_id TEXT NOT NULL,
                        scheme_label TEXT NOT NULL,
                        record_count INTEGER NOT NULL,
                        min_reference_number INTEGER NOT NULL,
                        max_reference_number INTEGER NOT NULL,
                        missing_slot_count INTEGER NOT NULL DEFAULT 0,
                        is_default INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(collection_id, scheme_id)
                    )
                    """.trimIndent()
                )
                database.executeSql(
                    """
                    INSERT OR REPLACE INTO hadith_scheme_stats(
                        collection_id, scheme_id, scheme_label,
                        record_count, min_reference_number, max_reference_number,
                        missing_slot_count, is_default
                    )
                    SELECT
                        r.collection_id,
                        r.scheme_id,
                        CASE r.scheme_id
                            WHEN 'sunnah' THEN 'No. Sunnah.com'
                            WHEN 'ar_risalah' THEN 'No. Ar-Risalah'
                            WHEN 'archive_gading' THEN 'No. Arsip'
                            ELSE r.scheme_id
                        END,
                        COUNT(DISTINCT CASE WHEN r.hadith_id IS NOT NULL THEN r.stable_id END),
                        COALESCE(MIN(CASE WHEN r.hadith_id IS NOT NULL THEN r.reference_number END), 0),
                        COALESCE(MAX(CASE WHEN r.hadith_id IS NOT NULL THEN r.reference_number END), 0),
                        COUNT(DISTINCT CASE WHEN r.hadith_id IS NULL THEN r.stable_id END),
                        CASE WHEN r.scheme_id = COALESCE(c.default_scheme, 'sunnah') THEN 1 ELSE 0 END
                    FROM hadith_references r
                    LEFT JOIN hadith_collections c ON c.id = r.collection_id
                    GROUP BY r.collection_id, r.scheme_id
                    """.trimIndent()
                )
            }
        }

        internal fun migrationsThrough15(): Array<Migration> =
            migrationsThrough14() + MIGRATION_14_15

        internal fun allMigrations(): Array<Migration> =
            migrationsThrough15() + MIGRATION_15_16

        private val FTS_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(database: SupportSQLiteDatabase) {
                createFts5(database)
            }

            override fun onCreate(database: SQLiteConnection) {
                createFts5(database)
                createRetrievalFts5(database)
            }
        }
    }
}
