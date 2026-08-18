# AGENTS.md — Quran Plus

> File ini adalah **entry-point wajib** untuk semua AI agent yang bekerja di project ini.
> Baca file ini PERTAMA sebelum melakukan perubahan apapun.

---

## Routing Dokumen

| Jika kamu bekerja pada... | Baca juga... |
|---------------------------|--------------|
| UI / Compose screens / komponen | `DESIGN.md` (wajib) |
| Fitur baru atau arsitektur | `PRD.md` Section 4 & 5 |
| Data layer / Room / Database | `PRD.md` Section 6 |
| RAG pipeline / LLM / Embedding | `PRD.md` Section 4.2 |
| Semua pekerjaan UI | `DESIGN.md` + Anti-Slop checklist di bawah |

---

## Identitas Proyek

**Nama:** Quran Plus
**Platform:** Android (Jetpack Compose KMM Multiplatform)
**Bahasa:** Kotlin 2.0+
**Min SDK:** API 32 (Android 12)
**Arsitektur:** Clean Architecture + MVVM + Feature-Modular KMM

---

## Prinsip Pengembangan (NON-NEGOTIABLE)

### 1. SOLID
- **S** — Setiap Use Case menangani SATU operasi spesifik
- **O** — Repository interfaces memungkinkan extension tanpa modifikasi
- **L** — Implementasi repository harus saling dapat dipertukarkan
- **I** — Interface kecil dan terfokus (jangan satu interface melakukan segalanya)
- **D** — Domain layer bergantung pada abstraksi, bukan concretion

### 2. DRY
- Utility bersama di module `:core`
- Base ViewModel class untuk pola umum
- Komponen Compose yang reusable

### 3. KISS
- State management sederhana dengan StateFlow
- Navigasi to-the-point
- Kode yang mudah dibaca

### 4. YAGNI
- Jangan tambahkan fitur yang tidak ada di PRD
- Mulai dari minimal viable, tambah kompleksitas jika benar-benar dibutuhkan
- Jangan over-engineer

### 5. Clean Code
- Ikuti Kotlin coding conventions
- Nama yang bermakna (jangan: `val x`, `fun doStuff()`)
- Fungsi maksimum 20-30 baris
- Tulis unit test untuk domain dan data layer

---

## Arsitektur & Modul

### Layer Hierarchy
```
Presentation (Compose UI + ViewModels)
    |
    v
Domain (Use Cases + Entities + Repository Interfaces)
    |
    v
Data (Repository Impls + Room + sqlite-vec + LiteRT-LM + ONNX)
```

### Aturan Dependency
- Presentation BOLEH bergantung pada Domain
- Domain TIDAK BOLEH bergantung pada Data atau Presentation
- Data BOLEH bergantung pada Domain
- Tidak ada circular dependency antar modul

### Struktur Modul KMM
```
shared/commonMain/
  core/
    di/          -> Koin modules
    database/    -> Room setup
    network/     -> Download manager
    utils/       -> Extensions
  features/
    quran/       -> presentation/ domain/ data/
    chatbot/     -> presentation/ domain/ data/
    rag/         -> presentation/ domain/ data/
    tahsin/      -> presentation/ domain/ data/
    settings/    -> presentation/ domain/ data/

shared/androidMain/
  llm/           -> LiteRT-LM Android implementation
  embedding/     -> ONNX Runtime Android implementation
  download/      -> ResumableDownloader
```

---

## Tech Stack Wajib

| Kebutuhan | Teknologi | JANGAN Ganti Dengan |
|-----------|-----------|---------------------|
| UI | Jetpack Compose + Material 3 | XML Views, Flutter |
| DI | Koin | Hilt (tidak kompatibel KMM) |
| Database | Room 2.7+ | Realm, SQLDelight (kecuali diminta) |
| Vector DB | sqlite-vec | Hnswlib, Faiss (terlalu berat) |
| LLM Engine | LiteRT-LM | MediaPipe (maintenance mode) |
| Embedding | ONNX Runtime + all-MiniLM-L6-v2 | TFLite Embedder |
| State | StateFlow | LiveData (deprecated untuk Compose) |
| Async | Kotlin Coroutines + Flow | RxJava |
| Build | Gradle Kotlin DSL | Groovy DSL |

---

## Aturan Kode

### Kotlin / Compose
```kotlin
// BENAR - UI state sebagai sealed interface
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// BENAR - ViewModel hanya memegang state
class QuranViewModel(
    private val getSurahListUseCase: GetSurahListUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Surah>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Surah>>> = _uiState.asStateFlow()
    // ...
}

// SALAH - business logic di Composable
@Composable
fun QuranScreen() {
    val list = database.getSurahList() // JANGAN LAKUKAN INI
}
```

### Room Entity Pattern
```kotlin
@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val nameArabic: String,
    val nameLatin: String,
    val nameTranslation: String,
    val revelationType: String,
    val verseCount: Int
)
```

