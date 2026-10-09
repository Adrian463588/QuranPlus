package com.quranplus.app.features.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.quranplus.shared.features.quran.domain.QuranMarker
import com.quranplus.shared.features.quran.domain.HadithMarker
import com.quranplus.shared.features.quran.domain.HadithBookmark
import com.quranplus.app.core.ui.theme.QuranColors
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quranplus_settings")

enum class TranslationMode(val id: String, val label: String) {
    INDONESIAN("id", "Indonesia"),
    ENGLISH("en", "English"),
    BOTH("both", "Keduanya");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: ENGLISH
    }
}



enum class AiPersona(val id: String, val title: String, val description: String, val defaultPrompt: String) {
    MUFTI(
        id = "mufti",
        title = "Mufti",
        description = "Formal, mengedepankan dalil shahih Quran dan Sunnah secara terstruktur",
        defaultPrompt = """
            Anda adalah seorang Mufti dan pakar studi Islam yang bijaksana, berlandaskan Al-Quran dan As-Sunnah Ash-Shahihah.
            Setiap jawaban harus:
            1. Menyertakan dalil jelas (Ayat Quran atau Hadith Sahih).
            2. Menjelaskan konteks dan hikmah syariat dengan bahasa yang bermartabat dan terstruktur.
            3. Menghindari spekulasi tanpa dasar rujukan yang sahih.
        """.trimIndent()
    ),
    USTADZ(
        id = "ustadz",
        title = "Ustadz",
        description = "Edukatif, ramah, dan menjelaskan tahapan pemahaman agama dengan mudah",
        defaultPrompt = """
            Anda adalah seorang Ustadz pendidik yang penuh empati dan sabar dalam menjelaskan ajaran Islam.
            Jelaskan permasalahan agama langkah demi langkah dengan rujukan Al-Quran dan Sunnah, serta aplikasinya dalam kehidupan sehari-hari dengan bahasa yang hangat dan mudah dipahami.
        """.trimIndent()
    ),
    SAHABAT(
        id = "sahabat",
        title = "Sahabat",
        description = "Conversational, santai, dan mengajak pada kebaikan",
        defaultPrompt = """
            Anda adalah seorang sahabat diskusi Islami yang ramah, santai, namun tetap berpegang teguh pada kebenaran Quran dan Sunnah. Berbicaralah dengan gaya santun, menyemangati, dan menyejukkan hati.
        """.trimIndent()
    ),
    CUSTOM(
        id = "custom",
        title = "Custom",
        description = "Persona kustom yang disesuaikan dengan kebutuhan Anda",
        defaultPrompt = "Anda adalah asisten AI Islami yang berlandaskan Al-Quran dan Sunnah."
    );

