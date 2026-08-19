package com.quranplus.app.features.waqaf.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle
import com.quranplus.app.core.utils.WaqafParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaqafGuideScreen(
    onBackClick: () -> Unit
) {
    if (!WaqafParser.SOURCE_CATALOG_VERIFIED) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = "Panduan Waqaf & Ibtida'",
                    subtitle = "Menunggu katalog sumber terverifikasi",
                    onBackClick = onBackClick
                )
            }
        ) { padding ->
            AppEmptyState(
                icon = Icons.Rounded.Info,
                title = "Panduan Waqaf diblokir",
                description = "Katalog aturan dan contoh ayat belum memiliki provenance yang direview. Tidak ada penjelasan pengganti yang ditampilkan.",
                modifier = Modifier.padding(padding)
            )
        }
        return
    }

    val waqafRules = WaqafParser.ALL_WAQAF_RULES

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Panduan Waqaf & Ibtida'",
                subtitle = "Tata cara berhenti dan memulai bacaan Al-Qur'an",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Practical Rules Card
            item {
                PracticalWaqafGuideCard()
            }

            item {
                Text(
                    text = "Daftar Tanda Simbol Waqaf Mushaf:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Waqaf Rules Cards
            items(waqafRules, key = { it.symbol }) { rule ->
                WaqafRuleCard(rule = rule)
            }
        }
    }
}

@Composable
private fun PracticalWaqafGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Pedoman Praktis Berwaqaf & Ibtida'",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "1. Waqaf di Akhir Ayat (رَأْسُ الْآيَةِ):\nSenantiasa disunnahkan berwaqaf pada setiap akhir ayat dan melanjutkan ke ayat berikutnya tanpa perlu mengulang.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "2. Ayat Panjang Bertanda Waqaf:\nJika membaca ayat panjang, berhentilah pada tanda waqaf yang tersedia (seperti صلى, ج, قلى) dan lanjutkan kata berikutnya tanpa mengulang.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "3. Kehabisan Nafas di Tengah Ayat (Waqaf Idhthirari):\nBila terpaksa berhenti karena nafas habis pada tempat tanpa tanda waqaf, lakukan Ibtida' dengan mengulang 1–2 kata sebelumnya yang maknanya utuh.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WaqafRuleCard(rule: WaqafParser.WaqafRule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rule.symbol,
                            style = getQuranArabicStyle(22f),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = rule.latinName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = rule.arabicName,
                            style = getQuranArabicStyle(14f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = rule.badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = rule.actionCategory.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = rule.badgeColor,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Arti: ${rule.meaning}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = rule.detailedRule,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Example Verse Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.sm)) {
                    Text(
                        text = rule.exampleAyah,
                        style = getQuranArabicStyle(18f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Contoh: ${rule.exampleRef}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
