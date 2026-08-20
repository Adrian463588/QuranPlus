package com.quranplus.app.features.hadith.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.R
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppOutlinedButton
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bundleState by viewModel.bundleState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.bundleReadyEvents.collect { onBundleReadyForAi() }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.hadith_title),
                subtitle = stringResource(R.string.hadith_subtitle)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.hadith_search_label)) }
            )
            HadithBundleSection(
                state = bundleState,
                onRequestStorage = onRequestStorage,
                onDownload = viewModel::startBundleDownload,
                modifier = Modifier.fillMaxWidth()
            )
            when (val current = state) {
                HadithUiState.Catalog -> HadithCollectionCatalog(
                    collections = collections,
                    onCollectionClick = viewModel::setCollection,
                    modifier = Modifier.weight(1f)
                )

                HadithUiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
                HadithUiState.Empty -> AppEmptyState(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = if (selectedCollection == null) {
                        stringResource(R.string.hadith_catalog_empty)
                    } else {
                        stringResource(R.string.hadith_no_search_results)
                    },
                    description = if (selectedCollection == null) {
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
                    onShowCollections = { viewModel.setCollection(null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HadithBundleSection(
    state: HadithBundleUiState,
    onRequestStorage: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = stringResource(R.string.hadith_bundle_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.hadith_bundle_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (val workState = state.workState) {
            is HadithBundleWorkState.Downloading -> {
                if (workState.totalBytes > 0L) {
                    LinearProgressIndicator(
                        progress = { workState.progressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = stringResource(R.string.hadith_bundle_downloading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HadithBundleWorkState.Importing -> Text(
                text = stringResource(R.string.hadith_bundle_importing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            HadithBundleWorkState.Queued -> Text(
                text = stringResource(R.string.hadith_bundle_queued),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HadithBundleWorkState.Completed,
            HadithBundleWorkState.Idle,
            is HadithBundleWorkState.Failed -> Unit
        }
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
        when {
            !state.storageLinked -> AppPrimaryButton(
                onClick = onRequestStorage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.hadith_bundle_choose_folder))
            }
            state.workState is HadithBundleWorkState.Downloading ||
                state.workState is HadithBundleWorkState.Importing ||
                state.workState is HadithBundleWorkState.Queued -> Unit
            else -> AppOutlinedButton(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            ) {
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

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
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
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Text(
                text = stringResource(R.string.hadith_collection_list_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        sectionedHadithCollections(collections).forEach { (section, items) ->
            item(key = "section:${section.name}") {
                HadithSectionHeader(section)
            }
            items(items, key = { it.id }) { collection ->
                HadithCollectionItem(collection) {
                    onCollectionClick(collection.id)
                }
            }
        }
    }
}

@Composable
private fun HadithSectionHeader(section: HadithCollectionSection) {
    Text(
        text = when (section) {
            HadithCollectionSection.KUTUBUS_SITTAH -> stringResource(R.string.hadith_section_kutubus_sittah)
            HadithCollectionSection.OTHER -> stringResource(R.string.hadith_section_other)
        },
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun HadithCollectionItem(
    collection: HadithCollection,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = collection.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = if (collection.hasLocalContent) {
                    stringResource(R.string.hadith_collection_count, collection.count)
                } else {
                    stringResource(R.string.hadith_collection_unavailable)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
    )
    HorizontalDivider()
}

@Composable
private fun HadithResults(
    records: List<HadithRecord>,
    selectedCollection: String?,
    collections: List<HadithCollection>,
    onShowCollections: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedCollection != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onShowCollections) {
                    Text(stringResource(R.string.hadith_show_collections))
                }
                Text(
                    text = collections.firstOrNull { it.id == selectedCollection }?.title.orEmpty(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            items(records, key = { it.id }) { record ->
                HadithRecordItem(record)
            }
        }
    }
}

@Composable
private fun HadithRecordItem(record: HadithRecord) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.hadith_record_heading, record.title, record.hadithNumber),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = record.textArabic,
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
            modifier = Modifier.fillMaxWidth()
        )
        if (record.translationId.isNotBlank()) {
            Text(
                text = stringResource(R.string.hadith_translation_indonesian),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = record.translationId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.translationEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = record.reference,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
