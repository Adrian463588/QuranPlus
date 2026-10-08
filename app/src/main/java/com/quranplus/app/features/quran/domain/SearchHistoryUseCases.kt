package com.quranplus.app.features.quran.domain

import kotlinx.coroutines.flow.Flow

class GetSearchHistoryUseCase(
    private val quranRepository: QuranRepository
) {
    operator fun invoke(): Flow<List<String>> = quranRepository.getSearchHistory()
}

class SaveSearchQueryUseCase(
    private val quranRepository: QuranRepository
) {
    suspend operator fun invoke(query: String) {
        quranRepository.saveSearchQuery(query)
    }
}

class DeleteSearchQueryUseCase(
    private val quranRepository: QuranRepository
) {
    suspend operator fun invoke(query: String) {
        quranRepository.deleteSearchQuery(query)
    }
}

class ClearSearchHistoryUseCase(
    private val quranRepository: QuranRepository
) {
    suspend operator fun invoke() {
        quranRepository.clearSearchHistory()
    }
}
