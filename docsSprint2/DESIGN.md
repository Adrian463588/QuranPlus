# DESIGN.md — Quran Plus Design System (Sprint 2)

> **Pedoman Desain & Anti-Slop UI:** Wajib dirujuk dalam merancang dan mengimplementasikan antarmuka Jetpack Compose pada Sprint 2.

---

## 1. Filosofi & Karakter Visual

- **Aura Desain:** Bermartabat, Khusyuk, Kontemporer, dan Bersih (*Quiet & Sacred Elegance*).
- **Inspirasi Utama:** Mushaf cetak berkualitas tinggi dipadukan dengan tipografi modern dan Material 3 Expressive yang terukur.
- **Anti-Slop Directive:** Menolak keras elemen dekoratif yang tidak memiliki tujuan fungsional (tidak ada gradient ungu AI generik, tidak ada card bertumpuk tanpa arti, tidak ada animasi berlebihan).

---

## 2. Sistem Warna (Color Tokens)

Quran Plus menggunakan palet berbasis **OKLCH** dengan kontras tinggi dan kenyamanan membaca jangka panjang (*eye-strain free*).

```kotlin
object QuranColors {
    // === Brand Primary — Deep Islamic Teal ===
    val Primary              = Color(0xFF006B6B)
    val OnPrimary            = Color(0xFFFFFFFF)
    val PrimaryContainer     = Color(0xFF0E3838)
    val OnPrimaryContainer   = Color(0xFF9EF2F2)

    // === Brand Secondary — Warm Gold / Sand ===
    val Secondary            = Color(0xFFC99700)
    val OnSecondary          = Color(0xFF271900)
    val SecondaryContainer   = Color(0xFF3D2E00)
    val OnSecondaryContainer = Color(0xFFFFDEA0)

    // === Surfaces — Dark Mode Default (Warm Near-Black) ===
    val BackgroundDark       = Color(0xFF0D1415) // Bukan pure #000000
    val SurfaceDark          = Color(0xFF141E1F)
    val SurfaceVariantDark   = Color(0xFF1D2A2C)
    val OutlineDark          = Color(0xFF2E4043)

    // === Typography Colors ===
    val TextPrimaryDark      = Color(0xFFE8F5F5)
    val TextSecondaryDark    = Color(0xFFA0BBBF)
    val TextTertiaryDark     = Color(0xFF6B8A8E)
    val TextArabicDefault    = Color(0xFFF2FAF9)

    // === Tajwid Palette (Terstandarisasi & Presisi) ===
    val TajwidGhunnah        = Color(0xFFEC407A) // Pink tua
    val TajwidIdgham         = Color(0xFF4CAF50) // Hijau segar
    val TajwidIdghamBila     = Color(0xFF81C784) // Hijau muda
    val TajwidIdghamMimi     = Color(0xFF2E7D32) // Hijau lumut
    val TajwidIqlab          = Color(0xFFEF5350) // Merah bata
    val TajwidIkhfa          = Color(0xFF42A5F5) // Biru langit
    val TajwidIkhfaSyafawi   = Color(0xFF1E88E5) // Biru safir
    val TajwidQalqalah       = Color(0xFFFF7043) // Oranye koral
    val TajwidIzhar          = Color(0xFF90A4AE) // Abu kebiruan
    val TajwidMad            = Color(0xFFAB47BC) // Ungu lembut
    val TajwidMadWajib       = Color(0xFF8E24AA) // Ungu tua
    val TajwidMadLazim       = Color(0xFF4A148C) // Ungu gelap

    // === Status & Badges ===
    val BadgeWaqafStop       = Color(0xFFE53935)
    val BadgeWaqafContinue   = Color(0xFF43A047)
    val BadgeWaqafOptional   = Color(0xFFFB8C00)
}
```

---

## 3. Sistem Tipografi (Typography)

### 3.1 Pemilihan Font Family
- **Teks Latin UI:** `DM Sans` (Geometrik, bersih, berkarakter kuat; bukan Inter/Roboto generic).
- **Teks Arab (Mushaf Utsmani):** `Hafs KFGQPC` / `Amiri Quran`.
- **Teks Arab (IndoPak / Asia):** `IndoPak Naskh Style`.
- **Transliterasi Fonetis:** `Scheherazade New` (Mendukung harakat & simbol IPA transliterasi).

