package com.quranplus.app.features.audio.presentation

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.core.audio.Qari
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.quran.domain.Surah
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.quran.presentation.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioManagerScreen(
    audioPlayerManager: AudioPlayerManager,
    quranViewModel: QuranViewModel,
    onBackClick: () -> Unit
) {
    val selectedQari by audioPlayerManager.selectedQari.collectAsStateWithLifecycle()
    val surahsState by quranViewModel.surahListState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var storageBytes by remember { mutableLongStateOf(audioPlayerManager.getAudioStorageBytes()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Audio Manager Murottal",
                subtitle = "Kelola unduhan murottal & penyimpanan offline",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Storage Capacity Meter Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Penyimpanan Murottal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${"%.1f".format(storageBytes / (1024.0 * 1024.0))} MB",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mendukung pemutaran tanpa kuota internet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = Spacing.sm)
                        )
                        OutlinedButton(
                            onClick = {
                                val deletedBytes = audioPlayerManager.clearDownloadedAudio()
                                storageBytes = audioPlayerManager.getAudioStorageBytes()
                                Toast.makeText(
                                    context,
                                    "Cache audio dihapus: ${deletedBytes / (1024 * 1024)} MB",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = storageBytes > 0L,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Cache", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Qari Selection Selector
            Text(
                text = "Pilih Qari Murattal:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Qari.entries.forEach { qari ->
                    val isSelected = selectedQari == qari
                    FilterChip(
                        selected = isSelected,
                        onClick = { audioPlayerManager.setSelectedQari(qari) },
                        label = { Text(qari.displayName.split(" ").first(), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Surahs Audio Download List
            when (val state = surahsState) {
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        items(state.data, key = { it.number }) { surah ->
                            SurahAudioDownloadRow(
                                surah = surah,
                                qari = selectedQari,
                                storedBytes = audioPlayerManager.getSurahAudioBytes(selectedQari, surah.number)
                            )
                        }
                    }
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Memuat daftar surah...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is UiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Data surah tidak tersedia", color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Blocked, UiState.Idle -> Unit
            }
        }
    }
}

@Composable
fun SurahAudioDownloadRow(
    surah: Surah,
    qari: Qari,
    storedBytes: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${surah.number}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text = surah.nameLatin,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (storedBytes > 0L) {
                            "${storedBytes / (1024 * 1024)} MB tersimpan • ${qari.displayName}"
                        } else {
                            "Asset audio terverifikasi belum tersedia"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = {},
                enabled = false
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Unduhan audio diblokir sampai manifest URL dan SHA-256 tersedia",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
