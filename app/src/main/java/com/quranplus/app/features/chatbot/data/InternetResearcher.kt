package com.quranplus.app.features.chatbot.data

import android.content.Context
import android.text.Html
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.AuthorityTier
import com.quranplus.app.features.rag.domain.CitationTargetValidator
import com.quranplus.app.features.rag.domain.EvidenceKind
import com.quranplus.app.features.rag.domain.InternetSearchProvider
import com.quranplus.app.features.rag.domain.IslamicQueryAnalyzer
import com.quranplus.app.features.rag.domain.IslamicQueryPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

internal const val MAX_RESEARCH_QUERY_LENGTH = 160

/**
 * Small, keyless web fallback for questions that have no adequate local evidence.
 *
 * Wikipedia is deliberately treated as supplementary web context, never as Quran,
 * Hadist, or a religious authority. The returned URL is retained as the citation
 * target so the user can inspect the exact page used by the model.
 */
class InternetResearcher(
    private val context: Context,
    private val client: OkHttpClient = createClient(context)
) {

    suspend fun search(query: String, limit: Int = DEFAULT_RESULT_LIMIT): List<RetrievedCitation> =
        withContext(Dispatchers.IO) {
            val normalizedQuery = query.trim().replace(Regex("\\s+"), " ")
                .take(MAX_RESEARCH_QUERY_LENGTH)
            if (normalizedQuery.length < MIN_QUERY_LENGTH || !hasValidatedInternet()) {
                return@withContext emptyList()
            }

            val plan = IslamicQueryAnalyzer.analyze(normalizedQuery)
            val quran = if (!plan.prefersHadith || plan.prefersQuran) {
                QuranCloudSearchProvider(client).search(normalizedQuery, plan, limit)
            } else {
                emptyList()
            }
            val hadith = if (plan.prefersHadith) {
                SunnahPublicSearchProvider(client).search(normalizedQuery, plan, limit)
            } else {
                emptyList()
            }

            val editorial = if (plan.domain != com.quranplus.app.features.rag.domain.IslamicQuestionDomain.SEJARAH_TARIKH &&
                !plan.prefersHadith
            ) {
                EditorialWebSearchProvider(client).search(
                    query = normalizedQuery,
                    plan = plan,
                    limit = limit - quran.size
                )
            } else {
                emptyList()
            }

            // Wikipedia is useful for tarikh/general context only. It is never
            // used as a substitute for a Quran, Hadith, or fatwa source.
            val wiki = if (plan.domain == com.quranplus.app.features.rag.domain.IslamicQuestionDomain.SEJARAH_TARIKH ||
                plan.domain == com.quranplus.app.features.rag.domain.IslamicQuestionDomain.UMUM
            ) {
                val indonesian = searchWiki(
                    host = INDONESIAN_WIKI_HOST,
                    query = normalizedQuery,
                    limit = limit - quran.size
                )
                val english = searchWiki(
                    host = ENGLISH_WIKI_HOST,
                    query = normalizedQuery,
                    limit = limit - quran.size - indonesian.size
                )
                indonesian + english
            } else {
                emptyList()
            }
            (quran + hadith + editorial + wiki)
                .distinctBy { it.deepLinkTarget ?: it.sourceId }
                .take(limit)
        }

    private fun searchWiki(
        host: String,
        query: String,
        limit: Int
    ): List<RetrievedCitation> {
        if (limit <= 0) return emptyList()
        val url = host.toHttpUrl().newBuilder()
            .addPathSegment("w")
            .addPathSegment("api.php")
            .addQueryParameter("action", "query")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", query)
            .addQueryParameter("gsrnamespace", "0")
            .addQueryParameter("gsrlimit", limit.toString())
            .addQueryParameter("prop", "extracts|info")
            .addQueryParameter("exintro", "1")
            .addQueryParameter("explaintext", "1")
            .addQueryParameter("exchars", MAX_EXTRACT_LENGTH.toString())
            .addQueryParameter("inprop", "url")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Accept-Language", "id-ID,id;q=0.9,en;q=0.6")
            .header("User-Agent", "QuranPlus/1.0 (Android; local-first research)")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = readBoundedBody(response, MAX_WEB_RESPONSE_BYTES) ?: return@use emptyList()
                MediaWikiCitationParser.parse(body, host, limit)
            }
        }.getOrDefault(emptyList())
    }

    private fun hasValidatedInternet(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private companion object {
        const val INDONESIAN_WIKI_HOST = "https://id.wikipedia.org"
        const val ENGLISH_WIKI_HOST = "https://en.wikipedia.org"
        const val DEFAULT_RESULT_LIMIT = 4
        const val MAX_EXTRACT_LENGTH = 1200
        const val MIN_QUERY_LENGTH = 3

        fun createClient(context: Context): OkHttpClient {
            val cacheDirectory = File(context.cacheDir, "internet-research-http")
            return OkHttpClient.Builder()
                .cache(Cache(cacheDirectory, 8L * 1024L * 1024L))
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(12, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        }
    }
}

/**
 * Bounded HTML search for allowlisted editorial sources. This intentionally
 * extracts article cards only; it never executes page JavaScript or follows a
 * citation to an untrusted host.
 */
internal class EditorialWebSearchProvider(
    private val client: OkHttpClient
) : InternetSearchProvider {

    override val providerId: String = "allowlisted-editorial"

    override suspend fun search(
        query: String,
        plan: IslamicQueryPlan,
        limit: Int
    ): List<RetrievedCitation> {
        if (limit <= 0 || plan.prefersHadith) return emptyList()
        val sources = when (plan.domain) {
            com.quranplus.app.features.rag.domain.IslamicQuestionDomain.HUKUM_FIQIH,
            com.quranplus.app.features.rag.domain.IslamicQuestionDomain.ADAB_AKHLAK,
            com.quranplus.app.features.rag.domain.IslamicQuestionDomain.IBADAH,
            com.quranplus.app.features.rag.domain.IslamicQuestionDomain.UMUM -> EDITORIAL_SOURCES
            com.quranplus.app.features.rag.domain.IslamicQuestionDomain.SEJARAH_TARIKH -> emptyList()
        }
        return sources.flatMap { source -> searchSource(source, query, limit) }
            .distinctBy { it.deepLinkTarget ?: it.sourceId }
            .take(limit)
    }

    private fun searchSource(
        source: EditorialSource,
        query: String,
        limit: Int
    ): List<RetrievedCitation> {
        val url = source.searchUrl(query, limit)
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "id-ID,id;q=0.9")
            .header("User-Agent", "QuranPlus/1.0 (allowlisted editorial fallback)")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.request.url.host != source.host) {
                    return@use emptyList()
                }
                val body = readBoundedBody(response, MAX_WEB_RESPONSE_BYTES) ?: return@use emptyList()
                EditorialCitationParser.parse(body, source, limit)
            }
        }.getOrDefault(emptyList())
    }

    internal data class EditorialSource(
        val id: String,
        val name: String,
        val baseUrl: String,
        val host: String,
        val searchUrl: (String, Int) -> String
    )

    private companion object {
        val EDITORIAL_SOURCES = listOf(
            EditorialSource(
                id = "nu-online",
                name = "NU Online",
                baseUrl = "https://islam.nu.or.id",
                host = "islam.nu.or.id",
                searchUrl = { query, _ ->
                    "https://islam.nu.or.id/search?q=${java.net.URLEncoder.encode(query, Charsets.UTF_8.name())}"
                }
            ),
            EditorialSource(
                id = "tarjih-muhammadiyah",
                name = "Majelis Tarjih Muhammadiyah",
                baseUrl = "https://tarjih.or.id",
                host = "tarjih.or.id",
                searchUrl = { query, _ ->
                    "https://tarjih.or.id/?s=${java.net.URLEncoder.encode(query, Charsets.UTF_8.name())}"
                }
            )
        )
    }
}

