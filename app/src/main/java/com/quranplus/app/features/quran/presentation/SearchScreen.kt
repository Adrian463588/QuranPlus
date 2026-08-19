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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.quran.domain.Ayah

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
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val surahListState by viewModel.surahListState.collectAsStateWithLifecycle()
    val selectedSurahNumber by viewModel.searchSurahFilter.collectAsStateWithLifecycle()
    val surahs = (surahListState as? UiState.Success)?.data.orEmpty()
    val selectedSurah = surahs.firstOrNull { it.number == selectedSurahNumber }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Filter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
}

@Composable
fun SearchResultCard(
    ayah: Ayah,
    query: String,
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

            Text(
                text = highlightSearchMatches(
                    text = ayah.translationId,
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
