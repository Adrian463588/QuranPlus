package com.quranplus.app

import com.quranplus.app.features.chatbot.data.SunnahCitationParser
import com.quranplus.app.features.rag.domain.isPrimaryEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SunnahCitationParserTest {
    @Test
    fun GIVEN_nestedSearchResult_WHEN_parsing_THEN_preservesTextAndCanonicalHadithLink() {
        // Controlled markup; source excerpt: https://sunnah.com/bukhari:1947
        val html = """
            <div class="actualHadithContainer">
                <div><a href="/bukhari">Sahih al-Bukhari</a></div>
                <div class="arabic_hadith_full">كُنَّا نُسَافِرُ مَعَ النَّبِيِّ صلى الله عليه وسلم</div>
                <div><a href="https://sunnah.com/bukhari:1947">Sahih al-Bukhari 1947</a></div>
            </div>
        """.trimIndent()
        val result = SunnahCitationParser.parse(html, 4).single()
        assertEquals("hadith", result.sourceType)
        assertEquals("https://sunnah.com/bukhari:1947", result.deepLinkTarget)
        assertTrue(result.textSnippet.contains("كُنَّا نُسَافِرُ"))
        assertTrue(result.isPrimaryEvidence())
    }

    @Test
    fun GIVEN_untrustedHostWithHadithPath_WHEN_parsing_THEN_rejectsIt() {
        val html = """
            <div class="actualHadithContainer">
                <div>Untrusted material must never become a Hadith source.</div>
                <a href="https://evil.example/bukhari:1947">Bukhari 1947</a>
            </div>
        """.trimIndent()
        assertTrue(SunnahCitationParser.parse(html, 4).isEmpty())
    }
}
