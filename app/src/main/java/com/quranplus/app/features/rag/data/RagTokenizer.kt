package com.quranplus.app.features.rag.data

/**
 * Common abstraction for embedding tokenizers (WordPiece and SentencePiece).
 */
interface RagTokenizer {
    val padId: Long
    val clsId: Long
    val sepId: Long
    val unknownId: Long

    fun tokenize(text: String, maxSequenceLength: Int = 512): LongArray
    fun countContentTokens(text: String): Int
}
