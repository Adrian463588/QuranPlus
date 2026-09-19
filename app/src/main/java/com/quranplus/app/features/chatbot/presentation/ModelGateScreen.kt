package com.quranplus.app.features.chatbot.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.QuranColors
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.chatbot.data.AiReadiness
import com.quranplus.app.features.chatbot.data.ModelAssetRole
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.chatbot.data.ModelRepository
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ModelGateScreen(
    viewModel: ChatViewModel,
    modelRepository: ModelRepository,
    onModelReady: () -> Unit,
    readiness: StateFlow<AiReadiness>,
    onRequestStorage: (() -> Unit)? = null,
    isStorageReady: Boolean = false
) {
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val downloadingModel by viewModel.downloadingModel.collectAsStateWithLifecycle()
    val aiReadiness by readiness.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val selectedEmbeddingModelId by viewModel.selectedEmbeddingModelId.collectAsStateWithLifecycle()
    var localSelectedEmbeddingModelId by remember(selectedEmbeddingModelId) {
        mutableStateOf(selectedEmbeddingModelId)
    }

    val availableModels = remember {
        modelRepository.availableModelConfigs.filter { it.role == ModelAssetRole.CHATBOT }
    }
    val embeddingModels = remember {
        modelRepository.availableModelConfigs.filter { it.role == ModelAssetRole.EMBEDDING }
    }

    var userSelectedModelId by remember { mutableStateOf<String?>(null) }
    var selectedModel by remember {
        mutableStateOf(
            availableModels.firstOrNull { it.id == selectedModelId && modelRepository.isModelReady(it) }
                ?: availableModels.firstOrNull { modelRepository.isModelReady(it) }
                ?: availableModels.firstOrNull { it.isRecommended }
                ?: availableModels.firstOrNull { it.isDownloadable }
                ?: availableModels.first()
        )
    }

    LaunchedEffect(aiReadiness, selectedModelId) {
        if (userSelectedModelId == null) {
            val persistedReadyModel = availableModels.firstOrNull {
                it.id == selectedModelId && modelRepository.isModelReady(it)
            }
            val firstReadyModel = availableModels.firstOrNull(modelRepository::isModelReady)
            val readyModel = persistedReadyModel ?: firstReadyModel
            if (readyModel != null && readyModel.id != selectedModel.id) {
                selectedModel = readyModel
            }
        } else {
            val userModel = availableModels.firstOrNull { it.id == userSelectedModelId }
            if (userModel != null && userModel.id != selectedModel.id) {
                selectedModel = userModel
            }
        }
    }

    val isSelectedModelReady = modelRepository.isModelReady(selectedModel)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tanya AI On-Device",
                subtitle = "AI companion Islami local-first"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("model_catalog"),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    // Hero Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Text(
                            text = "AI companion Islami local-first",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Text(
                            text = "Tanya jawab berbasis rujukan Quran, Hadist, dan dokumen lokal. Tetap dapat digunakan offline ketika sumber lokal tersedia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Clean Feature Chips Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FeatureBadge(
                                icon = Icons.Rounded.Security,
                                label = "On-device & Privat"
                            )
                            FeatureBadge(
                                icon = Icons.Rounded.Memory,
                                label = "LiteRT-LM On-Device"
                            )
                        }
                    }
                }

                // Storage Permission Status
                item {
                    if (!isStorageReady && onRequestStorage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = QuranColors.Secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Folder Penyimpanan Diperlukan",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pilih folder lokal untuk menyimpan model AI (disarankan folder Dokumen).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                OutlinedButton(
                                    onClick = onRequestStorage,
                                    modifier = Modifier.testTag("select_saf_folder")
                                ) {
                                    Text("Pilih")
                                }
                            }
                        }
                    } else if (isStorageReady) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Penyimpanan lokal siap",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (onRequestStorage != null) {
                                TextButton(
                                    onClick = onRequestStorage,
                                    modifier = Modifier.testTag("select_saf_folder")
                                ) {
                                    Text("Ubah Folder", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Section Header
                item {
                    Text(
                        text = "Pilih Model AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Chatbot Model Options
                items(
                    items = availableModels,
                    key = { model -> model.id }
                ) { model ->
                    val isReady = remember(model.id, aiReadiness) {
                        modelRepository.isModelReady(model)
                    }
                    val isDownloading = downloadingModel?.id == model.id
                    val downloadProgress = (downloadState as? DownloadState.Transferring)?.progressPercentage ?: 0

                    ModelOptionCard(
                        model = model,
                        isSelected = model.id == selectedModel.id,
                        isReady = isReady,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        onClick = {
                            selectedModel = model
                            userSelectedModelId = model.id
                        }
                    )
                }

                if (embeddingModels.isNotEmpty()) {
                    item {
                        Text(
                            text = "Model Embedding RAG",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
                    items(
                        items = embeddingModels,
                        key = { model -> model.id }
                    ) { model ->
                        val isReady = remember(model.id, aiReadiness) {
                            modelRepository.isModelReady(model)
                        }
                        val isDownloading = downloadingModel?.id == model.id
                        val downloadProgress = (downloadState as? DownloadState.Transferring)?.progressPercentage ?: 0

                        ModelOptionCard(
                            model = model,
                            isSelected = model.id == localSelectedEmbeddingModelId || (localSelectedEmbeddingModelId.isEmpty() && isReady),
                            isReady = isReady,
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress,
                            onClick = {
                                localSelectedEmbeddingModelId = model.id
                                if (isReady) {
                                    viewModel.selectEmbeddingModel(model.id)
                                } else if (model.isDownloadable && !isDownloading) {
                                    viewModel.startModelDownload(model)
                                }
                            },
                            onDownload = {
                                localSelectedEmbeddingModelId = model.id
                                viewModel.startModelDownload(model)
                            },
                            onCancelDownload = { viewModel.cancelModelDownload(model) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Bottom Action / Download State Bar
            BottomDownloadControl(
                downloadState = downloadState,
                selectedModel = selectedModel,
                downloadingModel = downloadingModel,
                isSelectedModelReady = isSelectedModelReady,
                isStorageReady = isStorageReady,
                onRequestStorage = onRequestStorage,
                onStartDownload = { viewModel.startModelDownload(selectedModel) },
                onCancelDownload = { viewModel.cancelModelDownload() },
                onSelectAndStart = {
                    viewModel.selectActiveModel(selectedModel)
                    onModelReady()
                }
            )

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun FeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModelOptionCard(
    model: ModelInfo,
    isSelected: Boolean,
    isReady: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    onClick: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onCancelDownload: (() -> Unit)? = null
) {
    val isAvailable = model.isDownloadable
    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("model_card_${model.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isReady) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(QuranColors.Success.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = QuranColors.Success,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Terpasang",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QuranColors.Success,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else if (isDownloading) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Mengunduh $downloadProgress%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    } else if (model.isRecommended) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Rekomendasi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Short Specs / Description
                val specs = when (model.id) {
                    "qwen2.5-1.5b-instruct" -> "Optimal untuk mobile • Respon cepat & hemat RAM (Min. 4 GB)"
                    "alif-islamic-v4-base" -> "Spesialis Fiqih & Q&A Islami • Ringan (Min. RAM 3 GB)"
                    "gemma4-e2b-it" -> "Penalaran mendalam • Analisis komprehensif (Min. RAM 6 GB)"
                    "all-minilm-l6-v2-onnx" -> "Embedding cepat untuk pencarian semantik ayat & hadist (384-dim)"
                    else -> "Format: ${model.format} • Runtime: ${model.runtime}"
                }
                Text(
                    text = specs,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Ukuran: ${model.sizeDescription}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!isAvailable) {
                        Text(
                            text = "• ${model.downloadBlocker}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (onDownload != null && !isReady && isAvailable) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mengunduh $downloadProgress%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            onCancelDownload?.let { cancel ->
                                TextButton(onClick = cancel) {
                                    Text("Batal")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Unduh embedding (${model.sizeDescription})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomDownloadControl(
    downloadState: DownloadState,
    selectedModel: ModelInfo,
    downloadingModel: ModelInfo?,
    isSelectedModelReady: Boolean,
    isStorageReady: Boolean,
    onRequestStorage: (() -> Unit)?,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onSelectAndStart: () -> Unit
) {
    val activeDownloadingModel = downloadingModel

    if (activeDownloadingModel != null && (downloadState is DownloadState.Transferring || downloadState is DownloadState.Queued || downloadState is DownloadState.Verifying || downloadState is DownloadState.Paused)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mengunduh ${activeDownloadingModel.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        when (downloadState) {
                            is DownloadState.Queued -> {
                                Text(
                                    text = "Menyiapkan unduhan...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is DownloadState.Paused -> {
                                Text(
                                    text = "Terjeda: ${(downloadState as DownloadState.Paused).reason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            is DownloadState.Verifying -> {
                                Text(
                                    text = "Memverifikasi integritas SHA-256...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            else -> {}
                        }
                    }
                    if (downloadState is DownloadState.Transferring) {
                        Text(
                            text = "${downloadState.progressPercentage}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (downloadState is DownloadState.Transferring) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { downloadState.progressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${downloadState.bytesDownloaded / (1024 * 1024)} MB / ${activeDownloadingModel.sizeDescription}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${downloadState.speedBytesPerSec / 1024} KB/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Cancel Button
                OutlinedButton(
                    onClick = onCancelDownload,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Batalkan Unduhan", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    } else if (downloadState is DownloadState.Failed || downloadState is DownloadState.ChecksumError) {
        val message = (downloadState as? DownloadState.Failed)?.message
            ?: (downloadState as? DownloadState.ChecksumError)?.message
            ?: "Terjadi kesalahan saat mengunduh model."
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            AppPrimaryButton(
                onClick = onStartDownload,
                enabled = selectedModel.isDownloadable && isStorageReady,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Coba Unduh Lagi")
            }
        }
    } else if (downloadState is DownloadState.Completed) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Model AI Berhasil Terpasang!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            AppPrimaryButton(
                onClick = onSelectAndStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mulai Tanya AI")
            }
        }
    } else {
        // Idle state: Either selected model is already ready, or ready to download
        if (!isSelectedModelReady && !isStorageReady && onRequestStorage != null) {
            AppPrimaryButton(
                onClick = onRequestStorage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Rounded.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Pilih Folder untuk Mengunduh")
            }
        } else if (isSelectedModelReady) {
            AppPrimaryButton(
                onClick = onSelectAndStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Gunakan ${selectedModel.name}")
            }
        } else {
            AppPrimaryButton(
                onClick = onStartDownload,
                enabled = selectedModel.isDownloadable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Rounded.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Unduh Model (${selectedModel.sizeDescription})")
            }
        }
    }
}
