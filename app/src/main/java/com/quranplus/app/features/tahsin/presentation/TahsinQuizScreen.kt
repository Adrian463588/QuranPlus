package com.quranplus.app.features.tahsin.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplus.app.core.ui.components.AppPrimaryButton
import com.quranplus.app.core.ui.components.AppTopBar
import com.quranplus.app.core.ui.theme.Spacing
import com.quranplus.app.core.ui.theme.getQuranArabicStyle

data class QuizQuestion(
    val id: Int,
    val prompt: String,
    val arabicSnippet: String,
    val reference: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object QuizQuestionBank {
    val QUESTIONS = listOf(
        QuizQuestion(
            id = 1,
            prompt = "Apa hukum tajwid pada potongan ayat berikut?",
            arabicSnippet = "مِنْ شَرِّ مَا خَلَقَ",
            reference = "QS. Al-Falaq: 2",
            options = listOf("Idgham Bighunnah", "Ikhfa Haqiqi", "Izhar Halqi", "Iqlab"),
            correctIndex = 1,
            explanation = "Nun sukun (نْ) bertemu huruf Syin (ش) yang merupakan salah satu dari 15 huruf Ikhfa, dibaca samar disertai dengung 2 harakat."
        ),
        QuizQuestion(
            id = 2,
            prompt = "Berapakah jumlah harakat untuk Mad Wajib Muttashil?",
            arabicSnippet = "إِذَا جَاءَ نَصْرُ اللَّهِ",
            reference = "QS. An-Nasr: 1",
            options = listOf("2 Harakat", "4–5 Harakat", "6 Harakat", "1 Harakat"),
            correctIndex = 1,
            explanation = "Mad Wajib Muttashil terjadi ketika huruf Mad bertemu Hamzah dalam satu kata bersambung, wajib dibaca 4 sampai 5 harakat."
        ),
        QuizQuestion(
            id = 3,
            prompt = "Apa tindakan yang dianjurkan ketika menemukan tanda waqaf (م)?",
            arabicSnippet = "الْوَقْفُ اللَّازِمُ (م)",
            reference = "Tanda Waqaf Mushaf",
            options = listOf("Dilarang berhenti", "Diharuskan / Wajib berhenti", "Lebih baik lanjut", "Berhenti di salah satu titik"),
            correctIndex = 1,
            explanation = "Tanda 'م' adalah Waqaf Lazim, di mana pembaca diharuskan berhenti agar tidak merusak kesempurnaan makna ayat."
        ),
        QuizQuestion(
            id = 4,
            prompt = "Apa hukum bacaan pada lafaz 'Kallaa Layumbadzanna' berikut?",
            arabicSnippet = "لَيُنْبَذَنَّ فِي الْحُطَمَةِ",
            reference = "QS. Al-Humazah: 4",
            options = listOf("Ikhfa Syafawi", "Idgham Bilaghunnah", "Iqlab", "Ghunnah"),
            correctIndex = 2,
            explanation = "Nun sukun bertemu Ba (ب) sehingga bunyi Nun diganti menjadi bunyi Mim samar disertai dengung 2 harakat (Iqlab)."
        ),
        QuizQuestion(
            id = 5,
            prompt = "Bagaimanakah cara membaca huruf Qalqalah sukun di akhir ayat (Waqaf)?",
            arabicSnippet = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝",
            reference = "QS. Al-Ikhlas: 1",
            options = listOf("Qalqalah Sughra (Ringan)", "Qalqalah Kubra (Pantulan Kuat)", "Tanpa Pantulan", "Dengung 2 Harakat"),
            correctIndex = 1,
            explanation = "Huruf Qalqalah (Dal) yang diwaqafkan di akhir ayat dibaca dengan Qalqalah Kubra (pantulan lebih kuat dan jelas)."
        ),
        QuizQuestion(
            id = 6,
            prompt = "Apa keunikan bacaan pada kata 'Majreehaa' di QS. Hud: 41?",
            arabicSnippet = "بِسْمِ اللَّهِ مَجْرٜىٰهَا",
            reference = "QS. Hud: 41",
            options = listOf("Isymam", "Tashil", "Imalah", "Naql"),
            correctIndex = 2,
            explanation = "Imalah adalah memiringkan bunyi fathah ke arah kasrah (dibaca 'Majreehaa')."
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TahsinQuizScreen(
    onBackClick: () -> Unit
) {
    val questions = remember { QuizQuestionBank.QUESTIONS }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    val currentQuestion = questions.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Kuis Latihan Tajwid & Waqaf",
                subtitle = "Uji pemahaman kaidah tartil Al-Qur'an",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (isQuizCompleted) {
            QuizResultScreen(
                score = score,
                totalQuestions = questions.size,
                onRestart = {
                    currentIndex = 0
                    selectedOptionIndex = null
                    isSubmitted = false
                    score = 0
                    isQuizCompleted = false
                },
                onBackClick = onBackClick
            )
        } else if (currentQuestion != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.md)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Spacer(modifier = Modifier.height(Spacing.xs))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Soal ${currentIndex + 1} dari ${questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Skor: $score",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Question Prompt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = currentQuestion.prompt,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Arabic Snippet
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentQuestion.arabicSnippet,
                                style = getQuranArabicStyle(24f),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(Spacing.md)
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Rujukan: ${currentQuestion.reference}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Multiple Choice Options
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    currentQuestion.options.forEachIndexed { optIndex, optionText ->
                        val isSelected = selectedOptionIndex == optIndex
                        val isCorrect = optIndex == currentQuestion.correctIndex

                        val containerColor = when {
                            !isSubmitted && isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isSubmitted && isCorrect -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                            isSubmitted && isSelected && !isCorrect -> Color(0xFFC62828).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val borderColor = when {
                            !isSubmitted && isSelected -> MaterialTheme.colorScheme.primary
                            isSubmitted && isCorrect -> Color(0xFF4CAF50)
                            isSubmitted && isSelected && !isCorrect -> Color(0xFFEF5350)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = !isSubmitted) {
                                    selectedOptionIndex = optIndex
                                },
                            color = containerColor
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ('A' + optIndex).toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSubmitted) {
                                    if (isCorrect) {
                                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF4CAF50))
                                    } else if (isSelected) {
                                        Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = Color(0xFFEF5350))
                                    }
                                }
                            }
                        }
                    }
                }

                // Explanation Banner (After Submit)
                AnimatedVisibility(visible = isSubmitted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = if (selectedOptionIndex == currentQuestion.correctIndex) "Benar! 🎉" else "Kurang Tepat",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOptionIndex == currentQuestion.correctIndex) Color(0xFF4CAF50) else Color(0xFFEF5350)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentQuestion.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Action Button
                if (!isSubmitted) {
                    AppPrimaryButton(
                        onClick = {
                            if (selectedOptionIndex != null) {
                                isSubmitted = true
                                if (selectedOptionIndex == currentQuestion.correctIndex) {
                                    score += 10
                                }
                            }
                        },
                        enabled = selectedOptionIndex != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Periksa Jawaban")
                    }
                } else {
                    AppPrimaryButton(
                        onClick = {
                            if (currentIndex + 1 < questions.size) {
                                currentIndex++
                                selectedOptionIndex = null
                                isSubmitted = false
                            } else {
                                isQuizCompleted = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (currentIndex + 1 < questions.size) "Lanjut Soal Berikutnya" else "Lihat Hasil Kuis")
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    score: Int,
    totalQuestions: Int,
    onRestart: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Kuis Selesai!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "Total Skor Anda:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "$score / ${totalQuestions * 10}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        AppPrimaryButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Ulangi Kuis")
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        androidx.compose.material3.OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kembali ke Tahsin")
        }
    }
}
