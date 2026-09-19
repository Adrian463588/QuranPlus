package com.quranplus.app.features.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.rag.presentation.RagDocumentViewModel
import com.quranplus.app.features.rag.presentation.RagImportState
import com.quranplus.app.features.settings.data.AiPersona

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onNavigateToAudioManager: () -> Unit = {},
    onNavigateToWaqafGuide: () -> Unit = {},
    onNavigateToGharib: () -> Unit = {},
    onNavigateToQuiz: () -> Unit = {},
    ragDocumentViewModel: RagDocumentViewModel,
    onRequestRagDocument: () -> Unit,
    onRequestRagDocumentFile: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val arabicFontSize by viewModel.arabicFontSize.collectAsStateWithLifecycle()
    val showTransliteration by viewModel.showTransliteration.collectAsStateWithLifecycle()
    val showTranslation by viewModel.showTranslation.collectAsStateWithLifecycle()
    val enableTajwid by viewModel.enableTajwid.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val customPrompt by viewModel.customSystemPrompt.collectAsStateWithLifecycle()
    val selectedEmbeddingModel by viewModel.selectedEmbeddingModel.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val downloadingModel by viewModel.downloadingModel.collectAsStateWithLifecycle()
    val ragImportState by ragDocumentViewModel.state.collectAsStateWithLifecycle()

    var customPromptInput by remember(customPrompt) { mutableStateOf(customPrompt) }
    var showEmbeddingSheet by remember { mutableStateOf(false) }

    val activeEmbeddingModelInfo = viewModel.availableEmbeddingModels.firstOrNull { it.id == selectedEmbeddingModel }

    Scaffold(
        topBar = {
            AppTopBar(title = "Pengaturan", onBackClick = onBackClick)
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
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            title = "Tajwid Berwarna (High Contrast)",
                            subtitle = "Beri sorotan warna kontras pada hukum tajwid mushaf",
                            checked = enableTajwid,
                            onCheckedChange = { viewModel.setEnableTajwid(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))

                        // Transliteration Toggle
                        SettingsSwitchRow(
                            title = "Transliterasi Latin",
                            subtitle = "Tampilkan teks latin pelafalan ayat sesuai tajwid",
                            checked = showTransliteration,
                            onCheckedChange = { viewModel.setShowTransliteration(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))

                        // Translation Toggle
                        SettingsSwitchRow(
                            title = "Terjemahan Bahasa Indonesia",
                            subtitle = "Tampilkan arti ayat terjemahan resmi Kemenag RI",
                            checked = showTranslation,
                            onCheckedChange = { viewModel.setShowTranslation(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))

                        // Arabic Font Size Slider & Live Preview
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ukuran Font Teks Arab", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${arabicFontSize.toInt()} sp",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Slider(
                                value = arabicFontSize,
                                onValueChange = { viewModel.setArabicFontSize(it) },
                                valueRange = 18f..48f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            // Live Preview Box
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                    style = getQuranArabicStyle(arabicFontSize),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(Spacing.sm)
                                )
                            }
                        }
                    }
                }

                // Section 2: Karakter Persona AI
                SettingsSectionHeader(title = "Karakter Persona Asisten AI", icon = Icons.Rounded.Psychology)

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AiPersona.entries.forEach { persona ->
                        PersonaSelectionCard(
                            persona = persona,
                            isSelected = persona == selectedPersona,
                            onSelect = { viewModel.setSelectedPersona(persona) }
                        )
                    }

                    if (selectedPersona == AiPersona.CUSTOM) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(Spacing.md)) {
                                OutlinedTextField(
                                    value = customPromptInput,
                                    onValueChange = { customPromptInput = it },
                                    label = { Text("Instruksi System Prompt Kustom") },
                                    placeholder = { Text("Masukkan instruksi khusus untuk asisten AI Anda...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                AppPrimaryButton(
                                    onClick = { viewModel.setCustomSystemPrompt(customPromptInput) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Simpan Prompt Kustom")
                                }
                            }
                        }
                    }
                }

                // Section 3: Basis Data & Dokumen AI (RAG)
                SettingsSectionHeader(title = "Basis Pengetahuan AI (RAG)", icon = Icons.Rounded.Storage)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        SettingsNavRow(
                            icon = Icons.Rounded.FolderOpen,
                            title = "Folder Model & Dokumen (SAF)",
                            subtitle = "Atur direktori lokal untuk model AI dan dokumen referensi",
                            isLoading = ragImportState is RagImportState.LinkingStorage,
                            onClick = onRequestRagDocument
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.xs))

                        SettingsNavRow(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            title = "Impor Dokumen Referensi",
                            subtitle = "Tambah berkas TXT, Markdown, atau JSON Hadist",
                            isLoading = ragImportState is RagImportState.Importing,
                            onClick = onRequestRagDocumentFile
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.xs))

                        SettingsNavRow(
                            icon = Icons.Rounded.Psychology,
                            title = "Bangun & Sinkronisasi Indeks AI",
                            subtitle = "Proses embedding Quran, Hadist, dan dokumen referensi",
                            isLoading = ragImportState is RagImportState.Indexing,
                            onClick = ragDocumentViewModel::buildIndex
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.xs))

                        SettingsNavRow(
                            icon = Icons.Rounded.Storage,
                            title = "Model Embedding RAG",
                            subtitle = activeEmbeddingModelInfo?.let { "${it.name} (${it.sizeDescription})" }
                                ?: if (selectedEmbeddingModel.contains("minilm", ignoreCase = true))
                                    "all-MiniLM-L6-v2 (ONNX • 384 Dim • Cepat & Presisi)"
                                else
                                    "$selectedEmbeddingModel (ONNX Vector Embedder)",
                            onClick = { showEmbeddingSheet = true }
                        )

                        // Dynamic Status Notification (Only shown when an active operation or status is present)
                        when (val state = ragImportState) {
                            RagImportState.Idle -> { /* No static slop box */ }
                            RagImportState.LinkingStorage,
                            RagImportState.Importing,
                            RagImportState.Indexing -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.sm)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = when (state) {
                                                RagImportState.Indexing -> "Sedang memproses embedding Quran, Hadist, dan dokumen..."
                                                RagImportState.Importing -> "Memvalidasi dan menyimpan dokumen referensi..."
                                                RagImportState.LinkingStorage -> "Menghubungkan folder penyimpanan..."
                                                else -> "Memproses..."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            is RagImportState.Indexed,
                            is RagImportState.StorageLinked,
                            is RagImportState.HadithImported,
                            is RagImportState.StoredAwaitingEmbedding -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.sm)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = when (state) {
                                                is RagImportState.Indexed -> "Indeks AI aktif untuk ${state.count} potongan sumber."
                                                is RagImportState.StorageLinked -> "Folder aktif: ${state.status.manifestCount} manifest, ${state.status.sourceCount} berkas sumber."
                                                is RagImportState.HadithImported -> "Hadist ${state.title} berhasil diimpor (${state.count} riwayat)."
                                                is RagImportState.StoredAwaitingEmbedding -> "Tersimpan ${state.metadata.displayName}; siap diindeks."
                                                else -> ""
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            is RagImportState.Error,
                            is RagImportState.IndexBlocked,
                            is RagImportState.Unsupported -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.sm)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = when (state) {
                                                is RagImportState.IndexBlocked -> "Indeks AI belum aktif: ${state.reason}"
                                                is RagImportState.Unsupported -> state.reason
                                                is RagImportState.Error -> state.reason
                                                else -> ""
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // App Version Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quran Plus v2.0.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Standar Mushaf Rasm Utsmani Kemenag RI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
            }
        }
    }

    if (showEmbeddingSheet) {
        EmbeddingModelSelectionModal(
            availableModels = viewModel.availableEmbeddingModels,
            selectedEmbeddingModelId = selectedEmbeddingModel,
            downloadState = downloadState,
            downloadingModel = downloadingModel,
            isModelInstalled = viewModel::isModelInstalled,
            onSelectEmbeddingModel = { modelId ->
                viewModel.setSelectedEmbeddingModel(modelId)
            },
            onDownloadModel = { model ->
                viewModel.startEmbeddingModelDownload(model)
            },
            onCancelDownload = { model ->
                viewModel.cancelEmbeddingModelDownload(model)
            },
            onDismiss = { showEmbeddingSheet = false }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddingModelSelectionModal(
    availableModels: List<ModelInfo>,
    selectedEmbeddingModelId: String,
    downloadState: DownloadState,
    downloadingModel: ModelInfo?,
    isModelInstalled: (ModelInfo) -> Boolean,
    onSelectEmbeddingModel: (String) -> Unit,
    onDownloadModel: (ModelInfo) -> Unit,
    onCancelDownload: (ModelInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pilih Model Embedding RAG",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Model representasi vektor ONNX 100% offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            availableModels.forEach { model ->
                val isInstalled = isModelInstalled(model)
                val isSelected = model.id == selectedEmbeddingModelId || (selectedEmbeddingModelId.isEmpty() && isInstalled)
                val isDownloading = downloadingModel?.id == model.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isInstalled) {
                                onSelectEmbeddingModel(model.id)
                            } else if (model.isDownloadable && !isDownloading) {
                                onDownloadModel(model)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected && isInstalled)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = if (isSelected && isInstalled)
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = if (isSelected && isInstalled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${model.sizeDescription} • ${model.runtime}" +
                                                (model.embeddingDimension?.let { " • $it-dim" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (model.isRecommended) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Rekomendasi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        val desc = when (model.id) {
                            "all-minilm-l6-v2-onnx" -> "Model standar bawaan, sangat cepat dan presisi untuk teks Al-Qur'an & Hadist."
                            "bge-small-en-v1.5-onnx" -> "Model representasi semantik tinggi untuk pencarian vektor kontekstual."
                            "paraphrase-multilingual-minilm-l12-v2-onnx" -> "Model multibahasa berdimensi 384 untuk pencarian lintas bahasa."
                            else -> "Format: ${model.format} • Runtime: ${model.runtime}"
                        }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        if (isInstalled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isSelected) "Sedang Aktif" else "Siap Digunakan",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (!isSelected) {
                                    OutlinedButton(
                                        onClick = { onSelectEmbeddingModel(model.id) },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = "Gunakan Model",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        } else if (isDownloading) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                when (val state = downloadState) {
                                    is DownloadState.Transferring -> {
                                        LinearProgressIndicator(
                                            progress = { state.progressPercentage / 100f },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val downloadedMb = state.bytesDownloaded / (1024 * 1024)
                                            val totalMb = state.totalBytes / (1024 * 1024)
                                            val speedMb = "%.1f".format(state.speedBytesPerSec / (1024.0 * 1024.0))
                                            Column {
                                                Text(
                                                    text = "Mengunduh... ${state.progressPercentage}%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "$downloadedMb MB / $totalMb MB • $speedMb MB/s",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            TextButton(
                                                onClick = { onCancelDownload(model) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Batal", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                    is DownloadState.Queued -> {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Menyiapkan unduhan...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            TextButton(
                                                onClick = { onCancelDownload(model) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Batal", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                    is DownloadState.Paused -> {
                                        LinearProgressIndicator(
                                            progress = { 0f },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Terjeda: ${state.reason}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            TextButton(
                                                onClick = { onCancelDownload(model) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Batal", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                    is DownloadState.Verifying -> {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Memverifikasi integritas SHA-256...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    is DownloadState.Failed -> {
                                        Text(
                                            text = "Gagal: ${state.message}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    else -> {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Menghubungkan ke server...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { onDownloadModel(model) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Unduh (${model.sizeDescription})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun PersonaSelectionCard(
    persona: AiPersona,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val (icon, subtitleBadge) = when (persona) {
        AiPersona.MUFTI -> Icons.AutoMirrored.Rounded.MenuBook to "Formal & Shahih"
        AiPersona.USTADZ -> Icons.Rounded.School to "Edukatif & Ramah"
        AiPersona.SAHABAT -> Icons.Rounded.AutoAwesome to "Hangat & Santai"
        AiPersona.CUSTOM -> Icons.Rounded.Tune to "Kustom"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .semantics {
                role = Role.RadioButton
                contentDescription = "${persona.title}: ${persona.description}"
                stateDescription = if (isSelected) "Terpilih" else "Tidak terpilih"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = persona.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = subtitleBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = persona.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title. $subtitle"
            }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = Spacing.md)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
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
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = if (checked) "Aktif" else "Nonaktif"
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
