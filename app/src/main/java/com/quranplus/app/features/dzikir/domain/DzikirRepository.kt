package com.quranplus.app.features.dzikir.domain

import kotlinx.coroutines.flow.Flow

interface DzikirRepository {
    fun getCategories(): Flow<List<DzikirCategory>>
    fun getItemsByCategory(category: DzikirCategory): Flow<List<DzikirItem>>
    fun getAllItems(): Flow<List<DzikirItem>>
    fun searchDzikir(query: String, category: DzikirCategory? = null): Flow<List<DzikirItem>>
    suspend fun getItemById(id: String): DzikirItem?
}
