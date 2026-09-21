package com.quranplus.app.features.rag.domain

import java.net.URI

/** The provenance class of evidence shown to the language model. */
enum class EvidenceKind {
    QURAN,
    HADITH,
    RAG_DOCUMENT,
    WEB;

    companion object {
        fun fromSourceType(sourceType: String): EvidenceKind = when (sourceType.trim().lowercase()) {
            "quran" -> QURAN
            "hadith", "hadist", "hadits" -> HADITH
            "internet", "web" -> WEB
            else -> RAG_DOCUMENT
        }
    }
}

/** Authority is metadata for how a source must be described, not a truth claim. */
enum class AuthorityTier {
    PRIMARY_TEXT,
    OFFICIAL_SOURCE,
    TRUSTED_EDITORIAL,
    GENERAL_REFERENCE
}

fun authorityTierFor(sourceType: String): AuthorityTier = when (
    EvidenceKind.fromSourceType(sourceType)
) {
    EvidenceKind.QURAN, EvidenceKind.HADITH -> AuthorityTier.PRIMARY_TEXT
    EvidenceKind.RAG_DOCUMENT -> AuthorityTier.TRUSTED_EDITORIAL
    EvidenceKind.WEB -> AuthorityTier.GENERAL_REFERENCE
}

/** Generation lifecycle persisted alongside an assistant message. */
enum class GenerationStatus {
    COMPLETE,
    STREAMING,
    STOPPED,
    TIMEOUT,
    UNAVAILABLE,
    FALLBACK
}

/** Only these hosts may be opened from a persisted citation or a web provider. */
object CitationTargetValidator {
    private val allowedHosts = setOf(
        "alquran.cloud",
        "api.alquran.cloud",
        "sunnah.com",
        "beta.sunnah.com",
        "id.wikipedia.org",
        "en.wikipedia.org",
        "lajnah.kemenag.go.id",
        "quran.kemenag.go.id",
        "nu.or.id",
        "islam.nu.or.id",
        "tarjih.or.id",
        "muhammadiyah.or.id",
        "rumaysho.com",
        "muslim.or.id",
        "almanhaj.or.id",
        "detik.com"
    )

    fun validateHttpsUrl(rawUrl: String?): String? {
        val candidate = rawUrl?.trim().orEmpty()
        if (candidate.isBlank() || candidate.length > MAX_URL_LENGTH) return null
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.userInfo != null || uri.port != -1 && uri.port != 443) return null
        if (host !in allowedHosts && allowedHosts.none { host.endsWith(".$it") }) return null
        if (uri.path?.contains("\\") == true) return null
        return uri.toString()
    }

    fun isAllowedHost(host: String): Boolean {
        val normalized = host.trim().lowercase().removeSuffix(".")
        return normalized in allowedHosts || allowedHosts.any { normalized.endsWith(".$it") }
    }

    private const val MAX_URL_LENGTH = 2048
}

/** Identifies web-derived evidence even when its semantic kind is Quran/Hadith. */
fun RetrievedCitation.isInternetSourced(): Boolean =
    sourceType.trim().lowercase() in setOf("internet", "web") ||
        providerId in setOf(
            "alquran-cloud",
            "sunnah-public",
            "nu-online",
            "tarjih-muhammadiyah",
            "duckduckgo-web",
            "mediawiki-wikipedia"
        )
