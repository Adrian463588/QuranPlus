package com.quranplus.app.core.utils

enum class TajwidRuleId {
    GHUNNAH,
    IDGHAM_BIGHUNNAH,
    IDGHAM_BILAGHUNNAH,
    IDGHAM_MIM_MIMI,
    IDGHAM_MUTAJANISAIN,
    IDGHAM_MUTAQARIBAIN,
    IQLAB,
    IKHFA_HAQIQI,
    IKHFA_SYAFAWI,
    IZHAR_HALQI,
    QALQALAH,
    MAD_TABII,
    MAD_WAJIB_JAIZ,
    MAD_LAZIM,
    MAD_PERMISSIBLE,
    HAMZAT_WASL,
    SILENT,
    LAM_SHAMSIYYAH
}

data class TajwidTagMapping(
    val sourceTag: String,
    val ruleId: TajwidRuleId
)

/** Exact compact tags present in the reviewed Quran asset. */
object TajwidTagCatalog {
    val mappings: List<TajwidTagMapping> = listOf(
        TajwidTagMapping("h", TajwidRuleId.HAMZAT_WASL),
        TajwidTagMapping("s", TajwidRuleId.SILENT),
        TajwidTagMapping("l", TajwidRuleId.LAM_SHAMSIYYAH),
        TajwidTagMapping("n", TajwidRuleId.MAD_TABII),
        TajwidTagMapping("p", TajwidRuleId.MAD_PERMISSIBLE),
        TajwidTagMapping("m", TajwidRuleId.MAD_LAZIM),
        TajwidTagMapping("q", TajwidRuleId.QALQALAH),
        TajwidTagMapping("o", TajwidRuleId.MAD_WAJIB_JAIZ),
        TajwidTagMapping("c", TajwidRuleId.IKHFA_SYAFAWI),
        TajwidTagMapping("f", TajwidRuleId.IKHFA_HAQIQI),
        TajwidTagMapping("w", TajwidRuleId.IDGHAM_MIM_MIMI),
        TajwidTagMapping("i", TajwidRuleId.IQLAB),
        TajwidTagMapping("a", TajwidRuleId.IDGHAM_BIGHUNNAH),
        TajwidTagMapping("u", TajwidRuleId.IDGHAM_BILAGHUNNAH),
        TajwidTagMapping("d", TajwidRuleId.IDGHAM_MUTAJANISAIN),
        TajwidTagMapping("b", TajwidRuleId.IDGHAM_MUTAQARIBAIN),
        TajwidTagMapping("g", TajwidRuleId.GHUNNAH)
    )

    private val byTag = mappings.associateBy { it.sourceTag }

    fun ruleIdFor(sourceTag: String): TajwidRuleId? =
        byTag[sourceTag.lowercase()]?.ruleId
}
