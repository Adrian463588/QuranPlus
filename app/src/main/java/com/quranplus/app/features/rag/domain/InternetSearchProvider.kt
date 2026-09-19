package com.quranplus.app.features.rag.domain

/** A bounded, allowlisted provider used only after local retrieval is insufficient. */
interface InternetSearchProvider {
    val providerId: String
    suspend fun search(query: String, plan: IslamicQueryPlan, limit: Int): List<RetrievedCitation>
}
