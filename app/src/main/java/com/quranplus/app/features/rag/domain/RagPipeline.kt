package com.quranplus.app.features.rag.domain

import com.quranplus.app.features.settings.data.AiPersona

class GroundingUnavailable(message: String) : IllegalStateException(message)

class RagPipeline {

    fun buildAugmentedPrompt(
        question: String,
        persona: AiPersona,
        customPrompt: String?,
        citations: List<RetrievedCitation>
    ): String {
        if (citations.isEmpty()) {
            throw GroundingUnavailable(
                "Tidak ada rujukan terverifikasi untuk pertanyaan ini. Jawaban tidak dibuat."
            )
        }
        val systemDirective = if (persona == AiPersona.CUSTOM && !customPrompt.isNullOrBlank()) {
            customPrompt
        } else {
            persona.defaultPrompt
        }

        val contextBuilder = StringBuilder()
        if (citations.isNotEmpty()) {
            contextBuilder.append("=== RUJUKAN DALIL AL-QUR'AN DAN AS-SUNNAH (GROUND TRUTH) ===\n\n")
            citations.forEachIndexed { idx, cite ->
                contextBuilder.append("[Dalil ${idx + 1}: ${cite.title}]\n")
                contextBuilder.append("${cite.textSnippet}\n")
                contextBuilder.append(
                    "Sumber: ${cite.collection ?: cite.sourceType} / ${cite.identifier}; " +
                        "referensi: ${cite.reference}; skor: ${cite.score}\n\n"
                )
            }
            contextBuilder.append("============================================================\n\n")
        }

        return """
            $systemDirective

            $contextBuilder
            Instruksi Khusus:
            Jawablah pertanyaan berikut dengan berlandaskan dalil-dalil di atas. Sertakan nomor surah/ayat atau nomor hadits dalam penjelasan Anda secara jelas dan santun.

            Pertanyaan: $question

            Jawaban:
        """.trimIndent()
    }
}