internal object EditorialCitationParser {
    private val articlePattern = Regex("(?is)<article\\b[^>]*>(.*?)</article>")
    private val anchorPattern = Regex("(?is)<a\\b[^>]*href\\s*=\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>")
    private val paragraphPattern = Regex("(?is)<p\\b[^>]*>(.*?)</p>")

    fun parse(
        html: String,
        source: EditorialWebSearchProvider.EditorialSource,
        limit: Int
    ): List<RetrievedCitation> {
        val blocks = articlePattern.findAll(html).map { it.groupValues[1] }.toList()
            .ifEmpty {
                anchorPattern.findAll(html)
                    .map { it.value }
                    .filter { paragraphPattern.containsMatchIn(it) }
                    .toList()
            }
        return buildList {
            for (block in blocks) {
                val anchor = anchorPattern.find(block) ?: continue
                val title = plainText(anchor.groupValues[2]).take(180)
                if (title.length < 8 || isNavigationTitle(title)) continue
                val url = resolveUrl(source, anchor.groupValues[1]) ?: continue
                val paragraphs = paragraphPattern.findAll(block)
                    .map { plainText(it.groupValues[1]) }
                    .filter { it.length >= 24 }
                    .toList()
                val snippet = (paragraphs.firstOrNull() ?: plainText(block))
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(800)
                if (snippet.length < 24) continue
                add(
                    RetrievedCitation(
                        sourceId = "web:${source.id}:${url.hashCode().toUInt()}",
                        sourceType = "internet",
                        title = "${source.name} • $title",
                        reference = "${source.name} — $title",
                        textSnippet = snippet,
                        score = 0.78f,
                        collection = source.id,
                        identifier = url,
                        deepLinkTarget = url,
                        canonicalUrl = url,
                        evidenceKind = EvidenceKind.WEB,
                        authorityTier = AuthorityTier.TRUSTED_EDITORIAL,
                        providerId = source.id,
                        retrievedAt = System.currentTimeMillis()
                    )
                )
                if (size >= limit) break
            }
        }
    }

