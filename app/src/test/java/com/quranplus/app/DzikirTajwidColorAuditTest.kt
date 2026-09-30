package com.quranplus.app

import com.quranplus.app.core.ui.theme.QuranColors
import com.quranplus.app.features.dzikir.data.DzikirDataCatalog
import com.quranplus.app.features.dzikir.data.DzikirTajwidFormatter
import org.junit.Test

class DzikirTajwidColorAuditTest {

    @Test
    fun auditCatalogTajwidColors() {
        var coloredCount = 0
        var uncoloredCount = 0
        val uncoloredList = mutableListOf<String>()

        DzikirDataCatalog.ALL_ITEMS.forEach { item ->
            val formatted = DzikirTajwidFormatter.formatArabic(
                arabicText = item.arabicText,
                tajwidTags = item.tajwidTags,
                enableTajwid = true
            )
            val nonBaseSpans = formatted.spanStyles.filter { it.item.color != QuranColors.TextArabicDefault }
            if (nonBaseSpans.isNotEmpty()) {
                coloredCount++
            } else {
                uncoloredCount++
                uncoloredList.add("${item.id} (${item.title})")
            }
        }

        println("AUDIT RESULT: colored=$coloredCount, uncolored=$uncoloredCount")
        if (uncoloredList.isNotEmpty()) {
            println("Uncolored items count: ${uncoloredList.size}")
            uncoloredList.take(20).forEach { println(" - $it") }
        }
    }
}
