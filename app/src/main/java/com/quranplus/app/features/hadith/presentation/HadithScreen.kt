package com.quranplus.app.features.hadith.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.R
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppOutlinedButton
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.QuranFontFamily
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.hadith.data.HadithBundleWorkState
import com.quranplus.app.features.hadith.domain.HadithCollection
import com.quranplus.app.features.hadith.domain.HadithCollectionSection
import com.quranplus.app.features.hadith.domain.HadithRecord
import com.quranplus.app.features.hadith.domain.sectionedHadithCollections

@Composable
fun HadithScreen(
    viewModel: HadithViewModel,
    onRequestStorage: () -> Unit,
    onBundleReadyForAi: () -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollection.collectAsStateWithLifecycle()
    val scrollToIndex by viewModel.scrollToIndex.collectAsStateWithLifecycle()
    val highlightedNumber by viewModel.highlightedNumber.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bundleState by viewModel.bundleState.collectAsStateWithLifecycle()

    var showBundleDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.bundleReadyEvents.collect { onBundleReadyForAi() }
    }

    val isReadingMode = selectedCollection != null || query.isNotBlank()
    val activeCollection = collections.firstOrNull { it.id == selectedCollection }

    // Intercept hardware / gesture back button to return to catalog first
    BackHandler(enabled = isReadingMode) {
        viewModel.resetToCatalog()
    }

    Scaffold(
        topBar = {
            if (isReadingMode) {
                AppTopBar(
                    title = activeCollection?.title ?: if (query.isNotBlank()) "Pencarian Hadist" else stringResource(R.string.hadith_title),
                    subtitle = if (activeCollection != null) {
                        val arabicPart = activeCollection.titleArabic.ifBlank { "" }
                        if (arabicPart.isNotBlank()) "$arabicPart · ${activeCollection.count} hadist" else "${activeCollection.count} hadist"
                    } else if (query.isNotBlank()) {
                        "Kata kunci / No: \"$query\""
                    } else {
                        stringResource(R.string.hadith_subtitle)
                    },
                    onBackClick = { viewModel.resetToCatalog() },
                    actions = {
                        IconButton(onClick = { showJumpDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Lompat ke No. Hadits",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            } else {
                AppTopBar(
                    title = stringResource(R.string.hadith_title),
                    subtitle = stringResource(R.string.hadith_subtitle),
                    actions = {
                        IconButton(onClick = { showJumpDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Cari Hadits",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showBundleDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = "Bundle Hadist Offline",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Search Input Field
            HadithSearchBar(
                query = query,
                onQueryChange = viewModel::setQuery,
                onClearQuery = { viewModel.setQuery("") },
                placeholder = if (activeCollection != null) {
                    "Cari no. hadits atau isi dalam ${activeCollection.title} (contoh: 1, 42)..."
                } else {
                    "Cari no. hadits, kitab, atau kata kunci (contoh: 42, shalat)..."
                }
            )

            // Quick Number Filter Chips when reading a collection
            if (activeCollection != null) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { showJumpDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Lompat No...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    val sampleNumbers = listOf(1, 10, 25, 42, 50, 100, 200, 500, 1000)
                        .filter { activeCollection.count == 0 || it <= activeCollection.count }
                    items(sampleNumbers) { num ->
                        val isSelected = query == num.toString()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.clickable {
                                viewModel.setQuery(num.toString())
                            }
                        ) {
                            Text(
                                text = "No. $num",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Offline Bundle Status / Download Banner (Collapsible/Adaptive)
            HadithBundleSection(
                state = bundleState,
                isReadingMode = isReadingMode,
                onRequestStorage = onRequestStorage,
                onDownload = viewModel::startBundleDownload,
                modifier = Modifier.fillMaxWidth()
            )

            // Content Area
            when (val current = state) {
                HadithUiState.Catalog -> HadithCollectionCatalog(
                    collections = collections,
                    onCollectionClick = viewModel::setCollection,
                    modifier = Modifier.weight(1f)
                )

                HadithUiState.Loading -> LoadingState(modifier = Modifier.weight(1f))

                HadithUiState.Empty -> AppEmptyState(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = if (query.isNotBlank()) {
                        "Hadist Tidak Ditemukan"
                    } else if (selectedCollection == null) {
                        stringResource(R.string.hadith_catalog_empty)
                    } else {
                        stringResource(R.string.hadith_no_search_results)
                    },
                    description = if (query.isNotBlank()) {
                        "Tidak ditemukan hadist dengan nomor atau kata kunci \"$query\". Pastikan bundle Hadist lokal sudah diunduh dan diverifikasi."
                    } else if (selectedCollection == null) {
                        stringResource(R.string.hadith_catalog_empty)
                    } else {
                        stringResource(R.string.hadith_no_records)
                    },
                    modifier = Modifier.weight(1f)
                )

                is HadithUiState.Error -> AppEmptyState(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = stringResource(R.string.hadith_no_search_results),
                    description = current.message,
                    modifier = Modifier.weight(1f)
                )

                is HadithUiState.Ready -> HadithResults(
                    records = current.records,
                    selectedCollection = selectedCollection,
                    collections = collections,
                    query = query,
                    scrollToIndex = scrollToIndex,
                    highlightedNumber = highlightedNumber,
                    onScrolledToIndex = viewModel::onScrolledToIndex,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showJumpDialog) {
            JumpToHadithDialog(
                collectionTitle = activeCollection?.title,
                maxNumber = activeCollection?.count,
                onJump = { num ->
                    viewModel.setQuery(num)
                },
                onDismiss = { showJumpDialog = false }
            )
        }

        if (showBundleDialog) {
            HadithBundleDialog(
                state = bundleState,
                onRequestStorage = onRequestStorage,
                onDownload = {
                    viewModel.startBundleDownload()
                },
                onDismiss = { showBundleDialog = false }
            )
        }
    }
}

@Composable
fun HadithBundleDialog(
    state: HadithBundleUiState,
    onRequestStorage: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Bundle Hadist Offline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (state.localRecordCount > 0) {
                    Text(
                        text = "Status: ${state.localRecordCount} Hadits Terpasang (${state.localCollectionCount} Kitab)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Kutubus Sittah dan kitab hadits lainnya telah siap digunakan secara offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Bundle Hadist belum terpasang di perangkat Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!state.storageLinked) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    AppPrimaryButton(
                        onClick = {
                            onRequestStorage()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Pilih Folder SAF")
                    }
                }
            }
        },
        confirmButton = {
            if (state.localRecordCount == 0 && state.storageLinked) {
                AppPrimaryButton(
                    onClick = {
                        onDownload()
                        onDismiss()
                    }
                ) {
                    Text("Unduh Bundle")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        },
        dismissButton = {
            if (state.localRecordCount > 0 && state.storageLinked) {
                TextButton(
                    onClick = {
                        onDownload()
                        onDismiss()
                    }
                ) {
                    Text("Perbarui Hadist")
                }
            }
        }
    )
}

@Composable
private fun HadithSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = Spacing.xs, end = Spacing.sm)
                    .size(22.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Spacing.xs),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Hapus pencarian",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HadithBundleSection(
    state: HadithBundleUiState,
    isReadingMode: Boolean,
    onRequestStorage: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = state.workState is HadithBundleWorkState.Downloading ||
        state.workState is HadithBundleWorkState.Importing ||
        state.workState is HadithBundleWorkState.Queued

    // Disappear once bundle is installed
    if (state.localRecordCount > 0 || isReadingMode) return

    Card(
        modifier = modifier.padding(vertical = Spacing.xs),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = if (state.localRecordCount > 0) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.CloudDownload
                    }
                    val iconTint = if (state.localRecordCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.hadith_bundle_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.localRecordCount > 0) {
                            Text(
                                text = stringResource(
                                    R.string.hadith_bundle_installed,
                                    state.localCollectionCount,
                                    state.localRecordCount
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Description when not busy and not installed
            if (!isBusy && state.localRecordCount == 0) {
                Text(
                    text = stringResource(R.string.hadith_bundle_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress / Status indicator during downloads
            when (val workState = state.workState) {
                is HadithBundleWorkState.Downloading -> {
                    if (workState.totalBytes > 0L) {
                        LinearProgressIndicator(
                            progress = { workState.progressPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs)
                        )
                    }
                    Text(
                        text = "${stringResource(R.string.hadith_bundle_downloading)} (${workState.progressPercentage}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HadithBundleWorkState.Importing -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs)
                    )
                    Text(
                        text = stringResource(R.string.hadith_bundle_importing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HadithBundleWorkState.Queued -> Text(
                    text = stringResource(R.string.hadith_bundle_queued),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HadithBundleWorkState.Completed,
                HadithBundleWorkState.Idle,
                is HadithBundleWorkState.Failed -> Unit
            }

            // Action Buttons
            when {
                !state.storageLinked -> AppPrimaryButton(
                    onClick = onRequestStorage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.hadith_bundle_choose_folder))
                }
                isBusy -> Unit
                else -> AppOutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = if (state.localRecordCount > 0) {
                            stringResource(R.string.hadith_bundle_update)
                        } else {
                            stringResource(R.string.hadith_bundle_download)
                        }
                    )
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HadithCollectionCatalog(
    collections: List<HadithCollection>,
    onCollectionClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hadith_collection_catalog"),
        contentPadding = PaddingValues(top = Spacing.xs, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        sectionedHadithCollections(collections).forEach { (section, items) ->
            item(key = "section:${section.name}") {
                HadithSectionHeader(section)
            }
            items(items, key = { it.id }) { collection ->
                HadithCollectionCard(
                    collection = collection,
                    onClick = { onCollectionClick(collection.id) }
                )
            }
        }
    }
}

@Composable
private fun HadithSectionHeader(section: HadithCollectionSection) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = when (section) {
                HadithCollectionSection.KUTUBUS_SITTAH -> "Kutubus Sittah (6 Kitab Utama)"
                HadithCollectionSection.OTHER -> stringResource(R.string.hadith_section_other)
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HadithCollectionCard(
    collection: HadithCollection,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.weight(1f)
            ) {
                // Book Icon Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title & Subtitle Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = collection.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (collection.hasLocalContent) {
                            stringResource(R.string.hadith_collection_count, collection.count)
                        } else {
                            stringResource(R.string.hadith_collection_unavailable)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (collection.hasLocalContent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Arabic Title Calligraphy on Right
            if (collection.titleArabic.isNotBlank()) {
                Text(
                    text = collection.titleArabic,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = QuranFontFamily,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
        }
    }
}

@Composable
fun JumpToHadithDialog(
    collectionTitle: String?,
    maxNumber: Int?,
    onJump: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputNumber by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Cari Nomor Hadits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = if (collectionTitle != null && maxNumber != null && maxNumber > 0) {
                        "Masukkan nomor hadits dalam $collectionTitle (1 - $maxNumber):"
                    } else {
                        "Masukkan nomor hadits yang ingin dicari (contoh: 1, 42, 100):"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                OutlinedTextField(
                    value = inputNumber,
                    onValueChange = { inputNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nomor Hadits") },
                    placeholder = { Text("Contoh: 42") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Mencari dari database lokal dan internet secara otomatis.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                onClick = {
                    if (inputNumber.isNotBlank()) {
                        onJump(inputNumber)
                        onDismiss()
                    }
                }
            ) {
                Text("Cari")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun HadithResults(
    records: List<HadithRecord>,
    selectedCollection: String?,
    collections: List<HadithCollection>,
    modifier: Modifier = Modifier,
    query: String = "",
    scrollToIndex: Int? = null,
    highlightedNumber: Int? = null,
    onScrolledToIndex: () -> Unit = {}
) {
    val collectionName = collections.firstOrNull { it.id == selectedCollection }?.title.orEmpty()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(scrollToIndex) {
        val target = scrollToIndex ?: return@LaunchedEffect
        if (target in records.indices) {
            val listIndex = if (query.isNotBlank() || highlightedNumber != null) target + 1 else target
            listState.animateScrollToItem(listIndex)
            onScrolledToIndex()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = Spacing.xs, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (highlightedNumber != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Menampilkan koleksi lengkap · Terfokus pada Hadits No. $highlightedNumber",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else if (query.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Menampilkan ${records.size} hadits untuk \"$query\" (Referensi Lokal)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                    )
                }
            }
        }
        items(records, key = { it.id }) { record ->
            val isHighlighted = highlightedNumber != null && record.hadithNumber == highlightedNumber
            HadithCardItem(
                record = record,
                collectionName = collectionName.ifBlank { record.title },
                isHighlighted = isHighlighted
            )
        }
    }
}

@Composable
private fun HadithCardItem(
    record: HadithRecord,
    collectionName: String,
    isHighlighted: Boolean = false
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 2.dp),
        border = if (isHighlighted) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // 1. Card Header: Hadith Identifier Badge & Quick Actions (Copy / Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Number Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "HR. $collectionName · No. ${record.hadithNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                        )
                    }
                    if (isHighlighted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = "🎯 Ditemukan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Copy & Share Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val fullText = buildString {
                                appendLine("HR. $collectionName No. ${record.hadithNumber}")
                                appendLine()
                                appendLine(record.textArabic)
                                appendLine()
                                if (record.translationId.isNotBlank()) {
                                    appendLine("Terjemahan:")
                                    appendLine(record.translationId)
                                } else if (record.translationEn.isNotBlank()) {
                                    appendLine("Translation:")
                                    appendLine(record.translationEn)
                                }
                                if (record.reference.isNotBlank()) {
                                    appendLine()
                                    appendLine("Rujukan: ${record.reference}")
                                }
                                appendLine("(Quran Plus)")
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hadist", fullText))
                            Toast.makeText(context, "Hadist No. ${record.hadithNumber} disalin", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Salin Hadist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareText = buildString {
                                appendLine("HR. $collectionName No. ${record.hadithNumber}")
                                appendLine()
                                appendLine(record.textArabic)
                                appendLine()
                                if (record.translationId.isNotBlank()) {
                                    appendLine("Terjemahan:")
                                    appendLine(record.translationId)
                                } else if (record.translationEn.isNotBlank()) {
                                    appendLine("Translation:")
                                    appendLine(record.translationEn)
                                }
                                if (record.reference.isNotBlank()) {
                                    appendLine()
                                    appendLine("Rujukan: ${record.reference}")
                                }
                                appendLine()
                                appendLine("Dibagikan via Quran Plus")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Hadist"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Bagikan Hadist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 2. Arabic Text with Uthmani Font & RTL formatting
            Text(
                text = record.textArabic,
                style = getQuranArabicStyle(fontSizeSp = 23f).copy(
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs)
            )

            // 3. Subtle Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = Spacing.xs)
            )

            // 4. Translation Section
            if (record.translationId.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = stringResource(R.string.hadith_translation_indonesian),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = record.translationId,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.hadith_translation_indonesian_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (record.translationEn.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.hadith_translation_english),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = record.translationEn,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5. Reference & Chapter Footer
            if (record.reference.isNotBlank()) {
                Text(
                    text = "Rujukan: ${record.reference}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        }
    }
}
