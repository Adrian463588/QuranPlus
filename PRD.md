# PRD — Quran Plus
**Platform:** Android (Jetpack Compose KMM Multiplatform)
**Target Device:** Poco X7 Pro (Dimensity 9300+, 8–12 GB RAM) dan perangkat serupa
**Versi:** 1.0.0
**Tanggal:** 2026-08-19

---

## 1. Ringkasan Produk

Quran Plus adalah aplikasi **offline-first** Android yang menggabungkan pengalaman membaca Al-Quran lengkap dengan asisten AI lokal berbasis RAG (Retrieval-Augmented Generation). Seluruh pemrosesan terjadi **di perangkat**—tidak ada data yang pernah meninggalkan perangkat pengguna.

### Nilai Utama
| Pilar | Deskripsi |
|-------|-----------|
| **Privasi Absolut** | Tidak ada koneksi internet setelah setup awal. Tidak ada API key eksternal. |
| **Al-Quran Lengkap** | Tajwid berwarna, transliterasi, terjemahan, pencarian FTS5. |
| **AI Islami Lokal** | Chatbot RAG berbasis Quran + Sunnah sebagai ground truth. |
| **Tahsin Interaktif** | Panduan makharij, sifat huruf, dan aturan tajwid dengan transliterasi. |

---

## 2. Tujuan Produk

### 2.1 Tujuan Bisnis
- Menjadi aplikasi Quran + AI Islami terlengkap yang berjalan **sepenuhnya offline**.
- Membangun kepercayaan pengguna Muslim yang sensitif terhadap privasi data.
- Memberikan nilai lebih dibanding aplikasi Quran biasa melalui fitur AI dan Tahsin.

### 2.2 Tujuan Pengguna
- Membaca Al-Quran dengan pemahaman lebih baik (tajwid berwarna, transliterasi, terjemahan).
- Bertanya tentang agama Islam dengan jawaban bersumber dari Quran dan Sunnah yang sahih.
- Memperbaiki bacaan Quran melalui panduan Tahsin interaktif.

### 2.3 Success Metrics
| Metrik | Target |
|--------|--------|
| Waktu respons chatbot (first token) | < 3 detik |
| Akurasi retrieval RAG (konteks relevan) | >= 85% |
| Cold start app | < 2 detik |
| Frame drop rate pada scrolling Quran | < 1% |
| Ukuran APK (tanpa model) | < 80 MB |

---

## 3. Pengguna Target

### Persona Utama
- **Pelajar Quran** (15-35 tahun): membutuhkan transliterasi dan terjemahan untuk memahami makna.
- **Penghafal/Tahfidz** (semua usia): membutuhkan tajwid berwarna dan fitur Tahsin.
- **Muslim Awam** yang ingin bertanya masalah agama tanpa harus online.

### Perangkat Target
- Android 12+ (API 32+)
- RAM minimum 4 GB (optimal 8 GB+)
- Storage bebas minimal 3-5 GB (untuk model AI)
- Perangkat fisik (emulator tidak mendukung inferensi LLM on-device)

---

## 4. Fitur yang Diimplementasikan

### 4.1 Fitur Al-Quran

#### F-01: Pembacaan Al-Quran
- **F-01.1** Tampilkan 114 surah dengan nama Arab, latin, dan terjemahan
- **F-01.2** Teks Arab Uthmani berkualitas tinggi (Madani Mushaf)
- **F-01.3** Navigasi per surah dan per ayat
- **F-01.4** Simpan posisi terakhir baca (last read position)

#### F-02: Tajwid Berwarna
- **F-02.1** Render tajwid berwarna menggunakan `AnnotatedString` Compose
- **F-02.2** Aturan warna tajwid:

