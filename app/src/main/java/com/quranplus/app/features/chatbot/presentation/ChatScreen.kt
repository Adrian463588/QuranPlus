package com.quranplus.app.features.chatbot.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.features.chatbot.domain.ChatMessage
import com.quranplus.app.features.chatbot.domain.MessageRole
import com.quranplus.app.features.rag.domain.RetrievedCitation
import com.quranplus.app.features.settings.data.AiPersona
import com.quranplus.app.features.settings.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    preferencesManager: PreferencesManager,
    onNavigateToAyah: (Int, Int) -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingContent by viewModel.streamingContent.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedPersona by preferencesManager.selectedPersona.collectAsStateWithLifecycle(initialValue = AiPersona.USTADZ)

    var inputPrompt by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size, streamingContent.length) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            listState.animateScrollToItem(index = (messages.size + if (isStreaming) 1 else 0).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tanya AI (${selectedPersona.title})",
                subtitle = "Ground truth: Al-Qur'an, As-Sunnah & RAG lokal",
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = "Hapus Riwayat")
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
            // Chat message list
            if (messages.isEmpty() && !isStreaming) {
                Box(modifier = Modifier.weight(1f)) {
                    AppEmptyState(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Asisten AI Islami Offline",
                        description = "Tanyakan seputar hukum, fiqih, tafsir ayat, atau akhlak. Jawaban hanya tersedia setelah sumber yang relevan dan model terverifikasi siap."
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(items = messages, key = { it.id }) { message ->
                        ChatBubbleItem(
                            message = message,
                                onCitationClick = { citation ->
                                    citation.quranTarget()?.let { (surahNumber, ayahNumber) ->
                                        onNavigateToAyah(surahNumber, ayahNumber)
                                    }
                                }
                        )
                    }

                    // Live Streaming Bubble
                    if (isStreaming) {
                        item {
                            if (streamingContent.isNotEmpty()) {
                                ChatBubbleItem(
                                    message = ChatMessage(
                                        conversationId = "stream",
                                        role = MessageRole.ASSISTANT,
                                        content = streamingContent,
                                        isStreaming = true
                                    ),
                                    onCitationClick = { citation ->
                                        citation.quranTarget()?.let { (surahNumber, ayahNumber) ->
                                            onNavigateToAyah(surahNumber, ayahNumber)
                                        }
                                    }
                                )
                            } else {
                                StreamingIndicator()
                            }
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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
                            Text("Tutup")
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    label = { Text("Pertanyaan agama") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(Spacing.sm))

                IconButton(
                    onClick = {
                        val text = inputPrompt
                        inputPrompt = ""
                        viewModel.sendMessage(text)
                    },
                    enabled = inputPrompt.isNotBlank() && !isStreaming,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputPrompt.isNotBlank() && !isStreaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (isStreaming) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Kirim",
                            tint = if (inputPrompt.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text("Menyiapkan jawaban dari rujukan terverifikasi...", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onCitationClick: (RetrievedCitation) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER

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
                .fillMaxWidth(0.85f)
                .widthIn(max = 640.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = if (isUser) MaterialTheme.shapes.medium else MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                // Render Sourced Citations
                if (message.citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Rujukan Dalil:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.citations.forEach { cite ->
                        CitationChip(
                            citation = cite,
                            onClick = cite.quranTarget()?.let { { onCitationClick(cite) } }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .heightIn(min = 48.dp)
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = if (onClick != null) citation.title else "${citation.title} (sumber belum dapat dibuka)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun RetrievedCitation.quranTarget(): Pair<Int, Int>? {
    if (!sourceType.equals("quran", ignoreCase = true)) return null
    val surah = surahNumber ?: return null
    val ayah = ayahNumber ?: return null
    return (surah to ayah).takeIf { (surahNumber, ayahNumber) ->
        surahNumber in 1..114 && ayahNumber > 0
    }
}
