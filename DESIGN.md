# DESIGN.md — Quran Plus Design System

> Baca DESIGN.md ini WAJIB sebelum menghasilkan UI apapun.
> File ini adalah filter anti-slop dan panduan desain visual aplikasi Quran Plus.

---

## 1. Identitas & Kepribadian Visual

**Nama App:** Quran Plus
**Kata Kunci:** Tenang — Bermartabat — Kontemporer — Islami
**Mood:** Seperti membuka mushaf berkualitas tinggi dengan cahaya redup di malam hari
**Bukan:** Aplikasi gaming / e-commerce / fintech

### Referensi Visual (Gunakan sebagai inspirasi, bukan duplikat)
- Mushaf Al-Quran edisi premium (bersih, bermargin besar, tipografi terfokus)
- Aplikasi baca seperti Kindle (minimalis, konten sebagai fokus)
- Notion Dark Mode (hierarchy jelas, spacing konsisten)

---

## 2. Color System

### 2.1 Palette Utama
Gunakan **OKLCH** untuk semua definisi warna (aksesibilitas lebih baik dari HSL).

```kotlin
// Quran Plus Color Tokens
object QuranColors {

    // === Primary — Deep Teal (Bukan purple-biru default AI) ===
    val Primary       = Color(0xFF006B6B)   // oklch(45% 0.12 195deg)
    val OnPrimary     = Color(0xFFFFFFFF)
    val PrimaryContainer  = Color(0xFF9EF2F2) // oklch(90% 0.08 195deg)
    val OnPrimaryContainer = Color(0xFF002020)

    // === Secondary — Warm Gold (Islami, bermartabat) ===
    val Secondary     = Color(0xFF7A5900)   // oklch(42% 0.12 75deg)
    val OnSecondary   = Color(0xFFFFFFFF)
    val SecondaryContainer  = Color(0xFFFFDEA0) // oklch(90% 0.08 75deg)
    val OnSecondaryContainer = Color(0xFF271900)

    // === Surface — Warm Near-Black (bukan pure black) ===
    val SurfaceDark   = Color(0xFF0D1415)   // Hijau gelap kebiruan
    val SurfaceMedium = Color(0xFF1A2526)
    val SurfaceLight  = Color(0xFF243132)

    // === Tajwid Colors (IMMUTABLE — jangan ganti) ===
    val TajwidIdgham  = Color(0xFF4CAF50)   // Hijau
    val TajwidIkhfa   = Color(0xFF42A5F5)   // Biru muda
    val TajwidIqlab   = Color(0xFFEF5350)   // Merah
    val TajwidQalqalah = Color(0xFFFF7043)  // Oranye
    val TajwidMad     = Color(0xFFAB47BC)   // Ungu muda
    val TajwidGhunnah = Color(0xFFEC407A)   // Merah muda

    // === Semantic ===
    val Success  = Color(0xFF66BB6A)
    val Error    = Color(0xFFEF5350)
    val Warning  = Color(0xFFFFA726)
    val Info     = Color(0xFF29B6F6)
}
```

### 2.2 Dark Mode (Default)
Quran Plus menggunakan **dark mode sebagai default** — mencerminkan pengalaman membaca di malam hari, menghemat baterai OLED, dan terasa lebih premium.

