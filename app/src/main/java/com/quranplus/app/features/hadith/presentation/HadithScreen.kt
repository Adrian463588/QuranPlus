package com.quranplus.app.features.hadith.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithScreen(viewModel: HadithViewModel) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val catalogCollectionCount = collections.size

    androidx.compose.material3.Scaffold(
        topBar = { AppTopBar(title = "Hadist", subtitle = "Arab + terjemahan English") }
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
                label = { Text("Cari hadist") }
            )
            when (val current = state) {
                HadithUiState.Loading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
                HadithUiState.Empty -> AppEmptyState(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = "Hadist belum tersedia",
                    description = if (catalogCollectionCount > 0) {
                        "$catalogCollectionCount koleksi tersedia sebagai katalog. Impor file JSON hadist dari folder referensi untuk membaca isinya."
                    } else {
                        "Belum ada riwayat hadist. Impor file JSON dari folder referensi."
                    }
                )
                is HadithUiState.Error -> AppEmptyState(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = "Hadist tidak dapat dimuat",
                    description = current.message
                )
                is HadithUiState.Ready -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    items(current.records, key = { it.id }) { record ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${record.title} · No. ${record.hadithNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = record.textArabic,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (record.translationEn.isNotBlank()) {
                                Text(
                                    text = record.translationEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Terjemahan English tidak tersedia pada sumber ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = record.reference,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
