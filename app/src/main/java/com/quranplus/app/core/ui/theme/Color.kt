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

    // === Tajwid Colors (IMMUTABLE — standard tajwid visual guidelines) ===
    val TajwidIdgham = Color(0xFF4CAF50)        // Hijau (Merger/Ghunnah)
    val TajwidIdghamBila = Color(0xFF66BB6A)    // Hijau muda (Idgham Bilaghunnah)
    val TajwidIdghamMimi = Color(0xFF43A047)    // Hijau tua (Idgham Mimi)
    val TajwidIkhfa = Color(0xFF42A5F5)         // Biru muda (Samar)
    val TajwidIkhfaSyafawi = Color(0xFF29B6F6)  // Biru kehijauan (Ikhfa Syafawi)
    val TajwidIqlab = Color(0xFFEF5350)         // Merah (Membalik nun ke mim)
    val TajwidQalqalah = Color(0xFFFF7043)      // Oranye (Memantul)
    val TajwidIzhar = Color(0xFF78909C)         // Abu-abu netral (Jelas)
    val TajwidMad = Color(0xFFAB47BC)           // Ungu muda (Mad Thabi'i 2 harakat)
    val TajwidMadWajib = Color(0xFF8E24AA)      // Ungu sedang (Mad Wajib/Jaiz 4-5 harakat)
    val TajwidMadLazim = Color(0xFF6A1B9A)      // Ungu pekat (Mad Lazim 6 harakat)
    val TajwidGhunnah = Color(0xFFEC407A)       // Merah muda (Dengung)

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
