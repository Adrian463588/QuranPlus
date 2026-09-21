package com.quranplus.app.features.dzikir.domain

import kotlinx.coroutines.flow.Flow

class GetDzikirCategoriesUseCase(
    private val repository: DzikirRepository
) {
    operator fun invoke(): Flow<List<DzikirCategory>> =
        repository.getCategories()
}

class GetDzikirItemsUseCase(
    private val repository: DzikirRepository
) {
    operator fun invoke(category: DzikirCategory): Flow<List<DzikirItem>> =
        repository.getItemsByCategory(category)
}

class SearchDzikirUseCase(
    private val repository: DzikirRepository
) {
    operator fun invoke(query: String, category: DzikirCategory? = null): Flow<List<DzikirItem>> =
        repository.searchDzikir(query, category)
}
