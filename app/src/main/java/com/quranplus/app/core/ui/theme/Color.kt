package com.quranplus.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Quran Plus Color Tokens
 * Strictly following DESIGN.md and Anti-Slop principles.
 * Primary: Deep Teal (#006B6B)
 * Secondary: Warm Gold (#7A5900)
 * Surface: Warm Near-Black (#0D1415)
 */
object QuranColors {
    // === Primary — Deep Teal ===
    val Primary = Color(0xFF006B6B)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF134E4E)
    val OnPrimaryContainer = Color(0xFFB4F1F1)

    // === Secondary — Warm Gold ===
    val Secondary = Color(0xFFC99700)
    val OnSecondary = Color(0xFF1E1400)
    val SecondaryContainer = Color(0xFF423000)
    val OnSecondaryContainer = Color(0xFFFFDF9E)

    // === Tertiary — Gentle Sage ===
    val Tertiary = Color(0xFF4D7E73)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF22433B)
    val OnTertiaryContainer = Color(0xFFBCEBE0)

    // === Dark Theme Surfaces (Default) ===
    val BackgroundDark = Color(0xFF0D1415)      // Warm near-black
    val SurfaceDark = Color(0xFF141E20)         // Slightly elevated surface
    val SurfaceMedium = Color(0xFF1A2628)       // Card / Dialog surface
    val SurfaceLight = Color(0xFF243436)        // Chip / Selected item
    val SurfaceVariantDark = Color(0xFF243132)  // Search bar / Input bg
    val OnSurfaceDark = Color(0xFFE8F5F5)       // Warm off-white
    val OnSurfaceVariantDark = Color(0xFFA0BBBF)// Muted text
    val OutlineDark = Color(0xFF3B4E50)         // Thin border
    val OutlineVariantDark = Color(0xFF223032)  // Dividers

    // === Light Theme Surfaces ===
    val BackgroundLight = Color(0xFFF0F7F7)
    val SurfaceLightMode = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFE0EEEE)
    val OnSurfaceLight = Color(0xFF0D1415)
    val OnSurfaceVariantLight = Color(0xFF3D5C5E)
    val OutlineLight = Color(0xFFBDD2D4)
    val OutlineVariantLight = Color(0xFFE2ECEE)

    // === Tajwid Colors (High-Contrast & Distinct Standard) ===
    val TajwidGhunnah = Color(0xFFFFA000)       // Amber / Oranye (Ghunnah Musyaddadah - Nun/Mim Tasydid)
    val TajwidIdgham = Color(0xFFBA68C8)        // Ungu / Violet (Idgham Bighunnah)
    val TajwidIdghamBila = Color(0xFF90A4AE)    // Abu-abu / Silver (Idgham Bilaghunnah)
    val TajwidIdghamMimi = Color(0xFFFFD54F)    // Emas / Kuning Cerah (Idgham Mitslain / Mimi)
    val TajwidIqlab = Color(0xFF29B6F6)         // Biru Muda / Cyan (Iqlab - Nun ke Mim)
    val TajwidIkhfa = Color(0xFFEF5350)         // Merah Coral Cerah (Ikhfa Haqiqi)
    val TajwidIkhfaSyafawi = Color(0xFFF06292)  // Merah Muda / Rose Pink (Ikhfa Syafawi)
    val TajwidQalqalah = Color(0xFF4CAF50)      // Hijau Zamrud Terang (Qalqalah Memantul)
    val TajwidIzhar = Color(0xFF78909C)         // Abu-abu Netral (Izhar Jelas)
    val TajwidMad = Color(0xFFB39DDB)           // Lavender Lembut (Mad 'Aridh Lissukun)
    val TajwidMadWajib = Color(0xFFD81B60)      // Merah Marun / Deep Crimson (Mad Wajib/Jaiz 4-5 Harakat)
    val TajwidMadLazim = Color(0xFFC62828)      // Merah Tua Pekat (Mad Lazim 6 Harakat)
    val TajwidIdghamMutajanisain = Color(0xFF26A69A) // Toska / Teal (Idgham Mutajanisain)
    val TajwidIdghamMutaqaribain = Color(0xFF80CBC4) // Mint / Soft Aqua (Idgham Mutaqaribain)

    // === Waqaf Colors ===
    val BadgeWaqafStop = Color(0xFFEF5350)      // Merah (Wajib Berhenti / Dilarang Berhenti)
    val BadgeWaqafContinue = Color(0xFF4CAF50)  // Hijau (Lebih Utama Lanjut)
    val BadgeWaqafOptional = Color(0xFFFFA726)  // Kuning / Oranye (Boleh Berhenti/Lanjut)

    val TextArabicDefault = Color(0xFFFFFFFF)

    // === Semantic ===
    val Success = Color(0xFF66BB6A)
    val Error = Color(0xFFEF5350)
    val Warning = Color(0xFFFFA726)
    val Info = Color(0xFF29B6F6)
}
