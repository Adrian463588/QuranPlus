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
import com.quranplus.app.features.hadith.data.HadithNumbering
import com.quranplus.app.core.database.entity.HadithEntity
import com.quranplus.app.features.rag.domain.CitationTarget
import com.quranplus.app.features.rag.domain.HadithReferenceRequest
import com.quranplus.app.features.rag.domain.navigationTarget
import com.quranplus.app.features.rag.domain.GenerationStatus
import com.quranplus.app.features.rag.domain.CitationMarkerValidator
import com.quranplus.app.features.rag.domain.EvidenceKind
import com.quranplus.app.features.rag.domain.isInternetSourced
import com.quranplus.app.features.rag.domain.isPrimaryEvidence
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
    private val llmRunner: LlmRunner,
    private val database: QuranDatabase,
    private val runtimeCoordinator: RagRuntimeCoordinator,
    private val internetResearcher: InternetResearcher,
    private val preferencesManager: PreferencesManager,
    private val hadithNumbering: HadithNumbering = HadithNumbering()
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
        val citations = resolveGroundingCitations(userQuery)
        if (citations.isEmpty()) {
            return RagGenerationResult(
                tokenStream = flow {
                    emit(GenerationEvent.Replace(
                        "Belum ditemukan rujukan Quran atau Hadist yang memadai untuk menjawab pertanyaan ini."
                    ))
                },
                citations = emptyList(),
                generationStatus = MutableStateFlow(GenerationStatus.UNAVAILABLE).asStateFlow()
            )
        }

        // A requested reference is answered verbatim; the model cannot rename
        // its number or replace its text with a remembered narration.
        if (IslamicQueryAnalyzer.analyze(userQuery).hadithReference != null) {
            return RagGenerationResult(
                tokenStream = flow { emit(GenerationEvent.Replace(generateFallbackAnswer(citations))) },
                citations = citations,
                generationStatus = MutableStateFlow(GenerationStatus.COMPLETE).asStateFlow()
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
            .filter { it.role == MessageRole.USER }

        // 4. Generate Stream with Fallback & Quality Guardrails
        val generationStatus = MutableStateFlow(GenerationStatus.STREAMING)
        val stream: Flow<GenerationEvent> = flow {
            if (llmRunner.isAvailable()) {
                var receivedTokens = false
                val buffer = StringBuilder()
                var fallbackEmitted = false
                try {
                    var timedOut = false
                    runtimeCoordinator.withInference {
                        // Older conversation caches may contain web/document answers.
                        llmRunner.clearConversation(conversationId)
                        val result = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                            llmRunner.generate(conversationId, augmentedPrompt, history).collect { token ->
                                receivedTokens = true
                                buffer.append(token)
                            }
                        }
                        // withTimeoutOrNull returns null when 5-minute budget is exceeded
                        if (result == null) timedOut = true
                    }

                    if (receivedTokens && buffer.isNotBlank()) {
                        // Validate before display processing removes internal markers.
                        val validation = CitationMarkerValidator.validate(buffer.toString(), citations)
                        val referencesValid = !HadithReferenceRequest.hasInvalidReference(buffer.toString()) &&
                            HadithReferenceRequest.findAll(buffer.toString())
                            .all { request -> citations.any(request::matches) }
                        val answerValid = validation.isValid && referencesValid
                        emit(GenerationEvent.Replace(
                            if (answerValid) {
                                com.quranplus.app.features.chatbot.domain.AiResponsePostProcessor.process(validation.content)
                            } else generateFallbackAnswer(citations)
                        ))
                        generationStatus.value = when {
                            timedOut -> GenerationStatus.TIMEOUT
                            answerValid -> GenerationStatus.COMPLETE
                            else -> GenerationStatus.FALLBACK
                        }
                        if (!answerValid) llmRunner.clearConversation(conversationId)
                    } else if (timedOut && !fallbackEmitted) {
                        // Timed out with empty buffer — use verified grounding fallback
                        generationStatus.value = GenerationStatus.TIMEOUT
                        val fallback = generateFallbackAnswer(citations)
                        emit(GenerationEvent.Replace(fallback))
                        fallbackEmitted = true
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    generationStatus.value = GenerationStatus.UNAVAILABLE
                    Log.w(TAG, "LiteRT-LM generation failed; using grounded fallback", e)
                    generationStatus.value = GenerationStatus.FALLBACK
                    emit(GenerationEvent.Replace(generateFallbackAnswer(citations)))
                    fallbackEmitted = true
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
        if (HadithReferenceRequest.hasInvalidReference(userQuery)) return emptyList()
        val plan = IslamicQueryAnalyzer.analyze(userQuery)
        val localCitations = try {
            withTimeoutOrNull(RETRIEVAL_TIMEOUT_MS) { retrieveGroundingCitations(plan) }.orEmpty()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            emptyList()
        }
        if (hasAdequateLocalGrounding(localCitations, plan)) return localCitations
        // An archive ordinal must never be sent as a different edition's number.
        if (plan.hadithReference?.archive == true) return emptyList()

        if (!preferencesManager.onlineResearchEnabled.first()) return emptyList()

        val onlineCitations = try {
            withTimeoutOrNull(ONLINE_RETRIEVAL_TIMEOUT_MS) {
                internetResearcher.search(userQuery)
            }.orEmpty()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            emptyList()
        }
        val selected = selectGroundingCitations(
            candidates = localCitations + onlineCitations,
            plan = plan
        )
        return selected.takeIf { hasAdequateLocalGrounding(it, plan) }.orEmpty()
    }

    private suspend fun retrieveGroundingCitations(plan: IslamicQueryPlan): List<RetrievedCitation> {
        plan.hadithReference?.let { request ->
            if (request.suffix.isNotEmpty()) return emptyList()
            if (request.archive) {
                val row = database.hadithDao().getHadithByNumber(request.collection, request.number)
                return listOfNotNull(row?.takeIf {
                    it.sourceRevision == HadithBundleManifest.VERIFIED.revision
                }?.let { hadithCitation(it, 1f) })
            }
            return hadithNumbering.archiveNumbers(request.collection, request.number).mapNotNull { number ->
                val row = database.hadithDao().getHadithByNumber(request.collection, number) ?: return@mapNotNull null
                if (hadithNumbering.referenceNumber(row) != request.number) return@mapNotNull null
                hadithCitation(row, 1f)
            }
        }
        val canonicalCitations = plan.canonicalAyahTargets.mapNotNull { (surahId, ayahNumber) ->
            val ayah = database.quranDao().getAyah(surahId, ayahNumber) ?: return@mapNotNull null
            val surah = database.quranDao().getSurahByNumber(surahId)
            val surahName = surah?.nameLatin ?: "Surah $surahId"
            val ref = "QS. $surahName:$ayahNumber"
            RetrievedCitation(
                sourceId = "quran-$surahId-$ayahNumber",
                sourceType = "quran",
                title = "$ref ($surahName)",
                reference = ref,
                textSnippet = "${ayah.textArabic}\nArtinya: \"${ayah.translationId}\"",
                score = 1.0f,
                collection = "quran",
                identifier = "$surahId:$ayahNumber",
                deepLinkTarget = "quran:$surahId:$ayahNumber",
                surahNumber = surahId,
                ayahNumber = ayahNumber,
                evidenceKind = EvidenceKind.QURAN
            )
        }

        if (plan.keywords.isEmpty() && canonicalCitations.isEmpty()) return emptyList()

        val lexicalCitations = if (plan.keywords.isNotEmpty() || plan.coreTopicTerms.isNotEmpty()) {
            retrieveLexicalCitations(plan)
        } else {
            emptyList()
        }
        val semanticCitations = if (embeddingService.isReady() && vectorRetriever.isIndexReady()) {
            retrieveSemanticCitations(plan.semanticQuery)
        } else {
            emptyList()
        }

        val filteredSemantic = semanticCitations.filter { citation ->
            citation.score >= MIN_VECTOR_RELEVANCE_SCORE &&
                citation.sourceType.isNotBlank() &&
                !citation.sourceType.equals("internet", ignoreCase = true)
        }.mapNotNull { citation ->
            if (citation.evidenceKind != EvidenceKind.HADITH) return@mapNotNull citation
            // Rehydrate old SAF/vector references from the current content row.
            val target = citation.navigationTarget() as? CitationTarget.Hadith ?: return@mapNotNull null
            val row = database.hadithDao().getHadithByNumber(target.collectionId, target.hadithNumber)
                ?: return@mapNotNull null
            hadithCitation(row, citation.score)
        }

        return selectGroundingCitations(
            candidates = canonicalCitations + lexicalCitations + filteredSemantic,
            plan = plan
        )
    }

    private fun hasAdequateLocalGrounding(
        citations: List<RetrievedCitation>,
        plan: IslamicQueryPlan
    ): Boolean {
        val relevant = citations.filter {
            it.isPrimaryEvidence() && it.score >= MIN_LOCAL_GROUNDING_SCORE
        }
        if (relevant.isEmpty()) return false
        plan.hadithReference?.let { request -> return relevant.any(request::matches) }

        // Substantive topic relevance validation:
        // If coreTopicTerms are present, at least one local citation MUST match a core topic term.
        // Generic intent terms ("membaca", "keutamaan", "hukum") alone do NOT constitute adequate grounding.
        if (plan.coreTopicTerms.isNotEmpty()) {
            val hasSubstantiveMatch = relevant.any { citation ->
                if (citation.surahNumber to citation.ayahNumber in plan.canonicalAyahTargets) return@any true
                val text = listOf(citation.title, citation.reference, citation.textSnippet).joinToString(" ")
                plan.coreTopicTerms.any { topic ->
                    keywordVariants(topic).any { variant ->
                        val normalized = normalizeArabicSearchText(variant).trim()
                        normalized.isNotBlank() && normalizeArabicSearchText(text).contains(normalized)
                    }
                }
            }
            if (!hasSubstantiveMatch) return false
        }

        return when {
            plan.prefersHadith && plan.prefersQuran ->
                relevant.any { it.evidenceKind == EvidenceKind.HADITH }
            plan.prefersQuran -> relevant.any { it.sourceType.equals("quran", ignoreCase = true) }
            plan.prefersHadith -> relevant.any { it.evidenceKind == EvidenceKind.HADITH }
            else -> true
        }
    }

    private fun selectGroundingCitations(
        candidates: List<RetrievedCitation>,
        plan: IslamicQueryPlan
    ): List<RetrievedCitation> {
        plan.hadithReference?.let { request ->
            return candidates.filter(::isUsableCitation).filter(request::matches)
                .distinctBy(::citationKey).take(MAX_CITATIONS)
        }
        val selected = linkedMapOf<String, RetrievedCitation>()

        // 1. Always prioritize canonical citations directly mapped for this query
        candidates
            .filter { it.score >= 1.0f && isUsableCitation(it) }
            .distinctBy(::citationKey)
            .forEach { selected.putIfAbsent(citationKey(it), it) }

        val ranked = candidates
            .filter(::isUsableCitation)
            .distinctBy(::citationKey)
            .sortedWith(
                compareByDescending<RetrievedCitation> { keywordMatchScore(it, plan) }
                    .thenByDescending { it.score }
            )

        fun addBestOf(kind: EvidenceKind) {
            ranked.firstOrNull {
                it.evidenceKind == kind && it.score >= MIN_VECTOR_RELEVANCE_SCORE &&
                    (plan.coreTopicTerms.isEmpty() || keywordMatchScore(it, plan) > 0 || it.isInternetSourced())
            }?.let { selected.putIfAbsent(citationKey(it), it) }
        }

        if (plan.prefersQuran) addBestOf(EvidenceKind.QURAN)
        if (plan.prefersHadith) addBestOf(EvidenceKind.HADITH)

        ranked.forEach { citation ->
            if (selected.size >= MAX_CITATIONS) return@forEach
            // Drop citations that scored 0 for topic match if they are local hadith
            if (citation.evidenceKind == EvidenceKind.HADITH && plan.coreTopicTerms.isNotEmpty() && keywordMatchScore(citation, plan) == 0) {
                return@forEach
            }
            selected.putIfAbsent(citationKey(citation), citation)
        }
        return selected.values.take(MAX_CITATIONS)
    }

    private fun isUsableCitation(citation: RetrievedCitation): Boolean {
        return citation.isPrimaryEvidence() && citation.score >= MIN_VECTOR_RELEVANCE_SCORE
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

    private suspend fun retrieveLexicalCitations(plan: IslamicQueryPlan): List<RetrievedCitation> {
        val keywords = plan.keywords
        val surahs = database.quranDao().getAllSurahsOnce().associateBy { it.number }
        val searchTerms = (plan.coreTopicTerms + keywords).distinct().take(MAX_KEYWORDS)
        val candidateAyahs = searchTerms
            .flatMap { keyword -> searchQuranByFts(keyword, limit = 20) }
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
                val matches = keywordMatchScore(searchableText, plan)
                if (matches == 0) return@forEach

                add(
                    RetrievedCitation(
                        sourceId = "quran-${ayah.surahId}-${ayah.ayahNumber}",
                        sourceType = "quran",
                        title = "$ref (${surah.nameLatin})",
                        reference = ref,
                        textSnippet = "${ayah.textArabic}\nArtinya: \"${ayah.translationId}\"",
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

        val candidateHadiths = searchHadithByKeywords(plan, limit = 24)
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
            val matches = keywordMatchScore(searchableText, plan)
            if (matches == 0) return@mapNotNull null
            hadithCitation(hadith, lexicalScore(matches, 0.75f))
        }

        val rankedQuran = quranCitations
            .sortedWith(
                compareByDescending<RetrievedCitation> { keywordMatchScore(it, plan) }
                    .thenByDescending { it.score }
            )
            .take(MAX_LEXICAL_PER_SOURCE)
        val rankedHadith = hadithCitations
            .sortedWith(
                compareByDescending<RetrievedCitation> { keywordMatchScore(it, plan) }
                    .thenByDescending { it.score }
            )
            .take(MAX_LEXICAL_PER_SOURCE)
        return rankedQuran + rankedHadith
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
                ORDER BY ayahs_fts5.rank ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf<Any>(expression, limit)
            )
        )
    }

    private suspend fun searchHadithByKeywords(
        plan: IslamicQueryPlan,
        limit: Int
    ): List<com.quranplus.app.core.database.entity.HadithEntity> {
        val keywords = plan.keywords
        if (keywords.isEmpty() && plan.coreTopicTerms.isEmpty()) return emptyList()

        val fields = listOf(
            "collection_id",
            "title",
            "text_arabic",
            "text_arabic_normalized",
            "translation_id",
            "translation_en",
            "reference"
        )

        val expression = if (plan.coreTopicTerms.isNotEmpty()) {
            buildFtsMatchExpression(plan.coreTopicTerms, fields)
        } else {
            buildFtsMatchExpression(keywords, fields)
        } ?: return emptyList()

        return if (hasCompleteHadithBundle()) {
            database.hadithDao().searchFts(
                SimpleSQLiteQuery(
                    """
                    SELECT h.* FROM hadiths AS h
                    JOIN hadiths_fts5 ON h.id = hadiths_fts5.rowid
                    WHERE hadiths_fts5 MATCH ?
                        AND h.is_complete = 1
                        AND h.source_revision = ?
                        AND h.source_sha256 = ?
                        AND h.license_status = ?
                    ORDER BY hadiths_fts5.rank ASC
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
        } else {
            database.hadithDao().searchFts(
                SimpleSQLiteQuery(
                    """
                    SELECT h.* FROM hadiths AS h
                    JOIN hadiths_fts5 ON h.id = hadiths_fts5.rowid
                    WHERE hadiths_fts5 MATCH ?
                    ORDER BY hadiths_fts5.rank ASC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf<Any>(expression, limit)
                )
            )
        }
    }

    private suspend fun hasCompleteHadithBundle(): Boolean =
        HadithBundleManifest.VERIFIED.isVerifiedCorpus(
            database.hadithDao().getVerifiedBundleCollectionCounts(
                sourceRevision = HadithBundleManifest.VERIFIED.revision,
                sourceSha256 = HadithBundleManifest.VERIFIED.archiveSha256,
                licenseStatus = "licensed"
            ).associate { it.collectionId to it.recordCount }
        )

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
                "Rujukan Quran/Hadist yang ditemukan:"
            } else {
                "Rujukan lokal yang ditemukan:"
            },
            references,
            "Ketuk chip rujukan untuk membaca sumber aslinya."
        ).joinToString("\n\n")
    }

    private val GENERIC_QUERY_WORDS = setOf(
        "keutamaan", "fadhilah", "fadilah", "keistimewaan", "manfaat", "pahala",
        "baca", "membaca", "arti", "artinya", "makna", "penjelasan", "uraian",
        "hukum", "syarat", "rukun", "amal", "amalan", "doa", "dzikir", "zikir",
        "wirid", "hizib", "tata", "cara", "perintah", "larangan", "dalil", "ayat", "hadis", "hadits", "hadist"
    )

    private fun keywordMatchScore(
        citation: RetrievedCitation,
        plan: IslamicQueryPlan
    ): Int = keywordMatchScore(
        listOf(citation.title, citation.reference, citation.textSnippet).joinToString(" "),
        plan
    )

    private fun keywordMatchScore(
        citation: RetrievedCitation,
        keywords: List<String>
    ): Int = keywordMatchScore(
        listOf(citation.title, citation.reference, citation.textSnippet).joinToString(" "),
        keywords
    )

    private fun keywordMatchScore(text: String, plan: IslamicQueryPlan): Int {
        val searchableText = normalizeArabicSearchText(text)

        // Strict topic relevance filter: if coreTopicTerms are specified,
        // citation MUST match at least one core topic term.
        // Generic intent words ("membaca", "keutamaan", "hukum") alone are NOT enough.
        if (plan.coreTopicTerms.isNotEmpty()) {
            val hasTopicMatch = plan.coreTopicTerms.any { topic ->
                val normalizedTopic = normalizeArabicSearchText(topic).trim().lowercase(Locale.ROOT)
                if (normalizedTopic.isBlank()) false
                else keywordVariants(topic).any { variant ->
                    searchableText.contains(normalizeArabicSearchText(variant))
                }
            }
            if (!hasTopicMatch) {
                return 0
            }
        }

        var totalScore = 0
        for (keyword in plan.keywords) {
            val normalizedKw = normalizeArabicSearchText(keyword).trim().lowercase(Locale.ROOT)
            if (normalizedKw.isBlank()) continue
            val hasMatch = keywordVariants(keyword).any { variant ->
                searchableText.contains(normalizeArabicSearchText(variant))
            }
            if (hasMatch) {
                val isCoreTopic = plan.coreTopicTerms.any { it.equals(keyword, ignoreCase = true) }
                totalScore += when {
                    isCoreTopic -> 10
                    normalizedKw in GENERIC_QUERY_WORDS -> 1
                    else -> 4
                }
            }
        }
        return totalScore
    }

    private fun keywordMatchScore(text: String, keywords: List<String>): Int {
        val searchableText = normalizeArabicSearchText(text)
        var totalScore = 0
        for (keyword in keywords) {
            val normalizedKw = normalizeArabicSearchText(keyword).trim().lowercase(Locale.ROOT)
            if (normalizedKw.isBlank()) continue
            val hasMatch = keywordVariants(keyword).any { variant ->
                searchableText.contains(normalizeArabicSearchText(variant))
            }
            if (hasMatch) {
                totalScore += if (normalizedKw in GENERIC_QUERY_WORDS) 1 else 5
            }
        }
        return totalScore
    }

    private fun keywordVariants(keyword: String): List<String> = (when (keyword) {
        "shalat", "salat" -> listOf("shalat", "salat")
        "hadis", "hadist", "hadits" -> listOf("hadis", "hadist", "hadits")
        "quran", "alquran", "al-quran" -> listOf("quran", "alquran", "al-quran")
        else -> listOf(keyword)
    } + IslamicQueryAnalyzer.hadithSearchTerm(keyword)).distinct()

    private fun com.quranplus.app.core.database.entity.HadithEntity.isVerifiedForGrounding(): Boolean =
        licenseStatus == "reference" ||
            (HadithBundleManifest.VERIFIED.isVerifiedRecord(this) && hadithNumbering.isContentValid(this))

    private fun hadithCitation(row: HadithEntity, score: Float): RetrievedCitation? {
        if (!row.isVerifiedForGrounding()) return null
        val reference = hadithNumbering.reference(row)
        return RetrievedCitation(
            sourceId = "hadith-${row.id}", sourceType = "hadith", title = reference,
            reference = reference,
            textSnippet = "${row.textArabic}\nArtinya: \"${hadithNumbering.indonesianTranslation(row).ifBlank { row.translationEn }}\"",
            score = score, collection = row.collectionId, identifier = row.hadithNumber.toString(),
            deepLinkTarget = "hadith:${row.collectionId}:${row.hadithNumber}",
            canonicalUrl = hadithNumbering.canonicalUrl(row), providerId = "local-hadith-bundle"
        )
    }

    private fun lexicalScore(matches: Int, base: Float): Float =
        (base + matches * 0.05f).coerceAtMost(0.99f)

    private fun citationKey(citation: RetrievedCitation): String = when {
        citation.sourceType.equals("quran", ignoreCase = true) &&
            citation.surahNumber != null && citation.ayahNumber != null ->
            "quran:${citation.surahNumber}:${citation.ayahNumber}"

        citation.sourceType.equals("hadith", ignoreCase = true) ->
            "hadith:${citation.sourceId.substringBefore('#')}"

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
        const val MAX_LEXICAL_CANDIDATES = 48
        const val MAX_LEXICAL_PER_SOURCE = 20
        const val MAX_SEMANTIC_CANDIDATES = 15
        const val MIN_VECTOR_SCORE = 0.55f
        const val MIN_VECTOR_RELEVANCE_SCORE = 0.60f
        const val MIN_LOCAL_GROUNDING_SCORE = 0.68f
        const val RETRIEVAL_TIMEOUT_MS = 8_000L
        const val ONLINE_RETRIEVAL_TIMEOUT_MS = 26_000L
        const val GENERATION_TIMEOUT_MS = 300_000L // 5 minutes max
        val QUERY_TOKEN_PATTERN = Regex("[\\p{L}\\p{N}]+")
    }
}
