package com.quranplus.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        BookmarkEntity::class,
        LastReadEntity::class,
        TahsinLessonEntity::class,
        HadithEntity::class,
        KnowledgeChunkEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
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
            val builder = Room.databaseBuilder(
                context.applicationContext,
                QuranDatabase::class.java,
                DB_NAME
            )
            // Check if prepackaged database asset exists
            val hasAsset = try {
                context.assets.open("databases/$DB_NAME").close()
                true
            } catch (e: Exception) {
                false
            }

            if (hasAsset) {
                builder.createFromAsset("databases/$DB_NAME")
            }

            return builder
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createFts5(database)
            }
        }

        private fun createFts5(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS ayahs_fts5 USING fts5(
                    rowid UNINDEXED,
                    translation_id,
                    translation_en,
                    transliteration,
                    text_arabic
                )
                """.trimIndent()
            )
            database.execSQL("DELETE FROM ayahs_fts5")
            database.execSQL(
                """
                INSERT INTO ayahs_fts5(rowid, translation_id, translation_en, transliteration, text_arabic)
                SELECT id, translation_id, translation_en, transliteration, text_arabic FROM ayahs
                """.trimIndent()
            )
        }
    }
}