| Role | Value |
|------|-------|
| Background | `#0D1415` (warm near-black, BUKAN pure #000000) |
| Surface | `#1A2526` |
| Surface Variant | `#243132` |
| Primary Text | `#E8F5F5` (warm off-white) |
| Secondary Text | `#A0BBBF` |
| Disabled Text | `#5A7A7E` |

### 2.3 Light Mode
| Role | Value |
|------|-------|
| Background | `#F0F7F7` (warm near-white) |
| Surface | `#FFFFFF` |
| Surface Variant | `#E0EEEE` |
| Primary Text | `#0D1415` |
| Secondary Text | `#3D5C5E` |

### 2.4 Aturan Warna (DILARANG keras)
- JANGAN gunakan `#6366F1` (Tailwind Indigo-500 — AI slop default)
- JANGAN gunakan purple-to-blue gradient sebagai background utama
- JANGAN gunakan pure black `#000000` atau pure white `#FFFFFF` sebagai background
- JANGAN tambahkan hardcoded hex di dalam feature composable — selalu gunakan `MaterialTheme.colorScheme.*`

---

## 3. Typography System

### 3.1 Font Pilihan

**JANGAN gunakan:** Inter, Roboto (default), Open Sans, Lato, Space Grotesk (AI slop fonts)

**Gunakan:**

| Fungsi | Font | Alasan |
|--------|------|--------|
| **UI Latin** | `DM Sans` | Geometrik modern, karakter unik, tidak generik |
| **Arabic (Quran)** | `Hafs KFGQPC` atau `Amiri Quran` | Font Uthmani resmi, terbaca |
| **Arabic (UI)** | `Noto Naskh Arabic` | Konsisten untuk label/UI |
| **Transliterasi** | `Scheherazade New` | Mendukung diakritik fonetis Arab |
| **Monospace** | `JetBrains Mono` | Untuk kode/referensi jika dibutuhkan |

### 3.2 Type Scale (Material 3)

```kotlin
val QuranPlusTypography = Typography(
    // Judul screen utama
    displayLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp
    ),
    // Judul surah
    headlineLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp
    ),
    // Teks UI umum (nama fitur, label)
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    // Body terjemahan
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Label tombol, chip
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    )
)
```

### 3.3 Arabic Quran Text Scale

```kotlin
// Ukuran teks Arab disesuaikan per preferensi user (default: 28sp)
// Rentang: 20sp - 40sp
val QuranArabicStyle = TextStyle(
    fontFamily = HafsQFGQPC,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = 54.sp,      // Jarak antar baris luas untuk harakat
    textDirection = TextDirection.Rtl
)

val TransliterationStyle = TextStyle(
    fontFamily = ScheherazadeNew,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 22.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

### 3.4 Aturan Typography
- JANGAN ciptakan ukuran font baru di dalam feature screen
- JANGAN gunakan lebih dari 4 level emphasis per screen
- SELALU gunakan `MaterialTheme.typography.*` token
- Teks HARUS menggunakan `sp` unit (bukan `dp`) agar mengikuti preferensi aksesibilitas user

---

## 4. Spacing System (8dp Grid)

```kotlin
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}
```

### Aturan Spacing
- JANGAN gunakan nilai arbitrary (7.dp, 13.dp, 19.dp)
- Gunakan SELALU dari `Spacing` object di atas
- **Small gap** (4-8dp) = elemen dalam satu grup
- **Medium gap** (16dp) = grup terkait
- **Large gap** (24-32dp) = seksi berbeda

---

## 5. Shape System

```kotlin
val QuranPlusShapes = Shapes(
    // Tombol kecil, chip
    extraSmall = RoundedCornerShape(8.dp),
    // Card, dialog kecil
    small = RoundedCornerShape(12.dp),
    // Card utama, bottom sheet handle
    medium = RoundedCornerShape(16.dp),
    // Modal dialog, expanded card
    large = RoundedCornerShape(24.dp),
    // Full bottom sheet
    extraLarge = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
)
```

**Aturan Shape:**
- Maksimum 3 nilai corner radius berbeda per screen
- Jangan gunakan fully rounded pill shape untuk container konten besar
- FAB menggunakan `CircleShape` atau `RoundedCornerShape(16.dp)` (M3 Extended FAB)

---

## 6. Elevation & Shadow

```kotlin
// Gunakan M3 elevation tones, bukan drop shadows
// Level elevation:
// 0dp = Surface (background)
// 1dp = Bottom nav bar
// 2dp = Card, list item
// 4dp = Top app bar scrolled
// 8dp = FAB, dialogs, bottom sheets

