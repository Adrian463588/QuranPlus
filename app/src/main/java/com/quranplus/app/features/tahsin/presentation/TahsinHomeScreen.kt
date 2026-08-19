package com.quranplus.app.features.tahsin.presentation

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
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.quran.presentation.UiState
import com.quranplus.app.features.tahsin.domain.TahsinCategory
import com.quranplus.app.features.tahsin.domain.TahsinLesson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TahsinHomeScreen(
    viewModel: TahsinViewModel,
    onLessonClick: (Int) -> Unit,
    onQuizClick: () -> Unit = {}
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val lessonsState by viewModel.lessonsState.collectAsStateWithLifecycle()
    val categories = TahsinCategory.entries

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tahsin & Makharij",
                subtitle = "Panduan tartil dan makhraj huruf",
                actions = {
                    IconButton(onClick = onQuizClick) {
                        Icon(
                            imageVector = Icons.Rounded.Quiz,
                            contentDescription = "Kuis Latihan Tajwid",
                            tint = MaterialTheme.colorScheme.secondary
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
            // Responsive Scrollable Tab Row (Prevents Left & Right Edge Clipping)
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                edgePadding = Spacing.md,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = category == selectedCategory,
                        onClick = { viewModel.selectCategory(category) },
                        text = {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            when (val state = lessonsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        AppEmptyState(
                            icon = Icons.Rounded.School,
                            title = "Materi Sedang Disiapkan",
                            description = "Data materi ${selectedCategory.title} akan segera dimuat."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Spacing.md,
                                vertical = Spacing.sm
                            ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(items = state.data, key = { it.id }) { lesson ->
                                TahsinLessonRow(
                                    lesson = lesson,
                                    onClick = { onLessonClick(lesson.id) }
                                )
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    AppEmptyState(
                        icon = Icons.Rounded.School,
                        title = "Gagal Memuat Materi",
                        description = state.message
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun TahsinLessonRow(
    lesson: TahsinLesson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Adaptive Arabic Letter Badge (Never Clips)
            val badgeText = lesson.letterArabic.ifBlank { lesson.title.take(2) }
            val fontSize = when {
                badgeText.length <= 3 -> 18f
                badgeText.length <= 6 -> 14f
                else -> 12f
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = getQuranArabicStyle(fontSize),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Lesson Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lesson.articulationPoint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Completion Icon
            Icon(
                imageVector = if (lesson.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (lesson.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            )
        }
    }
}
