# ANTISLOP.md — Quran Plus Anti-Slop & Quality Guardrails

> Dokumen ini adalah filter kualitas non-negosiabel untuk mencegah AI Slop, fabrikasi, kode placeholder, dan desain generik pada project **Quran Plus**.

---

## 1. Zero Data Fabrication & Zero Placeholder Policy

1. **NO FAKE DATA**: Dilarang keras menggunakan dummy text (`Lorem Ipsum`, `Dummy Surah`, `Ayat Contoh`), mock database, atau in-memory placeholder stubs.
2. **AUTHENTIC DATA ONLY**: Semua data Al-Qur'an (114 Surah, 6,236 Ayat), Hadis Nawawi, dan kurikulum Tahsin (17 Makharij, 15 Sifat) wajib bersumber langsung dari database SQLite `quranplus.db` di assets/Room.
3. **FAIL FAST**: Jika data tidak ditemukan di database, lemparkan state `UiState.Error` yang bermakna atau `UiState.Empty` dengan pesan informatif, BUKAN data palsu.

---

## 2. Anti-Slop Visual & UI/UX Principles

### 2.1 Authentic Typography
- **DILARANG**: Menggunakan font default sistem generic (Inter, Roboto, Arial) untuk teks Arab Al-Qur'an.
- **WAJIB**: Menggunakan font kaligrafi Uthmani asli (`kitab.ttf` / `uthman.otf`) dengan dukungan harakat lengkap, tashkeel, dan ligatur tajwid.
- **SCALING**: Ukuran font teks Arab wajib dapat diubah dinamis oleh pengguna (20sp – 40sp) secara real-time.

### 2.2 Semantic OKLCH Palette (No Default Purple Gradients)
- **DILARANG**: Background gradasi ungu/biru generic (`#6366f1`), neon borders, atau blur yang berlebihan.
- **WAJIB**: Menggunakan palet bernuansa islami yang hangat dan berkarakter:
  - Deep Emerald (`#0F3A30`)
  - Warm Gold (`#D4AF37`)
  - OLED True Dark (`#08100E` / `#0C1815`) untuk kenyamanan tilawah malam hari.
  - 6 Token Warna Tajwid Semantik (Idgham: Hijau, Ikhfa: Biru, Iqlab: Merah, Qalqalah: Oranye, Mad: Ungu, Ghunnah: Pink).

### 2.3 Layout & Adaptive Multi-Pane
- **CONTENT-FIRST**: Area tilawah Al-Qur'an adalah fokus utama. Sembunyikan bottom bar navigasi pada screen pembaca untuk memaksimalkan kanvas baca.
- **NO UNBOUNDED CARDS**: Pada layar lebar (Tablet, Foldable, Landscape), batasi lebar konten maksimum (`Modifier.widthIn(max = 840.dp)`) dan posisikan di tengah layar (`Alignment.CenterHorizontally`).
- **EDGE-TO-EDGE & INSETS**: Selalu gunakan `enableEdgeToEdge()` dengan penanganan `Modifier.imePadding()` pada textfield dan kontras status bar yang adaptif.

### 2.4 Meaningful Microinteractions & Motion
- **HAPTIC FEEDBACK**: Berikan haptic feedback (`HapticFeedbackType.LongPress` / `TextHandleMove`) saat bookmark, interaksi tombol tajwid, atau penyesuaian font.
- **SPRING TRANSITIONS**: Gunakan transisi spring Material 3 Expressive untuk animasi masuk dan keluar screen.

---

## 3. Clean Architecture & Code Discipline

1. **SOLID & Single Responsibility**: Setiap UseCase hanya menangani 1 operasi domain (e.g. `GetSurahListUseCase`, `SearchAyahsUseCase`, `ToggleBookmarkUseCase`).
2. **DRY (Don't Repeat Yourself)**: Komponen reusable seperti `AppTopBar`, `TajwidText`, `AdaptiveNavigation`, dan `ErrorStateView` dipusatkan di `:core`.
3. **KISS & YAGNI**: Jangan buat abstraksi berlapis yang tidak diperlukan. Cukup ViewModel -> UseCase -> Repository -> Room DAO / LiteRT-LM.
4. **State Management**: Selalu gunakan `StateFlow<UiState<T>>` di ViewModel dan `collectAsStateWithLifecycle()` di Composable. Jangan simpan business logic di dalam `@Composable`.

---

## 4. Quality Audit Checklist

Setiap perubahan kode harus lolos checklist berikut:

- [x] Tidak ada `TODO`, `FIXME`, dummy text, atau placeholder stub.
- [x] Tidak ada hardcoded color di Composable (semua memanggil `MaterialTheme.colorScheme` atau `TajwidColorScheme`).
- [x] Teks Arab menggunakan `QuranFontFamily` / `UthmanFontFamily`.
- [x] Input form mendukung soft keyboard (`imePadding()`).
- [x] Layar lebar tablet tidak membuat teks melar tak berbatas (`widthIn(max)`).
- [x] Unit test domain & ViewModel 100% PASS.
