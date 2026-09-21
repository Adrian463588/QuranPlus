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

    @Test
    fun GIVEN_halalHaramQuestion_WHEN_analyzed_THEN_extractsMaidah3AsCanonicalTarget() {
        val plan = IslamicQueryAnalyzer.analyze("apa hukum halal haram makanan dalam Islam")

        assertEquals(IslamicQuestionDomain.HUKUM_FIQIH, plan.domain)
        assertTrue(plan.keywords.contains("halal"))
        assertTrue(plan.keywords.contains("haram"))
        assertTrue(plan.canonicalAyahTargets.contains(Pair(5, 3)))
    }

    @Test
    fun GIVEN_keutamaanAyatKursi_WHEN_analyzed_THEN_extractsAlBaqarah255AndPrefersHadith() {
        val plan = IslamicQueryAnalyzer.analyze("keutamaan baca ayat kursi")

        assertTrue("kursi" in plan.keywords)
        assertTrue(plan.canonicalAyahTargets.contains(Pair(2, 255)))
        assertTrue(plan.prefersHadith)
        assertTrue(plan.prefersQuran)
        assertTrue("fadhilah" in plan.keywords || "keutamaan" in plan.keywords)
    }

    @Test
    fun GIVEN_puasaQuestion_WHEN_analyzed_THEN_routesToIbadahAndAlBaqarah183() {
        val plan = IslamicQueryAnalyzer.analyze("apa saja syarat dan rukun puasa")

        assertEquals(IslamicQuestionDomain.IBADAH, plan.domain)
        assertTrue("puasa" in plan.keywords)
        assertTrue(plan.canonicalAyahTargets.contains(Pair(2, 183)))
    }

    @Test
    fun GIVEN_rukunIslamAndIman_WHEN_analyzed_THEN_containsAppropriateKeywords() {
        val planIslam = IslamicQueryAnalyzer.analyze("sebutkan rukun islam")
        assertTrue("syahadat" in planIslam.keywords || "rukun" in planIslam.keywords)

        val planIman = IslamicQueryAnalyzer.analyze("sebutkan rukun iman")
        assertTrue("iman" in planIman.keywords)
    }

    @Test
    fun GIVEN_ayatKursiWithGenericAction_WHEN_analyzed_THEN_isolatesCoreTopicFromGenericIntent() {
        val plan = IslamicQueryAnalyzer.analyze("Apa keutamaan membaca Ayat Kursi?")

        assertTrue("kursi" in plan.coreTopicTerms)
        assertTrue("membaca" in plan.intentTerms)
        assertTrue("keutamaan" in plan.intentTerms)
        assertTrue("membaca" !in plan.coreTopicTerms)
        assertTrue("keutamaan" !in plan.coreTopicTerms)
    }

    @Test
    fun GIVEN_talakQuestion_WHEN_analyzed_THEN_extractsTalakCoreTopicAndCanonicalAyah() {
        val plan = IslamicQueryAnalyzer.analyze("bagaimana hukum talak")

        assertTrue("talak" in plan.coreTopicTerms)
        assertTrue(plan.canonicalAyahTargets.contains(Pair(65, 1)) || plan.canonicalAyahTargets.contains(Pair(2, 228)))
        assertEquals(IslamicQuestionDomain.HUKUM_FIQIH, plan.domain)
    }

    @Test
    fun GIVEN_puasaSunnahQuestion_WHEN_analyzed_THEN_identifiesSunnahFastingTopics() {
        val plan = IslamicQueryAnalyzer.analyze("bagaimana puasa sunah yang dianjurkan")

        assertTrue("puasa" in plan.coreTopicTerms)
        assertTrue("senin" in plan.coreTopicTerms || "kamis" in plan.coreTopicTerms || "daud" in plan.coreTopicTerms)
        assertEquals(IslamicQuestionDomain.IBADAH, plan.domain)
    }
}
