# Quran Plus

<div align="center">

[![Download APK](https://img.shields.io/badge/Download-APK%20v1.0.0-10b981?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Adrian463588/QuranPlus/releases/latest/download/app-debug.apk)
[![Latest Release](https://img.shields.io/github/v/release/Adrian463588/QuranPlus?style=for-the-badge&color=0284c7)](https://github.com/Adrian463588/QuranPlus/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7f52ff?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-4285f4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-API%2032%20(Android%2012)-orange?style=for-the-badge&logo=android)](https://android.com)

**Aplikasi Al-Qur'an 30 Juz, Hadist 9 Kitab, Tahsin, Dzikir & Hizib, serta Asisten AI Islami (Tanya AI) berbasis On-Device RAG Offline-First.**

**Dibuat oleh:** Adrian Syah Abidin

</div>

---

## ⚡ Unduh & Pasang Cepat (Direct Install)

Untuk langsung menginstal aplikasi **Quran Plus** di smartphone atau tablet Android Anda:

1. **[📥 Klik di sini untuk Mengunduh APK v1.0.0](https://github.com/Adrian463588/QuranPlus/releases/latest/download/app-debug.apk)** *(Ukuran: ~237 MB, siap pakai)*.
2. Setelah unduhan selesai, buka file `app-debug.apk` melalui notifikasi browser atau aplikasi **File Manager / Pengelola File**.
3. Jika Android menampilkan peringatan keamanan sumber tidak dikenal, pilih **Setelan / Settings** lalu aktifkan **Izinkan dari sumber ini (Allow from this source)**.
4. Tekan tombol **Pasang / Install**, tunggu hingga proses selesai, dan buka **Quran Plus**.
5. *(Opsional)* Kunjungi halaman [GitHub Releases](https://github.com/Adrian463588/QuranPlus/releases/latest) untuk melihat catatan rilis lengkap atau versi rilis lainnya.

---

## 🌟 Fitur Utama

### 📖 1. Al-Qur'an Al-Karim 30 Juz
- **114 Surah & 6.236 Ayat Lengkap:** Disertai penanda Juz, Halaman, dan Makkiyyah/Madaniyyah.
- **Tajwid Berwarna Interaktif:** Dibuat menggunakan native Compose `AnnotatedString` dengan pewarnaan standar Kemenag RI (Ikhfa, Idgham, Iqlab, Qalqalah, Mad, Gunnah).
- **Word-by-Word (Per Kata):** 77.429 unit sumber menyajikan teks Arab, transliterasi Latin presisi, dan terjemahan per kata berbasis standar [Islamic.app Word API](https://docs.islamic.app/api-reference/words) & Quran.com API v4.
- **Pencarian Kilat FTS5:** Pencarian teks Arab, transliterasi, maupun terjemahan Indonesia menggunakan full-text search SQLite FTS5.
- **Bookmark & Riwayat Baca:** Sinkronisasi posisi terakhir dibaca secara otomatis.

### 📚 2. Ensiklopedia 9 Kitab Hadist
- **38.102 Hadist Terverifikasi:** Mencakup Kutubus Sittah (Bukhari, Muslim, Abu Daud, Tirmidzi, An-Nasa'i, Ibnu Majah) dan Kutubut Tis'ah (Musnad Ahmad, Muwatha' Malik, Sunan Ad-Darimi).
- **Penyimpanan SAF Mandiri:** Arsip hadist diunduh langsung dari [gadingnst/hadith-api](https://github.com/gadingnst/hadith-api) dan disimpan di folder Storage Access Framework (SAF) milik pengguna, aman dari penghapusan saat aplikasi di-uninstall.
- **Integrasi Corpus RAG:** Seluruh hadist yang diunduh langsung diindeks ke dalam vector database lokal.

### 📿 3. Dzikir & Hizib Terlengkap
- **Koleksi Dzikir Harian:**
  - **Al-Ma'tsurat Kubro & Sughro** (Pagi & Petang) susunan Imam As-Syahid Hasan Al-Banna.
  - **Ratib Al-Haddad** karya Al-Imam Al-Habib Abdullah bin Alawi Al-Haddad.
  - **Ratib Al-Attas** karya Al-Imam Al-Habib Umar bin Abdurrahman Al-Attas.
  - **Hizib Bahr & Hizib Nashr** karya Syaikh Abu Hasan Asy-Syadzili.
  - **Hizib Nawawi** karya Al-Imam Yahya bin Syaraf An-Nawawi.
- **Tasbih Digital Interaktif:** Dilengkapi penghitung target per bacaan (3x, 7x, 10x, 100x), feedback haptik, dan reset otomatis.
- **Transliterasi Latin Presisi:** Penerapan kaidah fonetik tajwid dan ikhfa' yang rapi dan mudah dibaca pemula.

### 🎓 4. Belajar Tahsin & Ensiklopedia Gharib
- **54 Materi Tahsin Lengkap:** Pembelajaran sistematis meliputi Makharijul Huruf, Sifatul Huruf, Ahkamut Tajwid, Ahkamul Mad, hingga Kaidah Waqaf & Ibtida'.
- **Ensiklopedia Ayat Gharib:** Penjelasan mendalam beserta cara membaca ayat-ayat unik dalam riwayat Hafsh 'an 'Ashim (Saktah, Imalah, Isymam, Tashil, Naql).
- **Contoh Ayat Nyata:** Setiap kaidah merujuk langsung pada ayat Al-Qur'an yang ada di dalam database.

### 🤖 5. Tanya AI (On-Device Islamic RAG Companion)
- **100% On-Device AI:** Tanya jawab seputar hukum fiqih, tafsir ayat, dan hadits shahih tanpa mengirim data pribadi ke server eksternal, ditenagai oleh **Google AI Edge LiteRT-LM**.
- **Model yang Didukung:**
  - **Gemma 4 E2B IT** (Google LiteRT-LM)
  - **Qwen 2.5 1.5B Instruct** (LiteRT-LM)
  - **Alif Islamic v4 Base** (Model khusus fiqih & konsultasi agama)
- **Vector Retrieval Lokal (RAG):** Menggunakan **sqlite-vec** dan model embedding **all-MiniLM-L6-v2 ONNX** untuk mencari ayat dan hadits paling relevan dengan akurasi tinggi (BM25 + Cosine Similarity).
- **Batas Waktu Komputasi 5 Menit (Strict Timeout Budget):** Menjamin perangkat terhindar dari *thermal throttling* atau *out-of-memory*, dengan penanganan status `GenerationStatus.TIMEOUT` yang menyelamatkan teks parsial yang sudah terbentuk.
- **Pemrosesan Tipografi Otomatis (`AiResponsePostProcessor`):**
  - Membersihkan artefak model seperti tanda bintang ganda/tiga (`***Heading:***` → `**Heading:**`).
  - Pemisahan baris otomatis sebelum subheading tebal agar struktur jawaban rapi.
  - *Conjunction-aware sentence truncation* memotong kata sambung menggantung (`dan`, `atau`, `karena`, dll.) jika generasi terhenti.
  - Penyeimbangan tanda baca dan penutup delimiter (`**`, `()`, `""`).
- **Material 3 Rich Markdown:** Teks jawaban AI dirender secara visual menggunakan `ChatMarkdownText` tanpa memunculkan karakter mentah (`###`, `**`).
- **Verifikasi Rujukan & Fallback Internet:** AI memprioritaskan dalil shahih lokal; jika informasi lokal tidak mencukupi, sistem dapat melakukan pencarian web terverifikasi (DuckDuckGo Search) sebagai cadangan.

### 🎧 6. Audio Murottal Per Ayat
- **Qari Ternama:** Mishary Rashid Alafasy, Mahmud Khalil Al-Husary, dan Abdurrahman As-Sudais.
- **Download Resumable:** Menggunakan HTTP Range request, verifikasi MD5 checksum otomatis dari katalog [EveryAyah](https://everyayah.com/recitations_ayat.html), dan pemutaran offline melalui background service.

### 📂 7. Penyimpanan SAF (Storage Access Framework)
Penyimpanan model AI, indeks RAG, dan bundle Hadist dialokasikan ke folder pengguna pilihan Anda:
```text
QuranPlus/
├── models/             # File bobot model LLM & ONNX (.litertlm, .onnx)
├── rag/
│   ├── source/hadith/  # Arsip JSON hadist 9 kitab
│   └── index/          # Database vektor sqlite-vec
└── manifests/          # Verifikasi checksum SHA-256
```
Dengan skema ini, aset berukuran besar tidak akan terhapus secara tidak sengaja saat aplikasi diperbarui atau di-reinstall.

---

## 🤖 Model AI yang Dapat Diunduh

Semua model diverifikasi dengan checksum SHA-256 sebelum dapat dijalankan:

| Nama Model | Runtime / Engine | Ukuran | Sumber / Lisensi | Checksum (SHA-256) |
|---|---|---|---|---|
| **Qwen 2.5 1.5B Instruct** | LiteRT-LM | 1.56 GB | [HuggingFace](https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct) | `98c289e1c43cc592ac535594d5de4bdde449e8dc012ac66909064b6880f8b717` |
| **Alif Islamic v4 Base** | LiteRT-LM | 946 MB | [HuggingFace](https://huggingface.co/ahmedtamseer3/alif-islamic-v4-base) | `79deeca9f2120c08454ccb09f0399b42d4b3146e8d3fdc0bde4cfa2787f2bbaa` |
| **Gemma 4 E2B IT** | LiteRT-LM | 2.58 GB | [HuggingFace](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) | `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` |
| **all-MiniLM-L6-v2 ONNX** | ONNX Runtime | 23 MB | [HuggingFace](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2) | `4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474` |

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa:** Kotlin 2.0+ (100% modern Kotlin)
- **UI Toolkit:** Jetpack Compose + Material Design 3 (M3) dengan dukungan Adaptive Layout (Ponsel, Tablet Portrait, dan Tablet Landscape)
- **Arsitektur:** Clean Architecture + MVVM + Modular Pattern
- **Injeksi Dependensi:** Koin DI
- **Database Lokal:** Room Database 2.7+ dengan SQLite FTS5 Full-Text Search
- **Vector Database:** sqlite-vec untuk mobile vector embedding retrieval
- **Asynchronous:** Kotlin Coroutines & Reactive StateFlow
- **Jaringan & Latar Belakang:** Ktor HTTP Client (HTTP Range) & AndroidX WorkManager

---

## 💻 Cara Build dari Source Code

Pastikan Anda telah memasang **Android Studio Ladybug / Koala** atau yang lebih baru dengan **JDK 17** dan **Android SDK 35**:

```powershell
# 1. Clone repositori
git clone https://github.com/Adrian463588/QuranPlus.git
cd QuranPlus

# 2. Compile modul
.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --console=plain
.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain

# 3. Jalankan unit test
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain

# 4. Build APK Debug
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain

# 5. Pasang ke perangkat yang terhubung (ADB)
.\gradlew.bat installDebug
```

---

## 📱 Preview Aplikasi di Tablet & Smartphone

Tampilan antarmuka responsif Material 3 yang dioptimalkan untuk perangkat layar compact hingga tablet layar besar (teruji pada Samsung Galaxy Tab S7):

| Al-Qur'an Home | Reader Tajwid & Audio | Tanya AI (RAG Terverifikasi) |
|:---:|:---:|:---:|
| ![Quran Home](art/device-sm-g988b-current-home.png) | ![Reader](art/device-sm-g988b-current-reader.png) | ![Tanya AI](art/device-sm-g988b-current-ai.png) |

| Hadist 9 Kitab | Dzikir & Hizib | Materi Tahsin |
|:---:|:---:|:---:|
| ![Hadist](art/device-sm-g988b-current-hadith.png) | ![Word-by-word](art/device-sm-g988b-current-word.png) | ![Tahsin](art/screenshot_tahsin_home.png) |

---

## 📜 Lisensi & Sumber Referensi

1. **Al-Qur'an & Tajwid:** Mengikuti standar mushaf Kementerian Agama Republik Indonesia (Kemenag RI) dan [Quran Foundation API](https://api-docs.quran.com/).
2. **Word-by-Word:** Data bersumber dari [Islamic.app Word API](https://docs.islamic.app/api-reference/words).
3. **Koleksi Hadist:** Menggunakan format terstruktur Arab-Indonesia dari [gadingnst/hadith-api](https://github.com/gadingnst/hadith-api).
4. **Audio Murottal:** Sumber rekaman audio dan checksum MD5 EveryAyah ([EveryAyah.com](https://everyayah.com/recitations_ayat.html)).
5. **Model AI:** Google Gemma Terms of Use ([ai.google.dev/gemma/terms](https://ai.google.dev/gemma/terms)) dan Apache 2.0 untuk Qwen 2.5 & MiniLM.

---

<div align="center">
<b>Quran Plus — Membaca, Memahami, dan Mengamalkan Al-Qur'an & Sunnah dengan Bimbingan Teknologi Cerdas.</b>
</div>
