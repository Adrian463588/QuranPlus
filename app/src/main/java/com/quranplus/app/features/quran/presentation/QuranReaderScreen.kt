package com.quranplus.app.features.quran.presentation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.components.TajwidLegendSheet
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.TransliterationStyle
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.core.utils.TajwidParser
import com.quranplus.app.features.quran.domain.Ayah
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Translation mode badge
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
                        text = "Terjemahan: ${translationMode.label}  •  Geser untuk pindah surah",
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
                // Show content only for current page (performance)
                if (kotlin.math.abs(page - pagerState.currentPage) <= 1) {
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
                                        onBookmarkClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleBookmark(ayah, surah?.nameLatin ?: "Surah ${page + 1}")
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

        if (showTajwidSheet) {
            TajwidLegendSheet(
                sheetState = sheetState,
                onDismissRequest = { showTajwidSheet = false }
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

@Composable
fun AyahReaderItem(
    ayah: Ayah,
    fontSizeSp: Float,
    enableTajwid: Boolean,
    showTransliteration: Boolean,
    showTranslation: Boolean,
    translationMode: TranslationMode = TranslationMode.INDONESIAN,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.md)
    ) {
        // Ayah Metadata Header (Number Badge + Bookmark Button)
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
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Arabic Text with Tajwid Coloring (Character-Level Precision)
        val baseTextColor = MaterialTheme.colorScheme.onSurface
        val coloredArabic = remember(ayah.textArabic, ayah.tajwidTags, enableTajwid, baseTextColor) {
            TajwidParser.buildColoredAyahText(
                arabicText = ayah.textArabic,
                tajwidTags = ayah.tajwidTags,
                enableTajwid = enableTajwid,
                baseTextColor = baseTextColor
            )
        }
        Text(
            text = coloredArabic,
            style = getQuranArabicStyle(fontSizeSp),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )


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

        // Translation(s) based on selected mode
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
}