| Hukum Tajwid | Warna |
|--------------|-------|
| Idgham | Hijau (#2E7D32) |
| Ikhfa | Biru (#1565C0) |
| Iqlab | Merah (#C62828) |
| Qalqalah | Oranye (#E65100) |
| Mad | Ungu (#6A1B9A) |
| Ghunnah | Merah Muda (#AD1457) |

- **F-02.3** Toggle on/off pewarnaan tajwid dari Settings
- **F-02.4** Legenda tajwid dapat diakses melalui bottom sheet

#### F-03: Terjemahan
- **F-03.1** Terjemahan Bahasa Indonesia (default)
- **F-03.2** Terjemahan Bahasa Inggris
- **F-03.3** Sumber data: QuranEnc, King Saud University, Tanzil
- **F-03.4** Toggle tampilkan/sembunyikan terjemahan per ayat

#### F-04: Transliterasi Latin
- **F-04.1** Transliterasi standar internasional per ayat
- **F-04.2** Toggle tampilkan/sembunyikan transliterasi
- **F-04.3** Font transliterasi yang tepat untuk simbol fonetis Arab

#### F-05: Pencarian
- **F-05.1** Full-text search pada teks Arab, terjemahan Indonesia, dan Inggris
- **F-05.2** Implementasi SQLite FTS5 untuk pencarian cepat
- **F-05.3** Highlight kata kunci pada hasil pencarian
- **F-05.4** Filter pencarian per surah atau seluruh Al-Quran

#### F-06: Bookmark & Catatan
- **F-06.1** Bookmark ayat dengan satu tap
- **F-06.2** Tambah catatan pribadi pada bookmark
- **F-06.3** Daftar bookmark dapat diurutkan (terbaru, surah)
- **F-06.4** Hapus bookmark dengan swipe gesture

---

### 4.2 Fitur Chatbot RAG AI Lokal

#### F-07: Inferensi LLM On-Device
- **F-07.1** Engine: **LiteRT-LM** (Google AI Edge, penerus MediaPipe LLM Inference)
- **F-07.2** Format model: `.litertlm` atau `.task`
- **F-07.3** Model rekomendasi:

| Model | Size | RAM | Performa |
|-------|------|-----|----------|
| Gemma-3-1B-IT (4-bit) | ~600 MB | 4 GB+ | Terbaik secara keseluruhan |
| Qwen2.5-1.5B-Instruct | ~800 MB | 4 GB+ | Optimal untuk Islamic QA |
| Gemma 4 E2B Instruct | ~2.58 GB | 6 GB+ | Kualitas tertinggi |

- **F-07.4** Streaming token response via `StateFlow`
- **F-07.5** ModelGate: blokir chat sampai model tersedia

#### F-08: RAG Pipeline (Retrieval-Augmented Generation)
- **F-08.1** Ground truth knowledge base:
  - **Al-Quran** (Arab + Terjemahan + Tafsir)
  - **Hadith Sahih** (Bukhari, Muslim, Abu Dawud, Tirmidzi)
- **F-08.2** Embedding model: `all-MiniLM-L6-v2` (ONNX INT8 quantized, ~23 MB)
- **F-08.3** Chunking: 512 token, overlap 50 token
- **F-08.4** Vector storage: **sqlite-vec** (SQLite extension, zero-dependency)
- **F-08.5** Top-K retrieval: K=5 (cosine similarity)
- **F-08.6** Prompt augmentation dengan konteks Quran/Sunnah

#### F-09: Alur RAG Query
```
Pertanyaan User -> Embedding Query -> Vector Search (sqlite-vec)
-> Retrieve Top-5 Chunks -> Prompt Augmentation
-> LiteRT-LM Inference -> Stream Token ke UI
```

#### F-10: Sourcing Transparan
- **F-10.1** Tampilkan kutipan sumber (ayat/hadith) yang digunakan AI untuk menjawab
- **F-10.2** Referensi dapat di-tap untuk navigasi ke ayat asli di fitur Quran
- **F-10.3** Indikator kepercayaan sumber (Quran vs Hadith Sahih)

#### F-11: AI Persona
- **F-11.1** Preset persona:
  - **Mufti** — Formal, mengutip dalil lengkap
  - **Ustadz** — Edukatif, penjelasan bertahap
  - **Sahabat** — Conversational, ringan
- **F-11.2** Edit custom system prompt via Settings
- **F-11.3** Simpan persona di DataStore

#### F-12: Download Manager Model
- **F-12.1** Download model dari Hugging Face dengan progress bar
- **F-12.2** Resume download jika terputus (HTTP Range request)
- **F-12.3** Verifikasi integritas file (SHA-256)
- **F-12.4** File model ditulis ke `.tmp` dulu, rename setelah selesai (mencegah korupsi)
- **F-12.5** Auto-pause jika koneksi terputus, auto-resume saat online kembali

---

### 4.3 Fitur Tahsin & Transliterasi

#### F-13: Konten Tahsin
- **F-13.1** Panduan Makharij al-Huruf (titik artikulasi 17 huruf)
- **F-13.2** Sifat al-Huruf (karakteristik setiap huruf)
- **F-13.3** Aturan tajwid lengkap dengan contoh ayat
- **F-13.4** Konten dari Kitab Tahsin terstruktur sebagai database

#### F-14: Panduan Interaktif
- **F-14.1** Tampilkan huruf Arab + transliterasi fonetis + penjelasan cara baca
- **F-14.2** Contoh ayat langsung dari Al-Quran untuk setiap aturan
- **F-14.3** Navigasi per bab (Makharij -> Sifat -> Hukum Nun Mati -> dst.)

#### F-15: Transliterasi dalam Tahsin
- **F-15.1** Setiap aturan tajwid dilengkapi transliterasi Latin
- **F-15.2** Panduan pengucapan menggunakan sistem IPA yang disederhanakan untuk pengguna Indonesia

---

## 5. Arsitektur Sistem

### 5.1 Clean Architecture + MVVM

```
+----------------------------------------------------------+
|                  PRESENTATION LAYER                      |
|   Compose Screens | ViewModels | UI State (StateFlow)    |
+----------------------------------------------------------+
|                   DOMAIN LAYER                           |
|   Use Cases | Entities | Repository Interfaces           |
+----------------------------------------------------------+
|                    DATA LAYER                            |
|   Repository Impl | Room DB | sqlite-vec | LiteRT-LM    |
|   ONNX Runtime | File System | DataStore                |
+----------------------------------------------------------+
```

### 5.2 Module Structure (Feature-Modular KMM)

```
QuranPlus/
+-- composeApp/                    # Android entry point
+-- shared/                        # KMM shared module
|   +-- commonMain/
|   |   +-- core/
|   |   |   +-- di/                # Koin DI modules
|   |   |   +-- database/          # Room DB setup
|   |   |   +-- network/           # Download manager
|   |   |   +-- utils/             # Extensions, helpers
|   |   +-- features/
|   |       +-- quran/
|   |       |   +-- presentation/  # Screens, ViewModels
|   |       |   +-- domain/        # Use cases, entities
|   |       |   +-- data/          # Repositories, Room DAOs
|   |       +-- chatbot/
|   |       +-- rag/
|   |       +-- tahsin/
|   |       +-- settings/
|   +-- androidMain/               # Android-specific implementations
|       +-- llm/                   # LiteRT-LM integration
|       +-- embedding/             # ONNX Runtime
|       +-- download/              # ResumableDownloader
```

### 5.3 Tech Stack

| Layer | Teknologi | Tujuan |
|-------|-----------|--------|
| Language | Kotlin 2.0+ | Primary language |
| UI | Jetpack Compose + Material 3 Expressive | Declarative UI |
| Architecture | Clean Architecture + MVVM | Separation of concerns |
| DI | Koin (KMM-compatible) | Dependency injection |
| Navigation | Jetpack Navigation Compose | Screen navigation |
| State | StateFlow / SharedFlow | Reactive UI state |
| Concurrency | Kotlin Coroutines + Flow | Async operations |
| Database | Room 2.7+ | Quran data, bookmarks, chat |
| Vector DB | sqlite-vec | RAG vector embeddings |
| FTS | SQLite FTS5 | Pencarian Quran |
| LLM | LiteRT-LM (litertlm-android) | On-device inference |
| Embedding | ONNX Runtime + all-MiniLM-L6-v2 | Dokumen embeddings |
| Preferences | Jetpack DataStore | Settings, persona |
| Build | Gradle Kotlin DSL | Build config |
| Testing | JUnit 5, MockK, Turbine | Unit & integration tests |

---

## 6. Data & Sumber Konten

### 6.1 Data Al-Quran
| Konten | Sumber |
|--------|--------|
| Teks Arab Uthmani | Tanzil / quranenc.com |
| Terjemahan Indonesia | QuranEnc (Kemenag RI) |
| Terjemahan Inggris | King Saud University |
| Transliterasi | Standar internasional |
| Tajwid Rules | Custom JSON dari kitab tajwid |
| Madani Page Images | quran.com images project |

### 6.2 RAG Knowledge Base
| Sumber | Format | Ukuran Perkiraan |
|--------|--------|-----------------|
| Al-Quran (teks+terjemahan) | Pre-chunked DB | ~10 MB |
| Shahih Bukhari | Pre-embedded chunks | ~50 MB |
| Shahih Muslim | Pre-embedded chunks | ~40 MB |
| Sunan Abu Dawud | Pre-embedded chunks | ~35 MB |
| Sunan Tirmidzi | Pre-embedded chunks | ~30 MB |

---

## 7. Batasan & Constraint

| Constraint | Detail |
|------------|--------|
| Offline-first | Semua fitur utama bekerja tanpa internet |
| Privasi | Zero telemetry, tidak ada analytics cloud |
| Memory | Model max 2.58 GB, total RAM usage < 4 GB |
| Storage | Total app + model + DB < 5 GB |
| Battery | Embedding thread dibatasi 2 (anti thermal throttling) |
| SDK Min | API 32 (Android 12) |
| SDK Target | API 35 (Android 15) |

---

## 8. Fase Pengembangan

| Fase | Durasi | Deliverable |
|------|--------|-------------|
| P1: Setup & Arsitektur | 1 minggu | KMM setup, Clean Arch, Koin DI |
| P2: Data Layer Quran | 2 minggu | Room DB, data import, FTS5 |
| P3: Quran UI | 2 minggu | Pembacaan, tajwid, search, bookmark |
| P4: RAG Pipeline | 2 minggu | ONNX embedding, sqlite-vec, ingestion |
| P5: LLM Integration | 2 minggu | LiteRT-LM, ModelGate, streaming |
| P6: Chatbot UI | 1 minggu | Chat UI, source surfacing, persona |
| P7: Tahsin | 1 minggu | Tahsin DB, panduan interaktif |
| P8: Download Manager | 1 minggu | Resumable download, integrity check |
| P9: Polish & Test | 1 minggu | Testing, performance, accessibility |
| **Total** | **~13 minggu** | Production-ready app |

---

## 9. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Memori tidak cukup untuk model besar | Tinggi | Gunakan model 1B terlebih dulu; model 2.58 GB opsional |
| Performa LLM lambat di perangkat entry-level | Medium | Batasi model ke 1B-1.5B, streaming untuk UX responsif |
| Korupsi model saat download | Tinggi | .tmp -> rename + SHA-256 verification |
| Akurasi RAG rendah | Medium | Tune chunking dan top-K; pre-embed corpus berkualitas |
| APK terlalu besar | Medium | Model tidak dibundle di APK; download on-demand |

---

## 10. Out of Scope (Fase 1)

- Audio murottal streaming
- Sinkronisasi cloud / backup
- Fitur Kiblat dan Waktu Sholat
- Multiplayer / komunitas
- iOS port (dipertimbangkan di Fase 2 dengan KMM)