### 3.2 Skala Tipografi
```kotlin
val QuranTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

## 4. Sistem Spacing & Grid (8dp Scale)

```kotlin
object Spacing {
    val xs   = 4.dp   // Jarak mikro antar elemen sejenis (badge icon)
    val sm   = 8.dp   // Jarak internal dalam komponen
    val md   = 16.dp  // Padding standar screen & card
    val lg   = 24.dp  // Jarak antar seksi dalam halaman
    val xl   = 32.dp  // Header separator
    val xxl  = 48.dp  // Section breathing space
}
```

---

## 5. Komponen UI & Panduan Layar (Sprint 2)

### 5.1 Layar Pembacaan Al-Qur'an (Quran Reader)
- **Komponen Ayah Item:**
  - Tidak dibungkus Card tebal bertumpuk. Gunakan batas visual berbasis `Divider(thickness = 0.5.dp, color = OutlineDark)` dan padding `Spacing.md`.
  - Header ayat: Nomor Ayat dengan lingkaran minimalis di kiri, tombol aksi cepat di kanan.
  - Teks Arab ditampilkan dengan perataan kanan (RTL), line-height lebar (min 52sp) agar tanda waqaf dan harakat tidak bertumpuk.
  - Teks terjemahan dan Latin berjarak `Spacing.sm` di bawah teks Arab dengan warna `TextSecondaryDark`.
- **Mode Kata Demi Kata (Word-by-Word):**
  - Tampilan chip horizontal berurutan untuk setiap lafaz.
  - Setiap chip menampilkan lafaz Arab di atas dan arti bahasa Indonesia di bawahnya.
- **Ayat Action Bottom Sheet:**
  - Desain modular dengan list aksi: Putar Audio, Ulangi, Bookmark, Detail Tafsir, Detail Tajwid, Salin & Bagikan.

### 5.2 Layar Pengaturan & Konfigurasi Tajwid
- **Granular Tajwid Rule Card:**
  - Menampilkan nama hukum, swatch warna tajwid, durasi harakat, dan toggle switch di header card.
  - Deskripsi ringkas cara membaca dan contoh ayat dengan tombol play audio contoh lafaz.
- **Legenda Warna Tajwid:**
  - Bottom sheet ringkas yang memetakan seluruh warna hukum beserta contoh pendeknya.

### 5.3 Layar Direktori Bacaan Gharib & Ayat Sajdah
- **Card Khusus Kasus Langka:**
  - Judul hukum (e.g. `ISYMAM`, `IMALAH`, `SAKTAH`).
  - Referensi `Surah:Ayat` yang bertindak sebagai tombol navigasi langsung (*Deep Link*).
  - Kotak perbandingan cara penulisan vs cara pelafalan.
  - Khusus **Isymam**: Komponen diagram gerakan bibir.
  - Khusus **Sifir Mustatil**: Tombol ganda `▶ Wasal` dan `▶ Waqaf`.
  - Khusus **Ayat Sajdah**: Dialog tata cara Sujud Tilawah dan lafaz doanya.

### 5.4 Layar Panduan Waqaf & Ibtida'
- Grid kartu tanda waqaf dengan badge warna status:
  - Merah: Dilarang berhenti (`لا`).
  - Hijau: Diharuskan/Diutamakan berhenti (`م`, `قلى`).
  - Kuning: Pilihan / Bersyarat (`ج`, `صلى`, `∴ ∴`).

### 5.5 Layar Chatbot AI RAG Lokal
- **Honest State Handling:** Menampilkan banner `MODEL_UNAVAILABLE` bila model `.litertlm` belum diimpor atau diverifikasi.
- **Chat Bubbles:**
  - User: Pojok kanan, latar `PrimaryContainer`, teks `OnPrimaryContainer`.
  - AI Assistant: Pojok kiri, latar `SurfaceVariantDark`, teks `TextPrimaryDark`.
  - Indikator streaming: Animasi denyut titik minimalis saat token dialirkan.
- **Interactive Citation Badge:**
  - Chip sitasi di bagian bawah pesan (e.g. `QS. Al-Baqarah: 255`).
  - Klik chip langsung meluncurkan Quran Reader dan melakukan autoscroll ke ayat rujukan.

### 5.6 Layar Audio Manager & Mini Player Bar
- Bar pemutar audio mengambang (*docked*) di atas navigation bar saat audio aktif.
- Menampilkan nomor ayat, nama Qari, progress slider, tombol Play/Pause, dan speed selector (`0.5x`, `0.75x`, `1.0x`, `1.25x`).
- Halaman Audio Manager: Meteran penggunaan storage lokal dan list surah yang sudah terunduh dengan tombol hapus/unduh batch.

---

## 6. Aturan Ketat Anti-Slop (Anti-Slop Guardrails)

| Pola Dilarang (AI Slop) | Solusi Benar Quran Plus |
|---|---|
| Background gradient ungu-biru generic (`#6366F1`) | Latar gelap hangat konsisten (`#0D1415`) dengan aksen Deep Teal (`#006B6B`). |
| Card di dalam Card di dalam Card (*Card Soup*) | Gunakan hierarki spasi (`Spacing.md`), garis pemisah tipis, atau kontras warna surface. |
| Font generic Inter/Roboto tanpa karakter | Font terkurasi: `DM Sans` (Latin) + `Hafs KFGQPC` (Arab) + `Scheherazade New` (Latin Fonetis). |
| Nilai spasi acak (`7.dp`, `13.dp`, `21.dp`) | Wajib menggunakan token `Spacing.*` berbasis grid 8dp. |
| Icon tanpa label deskriptif | Seluruh icon interaktif wajib memiliki `contentDescription` untuk TalkBack. |
| Dummy metrics atau fake AI confidence bar | Tampilkan metadata riil (nama model, ukuran file, SHA-256, sitasi ayat aktual). |

---

## 7. Checklist Deletion Pass (Wajib untuk Setiap Layar Baru)

Sebelum menyetujui desain antarmuka, ajukan pertanyaan eliminasi berikut:
1. Apakah elemen ini berkontribusi langsung pada pembacaan ayat atau pemahaman materi?
2. Bisakah container Card ini diganti hanya dengan spasi vertikal (`Spacer(Spacing.md)`)?
3. Apakah hanya ada **maksimal 1 Primary CTA** pada layar ini?
4. Apakah seluruh target sentuh berukuran minimal **48dp × 48dp**?
5. Apakah kontras teks terhadap latar belakang mencapai rasio minimal **4.5:1**?