    private fun resolveUrl(
        source: EditorialWebSearchProvider.EditorialSource,
        rawUrl: String
    ): String? {
        val resolved = source.baseUrl.toHttpUrl().resolve(rawUrl.trim()) ?: return null
        if (resolved.host != source.host) return null
        return CitationTargetValidator.validateHttpsUrl(resolved.toString())
    }

    private fun plainText(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun isNavigationTitle(title: String): Boolean =
        title.lowercase().let { normalized ->
            normalized in setOf("selengkapnya", "baca juga", "read more", "home", "menu")
        }
}

internal object MediaWikiCitationParser {

    fun parse(json: String, host: String, limit: Int): List<RetrievedCitation> {
        val pages = runCatching {
            JSONObject(json)
                .optJSONObject("query")
                ?.optJSONArray("pages")
                ?: JSONArray()
        }.getOrElse { JSONArray() }

        return buildList {
            for (index in 0 until pages.length()) {
                val page = pages.optJSONObject(index) ?: continue
                val title = page.optString("title").trim()
                val extract = page.optString("extract").trim()
                val pageId = page.optLong("pageid", -1L)
                val url = safeHttpsUrl(page.optString("fullurl"), host, title)
                if (title.isBlank() || extract.isBlank() || pageId <= 0L || url == null) continue

                add(
                    RetrievedCitation(
                        sourceId = "web:$host:$pageId",
                        sourceType = "internet",
                        title = "Internet • $title",
                        reference = "$host — $title",
                        textSnippet = extract.take(1200),
                        score = (0.74f - index * 0.04f).coerceAtLeast(0.60f),
                        collection = host,
                        identifier = pageId.toString(),
                        deepLinkTarget = url,
                        canonicalUrl = url,
                        authorityTier = AuthorityTier.GENERAL_REFERENCE,
                        providerId = "mediawiki-wikipedia",
                        retrievedAt = System.currentTimeMillis()
                    )
                )
                if (size >= limit) break
            }
        }
    }

