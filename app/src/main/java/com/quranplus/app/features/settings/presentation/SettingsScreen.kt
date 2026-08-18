package com.quranplus.app.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.settings.data.AiPersona

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val showTransliteration by viewModel.showTransliteration.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val enableTajwid by viewModel.enableTajwid.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val customPrompt by viewModel.customSystemPrompt.collectAsState()

    var customPromptInput by remember(customPrompt) { mutableStateOf(customPrompt) }

    Scaffold(
        topBar = {
            AppTopBar(title = "Pengaturan")
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
            // Section 1: Tampilan & Tilawah
            SettingsSectionHeader(title = "Tampilan & Tilawah", icon = Icons.Rounded.Palette)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    // Dark Mode Toggle
                    SettingsSwitchRow(
                        title = "Mode Gelap (OLED Dark)",
                        subtitle = "Tampilan hangat ramah mata untuk tilawah malam hari",
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))

                    // Tajwid Coloring Toggle
                    SettingsSwitchRow(
                        title = "Tajwid Berwarna",
                        subtitle = "Beri warna pada hukum nun sukun, mim, mad, dan qalqalah",
                        checked = enableTajwid,
                        onCheckedChange = { viewModel.setEnableTajwid(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))

                    // Transliteration Toggle
                    SettingsSwitchRow(
                        title = "Transliterasi Latin",
                        subtitle = "Tampilkan teks latin pelafalan ayat",
                        checked = showTransliteration,
                        onCheckedChange = { viewModel.setShowTransliteration(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))

                    // Translation Toggle
                    SettingsSwitchRow(
                        title = "Terjemahan Bahasa Indonesia",
                        subtitle = "Tampilkan arti ayat Kemenag RI",
                        checked = showTranslation,
                        onCheckedChange = { viewModel.setShowTranslation(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))

                    // Arabic Font Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ukuran Font Teks Arab", style = MaterialTheme.typography.titleSmall)
                            Text("${arabicFontSize.toInt()} sp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Slider(
                            value = arabicFontSize,
                            onValueChange = { viewModel.setArabicFontSize(it) },
                            valueRange = 20f..40f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Section 2: Persona AI Islami
            SettingsSectionHeader(title = "Karakter Persona AI", icon = Icons.Rounded.Psychology)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    AiPersona.entries.forEach { persona ->
                        val isSelected = persona == selectedPersona
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedPersona(persona) }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = Spacing.sm)
                            ) {
                                Text(
                                    text = persona.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = persona.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (selectedPersona == AiPersona.CUSTOM) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        OutlinedTextField(
                            value = customPromptInput,
                            onValueChange = { customPromptInput = it },
                            label = { Text("Instruksi System Prompt Kustom") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        AppPrimaryButton(
                            onClick = { viewModel.setCustomSystemPrompt(customPromptInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simpan Prompt Kustom")
                        }
                    }
                }
            }

            // Section 3: Tentang & Privasi
            SettingsSectionHeader(title = "Tentang & Keamanan Data", icon = Icons.Rounded.Security)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Quran Plus v1.0.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Aplikasi tilawah Al-Qur'an dan asisten AI lokal berlandaskan Al-Qur'an & As-Sunnah Ash-Shahihah. Dibuat dengan arsitektur offline-first, tanpa pengiriman data pribadi, dan tanpa telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = Spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
