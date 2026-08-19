package com.quranplus.app.features.gharib.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.gharib.domain.GharibDataRepository
import com.quranplus.app.features.gharib.domain.GharibReading
import com.quranplus.app.features.gharib.domain.GharibType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GharibScreen(
    audioPlayerManager: AudioPlayerManager,
    onNavigateToAyah: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedSajdahItem by remember { mutableStateOf<GharibReading?>(null) }
    val context = LocalContext.current

    val filters = listOf(
        "ALL" to "Semua",
        "IMALAH" to "Imalah",
        "ISYMAM" to "Isymam",
        "TASHIL" to "Tashil",
        "NAQL" to "Naql",
        "SAKTAH" to "Saktah",
        "SIFIR_MUSTATIL" to "Sifir Mustatil",
        "SIFIR_MUSTADIR" to "Sifir Mustadir",
        "NUN_WIQAYAH" to "Nun Wiqayah",
        "AYAT_SAJDAH" to "Ayat Sajdah"
    )

    val filteredList = remember(selectedFilter) {
        if (selectedFilter == "ALL") {
            GharibDataRepository.ALL_GHARIB_READINGS
        } else {
            GharibDataRepository.ALL_GHARIB_READINGS.filter {
                it.ruleType.name.equals(selectedFilter, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Ensiklopedia Bacaan Gharib",
                subtitle = "Panduan bacaan khusus & langka dalam Al-Qur'an",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(filters) { (id, label) ->
                    val isSelected = selectedFilter == id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = id },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Readings List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(filteredList, key = { it.id }) { reading ->
                    GharibCardItem(
                        reading = reading,
                        onPlayAudio = {
                            audioPlayerManager.playAyah(
                                surahNumber = reading.surahNumber,
                                surahName = reading.surahName,
                                ayahNumber = reading.ayahNumber,
                                totalAyahsInSurah = 200
                            )
                            Toast.makeText(context, "Memutar QS. ${reading.surahName}:${reading.ayahNumber}", Toast.LENGTH_SHORT).show()
                        },
                        onNavigate = {
                            onNavigateToAyah(reading.surahNumber, reading.ayahNumber)
                        },
                        onShowSajdahDialog = {
                            selectedSajdahItem = reading
                        }
                    )
                }
            }
        }

        // Sujud Tilawah Information Dialog
        selectedSajdahItem?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedSajdahItem = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tata Cara Sujud Tilawah ۩", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "QS. ${item.surahName} : ${item.ayahNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Hukum: Sunnah Muakkadah bagi pembaca dan pendengar ayat Sajdah.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "Bacaan Doa Sujud Tilawah:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "سَجَدَ وَجْهِي لِلَّذِي خَلَقَهُ وَصَوَّرَهُ وَشَقَّ سَمْعَهُ وَبَصَرَهُ بِحَوْلِهِ وَقُوَّتِهِ فَتَبَارَكَ اللَّهُ أَحْسَنُ الْخَالِقِينَ",
                                style = getQuranArabicStyle(18f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(Spacing.sm)
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "\"Wajahku bersujud kepada Dzat yang menciptakannya, membentuk rupanya, dan membuka pendengaran serta penglihatannya dengan daya dan kekuatan-Nya. Mahasuci Allah, sebaik-baik Pencipta.\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    AppPrimaryButton(onClick = {
                        val surah = item.surahNumber
                        val ayah = item.ayahNumber
                        selectedSajdahItem = null
                        onNavigateToAyah(surah, ayah)
                    }) {
                        Text("Buka di Reader")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedSajdahItem = null }) {
                        Text("Tutup")
                    }
                }
            )
        }
    }
}

@Composable
fun GharibCardItem(
    reading: GharibReading,
    onPlayAudio: () -> Unit,
    onNavigate: () -> Unit,
    onShowSajdahDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Header Row: Category Badge + Reference Deep Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = reading.categoryTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "QS. ${reading.surahName} : ${reading.ayahNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Arabic Snippet
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = reading.wordSnippetArabic,
                        style = getQuranArabicStyle(26f),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = reading.fullAyahArabic,
                        style = getQuranArabicStyle(18f),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Written vs Spoken Comparison Box
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reading.writtenVsSpoken,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Special Component: Isymam Lip Movement Visual Diagram
            if (reading.ruleType == GharibType.ISYMAM) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                IsymamLipDiagram()
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Pronunciation Guide & Detailed Explanation
            Text(
                text = reading.pronunciationGuide,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reading.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedButton(onClick = onPlayAudio) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Putar Audio")
                    }

                    if (reading.ruleType == GharibType.AYAT_SAJDAH) {
                        OutlinedButton(onClick = onShowSajdahDialog) {
                            Text("Doa Sujud ۩")
                        }
                    }
                }

                AppPrimaryButton(onClick = onNavigate) {
                    Icon(imageVector = Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buka Ayat")
                }
            }
        }
    }
}

@Composable
fun IsymamLipDiagram() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Diagram Gerakan Bibir Saat Isymam:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LipStageCard(step = "1. Buka", label = "Ta' (تَـ)", description = "Bibir normal terbuka")
                Text("➔", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                LipStageCard(step = "2. Moncongkan", label = "Memoncong ۫", description = "Bibir maju tanpa suara")
                Text("➔", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                LipStageCard(step = "3. Sempurnakan", label = "ـنَّا", description = "Dengung Nun 2 harakat")
            }
        }
    }
}

@Composable
private fun LipStageCard(step: String, label: String, description: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(step, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 9.sp)
    }
}