    private fun safeHttpsUrl(rawUrl: String, host: String, title: String): String? {
        val candidate = rawUrl.trim().ifBlank {
            val encodedTitle = java.net.URLEncoder.encode(title, Charsets.UTF_8.name())
                .replace("+", "_")
            "$host/wiki/$encodedTitle"
        }
        val uri = runCatching { java.net.URI(candidate) }.getOrNull() ?: return null
        val expectedHost = runCatching { java.net.URI(host).host }.getOrNull()
        if (uri.host == null || !uri.host.equals(expectedHost, ignoreCase = true)) return null
        return CitationTargetValidator.validateHttpsUrl(uri.toString())
    }
}

/**
 * Keyless fallback for public Sunnah.com search pages. The page may reject
 * automated clients; that is an expected unavailable state, never a reason to
 * use an unverified mirror or invent a Hadist result.
 */
internal class SunnahPublicSearchProvider(
    private val client: OkHttpClient
) : InternetSearchProvider {
    override val providerId: String = "sunnah-public"

    override suspend fun search(
        query: String,
        plan: IslamicQueryPlan,
        limit: Int
    ): List<RetrievedCitation> {
        if (limit <= 0 || !plan.prefersHadith) return emptyList()
        val url = "https://sunnah.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.take(MAX_RESEARCH_QUERY_LENGTH))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "en-US,en;q=0.8")
            .header("User-Agent", "QuranPlus/1.0 (allowlisted Sunnah fallback)")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.request.url.host != SUNNAH_HOST) {
                    return@use emptyList()
                }
                val body = readBoundedBody(response, MAX_WEB_RESPONSE_BYTES) ?: return@use emptyList()
                SunnahCitationParser.parse(body, limit)
            }
        }.getOrDefault(emptyList())

    }

    private companion object {
        const val SUNNAH_HOST = "sunnah.com"
    }
}

internal object SunnahCitationParser {
    private val blockPattern = Regex("(?is)<(?:article|li|div)\\b[^>]*>(.{0,8000}?)</(?:article|li|div)>")
    private val anchorPattern = Regex(
        "(?is)<a\\b[^>]*href\\s*=\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>"
    )
    private val resultPathPattern = Regex(
        "^/(bukhari|muslim|abudawud|tirmidhi|nasai|ibnmajah|ahmad|malik|darimi):([0-9]+)(?:[/?#].*)?$",
        RegexOption.IGNORE_CASE
    )

    fun parse(html: String, limit: Int): List<RetrievedCitation> {
        if (limit <= 0) return emptyList()
        val blocks = blockPattern.findAll(html).map { it.groupValues[1] }.toList()
        val candidates = if (blocks.isEmpty()) listOf(html) else blocks
        return buildList {
            for (block in candidates) {
                val anchor = anchorPattern.find(block) ?: continue
                val path = anchor.groupValues[1].trim().substringBefore('#')
                val result = resultPathPattern.matchEntire(path) ?: continue
                val collection = result.groupValues[1].lowercase()
                val number = result.groupValues[2].toIntOrNull() ?: continue
                val url = CitationTargetValidator.validateHttpsUrl("https://sunnah.com$path") ?: continue
                val text = Html.fromHtml(block, Html.FROM_HTML_MODE_LEGACY).toString()
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(800)
                if (text.length < 24) continue
                add(
                    RetrievedCitation(
                        sourceId = "web:sunnah:$collection:$number",
                        sourceType = "internet",
                        title = "Sunnah.com • ${collection.replaceFirstChar { it.uppercase() }} no. $number",
                        reference = "Sunnah.com — $collection:$number",
                        textSnippet = text,
                        score = (0.82f - size * 0.03f).coerceAtLeast(0.68f),
                        collection = collection,
                        identifier = "$collection:$number",
                        deepLinkTarget = url,
                        canonicalUrl = url,
                        authorityTier = AuthorityTier.TRUSTED_EDITORIAL,
                        providerId = "sunnah-public",
                        retrievedAt = System.currentTimeMillis()
                    )
                )
                if (size >= limit) break
            }
        }.distinctBy { it.deepLinkTarget ?: it.sourceId }
    }
}

