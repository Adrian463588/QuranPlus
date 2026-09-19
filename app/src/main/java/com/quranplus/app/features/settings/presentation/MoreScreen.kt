package com.quranplus.app.features.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing

import androidx.compose.material.icons.rounded.Quiz

private data class MoreAction(
    val label: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MoreScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWaqaf: () -> Unit,
    onNavigateToGharib: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToQuiz: () -> Unit = {}
) {
    val actions = listOf(
        MoreAction(
            label = "Bookmark",
            description = "Ayat-ayat pilihan yang disimpan",
            icon = Icons.Rounded.Bookmark,
            onClick = onNavigateToBookmarks
        ),
        MoreAction(
            label = "Pengaturan",
            description = "Tampilan, tilawah, dan preferensi AI",
            icon = Icons.Rounded.Settings,
            onClick = onNavigateToSettings
        ),
        MoreAction(
            label = "Panduan Waqaf & Ibtida'",
            description = "Tata cara berhenti dan memulai bacaan",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            onClick = onNavigateToWaqaf
        ),
        MoreAction(
            label = "Ensiklopedia Gharib",
            description = "Bacaan khusus dalam mushaf Al-Qur'an",
            icon = Icons.Rounded.School,
            onClick = onNavigateToGharib
        ),
        MoreAction(
            label = "Audio Murottal",
            description = "Pemutar dan unduhan audio offline",
            icon = Icons.Rounded.Audiotrack,
            onClick = onNavigateToAudio
        ),
        MoreAction(
            label = "Kuis Tajwid & Waqaf",
            description = "Uji pemahaman kaidah tartil",
            icon = Icons.Rounded.Quiz,
            onClick = onNavigateToQuiz
        )
    )

    Scaffold(
        topBar = { AppTopBar(title = "More") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Spacing.md,
                vertical = Spacing.sm
            )
        ) {
            items(actions.size) { index ->
                val action = actions[index]
                ListItem(
                    headlineContent = {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    supportingContent = {
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .heightIn(min = 64.dp)
                        .clickable(onClick = action.onClick)
                        .semantics {
                            contentDescription = "${action.label}: ${action.description}"
                        }
                )
            }
        }
    }
}
