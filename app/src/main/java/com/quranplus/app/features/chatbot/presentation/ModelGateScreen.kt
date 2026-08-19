package com.quranplus.app.features.chatbot.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.chatbot.data.ModelAssetRole
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.chatbot.data.AiBlocker
import com.quranplus.app.features.chatbot.data.AiReadiness
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ModelGateScreen(
    viewModel: ChatViewModel,
    modelRepository: ModelRepository,
    onModelReady: () -> Unit,
    readiness: StateFlow<AiReadiness>
) {
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val aiReadiness by readiness.collectAsStateWithLifecycle()
    val availableModels = remember {
        modelRepository.availableModelConfigs.filter { it.role == ModelAssetRole.CHATBOT }
    }
    val embeddingModels = remember {
        modelRepository.availableModelConfigs.filter { it.role == ModelAssetRole.EMBEDDING }
    }

    if (availableModels.isEmpty()) {
        Scaffold(
            topBar = { AppTopBar(title = "Setup AI On-Device") }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "Model lokal belum tersedia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "ModelGate diblokir: ${aiReadiness.blockers.joinToString(", ") { blockerLabel(it) }}. Katalog, embedding, dan index harus memiliki provenance serta SHA-256 yang direview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var selectedModel by remember {
        mutableStateOf(availableModels.firstOrNull { it.isRecommended } ?: availableModels.first())
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Setup AI On-Device")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // AI Icon & Title
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "Tanya AI Lokal Berbasis RAG",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "Inferensi dirancang berjalan di perangkat setelah model, tokenizer, corpus, dan indeks terverifikasi. Unduhan awal tetap membutuhkan jaringan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Privacy & Architecture Assurance Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Privasi Penuh",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Tanpa Server Luar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "LiteRT-LM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Akselerasi NPU/GPU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Download Status Card or Model Selection
            when (val state = downloadState) {
                is DownloadState.Queued -> {
                    Text(
                        text = "Menyiapkan unduhan ${selectedModel.name}...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                is DownloadState.Transferring -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "Mengunduh ${selectedModel.name}...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            LinearProgressIndicator(
                                progress = { state.progressPercentage / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${state.progressPercentage}% (${state.bytesDownloaded / (1024 * 1024)} MB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${state.speedBytesPerSec / 1024} KB/s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                is DownloadState.Paused -> {
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                is DownloadState.Verifying -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text("Memverifikasi SHA-256 sebelum model diaktifkan")
                }
                is DownloadState.Completed -> {
                    Text(
                        text = "Model siap digunakan!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    AppPrimaryButton(onClick = onModelReady) {
                        Text("Mulai Tanya AI")
                    }
                }
                is DownloadState.ChecksumError -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    AppPrimaryButton(
                        onClick = { viewModel.startModelDownload(selectedModel) },
                        enabled = selectedModel.isDownloadable
                    ) {
                        Text("Coba Lagi")
                    }
                }
                is DownloadState.Failed -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    AppPrimaryButton(
                        onClick = { viewModel.startModelDownload(selectedModel) },
                        enabled = selectedModel.isDownloadable
                    ) {
                        Text("Coba Lagi")
                    }
                }
                is DownloadState.Idle -> {
                    Text(
                        text = "Pilih Model AI yang Diinginkan:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("model_catalog"),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        item {
                            Text(
                                text = "Model chatbot",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(
                            items = availableModels,
                            key = { model -> model.id }
                        ) { model ->
                            ModelSelectCard(
                                model = model,
                                isSelected = model.id == selectedModel.id,
                                onClick = { selectedModel = model }
                            )
                        }
                        if (embeddingModels.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Embedding RAG",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = Spacing.sm)
                                )
                            }
                            items(
                                items = embeddingModels,
                                key = { model -> model.id }
                            ) { model ->
                                ModelCatalogCard(model = model)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    AppPrimaryButton(
                        onClick = { viewModel.startModelDownload(selectedModel) },
                        enabled = selectedModel.isDownloadable,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Unduh Model (${selectedModel.sizeDescription})")
                    }
                    if (!selectedModel.isDownloadable) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Status model terpilih: ${selectedModel.downloadBlocker}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

private fun blockerLabel(blocker: AiBlocker): String = when (blocker) {
    AiBlocker.MODEL_UNAVAILABLE -> "MODEL_UNAVAILABLE"
    AiBlocker.EMBEDDER_UNAVAILABLE -> "EMBEDDER_UNAVAILABLE"
    AiBlocker.INDEX_UNAVAILABLE -> "INDEX_UNAVAILABLE"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelSelectCard(
    model: ModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    val uriHandler = LocalUriHandler.current
    Card(
        onClick = onClick,
        enabled = isInteractive,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (model.isRecommended) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.secondary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Rekomendasi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ukuran: ${model.sizeDescription} • ${model.ramRequirement}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Format: ${model.format} • Runtime: ${model.runtime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = if (model.isDownloadable) {
                        "Manifest lengkap; unduhan melewati gate aplikasi."
                    } else {
                        model.downloadBlocker
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (model.isDownloadable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (model.downloadUrl.startsWith("https://")) {
                        TextButton(
                            onClick = { uriHandler.openUri(model.downloadUrl) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            Text("Buka link unduh")
                        }
                    }
                    TextButton(
                        onClick = { uriHandler.openUri(model.sourceUrl) },
                        enabled = model.sourceUrl.startsWith("https://"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Text("Buka sumber model")
                    }
                    TextButton(
                        onClick = { uriHandler.openUri(model.licenseUrl) },
                        enabled = model.licenseUrl.startsWith("https://"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Text("Buka lisensi")
                    }
                }
            }
        }
    }
}

@Composable
fun ModelCatalogCard(model: ModelInfo, modifier: Modifier = Modifier) {
    ModelSelectCard(
        model = model,
        isSelected = false,
        onClick = {},
        modifier = modifier,
        isInteractive = false
    )
}
