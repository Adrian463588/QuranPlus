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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quranplus_settings")

enum class TranslationMode(val id: String, val label: String) {
    INDONESIAN("id", "Indonesia"),
    ENGLISH("en", "English"),
    BOTH("both", "Keduanya");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: INDONESIAN
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
        val TRANSLATION_MODE = stringPreferencesKey("translation_mode")
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

    val translationMode: Flow<TranslationMode> = context.dataStore.data.map { preferences ->
        TranslationMode.fromId(preferences[PreferencesKeys.TRANSLATION_MODE] ?: TranslationMode.INDONESIAN.id)
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

    suspend fun setTranslationMode(mode: TranslationMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRANSLATION_MODE] = mode.id
        }
    }
}