/** Keyless Quran fallback. The app stores only the returned citation metadata. */
internal class QuranCloudSearchProvider(
    private val client: OkHttpClient
) : InternetSearchProvider {
    override val providerId: String = "alquran-cloud"

    override suspend fun search(
        query: String,
        plan: IslamicQueryPlan,
        limit: Int
    ): List<RetrievedCitation> {
        if (limit <= 0) return emptyList()
        val url = "https://api.alquran.cloud/v1/search".toHttpUrl()
            .newBuilder()
            .addPathSegment(query.trim())
            .addPathSegment("all")
            .addPathSegment("id")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "QuranPlus/1.0 (allowlisted Quran fallback)")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = readBoundedBody(response, MAX_WEB_RESPONSE_BYTES) ?: return@use emptyList()
                parse(body, limit)
            }
        }.getOrDefault(emptyList())
    }

    private fun parse(json: String, limit: Int): List<RetrievedCitation> {
        val matches = runCatching {
            JSONObject(json).optJSONObject("data")?.optJSONArray("matches") ?: JSONArray()
        }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until matches.length()) {
                val item = matches.optJSONObject(index) ?: continue
                val surah = item.optJSONObject("surah") ?: continue
                val surahNumber = surah.optInt("number", -1)
                val ayahNumber = item.optInt("numberInSurah", -1)
                val text = item.optString("text").trim()
                if (surahNumber !in 1..114 || ayahNumber <= 0 || text.isBlank()) continue
                val canonical = "https://alquran.cloud/ayah/$surahNumber:$ayahNumber/id.indonesian"
                add(
                    RetrievedCitation(
                        sourceId = "quran-cloud:$surahNumber:$ayahNumber",
                        sourceType = "quran",
                        title = "QS. ${surah.optString("englishName", "Al-Quran")} ($surahNumber):$ayahNumber",
                        reference = "QS. ${surah.optString("englishName", "Al-Quran")} ($surahNumber):$ayahNumber",
                        textSnippet = "Artinya: \"${text.take(MAX_TEXT_LENGTH)}\"",
                        score = (0.86f - index * 0.03f).coerceAtLeast(0.65f),
                        collection = "quran",
                        identifier = "$surahNumber:$ayahNumber",
                        deepLinkTarget = canonical,
                        canonicalUrl = canonical,
                        surahNumber = surahNumber,
                        ayahNumber = ayahNumber,
                        evidenceKind = EvidenceKind.QURAN,
                        authorityTier = AuthorityTier.PRIMARY_TEXT,
                        providerId = providerId,
                        retrievedAt = System.currentTimeMillis()
                    )
                )
                if (size >= limit) break
            }
        }
    }

    private companion object {
        const val MAX_TEXT_LENGTH = 1000
    }
}

private fun readBoundedBody(response: okhttp3.Response, maxBytes: Int): String? {
    val body = response.body ?: return null
    if (body.contentLength() > maxBytes) return null
    return runCatching {
        body.source().use { source ->
            source.readUtf8(maxBytes.toLong() + 1L)
        }
    }.getOrNull()?.takeIf { value ->
        value.toByteArray(Charsets.UTF_8).size <= maxBytes
    }
}

private const val MAX_WEB_RESPONSE_BYTES = 512 * 1024
