package com.quranplus.app.features.quran.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.core.audio.AudioRepeatMode
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.core.utils.TajwidParser
import com.quranplus.app.features.quran.domain.Ayah
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahActionBottomSheet(
    ayah: Ayah,
    surahName: String,
    totalAyahsInSurah: Int,
    sheetState: SheetState,
    audioPlayerManager: AudioPlayerManager,
    onDismissRequest: () -> Unit,
    onBookmarkToggle: (note: String?) -> Unit
) {
    val context = LocalContext.current
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var showTafsirSection by remember { mutableStateOf(false) }
    var showTajwidSection by remember { mutableStateOf(false) }
    val selectedQari by audioPlayerManager.selectedQari.collectAsStateWithLifecycle()
    val audioAvailable = remember(ayah.surahNumber, ayah.ayahNumber, selectedQari) {
        audioPlayerManager.getAyahAudioUrl(
            selectedQari,
            ayah.surahNumber,
            ayah.ayahNumber
        ) != null
    }

    val tajwidOccurrences = remember(ayah.textArabic, ayah.tajwidTags) {
        TajwidParser.extractTajwidOccurrences(ayah.textArabic, ayah.tajwidTags)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.xxl)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Surah & Ayah Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QS. $surahName : ${ayah.ayahNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Juz ${ayah.juz} • Halaman ${ayah.page}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showNoteDialog = true }) {
                    Icon(
                        imageVector = if (ayah.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark & Catatan",
                        tint = if (ayah.isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Ayah Arabic Snippet Preview
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = TajwidParser.buildColoredAyahText(
                        arabicText = ayah.textArabic,
                        tajwidTags = ayah.tajwidTags,
                        enableTajwid = true
                    ),
                    style = getQuranArabicStyle(22f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(Spacing.md)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(Spacing.sm))

            // 1. Play Audio Ayah
            ActionItemRow(
                icon = Icons.Rounded.PlayArrow,
                title = if (audioAvailable) "Putar Audio Murottal" else "Audio Murottal belum tersedia",
                subtitle = if (audioAvailable) {
                    "Dengarkan pelafalan qari pilihan untuk ayat ini"
                } else {
                    "Asset audio dan checksum terverifikasi belum tersedia"
                },
                enabled = audioAvailable,
                onClick = {
                    audioPlayerManager.playAyah(
                        surahNumber = ayah.surahNumber,
                        surahName = surahName,
                        ayahNumber = ayah.ayahNumber,
                        totalAyahsInSurah = totalAyahsInSurah
                    )
                    Toast.makeText(context, "Memutar QS. $surahName:${ayah.ayahNumber}", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                }
            )

            // 2. Repeat Ayah Mode
            ActionItemRow(
                icon = Icons.Rounded.Repeat,
                title = if (audioAvailable) "Ulangi Pemutaran Ayat (Muraja'ah)" else "Pengulangan audio belum tersedia",
                subtitle = if (audioAvailable) {
                    "Setel pengulangan otomatis 1x, 2x, 3x, 5x, atau loop tak terbatas"
                } else {
                    "Pengulangan aktif setelah asset audio terverifikasi tersedia"
                },
                enabled = audioAvailable,
                onClick = {
                    audioPlayerManager.setRepeatMode(AudioRepeatMode.THREE_TIMES)
                    audioPlayerManager.playAyah(
                        surahNumber = ayah.surahNumber,
                        surahName = surahName,
                        ayahNumber = ayah.ayahNumber,
                        totalAyahsInSurah = totalAyahsInSurah
                    )
                    Toast.makeText(context, "Mode Pengulangan 3x Aktif", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                }
            )

            // 3. Bookmark & Note
            ActionItemRow(
                icon = Icons.Rounded.EditNote,
                title = "Simpan Bookmark & Catatan",
                subtitle = "Tandai ayat dan tambahkan refleksi / tadabbur pribadi",
                onClick = { showNoteDialog = true }
            )

            // 4. Copy Ayah
            ActionItemRow(
                icon = Icons.Rounded.ContentCopy,
                title = "Salin Teks Ayat & Terjemahan",
                subtitle = "Salin teks Arab, Latin, terjemahan, dan rujukan",
                onClick = {
                    val fullText = """
                        ${ayah.textArabic}
                        
                        "${ayah.transliteration}"
                        
                        Artinya:
                        "${ayah.translationId}"
                        (QS. $surahName: ${ayah.ayahNumber})
                    """.trimIndent()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Ayat Al-Quran", fullText))
                    Toast.makeText(context, "Teks ayat berhasil disalin", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                }
            )

            // 5. Share Ayah
            ActionItemRow(
                icon = Icons.Rounded.Share,
                title = "Bagikan Ayat",
                subtitle = "Kirim ayat ke WhatsApp, media sosial, atau aplikasi lain",
                onClick = {
                    val shareText = """
                        ${ayah.textArabic}
                        
                        "${ayah.translationId}"
                        (QS. $surahName: ${ayah.ayahNumber})
                        
                        Dibagikan via Quran Plus App
                    """.trimIndent()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Ayat"))
                    onDismissRequest()
                }
            )

            // 6. Detail Tafsir Section
            ActionExpandableHeader(
                icon = Icons.Rounded.Description,
                title = "Detail Tafsir Ringkas",
                isExpanded = showTafsirSection,
                onToggle = { showTafsirSection = !showTafsirSection }
            )
            AnimatedVisibility(visible = showTafsirSection) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Tafsir terverifikasi:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Tafsir terverifikasi belum tersedia di korpus aplikasi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 7. Detail Hukum Tajwid Section
            ActionExpandableHeader(
                icon = Icons.Rounded.Palette,
                title = "Rincian Hukum Tajwid (${tajwidOccurrences.size} Terdeteksi)",
                isExpanded = showTajwidSection,
                onToggle = { showTajwidSection = !showTajwidSection }
            )
            AnimatedVisibility(visible = showTajwidSection) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (tajwidOccurrences.isEmpty()) {
                        Text(
                            text = "Tidak ada hukum tajwid khusus di luar bacaan dasar pada ayat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.sm)
                        )
                    } else {
                        tajwidOccurrences.forEach { occurrence ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(occurrence.type.color)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = occurrence.type.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = occurrence.type.harakatDuration,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = occurrence.type.ruleExplanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Reflection / Note Dialog
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Bookmark & Catatan Refleksi") },
            text = {
                Column {
                    Text(
                        text = "QS. $surahName : ${ayah.ayahNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Catatan / Tadabbur Pribadi (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                AppPrimaryButton(onClick = {
                    onBookmarkToggle(noteText.ifBlank { null })
                    showNoteDialog = false
                    onDismissRequest()
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ActionItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionExpandableHeader(
    icon: ImageVector,
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .heightIn(min = 48.dp)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
