package com.quranplus.app.features.chatbot.data

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.database.dao.ChatDao
import com.quranplus.app.core.database.entity.ChatMessageEntity
import com.quranplus.app.features.chatbot.domain.ChatMessage
import com.quranplus.app.features.chatbot.domain.ChatRepository
import com.quranplus.app.features.chatbot.domain.ChatSession
import com.quranplus.app.features.chatbot.domain.GenerationEvent
import com.quranplus.app.features.chatbot.domain.MessageRole
import com.quranplus.app.features.chatbot.domain.RagGenerationResult
import com.quranplus.app.features.hadith.data.HadithBundleManifest
import com.quranplus.app.features.rag.domain.GroundingUnavailable
import com.quranplus.app.features.rag.domain.GenerationStatus
import com.quranplus.app.features.rag.domain.CitationMarkerValidator
import com.quranplus.app.features.rag.domain.EvidenceKind
import com.quranplus.app.features.rag.domain.isInternetSourced
import com.quranplus.app.features.rag.domain.navigationTarget
import com.quranplus.app.features.rag.data.EmbeddingService
import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.RagRuntimeCoordinator
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.VectorRetriever
import com.quranplus.app.features.rag.domain.IslamicQueryAnalyzer
import com.quranplus.app.features.rag.domain.IslamicQueryPlan
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val embeddingService: EmbeddingService,
    private val vectorRetriever: VectorRetriever,
    private val ragPipeline: RagPipeline,
    private val llmRunner: LiteRtLmRunner,
    private val database: QuranDatabase,
    private val runtimeCoordinator: RagRuntimeCoordinator,
    private val internetResearcher: InternetResearcher,
    private val preferencesManager: PreferencesManager
) : ChatRepository {

    override fun getChatHistory(conversationId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessages(conversationId).map { entities ->
            entities.map(::toDomain)
        }
    }

    override fun getChatSessions(): Flow<List<ChatSession>> {
        return chatDao.getAllMessages().map { messages ->
            messages.groupBy { it.conversationId }
                .map { (convId, msgList) ->
                    val sorted = msgList.sortedBy { it.timestamp }
                    val firstUserMsg = sorted.firstOrNull { it.role == "user" }?.content
                    val lastMsg = sorted.lastOrNull()
                    val title = firstUserMsg?.take(40)?.let { if (it.length >= 40) "$it..." else it }
                        ?: "Obrolan Baru"
                    ChatSession(
                        conversationId = convId,
                        title = title,
                        lastMessage = lastMsg?.content ?: "",
                        timestamp = lastMsg?.timestamp ?: System.currentTimeMillis()
                    )
                }
                .sortedByDescending { it.timestamp }
        }
    }

    override suspend fun saveMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                id = message.id,
                conversationId = message.conversationId,
                role = message.role.name.lowercase(),
                content = message.content,
                citationsJson = message.citations
                    .takeIf { it.isNotEmpty() }
                    ?.let(CitationJsonCodec::encode),
                completionStatus = message.generationStatus.name.lowercase(),
                timestamp = message.timestamp
            )
        )
    }

    override suspend fun clearHistory(conversationId: String) {
        chatDao.clearConversation(conversationId)
        llmRunner.clearConversation(conversationId)
    }

    override suspend fun clearAllHistory() {
        chatDao.clearAllConversations()
        llmRunner.clearAllConversations()
    }

    override suspend fun generateRagResponse(
        conversationId: String,
        userQuery: String,
        persona: AiPersona,
        customPrompt: String?
    ): RagGenerationResult {
        // Local Quran/Hadist/RAG evidence always has priority. Web research is only
        // consulted when local evidence is absent or below the relevance contract.
        val citations = withTimeoutOrNull(RETRIEVAL_TIMEOUT_MS) {
            resolveGroundingCitations(userQuery)
        } ?: throw GroundingUnavailable(
            "Pencarian rujukan lokal terlalu lama. Silakan coba pertanyaan yang lebih singkat."
        )
        if (citations.isEmpty()) {
            val onlineEnabled = preferencesManager.onlineResearchEnabled.first()
            throw GroundingUnavailable(
                if (onlineEnabled) {
                    "Tidak ditemukan rujukan lokal atau sumber internet yang dapat diverifikasi."
                } else {
                    "Tidak ditemukan rujukan lokal. Aktifkan fallback internet untuk mencari sumber tambahan."
                }
            )
        }

        // RagPipeline fails closed when retrieval has no verified source.
        val augmentedPrompt = ragPipeline.buildAugmentedPrompt(
            question = userQuery,
            persona = persona,
            customPrompt = customPrompt,
            citations = citations,
            domain = IslamicQueryAnalyzer.analyze(userQuery).domain
        )
        val history = getHistoryBeforeCurrentTurn(conversationId, userQuery)

        // 4. Generate Stream with Fallback & Quality Guardrails
        val generationStatus = MutableStateFlow(GenerationStatus.STREAMING)
        val stream: Flow<GenerationEvent> = flow {
            if (llmRunner.isAvailable()) {
                var receivedTokens = false
                val buffer = StringBuilder()
                var fallbackEmitted = false
                try {
                    val completed = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                        runtimeCoordinator.withInference {
                            llmRunner.generate(conversationId, augmentedPrompt, history).collect { token ->
                                receivedTokens = true
                                buffer.append(token)
                                emit(GenerationEvent.Append(token))
                            }
                        }
                        true
                    } ?: false

                    if (!completed && !receivedTokens) {
                        generationStatus.value = GenerationStatus.FALLBACK
                        emit(GenerationEvent.Replace(generateFallbackAnswer(
                            citations,
                            "Model belum selesai dalam batas waktu; berikut rujukan lokal yang terverifikasi."
                        )))
                        fallbackEmitted = true
                    }
                    if (!completed && receivedTokens) {
                        generationStatus.value = GenerationStatus.TIMEOUT
                    }
                    if (completed && receivedTokens) {
                        val validation = CitationMarkerValidator.validate(buffer.toString(), citations)
                        if (!validation.isValid) {
                            generationStatus.value = GenerationStatus.FALLBACK
                            Log.w(TAG, "Model returned invalid citation markers: ${validation.invalidIds}")
                            llmRunner.clearConversation(conversationId)
                            emit(GenerationEvent.Replace(generateFallbackAnswer(
                                citations,
                                "Jawaban model tidak dapat diverifikasi; berikut rujukan yang ditemukan."
                            )))
                            fallbackEmitted = true
                        } else {
                            generationStatus.value = GenerationStatus.COMPLETE
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    generationStatus.value = GenerationStatus.UNAVAILABLE
                    Log.w(TAG, "LiteRT-LM generation failed; using grounded fallback", e)
                    if (!receivedTokens) {
                        generationStatus.value = GenerationStatus.FALLBACK
                        val fallback = generateFallbackAnswer(citations)
                        emit(GenerationEvent.Replace(fallback))
                        fallbackEmitted = true
                    }
                }
                if ((!receivedTokens || buffer.isBlank()) && !fallbackEmitted) {
                    generationStatus.value = GenerationStatus.FALLBACK
                    llmRunner.clearConversation(conversationId)
                    val fallback = generateFallbackAnswer(citations)
                    emit(GenerationEvent.Replace(fallback))
                }
            } else {
                generationStatus.value = GenerationStatus.FALLBACK
                val fallback = generateFallbackAnswer(citations)
                emit(GenerationEvent.Replace(fallback))
            }
        }.flowOn(Dispatchers.Default)

        return RagGenerationResult(
            tokenStream = stream,
            citations = citations,
            generationStatus = generationStatus.asStateFlow()
        )
    }

    private suspend fun getHistoryBeforeCurrentTurn(
        conversationId: String,
        userQuery: String
    ): List<ChatMessage> {
        val history = chatDao.getMessages(conversationId)
            .first()
            .map(::toDomain)
        val lastMessage = history.lastOrNull()
        return if (lastMessage?.role == MessageRole.USER && lastMessage.content == userQuery) {
            history.dropLast(1)
        } else {
            history
        }
    }

    private fun toDomain(entity: ChatMessageEntity): ChatMessage = ChatMessage(
        id = entity.id,
        conversationId = entity.conversationId,
        role = when (entity.role.lowercase()) {
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            else -> MessageRole.SYSTEM
        },
        content = entity.content,
        citations = CitationJsonCodec.decode(entity.citationsJson),
        generationStatus = entity.completionStatus.toGenerationStatus(),
        timestamp = entity.timestamp
    )

    private fun String.toGenerationStatus(): GenerationStatus = when (lowercase()) {
        "streaming" -> GenerationStatus.STREAMING
        "stopped" -> GenerationStatus.STOPPED
        "timeout" -> GenerationStatus.TIMEOUT
        "unavailable" -> GenerationStatus.UNAVAILABLE
        "fallback" -> GenerationStatus.FALLBACK
        else -> GenerationStatus.COMPLETE
    }

    private suspend fun resolveGroundingCitations(userQuery: String): List<RetrievedCitation> {
        val plan = IslamicQueryAnalyzer.analyze(userQuery)
        val localCitations = retrieveGroundingCitations(plan)
        if (hasAdequateLocalGrounding(localCitations, plan)) return localCitations

        if (!preferencesManager.onlineResearchEnabled.first()) return localCitations

        val onlineCitations = internetResearcher.search(plan.semanticQuery)
        return selectGroundingCitations(
            candidates = localCitations + onlineCitations,
            plan = plan
        )
    }

    private suspend fun retrieveGroundingCitations(plan: IslamicQueryPlan): List<RetrievedCitation> {
        if (plan.keywords.isEmpty()) return emptyList()

        val lexicalCitations = retrieveLexicalCitations(plan.keywords)
        val semanticCitations = if (embeddingService.isReady() && vectorRetriever.isIndexReady()) {
            retrieveSemanticCitations(plan.semanticQuery)
        } else {
            emptyList()
        }

        val filteredSemantic = semanticCitations.filter { citation ->
            citation.score >= MIN_VECTOR_RELEVANCE_SCORE &&
                citation.sourceType.isNotBlank() &&
                !citation.sourceType.equals("internet", ignoreCase = true)
        }

        return selectGroundingCitations(
            candidates = lexicalCitations + filteredSemantic,
            plan = plan
        )
    }

    private fun hasAdequateLocalGrounding(
        citations: List<RetrievedCitation>,
        plan: IslamicQueryPlan
    ): Boolean {
        val relevant = citations.filter {
            !it.sourceType.equals("internet", ignoreCase = true) &&
                it.score >= MIN_LOCAL_GROUNDING_SCORE
        }
        if (relevant.isEmpty()) return false
        return when {
            plan.prefersQuran -> relevant.any { it.sourceType.equals("quran", ignoreCase = true) }
            plan.prefersHadith -> relevant.any { it.sourceType.equals("hadith", ignoreCase = true) }
            else -> true
        }
    }

    private fun selectGroundingCitations(
        candidates: List<RetrievedCitation>,
        plan: IslamicQueryPlan
    ): List<RetrievedCitation> {
        val ranked = candidates
            .filter(::isUsableCitation)
            .distinctBy(::citationKey)
            .sortedWith(
                compareByDescending<RetrievedCitation> { keywordMatchCount(it, plan.keywords) }
                    .thenByDescending { it.score }
            )
        val selected = linkedMapOf<String, RetrievedCitation>()

        fun addBestOf(kind: EvidenceKind) {
            ranked.firstOrNull {
                it.evidenceKind == kind && it.score >= MIN_VECTOR_RELEVANCE_SCORE
            }?.let { selected.putIfAbsent(citationKey(it), it) }
        }

        if (!plan.prefersQuran && !plan.prefersHadith) {
            addBestOf(EvidenceKind.QURAN)
            addBestOf(EvidenceKind.HADITH)
        } else {
            if (plan.prefersQuran) addBestOf(EvidenceKind.QURAN)
            if (plan.prefersHadith) addBestOf(EvidenceKind.HADITH)
        }

        val focusedRanked = when {
            plan.prefersQuran && !plan.prefersHadith -> ranked.filter {
                it.evidenceKind == EvidenceKind.QURAN || it.sourceType.equals("rag_document", true)
            }
            plan.prefersHadith && !plan.prefersQuran -> ranked.filter {
                it.evidenceKind == EvidenceKind.HADITH ||
                    it.sourceType.equals("rag_document", true) ||
                    (it.sourceType.equals("internet", true) && it.providerId == "sunnah-public")
            }
            else -> ranked
        }
        focusedRanked.forEach { citation ->
            if (selected.size >= MAX_CITATIONS) return@forEach
            selected.putIfAbsent(citationKey(citation), citation)
        }
        return selected.values.take(MAX_CITATIONS)
    }

    private fun isUsableCitation(citation: RetrievedCitation): Boolean {
        if (citation.textSnippet.isBlank() || citation.sourceId.isBlank()) return false
        return when (citation.sourceType.lowercase(Locale.ROOT)) {
            "quran", "hadith" -> citation.navigationTarget() != null
            "internet", "web" -> citation.navigationTarget() != null
            else -> true
        }
    }

    private suspend fun retrieveSemanticCitations(userQuery: String): List<RetrievedCitation> {
        return try {
            val queryVector = runtimeCoordinator.withInference {
                embeddingService.embed(userQuery)
            }
            vectorRetriever.retrieveTopK(
                query = userQuery,
                queryEmbedding = queryVector,
                k = MAX_SEMANTIC_CANDIDATES,
                minScore = MIN_VECTOR_SCORE
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(TAG, "Semantic retrieval unavailable; lexical retrieval remains active", exception)
            emptyList()
        }
    }

    private suspend fun retrieveLexicalCitations(keywords: List<String>): List<RetrievedCitation> {
        val surahs = database.quranDao().getAllSurahsOnce().associateBy { it.number }
        val candidateAyahs = keywords
            .take(MAX_KEYWORDS)
            .flatMap { keyword -> searchQuranByFts(keyword, limit = 8) }
            .distinctBy { "${it.surahId}:${it.ayahNumber}" }
            .take(MAX_LEXICAL_CANDIDATES)

        val quranCitations = buildList {
            candidateAyahs.forEach { ayah ->
                val surah = surahs[ayah.surahId] ?: return@forEach
                val ref = "QS. ${surah.nameLatin}:${ayah.ayahNumber}"
                val searchableText = listOf(
                    ayah.translationId,
                    ayah.translationEn,
                    ayah.transliteration,
                    ayah.textArabic
                ).joinToString(" ")
                val matches = keywordMatchCount(searchableText, keywords)
                if (matches == 0) return@forEach

                val tafsir = database.tafsirDao().getTafsirByAyah(
                    surahId = ayah.surahId,
                    ayahNumber = ayah.ayahNumber
                )
                val tafsirText = tafsir?.tafsirText?.takeIf(String::isNotBlank)?.let {
                    "\nTafsir ${tafsir.source}: \"${shorten(it, 180)}\""
                }.orEmpty()
                add(
                    RetrievedCitation(
                        sourceId = "quran-${ayah.surahId}-${ayah.ayahNumber}",
                        sourceType = "quran",
                        title = "$ref (${surah.nameLatin})",
                        reference = ref,
                        textSnippet = "${shorten(ayah.textArabic, 180)}\nArtinya: \"${shorten(ayah.translationId, 220)}\"${tafsirText}",
                        score = lexicalScore(matches, 0.80f),
                        collection = "quran",
                        identifier = "${ayah.surahId}:${ayah.ayahNumber}",
                        deepLinkTarget = "quran:${ayah.surahId}:${ayah.ayahNumber}",
                        surahNumber = ayah.surahId,
                        ayahNumber = ayah.ayahNumber
                    )
                )
            }
        }

        val candidateHadiths = searchHadithByKeywords(keywords.take(MAX_KEYWORDS), limit = 16)
            .distinctBy { "${it.collectionId}:${it.hadithNumber}" }

        val hadithCitations = candidateHadiths.mapNotNull { hadith ->
            if (!hadith.isVerifiedForGrounding()) return@mapNotNull null
            val searchableText = listOf(
                hadith.title,
                hadith.reference,
                hadith.textArabic,
                hadith.translationId,
                hadith.translationEn
            ).joinToString(" ")
            val matches = keywordMatchCount(searchableText, keywords)
            if (matches == 0) return@mapNotNull null
            val collectionName = hadith.collectionId.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            val reference = hadith.reference.ifBlank {
                "$collectionName No. ${hadith.hadithNumber}"
            }
            val translation = hadith.translationId.ifBlank { hadith.translationEn }
            RetrievedCitation(
                sourceId = "hadith-${hadith.id}",
                sourceType = "hadith",
                title = "$collectionName No. ${hadith.hadithNumber}",
                reference = reference,
                textSnippet = "${shorten(hadith.textArabic, 180)}\nArtinya: \"${shorten(translation, 220)}\"",
                score = lexicalScore(matches, 0.75f),
                collection = hadith.collectionId,
                identifier = hadith.hadithNumber.toString(),
                deepLinkTarget = "hadith:${hadith.collectionId}:${hadith.hadithNumber}",
                providerId = "local-hadith-bundle"
            )
        }

        val rankedQuran = quranCitations
            .sortedWith(
                compareByDescending<RetrievedCitation> { keywordMatchCount(it, keywords) }
                    .thenByDescending { it.score }
            )
            .take(MAX_LEXICAL_PER_SOURCE)
        val rankedHadith = hadithCitations
            .sortedWith(
                compareByDescending<RetrievedCitation> { keywordMatchCount(it, keywords) }
                    .thenByDescending { it.score }
            )
            .take(MAX_LEXICAL_PER_SOURCE)
        val knowledgeCitations = searchKnowledgeByKeywords(keywords.take(MAX_KEYWORDS), limit = 12)
            .map { chunk ->
                RetrievedCitation(
                    sourceId = "${chunk.sourceType}:${chunk.sourceId}#${chunk.id}",
                    sourceType = "rag_document",
                    title = chunk.title.ifBlank { "Dokumen lokal" },
                    reference = chunk.sourceId,
                    textSnippet = shorten(chunk.textContent, 420),
                    score = lexicalScore(keywordMatchCount(chunk.textContent, keywords), 0.70f),
                    collection = chunk.sourceType,
                    identifier = chunk.id.toString(),
                    providerId = "local-rag"
                )
            }
        return rankedQuran + rankedHadith + knowledgeCitations
    }

    private suspend fun searchQuranByFts(
        keyword: String,
        limit: Int
    ): List<com.quranplus.app.core.database.entity.AyahEntity> {
        val expression = buildQuranFtsMatchExpression(keyword) ?: return emptyList()
        return database.quranDao().searchAyahsFts(
            SimpleSQLiteQuery(
                """
                SELECT a.* FROM ayahs AS a
                JOIN ayahs_fts5 ON a.id = ayahs_fts5.rowid
                WHERE ayahs_fts5 MATCH ?
                ORDER BY a.surah_id ASC, a.ayah_number ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf<Any>(expression, limit)
            )
        )
    }

    private suspend fun searchHadithByKeywords(
        keywords: List<String>,
        limit: Int
    ): List<com.quranplus.app.core.database.entity.HadithEntity> {
        if (keywords.isEmpty()) return emptyList()
        if (!hasCompleteHadithBundle()) return emptyList()

        val expression = buildFtsMatchExpression(
            keywords = keywords,
            fields = listOf(
                "collection_id",
                "title",
                "text_arabic",
                "text_arabic_normalized",
                "translation_id",
                "translation_en",
                "reference"
            )
        ) ?: return emptyList()
        return database.hadithDao().searchFts(
            SimpleSQLiteQuery(
                """
                SELECT h.* FROM hadiths AS h
                JOIN hadiths_fts5 ON h.id = hadiths_fts5.rowid
                WHERE hadiths_fts5 MATCH ?
                    AND h.is_complete = 1
                    AND h.source_revision = ?
                    AND h.source_sha256 = ?
                    AND h.license_status = ?
                ORDER BY h.collection_id ASC, h.hadith_number ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf<Any>(
                    expression,
                    HadithBundleManifest.VERIFIED.revision,
                    HadithBundleManifest.VERIFIED.archiveSha256,
                    "licensed",
                    limit
                )
            )
        )
    }

    private suspend fun hasCompleteHadithBundle(): Boolean =
        HadithBundleManifest.VERIFIED.isVerifiedCorpus(
            database.hadithDao().getVerifiedBundleCollectionCounts(
                sourceRevision = HadithBundleManifest.VERIFIED.revision,
                sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256,
                licenseStatus = "licensed"
            ).associate { it.collectionId to it.recordCount }
        )

    private suspend fun searchKnowledgeByKeywords(
        keywords: List<String>,
        limit: Int
    ): List<com.quranplus.app.core.database.entity.KnowledgeChunkEntity> {
        val expression = buildFtsMatchExpression(
            keywords = keywords,
            fields = listOf("source_type", "source_id", "title", "text_content")
        ) ?: return emptyList()
        return database.knowledgeChunkDao().searchFts(
            SimpleSQLiteQuery(
                """
                SELECT k.* FROM knowledge_chunks AS k
                JOIN knowledge_chunks_fts5 ON k.id = knowledge_chunks_fts5.rowid
                WHERE knowledge_chunks_fts5 MATCH ?
                ORDER BY k.id ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf<Any>(expression, limit)
            )
        )
    }

    private fun buildFtsMatchExpression(keywords: List<String>, fields: List<String>): String? {
        val tokens = keywords
            .flatMap { QUERY_TOKEN_PATTERN.findAll(normalizeArabicSearchText(it)).map { match -> match.value }.toList() }
            .distinct()
        if (tokens.isEmpty()) return null
        val fieldList = fields.joinToString(" ")
        return tokens.joinToString(" OR ") { token ->
            "{$fieldList}: \"${escapeFtsPhrase(token)}\"*"
        }
    }

    private fun buildQuranFtsMatchExpression(keyword: String): String? {
        val tokens = QUERY_TOKEN_PATTERN.findAll(normalizeArabicSearchText(keyword))
            .map { it.value }
            .filter(String::isNotBlank)
            .toList()
        if (tokens.isEmpty()) return null

        val expression = tokens.joinToString(" AND ") { token ->
            "\"${escapeFtsPhrase(token)}\"*"
        }
        return "{translation_id translation_en transliteration text_arabic text_arabic_normalized} : ($expression)"
    }

    private fun escapeFtsPhrase(value: String): String = value.replace("\"", "\"\"")

    private fun generateFallbackAnswer(
        citations: List<RetrievedCitation>,
        notice: String? = null
    ): String {
        val hasInternetCitation = citations.any { it.isInternetSourced() }
        val references = citations.mapIndexed { index, citation ->
            "[[cite:C${index + 1}]] ${citation.title}\n${citation.textSnippet}"
        }.joinToString("\n\n")
        return listOfNotNull(
            notice,
            if (hasInternetCitation) {
                "Rujukan yang ditemukan (lokal dan internet tambahan):"
            } else {
                "Rujukan lokal yang ditemukan:"
            },
            references,
            "Ketuk chip rujukan untuk membaca sumber aslinya."
        ).joinToString("\n\n")
    }

    private fun keywordMatchCount(
        citation: RetrievedCitation,
        keywords: List<String>
    ): Int = keywordMatchCount(
        listOf(citation.title, citation.reference, citation.textSnippet).joinToString(" "),
        keywords
    )

    private fun keywordMatchCount(text: String, keywords: List<String>): Int {
        val searchableText = normalizeArabicSearchText(text)
        return keywords.count { keyword ->
            keywordVariants(keyword).any { variant ->
                searchableText.contains(normalizeArabicSearchText(variant))
            }
        }
    }

    private fun keywordVariants(keyword: String): List<String> = when (keyword) {
        "shalat", "salat" -> listOf("shalat", "salat")
        "hadis", "hadist", "hadits" -> listOf("hadis", "hadist", "hadits")
        "quran", "alquran", "al-quran" -> listOf("quran", "alquran", "al-quran")
        else -> listOf(keyword)
    }

    private fun com.quranplus.app.core.database.entity.HadithEntity.isVerifiedForGrounding(): Boolean =
        HadithBundleManifest.VERIFIED.isVerifiedRecord(this)

    private fun lexicalScore(matches: Int, base: Float): Float =
        (base + matches * 0.05f).coerceAtMost(0.99f)

    private fun shorten(value: String, maxLength: Int): String =
        if (value.length > maxLength) value.take(maxLength) + "..." else value

    private fun citationKey(citation: RetrievedCitation): String = when {
        citation.sourceType.equals("quran", ignoreCase = true) &&
            citation.surahNumber != null && citation.ayahNumber != null ->
            "quran:${citation.surahNumber}:${citation.ayahNumber}"

        citation.sourceType.equals("hadith", ignoreCase = true) ->
            "hadith:${citation.collection}:${citation.identifier.substringBefore('#')}"

        else -> "${citation.sourceType}:${citation.reference}"
    }

    private fun normalizeArabicSearchText(value: String): String = buildString(value.length) {
        value.lowercase(Locale.ROOT).forEach { character ->
            when (character.code) {
                1600, in 1611..1648, in 1750..1773 -> Unit
                1649 -> append('\u0627')
                else -> append(character)
            }
        }
    }

    private companion object {
        const val TAG = "ChatRepository"
        const val MAX_CITATIONS = 5
        const val MAX_KEYWORDS = 12
        const val MAX_LEXICAL_CANDIDATES = 24
        const val MAX_LEXICAL_PER_SOURCE = 12
        const val MAX_SEMANTIC_CANDIDATES = 15
        const val MIN_VECTOR_SCORE = 0.55f
        const val MIN_VECTOR_RELEVANCE_SCORE = 0.60f
        const val MIN_LOCAL_GROUNDING_SCORE = 0.68f
        const val RETRIEVAL_TIMEOUT_MS = 8_000L
        const val GENERATION_TIMEOUT_MS = 30_000L
        val QUERY_TOKEN_PATTERN = Regex("[\\p{L}\\p{N}]+")
    }
}
