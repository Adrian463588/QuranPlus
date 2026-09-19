package com.quranplus.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quranplus.app.features.chatbot.data.MediaWikiCitationParser
import com.quranplus.app.features.chatbot.data.EditorialCitationParser
import com.quranplus.app.features.chatbot.data.EditorialWebSearchProvider
import com.quranplus.app.features.chatbot.data.SunnahCitationParser
import com.quranplus.app.features.rag.domain.CitationTarget
import com.quranplus.app.features.rag.domain.navigationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InternetResearcherTest {

    @Test
    fun GIVEN_mediaWikiSearchResponse_WHEN_parsed_THEN_keepsVerifiedWebCitation() {
        val json = """
            {
              "query": {
                "pages": [
                  {
                    "pageid": 123,
                    "title": "Sejarah Islam",
                    "extract": "Ringkasan sejarah Islam.",
                    "fullurl": "https://id.wikipedia.org/wiki/Sejarah_Islam"
                  }
                ]
              }
            }
        """.trimIndent()

        val citations = MediaWikiCitationParser.parse(json, "https://id.wikipedia.org", 4)

        assertEquals(1, citations.size)
        assertTrue(citations.single().title.contains("Sejarah Islam"))
        assertEquals(
            CitationTarget.Web("https://id.wikipedia.org/wiki/Sejarah_Islam"),
            citations.single().navigationTarget()
        )
    }

    @Test
    fun GIVEN_mediaWikiSearchResponseWithUnsafeUrl_WHEN_parsed_THEN_dropsTheCitation() {
        val json = """
            {
              "query": {
                "pages": [
                  {
                    "pageid": 123,
                    "title": "Unsafe",
                    "extract": "Should not be surfaced.",
                    "fullurl": "http://example.com/unsafe"
                  }
                ]
              }
            }
        """.trimIndent()

        assertTrue(
            MediaWikiCitationParser.parse(json, "https://id.wikipedia.org", 4).isEmpty()
        )
    }

    @Test
    fun GIVEN_editorialArticleCard_WHEN_parsed_THEN_keepsBoundedAllowlistedCitation() {
        val source = EditorialWebSearchProvider.EditorialSource(
            id = "test-editorial",
            name = "Sumber Editorial",
            baseUrl = "https://islam.nu.or.id",
            host = "islam.nu.or.id",
            searchUrl = { query, _ -> "https://islam.nu.or.id/search?q=$query" }
        )
        val html = """
            <article>
              <a href="/syariah/hukum-talak">Hukum talak dalam fikih</a>
              <p>Ringkasan editorial yang menjelaskan konteks hukum dan perbedaan pendapat.</p>
            </article>
        """.trimIndent()

        val citations = EditorialCitationParser.parse(html, source, 4)

        assertEquals(1, citations.size)
        assertEquals(
            CitationTarget.Web("https://islam.nu.or.id/syariah/hukum-talak"),
            citations.single().navigationTarget()
        )
        assertTrue(citations.single().textSnippet.length <= 800)
    }

    @Test
    fun GIVEN_editorialArticleCardWithUnsafeHost_WHEN_parsed_THEN_dropsCitation() {
        val source = EditorialWebSearchProvider.EditorialSource(
            id = "test-editorial",
            name = "Sumber Editorial",
            baseUrl = "https://islam.nu.or.id",
            host = "islam.nu.or.id",
            searchUrl = { query, _ -> "https://islam.nu.or.id/search?q=$query" }
        )
        val html = """
            <article>
              <a href="https://example.com/hukum">Hukum talak dalam fikih</a>
              <p>Isi artikel tidak boleh diarahkan ke host yang tidak diizinkan.</p>
            </article>
        """.trimIndent()

        assertTrue(EditorialCitationParser.parse(html, source, 4).isEmpty())
    }

    @Test
    fun GIVEN_sunnahPublicResult_WHEN_parsed_THEN_keepsOnlyAllowlistedHadithPath() {
        val html = """
            <article>
              <a href="/bukhari:1">Sahih al-Bukhari 1</a>
              <p>Amal itu bergantung pada niat dan setiap orang mendapatkan sesuai niatnya.</p>
            </article>
            <article>
              <a href="https://example.com/bukhari:2">Unsafe</a>
              <p>Hasil dari host lain tidak boleh digunakan.</p>
            </article>
        """.trimIndent()

        val citations = SunnahCitationParser.parse(html, 4)

        assertEquals(1, citations.size)
        assertEquals(
            CitationTarget.Web("https://sunnah.com/bukhari:1"),
            citations.single().navigationTarget()
        )
    }
}
