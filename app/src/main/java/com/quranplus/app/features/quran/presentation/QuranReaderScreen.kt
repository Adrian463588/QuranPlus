package com.quranplus.app.features.quran.presentation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.core.audio.PlaybackState
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.components.TajwidLegendSheet
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.TransliterationStyle
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.core.utils.TajwidParser
import com.quranplus.app.core.utils.WaqafParser
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.presentation.components.AyahActionBottomSheet
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.app.features.settings.data.TranslationMode
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TOTAL_SURAHS = 114

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    initialAyahNumber: Int = 1,
    viewModel: QuranViewModel,
    preferencesManager: PreferencesManager,
    audioPlayerManager: AudioPlayerManager,
    onBackClick: () -> Unit
) {
    val surah by viewModel.currentSurah.collectAsState()
    val ayahsState by viewModel.currentAyahsState.collectAsState()

    val arabicFontSize by preferencesManager.arabicFontSize.collectAsState(initial = 28f)
    val showTransliteration by preferencesManager.showTransliteration.collectAsState(initial = true)
    val showTranslation by preferencesManager.showTranslation.collectAsState(initial = true)
    val enableTajwid by preferencesManager.enableTajwid.collectAsState(initial = true)
    val translationMode by preferencesManager.translationMode.collectAsState(initial = TranslationMode.INDONESIAN)

    var showTajwidSheet by remember { mutableStateOf(false) }
    var showFontSlider by remember { mutableStateOf(false) }
    var isWordByWordMode by remember { mutableStateOf(false) }
    var selectedAyahForAction by remember { mutableStateOf<Ayah?>(null) }
    var selectedWaqafRule by remember { mutableStateOf<WaqafParser.WaqafRule?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // HorizontalPager for swipe between surahs
    val pagerState = rememberPagerState(
        initialPage = surahNumber - 1,
        pageCount = { TOTAL_SURAHS }
    )

    // Load surah when pager page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.loadSurahDetail(page + 1)
        }
    }

    // Scroll to initial ayah when ayahs load
    LaunchedEffect(ayahsState, initialAyahNumber) {
        if (ayahsState is UiState.Success && initialAyahNumber > 1) {
            val currentSurahNum = pagerState.currentPage + 1
            val targetIndex = (initialAyahNumber - 1 + if (currentSurahNum != 9) 1 else 0).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Auto-update last read position as user scrolls
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex) {
        val currentSurah = surah
        val currentSurahNum = pagerState.currentPage + 1
        if (currentSurah != null && firstVisibleItemIndex >= 0) {
            val ayahNum = firstVisibleItemIndex + 1
            viewModel.onAyahVisible(currentSurahNum, currentSurah.nameLatin, ayahNum)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = surah?.nameLatin ?: "Surah ${pagerState.currentPage + 1}",
                subtitle = surah?.let { "${it.revelationType.uppercase()} • ${it.ayahCount} Ayat" },
                onBackClick = onBackClick,
                actions = {
                    // Word-by-word mode toggle
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isWordByWordMode = !isWordByWordMode
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.TextFields,
                            contentDescription = "Mode Kata Demi Kata",
                            tint = if (isWordByWordMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Translation mode toggle
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val next = when (translationMode) {
                            TranslationMode.INDONESIAN -> TranslationMode.ENGLISH
                            TranslationMode.ENGLISH    -> TranslationMode.BOTH
                            TranslationMode.BOTH       -> TranslationMode.INDONESIAN
                        }
                        scope.launch { preferencesManager.setTranslationMode(next) }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = "Terjemahan: ${translationMode.label}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showTajwidSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Panduan Tajwid"
                        )
                    }
                    IconButton(onClick = { showFontSlider = !showFontSlider }) {
                        Icon(
                            imageVector = Icons.Rounded.FormatSize,
                            contentDescription = "Ukuran Font"
                        )
                    }
                }
            )
        },
        bottomBar = {
            DockedMiniPlayerBar(audioPlayerManager = audioPlayerManager)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Banner
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Terjemahan: ${translationMode.label}  •  ${if (isWordByWordMode) "Kata per Kata ON" else "Baris Ayat"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Font Scale Quick Control Bar
            AnimatedVisibility(visible = showFontSlider) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ukuran Font Arab: ${arabicFontSize.toInt()}sp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    preferencesManager.setArabicFontSize((arabicFontSize - 2f).coerceIn(20f, 40f))
                                }
                            }) {
                                Text("A-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    preferencesManager.setArabicFontSize((arabicFontSize + 2f).coerceIn(20f, 40f))
                                }
                            }) {
                                Text("A+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // HorizontalPager for swipe between surahs
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // Show content only for adjacent pages
                if (abs(page - pagerState.currentPage) <= 1) {
                    when (val state = ayahsState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Success -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = Spacing.xxl)
                            ) {
                                // Bismillah Header (except At-Tawbah)
                                if (page + 1 != 9) {
                                    item { BismillahHeader() }
                                }
                                items(
                                    items = state.data,
                                    key = { "${it.surahNumber}_${it.ayahNumber}" }
                                ) { ayah ->
                                    AyahReaderItem(
                                        ayah = ayah,
                                        fontSizeSp = arabicFontSize,
                                        enableTajwid = enableTajwid,
                                        showTransliteration = showTransliteration,
                                        showTranslation = showTranslation,
                                        translationMode = translationMode,
                                        isWordByWordMode = isWordByWordMode,
                                        onAyahClick = {
                                            selectedAyahForAction = ayah
                                        },
                                        onBookmarkClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleBookmark(ayah, surah?.nameLatin ?: "Surah ${page + 1}")
                                        },
                                        onWaqafClick = { rule ->
                                            selectedWaqafRule = rule
                                        }
                                    )
                                }
                            }
                        }
                        is UiState.Error -> {
                            AppEmptyState(
                                icon = Icons.Rounded.FormatSize,
                                title = "Gagal Memuat Ayat",
                                description = state.message
                            )
                        }
                        else -> {}
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Tajwid Legend Bottom Sheet
        if (showTajwidSheet) {
            TajwidLegendSheet(
                sheetState = sheetState,
                onDismissRequest = { showTajwidSheet = false }
            )
        }

        // Ayah Action Bottom Sheet
        selectedAyahForAction?.let { ayah ->
            AyahActionBottomSheet(
                ayah = ayah,
                surahName = surah?.nameLatin ?: "Surah ${surahNumber}",
                totalAyahsInSurah = surah?.ayahCount ?: 100,
                sheetState = sheetState,
                audioPlayerManager = audioPlayerManager,
                onDismissRequest = { selectedAyahForAction = null },
                onBookmarkToggle = { note ->
                    viewModel.toggleBookmark(ayah, surah?.nameLatin ?: "Surah ${surahNumber}", note)
                }
            )
        }

        // Waqaf Rule Information Dialog
        selectedWaqafRule?.let { rule ->
            AlertDialog(
                onDismissRequest = { selectedWaqafRule = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rule.symbol, style = getQuranArabicStyle(20f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(rule.latinName, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(text = "Rekomendasi: ${rule.actionCategory.label}", fontWeight = FontWeight.Bold, color = rule.badgeColor)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(text = rule.detailedRule, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedWaqafRule = null }) {
                        Text("Mengerti")
                    }
                }
            )
        }
    }
}