// JANGAN tambahkan custom drop shadows
// JANGAN gunakan elevation > 8dp tanpa alasan kuat
```

**Aturan Elevation:**
- Maksimum 2-3 level elevation per screen
- Jangan nested Cards (Card di dalam Card)
- Gunakan warna surface berbeda (surfaceVariant) sebelum menambah elevation

---

## 7. Icon Policy

- Gunakan **Material Icons** (rounded variant) untuk konsistensi
- Jangan mix icon style (outlined + filled + sharp di screen yang sama)
- Icon tanpa label HARUS memiliki `contentDescription`
- Ukuran icon: 24dp (standard), 20dp (inline), 32dp (featured)
- Jangan gunakan lebih dari 1 icon per tombol kecuali benar-benar diperlukan
- Icon TIDAK menggantikan teks label pada navigasi utama

---

## 8. Motion & Animation

### Aturan Wajib
- Animasi harus **menjelaskan perubahan state**, bukan dekorasi
- Satu animasi yang tepat waktu > lima animasi serentak
- Jangan: setiap elemen fade-in saat scroll
- Jangan: gradient bergerak terus-menerus
- Jangan: icon pulse tanpa tujuan

### Animasi yang Diizinkan
```kotlin
// Navigasi antar screen: shared element transition (M3)
// Bottom sheet: spring animation (default M3)
// List item tambah/hapus: AnimatedVisibility dengan Crossfade
// AI response streaming: text yang muncul karakter per karakter (fade)
// Loading state: CircularProgressIndicator (tidak ada Lottie yang berlebihan)
// Bookmark: scale pulse saat ditap (one-shot, 150ms)
```

### Durasi
| Jenis | Durasi |
|-------|--------|
| Micro-interaction (tap, toggle) | 100-150ms |
| Transisi elemen | 200-250ms |
| Dialog/sheet open/close | 300-350ms |
| Page transition | 350-400ms |

---

## 9. Screen Hierarchy Rules

### Per-Screen Budget

| Property | Default |
|----------|---------|
| Primary CTA | Maksimum 1 |
| Strong accent color | 1 family |
| Typography emphasis levels | 3-4 |
| Surface elevation levels | Maks 2-3 |
| Nested cards | DILARANG |
| Gradient | Tidak ada (kecuali tajwid legend) |
| Dekoratif illustrations | 0 (kecuali onboarding) |
| FAB | Hanya jika benar-benar diperlukan |

### Hierarchy Level
```
Level 1 — Primary content / purpose screen
Level 2 — Primary action
Level 3 — Supporting information
Level 4 — Secondary actions
Level 5 — Metadata / tertiary
```

---

## 10. Navigation Pattern

```
Compact (phone):
  -> NavigationBar (bottom) — maks 5 destinations

Medium (foldable, landscape):
  -> NavigationRail (left)

Expanded (tablet):
  -> NavigationDrawer (permanent)
