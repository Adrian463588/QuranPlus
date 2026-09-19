package com.quranplus.app

import com.quranplus.app.features.rag.domain.IslamicQuestionDomain
import com.quranplus.app.features.rag.domain.IslamicQueryAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicQueryAnalyzerTest {

    @Test
    fun GIVEN_talakQuestion_WHEN_analyzed_THEN_routesToFiqhAndExpandsTerms() {
        val plan = IslamicQueryAnalyzer.analyze("berikan hukum talak dalam Islam")

        assertEquals(IslamicQuestionDomain.HUKUM_FIQIH, plan.domain)
        assertTrue("talak" in plan.keywords)
        assertTrue("cerai" in plan.keywords)
        assertTrue(plan.semanticQuery.contains("talak"))
    }

    @Test
    fun GIVEN_adabQuestion_WHEN_analyzed_THEN_routesToAdab() {
        val plan = IslamicQueryAnalyzer.analyze("bagaimana adab kepada tetangga")

        assertEquals(IslamicQuestionDomain.ADAB_AKHLAK, plan.domain)
        assertTrue("adab" in plan.keywords)
        assertTrue("akhlak" in plan.keywords)
    }

    @Test
    fun GIVEN_wudhuQuestion_WHEN_analyzed_THEN_routesToIbadah() {
        val plan = IslamicQueryAnalyzer.analyze("syarat wudu sebelum shalat")

        assertEquals(IslamicQuestionDomain.IBADAH, plan.domain)
        assertTrue("wudu" in plan.keywords)
        assertTrue("wudhu" in plan.keywords)
        assertTrue("shalat" in plan.keywords)
    }

    @Test
    fun GIVEN_tarikhQuestion_WHEN_analyzed_THEN_routesToHistory() {
        val plan = IslamicQueryAnalyzer.analyze("sejarah hijrah Nabi ke Madinah")

        assertEquals(IslamicQuestionDomain.SEJARAH_TARIKH, plan.domain)
        assertTrue("hijrah" in plan.keywords)
        assertTrue("madinah" in plan.keywords)
    }

    @Test
    fun GIVEN_explicitSourceTerms_WHEN_analyzed_THEN_preservesSourcePreference() {
        val plan = IslamicQueryAnalyzer.analyze("ayat Quran dan hadist tentang sabar")

        assertTrue(plan.prefersQuran)
        assertTrue(plan.prefersHadith)
        assertTrue(plan.keywords.size <= 12)
    }
}
