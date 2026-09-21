package com.quranplus.app.features.chatbot.domain

import com.quranplus.app.features.rag.domain.CitationMarkerValidator

/**
 * Normalizes, formats, and cleans responses from local LLMs.
 *
 * Ensures:
 * 1. LLM artifact tokens (<|im_end|>, <end_of_turn>, etc.) and leaked prompt instructions are stripped.
 * 2. Typography is clean (proper Indonesian punctuation, single spaces, well-formed newlines).
 * 3. Point inference / word trail is structured and readable.
 * 4. Incomplete sentences cut off at the end are cleanly finalized.
 * 5. Citation markers are cleaned and formatted properly.
 * 6. Triple-asterisk artifacts (***Heading:***) normalized to **Heading:**.
 * 7. Bold subheadings that run on without a preceding newline get one inserted.
 * 8. Unclosed paired delimiters (**,  (), "") are balanced.
 */
object AiResponsePostProcessor {

    private val STOP_TOKENS = listOf(
        "<start_of_turn>",
        "<end_of_turn>",
        "<|im_start|>",
        "<|im_end|>",
        "<|user|>",
        "<|model|>",
        "<|endoftext|>",
        "<eos>",
        "[EOS]",
        "<s>",
        "</s>",
        "<pad>",
        "### Response:",
        "### Answer:",
        "### Assistant:",
        "### Human:",
        "### Instruction:"
    )

    private val LEAKED_PROMPT_PATTERNS = listOf(
        Regex("(?i)^\\s*Instruksi sistem:.*?\\n"),
        Regex("(?i)^\\s*Domain pertanyaan:.*?\\n"),
        Regex("(?i)^\\s*Pedoman Jawaban:.*?\\n"),
        Regex("(?i)^\\s*=== RUJUKAN.*?===\\s*\\n"),
        Regex("(?i)^\\s*Pertanyaan:.*?\\n"),
        Regex("(?i)^\\s*Jawaban:\\s*")
    )

    private val SECTION_HEADER_PATTERN = Regex(
        "(?i)(#{1,4}\\s*(?:Intisari|Dalil(?:\\s+Al-Qur'?an)?(?:\\s*(?:&|dan)\\s*Hadits?)?|Uraian\\s*(?:&|dan)\\s*Penjelasan|Penjelasan\\s*(?:&|dan)\\s*Uraian|Kesimpulan\\s*(?:&|dan)\\s*Amalan(?:\\s*Praktis)?|Amalan\\s*Praktis))([\\p{Lu}][\\p{Ll}])"
    )

    // Matches ***Heading:*** or ***Heading:** or **Heading:*** (triple-asterisk LLM artifact)
    private val TRIPLE_ASTERISK = Regex("(?<!\\*)\\*{3,}([^*\\n]+?)\\*{2,3}(?!\\*)")

    // Matches bold subheading **Xyz:** preceded by a non-newline character
    private val BOLD_SUBHEADING_RUNON = Regex("([^\\n])( {0,3})(\\*\\*[^*\\n]{1,80}:\\*\\*)")

    // Hanging conjunctions / prepositions in Indonesian that should not end a sentence
    private val HANGING_CONJUNCTIONS = setOf(
        "dan", "atau", "tetapi", "namun", "serta", "karena", "sebab",
        "sehingga", "agar", "supaya", "bahwa", "jika", "apabila",
        "ketika", "sedangkan", "melainkan", "dengan", "untuk", "dari",
        "ke", "di", "pada", "dalam", "oleh", "seperti", "maka"
    )