### Use Case Pattern
```kotlin
class GetSurahListUseCase(
    private val quranRepository: QuranRepository
) {
    suspend operator fun invoke(): Flow<List<Surah>> =
        quranRepository.getAllSurahs()
}
```

### LLM Streaming Pattern
```kotlin
// Gunakan callbackFlow untuk wrap LiteRT-LM callback ke Flow
fun generateResponseStream(prompt: String): Flow<String> = callbackFlow {
    llmInference.generateResponseAsync(prompt) { partial, done ->
        trySend(partial)
        if (done) close()
    }
    awaitClose { /* cleanup session */ }
}
```

---

## Aturan Khusus Fitur

### Al-Quran / Tajwid
- Tajwid warna HARUS menggunakan `AnnotatedString` — jangan `WebView` kecuali ada alasan kuat
- Warna tajwid HARUS sesuai `TajwidColorScheme` di `DESIGN.md`
- Search WAJIB menggunakan FTS5, bukan LIKE query
- Bookmark harus dapat sync dengan last-read position

### RAG / LLM
- ModelGate composable WAJIB diimplementasikan — jangan langsung akses LLM tanpa cek model
- Model file HARUS ditulis ke `.tmp` dulu, baru rename saat selesai
- Embedding thread DIBATASI maksimal 2 (anti thermal throttling)
- Chunk size: 512 token, overlap: 50 token — JANGAN ubah tanpa pengujian
- Top-K retrieval: 5 — bisa ditune tapi default 5

### Download Manager
- WAJIB gunakan HTTP Range request untuk resume capability
- WAJIB verifikasi SHA-256 setelah download selesai
- WAJIB auto-pause saat network loss, auto-resume saat reconnect
- Gunakan WorkManager untuk download background

### Tahsin
- Konten Tahsin HARUS di-store di Room, bukan hardcode di kode
- Navigasi Tahsin mengikuti struktur kitab: Makharij -> Sifat -> Hukum -> dst.
- Setiap aturan HARUS memiliki contoh ayat dari Al-Quran

---

## Anti-Pattern yang DILARANG

### Arsitektur
- Jangan akses database langsung dari Composable atau ViewModel
- Jangan bypass Use Case — langsung ke repository dari ViewModel
- Jangan hardcode string di Composable (kecuali teks Arab/Al-Quran)
- Jangan gunakan `GlobalScope` — selalu gunakan `viewModelScope` atau `lifecycleScope`

### Performa
- Jangan jalankan operasi berat di Main thread
- Jangan buat `remember` yang menghitung ulang setiap recomposition
- Jangan lupakan `key` pada `LazyColumn` items
- Jangan gunakan `derivedStateOf` berlebihan

### LLM / RAG
- Jangan inisialisasi LLM session berkali-kali tanpa menutup yang lama
- Jangan kirim query ke LLM tanpa RAG augmentation (untuk pertanyaan agama)
- Jangan cache embedding di memory terlalu lama

---

## Workflow Agent

Sebelum menulis kode UI apapun, jawab pertanyaan ini:

1. Apa tujuan UTAMA screen ini?
2. Apa aksi PRIMER yang user lakukan?
3. Informasi apa yang HARUS terlihat langsung?
4. Informasi apa yang bisa disembunyikan (progressive disclosure)?
5. Apakah ada elemen yang bisa dihapus tanpa mengurangi fungsi?

Setelah menghasilkan UI, lakukan **deletion pass**:
- Hapus elemen yang tidak mendukung task utama user
- Ganti Card dengan spacing jika spacing cukup untuk grouping
- Pastikan hanya ada 1 primary CTA per screen

---

## Testing Requirements

| Layer | Jenis Test | Tool |
|-------|-----------|------|
| Domain (Use Cases) | Unit tests | JUnit 5 + MockK |
| Data (Repository) | Integration tests | JUnit 5 + Room in-memory |
| ViewModel | Unit tests | JUnit 5 + Turbine (Flow testing) |
| UI (Compose) | UI tests | Compose Testing |
| RAG Pipeline | Integration tests | JUnit 5 |

**Test naming convention:** `GIVEN_<state>_WHEN_<action>_THEN_<expectation>`

---

## Referensi Repository

| Kebutuhan | Repository Referensi |
|-----------|----------------------|
| Offline RAG Android | `nicolas-raoul/offline-rag-android` |
| PocketSage (RAG + LiteRT-LM) | `umerdilpazir/pocketsage` |
| Quran Android (struktur data) | `quran/quran_android` |
| Quran Reader UI (Compose) | `Hotaro26/QuranReader` |
| LLM Android | `BEKO2210/OFF-Line-AI-LLM` |
| Architecture Reference | `igorwojda/android-showcase` |

---

## File Penting Lainnya

- `PRD.md` — Product Requirements Document (fitur, arsitektur, tech stack)
- `DESIGN.md` — Design system, color palette, typography, anti-slop rules
- `shared/commonMain/core/di/` — Koin DI modules
- `shared/androidMain/llm/` — LiteRT-LM implementation
