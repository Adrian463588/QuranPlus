package com.quranplus.app.core.database

import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.entity.BookmarkEntity
import com.quranplus.app.features.rag.data.SafAssetStore
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.shared.features.quran.domain.HadithMarker
import com.quranplus.shared.features.quran.domain.QuranMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages backup and restore of user-created data (bookmarks & reading markers)
 * in the linked SAF folder (under userdata/bookmarks.json and userdata/markers.json).
 */
class SafUserDataManager(
    private val bookmarkDao: BookmarkDao,
    private val safAssetStore: SafAssetStore,
    private val preferencesManager: PreferencesManager
) {
    private val syncMutex = Mutex()

    suspend fun exportBookmarksToSaf(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            runCatching {
                val bookmarks = bookmarkDao.getAllBookmarks().first()
                val array = JSONArray()
                bookmarks.forEach { b ->
                    val obj = JSONObject().apply {
                        put("surahId", b.surahId)
                        put("surahName", b.surahName)
                        put("ayahNumber", b.ayahNumber)
                        put("ayahTextArabic", b.ayahTextArabic)
                        put("ayahTranslation", b.ayahTranslation)
                        put("note", b.note ?: "")
                        put("timestamp", b.timestamp)
                    }
                    array.put(obj)
                }
                safAssetStore.publishText(
                    text = array.toString(2),
                    relativeDirectory = "userdata",
                    filename = "bookmarks.json"
                )
                true
            }.getOrDefault(false)
        }
    }

    suspend fun importBookmarksFromSaf(): Int = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            runCatching {
                val jsonText = safAssetStore.readText("userdata", "bookmarks.json") ?: return@withLock 0
                val array = JSONArray(jsonText)
                var restoredCount = 0
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val surahId = obj.getInt("surahId")
                    val ayahNumber = obj.getInt("ayahNumber")
                    val surahName = obj.optString("surahName", "")
                    val ayahTextArabic = obj.optString("ayahTextArabic", "")
                    val ayahTranslation = obj.optString("ayahTranslation", "")
                    val note = obj.optString("note", "").takeIf { it.isNotBlank() }
                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                    val existing = bookmarkDao.getBookmark(surahId, ayahNumber)
                    if (existing == null) {
                        bookmarkDao.insertBookmark(
                            BookmarkEntity(
                                surahId = surahId,
                                surahName = surahName,
                                ayahNumber = ayahNumber,
                                ayahTextArabic = ayahTextArabic,
                                ayahTranslation = ayahTranslation,
                                note = note,
                                timestamp = timestamp
                            )
                        )
                        restoredCount++
                    } else if (existing.note.isNullOrBlank() && !note.isNullOrBlank()) {
                        bookmarkDao.updateNote(existing.id, note)
                    }
                }
                restoredCount
            }.getOrDefault(0)
        }
    }

    suspend fun exportMarkersToSaf(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            runCatching {
                val qMarker = preferencesManager.quranMarker.firstOrNull()
                val hMarker = preferencesManager.hadithMarker.firstOrNull()
                val root = JSONObject()
                if (qMarker != null) {
                    root.put("quran", JSONObject().apply {
                        put("surahNumber", qMarker.surahNumber)
                        put("surahName", qMarker.surahName)
                        put("ayahNumber", qMarker.ayahNumber)
                        put("timestamp", qMarker.timestamp)
                    })
                }
                if (hMarker != null) {
                    root.put("hadith", JSONObject().apply {
                        put("collectionId", hMarker.collectionId)
                        put("collectionName", hMarker.collectionName)
                        put("hadithNumber", hMarker.hadithNumber)
                        put("timestamp", hMarker.timestamp)
                    })
                }
                safAssetStore.publishText(
                    text = root.toString(2),
                    relativeDirectory = "userdata",
                    filename = "markers.json"
                )
                true
            }.getOrDefault(false)
        }
    }

    suspend fun importMarkersToSaf(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            runCatching {
                val jsonText = safAssetStore.readText("userdata", "markers.json") ?: return@withLock false
                val root = JSONObject(jsonText)
                if (root.has("quran") && !root.isNull("quran")) {
                    val qObj = root.getJSONObject("quran")
                    val qMarker = QuranMarker(
                        surahNumber = qObj.getInt("surahNumber"),
                        surahName = qObj.getString("surahName"),
                        ayahNumber = qObj.getInt("ayahNumber"),
                        timestamp = qObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    preferencesManager.setQuranMarker(qMarker)
                }
                if (root.has("hadith") && !root.isNull("hadith")) {
                    val hObj = root.getJSONObject("hadith")
                    val hMarker = HadithMarker(
                        collectionId = hObj.getString("collectionId"),
                        collectionName = hObj.getString("collectionName"),
                        hadithNumber = hObj.getInt("hadithNumber"),
                        timestamp = hObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    preferencesManager.setHadithMarker(hMarker)
                }
                true
            }.getOrDefault(false)
        }
    }

    suspend fun restoreFromSaf(): Boolean = withContext(Dispatchers.IO) {
        val bookmarksRestored = importBookmarksFromSaf()
        val markersRestored = importMarkersToSaf()
        if (bookmarksRestored == 0) {
            val localBookmarks = bookmarkDao.getAllBookmarks().first()
            if (localBookmarks.isNotEmpty()) {
                exportBookmarksToSaf()
            }
        }
        if (!markersRestored) {
            val qMarker = preferencesManager.quranMarker.firstOrNull()
            val hMarker = preferencesManager.hadithMarker.firstOrNull()
            if (qMarker != null || hMarker != null) {
                exportMarkersToSaf()
            }
        }
        true
    }
}
