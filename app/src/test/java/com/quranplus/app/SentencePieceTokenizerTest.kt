package com.quranplus.app

import com.quranplus.app.features.rag.data.SentencePieceTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SentencePieceTokenizerTest {

    private fun getModelStream() =
        File("src/main/assets/embedding/sentencepiece.bpe.model").inputStream()

    @Test
    fun GIVEN_sentencePieceModel_WHEN_loading_THEN_specialTokenConstantsAreCorrect() {
        val tokenizer = SentencePieceTokenizer(getModelStream())

        assertEquals(1L, tokenizer.padId)
        assertEquals(0L, tokenizer.clsId)
        assertEquals(2L, tokenizer.sepId)
        assertEquals(3L, tokenizer.unknownId)
    }

    @Test
    fun GIVEN_indonesianText_WHEN_tokenizing_THEN_producesValidTokenIds() {
        val tokenizer = SentencePieceTokenizer(getModelStream())
        val text = "keutamaan membaca ayat kursi"
        val maxLen = 32

        val tokens = tokenizer.tokenize(text, maxLen)

        assertEquals(maxLen, tokens.size)
        assertEquals(0L, tokens[0]) // <s> BOS/CLS

        // Must have non-zero, non-pad tokens in between
        assertTrue(tokens[1] > 3L)

        // Must contain </s> (EOS/SEP) before padding
        val sepIndex = tokens.indexOf(2L)
        assertTrue("Tokens must contain </s> (2L)", sepIndex > 1)

        // All tokens after </s> must be padding (<pad> = 1L)
        for (i in (sepIndex + 1) until maxLen) {
            assertEquals(1L, tokens[i])
        }

        val contentTokens = tokenizer.countContentTokens(text)
        assertTrue(contentTokens > 0)
    }

    @Test
    fun GIVEN_arabicText_WHEN_tokenizing_THEN_handlesArabicCharacters() {
        val tokenizer = SentencePieceTokenizer(getModelStream())
        val text = "بسم الله الرحمن الرحيم"
        val maxLen = 32

        val tokens = tokenizer.tokenize(text, maxLen)

        assertEquals(maxLen, tokens.size)
        assertEquals(0L, tokens[0]) // <s>
        assertTrue(tokens[1] > 3L)

        val sepIndex = tokens.indexOf(2L)
        assertTrue("Tokens must contain </s> (2L)", sepIndex > 1)
    }

    @Test
    fun GIVEN_emptyText_WHEN_tokenizing_THEN_returnsAllPadding() {
        val tokenizer = SentencePieceTokenizer(getModelStream())
        val tokens = tokenizer.tokenize("", 16)

        assertEquals(16, tokens.size)
        assertTrue(tokens.all { it == 1L })
        assertEquals(0, tokenizer.countContentTokens(""))
    }
}
