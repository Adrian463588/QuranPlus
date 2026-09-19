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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
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
import com.quranplus.app.core.ui.theme.QuranColors
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
    onQuizClick: () -> Unit
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
                            title = "Materi Belum Tersedia",
                            description = "Database belum memiliki materi terverifikasi untuk ${selectedCategory.title}."
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
                is UiState.Empty -> {
                    AppEmptyState(
                        icon = Icons.Rounded.School,
                        title = "Materi Belum Tersedia",
                        description = "Tidak ada materi Tahsin valid pada kategori ini."
                    )
                }
                is UiState.Blocked -> {
                    AppEmptyState(
                        icon = Icons.Rounded.School,
                        title = "Materi Diblokir",
                        description = state.reason
                    )
                }
                is UiState.Idle -> {
                    AppEmptyState(
                        icon = Icons.Rounded.School,
                        title = "Menyiapkan Materi",
                        description = "Status database Tahsin belum tersedia."
                    )
                }
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
    val badgeInfo = resolveTahsinBadge(lesson)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant Arabic Calligraphy / Tajweed Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(badgeInfo.containerColor),
                contentAlignment = Alignment.Center
            ) {
                if (badgeInfo.arabicText != null) {
                    Text(
                        text = badgeInfo.arabicText,
                        style = getQuranArabicStyle(badgeInfo.fontSize),
                        color = badgeInfo.contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                } else {
                    Icon(
                        imageVector = badgeInfo.icon ?: Icons.Rounded.School,
                        contentDescription = null,
                        tint = badgeInfo.contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
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

            Spacer(modifier = Modifier.width(Spacing.xs))

            // Trailing Progress Indicator / Detail Chevron
            if (lesson.isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selesai",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private data class TahsinBadgeInfo(
    val arabicText: String? = null,
    val fontSize: Float = 16f,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color
)

@Composable
private fun resolveTahsinBadge(lesson: TahsinLesson): TahsinBadgeInfo {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)

    return when (lesson.category) {
        TahsinCategory.MAKHARIJ -> {
            val rawArabic = lesson.letterArabic.trim()
            val isPureArabic = rawArabic.isNotEmpty() && rawArabic.all {
                it.isWhitespace() || (it in '\u0600'..'\u06FF') || it == '-' || it == '/' || it == ','
            }
            if (isPureArabic && rawArabic.length <= 8) {
                val size = when {
                    rawArabic.length <= 2 -> 20f
                    rawArabic.length <= 4 -> 16f
                    else -> 13f
                }
                TahsinBadgeInfo(
                    arabicText = rawArabic,
                    fontSize = size,
                    containerColor = primaryContainer,
                    contentColor = primaryColor
                )
            } else {
                TahsinBadgeInfo(
                    arabicText = "مَخْرَج",
                    fontSize = 13f,
                    containerColor = primaryContainer,
                    contentColor = primaryColor
                )
            }
        }

        TahsinCategory.SIFAT -> {
            val title = lesson.title
            val text = when {
                title.contains("Hams", true) -> "هَمْس"
                title.contains("Syiddah", true) -> "شِدَّة"
                title.contains("Isti'la", true) -> "اِسْتِعْلَاء"
                title.contains("Ithbaq", true) -> "إِطْبَاق"
                title.contains("Idzlaq", true) -> "إِذْلَاق"
                title.contains("Qalqalah", true) -> "قَلْقَلَة"
                title.contains("Shafir", true) -> "صَفِير"
                title.contains("Lien", true) -> "لِين"
                title.contains("Inhiraf", true) -> "اِنْحِرَاف"
                title.contains("Takrir", true) -> "تَكْرِير"
                title.contains("Tafasysyi", true) -> "تَفَشِّي"
                title.contains("Istithalah", true) -> "اِسْتِطَالَة"
                title.contains("Ghunnah", true) -> "غُنَّة"
                else -> "صِفَة"
            }
            val size = if (text.length <= 5) 15f else 12f
            TahsinBadgeInfo(
                arabicText = text,
                fontSize = size,
                containerColor = secondaryContainer,
                contentColor = secondaryColor
            )
        }

        TahsinCategory.HUKUM_TAJWID -> {
            val title = lesson.title
            val (text, color) = when {
                title.contains("Mad Wajib", true) -> "مَدّ ٤-٥" to QuranColors.TajwidMadWajib
                title.contains("Mad Jaiz", true) -> "مَدّ ٢-٥" to QuranColors.TajwidMadWajib
                title.contains("Mad 'Aridh", true) -> "مَدّ ٢-٦" to QuranColors.TajwidMad
                title.contains("Mad Lazim", true) -> "مَدّ ٦" to QuranColors.TajwidMadLazim
                title.contains("Mad Shilah", true) -> "صِلَة" to QuranColors.TajwidMad
                title.contains("Mad Thabi'i", true) -> "مَدّ ٢" to QuranColors.TajwidMad
                title.contains("Mad", true) -> "مَدّ" to QuranColors.TajwidMad
                title.contains("Izhar Halqi", true) || title.contains("Idzhar", true) -> "إِظْهَار" to QuranColors.TajwidIzhar
                title.contains("Idgham Bighunnah", true) -> "إِدْغَام" to QuranColors.TajwidIdgham
                title.contains("Idgham Bilaghunnah", true) -> "إِدْغَام" to QuranColors.TajwidIdghamBila
                title.contains("Iqlab", true) -> "إِقْلَاب" to QuranColors.TajwidIqlab
                title.contains("Ikhfa Haqiqi", true) -> "إِخْفَاء" to QuranColors.TajwidIkhfa
                title.contains("Ikhfa Syafawi", true) -> "إِخْفَاء" to QuranColors.TajwidIkhfaSyafawi
                title.contains("Idgham Mimi", true) -> "إِدْغَام" to QuranColors.TajwidIdghamMimi
                title.contains("Izhar Syafawi", true) -> "إِظْهَار" to QuranColors.TajwidIzhar
                title.contains("Ghunnah", true) -> "غُنَّة" to QuranColors.TajwidGhunnah
                title.contains("Idgham Mutamatsilain", true) -> "مِثْلَيْن" to QuranColors.TajwidIdgham
                title.contains("Idgham Mutajanisain", true) -> "مُتَجَانِس" to QuranColors.TajwidIdghamMutajanisain
                title.contains("Idgham Mutaqaribain", true) -> "مُتَقَارِب" to QuranColors.TajwidIdghamMutaqaribain
                title.contains("Qamariyah", true) -> "قَمَرِيَّة" to QuranColors.TajwidIzhar
                title.contains("Syamsiyah", true) -> "شَمْسِيَّة" to QuranColors.TajwidIdgham
                title.contains("Ra'", true) -> "رَاء" to primaryColor
                title.contains("Qalqalah", true) -> "قَلْقَلَة" to QuranColors.TajwidQalqalah
                title.contains("Waqaf", true) -> "وَقْف" to QuranColors.BadgeWaqafOptional
                else -> "تَجْوِيد" to primaryColor
            }
            val size = if (text.length <= 5) 15f else 12f
            TahsinBadgeInfo(
                arabicText = text,
                fontSize = size,
                containerColor = color.copy(alpha = 0.18f),
                contentColor = color
            )
        }
    }
}
