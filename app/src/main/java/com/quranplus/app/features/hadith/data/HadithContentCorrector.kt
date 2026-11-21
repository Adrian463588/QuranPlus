package com.quranplus.app.features.hadith.data

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Single-source-of-truth authoritative correction applicator backed by
 * `app/src/main/assets/hadith/content-corrections.json`.
 */
class HadithContentCorrector(
    private val loadJson: () -> String = DEFAULT_LOADER
) {
    data class CorrectionRule(
        val correctionId: String,
        val collectionId: String,
        val stableId: String,
        val canonicalReference: Int,
        val archiveReference: Int?,
        val language: String,
        val acceptedOriginalSha256s: Set<String>,
        val correctedTextSha256: String,
        val correctedText: String,
        val arabicWhitespaceFrom: String?,
        val arabicWhitespaceTo: String?,
        val sourceReference: String
    )

    sealed interface CorrectionOutcome {
        data class Applied(
            val correctionId: String,
            val fixedArabic: String,
            val fixedTranslationId: String
        ) : CorrectionOutcome

        data class AlreadyCorrected(
            val correctionId: String,
            val fixedArabic: String,
            val fixedTranslationId: String
        ) : CorrectionOutcome

        data class DigestMismatch(
            val correctionId: String,
            val expectedDigests: Set<String>,
            val actualDigest: String
        ) : CorrectionOutcome

        data object NoRule : CorrectionOutcome
    }

    private val rules: List<CorrectionRule> by lazy {
        val raw = runCatching { loadJson() }.getOrDefault("{}")
        val doc = runCatching { JSONObject(raw) }.getOrNull() ?: return@lazy emptyList()
        val array = doc.optJSONArray("corrections") ?: return@lazy emptyList()
        (0 until array.length()).map { idx ->
            val obj = array.getJSONObject(idx)
            val acceptedArr = obj.optJSONArray("accepted_original_sha256s")
            val accepted = buildSet {
                add(obj.getString("original_text_sha256"))
                if (acceptedArr != null) {
                    for (i in 0 until acceptedArr.length()) {
                        add(acceptedArr.getString(i))
                    }
                }
            }
            CorrectionRule(
                correctionId = obj.getString("correction_id"),
                collectionId = obj.getString("collection_id"),
                stableId = obj.getString("stable_id"),
                canonicalReference = obj.getInt("canonical_reference"),
                archiveReference = obj.optInt("archive_reference", -1).takeIf { it > 0 },
                language = obj.optString("language", "id"),
                acceptedOriginalSha256s = accepted,
                correctedTextSha256 = obj.getString("corrected_text_sha256"),
                correctedText = obj.getString("corrected_text"),
                arabicWhitespaceFrom = obj.optString("arabic_whitespace_from").takeIf { it.isNotBlank() },
                arabicWhitespaceTo = obj.optString("arabic_whitespace_to").takeIf { it.isNotBlank() },
                sourceReference = obj.optString("source_reference")
            )
        }
    }

    fun ruleCount(): Int = rules.size

    fun apply(
        collectionId: String,
        hadithNumber: Int,
        stableId: String?,
        arabicText: String,
        translationId: String,
        matchByCanonicalOrArchive: Boolean = true
    ): CorrectionOutcome {
        val rule = findRule(collectionId, hadithNumber, stableId, matchByCanonicalOrArchive)
            ?: return CorrectionOutcome.NoRule

        val repairedArabic = repairArabicWhitespace(arabicText, rule)
        val arabicChanged = repairedArabic != arabicText
        val translationDigest = sha256Hex(translationId)

        return when (translationDigest) {
            rule.correctedTextSha256 -> {
                if (arabicChanged) {
                    CorrectionOutcome.Applied(rule.correctionId, repairedArabic, translationId)
                } else {
                    CorrectionOutcome.AlreadyCorrected(rule.correctionId, repairedArabic, translationId)
                }
            }
            in rule.acceptedOriginalSha256s -> {
                CorrectionOutcome.Applied(rule.correctionId, repairedArabic, rule.correctedText)
            }
            else -> CorrectionOutcome.DigestMismatch(
                correctionId = rule.correctionId,
                expectedDigests = rule.acceptedOriginalSha256s + rule.correctedTextSha256,
                actualDigest = translationDigest
            )
        }
    }

    fun applyTranslationOnly(
        collectionId: String,
        referenceOrArchiveNumber: Int,
        stableId: String?,
        translationId: String
    ): String = when (
        val outcome = apply(
            collectionId = collectionId,
            hadithNumber = referenceOrArchiveNumber,
            stableId = stableId,
            arabicText = "",
            translationId = translationId,
            matchByCanonicalOrArchive = true
        )
    ) {
        is CorrectionOutcome.Applied -> outcome.fixedTranslationId
        is CorrectionOutcome.AlreadyCorrected -> outcome.fixedTranslationId
        else -> translationId
    }

    private fun findRule(
        collectionId: String,
        hadithNumber: Int,
        stableId: String?,
        matchByCanonicalOrArchive: Boolean
    ): CorrectionRule? {
        if (!stableId.isNullOrBlank()) {
            rules.firstOrNull { it.collectionId == collectionId && it.stableId == stableId }?.let { return it }
        }
        if (!matchByCanonicalOrArchive) return null
        return rules.firstOrNull { rule ->
            rule.collectionId == collectionId &&
                (rule.canonicalReference == hadithNumber || rule.archiveReference == hadithNumber)
        }
    }

    private fun repairArabicWhitespace(arabicText: String, rule: CorrectionRule): String {
        val from = rule.arabicWhitespaceFrom ?: return arabicText
        val to = rule.arabicWhitespaceTo ?: return arabicText
        return if (from in arabicText) arabicText.replace(from, to) else arabicText
    }

    companion object {
        private val DEFAULT_LOADER: () -> String = {
            listOf(
                File("src/main/assets/hadith/content-corrections.json"),
                File("app/src/main/assets/hadith/content-corrections.json")
            ).firstOrNull(File::isFile)?.readText(Charsets.UTF_8) ?: "{}"
        }

        fun sha256Hex(text: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
