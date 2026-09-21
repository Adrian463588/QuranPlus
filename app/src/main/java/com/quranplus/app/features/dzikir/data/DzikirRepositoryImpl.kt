package com.quranplus.app.features.dzikir.data

import com.quranplus.app.features.dzikir.domain.DzikirCategory
import com.quranplus.app.features.dzikir.domain.DzikirItem
import com.quranplus.app.features.dzikir.domain.DzikirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DzikirRepositoryImpl : DzikirRepository {

    override fun getCategories(): Flow<List<DzikirCategory>> {
        val categories = DzikirCategory.entries.sortedBy { it.orderIndex }
        return flowOf(categories)
    }

    override fun getItemsByCategory(category: DzikirCategory): Flow<List<DzikirItem>> {
        val items = DzikirDataCatalog.ALL_ITEMS
            .filter { it.category == category }
            .sortedBy { it.orderNumber }
        return flowOf(items)
    }

    override fun getAllItems(): Flow<List<DzikirItem>> {
        return flowOf(DzikirDataCatalog.ALL_ITEMS)
    }

    override fun searchDzikir(query: String, category: DzikirCategory?): Flow<List<DzikirItem>> {
        val q = query.trim().lowercase()
        val baseList = if (category != null) {
            DzikirDataCatalog.ALL_ITEMS.filter { it.category == category }
        } else {
            DzikirDataCatalog.ALL_ITEMS
        }

        if (q.isBlank()) {
            return flowOf(baseList.sortedBy { it.orderNumber })
        }

        val filtered = baseList.filter { item ->
            item.title.lowercase().contains(q) ||
                    item.transliteration.lowercase().contains(q) ||
                    item.translationId.lowercase().contains(q) ||
                    item.translationEn.lowercase().contains(q) ||
                    item.arabicText.contains(q) ||
                    item.sourceNote.lowercase().contains(q) ||
                    (item.fadhilah?.lowercase()?.contains(q) == true)
        }
        return flowOf(filtered)
    }

    override suspend fun getItemById(id: String): DzikirItem? {
        return DzikirDataCatalog.ALL_ITEMS.firstOrNull { it.id == id }
    }
}
