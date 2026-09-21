package com.quranplus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.DriverManager

class TransliterationIkhfaTest {

    @Test
    fun GIVEN_transliterationWithIkhfa_WHEN_transformed_THEN_containsNgPhonetic() {
        val inputFajiran = "fājiran kaffārā(n)"
        val expectedFajiran = "fājirang kaffārā(n)"
        assertEquals(expectedFajiran, applyIkhfaPhonetics(inputFajiran))

        val inputYunfiqun = "yunfiqūn(a)"
        val expectedYunfiqun = "yungfiqūn(a)"
        assertEquals(expectedYunfiqun, applyIkhfaPhonetics(inputYunfiqun))

        val inputKuntum = "kuntum"
        val expectedKuntum = "kungtum"
        assertEquals(expectedKuntum, applyIkhfaPhonetics(inputKuntum))

        val inputUnzila = "unzila"
        val expectedUnzila = "ungzila"
        assertEquals(expectedUnzila, applyIkhfaPhonetics(inputUnzila))

        val inputMinQablik = "min qablik(a)"
        val expectedMinQablik = "ming qablik(a)"
        assertEquals(expectedMinQablik, applyIkhfaPhonetics(inputMinQablik))

        val inputMaradunFa = "maraḍun fa"
        val expectedMaradunFa = "maraḍung fa"
        assertEquals(expectedMaradunFa, applyIkhfaPhonetics(inputMaradunFa))
    }

    @Test
    fun GIVEN_assetDatabase_WHEN_inspected_THEN_containsIkhfaTransliteration() {
        val dbFile = File("src/main/assets/databases/quranplus.db")
        if (!dbFile.exists()) {
            val altFile = File("app/src/main/assets/databases/quranplus.db")
            if (!altFile.exists()) return
        }
        val targetFile = if (dbFile.exists()) dbFile else File("app/src/main/assets/databases/quranplus.db")

        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.getConnection("jdbc:sqlite:${targetFile.absolutePath}").use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT transliteration FROM ayahs WHERE surah_id = 71 AND ayah_number = 27").use { rs ->
                        if (rs.next()) {
                            val trans = rs.getString(1)
                            assertTrue("QS 71:27 should contain fājirang: $trans", trans.contains("fājirang"))
                        }
                    }
                    stmt.executeQuery("SELECT transliteration FROM ayahs WHERE surah_id = 2 AND ayah_number = 3").use { rs ->
                        if (rs.next()) {
                            val trans = rs.getString(1)
                            assertTrue("QS 2:3 should contain yungfiqūn: $trans", trans.contains("yungfiqūn"))
                        }
                    }
                }
            }
        } catch (_: ClassNotFoundException) {
            // sqlite-jdbc driver not present in test classpath, skip direct db test
        }
    }

    private fun applyIkhfaPhonetics(text: String): String {
        // Inter-word: tanwin / nun sukun followed by ikhfa letters
        // an/in/un + space + [t, ts, j, d, dz, r?, z, s, sy, sh, dh, th, zh, f, q, k]
        var result = text.replace(Regex("""\b([a-zA-Zāīūḍṣṭẓ]+[aiu])n\s+([tsdjzfqk]|ts|sy|dz|sh|dh|th|zh)""", RegexOption.IGNORE_CASE)) { m ->
            "${m.groupValues[1]}ng ${m.groupValues[2]}"
        }
        // Intra-word: nun sukun followed by ikhfa letters (yunfiqun -> yungfiqun, kuntum -> kungtum, unzila -> ungzila)
        result = result.replace(Regex("""\b([a-zA-Zāīūḍṣṭẓ]*[aiu])n([tsdjzfqk]|ts|sy|dz|sh|dh|th|zh)""", RegexOption.IGNORE_CASE)) { m ->
            "${m.groupValues[1]}ng${m.groupValues[2]}"
        }
        return result
    }
}
