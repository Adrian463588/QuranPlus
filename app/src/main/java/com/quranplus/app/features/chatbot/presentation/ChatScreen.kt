package com.quranplus.app.features.chatbot.presentation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranplus.app.core.network.DownloadState
import com.quranplus.app.core.ui.components.AppOutlinedButton
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.chatbot.data.ModelAssetRole
import com.quranplus.app.features.chatbot.data.ModelInfo
import com.quranplus.app.features.chatbot.domain.ChatMessage
import com.quranplus.app.features.chatbot.domain.ChatSession
import com.quranplus.app.features.chatbot.domain.MessageRole
import com.quranplus.app.features.hadith.presentation.HadithBundleUiState
import com.quranplus.app.features.hadith.data.HadithBundleWorkState
import com.quranplus.app.features.rag.domain.CitationTarget
import com.quranplus.app.features.rag.domain.GenerationStatus
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.rag.domain.navigationTarget
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.settings.data.PreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    preferencesManager: PreferencesManager,
    onNavigateToAyah: (Int, Int) -> Unit,
    onNavigateToHadith: (String, Int) -> Unit,
    onNavigateToExternalUrl: (String) -> Unit,
    onRequestStorage: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentConversationId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingContent by viewModel.streamingContent.collectAsStateWithLifecycle()
    val streamingCitations by viewModel.streamingCitations.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedPersona by preferencesManager.selectedPersona.collectAsStateWithLifecycle(initialValue = AiPersona.USTADZ)
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val selectedEmbeddingModelId by viewModel.selectedEmbeddingModelId.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val downloadingModel by viewModel.downloadingModel.collectAsStateWithLifecycle()
    val hadithBundleState by viewModel.hadithBundleState.collectAsStateWithLifecycle()
    val onlineResearchEnabled by viewModel.onlineResearchEnabled.collectAsStateWithLifecycle()

    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showBundleBottomSheet by remember { mutableStateOf(false) }

    val activeModel = viewModel.availableModels
        .filter { it.role == ModelAssetRole.CHATBOT }
        .firstOrNull { it.id == selectedModelId }
        ?: viewModel.availableModels
            .filter { it.role == ModelAssetRole.CHATBOT }
            .firstOrNull { viewModel.isModelInstalled(it) }
        ?: viewModel.availableModels.firstOrNull { it.role == ModelAssetRole.CHATBOT }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var inputPrompt by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showClearAllDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    // Auto-scroll to latest message on update
    LaunchedEffect(messages.size, streamingContent.length) {
        val itemCount = messages.size + if (isStreaming) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(
                index = (itemCount - 1).coerceAtLeast(0)
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                ChatHistoryDrawerContent(
                    sessions = sessions,
                    activeConversationId = currentConversationId,
                    onSelectSession = { sessionId ->
                        viewModel.selectSession(sessionId)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        viewModel.createNewSession()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteSession = { sessionId ->
                        sessionToDelete = sessionId
                    },
                    onClearAll = {
                        showClearAllDialog = true
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    activePersona = selectedPersona,
                    activeModelName = activeModel?.name ?: "Model AI",
                    hadithBundleCount = hadithBundleState.localRecordCount,
                    onOpenModelSelector = {
                        coroutineScope.launch {
                            drawerState.close()
                            showModelBottomSheet = true
                        }
                    },
                    onOpenHadithBundle = {
                        coroutineScope.launch {
                            drawerState.close()
                            showBundleBottomSheet = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                AppTopBar(
                    title = "Tanya AI (${selectedPersona.title})",
                    subtitle = "${activeModel?.name ?: "Model AI"} • Local-first",
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Menu Riwayat Obrolan",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showModelBottomSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = "Pilih Model AI",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showBundleBottomSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = "Bundle Hadist",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.createNewSession()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddComment,
                                contentDescription = "Obrolan Baru",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .imePadding()
            ) {
                // Quick Selector Chips Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Model Chip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showModelBottomSheet = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activeModel?.name ?: "Model AI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    // Hadith Bundle Chip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBundleBottomSheet = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (hadithBundleState.localRecordCount > 0) {
                                    "${hadithBundleState.localRecordCount} Hadist"
                                } else {
                                    "Bundle Hadist"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                CompanionSourceBento(
                    onlineResearchEnabled = onlineResearchEnabled,
                    onOnlineResearchChanged = viewModel::setOnlineResearchEnabled
                )

                // Message List or Welcoming Hero
                if (messages.isEmpty() && !isStreaming) {
                    EmptyChatGreeting(
                        persona = selectedPersona,
                        onSuggestionClick = { suggestion ->
                            viewModel.sendMessage(suggestion)
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        items(items = messages, key = { it.id }) { message ->
                            ChatBubbleItem(
                                message = message,
                                onCitationClick = { citation ->
                                    when (val target = citation.navigationTarget()) {
                                        is CitationTarget.Quran -> onNavigateToAyah(
                                            target.surahNumber,
                                            target.ayahNumber
                                        )

                                        is CitationTarget.Hadith -> onNavigateToHadith(
                                            target.collectionId,
                                            target.hadithNumber
                                        )

                                        is CitationTarget.Web -> onNavigateToExternalUrl(target.url)

                                        null -> Unit
                                    }
                                }
                            )
                        }

                        // Live Typing Indicator ("Mengetik...")
                        if (isStreaming) {
                            item {
                                if (streamingContent.isBlank()) {
                                    StreamingIndicator()
                                } else {
                                    ChatBubbleItem(
                                        message = ChatMessage(
                                            id = Long.MIN_VALUE,
                                            conversationId = currentConversationId,
                                            role = MessageRole.ASSISTANT,
                                            content = streamingContent,
                                            citations = streamingCitations,
                                            isStreaming = true
                                        ),
                                        onCitationClick = { citation ->
                                            when (val target = citation.navigationTarget()) {
                                                is CitationTarget.Quran -> onNavigateToAyah(
                                                    target.surahNumber,
                                                    target.ayahNumber
                                                )

                                                is CitationTarget.Hadith -> onNavigateToHadith(
                                                    target.collectionId,
                                                    target.hadithNumber
                                                )

                                                is CitationTarget.Web -> onNavigateToExternalUrl(target.url)

                                                null -> Unit
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Message Card
                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = viewModel::clearError) {
                                Text("Tutup", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                // Refined Modern Chat Input Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputPrompt,
                                onValueChange = { inputPrompt = it },
                                placeholder = {
                                    Text(
                                        text = "Tanyakan seputar Al-Qur'an & Hadits...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp, max = 120.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        val text = inputPrompt.trim()
                                        if (text.isNotBlank() && !isStreaming) {
                                            inputPrompt = ""
                                            viewModel.sendMessage(text)
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.width(Spacing.sm))

                            if (isStreaming) {
                                IconButton(
                                    onClick = { viewModel.stopGeneration() },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = "Hentikan Generasi",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                val isSendEnabled = inputPrompt.isNotBlank()
                                IconButton(
                                    onClick = {
                                        val text = inputPrompt.trim()
                                        if (text.isNotBlank()) {
                                            inputPrompt = ""
                                            viewModel.sendMessage(text)
                                        }
                                    },
                                    enabled = isSendEnabled,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSendEnabled)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Kirim",
                                        tint = if (isSendEnabled)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Model Selection Bottom Sheet
    if (showModelBottomSheet) {
        ModelSelectionModal(
            availableModels = viewModel.availableModels,
            selectedModelId = selectedModelId,
            selectedEmbeddingModelId = selectedEmbeddingModelId,
            downloadState = downloadState,
            downloadingModel = downloadingModel,
            isModelInstalled = viewModel::isModelInstalled,
            onSelectModel = { modelId ->
                viewModel.selectModel(modelId)
            },
            onSelectEmbeddingModel = { modelId ->
                viewModel.selectEmbeddingModel(modelId)
            },
            onDownloadModel = { model ->
                viewModel.startModelDownload(model)
            },
            onCancelDownload = { model ->
                viewModel.cancelModelDownload(model)
            },
            onDismiss = { showModelBottomSheet = false }
        )
    }

    // Hadith Bundle Status Modal
    if (showBundleBottomSheet) {
        HadithBundleModal(
            bundleState = hadithBundleState,
            onRequestStorage = onRequestStorage,
            onDownload = viewModel::startHadithBundleDownload,
            onDismiss = { showBundleBottomSheet = false }
        )
    }

    // Confirmation Dialog for Clearing All Sessions
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Hapus Semua Riwayat Obrolan?") },
            text = { Text("Seluruh riwayat obrolan dengan Asisten AI akan dihapus permanen dari perangkat ini.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllSessions()
                        showClearAllDialog = false
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Deleting Single Session
    sessionToDelete?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Hapus Sesi Obrolan?") },
            text = { Text("Obrolan ini akan dihapus dari daftar riwayat.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(sessionId)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun CompanionSourceBento(
    onlineResearchEnabled: Boolean,
    onOnlineResearchChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Companion local-first",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Quran • Hadist • Dokumen RAG",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (onlineResearchEnabled) "Web fallback" else "Offline saja",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (onlineResearchEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Switch(
                    checked = onlineResearchEnabled,
                    onCheckedChange = onOnlineResearchChanged,
                    modifier = Modifier.semantics {
                        contentDescription = "Aktifkan fallback internet"
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatHistoryDrawerContent(
    sessions: List<ChatSession>,
    activeConversationId: String,
    onSelectSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onClearAll: () -> Unit,
    onCloseDrawer: () -> Unit,
    activePersona: AiPersona,
    activeModelName: String,
    hadithBundleCount: Int,
    onOpenModelSelector: () -> Unit,
    onOpenHadithBundle: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale("id", "ID")) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(Spacing.md)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "QuranPlus AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onCloseDrawer) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Tutup Menu")
            }
        }

        // New Chat Action Button
        OutlinedButton(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.sm),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "Obrolan Baru",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "RIWAYAT OBROLAN",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)
        )

        // Session List
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat obrolan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = sessions, key = { it.conversationId }) { session ->
                    val isSelected = session.conversationId == activeConversationId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    Color.Transparent
                            )
                            .then(
                                if (isSelected)
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                else Modifier
                            )
                            .clickable { onSelectSession(session.conversationId) }
                            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(session.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteSession(session.conversationId) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Hapus Obrolan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

        // Model & Bundle Quick Selectors in Drawer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenModelSelector() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Model AI: $activeModelName",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Ketuk untuk mengganti model",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenHadithBundle() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hadithBundleCount > 0) "Bundle Hadist: $hadithBundleCount Terpasang" else "Bundle Hadist Offline",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "9 Kitab Hadits Lengkap",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Footer Actions
        if (sessions.isNotEmpty()) {
            TextButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Hapus Semua Riwayat",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Persona Chip Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = "Persona Aktif: ${activePersona.title}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TypingDots(
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 5.dp,
    dotColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val dot0OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot0"
    )
    val dot1OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 120),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 240),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot0OffsetY.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .offset(y = dot1OffsetY.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2OffsetY.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatGreeting(
    persona: AiPersona,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "Apa keutamaan membaca Ayat Kursi?",
        "Jelaskan rukun sholat menurut sunnah",
        "Bagaimana urutan wudhu yang benar?",
        "Doa memohon ilmu yang bermanfaat"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            text = "Asisten AI Islami (${persona.title})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "Tanyakan seputar hukum fikih, tafsir ayat Al-Qur'an, dan hadits shahih secara offline di perangkat Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = "Contoh Pertanyaan:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { suggestion ->
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onCitationClick: (RetrievedCitation) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val cursorTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
        }

        Card(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 560.dp)
                .fillMaxWidth(if (isUser) 0.85f else 0.92f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null,
            shape = if (isUser)
                RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
            else
                RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Assistant Header (badge + copy)
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Asisten AI Islami",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!message.isStreaming && message.content.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    isCopied = true
                                    Toast.makeText(context, "Jawaban disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = "Salin Jawaban",
                                    tint = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (!isUser && !message.isStreaming && message.generationStatus != GenerationStatus.COMPLETE) {
                    Text(
                        text = when (message.generationStatus) {
                            GenerationStatus.STOPPED -> "Jawaban dihentikan; teks yang sudah tersedia disimpan."
                            GenerationStatus.TIMEOUT -> "Jawaban melewati batas waktu; periksa rujukan di bawah."
                            GenerationStatus.FALLBACK -> "Jawaban model digantikan rujukan terverifikasi."
                            GenerationStatus.UNAVAILABLE -> "Model tidak tersedia; rujukan tetap ditampilkan."
                            GenerationStatus.STREAMING -> "Jawaban masih diproses."
                            GenerationStatus.COMPLETE -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 7.dp, height = 16.dp)
                                .alpha(cursorAlpha)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                if (message.isStreaming) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        TypingDots(dotSize = 4.dp)
                        Text(
                            text = "Mengetik...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }

                // Render Grounded Citations
                if (message.citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (message.citations.any { it.sourceType.equals("internet", ignoreCase = true) }) {
                            "Rujukan lokal & internet:"
                        } else {
                            "Rujukan Dalil:"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.citations.forEach { cite ->
                        CitationChip(
                            citation = cite,
                            onClick = if (cite.navigationTarget() != null) {
                                { onCitationClick(cite) }
                            } else {
                                null
                            }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CitationChip(
    citation: RetrievedCitation,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val target = citation.navigationTarget()
    val isQuran = target is CitationTarget.Quran
    val isWeb = target is CitationTarget.Web
    val clickAction = onClick?.takeIf { target != null }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (clickAction != null) {
                    "Buka ${citation.title}"
                } else {
                    citation.title
                }
            }
            .clickable(enabled = clickAction != null) { clickAction?.invoke() },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    isQuran -> Icons.AutoMirrored.Rounded.MenuBook
                    isWeb -> Icons.Rounded.Public
                    else -> Icons.Rounded.AutoStories
                },
                contentDescription = null,
                tint = when {
                    isQuran -> MaterialTheme.colorScheme.primary
                    isWeb -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = citation.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (clickAction != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            TypingDots(dotSize = 5.dp)
            Text(
                text = "Mengetik...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionModal(
    availableModels: List<ModelInfo>,
    selectedModelId: String,
    selectedEmbeddingModelId: String = "all-minilm-l6-v2-onnx",
    downloadState: DownloadState,
    downloadingModel: ModelInfo?,
    isModelInstalled: (ModelInfo) -> Boolean,
    onSelectModel: (String) -> Unit,
    onSelectEmbeddingModel: (String) -> Unit,
    onDownloadModel: (ModelInfo) -> Unit,
    onCancelDownload: (ModelInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chatbotModels = availableModels.filter { it.role == ModelAssetRole.CHATBOT }
    val embeddingModels = availableModels.filter { it.role == ModelAssetRole.EMBEDDING }

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
                        text = "Pilih Model AI On-Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Model lokal berjalan offline • web fallback opsional",
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

            // Section 1: LLM Chatbot Models
            Text(
                text = "Model Bahasa (LLM Chatbot):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            chatbotModels.forEach { model ->
                val isInstalled = isModelInstalled(model)
                val isSelected = (model.id == selectedModelId) ||
                    (selectedModelId.isEmpty() && isInstalled && chatbotModels.firstOrNull { isModelInstalled(it) }?.id == model.id)
                ModelCardItem(
                    model = model,
                    isInstalled = isInstalled,
                    isSelected = isSelected,
                    isDownloading = downloadingModel?.id == model.id,
                    downloadState = downloadState,
                    onSelect = { onSelectModel(model.id) },
                    onDownload = { onDownloadModel(model) },
                    onCancelDownload = { onCancelDownload(model) }
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }

            // Section 2: RAG Embedding Models
            if (embeddingModels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Model Pencarian Vektor (RAG Embedding):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                embeddingModels.forEach { model ->
                    val isInstalled = isModelInstalled(model)
                    val isSelected = (model.id == selectedEmbeddingModelId) ||
                        (selectedEmbeddingModelId.isEmpty() && isInstalled && embeddingModels.firstOrNull { isModelInstalled(it) }?.id == model.id)
                    ModelCardItem(
                        model = model,
                        isInstalled = isInstalled,
                        isSelected = isSelected,
                        isDownloading = downloadingModel?.id == model.id,
                        downloadState = downloadState,
                        onSelect = { onSelectEmbeddingModel(model.id) },
                        onDownload = { onDownloadModel(model) },
                        onCancelDownload = { onCancelDownload(model) }
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun ModelCardItem(
    model: ModelInfo,
    isInstalled: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    downloadState: DownloadState,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (isInstalled) {
                    onSelect()
                } else if (model.isDownloadable && !isDownloading) {
                    onDownload()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && isInstalled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
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
                        imageVector = if (model.role == ModelAssetRole.EMBEDDING)
                            Icons.Rounded.Storage
                        else
                            Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = if (isSelected && isInstalled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
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
                            onClick = onSelect,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Pilih Model",
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
                                    onClick = onCancelDownload,
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
                                    onClick = onCancelDownload,
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
                                    onClick = onCancelDownload,
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
            } else if (model.isDownloadable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDownload,
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
            } else {
                Text(
                    text = "Belum tersedia: ${model.downloadBlocker}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBundleModal(
    bundleState: HadithBundleUiState,
    onRequestStorage: () -> Unit,
    onDownload: () -> Unit,
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
                        text = "Bundle Hadist Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Database 9 kitab hadits on-device untuk pencarian dan RAG",
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (bundleState.localRecordCount > 0)
                                "Status: ${bundleState.localRecordCount} Hadits Terpasang"
                            else
                                "Status: Belum Terpasang",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "• 9 Koleksi Kitab: Shahih Bukhari, Muslim, Abu Dawud, Tirmidzi, Nasa'i, Ibnu Majah, Ahmad, Malik, Darimi",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• ${bundleState.localRecordCount} total hadits berindeks lengkap",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Dipakai sebagai rujukan offline setelah checksum dan lisensi tervalidasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            when (val workState = bundleState.workState) {
                is HadithBundleWorkState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { workState.progressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Mengunduh bundle Hadist… ${workState.progressPercentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HadithBundleWorkState.Queued -> Text(
                    text = "Unduhan dijadwalkan dan akan dilanjutkan saat jaringan tervalidasi.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HadithBundleWorkState.Importing -> Text(
                    text = "Memvalidasi dan mengimpor bundle Hadist…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                HadithBundleWorkState.Completed -> Text(
                    text = "Bundle Hadist terpasang dan siap diindeks.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                HadithBundleWorkState.Idle,
                is HadithBundleWorkState.Failed -> Unit
            }

            bundleState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            if (!bundleState.storageLinked) {
                AppPrimaryButton(
                    onClick = onRequestStorage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Pilih Folder SAF")
                }
            } else if (bundleState.workState !is HadithBundleWorkState.Downloading &&
                bundleState.workState !is HadithBundleWorkState.Queued &&
                bundleState.workState !is HadithBundleWorkState.Importing
            ) {
                AppPrimaryButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(if (bundleState.localRecordCount > 0) "Perbarui Bundle" else "Unduh Bundle")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}
