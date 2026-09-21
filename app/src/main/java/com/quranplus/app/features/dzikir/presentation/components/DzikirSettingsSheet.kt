package com.quranplus.app.features.dzikir.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.dzikir.domain.DzikirUiSettings
import com.quranplus.app.features.dzikir.domain.TranslationLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DzikirSettingsSheet(
    settings: DzikirUiSettings,
    onFontSizeChange: (Float) -> Unit,
    onToggleTajwid: () -> Unit,
    onToggleTransliteration: () -> Unit,
    onToggleTranslation: () -> Unit,
    onLanguageSelect: (TranslationLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.lg)
        ) {
            Text(
                text = "Pengaturan Tampilan Bacaan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Font Size Slider
            Text(
                text = "Ukuran Huruf Arab (${settings.arabicFontSize.toInt()} sp)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = settings.arabicFontSize,
                onValueChange = onFontSizeChange,
                valueRange = 20f..40f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            // Tajwid Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Warna Tajwid Tartil",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Memberi warna hukum bacaan ikhfa, idgham, ghunnah, dsb.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showTajwid,
                    onCheckedChange = { onToggleTajwid() }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Transliteration Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transliterasi Latin",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Teks latin dengan kaidah fonetik tajwid (ikhfa 'ng')",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showTransliteration,
                    onCheckedChange = { onToggleTransliteration() }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Translation Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Terjemahan",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Menampilkan arti bacaan dzikir & doa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showTranslation,
                    onCheckedChange = { onToggleTranslation() }
                )
            }

            if (settings.showTranslation) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Bahasa Terjemahan:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = settings.translationLanguage == TranslationLanguage.INDONESIAN,
                        onClick = { onLanguageSelect(TranslationLanguage.INDONESIAN) },
                        label = { Text("Indonesia") }
                    )
                    FilterChip(
                        selected = settings.translationLanguage == TranslationLanguage.ENGLISH,
                        onClick = { onLanguageSelect(TranslationLanguage.ENGLISH) },
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = settings.translationLanguage == TranslationLanguage.BOTH,
                        onClick = { onLanguageSelect(TranslationLanguage.BOTH) },
                        label = { Text("Keduanya") }
                    )
                }
            }
        }
    }
}
