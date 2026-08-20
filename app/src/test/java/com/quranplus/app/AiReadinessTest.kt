package com.quranplus.app

import com.quranplus.app.features.chatbot.data.hasRequiredCorpus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiReadinessTest {

    @Test
    fun GIVEN_quranHadithAndDocumentAreIndexed_WHEN_checkingCorpus_THEN_allowsChat() {
        assertTrue(
            hasRequiredCorpus(
                indexedSourceTypes = setOf("quran", "hadith", "user_document"),
                hadithAvailable = true,
                documentSourceTypes = setOf("user_document")
            )
        )
    }

    @Test
    fun GIVEN_downloadedHadithIsNotIndexed_WHEN_checkingCorpus_THEN_blocksChat() {
        assertFalse(
            hasRequiredCorpus(
                indexedSourceTypes = setOf("quran"),
                hadithAvailable = true,
                documentSourceTypes = emptySet()
            )
        )
    }

    @Test
    fun GIVEN_importedDocumentSourceIsNotIndexed_WHEN_checkingCorpus_THEN_blocksChat() {
        assertFalse(
            hasRequiredCorpus(
                indexedSourceTypes = setOf("quran", "hadith"),
                hadithAvailable = true,
                documentSourceTypes = setOf("user_document")
            )
        )
    }

    @Test
    fun GIVEN_quranOnlyIsAvailable_WHEN_noOptionalCorpusExists_THEN_allowsCorpus() {
        assertTrue(
            hasRequiredCorpus(
                indexedSourceTypes = setOf("quran"),
                hadithAvailable = false,
                documentSourceTypes = emptySet()
            )
        )
    }
}