@Composable
fun BismillahHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg, horizontal = Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            style = getQuranArabicStyle(26f),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AyahReaderItem(
    ayah: Ayah,
    fontSizeSp: Float,
    enableTajwid: Boolean,
    showTransliteration: Boolean,
    showTranslation: Boolean,
    translationMode: TranslationMode = TranslationMode.INDONESIAN,
    isWordByWordMode: Boolean = false,
    onAyahClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onWaqafClick: (WaqafParser.WaqafRule) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWordDetail by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAyahClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md)
    ) {
        // Header (Number Badge + Quick Action Icons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (ayah.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (ayah.isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onAyahClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opsi Ayat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Arabic Text / Word-by-Word View
        if (isWordByWordMode) {
            // Word-by-word chip layout
            val words = remember(ayah.textArabic) { ayah.textArabic.split(" ").filter { it.isNotBlank() } }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                words.forEachIndexed { wIdx, wordText ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(2.dp)
                            .clickable {
                                selectedWordDetail = wordText
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = TajwidParser.buildColoredAyahText(wordText, enableTajwid = enableTajwid),
                                style = getQuranArabicStyle(fontSizeSp * 0.85f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kata ${wIdx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Standard Line-by-Line Uthmani Text with End of Ayah glyph
            val baseTextColor = MaterialTheme.colorScheme.onSurface
            val ayahEndMarker = WaqafParser.formatAyahEndMarker(ayah.ayahNumber)
            val fullArabic = ayah.textArabic + ayahEndMarker

            val coloredArabic = remember(fullArabic, ayah.tajwidTags, enableTajwid, baseTextColor) {
                TajwidParser.buildColoredAyahText(
                    arabicText = fullArabic,
                    tajwidTags = ayah.tajwidTags,
                    enableTajwid = enableTajwid,
                    baseTextColor = baseTextColor
                )
            }
            Text(
                text = coloredArabic,
                style = getQuranArabicStyle(fontSizeSp),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                lineHeight = (fontSizeSp * 1.8f).sp
            )
        }

        // Latin Transliteration
        if (showTransliteration && ayah.transliteration.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = ayah.transliteration,
                style = TransliterationStyle,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Translation(s)
        if (showTranslation) {
            when (translationMode) {
                TranslationMode.INDONESIAN -> {
                    if (ayah.translationId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = ayah.translationId,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 22.sp
                        )
                    }
                }
                TranslationMode.ENGLISH -> {
                    if (ayah.translationEn.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = ayah.translationEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 22.sp
                        )
                    }
                }
                TranslationMode.BOTH -> {
                    if (ayah.translationId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = ayah.translationId,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 22.sp
                        )
                    }
                    if (ayah.translationEn.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ayah.translationEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    // Word Detail Dialog
    selectedWordDetail?.let { word ->
        AlertDialog(
            onDismissRequest = { selectedWordDetail = null },
            title = { Text("Detail Kata Arab") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = word, style = getQuranArabicStyle(32f), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(text = "Lafaz Al-Qur'an Utsmani", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWordDetail = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun DockedMiniPlayerBar(
    audioPlayerManager: AudioPlayerManager
) {
    val playbackState by audioPlayerManager.playbackState.collectAsState()
    val currentTrack by audioPlayerManager.currentTrack.collectAsState()
    val progress by audioPlayerManager.playbackProgress.collectAsState()
    val speed by audioPlayerManager.playbackSpeed.collectAsState()

    if (currentTrack != null && playbackState !is PlaybackState.Idle) {
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
                            text = "QS. ${currentTrack?.surahName} : Ayat ${currentTrack?.ayahNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentTrack?.qari?.displayName?.split(" ")?.first()} • ${speed}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val nextSpeed = when (speed) {
                                0.75f -> 1.0f
                                1.0f -> 1.25f
                                1.25f -> 0.5f
                                else -> 0.75f
                            }
                            audioPlayerManager.setPlaybackSpeed(nextSpeed)
                        }) {
                            Icon(imageVector = Icons.Rounded.Speed, contentDescription = "Kecepatan Audio", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(onClick = { audioPlayerManager.previousAyah() }) {
                            Icon(imageVector = Icons.Rounded.SkipPrevious, contentDescription = "Ayat Sebelumnya")
                        }

                        IconButton(
                            onClick = { audioPlayerManager.togglePlayPause() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (playbackState is PlaybackState.Playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        IconButton(onClick = { audioPlayerManager.nextAyah() }) {
                            Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = "Ayat Berikutnya")
                        }
                    }
                }
            }
        }
    }
}
