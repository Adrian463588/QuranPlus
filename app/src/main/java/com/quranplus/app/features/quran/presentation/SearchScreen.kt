package com.quranplus.app.features.quran.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.QuranSearchField
import com.quranplus.app.features.quran.domain.QuranSearchFilter
import com.quranplus.app.features.quran.domain.QuranSearchMode

import androidx.compose.foundation.layout.imePadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: QuranViewModel,
    onAyahClick: (Int, Int) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var filterSheetVisible by rememberSaveable { mutableStateOf(false) }
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val surahListState by viewModel.surahListState.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val selectedSurahNumber = searchFilter.surahNumber
    val surahs = (surahListState as? UiState.Success)?.data.orEmpty()
    val selectedSurah = surahs.firstOrNull { it.number == selectedSurahNumber }
    val hasSpecificFilter = searchFilter.field != QuranSearchField.ALL ||
        searchFilter.mode != QuranSearchMode.ALL_WORDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.searchQuran(it)
                        },
                        label = { Text("Cari ayat") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.clearSearch()
                                }) {
                                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Hapus")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Text(
                text = "Filter",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(bottom = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { filterMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedSurah?.nameLatin ?: "Semua Surah",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Surah") },
                            onClick = {
                                viewModel.setSearchSurahFilter(null)
                                filterMenuExpanded = false
                            }
                        )
                        surahs.forEach { surah ->
                            DropdownMenuItem(
                                text = { Text(surah.nameLatin) },
                                onClick = {
                                    viewModel.setSearchSurahFilter(surah.number)
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                FilterChip(
                    selected = hasSpecificFilter,
                    onClick = { filterSheetVisible = true },
                    label = {
                        Text(
                            text = searchFilterChipLabel(searchFilter),
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .heightIn(min = Spacing.xxl)
                        .testTag("search_filter_specific")
                )
            }
            when (val state = searchState) {
                is UiState.Idle -> {
                    AppEmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Pencarian FTS5 Cepat",
                        description = "Ketikkan kata kunci dalam Bahasa Indonesia, Arab, atau Latin untuk mencari seluruh ayat Al-Qur'an secara instan."
                    )
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        AppEmptyState(
                            icon = Icons.Rounded.Search,
                            title = "Tidak Ditemukan",
                            description = "Tidak ada ayat yang cocok dengan kata kunci \"$searchQuery\"."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(
                                items = state.data,
                                key = { "${it.surahNumber}_${it.ayahNumber}" }
                            ) { ayah ->
                                SearchResultCard(
                                    ayah = ayah,
                                    query = searchQuery,
                                    filter = searchFilter,
                                    onClick = { onAyahClick(ayah.surahNumber, ayah.ayahNumber) }
                                )
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    AppEmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Pencarian Gagal",
                        description = state.message
                    )
                }
                UiState.Empty -> {
                    AppEmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Tidak Ditemukan",
                        description = "Tidak ada ayat yang cocok dengan kata kunci \"$searchQuery\"."
                    )
                }
                is UiState.Blocked -> {
                    AppEmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Pencarian Tidak Tersedia",
                        description = state.reason
                    )
                }
            }
        }
    }

    if (filterSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { filterSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            modifier = Modifier.testTag("search_filter_sheet")
        ) {
            SearchFilterSheet(
                filter = searchFilter,
                onFilterChanged = viewModel::setSearchFilter,
                onClose = { filterSheetVisible = false }
            )
        }
    }
}

@Composable
private fun SearchFilterSheet(
    filter: QuranSearchFilter,
    onFilterChanged: (QuranSearchFilter) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.md)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Pencarian spesifik",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Sumber teks",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.selectableGroup()) {
            QuranSearchField.entries.forEach { field ->
                SearchFilterOption(
                    label = searchFieldLabel(field),
                    selected = filter.field == field,
                    onClick = { onFilterChanged(filter.copy(field = field)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "Pola pencarian",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.selectableGroup()) {
            SearchFilterOption(
                label = "Semua kata",
                selected = filter.mode == QuranSearchMode.ALL_WORDS,
                onClick = {
                    onFilterChanged(filter.copy(mode = QuranSearchMode.ALL_WORDS))
                }
            )
            SearchFilterOption(
                label = "Frasa tepat",
                selected = filter.mode == QuranSearchMode.EXACT_PHRASE,
                onClick = {
                    onFilterChanged(filter.copy(mode = QuranSearchMode.EXACT_PHRASE))
                }
            )
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Tutup")
        }
    }
}

@Composable
private fun SearchFilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.xxl)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.sm)
        )
    }
}

private fun searchFieldLabel(field: QuranSearchField): String = when (field) {
    QuranSearchField.ALL -> "Semua teks"
    QuranSearchField.ARABIC -> "Arab"
    QuranSearchField.INDONESIAN -> "Terjemahan Indonesia"
    QuranSearchField.ENGLISH -> "Terjemahan English"
    QuranSearchField.TRANSLITERATION -> "Transliterasi Latin"
}

private fun searchFilterChipLabel(filter: QuranSearchFilter): String {
    if (filter.field == QuranSearchField.ALL && filter.mode == QuranSearchMode.ALL_WORDS) {
        return "Spesifik"
    }
    val labels = buildList {
        if (filter.field != QuranSearchField.ALL) {
            add(
                when (filter.field) {
                    QuranSearchField.ARABIC -> "Arab"
                    QuranSearchField.INDONESIAN -> "Indonesia"
                    QuranSearchField.ENGLISH -> "English"
                    QuranSearchField.TRANSLITERATION -> "Latin"
                    QuranSearchField.ALL -> ""
                }
            )
        }
        if (filter.mode == QuranSearchMode.EXACT_PHRASE) add("Frasa")
    }
    return labels.joinToString(" • ")
}

@Composable
fun SearchResultCard(
    ayah: Ayah,
    query: String,
    filter: QuranSearchFilter = QuranSearchFilter(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QS. ${ayah.surahName} : Ayat ${ayah.ayahNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = highlightSearchMatches(
                    text = ayah.textArabic,
                    query = query,
                    highlightColor = MaterialTheme.colorScheme.primaryContainer,
                    highlightTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                style = getQuranArabicStyle(20f),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            val secondaryText = when (filter.field) {
                QuranSearchField.ARABIC -> null
                QuranSearchField.INDONESIAN -> ayah.translationId
                QuranSearchField.ENGLISH -> ayah.translationEn
                QuranSearchField.TRANSLITERATION -> ayah.transliteration
                QuranSearchField.ALL -> ayah.translationId.ifBlank { ayah.translationEn }
            }
            if (!secondaryText.isNullOrBlank()) {
                Text(
                    text = highlightSearchMatches(
                        text = secondaryText,
                        query = query,
                        highlightColor = MaterialTheme.colorScheme.secondaryContainer,
                        highlightTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
