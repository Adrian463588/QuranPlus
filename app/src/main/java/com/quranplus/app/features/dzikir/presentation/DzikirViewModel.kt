package com.quranplus.app.features.dzikir.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranplus.app.features.dzikir.domain.DzikirCategory
import com.quranplus.app.features.dzikir.domain.DzikirItem
import com.quranplus.app.features.dzikir.domain.DzikirUiSettings
import com.quranplus.app.features.dzikir.domain.GetDzikirCategoriesUseCase
import com.quranplus.app.features.dzikir.domain.GetDzikirItemsUseCase
import com.quranplus.app.features.dzikir.domain.SearchDzikirUseCase
import com.quranplus.app.features.dzikir.domain.TranslationLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DzikirUiState(
    val categories: List<DzikirCategory> = emptyList(),
    val selectedCategory: DzikirCategory = DzikirCategory.PAGI,
    val items: List<DzikirItem> = emptyList(),
    val searchQuery: String = "",
    val counterMap: Map<String, Int> = emptyMap(),
    val settings: DzikirUiSettings = DzikirUiSettings(),
    val isLoading: Boolean = false
)

class DzikirViewModel(
    private val getCategoriesUseCase: GetDzikirCategoriesUseCase,
    private val getItemsUseCase: GetDzikirItemsUseCase,
    private val searchDzikirUseCase: SearchDzikirUseCase
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(DzikirCategory.PAGI)
    private val _searchQuery = MutableStateFlow("")
    private val _counterMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _settings = MutableStateFlow(DzikirUiSettings())
    private val _categories = MutableStateFlow<List<DzikirCategory>>(emptyList())
    private val _rawItems = MutableStateFlow<List<DzikirItem>>(emptyList())

    private val _dataFlow = combine(_categories, _selectedCategory, _rawItems) { categories, selectedCat, rawItems ->
        Triple(categories, selectedCat, rawItems)
    }

    val uiState: StateFlow<DzikirUiState> = combine(
        _dataFlow,
        _searchQuery,
        _counterMap,
        _settings
    ) { (categories, selectedCat, rawItems), query, counters, settings ->
        val filteredItems = if (query.isBlank()) {
            rawItems
        } else {
            val q = query.trim().lowercase()
            rawItems.filter { item ->
                item.title.lowercase().contains(q) ||
                        item.transliteration.lowercase().contains(q) ||
                        item.translationId.lowercase().contains(q) ||
                        item.translationEn.lowercase().contains(q) ||
                        item.arabicText.contains(q) ||
                        item.sourceNote.lowercase().contains(q) ||
                        (item.fadhilah?.lowercase()?.contains(q) == true)
            }
        }
        DzikirUiState(
            categories = categories,
            selectedCategory = selectedCat,
            items = filteredItems,
            searchQuery = query,
            counterMap = counters,
            settings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DzikirUiState(isLoading = true)
    )

    init {
        loadCategories()
        loadItems(DzikirCategory.PAGI)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { list ->
                _categories.value = list
            }
        }
    }

    fun selectCategory(category: DzikirCategory) {
        if (_selectedCategory.value == category && _searchQuery.value.isBlank()) return
        _selectedCategory.value = category
        _searchQuery.value = ""
        loadItems(category)
    }

    private fun loadItems(category: DzikirCategory) {
        viewModelScope.launch {
            getItemsUseCase(category).collect { items ->
                _rawItems.value = items
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                searchDzikirUseCase(query, null).collect { items ->
                    _rawItems.value = items
                }
            }
        } else {
            loadItems(_selectedCategory.value)
        }
    }

    fun incrementCounter(itemId: String, maxCount: Int) {
        _counterMap.update { current ->
            val currentVal = current[itemId] ?: 0
            val nextVal = if (currentVal >= maxCount) currentVal else currentVal + 1
            current + (itemId to nextVal)
        }
    }

    fun resetCounter(itemId: String) {
        _counterMap.update { current ->
            current + (itemId to 0)
        }
    }

    fun resetAllCounters() {
        _counterMap.value = emptyMap()
    }

    fun updateFontSize(size: Float) {
        _settings.update { it.copy(arabicFontSize = size.coerceIn(18f, 44f)) }
    }

    fun toggleTajwid() {
        _settings.update { it.copy(showTajwid = !it.showTajwid) }
    }

    fun toggleTransliteration() {
        _settings.update { it.copy(showTransliteration = !it.showTransliteration) }
    }

    fun toggleTranslation() {
        _settings.update { it.copy(showTranslation = !it.showTranslation) }
    }

    fun setTranslationLanguage(language: TranslationLanguage) {
        _settings.update { it.copy(translationLanguage = language) }
    }
}
