package com.quranplus.app.features.quran.presentation

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Menu
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.quranplus.app.core.utils.alignQuranWords
import com.quranplus.app.features.quran.domain.Ayah
import com.quranplus.app.features.quran.domain.WordByWord
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
    initialAyahNumber: Int,
    viewModel: QuranViewModel,
    preferencesManager: PreferencesManager,
    audioPlayerManager: AudioPlayerManager,
    onBackClick: () -> Unit,
    onNavigateToQuranRoot: () -> Unit,
    onNavigateToAyah: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    val surah by viewModel.currentSurah.collectAsStateWithLifecycle()
    val ayahsState by viewModel.currentAyahsState.collectAsStateWithLifecycle()
    val wordByWordState by viewModel.wordByWordState.collectAsStateWithLifecycle()

    val arabicFontSize by preferencesManager.arabicFontSize.collectAsStateWithLifecycle(initialValue = 28f)
    val showTransliteration by preferencesManager.showTransliteration.collectAsStateWithLifecycle(initialValue = true)
    val showTranslation by preferencesManager.showTranslation.collectAsStateWithLifecycle(initialValue = true)
    val enableTajwid by preferencesManager.enableTajwid.collectAsStateWithLifecycle(initialValue = true)
    val translationMode by preferencesManager.translationMode.collectAsStateWithLifecycle(initialValue = TranslationMode.ENGLISH)
    val view = LocalView.current
    val activity = view.context as? Activity

    var showTajwidSheet by remember { mutableStateOf(false) }
    var showReaderMenu by remember { mutableStateOf(false) }
    var showNavigationSheet by remember { mutableStateOf(false) }
    var showFontSlider by remember { mutableStateOf(false) }
    var isWordByWordMode by remember { mutableStateOf(false) }
    var isImmersiveReader by remember { mutableStateOf(false) }
    var selectedAyahForAction by remember { mutableStateOf<Ayah?>(null) }
    var selectedWaqafRule by remember { mutableStateOf<WaqafParser.WaqafRule?>(null) }
    var selectedTajwidRule by remember { mutableStateOf<TajwidParser.TajwidType?>(null) }

    val hasWordIndonesianTranslation = ((wordByWordState as? UiState.Success<Map<Int, List<WordByWord>>>)
        ?.data
        ?.values
        ?.any { words -> words.any { it.translationId != null } }
        == true)
    val wordTranslationMode = if (
        isWordByWordMode &&
        translationMode == TranslationMode.INDONESIAN &&
        !hasWordIndonesianTranslation
    ) {
        TranslationMode.ENGLISH
    } else {
        translationMode
    }

    val sheetState = rememberModalBottomSheetState()
    val navigationSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    DisposableEffect(activity) {
        val window = activity?.window
        val alreadyKeptOn = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (!alreadyKeptOn) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    LaunchedEffect(activity, isImmersiveReader) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (isImmersiveReader) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(activity) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // HorizontalPager for swipe between surahs
    var restoredPage by rememberSaveable(surahNumber) {
        mutableIntStateOf((surahNumber - 1).coerceIn(0, TOTAL_SURAHS - 1))
    }
    val pagerState = rememberPagerState(
        initialPage = restoredPage,
        pageCount = { TOTAL_SURAHS }
    )

    // Load surah when pager page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            restoredPage = page
            viewModel.loadSurahDetail(page + 1)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = surah?.nameLatin ?: "Memuat surah",
                subtitle = surah?.let { "${it.revelationType.uppercase()} • ${it.ayahCount} Ayat" },
                onBackClick = onBackClick,
                onTitleClick = onNavigateToQuranRoot,
                titleContentDescription = "Beranda Quran",
                actions = {
                    Box {
                        IconButton(onClick = { showReaderMenu = true }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Menu Quran")
                        }
                        DropdownMenu(
                            expanded = showReaderMenu,
                            onDismissRequest = { showReaderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Beranda Quran") },
                                leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showReaderMenu = false
                                    onNavigateToQuranRoot()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isWordByWordMode) "Kembali ke baris ayat" else "Kata per kata") },
                                leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isWordByWordMode = !isWordByWordMode
                                    showReaderMenu = false
                                }
                            )
                            if (!isWordByWordMode) {
                                DropdownMenuItem(
                                    text = { Text("Terjemahan: ${translationMode.label}") },
                                    leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null) },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val next = when (translationMode) {
                                            TranslationMode.INDONESIAN -> TranslationMode.ENGLISH
                                            TranslationMode.ENGLISH -> TranslationMode.BOTH
                                            TranslationMode.BOTH -> TranslationMode.INDONESIAN
                                        }
                                        scope.launch { preferencesManager.setTranslationMode(next) }
                                        showReaderMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Legenda Tajwid") },
                                leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                                onClick = { showTajwidSheet = true; showReaderMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Navigasi halaman dan juz") },
                                leadingIcon = { Icon(Icons.Rounded.AutoStories, contentDescription = null) },
                                onClick = { showNavigationSheet = true; showReaderMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Ukuran font") },
                                leadingIcon = { Icon(Icons.Rounded.FormatSize, contentDescription = null) },
                                onClick = { showFontSlider = !showFontSlider; showReaderMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isImmersiveReader) "Keluar mode imersif" else "Mode imersif") },
                                leadingIcon = {
                                    Icon(
                                        if (isImmersiveReader) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isImmersiveReader = !isImmersiveReader
                                    showReaderMenu = false
                                }
                            )
                        }
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
                                    preferencesManager.setArabicFontSize((arabicFontSize - 2f).coerceIn(18f, 48f))
                                }
                            }, modifier = Modifier.semantics {
                                contentDescription = "Perkecil ukuran font Arab"
                            }) {
                                Text("A-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    preferencesManager.setArabicFontSize((arabicFontSize + 2f).coerceIn(18f, 48f))
                                }
                            }, modifier = Modifier.semantics {
                                contentDescription = "Perbesar ukuran font Arab"
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
                val pageListState = rememberSaveable(
                    page,
                    saver = LazyListState.Saver
                ) { LazyListState() }

                LaunchedEffect(page, pageListState, ayahsState, initialAyahNumber) {
                    if (page != pagerState.currentPage || ayahsState !is UiState.Success) return@LaunchedEffect
                    val data = (ayahsState as UiState.Success<List<Ayah>>).data
                    if (page + 1 == surahNumber && initialAyahNumber in 1..data.size) {
                        val headerOffset = if (page + 1 != 9) 1 else 0
                        pageListState.animateScrollToItem(headerOffset + initialAyahNumber - 1)
                    }
                }

                LaunchedEffect(page, pageListState, ayahsState) {
                    snapshotFlow { pageListState.firstVisibleItemIndex }.collect { firstVisibleItemIndex ->
                        if (page != pagerState.currentPage) return@collect
                        val currentSurah = surah ?: return@collect
                        if (currentSurah.number != page + 1) return@collect
                        val state = ayahsState as? UiState.Success ?: return@collect
                        val headerOffset = if (page + 1 != 9) 1 else 0
                        val ayahIndex = (firstVisibleItemIndex - headerOffset).coerceAtLeast(0)
                        state.data.getOrNull(ayahIndex)?.let { ayah ->
                            viewModel.onAyahVisible(
                                surahNumber = page + 1,
                                surahName = currentSurah.nameLatin,
                                ayahNumber = ayah.ayahNumber,
                                juz = ayah.juz,
                                page = ayah.page
                            )
                        }
                    }
                }

                // Show content only for adjacent pages
                if (abs(page - pagerState.currentPage) <= 1 && surah?.number == page + 1) {
                    when (val state = ayahsState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Success -> {
                            if (page + 1 == surahNumber && initialAyahNumber > state.data.size) {
                                AppEmptyState(
                                    icon = Icons.Rounded.FormatSize,
                                    title = "Ayat tidak ditemukan",
                                    description = "Rute meminta ayat $initialAyahNumber, tetapi surah ini hanya memiliki ${state.data.size} ayat."
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    LazyColumn(
                                        state = pageListState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 840.dp),
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
                                                translationMode = wordTranslationMode,
                                                isWordByWordMode = isWordByWordMode,
                                                wordByWord = (wordByWordState as? UiState.Success<Map<Int, List<WordByWord>>>)
                                                    ?.data?.get(ayah.ayahNumber).orEmpty(),
                                                onAyahClick = {
                                                    selectedAyahForAction = ayah
                                                },
                                                onBookmarkClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    surah?.nameLatin?.let { name ->
                                                        viewModel.toggleBookmark(ayah, name)
                                                    }
                                                },
                                                onWaqafClick = { rule ->
                                                    selectedWaqafRule = rule
                                                },
                                                onTajwidClick = { rule ->
                                                    selectedTajwidRule = rule
                                                }
                                            )
                                        }
                                    }
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
                        is UiState.Empty -> {
                            AppEmptyState(
                                icon = Icons.Rounded.FormatSize,
                                title = "Ayat Belum Tersedia",
                                description = "Database belum memiliki ayat untuk surah ini."
                            )
                        }
                        is UiState.Blocked -> {
                            AppEmptyState(
                                icon = Icons.Rounded.FormatSize,
                                title = "Reader Diblokir",
                                description = state.reason
                            )
                        }
                        is UiState.Idle -> {
                            AppEmptyState(
                                icon = Icons.Rounded.FormatSize,
                                title = "Reader Belum Siap",
                                description = "Posisi bacaan belum dapat dimuat dari database."
                            )
                        }
                    }
                } else {
                    when (val state = ayahsState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Error -> AppEmptyState(
                            icon = Icons.Rounded.FormatSize,
                            title = "Gagal Memuat Reader",
                            description = state.message
                        )
                        is UiState.Empty -> AppEmptyState(
                            icon = Icons.Rounded.FormatSize,
                            title = "Ayat Belum Tersedia",
                            description = "Database belum memiliki ayat untuk target ini."
                        )
                        is UiState.Blocked -> AppEmptyState(
                            icon = Icons.Rounded.FormatSize,
                            title = "Reader Diblokir",
                            description = state.reason
                        )
                        is UiState.Idle -> AppEmptyState(
                            icon = Icons.Rounded.FormatSize,
                            title = "Reader Belum Siap",
                            description = "Surah belum dapat dipulihkan dari database."
                        )
                        is UiState.Success -> AppEmptyState(
                            icon = Icons.Rounded.FormatSize,
                            title = "Surah Tidak Tersedia",
                            description = "Target surah tidak cocok dengan data yang dimuat."
                        )
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

        if (showNavigationSheet) {
            ReaderNavigationSheet(
                sheetState = navigationSheetState,
                viewModel = viewModel,
                onDismissRequest = { showNavigationSheet = false },
                onNavigateToAyah = onNavigateToAyah
            )
        }

        // Ayah Action Bottom Sheet
        selectedAyahForAction?.let { ayah ->
            val currentSurah = surah
            if (currentSurah != null) {
            AyahActionBottomSheet(
                ayah = ayah,
                surahName = currentSurah.nameLatin,
                totalAyahsInSurah = currentSurah.ayahCount,
                sheetState = sheetState,
                audioPlayerManager = audioPlayerManager,
                onDismissRequest = { selectedAyahForAction = null },
                onBookmarkToggle = { note ->
                    viewModel.toggleBookmark(ayah, currentSurah.nameLatin, note)
                }
            )
            }
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

        selectedTajwidRule?.let { rule ->
            AlertDialog(
                onDismissRequest = { selectedTajwidRule = null },
                title = { Text(rule.label, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(rule.description, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Durasi: ${rule.harakatDuration}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = rule.ruleExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTajwidRule = null }) {
                        Text("Mengerti")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderNavigationSheet(
    sheetState: androidx.compose.material3.SheetState,
    viewModel: QuranViewModel,
    onDismissRequest: () -> Unit,
    onNavigateToAyah: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    var pageInput by remember { mutableStateOf("") }
    var juzInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun navigateTo(target: Ayah?) {
        if (target == null) {
            errorMessage = "Posisi tidak tersedia di database Quran."
            return
        }
        onDismissRequest()
        onNavigateToAyah(target.surahNumber, target.ayahNumber)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = "Navigasi Quran",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Buka awal halaman atau juz dari indeks database yang terverifikasi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it.filter(Char::isDigit); errorMessage = null },
                label = { Text("Halaman (1–604)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = {
                    val page = pageInput.toIntOrNull()?.takeIf { it in 1..604 }
                    if (page == null) {
                        errorMessage = "Masukkan halaman antara 1 dan 604."
                    } else {
                        viewModel.findFirstAyahByPage(page, ::navigateTo)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buka halaman")
            }
            OutlinedTextField(
                value = juzInput,
                onValueChange = { juzInput = it.filter(Char::isDigit); errorMessage = null },
                label = { Text("Juz (1–30)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = {
                    val juz = juzInput.toIntOrNull()?.takeIf { it in 1..30 }
                    if (juz == null) {
                        errorMessage = "Masukkan juz antara 1 dan 30."
                    } else {
                        viewModel.findFirstAyahByJuz(juz, ::navigateTo)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buka juz")
            }
            errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
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
    onAyahClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onWaqafClick: (WaqafParser.WaqafRule) -> Unit,
    onTajwidClick: (TajwidParser.TajwidType) -> Unit,
    modifier: Modifier = Modifier,
    translationMode: TranslationMode = TranslationMode.ENGLISH,
    isWordByWordMode: Boolean = false,
    wordByWord: List<WordByWord> = emptyList()
) {
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (ayah.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (ayah.isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onAyahClick,
                    modifier = Modifier.size(48.dp)
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
        val baseTextColor = MaterialTheme.colorScheme.onSurface
        val fullArabic = WaqafParser.formatAyahTextWithEndMarker(
            ayahText = ayah.textArabic,
            ayahNumber = ayah.ayahNumber
        )
        val annotatedArabic = remember(fullArabic, ayah.tajwidTags, enableTajwid, baseTextColor) {
            WaqafParser.annotateWaqafMarkers(
                TajwidParser.buildColoredAyahText(
                    arabicText = fullArabic,
                    tajwidTags = ayah.tajwidTags,
                    enableTajwid = enableTajwid,
                    baseTextColor = baseTextColor
                )
            )
        }
        if (isWordByWordMode) {
            if (wordByWord.isEmpty()) {
                Text(
                    text = "Data kata per kata belum tersedia untuk ayat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                WordByWordAyah(
                    words = wordByWord,
                    ayahNumber = ayah.ayahNumber,
                    annotatedAyah = annotatedArabic,
                    fontSizeSp = fontSizeSp,
                    showTransliteration = showTransliteration,
                    showTranslation = showTranslation,
                    translationMode = translationMode,
                    onWaqafClick = onWaqafClick,
                    onTajwidClick = onTajwidClick
                )
            }
        } else {
            // Keep the ayah number in a dedicated marker badge. U+06DD is
            // retained in the annotated source for alignment, but its digit
            // shaping is not reliable across Android font fallbacks.
            val displayArabic = remember(annotatedArabic) {
                WaqafParser.removeAyahEndMarker(annotatedArabic)
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AyahEndMarker(
                        ayahNumber = ayah.ayahNumber,
                        fontSizeSp = fontSizeSp,
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                    ClickableText(
                        text = displayArabic,
                        style = getQuranArabicStyle(fontSizeSp).copy(
                            textAlign = TextAlign.End,
                            lineHeight = (fontSizeSp * 1.8f).sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Teks Arab ayat ${ayah.ayahNumber}. Ketuk tanda atau warna Tajwid untuk detail."
                            },
                        onClick = { offset ->
                            if (displayArabic.text.isEmpty()) return@ClickableText
                            val safeOffset = offset.coerceIn(0, displayArabic.text.lastIndex)
                            displayArabic
                                .getStringAnnotations(
                                    WaqafParser.WAQAF_ANNOTATION,
                                    safeOffset,
                                    safeOffset + 1
                                )
                                .firstOrNull()
                                ?.let { annotation ->
                                    WaqafParser.findRuleBySymbol(annotation.item.first())?.let(onWaqafClick)
                                    return@ClickableText
                                }
                            displayArabic
                                .getStringAnnotations(
                                    TajwidParser.TAJWID_ANNOTATION,
                                    safeOffset,
                                    safeOffset + 1
                                )
                                .firstOrNull()
                                ?.let { annotation ->
                                    runCatching { TajwidParser.TajwidType.valueOf(annotation.item) }
                                        .getOrNull()
                                        ?.let(onTajwidClick)
                                }
                        }
                    )
                }
            }
        }

        if (!isWordByWordMode) {
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
                        Spacer(modifier = Modifier.height(Spacing.xs))
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
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

}

internal data class WordRenderSlice(
    val word: WordByWord,
    val text: AnnotatedString
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordByWordAyah(
    words: List<WordByWord>,
    ayahNumber: Int,
    annotatedAyah: AnnotatedString,
    fontSizeSp: Float,
    showTransliteration: Boolean,
    showTranslation: Boolean,
    translationMode: TranslationMode,
    onWaqafClick: (WaqafParser.WaqafRule) -> Unit,
    onTajwidClick: (TajwidParser.TajwidType) -> Unit
) {
    var selectedWordIndex by remember(words) { mutableIntStateOf(-1) }
    val slices = remember(words, annotatedAyah) {
        buildWordRenderSlices(words, annotatedAyah)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (slices == null) {
            Text(
                text = "WORD_ALIGNMENT_UNAVAILABLE: data kata tidak sejajar dengan teks Uthmani sumber.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                slices.forEach { slice ->
                val word = slice.word
                val selected = word.wordIndex == selectedWordIndex
                val transliteration = word.transliteration?.takeIf(String::isNotBlank)
                val translations = if (showTranslation) {
                    wordTranslations(word, translationMode)
                } else {
                    emptyList()
                }
                val wordDetails = buildList {
                    add("Kata ${word.wordIndex}")
                    add("Arab: ${word.textArabic}")
                    transliteration?.let { add("Transliterasi: $it") }
                    translations.forEach { (label, value) -> add("$label: $value") }
                }.joinToString(". ")
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .widthIn(max = 180.dp)
                        .semantics {
                            contentDescription = wordDetails
                            this.selected = selected
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ClickableText(
                            text = slice.text,
                            style = getQuranArabicStyle(fontSizeSp).copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { offset ->
                                selectedWordIndex = word.wordIndex
                                slice.text
                                    .getStringAnnotations(WaqafParser.WAQAF_ANNOTATION, offset, offset + 1)
                                    .firstOrNull()
                                    ?.let { annotation ->
                                        WaqafParser.findRuleBySymbol(annotation.item.first())?.let(onWaqafClick)
                                    }
                                slice.text
                                    .getStringAnnotations(TajwidParser.TAJWID_ANNOTATION, offset, offset + 1)
                                    .firstOrNull()
                                    ?.let { annotation ->
                                        runCatching { TajwidParser.TajwidType.valueOf(annotation.item) }
                                            .getOrNull()
                                            ?.let(onTajwidClick)
                                }
                            }
                        )
                        if (showTransliteration) {
                            if (transliteration != null) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = transliteration,
                                        style = TransliterationStyle,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else if (selected) {
                                Text(
                                    text = "TRANSLITERATION_UNAVAILABLE: sumber per kata belum tersedia",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        if (showTranslation) {
                            if (translations.isEmpty() && selected) {
                                Text(
                                    text = "TRANSLATION_${translationMode.name}_UNAVAILABLE: sumber belum tersedia",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                translations.forEach { (label, value) ->
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = "$label: $value",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AyahEndMarker(
                        ayahNumber = ayahNumber,
                        fontSizeSp = fontSizeSp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            }
        }
    }
}

@Composable
private fun AyahEndMarker(
    ayahNumber: Int,
    fontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val markerSize = (fontSizeSp * 1.7f).coerceIn(48f, 72f).dp
    val numberSize = (fontSizeSp * 0.52f).coerceIn(14f, 22f)
    Box(
        modifier = modifier
            .size(markerSize)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape
            )
            .semantics {
                contentDescription =
                    "Penanda akhir ayat ${WaqafParser.toArabicDigits(ayahNumber)}. Boleh berhenti."
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = WaqafParser.toArabicDigits(ayahNumber),
            style = getQuranArabicStyle(numberSize).copy(
                textAlign = TextAlign.Center,
                lineHeight = numberSize.sp
            ),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun wordTranslations(
    word: WordByWord,
    mode: TranslationMode
): List<Pair<String, String>> = when (mode) {
    TranslationMode.INDONESIAN -> word.translationId?.let { listOf("Indonesia" to it) }
        ?: word.translationEn?.let { listOf("English (source)" to it) }
        .orEmpty()
    TranslationMode.ENGLISH -> word.translationEn?.let { listOf("English" to it) }.orEmpty()
    TranslationMode.BOTH -> buildList {
        word.translationId?.let { add("Indonesia" to it) }
        word.translationEn?.let { add("English (source)" to it) }
    }
}

internal fun buildWordRenderSlices(
    words: List<WordByWord>,
    annotatedAyah: AnnotatedString
): List<WordRenderSlice>? {
    val ranges = alignQuranWords(
        displayText = annotatedAyah.text,
        sourceWords = words.map { it.wordIndex to it.textArabic }
    ) ?: return null
    val ayahEndMarkerStart = findAyahEndMarkerStart(annotatedAyah)

    return words.mapIndexed { index, word ->
        val start = ranges[index].start
        val end = ranges.getOrNull(index + 1)?.start
            ?: ayahEndMarkerStart?.let { markerStart ->
                annotatedAyah.text
                    .substring(0, markerStart)
                    .trimEnd()
                    .length
            }
            ?: annotatedAyah.text.length
        WordRenderSlice(word, annotatedAyah.subSequence(start, end))
    }
}

internal fun extractAyahEndMarker(annotatedAyah: AnnotatedString): AnnotatedString? {
    val markerStart = findAyahEndMarkerStart(annotatedAyah) ?: return null
    val markerEnd = annotatedAyah.text.trimEnd().length
    return annotatedAyah.subSequence(markerStart, markerEnd)
}

private fun findAyahEndMarkerStart(annotatedAyah: AnnotatedString): Int? {
    val annotatedStart = annotatedAyah
        .getStringAnnotations(
            WaqafParser.AYAH_END_ANNOTATION,
            0,
            annotatedAyah.length
        )
        .firstOrNull()
        ?.start
    if (annotatedStart != null) return annotatedStart

    return annotatedAyah.text
        .indexOf(WaqafParser.AYAH_END_SYM)
        .takeIf { it >= 0 }
}

@Composable
fun DockedMiniPlayerBar(
    audioPlayerManager: AudioPlayerManager
) {
    val playbackState by audioPlayerManager.playbackState.collectAsStateWithLifecycle()
    val currentTrack by audioPlayerManager.currentTrack.collectAsStateWithLifecycle()
    val progress by audioPlayerManager.playbackProgress.collectAsStateWithLifecycle()
    val speed by audioPlayerManager.playbackSpeed.collectAsStateWithLifecycle()

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
                                .size(48.dp)
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
