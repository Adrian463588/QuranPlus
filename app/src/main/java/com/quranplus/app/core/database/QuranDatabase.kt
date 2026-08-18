package com.quranplus.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.dao.ChatDao
import com.quranplus.app.core.database.dao.HadithDao
import com.quranplus.app.core.database.dao.KnowledgeChunkDao
import com.quranplus.app.core.database.dao.LastReadDao
import com.quranplus.app.core.database.dao.QuranDao
import com.quranplus.app.core.database.dao.TahsinDao
import com.quranplus.app.core.database.entity.AyahEntity
import com.quranplus.app.core.database.entity.AyahFtsEntity
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
        AyahFtsEntity::class,
        BookmarkEntity::class,
        LastReadEntity::class,
        TahsinLessonEntity::class,
        HadithEntity::class,
        KnowledgeChunkEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
