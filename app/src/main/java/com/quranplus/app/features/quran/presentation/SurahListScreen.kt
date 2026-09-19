package com.quranplus.app.features.quran.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.components.TajwidLegendSheet
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.quran.domain.Bookmark
import com.quranplus.app.features.quran.domain.BookmarkSort
import com.quranplus.app.features.quran.domain.JuzCatalog
import com.quranplus.app.features.quran.domain.JuzInfo
import com.quranplus.app.features.quran.domain.LastRead
import com.quranplus.app.features.quran.domain.Surah

enum class QuranListTab(val title: String) {
    SURAH("SURAH"),
    JUZ("JUZ"),
    BOOKMARK("BOOKMARK")
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    viewModel: QuranViewModel,
    onSurahClick: (Int, Int) -> Unit,
    onSearchClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val surahState by viewModel.surahListState.collectAsStateWithLifecycle()
    val lastRead by viewModel.lastReadState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(QuranListTab.SURAH) }
    var showTajwidSheet by remember { mutableStateOf(false) }
    var showQuranMenu by remember { mutableStateOf(false) }
    var selectedSurahNumber by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Al-Qur'an Al-Karim",
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Cari ayat"
                        )
                    }
                    IconButton(onClick = { showQuranMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Menu Al-Qur'an"
                        )
                    }
                    DropdownMenu(
                        expanded = showQuranMenu,
                        onDismissRequest = { showQuranMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cari ayat") },
                            onClick = {
                                showQuranMenu = false
                                onSearchClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Panduan Tajwid") },
                            onClick = {
                                showQuranMenu = false
                                showTajwidSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pengaturan") },
                            onClick = {
                                showQuranMenu = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tabs: SURAH | JUZ | BOOKMARK
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
            ) {
                QuranListTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Quick Continue / Last Read Card (visible on SURAH and JUZ tabs)
            if (lastRead != null && selectedTab != QuranListTab.BOOKMARK) {
                Box(modifier = Modifier.widthIn(max = 840.dp)) {
                    LastReadBanner(
                        lastRead = lastRead!!,
                        onClick = {
                            onSurahClick(lastRead!!.surahNumber, lastRead!!.ayahNumber)
                        }
                    )
                }
            }

            when (selectedTab) {
                QuranListTab.SURAH -> {
                    when (val state = surahState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Success -> {
                            SurahContent(
                                surahs = state.data,
                                widthSizeClass = widthSizeClass,
                                selectedSurahNumber = selectedSurahNumber,
                                onSelectSurah = { selectedSurahNumber = it.number },
                                onOpenSurah = { onSurahClick(it, 1) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                        is UiState.Error -> {
                            AppEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                title = "Gagal Memuat Surah",
                                description = state.message
                            )
                        }
                        UiState.Empty -> {
                            AppEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                title = "Data Surah Kosong",
                                description = "Database Al-Qur'an belum memiliki data yang dapat dibaca."
                            )
                        }
                        is UiState.Blocked -> {
                            AppEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                title = "Surah Tidak Tersedia",
                                description = state.reason
                            )
                        }
                        UiState.Idle -> {
                            AppEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                title = "Memuat Surah",
                                description = "Data Al-Qur'an sedang disiapkan."
                            )
                        }
                    }
                }
                QuranListTab.JUZ -> {
                    JuzListPane(
                        juzList = JuzCatalog.ALL_JUZ,
                        onJuzClick = { juz ->
                            onSurahClick(juz.startSurahNumber, juz.startAyahNumber)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                QuranListTab.BOOKMARK -> {
                    BookmarksListPane(
                        viewModel = viewModel,
                        onBookmarkClick = onSurahClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
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
private fun JuzListPane(
    juzList: List<JuzInfo>,
    onJuzClick: (JuzInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            items = juzList,
            key = { it.number }
        ) { juz ->
            JuzItemRow(
                juz = juz,
                onClick = { onJuzClick(juz) }
            )
        }
    }
}

@Composable
fun RubElHizbBadge(
    number: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizePx = size.minDimension
            val radius = sizePx / 2f
            val center = Offset(sizePx / 2f, sizePx / 2f)

            val path = Path()
            val points = 8
            val outerRadius = radius * 0.92f
            val innerRadius = radius * 0.68f

            for (i in 0 until (points * 2)) {
                val angle = (i * Math.PI / points) - (Math.PI / 2)
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val x = center.x + (r * Math.cos(angle)).toFloat()
                val y = center.y + (r * Math.sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                color = color.copy(alpha = 0.12f)
            )
            drawPath(
                path = path,
                color = color.copy(alpha = 0.8f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun JuzItemRow(
    juz: JuzInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 8-Pointed Star (Rub-el-Hizb) Badge
            RubElHizbBadge(number = juz.number)

            Spacer(modifier = Modifier.width(Spacing.md))

            // Juz information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Juz ${juz.number}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = juz.startDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Arabic Calligraphy start title
            Text(
                text = juz.nameArabic,
                style = getQuranArabicStyle(22f),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 140.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BookmarksListPane(
    viewModel: QuranViewModel,
    onBookmarkClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val bookmarkSort by viewModel.bookmarkSort.collectAsStateWithLifecycle()
    var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var noteInput by remember { mutableStateOf("") }

    when (val state = bookmarksState) {
        is UiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                AppEmptyState(
                    modifier = modifier.fillMaxSize(),
                    icon = Icons.Rounded.BookmarkBorder,
                    title = "Belum Ada Bookmark",
                    description = "Simpan ayat-ayat Al-Qur'an penting saat tilawah untuk membacanya kembali di sini."
                )
            } else {
                Column(modifier = modifier.fillMaxSize().widthIn(max = 840.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        FilterChip(
                            selected = bookmarkSort == BookmarkSort.NEWEST,
                            onClick = { viewModel.setBookmarkSort(BookmarkSort.NEWEST) },
                            label = { Text("Terbaru") }
                        )
                        FilterChip(
                            selected = bookmarkSort == BookmarkSort.SURAH,
                            onClick = { viewModel.setBookmarkSort(BookmarkSort.SURAH) },
                            label = { Text("Urut Surah") }
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(state.data, key = { it.id }) { bookmark ->
                            BookmarkItemCard(
                                bookmark = bookmark,
                                onClick = { onBookmarkClick(bookmark.surahNumber, bookmark.ayahNumber) },
                                onDeleteClick = { viewModel.deleteBookmark(bookmark) },
                                onNoteClick = {
                                    noteInput = bookmark.note.orEmpty()
                                    editingBookmark = bookmark
                                }
                            )
                        }
                    }
                }
            }
        }
        else -> {
            AppEmptyState(
                modifier = modifier.fillMaxSize(),
                icon = Icons.Rounded.BookmarkBorder,
                title = "Belum Ada Bookmark",
                description = "Simpan ayat-ayat Al-Qur'an penting saat tilawah untuk membacanya kembali di sini."
            )
        }
    }

    editingBookmark?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { editingBookmark = null },
            title = { Text("Catatan Bookmark") },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("Catatan") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateBookmarkNote(bookmark.id, noteInput)
                    editingBookmark = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editingBookmark = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SurahContent(
    surahs: List<Surah>,
    widthSizeClass: WindowWidthSizeClass,
    selectedSurahNumber: Int?,
    onSelectSurah: (Surah) -> Unit,
    onOpenSurah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (widthSizeClass == WindowWidthSizeClass.Compact) {
        SurahListPane(
            surahs = surahs,
            selectedSurahNumber = null,
            onSurahClick = { onOpenSurah(it.number) },
            modifier = modifier
        )
        return
    }

    val selectedSurah = surahs.firstOrNull { it.number == selectedSurahNumber }
        ?: surahs.firstOrNull()

    Row(
        modifier = modifier
    ) {
        SurahListPane(
            surahs = surahs,
            selectedSurahNumber = selectedSurah?.number,
            onSurahClick = onSelectSurah,
            showArabicName = false,
            modifier = Modifier
                .weight(0.52f)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        if (selectedSurah != null) {
            SurahDetailPane(
                surah = selectedSurah,
                onOpen = { onOpenSurah(selectedSurah.number) },
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
            )
        } else {
            AppEmptyState(
                icon = Icons.Rounded.AutoStories,
                title = "Pilih surah",
                description = "Pilih surah dari daftar untuk melihat detailnya.",
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SurahListPane(
    surahs: List<Surah>,
    selectedSurahNumber: Int?,
    onSurahClick: (Surah) -> Unit,
    modifier: Modifier = Modifier,
    showArabicName: Boolean = true
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            items = surahs,
            key = { it.number }
        ) { surah ->
            SurahItemRow(
                surah = surah,
                selected = surah.number == selectedSurahNumber,
                showArabicName = showArabicName,
                onClick = { onSurahClick(surah) }
            )
        }
    }
}

@Composable
private fun SurahDetailPane(
    surah: Surah,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = surah.nameArabic,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
        Text(
            text = surah.nameLatin,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${surah.revelationType.uppercase()} • ${surah.ayahCount} Ayat",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Baca surah ini dengan tampilan reader yang mendukung tajwid, terjemahan, transliterasi, dan bookmark.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppPrimaryButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Rounded.AutoStories, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(text = "Buka reader")
        }
    }
}

@Composable
fun LastReadBanner(
    lastRead: LastRead,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Terakhir Dibaca",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "QS. ${lastRead.surahName} (Ayat ${lastRead.ayahNumber})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Juz ${lastRead.juz} • Halaman ${lastRead.page}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SurahItemRow(
    surah: Surah,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showArabicName: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah Number Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Surah Names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameLatin,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = surah.revelationType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = " • ${surah.ayahCount} Ayat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showArabicName) {
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 140.dp),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
