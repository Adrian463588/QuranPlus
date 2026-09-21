package com.quranplus.app

import com.quranplus.app.features.chatbot.data.DuckDuckGoCitationParser
import com.quranplus.app.features.rag.domain.EvidenceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuckDuckGoCitationParserTest {

    @Test
    fun GIVEN_duckDuckGoHtml_WHEN_parsed_THEN_extractsValidCitations() {
        val sampleHtml = """
            <div class="links_main links_deep result__body">
                <h2 class="result__title">
                    <a rel="nofollow" class="result__a" href="https://jatim.nu.or.id/keislaman/berikut-keutamaan-membaca-ayat-kursi-WUVjH">Berikut Keutamaan Membaca Ayat Kursi - NU Online Jatim</a>
                </h2>
                <div class="result__snippet">
                    <a class="result__snippet" href="https://jatim.nu.or.id/keislaman/berikut-keutamaan-membaca-ayat-kursi-WUVjH">Tiap surat dan ayat al-Quran memiliki kandungan makna yang dalam. Keutamaan membaca ayat kursi sangat agung.</a>
                </div>
            </div>
            <div class="links_main links_deep result__body">
                <h2 class="result__title">
                    <a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Frumaysho.com%2F1234-keutamaan-ayat-kursi.html&amp;rut=123">Keutamaan Ayat Kursi - Rumaysho</a>
                </h2>
                <div class="result__snippet">
                    <a class="result__snippet" href="https://rumaysho.com/1234-keutamaan-ayat-kursi.html">Ayat Kursi adalah ayat paling agung dalam Kitabullah Al-Qur'an.</a>
                </div>
            </div>
        """.trimIndent()

        val citations = DuckDuckGoCitationParser.parse(sampleHtml, limit = 5)

        assertEquals(2, citations.size)

        val first = citations[0]
        assertEquals("NU Online • Berikut Keutamaan Membaca Ayat Kursi - NU Online Jatim", first.title)
        assertEquals("https://jatim.nu.or.id/keislaman/berikut-keutamaan-membaca-ayat-kursi-WUVjH", first.deepLinkTarget)
        assertEquals(EvidenceKind.WEB, first.evidenceKind)
        assertTrue(first.textSnippet.contains("Keutamaan membaca ayat kursi"))

        val second = citations[1]
        assertEquals("Rumaysho • Keutamaan Ayat Kursi - Rumaysho", second.title)
        assertEquals("https://rumaysho.com/1234-keutamaan-ayat-kursi.html", second.deepLinkTarget)
        assertTrue(second.textSnippet.contains("paling agung"))
    }

    @Test
    fun GIVEN_disallowedHost_WHEN_parsed_THEN_filtersOut() {
        val untrustedHtml = """
            <div class="links_main links_deep result__body">
                <h2 class="result__title">
                    <a rel="nofollow" class="result__a" href="https://untrusted-blog.xyz/post">Artikel Tanpa Sumber</a>
                </h2>
                <div class="result__snippet">
                    <a class="result__snippet" href="https://untrusted-blog.xyz/post">Ini adalah snippet tidak terpercaya.</a>
                </div>
            </div>
        """.trimIndent()

        val citations = DuckDuckGoCitationParser.parse(untrustedHtml, limit = 5)
        assertEquals(0, citations.size)
    }
}
