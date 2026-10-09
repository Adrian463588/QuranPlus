package com.quranplus.app.features.quran.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.quranplus.app.features.hadith.presentation.HadithViewModel
import com.quranplus.shared.features.quran.domain.HadithBookmark
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.quran.domain.Bookmark
import com.quranplus.app.features.quran.domain.BookmarkSort
import kotlinx.coroutines.launch

@Composable
fun BookmarksScreen(
    viewModel: QuranViewModel,
    hadithViewModel: HadithViewModel? = null,
    onBookmarkClick: (Int, Int) -> Unit,
    onHadithBookmarkClick: (String, Int) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(0) }
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val bookmarkSort by viewModel.bookmarkSort.collectAsStateWithLifecycle()
    val hadithBookmarks = hadithViewModel?.hadithBookmarks?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var noteInput by remember { mutableStateOf("") }
    var editingHadithBookmark by remember { mutableStateOf<HadithBookmark?>(null) }
    var hadithNoteInput by remember { mutableStateOf("") }

    fun deleteWithUndo(bookmark: Bookmark) {
        viewModel.deleteBookmark(bookmark)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Bookmark Al-Qur'an dihapus",
                actionLabel = "Urungkan"
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.restoreBookmark(bookmark)
            }
        }
    }

    fun deleteHadithWithUndo(bookmark: HadithBookmark) {
        hadithViewModel?.deleteHadithBookmark(bookmark)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Bookmark hadits dihapus",
                actionLabel = "Urungkan"
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                hadithViewModel?.restoreHadithBookmark(bookmark)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Daftar Bookmark")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        val count = (bookmarksState as? UiState.Success)?.data?.size ?: 0
                        Text(if (count > 0) "Al-Qur'an ($count)" else "Al-Qur'an")
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val count = hadithBookmarks.size
                        Text(if (count > 0) "Hadist ($count)" else "Hadist")
                    }
                )
            }

            if (selectedTab == 0) {
                when (val state = bookmarksState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        AppEmptyState(
                            icon = Icons.Rounded.BookmarkBorder,
                            title = "Belum Ada Bookmark",
                            description = "Simpan ayat-ayat Al-Qur'an penting saat tilawah untuk membacanya kembali di sini."
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
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
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(state.data, key = { it.id }) { bookmark ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                                deleteWithUndo(bookmark)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.errorContainer)
                                                    .padding(horizontal = Spacing.lg),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Hapus bookmark")
                                            }
                                        }
                                    ) {
                                        BookmarkItemCard(
                                            bookmark = bookmark,
                                            onClick = { onBookmarkClick(bookmark.surahNumber, bookmark.ayahNumber) },
                                            onDeleteClick = { deleteWithUndo(bookmark) },
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
                }
                is UiState.Error -> {
                    AppEmptyState(
                        icon = Icons.Rounded.BookmarkBorder,
                        title = "Terjadi Kesalahan",
                        description = state.message
                    )
                }
                is UiState.Empty -> AppEmptyState(
                    icon = Icons.Rounded.BookmarkBorder,
                    title = "Belum Ada Bookmark",
                    description = "Simpan ayat-ayat Al-Qur'an penting saat tilawah untuk membacanya kembali di sini."
                )
                is UiState.Blocked, UiState.Idle -> AppEmptyState(
                    icon = Icons.Rounded.BookmarkBorder,
                    title = "Bookmark belum siap",
                    description = "Data bookmark belum dapat dimuat."
                )
            }
            } else {
                if (hadithBookmarks.isEmpty()) {
                    AppEmptyState(
                        icon = Icons.Rounded.BookmarkBorder,
                        title = "Belum Ada Bookmark Hadits",
                        description = "Simpan hadits-hadits penting saat membaca untuk membukanya kembali di sini."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(hadithBookmarks, key = { it.id }) { bookmark ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        deleteHadithWithUndo(bookmark)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(horizontal = Spacing.lg),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus bookmark hadits")
                                    }
                                }
                            ) {
                                HadithBookmarkItemCard(
                                    bookmark = bookmark,
                                    onClick = { onHadithBookmarkClick(bookmark.collectionId, bookmark.hadithNumber) },
                                    onDeleteClick = { deleteHadithWithUndo(bookmark) },
                                    onNoteClick = {
                                        hadithNoteInput = bookmark.note.orEmpty()
                                        editingHadithBookmark = bookmark
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingBookmark?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { editingBookmark = null },
            title = { Text("Catatan Bookmark Al-Qur'an") },
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

    editingHadithBookmark?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { editingHadithBookmark = null },
            title = { Text("Catatan Bookmark Hadits") },
            text = {
                OutlinedTextField(
                    value = hadithNoteInput,
                    onValueChange = { hadithNoteInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("Catatan") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hadithViewModel?.updateHadithBookmarkNote(bookmark.collectionId, bookmark.hadithNumber, hadithNoteInput)
                    editingHadithBookmark = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editingHadithBookmark = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun HadithBookmarkItemCard(
    bookmark: HadithBookmark,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HR. ${bookmark.collectionName} · No. ${bookmark.hadithNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onNoteClick) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit catatan bookmark"
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Hapus Bookmark",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (bookmark.hadithTextArabic.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = bookmark.hadithTextArabic,
                    style = getQuranArabicStyle(20f),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (bookmark.hadithTranslation.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = bookmark.hadithTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            bookmark.note?.takeIf(String::isNotBlank)?.let { note ->
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BookmarkItemCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QS. ${bookmark.surahName} (${bookmark.ayahNumber})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onNoteClick) {
                        Icon(
                        imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit catatan bookmark"
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Hapus Bookmark",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = bookmark.ayahTextArabic,
                style = getQuranArabicStyle(20f),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = bookmark.ayahTranslation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            bookmark.note?.takeIf(String::isNotBlank)?.let { note ->
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
