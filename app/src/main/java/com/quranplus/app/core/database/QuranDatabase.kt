package com.quranplus.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.dao.ChatDao
import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.core.database.dao.KnowledgeChunkDao
import com.quranplus.app.core.database.dao.LastReadDao
import com.quranplus.app.core.database.dao.QuranDao
import com.quranplus.app.core.database.dao.TahsinDao
import com.quranplus.app.core.database.entity.AyahEntity
import com.quranplus.app.core.database.entity.BookmarkEntity
import com.quranplus.app.core.database.entity.ChatMessageEntity
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.core.database.entity.KnowledgeChunkEntity
import com.quranplus.app.core.database.entity.LastReadEntity
import com.quranplus.app.core.database.entity.SurahEntity
import com.quranplus.app.core.database.entity.TahsinLessonEntity
import com.quranplus.app.core.database.entity.QuizAttemptEntity
import com.quranplus.app.core.database.entity.QuizQuestionEntity

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        BookmarkEntity::class,
        LastReadEntity::class,
        TahsinLessonEntity::class,
        HadithEntity::class,
        KnowledgeChunkEntity::class,
        ChatMessageEntity::class,
        QuizQuestionEntity::class,
        QuizAttemptEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun lastReadDao(): LastReadDao
    abstract fun tahsinDao(): TahsinDao
    abstract fun hadithDao(): HadithDao
    abstract fun knowledgeChunkDao(): KnowledgeChunkDao
    abstract fun chatDao(): ChatDao
    abstract fun quizDao(): com.quranplus.app.core.database.dao.QuizDao

    companion object {
        private const val DB_NAME = "quranplus.db"

        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getInstance(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): QuranDatabase {
            copyPrepackagedAsset(context)
            val builder = Room.databaseBuilder(
                context.applicationContext,
                QuranDatabase::class.java,
                DB_NAME
            )

            return builder
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addCallback(FTS_CALLBACK)
                .build()
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

        private fun createFts5(database: SupportSQLiteDatabase) {
            fts5Statements().forEach(database::execSQL)
        }

        private fun createFts5(database: SQLiteConnection) {
            fts5Statements().forEach { sql -> database.executeSql(sql) }
        }

        private fun SQLiteConnection.executeSql(sql: String) {
            prepare(sql).use { it.step() }
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
