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
import com.quranplus.app.core.database.entity.KnowledgeChunkEntity
import com.quranplus.app.core.database.entity.LastReadEntity
import com.quranplus.app.core.database.entity.SurahEntity
import com.quranplus.app.core.database.entity.TahsinLessonEntity
import com.quranplus.app.core.database.entity.QuizAttemptEntity
import com.quranplus.app.core.database.entity.QuizQuestionEntity
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
        KnowledgeChunkEntity::class,
        ChatMessageEntity::class,
        QuizQuestionEntity::class,
        QuizAttemptEntity::class
    ],
    version = 9,
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
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
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

        private fun createFts5(database: SupportSQLiteDatabase) {
            fts5Statements().forEach(database::execSQL)
        }

        private fun createFts5(database: SQLiteConnection) {
            fts5Statements().forEach { sql -> database.executeSql(sql) }
        }

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
                    INSERT INTO ayahs_fts5(ayahs_fts5, rowid, translation_id, translation_en, transliteration, text_arabic, text_arabic_normalized)
                    VALUES ('delete', old.id, old.translation_id, old.translation_en, old.transliteration, old.text_arabic, ${arabicSearchExpression(oldArabicText)});
                END
                """.trimIndent(),
                """
                CREATE TRIGGER IF NOT EXISTS ayahs_fts5_after_update
                AFTER UPDATE ON ayahs BEGIN
                    INSERT INTO ayahs_fts5(ayahs_fts5, rowid, translation_id, translation_en, transliteration, text_arabic, text_arabic_normalized)
                    VALUES ('delete', old.id, old.translation_id, old.translation_en, old.transliteration, old.text_arabic, ${arabicSearchExpression(oldArabicText)});
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
            for (code in 1611..1648) expression = "replace($expression, char($code), '')"
            for (code in 1750..1773) expression = "replace($expression, char($code), '')"
            return expression
        }

        private val FTS_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(database: SupportSQLiteDatabase) {
                createFts5(database)
            }

            override fun onCreate(database: SQLiteConnection) {
                createFts5(database)
            }
        }
    }
}