    fun process(rawResponse: String): String {
        if (rawResponse.isBlank()) return ""

        var cleaned = rawResponse

        // 1. Strip raw stop tokens
        STOP_TOKENS.forEach { token ->
            cleaned = cleaned.replace(token, "")
        }

        // 2. Strip leaked prompt headers if present at the beginning
        LEAKED_PROMPT_PATTERNS.forEach { pattern ->
            cleaned = pattern.replace(cleaned, "")
        }

        // 3. Strip internal citation markers from displayed prose
        cleaned = CitationMarkerValidator.stripMarkers(cleaned)
        cleaned = cleaned.replace(Regex("\\[\\s*]"), "")
        cleaned = cleaned.replace(Regex("\\[\\s*C\\d+\\s*]"), "")

        // 4. Normalize quotes and dashes
        cleaned = cleaned
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replace("\u2014", " - ")
            .replace("\u2013", " - ")

        // 5. Normalize triple-asterisk artifacts: ***Heading:*** → **Heading:**
        cleaned = TRIPLE_ASTERISK.replace(cleaned) { mr -> "**${mr.groupValues[1]}**" }

        // 6. Structure markdown headings (#, ##, ###) and separate them from adjoining text
        cleaned = cleaned.replace(Regex("([^\\n])\\s*(#{1,4}\\s*)"), "$1\n\n$2")
        cleaned = cleaned.replace(SECTION_HEADER_PATTERN, "$1\n\n$2")
        cleaned = cleaned.replace(Regex("(#{1,4}[^\\n*]+?)\\*+\\s*"), "$1\n\n")

        // 7. Insert blank line before bold subheadings that run on without a newline
        //    e.g. "...keutamaan.**Pertama:** ..." → "...keutamaan.\n\n**Pertama:** ..."
        cleaned = BOLD_SUBHEADING_RUNON.replace(cleaned, "$1\n\n$3")

        // 8. Structure numbered lists (e.g. "1.**Jaminan" -> "\n\n1. **Jaminan")
        cleaned = cleaned.replace(Regex("([^\\n])\\s*(\\d+\\.)\\s*(\\S)"), "$1\n\n$2 $3")
        cleaned = cleaned.replace(Regex("(\\d+\\.)\\s*(\\S)"), "$1 $2")
        cleaned = cleaned.replace(Regex("(?m)^(\\s*\\d+\\.\\s*)([^*:\\n]+?:\\*\\*)"), "$1**$2")

        // 9. Structure bold inline subheadings after sentence ends and clean bullets
        cleaned = cleaned.replace(Regex("([^\\d\\n][.?!])\\s*(\\*\\*[^*]+?:\\*\\*)"), "$1\n\n$2")
        cleaned = cleaned.replace(Regex("(\\*\\*[^*]+?:\\*\\*)\\*+\\s*"), "$1\n- ")
        cleaned = cleaned.replace(Regex("(\\*\\*[^*]+?:\\*\\*)\\s*([\\p{L}])"), "$1 $2")
        cleaned = cleaned.replace(Regex("([.?!])\\s*(?<!\\*)\\*(?!\\*)\\s*([\\p{Lu}])"), "$1\n- $2")

        // 10. Clean orphan asterisk lines (e.g. standalone lines containing only ** or *)
        cleaned = cleaned.replace(Regex("(?m)^\\s*\\*+\\s*$"), "")

        // 11. Clean punctuation spacing
        cleaned = cleaned.replace(Regex("\\s+([,.:;?!])"), "$1")
        cleaned = cleaned.replace(Regex("([,.:;?!])([\\p{L}\\p{N}])"), "$1 $2")

        // 12. Clean bullet points
        cleaned = cleaned.lines().joinToString("\n") { line ->
            val trimmedLine = line.trimStart()
            when {
                trimmedLine.startsWith("• ") -> "- " + trimmedLine.substring(2).trim()
                trimmedLine.startsWith("* ") -> "- " + trimmedLine.substring(2).trim()
                trimmedLine.startsWith("+ ") -> "- " + trimmedLine.substring(2).trim()
                else -> line
            }
        }

        // 13. Normalize whitespace: remove trailing spaces and collapse 3+ newlines to 2
        cleaned = cleaned.lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        // 14. Balance unclosed delimiters before sentence termination
        cleaned = balanceDelimiters(cleaned)

        // 15. Ensure the response ends with terminal punctuation if truncated
        cleaned = ensureTerminatedSentence(cleaned)

        return cleaned
    }

    /**
     * Closes any unpaired ** bold markers, open parentheses, or unclosed double-quotes
     * that are hanging at the end of the response due to token budget truncation.
     */
    private fun balanceDelimiters(text: String): String {
        var result = text

        // Balance bold **: count occurrences; if odd, append closing **
        val boldCount = Regex("\\*\\*").findAll(result).count()
        if (boldCount % 2 != 0) {
            result = "$result**"
        }

        // Balance open parentheses
        val openParens = result.count { it == '(' }
        val closeParens = result.count { it == ')' }
        if (openParens > closeParens) {
            result = result + ")".repeat(openParens - closeParens)
        }

        // Balance double-quotes: if trailing content after last " has no closing ", close it
        val quoteCount = result.count { it == '"' }
        if (quoteCount % 2 != 0) {
            result = "$result\""
        }

        return result
    }

    /**
     * If the response ends abruptly without terminal punctuation (due to token budget limit),
     * this checks if the last word is a hanging conjunction/preposition and trims back
     * to the last complete sentence, or appends a closing period.
     */
    private fun ensureTerminatedSentence(text: String): String {
        if (text.isBlank()) return text
        val lastChar = text.last()
        if (lastChar in setOf('.', '!', '?', '"', '\'', ')', ']', '*')) {
            return text
        }

        // Check if the last word is a hanging conjunction
        val lastWord = text.trimEnd().substringAfterLast(' ').lowercase()
        val lastPeriod = text.lastIndexOfAny(charArrayOf('.', '!', '?'))

        if (lastWord in HANGING_CONJUNCTIONS && lastPeriod > 0) {
            // Trim back to last complete sentence
            return text.substring(0, lastPeriod + 1).trim()
        }

        return if (lastPeriod > text.length * 0.7) {
            text.substring(0, lastPeriod + 1).trim()
        } else {
            "$text."
        }
    }
}