```

**Primary Navigation Destinations:**
1. Al-Quran (Beranda Quran)
2. Chatbot AI
3. Tahsin
4. Bookmark
5. Pengaturan

**Aturan Navigasi:**
- Jangan ciptakan custom floating navigation tanpa UX evidence
- Primary destination HARUS memiliki icon + label
- Gunakan `rememberNavController()` dengan NavHost standar

---

## 11. Screen-Specific Guidelines

### 11.1 Quran Reader Screen
- Konten Arab HARUS menjadi **fokus visual dominan** (ukuran besar, center)
- Terjemahan dan transliterasi secara visual HARUS lebih kecil dari teks Arab
- Tidak ada Card per ayat — gunakan `Divider` tipis atau spacing saja
- Bottom bar: kontrol minimalis (bookmark, share) — jangan overflow buttons
- Mode baca penuh: hilangkan nav bar dengan `Immersive Mode`

```
LAYOUT AYAH:
[Nomor Ayah]                    [Icon Bookmark]
[Teks Arab — ukuran besar, RTL, tajwid berwarna]
[Transliterasi — kecil, muted]
[Terjemahan — sedang, readable]
[Divider tipis]
```

### 11.2 Chatbot Screen
- Chat bubble milik user: surface kanan, warna `PrimaryContainer`
- Chat bubble AI: surface kiri, warna `SurfaceVariant`
- Source citation: di bawah chat bubble AI, font lebih kecil, tap-able
- Streaming indicator: 3 titik animasi (dots) selama AI masih generate
- Input field: `OutlinedTextField` standar M3, dengan Send button
- ModelGate: full-screen state jika model belum tersedia

```
LAYOUT CHAT BUBBLE AI:
+-- SurfaceVariant background --+
| [Quran Plus AI Icon]           |
| Teks jawaban AI...             |
|                                |
| Sumber:                        |
| > QS. Al-Baqarah: 255         |  <- tap untuk navigasi ke ayat
| > HR. Bukhari No. 1           |
+--------------------------------+
```

### 11.3 Tahsin Screen
- Struktur seperti buku: daftar bab di kiri (NavigationRail/drawer) atau tab di atas
- Per aturan: Huruf Arab besar + transliterasi + penjelasan + contoh ayat
- Progress indicator per bab yang sudah dibaca
- Tidak ada animasi berlebihan — konten adalah prioritas

### 11.4 Bookmark Screen
- List sederhana — LazyColumn
- Per item: Nama Surah + Nomor Ayat + Cuplikan teks + Waktu bookmark
- Swipe-to-delete dengan undo Snackbar
- Empty state yang jelas dan informatif (bukan hanya "No data")

### 11.5 Settings Screen
- Grouped list dengan section header
- Jangan tampilkan semua setting sekaligus — progressive disclosure
- Bagian: Tampilan | Quran | AI Model | Persona | Tentang

---

## 12. Anti-Slop Checklist (Wajib sebelum ship UI)

Sebelum menganggap UI selesai, verifikasi:

- [ ] Font bukan Inter / Roboto default / Arial / Space Grotesk
- [ ] Tidak ada purple-to-blue gradient sebagai background
- [ ] Tidak ada emoji sebagai icon (kecuali brand memang pakai emoji)
- [ ] Tidak ada rounded card dengan left-border accent stripe
- [ ] Tidak ada "gradient orb" dekoratif yang mewakili AI
- [ ] Spacing menggunakan sistem konsisten (8dp grid dari `Spacing` object)
- [ ] Warna intentional — bukan default Tailwind atau Material tanpa customisasi
- [ ] Layout tidak menggunakan template: centered-hero + 3 cards + CTA
- [ ] Motion purposeful — tidak scattered
- [ ] Konten adalah REAL, bukan placeholder "Lorem ipsum"
- [ ] Semua interactive elements: min touch target 48dp x 48dp
- [ ] Contrast ratio teks minimal 4.5:1 (normal text), 3:1 (large text)
- [ ] Tidak ada nested Card (Card di dalam Card)
- [ ] Hanya 1 primary CTA per screen
- [ ] Screen memiliki 1 dominant visual purpose

---

## 13. Accessibility Requirements

| Requirement | Nilai |
|-------------|-------|
| Touch target minimum | 48dp x 48dp |
| Text contrast (normal) | >= 4.5:1 |
| Text contrast (large/bold) | >= 3:1 |
| Text unit | `sp` (bukan `dp`) |
| Content description | Wajib untuk semua icon tanpa teks |
| Color-only state | DILARANG — selalu kombinasikan dengan shape/text |
| Font scaling | Test hingga 200% font size |

---

## 14. Deletion Pass (Wajib setelah generate UI)

Setelah menghasilkan setiap screen, lakukan pass ini:

Untuk setiap elemen yang terlihat, tanyakan:
1. Apakah ini mendukung task utama user di screen ini?
2. Apakah ini mengkomunikasikan informasi yang diperlukan?
3. Apakah ini mengkomunikasikan hierarchy atau state?
4. Apakah menghapusnya akan mengurangi pemahaman?
5. Bisakah spacing menggantikan container ini?
6. Bisakah tipografi menggantikan divider ini?
7. Bisakah satu action menggantikan multiple actions?
8. Bisakah informasi ini muncul belakangan (progressive disclosure)?

**Hapus semua yang gagal test.**

---

## 15. Component Hierarchy (Prioritas Penggunaan)

```
1. Material 3 standard component (ExposedDropdownMenuBox, etc.)
   |
   v (jika tidak cukup)
2. Configured Material component (dengan parameter theme)
   |
   v (jika tidak cukup)
3. Shared App component (AppButton, AppListItem, dll.)
   |
   v (hanya jika benar-benar diperlukan)
4. Custom primitive composable
```

**JANGAN buat custom button hanya untuk novelty visual.**

---

## 16. App Design System File Structure

```
shared/commonMain/ui/theme/
  Color.kt          -> QuranColors, tajwid colors
  Type.kt           -> QuranPlusTypography
  Shape.kt          -> QuranPlusShapes
  Spacing.kt        -> Spacing object (4/8/16/24/32/48dp)
  Theme.kt          -> QuranPlusTheme (dark + light)

shared/commonMain/ui/components/
  AppButton.kt      -> Primary, Secondary, Ghost variants
  AppTopBar.kt      -> Consistent top bar
  AppListItem.kt    -> Quran list item, tahsin list item
  AppEmptyState.kt  -> Empty states yang informatif
  AyahCard.kt       -> Ayah reader component
  ChatBubble.kt     -> AI & User chat bubbles
  ModelGate.kt      -> LLM model gating screen
```
