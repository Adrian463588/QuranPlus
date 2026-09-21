package com.quranplus.app

import com.quranplus.app.features.chatbot.domain.AiResponsePostProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponsePostProcessorTest {

    @Test
    fun GIVEN_responseWithLlmStopTokens_WHEN_processed_THEN_stripsArtifacts() {
        val raw = "Ini adalah jawaban yang benar.<|im_end|><end_of_turn>[EOS]"
        val result = AiResponsePostProcessor.process(raw)
        assertEquals("Ini adalah jawaban yang benar.", result)
    }

    @Test
    fun GIVEN_responseWithLeakedPrompt_WHEN_processed_THEN_stripsPromptHeaders() {
        val raw = """
            Instruksi sistem: Jawab dengan gaya santun.
            Pedoman Jawaban: 1. Jawab secara tuntas.
            Jawaban: Keutamaan Ayat Kursi sangat besar[[cite:C1]].
        """.trimIndent()

        val result = AiResponsePostProcessor.process(raw)
        assertFalse(result.contains("Instruksi sistem:"))
        assertFalse(result.contains("Pedoman Jawaban:"))
        assertFalse(result.contains("Jawaban:"))
        assertFalse(result.contains("[[cite:C1]]"))
        assertTrue(result.contains("Keutamaan Ayat Kursi sangat besar."))
    }

    @Test
    fun GIVEN_responseWithMessyPunctuation_WHEN_processed_THEN_normalizesSpacing() {
        val raw = "Pertama ,shalat lima waktu .Kedua ,puasa Ramadhan ."
        val result = AiResponsePostProcessor.process(raw)
        assertEquals("Pertama, shalat lima waktu. Kedua, puasa Ramadhan.", result)
    }

    @Test
    fun GIVEN_responseWithBulletsAndQuotes_WHEN_processed_THEN_normalizesCleanMarkdown() {
        val raw = "Berikut rukunnya:\n• Syahadat\n* Shalat\n+ Zakat\n\n\n\nSelesai."
        val result = AiResponsePostProcessor.process(raw)
        assertTrue(result.contains("- Syahadat"))
        assertTrue(result.contains("- Shalat"))
        assertTrue(result.contains("- Zakat"))
        assertFalse(result.contains("\n\n\n"))
    }

    @Test
    fun GIVEN_truncatedResponseWithoutPeriod_WHEN_processed_THEN_appendsOrTrimsProperly() {
        val raw = "Rasulullah shallallahu 'alaihi wa sallam bersabda bahwa ayat ini agung"
        val result = AiResponsePostProcessor.process(raw)
        assertTrue(result.endsWith("."))
    }

    @Test
    fun GIVEN_squashedMarkdownHeadingsAndLists_WHEN_processed_THEN_structuresCleanly() {
        val raw = "Al-Qur'an [ ].### Dalil Al-Qur'an & HaditsKeutamaan membaca Ayat Kursi didukung: 1.**Jaminan Masuk Surga:** Rasulullah bersabda bahwa ayat ini agung.### Uraian & PenjelasanKeutamaan membaca lebih luas. dari Allah SWT.**Amalan Praktis:**Lakukanlah membaca setiap hari."
        val result = AiResponsePostProcessor.process(raw)

        assertFalse(result.contains("[ ]"))
        assertTrue(result.contains("### Dalil Al-Qur'an & Hadits\n\nKeutamaan"))
        assertTrue(result.contains("### Uraian & Penjelasan\n\nKeutamaan"))
        assertTrue(result.contains("1. **Jaminan Masuk Surga:**"))
        assertTrue(result.contains("Allah SWT.\n\n**Amalan Praktis:** Lakukanlah"))
    }

    @Test
    fun GIVEN_tripleAsteriskSubheadings_WHEN_processed_THEN_normalizedToDouble() {
        val raw = "Berikut keutamaannya:***Kedudukan Ayat Paling Agung:*** Ayat Kursi adalah ayat terbesar.***Jaminan Masuk Surga:** Siapa membacanya dijamin aman."
        val result = AiResponsePostProcessor.process(raw)
        assertFalse(result.contains("***"))
        assertTrue(result.contains("**Kedudukan Ayat Paling Agung:**"))
        assertTrue(result.contains("**Jaminan Masuk Surga:**"))
    }

    @Test
    fun GIVEN_responseEndsWithHangingConjunction_WHEN_processed_THEN_trimmedToLastSentence() {
        val raw = "Shalat adalah tiang agama. Orang yang menjaganya akan mendapatkan ridha Allah dan"
        val result = AiResponsePostProcessor.process(raw)
        // Should trim back to last complete sentence
        assertFalse(result.trimEnd().endsWith(" dan"))
        assertTrue(result.contains("Shalat adalah tiang agama."))
    }

    @Test
    fun GIVEN_responseWithUnclosedBoldDelimiter_WHEN_processed_THEN_balanced() {
        val raw = "Keutamaan Ayat Kursi sangat besar. **Pertama: perlindungan dari jin."
        val result = AiResponsePostProcessor.process(raw)
        // The odd ** should be closed
        val boldCount = result.split("**").size - 1
        assertEquals(0, boldCount % 2)
    }
}
