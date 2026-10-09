package com.quranplus.app.core.database

import com.quranplus.app.core.database.dao.BookmarkDao
import com.quranplus.app.core.database.entity.BookmarkEntity
import com.quranplus.app.features.rag.data.SafAssetStore
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.shared.features.quran.domain.HadithMarker
import com.quranplus.shared.features.quran.domain.QuranMarker
import com.quranplus.shared.features.quran.domain.HadithBookmark
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

    suspend fun exportHadithBookmarksToSaf(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            runCatching {
                val bookmarks = preferencesManager.hadithBookmarks.firstOrNull() ?: emptyList()
                val array = JSONArray()
                bookmarks.forEach { b ->
                    val obj = JSONObject().apply {
                        put("id", b.id)
                        put("collectionId", b.collectionId)
                        put("collectionName", b.collectionName)
                        put("hadithNumber", b.hadithNumber)
                        put("hadithTextArabic", b.hadithTextArabic)
                        put("hadithTranslation", b.hadithTranslation)
                        put("note", b.note ?: "")
                        put("timestamp", b.timestamp)
                    }
                    array.put(obj)
                }
                safAssetStore.publishText(
                    text = array.toString(2),
                    relativeDirectory = "userdata",
                    filename = "hadith_bookmarks.json"
                )
                true
            }.getOrDefault(false)
        }
    }

    suspend fun importHadithBookmarksFromSaf(): Int = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            runCatching {
                val jsonText = safAssetStore.readText("userdata", "hadith_bookmarks.json") ?: return@withLock 0
                val array = JSONArray(jsonText)
                val current = preferencesManager.hadithBookmarks.firstOrNull() ?: emptyList()
                val currentMap = current.associateBy { "${it.collectionId}:${it.hadithNumber}" }.toMutableMap()
                var restoredCount = 0

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val collectionId = obj.getString("collectionId")
                    val collectionName = obj.optString("collectionName", "")
                    val hadithNumber = obj.getInt("hadithNumber")
                    val hadithTextArabic = obj.optString("hadithTextArabic", "")
                    val hadithTranslation = obj.optString("hadithTranslation", "")
                    val note = obj.optString("note", "").takeIf { it.isNotBlank() }
                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    val key = "$collectionId:$hadithNumber"

                    val existing = currentMap[key]
                    if (existing == null) {
                        currentMap[key] = HadithBookmark(
                            id = obj.optLong("id", currentMap.size.toLong() + 1),
                            collectionId = collectionId,
                            collectionName = collectionName,
                            hadithNumber = hadithNumber,
                            hadithTextArabic = hadithTextArabic,
                            hadithTranslation = hadithTranslation,
                            note = note,
                            timestamp = timestamp
                        )
                        restoredCount++
                    } else if (existing.note.isNullOrBlank() && !note.isNullOrBlank()) {
                        currentMap[key] = existing.copy(note = note)
                    }
                }

                if (restoredCount > 0 || currentMap.size > current.size) {
                    preferencesManager.setHadithBookmarks(currentMap.values.toList())
                }
                restoredCount
            }.getOrDefault(0)
        }
    }

    suspend fun exportMarkersToSaf(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            runCatching {
                val qMarkers = preferencesManager.quranMarkers.firstOrNull() ?: emptyList()
                val hMarkers = preferencesManager.hadithMarkers.firstOrNull() ?: emptyList()
                val root = JSONObject()

                val qArray = JSONArray()
                qMarkers.forEach { m ->
                    qArray.put(JSONObject().apply {
                        put("surahNumber", m.surahNumber)
                        put("surahName", m.surahName)
                        put("ayahNumber", m.ayahNumber)
                        put("timestamp", m.timestamp)
                        put("colorIndex", m.colorIndex)
                    })
                }
                root.put("quranMarkers", qArray)

                val hArray = JSONArray()
                hMarkers.forEach { m ->
                    hArray.put(JSONObject().apply {
                        put("collectionId", m.collectionId)
                        put("collectionName", m.collectionName)
                        put("hadithNumber", m.hadithNumber)
                        put("timestamp", m.timestamp)
                        put("colorIndex", m.colorIndex)
                    })
                }
                root.put("hadithMarkers", hArray)

                // Legacy fallback objects
                qMarkers.firstOrNull()?.let { q ->
                    root.put("quran", JSONObject().apply {
                        put("surahNumber", q.surahNumber)
                        put("surahName", q.surahName)
                        put("ayahNumber", q.ayahNumber)
                        put("timestamp", q.timestamp)
                        put("colorIndex", q.colorIndex)
                    })
                }
                hMarkers.firstOrNull()?.let { h ->
                    root.put("hadith", JSONObject().apply {
                        put("collectionId", h.collectionId)
                        put("collectionName", h.collectionName)
                        put("hadithNumber", h.hadithNumber)
                        put("timestamp", h.timestamp)
                        put("colorIndex", h.colorIndex)
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

                val restoredQMarkers = mutableListOf<QuranMarker>()
                if (root.has("quranMarkers") && !root.isNull("quranMarkers")) {
                    val array = root.getJSONArray("quranMarkers")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        restoredQMarkers.add(
                            QuranMarker(
                                surahNumber = obj.getInt("surahNumber"),
                                surahName = obj.getString("surahName"),
                                ayahNumber = obj.getInt("ayahNumber"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                colorIndex = obj.optInt("colorIndex", i)
                            )
                        )
                    }
                } else if (root.has("quran") && !root.isNull("quran")) {
                    val qObj = root.getJSONObject("quran")
                    restoredQMarkers.add(
                        QuranMarker(
                            surahNumber = qObj.getInt("surahNumber"),
                            surahName = qObj.getString("surahName"),
                            ayahNumber = qObj.getInt("ayahNumber"),
                            timestamp = qObj.optLong("timestamp", System.currentTimeMillis()),
                            colorIndex = qObj.optInt("colorIndex", 0)
                        )
                    )
                }
                if (restoredQMarkers.isNotEmpty()) {
                    preferencesManager.setQuranMarkers(restoredQMarkers)
                }

                val restoredHMarkers = mutableListOf<HadithMarker>()
                if (root.has("hadithMarkers") && !root.isNull("hadithMarkers")) {
                    val array = root.getJSONArray("hadithMarkers")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        restoredHMarkers.add(
                            HadithMarker(
                                collectionId = obj.getString("collectionId"),
                                collectionName = obj.getString("collectionName"),
                                hadithNumber = obj.getInt("hadithNumber"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                colorIndex = obj.optInt("colorIndex", i)
                            )
                        )
                    }
                } else if (root.has("hadith") && !root.isNull("hadith")) {
                    val hObj = root.getJSONObject("hadith")
                    restoredHMarkers.add(
                        HadithMarker(
                            collectionId = hObj.getString("collectionId"),
                            collectionName = hObj.getString("collectionName"),
                            hadithNumber = hObj.getInt("hadithNumber"),
                            timestamp = hObj.optLong("timestamp", System.currentTimeMillis()),
                            colorIndex = hObj.optInt("colorIndex", 0)
                        )
                    )
                }
                if (restoredHMarkers.isNotEmpty()) {
                    preferencesManager.setHadithMarkers(restoredHMarkers)
                }

                restoredQMarkers.isNotEmpty() || restoredHMarkers.isNotEmpty()
            }.getOrDefault(false)
        }
    }

    suspend fun restoreFromSaf(): Boolean = withContext(Dispatchers.IO) {
        val bookmarksRestored = importBookmarksFromSaf()
        val hadithBookmarksRestored = importHadithBookmarksFromSaf()
        val markersRestored = importMarkersToSaf()
        if (bookmarksRestored == 0) {
            val localBookmarks = bookmarkDao.getAllBookmarks().first()
            if (localBookmarks.isNotEmpty()) {
                exportBookmarksToSaf()
            }
        }
        if (hadithBookmarksRestored == 0) {
            val localHadithBookmarks = preferencesManager.hadithBookmarks.firstOrNull() ?: emptyList()
            if (localHadithBookmarks.isNotEmpty()) {
                exportHadithBookmarksToSaf()
            }
        }
        if (!markersRestored) {
            val qMarkers = preferencesManager.quranMarkers.firstOrNull() ?: emptyList()
            val hMarkers = preferencesManager.hadithMarkers.firstOrNull() ?: emptyList()
            if (qMarkers.isNotEmpty() || hMarkers.isNotEmpty()) {
                exportMarkersToSaf()
            }
        }
        true
    }
}
