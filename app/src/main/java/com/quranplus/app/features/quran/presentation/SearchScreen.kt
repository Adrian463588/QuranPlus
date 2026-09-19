package com.quranplus.app.features.quran.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.core.utils.SurahMapper
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.QuranSearchField
import com.quranplus.app.features.quran.domain.QuranSearchFilter
import com.quranplus.app.features.quran.domain.QuranSearchMode
import com.quranplus.app.features.quran.domain.Surah

private val POPULAR_SEARCH_KEYWORDS = listOf(
    "Bismillah",
    "Al-Ikhlas",
    "Ayat Kursi",
    "Sabar",
    "Shalat",
    "Rezeki",
    "Surga",
    "Orang Tua",
    "Taubat",
    "Petunjuk",
    "Keadilan",
    "Kedamaian"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val effectiveSurahs = remember(surahs) {
        if (surahs.isNotEmpty()) surahs
        else (1..114).mapNotNull { num ->
            val ref = SurahMapper.getSurah(num) ?: return@mapNotNull null
            Surah(
                number = ref.number,
                nameArabic = "",
                nameLatin = ref.latinName,
                nameEnglish = "",
                revelationType = "",
                ayahCount = ref.ayahCount
            )
        }
    }
    val matchingSurahs = remember(searchQuery, effectiveSurahs) {
        if (searchQuery.isBlank()) emptyList()
        else {
            effectiveSurahs.filter { surah ->
                SurahMapper.matchesSurah(
                    query = searchQuery,
                    surahNumber = surah.number,
                    nameLatin = surah.nameLatin,
                    nameArabic = surah.nameArabic,
                    nameEnglish = surah.nameEnglish
                )
            }
        }
    }
    val hasSpecificFilter = searchFilter.field != QuranSearchField.ALL ||
        searchFilter.mode != QuranSearchMode.ALL_WORDS
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                ) {
                    // Integrated Material 3 Search Bar
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = Spacing.xs),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari surah (misal: Al-Mulk, Ad-Duha) atau ayat...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        viewModel.searchQuran(it)
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        viewModel.clearSearch()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    // Horizontal Filter Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Surah Filter Chip
                        item {
                            Box {
                                FilterChip(
                                    selected = selectedSurahNumber != null,
                                    onClick = { filterMenuExpanded = true },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    label = {
                                        Text(selectedSurah?.nameLatin ?: "Semua Surah")
                                    }
                                )
                                DropdownMenu(
                                    expanded = filterMenuExpanded,
                                    onDismissRequest = { filterMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Semua Surah (1-114)") },
                                        onClick = {
                                            viewModel.setSearchSurahFilter(null)
                                            filterMenuExpanded = false
                                        }
                                    )
                                    surahs.forEach { surah ->
                                        DropdownMenuItem(
                                            text = { Text("${surah.number}. ${surah.nameLatin}") },
                                            onClick = {
                                                viewModel.setSearchSurahFilter(surah.number)
                                                filterMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Specific Filter Sheet Pill (with testTag)
                        item {
                            FilterChip(
                                selected = hasSpecificFilter,
                                onClick = { filterSheetVisible = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(text = searchFilterChipLabel(searchFilter))
                                },
                                modifier = Modifier.testTag("search_filter_specific")
                            )
                        }

                        // 3. Quick Language Target Chips
                        item {
                            FilterChip(
                                selected = searchFilter.field == QuranSearchField.ARABIC,
                                onClick = {
                                    val newField = if (searchFilter.field == QuranSearchField.ARABIC) QuranSearchField.ALL else QuranSearchField.ARABIC
                                    viewModel.setSearchFilter(searchFilter.copy(field = newField))
                                },
                                label = { Text("Arab") }
                            )
                        }

                        item {
                            FilterChip(
                                selected = searchFilter.field == QuranSearchField.INDONESIAN,
                                onClick = {
                                    val newField = if (searchFilter.field == QuranSearchField.INDONESIAN) QuranSearchField.ALL else QuranSearchField.INDONESIAN
                                    viewModel.setSearchFilter(searchFilter.copy(field = newField))
                                },
                                label = { Text("Indonesia") }
                            )
                        }

                        item {
                            FilterChip(
                                selected = searchFilter.field == QuranSearchField.ENGLISH,
                                onClick = {
                                    val newField = if (searchFilter.field == QuranSearchField.ENGLISH) QuranSearchField.ALL else QuranSearchField.ENGLISH
                                    viewModel.setSearchFilter(searchFilter.copy(field = newField))
                                },
                                label = { Text("English") }
                            )
                        }

                        item {
                            FilterChip(
                                selected = searchFilter.field == QuranSearchField.TRANSLITERATION,
                                onClick = {
                                    val newField = if (searchFilter.field == QuranSearchField.TRANSLITERATION) QuranSearchField.ALL else QuranSearchField.TRANSLITERATION
                                    viewModel.setSearchFilter(searchFilter.copy(field = newField))
                                },
                                label = { Text("Latin") }
                            )
                        }

                        // 4. Exact Phrase Mode Chip
                        item {
                            FilterChip(
                                selected = searchFilter.mode == QuranSearchMode.EXACT_PHRASE,
                                onClick = {
                                    val newMode = if (searchFilter.mode == QuranSearchMode.EXACT_PHRASE) QuranSearchMode.ALL_WORDS else QuranSearchMode.EXACT_PHRASE
                                    viewModel.setSearchFilter(searchFilter.copy(mode = newMode))
                                },
                                label = { Text("Frasa Tepat") }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (searchQuery.isBlank()) {
                when (searchState) {
                    is UiState.Idle -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            // Helpful FTS5 Information Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Column {
                                        Text(
                                            text = "Pencarian FTS5 Cepat",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Cari surah (misal: Al-Mulk, Ad-Duha) atau seluruh ayat Al-Qur'an secara instan.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Pencarian Populer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Popular Keywords Flow
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                POPULAR_SEARCH_KEYWORDS.forEach { keyword ->
                                    SuggestionChip(
                                        onClick = {
                                            searchQuery = keyword
                                            viewModel.searchQuran(keyword)
                                        },
                                        label = {
                                            Text(text = keyword)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // 1. Matching Surahs Section
                    if (matchingSurahs.isNotEmpty()) {
                        item(key = "surah_matches_header") {
                            Text(
                                text = "Surah Ditemukan (${matchingSurahs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xs)
                            )
                        }
                        items(
                            items = matchingSurahs,
                            key = { "surah_${it.number}" }
                        ) { surah ->
                            SearchSurahMatchCard(
                                surah = surah,
                                onClick = { onAyahClick(surah.number, 1) }
                            )
                        }
                        item(key = "surah_matches_divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // 2. Ayat Search Results Section
                    when (val state = searchState) {
                        is UiState.Loading -> {
                            item(key = "loading_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.lg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        is UiState.Success -> {
                            if (state.data.isNotEmpty()) {
                                item(key = "ayah_results_count") {
                                    Text(
                                        text = "Ayat Ditemukan (${state.data.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Spacing.xs)
                                    )
                                }
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
                            } else if (matchingSurahs.isEmpty()) {
                                item(key = "empty_ayahs_none") {
                                    AppEmptyState(
                                        icon = Icons.Rounded.Search,
                                        title = "Tidak Ditemukan",
                                        description = "Tidak ada surah atau ayat yang cocok dengan kata kunci \"$searchQuery\"."
                                    )
                                }
                            } else {
                                item(key = "no_matching_ayahs_note") {
                                    Text(
                                        text = "Tidak ada ayat yang cocok dengan kata kunci \"$searchQuery\".",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Spacing.sm)
                                    )
                                }
                            }
                        }
                        is UiState.Empty -> {
                            if (matchingSurahs.isEmpty()) {
                                item(key = "empty_state") {
                                    AppEmptyState(
                                        icon = Icons.Rounded.Search,
                                        title = "Tidak Ditemukan",
                                        description = "Tidak ada surah atau ayat yang cocok dengan kata kunci \"$searchQuery\"."
                                    )
                                }
                            } else {
                                item(key = "no_matching_ayahs_note") {
                                    Text(
                                        text = "Tidak ada ayat yang cocok dengan kata kunci \"$searchQuery\".",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Spacing.sm)
                                    )
                                }
                            }
                        }
                        is UiState.Error -> {
                            if (matchingSurahs.isEmpty()) {
                                item(key = "error_state") {
                                    AppEmptyState(
                                        icon = Icons.Rounded.Search,
                                        title = "Pencarian Gagal",
                                        description = state.message
                                    )
                                }
                            } else {
                                item(key = "error_ayah_note") {
                                    Text(
                                        text = "Pencarian ayat gagal: ${state.message}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(vertical = Spacing.sm)
                                    )
                                }
                            }
                        }
                        is UiState.Blocked -> {
                            if (matchingSurahs.isEmpty()) {
                                item(key = "blocked_state") {
                                    AppEmptyState(
                                        icon = Icons.Rounded.Search,
                                        title = "Pencarian Tidak Tersedia",
                                        description = state.reason
                                    )
                                }
                            }
                        }
                        UiState.Idle -> {
                            // User typed query but search debounce / job has not emitted yet
                        }
                    }
                }
            }
        }
    }

    if (filterSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { filterSheetVisible = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
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
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.lg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pencarian spesifik",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Tutup")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

        Text(
            text = "Sumber teks",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Column(modifier = Modifier.selectableGroup()) {
            QuranSearchField.entries.forEach { field ->
                SearchFilterOption(
                    label = searchFieldLabel(field),
                    selected = filter.field == field,
                    onClick = { onFilterChanged(filter.copy(field = field)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "Pola pencarian",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
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

        Spacer(modifier = Modifier.height(Spacing.lg))

        AppPrimaryButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Terapkan & Tutup")
        }
    }
}

@Composable
private fun SearchFilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filter: QuranSearchFilter = QuranSearchFilter()
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
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
                    text = "QS. ${ayah.surahName} (${ayah.surahNumber}) : Ayat ${ayah.ayahNumber}",
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
                style = getQuranArabicStyle(22f),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
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

@Composable
fun SearchSurahMatchCard(
    surah: Surah,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameLatin,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = buildString {
                    if (surah.nameEnglish.isNotBlank()) append(surah.nameEnglish)
                    if (surah.revelationType.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(surah.revelationType.uppercase())
                    }
                    if (surah.ayahCount > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("${surah.ayahCount} Ayat")
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (surah.nameArabic.isNotBlank()) {
                Text(
                    text = surah.nameArabic,
                    style = getQuranArabicStyle(22f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