    companion object {
        fun fromId(id: String): AiPersona {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: USTADZ
        }
    }
}

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ARABIC_FONT_SIZE = floatPreferencesKey("arabic_font_size")
        val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val ENABLE_TAJWID = booleanPreferencesKey("enable_tajwid")
        val SELECTED_PERSONA = stringPreferencesKey("selected_persona")
        val CUSTOM_SYSTEM_PROMPT = stringPreferencesKey("custom_system_prompt")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val SELECTED_EMBEDDING_MODEL = stringPreferencesKey("selected_embedding_model")
        val TRANSLATION_MODE = stringPreferencesKey("translation_mode")
        val SAF_ROOT_URI = stringPreferencesKey("saf_root_uri")
        val ONLINE_RESEARCH_ENABLED = booleanPreferencesKey("online_research_enabled")
        val SEARCH_HISTORY = stringPreferencesKey("quran_search_history")
        val QURAN_MARKER = stringPreferencesKey("quran_marker_data")
        val HADITH_MARKER = stringPreferencesKey("hadith_marker_data")
        val QURAN_MARKERS = stringPreferencesKey("quran_markers_list_data")
        val HADITH_MARKERS = stringPreferencesKey("hadith_markers_list_data")
        val HADITH_BOOKMARKS = stringPreferencesKey("hadith_bookmarks_list_data")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: true // Default dark mode per DESIGN.md
    }

    val arabicFontSize: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ARABIC_FONT_SIZE] ?: 28f
    }

    val showTransliteration: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_TRANSLITERATION] ?: true
    }

    val showTranslation: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_TRANSLATION] ?: true
    }

    val enableTajwid: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ENABLE_TAJWID] ?: true
    }

    val selectedPersona: Flow<AiPersona> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.SELECTED_PERSONA] ?: AiPersona.USTADZ.id
        AiPersona.fromId(id)
    }

    val customSystemPrompt: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_SYSTEM_PROMPT] ?: AiPersona.CUSTOM.defaultPrompt
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_MODEL].orEmpty()
    }

    val selectedEmbeddingModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_EMBEDDING_MODEL] ?: "all-minilm-l6-v2-onnx"
    }

    val translationMode: Flow<TranslationMode> = context.dataStore.data.map { preferences ->
        TranslationMode.fromId(preferences[PreferencesKeys.TRANSLATION_MODE] ?: TranslationMode.ENGLISH.id)
    }

    val safRootUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAF_ROOT_URI]
    }

    /** Allows a transparent internet fallback only when local retrieval is insufficient. */
    val onlineResearchEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONLINE_RESEARCH_ENABLED] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    suspend fun setArabicFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARABIC_FONT_SIZE] = size.coerceIn(18f, 48f)
        }
    }

    suspend fun setShowTransliteration(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TRANSLITERATION] = show
        }
    }

    suspend fun setShowTranslation(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TRANSLATION] = show
        }
    }

    suspend fun setEnableTajwid(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_TAJWID] = enabled
        }
    }

    suspend fun setSelectedPersona(persona: AiPersona) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_PERSONA] = persona.id
        }
    }

    suspend fun setCustomSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun setSelectedModel(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODEL] = modelName
        }
    }

    suspend fun setSelectedEmbeddingModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_EMBEDDING_MODEL] = modelId
        }
    }

    suspend fun setTranslationMode(mode: TranslationMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRANSLATION_MODE] = mode.id
        }
    }

    suspend fun setSafRootUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAF_ROOT_URI] = uri
        }
    }

    suspend fun clearSafRootUri() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SAF_ROOT_URI)
        }
    }

    suspend fun setOnlineResearchEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONLINE_RESEARCH_ENABLED] = enabled
        }
    }

    val searchHistory: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.SEARCH_HISTORY].orEmpty()
        if (raw.isBlank()) emptyList()
        else raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    suspend fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SEARCH_HISTORY].orEmpty()
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(trimmed, ignoreCase = true) }
            val updated = (listOf(trimmed) + current).take(15)
            preferences[PreferencesKeys.SEARCH_HISTORY] = updated.joinToString("\n")
        }
    }

    suspend fun removeSearchHistory(query: String) {
        val trimmed = query.trim()
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SEARCH_HISTORY].orEmpty()
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(trimmed, ignoreCase = true) }
            preferences[PreferencesKeys.SEARCH_HISTORY] = current.joinToString("\n")
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SEARCH_HISTORY)
        }
    }

    val quranMarkers: Flow<List<QuranMarker>> = context.dataStore.data.map { preferences ->
        val rawList = preferences[PreferencesKeys.QURAN_MARKERS]
        if (!rawList.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(rawList)
                (0 until array.length()).mapNotNull { i ->
                    val json = array.optJSONObject(i) ?: return@mapNotNull null
                    QuranMarker(
                        surahNumber = json.getInt("surahNumber"),
                        surahName = json.getString("surahName"),
                        ayahNumber = json.getInt("ayahNumber"),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        colorIndex = json.optInt("colorIndex", i.coerceAtMost(QuranColors.MAX_READING_MARKERS - 1))
                    )
                }
            }.getOrDefault(emptyList())
        } else {
            val rawSingle = preferences[PreferencesKeys.QURAN_MARKER] ?: return@map emptyList()
            runCatching {
                val json = JSONObject(rawSingle)
                listOf(
                    QuranMarker(
                        surahNumber = json.getInt("surahNumber"),
                        surahName = json.getString("surahName"),
                        ayahNumber = json.getInt("ayahNumber"),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        colorIndex = json.optInt("colorIndex", 0)
                    )
                )
            }.getOrDefault(emptyList())
        }
    }

    val quranMarker: Flow<QuranMarker?> = quranMarkers.map { it.firstOrNull() }

    val hadithMarkers: Flow<List<HadithMarker>> = context.dataStore.data.map { preferences ->
        val rawList = preferences[PreferencesKeys.HADITH_MARKERS]
        if (!rawList.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(rawList)
                (0 until array.length()).mapNotNull { i ->
                    val json = array.optJSONObject(i) ?: return@mapNotNull null
                    HadithMarker(
                        collectionId = json.getString("collectionId"),
                        collectionName = json.getString("collectionName"),
                        hadithNumber = json.getInt("hadithNumber"),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        colorIndex = json.optInt("colorIndex", i.coerceAtMost(QuranColors.MAX_READING_MARKERS - 1))
                    )
                }
            }.getOrDefault(emptyList())
        } else {
            val rawSingle = preferences[PreferencesKeys.HADITH_MARKER] ?: return@map emptyList()
            runCatching {
                val json = JSONObject(rawSingle)
                listOf(
                    HadithMarker(
                        collectionId = json.getString("collectionId"),
                        collectionName = json.getString("collectionName"),
                        hadithNumber = json.getInt("hadithNumber"),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        colorIndex = json.optInt("colorIndex", 0)
                    )
                )
            }.getOrDefault(emptyList())
        }
    }

    val hadithMarker: Flow<HadithMarker?> = hadithMarkers.map { it.firstOrNull() }

    suspend fun setQuranMarkers(markers: List<QuranMarker>) {
        val limited = markers.take(QuranColors.MAX_READING_MARKERS)
        context.dataStore.edit { preferences ->
            if (limited.isEmpty()) {
                preferences.remove(PreferencesKeys.QURAN_MARKERS)
                preferences.remove(PreferencesKeys.QURAN_MARKER)
            } else {
                val array = JSONArray()
                limited.forEach { m ->
                    array.put(JSONObject().apply {
                        put("surahNumber", m.surahNumber)
                        put("surahName", m.surahName)
                        put("ayahNumber", m.ayahNumber)
                        put("timestamp", m.timestamp)
                        put("colorIndex", m.colorIndex)
                    })
                }
                preferences[PreferencesKeys.QURAN_MARKERS] = array.toString()
                val first = limited.first()
                preferences[PreferencesKeys.QURAN_MARKER] = JSONObject().apply {
                    put("surahNumber", first.surahNumber)
                    put("surahName", first.surahName)
                    put("ayahNumber", first.ayahNumber)
                    put("timestamp", first.timestamp)
                    put("colorIndex", first.colorIndex)
                }.toString()
            }
        }
    }

    suspend fun setQuranMarker(marker: QuranMarker?) {
        if (marker == null) {
            setQuranMarkers(emptyList())
        } else {
            setQuranMarkers(listOf(marker))
        }
    }

    suspend fun setHadithMarkers(markers: List<HadithMarker>) {
        val limited = markers.take(QuranColors.MAX_READING_MARKERS)
        context.dataStore.edit { preferences ->
            if (limited.isEmpty()) {
                preferences.remove(PreferencesKeys.HADITH_MARKERS)
                preferences.remove(PreferencesKeys.HADITH_MARKER)
            } else {
                val array = JSONArray()
                limited.forEach { m ->
                    array.put(JSONObject().apply {
                        put("collectionId", m.collectionId)
                        put("collectionName", m.collectionName)
                        put("hadithNumber", m.hadithNumber)
                        put("timestamp", m.timestamp)
                        put("colorIndex", m.colorIndex)
                    })
                }
                preferences[PreferencesKeys.HADITH_MARKERS] = array.toString()
                val first = limited.first()
                preferences[PreferencesKeys.HADITH_MARKER] = JSONObject().apply {
                    put("collectionId", first.collectionId)
                    put("collectionName", first.collectionName)
                    put("hadithNumber", first.hadithNumber)
                    put("timestamp", first.timestamp)
                    put("colorIndex", first.colorIndex)
                }.toString()
            }
        }
    }

    suspend fun setHadithMarker(marker: HadithMarker?) {
        if (marker == null) {
            setHadithMarkers(emptyList())
        } else {
            setHadithMarkers(listOf(marker))
        }
    }

    val hadithBookmarks: Flow<List<HadithBookmark>> = context.dataStore.data.map { preferences ->
        val json = preferences[PreferencesKeys.HADITH_BOOKMARKS] ?: return@map emptyList()
        runCatching {
            val array = JSONArray(json)
            val list = mutableListOf<HadithBookmark>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HadithBookmark(
                        id = obj.optLong("id", i.toLong() + 1),
                        collectionId = obj.getString("collectionId"),
                        collectionName = obj.getString("collectionName"),
                        hadithNumber = obj.getInt("hadithNumber"),
                        hadithTextArabic = obj.optString("hadithTextArabic", ""),
                        hadithTranslation = obj.optString("hadithTranslation", ""),
                        note = obj.optString("note", "").takeIf { it.isNotBlank() },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    suspend fun setHadithBookmarks(bookmarks: List<HadithBookmark>) {
        context.dataStore.edit { preferences ->
            if (bookmarks.isEmpty()) {
                preferences.remove(PreferencesKeys.HADITH_BOOKMARKS)
            } else {
                val array = JSONArray()
                bookmarks.forEach { b ->
                    array.put(JSONObject().apply {
                        put("id", b.id)
                        put("collectionId", b.collectionId)
                        put("collectionName", b.collectionName)
                        put("hadithNumber", b.hadithNumber)
                        put("hadithTextArabic", b.hadithTextArabic)
                        put("hadithTranslation", b.hadithTranslation)
                        put("note", b.note ?: "")
                        put("timestamp", b.timestamp)
                    })
                }
                preferences[PreferencesKeys.HADITH_BOOKMARKS] = array.toString()
            }
        }
    }
}

