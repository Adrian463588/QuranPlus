package com.quranplus.app.features.audio.presentation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.core.audio.PlaybackState
import com.quranplus.app.core.audio.Qari
import com.quranplus.app.core.ui.components.AppOutlinedButton
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.QuranColors
import com.quranplus.app.core.ui.theme.QuranFontFamily
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.audio.domain.AudioDownloadKey
import com.quranplus.app.features.audio.domain.AudioDownloadState
import com.quranplus.app.features.quran.domain.Surah
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.quran.presentation.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AudioManagerScreen(
    audioPlayerManager: AudioPlayerManager,
    quranViewModel: QuranViewModel,
    downloadViewModel: AudioDownloadViewModel,
    onRequestStorage: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val selectedQari by audioPlayerManager.selectedQari.collectAsStateWithLifecycle()
    val surahsState by quranViewModel.surahListState.collectAsStateWithLifecycle()
    val downloadStates by downloadViewModel.states.collectAsStateWithLifecycle()
    val safStatus by downloadViewModel.safStatus.collectAsStateWithLifecycle()
    val isBatchDownloading by downloadViewModel.isBatchDownloading.collectAsStateWithLifecycle()
    val playbackState by audioPlayerManager.playbackState.collectAsStateWithLifecycle()
    val currentTrack by audioPlayerManager.currentTrack.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var storageBytes by remember { mutableLongStateOf(0L) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        storageBytes = withContext(Dispatchers.IO) { audioPlayerManager.getAudioStorageBytes() }
        downloadViewModel.refreshSafStatus()
    }
    LaunchedEffect(downloadStates.values.count { it is AudioDownloadState.Completed }) {
        storageBytes = withContext(Dispatchers.IO) { audioPlayerManager.getAudioStorageBytes() }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Audio Manager Murottal",
                subtitle = "Kelola unduhan murottal & penyimpanan aman SAF",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            AudioManagerDockedPlayer(audioPlayerManager = audioPlayerManager)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. SAF Storage Status & Sync Banner
            SafStorageSection(
                safLinked = safStatus?.isAccessible == true,
                onRequestStorage = onRequestStorage,
                onRestoreFromSaf = {
                    downloadViewModel.restoreFromSaf { count ->
                        Toast.makeText(
                            context,
                            if (count > 0) "$count audio berhasil dipulihkan dari SAF" else "Semua audio sudah tersinkronisasi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )

            // 2. Storage Metrics Header
            StorageMetricsCard(
                storageBytes = storageBytes,
                onClearCache = {
                    val deletedBytes = audioPlayerManager.clearDownloadedAudio()
                    storageBytes = audioPlayerManager.getAudioStorageBytes()
                    Toast.makeText(
                        context,
                        "Cache audio dibersihkan: ${deletedBytes / (1024 * 1024)} MB",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )

            // 3. Qari Selection Row
            QariSelectorSection(
                selectedQari = selectedQari,
                onSelectQari = { audioPlayerManager.setSelectedQari(it) },
                modifier = Modifier.padding(vertical = Spacing.xs)
            )

            // 4. Batch Download & Search Action Bar
            val allSurahs = (surahsState as? UiState.Success)?.data.orEmpty()
            val filteredSurahs = remember(allSurahs, searchQuery) {
                if (searchQuery.isBlank()) allSurahs
                else allSurahs.filter {
                    it.nameLatin.contains(searchQuery, ignoreCase = true) ||
                        it.number.toString() == searchQuery.trim() ||
                        it.nameArabic.contains(searchQuery)
                }
            }

            val allDownloaded = allSurahs.isNotEmpty() && allSurahs.all {
                audioPlayerManager.isSurahFullyDownloaded(selectedQari, it.number, it.ayahCount)
            }

            ActionBarSection(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onClearSearch = { searchQuery = "" },
                isBatchDownloading = isBatchDownloading,
                isAllDownloaded = allDownloaded,
                onDownloadAll = {
                    if (allSurahs.isNotEmpty()) {
                        downloadViewModel.downloadAllSurahs(selectedQari, allSurahs)
                        Toast.makeText(context, "Memulai unduhan 114 surah untuk ${selectedQari.displayName}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = Spacing.xs)
            )

            // 5. Surahs Audio Download List
            when (val state = surahsState) {
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        items(filteredSurahs, key = { it.number }) { surah ->
                            val downloadKey = AudioDownloadKey(selectedQari.id, surah.number)
                            val downloadState = downloadStates[downloadKey] ?: AudioDownloadState.Idle
                            val isFullyDownloaded = audioPlayerManager.isSurahFullyDownloaded(selectedQari, surah.number, surah.ayahCount)
                            val storedBytes = audioPlayerManager.getSurahAudioBytes(selectedQari, surah.number)
                            val isSurahDownloaded = downloadState is AudioDownloadState.Completed || isFullyDownloaded

                            val isThisSurahActive = currentTrack?.qari == selectedQari && currentTrack?.surahNumber == surah.number
                            val isThisSurahPlaying = isThisSurahActive && (playbackState is PlaybackState.Playing || playbackState is PlaybackState.Buffering)
                            val isThisSurahPaused = isThisSurahActive && playbackState is PlaybackState.Paused

                            SurahAudioDownloadRow(
                                surah = surah,
                                qari = selectedQari,
                                storedBytes = storedBytes,
                                downloadState = downloadState,
                                isPlaying = isThisSurahPlaying,
                                isPaused = isThisSurahPaused,
                                isDownloaded = isSurahDownloaded,
                                onDownloadClick = {
                                    if (!isSurahDownloaded) {
                                        downloadViewModel.download(
                                            qari = selectedQari,
                                            surahNumber = surah.number,
                                            totalAyahs = surah.ayahCount
                                        )
                                    }
                                },
                                onCancelClick = {
                                    downloadViewModel.cancel(selectedQari, surah.number)
                                },
                                onPlayClick = {
                                    if (isThisSurahPlaying) {
                                        audioPlayerManager.pause()
                                    } else if (isThisSurahPaused) {
                                        audioPlayerManager.resume()
                                    } else {
                                        audioPlayerManager.playAyah(
                                            surahNumber = surah.number,
                                            surahName = surah.nameLatin,
                                            ayahNumber = 1,
                                            totalAyahsInSurah = surah.ayahCount,
                                            qari = selectedQari,
                                            autoContinue = true
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                is UiState.Blocked -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Unduhan audio diblokir: ${state.reason}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Daftar surah belum siap.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SafStorageSection(
    safLinked: Boolean,
    onRequestStorage: () -> Unit,
    onRestoreFromSaf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (safLinked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (safLinked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = if (safLinked) Icons.Rounded.FolderSpecial else Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = if (safLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (safLinked) "Folder Aman (SAF) Terhubung" else "Penyimpanan Aman (SAF) Belum Terhubung",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (safLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (safLinked) {
                    IconButton(
                        onClick = onRestoreFromSaf,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Sinkronisasi / Pulihkan dari SAF",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = if (safLinked) {
                    "File audio murottal otomatis tersimpan di folder luar (SAF), aman dari rebuild atau uninstal."
                } else {
                    "Pilih folder SAF agar file audio tersimpan permanen dan tidak terhapus saat uninstal/update aplikasi."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!safLinked) {
                AppPrimaryButton(
                    onClick = onRequestStorage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Pilih Folder SAF Penyimpanan Aman")
                }
            }
        }
    }
}

@Composable
private fun StorageMetricsCard(
    storageBytes: Long,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Column {
                    Text(
                        text = "Total Cache Murottal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${"%.1f".format(storageBytes / (1024.0 * 1024.0))} MB digunakan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedButton(
                onClick = onClearCache,
                enabled = storageBytes > 0L,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hapus Cache", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QariSelectorSection(
    selectedQari: Qari,
    onSelectQari: (Qari) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Pilih Qari Murottal:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Qari.entries.forEach { qari ->
                val isSelected = selectedQari == qari
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectQari(qari) },
                    label = {
                        Text(
                            text = qari.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionBarSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    isBatchDownloading: Boolean,
    onDownloadAll: () -> Unit,
    modifier: Modifier = Modifier,
    isAllDownloaded: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari nomor / nama surah...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Batch Download Button
        AppOutlinedButton(
            onClick = onDownloadAll,
            enabled = !isBatchDownloading && !isAllDownloaded,
            modifier = Modifier.height(44.dp)
        ) {
            Icon(
                imageVector = if (isAllDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isAllDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (isAllDownloaded) "Semua Terunduh (114)" else "Unduh Semua (114)",
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SurahAudioDownloadRow(
    surah: Surah,
    qari: Qari,
    storedBytes: Long,
    downloadState: AudioDownloadState,
    isPlaying: Boolean,
    isPaused: Boolean = false,
    isDownloaded: Boolean = false,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val isBusy = downloadState is AudioDownloadState.Queued ||
        downloadState is AudioDownloadState.Downloading ||
        downloadState is AudioDownloadState.Verifying

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDownloaded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${surah.number}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDownloaded) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = surah.nameLatin,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = surah.nameArabic,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = QuranFontFamily,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Text(
                            text = audioStatusText(
                                state = downloadState,
                                storedBytes = storedBytes,
                                totalAyahs = surah.ayahCount,
                                qari = qari,
                                isPlaying = isPlaying,
                                isPaused = isPaused,
                                isFullyDownloaded = isDownloaded
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isPlaying) MaterialTheme.colorScheme.secondary
                            else if (isDownloaded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.xs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Play Preview Button
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else if (isPaused) "Lanjutkan" else "Putar Murottal",
                            tint = if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Download / Cancel / Status Action
                    if (isBusy) {
                        IconButton(
                            onClick = onCancelClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Batalkan unduhan",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDownloadClick,
                            enabled = !isDownloaded,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Rounded.CheckCircle
                                else if (downloadState is AudioDownloadState.Paused) Icons.Rounded.CloudOff
                                else Icons.Rounded.Download,
                                contentDescription = if (isDownloaded) "Sudah diunduh" else "Unduh audio ${surah.nameLatin}",
                                tint = if (isDownloaded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (downloadState is AudioDownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { downloadState.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun audioStatusText(
    state: AudioDownloadState,
    storedBytes: Long,
    totalAyahs: Int,
    qari: Qari,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    isFullyDownloaded: Boolean = false
): String = when {
    isPlaying -> "Sedang memutar audio ${qari.displayName}..."
    isPaused -> "Murottal dijeda (Ketuk untuk melanjutkan)"
    state is AudioDownloadState.Downloading ->
        "Mengunduh ayat ${state.currentAyah}/$totalAyahs • ${state.progressPercentage}%"
    state is AudioDownloadState.Verifying -> "Memverifikasi ayat ${state.currentAyah}/$totalAyahs..."
    state is AudioDownloadState.Queued -> "Menunggu antrean untuk $totalAyahs ayat..."
    state is AudioDownloadState.Paused -> "Dijeda: ${state.reason}"
    state is AudioDownloadState.Completed || isFullyDownloaded || storedBytes > 0L ->
        "${"%.1f".format(storedBytes / (1024.0 * 1024.0))} MB • Tersimpan di SAF & Offline"
    state is AudioDownloadState.Failed -> "Gagal: ${state.message}"
    else -> "$totalAyahs ayat • Siap diunduh (~${"%.1f".format(totalAyahs * 0.08)} MB)"
}

@Composable
private fun AudioManagerDockedPlayer(
    audioPlayerManager: AudioPlayerManager
) {
    val playbackState by audioPlayerManager.playbackState.collectAsStateWithLifecycle()
    val currentTrack by audioPlayerManager.currentTrack.collectAsStateWithLifecycle()
    val progress by audioPlayerManager.playbackProgress.collectAsStateWithLifecycle()

    if (currentTrack != null && playbackState !is PlaybackState.Idle) {
        val track = currentTrack ?: return
        val isPlaying = playbackState is PlaybackState.Playing
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QS. ${track.surahName} (Ayat ${track.ayahNumber}/${track.totalAyahsInSurah})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Qari: ${track.qari.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        IconButton(onClick = { audioPlayerManager.previousAyah() }) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Ayat Sebelumnya",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { audioPlayerManager.togglePlayPause() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Lanjutkan",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { audioPlayerManager.nextAyah() }) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Ayat Berikutnya",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { audioPlayerManager.stop() }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hentikan Pemutaran",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
