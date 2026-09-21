package com.quranplus.app.features.dzikir.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.components.TajwidLegendSheet
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.dzikir.domain.DzikirCategory
import com.quranplus.app.features.dzikir.presentation.components.DzikirCard
import com.quranplus.app.features.dzikir.presentation.components.DzikirSettingsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DzikirScreen(
    viewModel: DzikirViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTajwidLegend by remember { mutableStateOf(false) }
    val tajwidSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Dzikir, Wirid & Hizib",
                subtitle = if (state.searchQuery.isNotBlank()) "Pencarian: \"${state.searchQuery}\"" else state.selectedCategory.title,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showTajwidLegend = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Panduan Tajwid Berwarna",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Pengaturan Tampilan",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.resetAllCounters() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset Semua Hitungan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isTablet) {
                // Two-Pane Adaptive Layout for Tablets (e.g. Samsung Galaxy Tab S7)
                TabletDzikirLayout(
                    state = state,
                    onCategorySelect = { viewModel.selectCategory(it) },
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onIncrement = { id, max -> viewModel.incrementCounter(id, max) },
                    onReset = { id -> viewModel.resetCounter(id) },
                    onResetAll = { viewModel.resetAllCounters() },
                    onOpenTajwidLegend = { showTajwidLegend = true },
                    onOpenSettings = { showSettingsSheet = true }
                )
            } else {
                // Single-Column Layout for Mobile / Narrow Screens
                PhoneDzikirLayout(
                    state = state,
                    onCategorySelect = { viewModel.selectCategory(it) },
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onIncrement = { id, max -> viewModel.incrementCounter(id, max) },
                    onReset = { id -> viewModel.resetCounter(id) }
                )
            }
        }
    }

    if (showSettingsSheet) {
        DzikirSettingsSheet(
            settings = state.settings,
            onFontSizeChange = { viewModel.updateFontSize(it) },
            onToggleTajwid = { viewModel.toggleTajwid() },
            onToggleTransliteration = { viewModel.toggleTransliteration() },
            onToggleTranslation = { viewModel.toggleTranslation() },
            onLanguageSelect = { viewModel.setTranslationLanguage(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showTajwidLegend) {
        TajwidLegendSheet(
            sheetState = tajwidSheetState,
            onDismissRequest = { showTajwidLegend = false }
        )
    }
}

@Composable
private fun TabletDzikirLayout(
    state: DzikirUiState,
    onCategorySelect: (DzikirCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onIncrement: (String, Int) -> Unit,
    onReset: (String) -> Unit,
    onResetAll: () -> Unit,
    onOpenTajwidLegend: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Pane: Category Navigation & Stats
        Surface(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md)
            ) {
                // Search Field
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cari dzikir, doa, arti...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Hapus")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = "Daftar Amalan & Hizib",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(state.categories, key = { it.id }) { cat ->
                        val isSelected = cat == state.selectedCategory && state.searchQuery.isBlank()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategorySelect(cat) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(8.dp)
                                ) {}

                                Spacer(modifier = Modifier.width(Spacing.sm))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = cat.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Pane: Content List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Tidak ada bacaan ditemukan",
                    description = if (state.searchQuery.isNotBlank()) "Tidak ada hasil untuk \"${state.searchQuery}\"" else "Kategori ini belum memiliki bacaan.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        val currentCount = state.counterMap[item.id] ?: 0
                        DzikirCard(
                            item = item,
                            currentCount = currentCount,
                            settings = state.settings,
                            onIncrement = { onIncrement(item.id, item.repeatCount) },
                            onReset = { onReset(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneDzikirLayout(
    state: DzikirUiState,
    onCategorySelect: (DzikirCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onIncrement: (String, Int) -> Unit,
    onReset: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari dzikir, doa, transliterasi, terjemahan...") },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Hapus")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )

        // Horizontal Category Tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            items(state.categories, key = { it.id }) { cat ->
                val isSelected = cat == state.selectedCategory && state.searchQuery.isBlank()
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(cat) },
                    label = { Text(cat.title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Content List
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Tidak ada bacaan ditemukan",
                    description = if (state.searchQuery.isNotBlank()) "Tidak ada hasil untuk \"${state.searchQuery}\"" else "Kategori ini belum memiliki bacaan.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        val currentCount = state.counterMap[item.id] ?: 0
                        DzikirCard(
                            item = item,
                            currentCount = currentCount,
                            settings = state.settings,
                            onIncrement = { onIncrement(item.id, item.repeatCount) },
                            onReset = { onReset(item.id) }
                        )
                    }
                }
            }
        }
    }
}
