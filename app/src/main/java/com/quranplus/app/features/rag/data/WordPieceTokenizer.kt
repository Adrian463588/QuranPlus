package com.quranplus.app.features.rag.data

/**
 * BERT WordPiece Tokenizer for English & multilingual models with WordPiece vocabs
 * (e.g. all-MiniLM-L6-v2, BAAI BGE Small EN v1.5).
 */
class WordPieceTokenizer(
    private val vocabulary: Map<String, Long>
) : RagTokenizer {

    override val padId: Long = PAD_ID
    override val clsId: Long = CLS_ID
    override val sepId: Long = SEP_ID
    override val unknownId: Long = UNKNOWN_ID

    override fun tokenize(text: String, maxSequenceLength: Int): LongArray {
        val tokens = ArrayList<Long>(maxSequenceLength)
        tokens += vocabulary[CLS_TOKEN] ?: CLS_ID
        splitOnWhitespaceAndPunctuation(text.lowercase())
            .flatMap(::wordPiece)
            .take((maxSequenceLength - 2).coerceAtLeast(0))
            .forEach { token ->
                tokens += vocabulary[token] ?: (vocabulary[UNKNOWN_TOKEN] ?: UNKNOWN_ID)
            }
        tokens += vocabulary[SEP_TOKEN] ?: SEP_ID
        while (tokens.size < maxSequenceLength) tokens += PAD_ID
        return tokens.take(maxSequenceLength).toLongArray()
    }

    override fun countContentTokens(text: String): Int =
        splitOnWhitespaceAndPunctuation(text.lowercase())
            .flatMap(::wordPiece)
            .size

    private fun splitOnWhitespaceAndPunctuation(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                result += current.toString()
                current.clear()
            }
        }
        text.forEach { character ->
            if (character.isWhitespace() || isAsciiPunctuation(character)) {
                flush()
                if (!character.isWhitespace()) result += character.toString()
            } else {
                current.append(character)
            }
        }
        flush()
        return result
    }

    private fun wordPiece(word: String): List<String> {
        if (word.isBlank()) return emptyList()
        val pieces = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var match: String? = null
            while (start < end) {
                val candidate = if (start == 0) word.substring(0, end) else "##${word.substring(start, end)}"
                if (candidate in vocabulary) {
                    match = candidate
                    break
                }
                end--
            }
            if (match == null) return listOf(UNKNOWN_TOKEN)
            pieces += match
            start = end
        }
        return pieces
    }

    private fun isAsciiPunctuation(character: Char): Boolean {
        val code = character.code
        return code in 33..47 || code in 58..64 || code in 91..96 || code in 123..126
    }

    companion object {
        const val PAD_ID = 0L
        const val CLS_ID = 101L
        const val SEP_ID = 102L
        const val UNKNOWN_ID = 100L
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"
        const val UNKNOWN_TOKEN = "[UNK]"
    }
}
