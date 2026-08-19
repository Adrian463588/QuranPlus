package com.quranplus.app.features.tahsin.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.core.audio.PlaybackState
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppSecondaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.core.utils.SurahMapper

@Composable
fun LessonDetailScreen(
    lessonId: Int,
    viewModel: TahsinViewModel,
    audioPlayerManager: AudioPlayerManager? = null,
    onNavigateToAyah: ((Int, Int) -> Unit)? = null,
    onBackClick: () -> Unit
) {
    val lesson by viewModel.selectedLesson.collectAsState()
    val context = LocalContext.current
    val playbackState = audioPlayerManager?.playbackState?.collectAsState()?.value

    LaunchedEffect(lessonId) {
        viewModel.loadLessonDetail(lessonId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = lesson?.title ?: "Detail Materi",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (lesson == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val item = lesson!!
            val parsedRef = SurahMapper.parseAyahReference(item.exampleAyahRef)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Arabic Letter Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.letterArabic.ifBlank { item.title.take(1) },
                                style = getQuranArabicStyle(40f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = item.letterLatin,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.subcategory,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Makhraj / Articulation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Titik Artikulasi (Makhraj):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = item.articulationPoint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Description Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Penjelasan & Kaidah:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Quran Example Ayah Card
                if (item.exampleAyahText.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Contoh Ayat Al-Qur'an:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = item.exampleAyahRef.substringBefore("(").trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = item.exampleAyahText,
                                style = getQuranArabicStyle(24f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )

                            // Interactive Audio & Navigation Bar for Example Ayah
                            if (parsedRef != null && audioPlayerManager != null) {
                                val (surahRef, ayahNum) = parsedRef
                                val isCurrentlyPlaying = playbackState is PlaybackState.Playing &&
                                        playbackState.surahNumber == surahRef.number &&
                                        playbackState.ayahNumber == ayahNum

                                Spacer(modifier = Modifier.height(Spacing.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (isCurrentlyPlaying) {
                                                audioPlayerManager.togglePlayPause()
                                            } else {
                                                audioPlayerManager.playAyah(
                                                    surahNumber = surahRef.number,
                                                    surahName = surahRef.latinName,
                                                    ayahNumber = ayahNum,
                                                    totalAyahsInSurah = surahRef.ayahCount
                                                )
                                                Toast.makeText(context, "Memutar QS. ${surahRef.latinName}:$ayahNum", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentlyPlaying) Icons.Rounded.Pause else Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text(if (isCurrentlyPlaying) "Jeda" else "Dengarkan", fontSize = 13.sp)
                                    }

                                    if (onNavigateToAyah != null) {
                                        OutlinedButton(
                                            onClick = { onNavigateToAyah(surahRef.number, ayahNum) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text("Lihat Surah", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                val haptic = LocalHapticFeedback.current

                // Mark Completed CTA
                if (item.isCompleted) {
                    AppSecondaryButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleLessonCompleted(item)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Sudah Dipelajari (Ketuk untuk batalkan)")
                    }
                } else {
                    AppPrimaryButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleLessonCompleted(item)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Tandai Selesai Dipelajari")
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xxl))
            }
        }
    }
}
