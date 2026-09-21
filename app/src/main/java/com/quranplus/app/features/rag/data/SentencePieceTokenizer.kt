package com.quranplus.app.features.rag.data

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

/**
 * Pure Kotlin SentencePiece Unigram Tokenizer for multilingual models
 * (e.g. paraphrase-multilingual-MiniLM-L12-v2).
 *
 * Implements SentencePiece protobuf parsing and Viterbi dynamic programming segmentation
 * without requiring NDK or external C++ dependencies.
 */
class SentencePieceTokenizer(
    inputStream: InputStream
) : RagTokenizer {

    override val padId: Long = PAD_ID
    override val clsId: Long = CLS_ID
    override val sepId: Long = SEP_ID
    override val unknownId: Long = UNKNOWN_ID

    private val pieceToId = ConcurrentHashMap<String, Long>()
    private val pieceToScore = ConcurrentHashMap<String, Float>()

    init {
        loadModel(inputStream)
    }

    override fun tokenize(text: String, maxSequenceLength: Int): LongArray {
        if (text.isBlank()) {
            return LongArray(maxSequenceLength) { PAD_ID }
        }

        // 1. Normalize unicode (NFKC)
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).trim()

        // 2. Metaspace pre-tokenization: prepend   and replace whitespace with  
        val prepared = METASPACE + normalized.replace(Regex("\\s+"), METASPACE)

        // 3. Viterbi segmentation
        val pieces = viterbiSegment(prepared)

        // 4. Build token sequence: <s> + tokens + </s> + padding
        val tokenIds = ArrayList<Long>(maxSequenceLength)
        tokenIds.add(CLS_ID)

        val maxContentTokens = (maxSequenceLength - 2).coerceAtLeast(0)
        for (i in 0 until minOf(pieces.size, maxContentTokens)) {
            val piece = pieces[i]
            val id = pieceToId[piece] ?: UNKNOWN_ID
            tokenIds.add(id)
        }

        tokenIds.add(SEP_ID)

        while (tokenIds.size < maxSequenceLength) {
            tokenIds.add(PAD_ID)
        }

        return tokenIds.take(maxSequenceLength).toLongArray()
    }

    override fun countContentTokens(text: String): Int {
        if (text.isBlank()) return 0
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).trim()
        val prepared = METASPACE + normalized.replace(Regex("\\s+"), METASPACE)
        return viterbiSegment(prepared).size
    }

    private fun viterbiSegment(text: String): List<String> {
        val n = text.length
        if (n == 0) return emptyList()

        val dp = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val prev = IntArray(n + 1) { -1 }
        dp[0] = 0f

        val maxSubwordLength = 32

        for (i in 1..n) {
            val startBound = maxOf(0, i - maxSubwordLength)
            for (j in startBound until i) {
                val sub = text.substring(j, i)
                val score = pieceToScore[sub]
                if (score != null) {
                    val candidate = dp[j] + score
                    if (candidate > dp[i]) {
                        dp[i] = candidate
                        prev[i] = j
                    }
                } else if (i - j == 1) {
                    // Single character fallback penalty
                    val fallbackScore = dp[j] - 100.0f
                    if (fallbackScore > dp[i]) {
                        dp[i] = fallbackScore
                        prev[i] = j
                    }
                }
            }
        }

        // Backtrack
        var curr = n
        val result = mutableListOf<String>()
        while (curr > 0) {
            val p = prev[curr]
            if (p == -1 || p == curr) {
                result.add(text.substring(curr - 1, curr))
                curr -= 1
            } else {
                result.add(text.substring(p, curr))
                curr = p
            }
        }
        result.reverse()
        return result
    }

    /**
     * Parses SentencePiece ModelProto protobuf stream and constructs Hugging Face
     * compatible vocabulary mapping (0: <s>, 1: <pad>, 2: </s>, 3: <unk>, 4+: pieces, 250001: <mask>).
     */
    private fun loadModel(inputStream: InputStream) {
        val data = inputStream.readBytes()
        var pos = 0

        // Special tokens standard for Hugging Face XLM-RoBERTa / SentenceTransformers
        pieceToId["<s>"] = CLS_ID
        pieceToScore["<s>"] = 0.0f

        pieceToId["<pad>"] = PAD_ID
        pieceToScore["<pad>"] = 0.0f

        pieceToId["</s>"] = SEP_ID
        pieceToScore["</s>"] = 0.0f

        pieceToId["<unk>"] = UNKNOWN_ID
        pieceToScore["<unk>"] = 0.0f

        var nextId = 4L

        while (pos < data.size) {
            val byte = data[pos++].toInt() and 0xFF
            val tag = byte ushr 3
            val wire = byte and 0x07

            if (tag == 1 && wire == 2) {
                // SentencePiece message within ModelProto
                var length = 0
                var shift = 0
                while (pos < data.size) {
                    val b = data[pos++].toInt() and 0xFF
                    length = length or ((b and 0x7F) shl shift)
                    shift += 7
                    if ((b and 0x80) == 0) break
                }
                val subEnd = minOf(pos + length, data.size)
                var pieceStr = ""
                var pieceScore = 0.0f

                while (pos < subEnd) {
                    val subByte = data[pos++].toInt() and 0xFF
                    val subTag = subByte ushr 3
                    val subWire = subByte and 0x07

                    when {
                        subTag == 1 && subWire == 2 -> {
                            var strLen = 0
                            var strShift = 0
                            while (pos < subEnd) {
                                val sb = data[pos++].toInt() and 0xFF
                                strLen = strLen or ((sb and 0x7F) shl strShift)
                                strShift += 7
                                if ((sb and 0x80) == 0) break
                            }
                            val safeLen = minOf(strLen, subEnd - pos)
                            pieceStr = String(data, pos, safeLen, Charsets.UTF_8)
                            pos += safeLen
                        }
                        subTag == 2 && subWire == 5 -> {
                            if (pos + 4 <= subEnd) {
                                val buffer = ByteBuffer.wrap(data, pos, 4).order(ByteOrder.LITTLE_ENDIAN)
                                pieceScore = buffer.float
                                pos += 4
                            }
                        }
                        subWire == 0 -> {
                            while (pos < subEnd && (data[pos++].toInt() and 0x80) != 0) { /* skip varint */ }
                        }
                        subWire == 2 -> {
                            var slen = 0
                            var sshift = 0
                            while (pos < subEnd) {
                                val b = data[pos++].toInt() and 0xFF
                                slen = slen or ((b and 0x7F) shl sshift)
                                sshift += 7
                                if ((b and 0x80) == 0) break
                            }
                            pos = minOf(pos + slen, subEnd)
                        }
                        subWire == 5 -> pos = minOf(pos + 4, subEnd)
                        subWire == 1 -> pos = minOf(pos + 8, subEnd)
                        else -> break
                    }
                }

                if (pieceStr.isNotEmpty() && !pieceToId.containsKey(pieceStr)) {
                    pieceToId[pieceStr] = nextId
                    nextId++
                }
                if (pieceStr.isNotEmpty()) {
                    pieceToScore[pieceStr] = pieceScore
                }
            } else if (wire == 0) {
                while (pos < data.size && (data[pos++].toInt() and 0x80) != 0) { /* skip */ }
            } else if (wire == 2) {
                var l = 0
                var s = 0
                while (pos < data.size) {
                    val b = data[pos++].toInt() and 0xFF
                    l = l or ((b and 0x7F) shl s)
                    s += 7
                    if ((b and 0x80) == 0) break
                }
                pos = minOf(pos + l, data.size)
            } else if (wire == 5) {
                pos = minOf(pos + 4, data.size)
            } else if (wire == 1) {
                pos = minOf(pos + 8, data.size)
            } else {
                break
            }
        }

        // Add <mask> at index 250001 if not already present
        if (!pieceToId.containsKey("<mask>")) {
            pieceToId["<mask>"] = 250001L
            pieceToScore["<mask>"] = 0.0f
        }
    }

    companion object {
        const val PAD_ID = 1L
        const val CLS_ID = 0L
        const val SEP_ID = 2L
        const val UNKNOWN_ID = 3L
        const val METASPACE = "\u2581"
    }
}
