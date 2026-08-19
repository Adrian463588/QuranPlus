# AGENTS.md — Quran Plus (Sprint 2)

> **Dokumen Kendali Agent:** Wajib dibaca pertama kali oleh setiap AI coding assistant sebelum merencanakan atau memodifikasi kode pada Sprint 2.

---

## 1. Peta Rujukan & Dokumen Terkait

| Bidang Pekerjaan | Dokumen Rujukan Wajib |
|---|---|
| Spesifikasi Fitur, Scope & Schema Data | [`docsSprint2/PRD.md`](file:///d:/SEMESTER12/ProjectKode/QuranPlus/docsSprint2/PRD.md) |
| Desain UI, Color Tokens, Tipografi & Aturan Anti-Slop | [`docsSprint2/DESIGN.md`](file:///d:/SEMESTER12/ProjectKode/QuranPlus/docsSprint2/DESIGN.md) |
| Ground Truth Arsitektur & Hardware Spec | [`docs/Reference.md`](file:///d:/SEMESTER12/ProjectKode/QuranPlus/docs/Reference.md) |
| Referensi Desain & Audio/Tajwid Visual | [`docs/AlquranReference/imgReference/reference1.md`](file:///d:/SEMESTER12/ProjectKode/QuranPlus/docs/AlquranReference/imgReference/reference1.md) & [`reference2.md`](file:///d:/SEMESTER12/ProjectKode/QuranPlus/docs/AlquranReference/imgReference/reference2.md) |
| Pipeline RAG & On-Device LLM Runtime | [`docs/AndroidChatbot/pocketsage-master`](file:///d:/SEMESTER12/ProjectKode/QuranPlus/docs/AndroidChatbot/pocketsage-master) |

---

## 2. Prinsip Rekayasa Perangkat Lunak (NON-NEGOTIABLE)

1. **You Are the Driver, Quality is Non-Negotiable:**
   - Jangan menghasilkan boilerplate atau placeholder acak.
   - Tidak ada kode dummy/mock tanpa anotasi pengujian yang jelas.
2. **Spec-Driven & Layer-by-Layer Generation:**
   - Bangun kode secara berurutan: `Data Layer (Room / DAO / API / Engine)` → `Domain Layer (Entities / UseCases)` → `Presentation Layer (ViewModels / StateFlow / Composables)`.
3. **Jetpack Compose Only (No XML):**
   - 100% antarmuka dibuat menggunakan Jetpack Compose dengan Material 3.
4. **State Management Bersih:**
   - ViewModel HANYA mengelola `StateFlow<UiState<T>>`.
   - Composable berstatus *stateless*, menerima state dan memancarkan (*hoist*) events.
5. **No Hardcoded Tokens:**
   - Dilarang keras menuliskan warna heksadesimal atau ukuran `dp`/`sp` sembarangan di dalam Composable. Semua wajib merujuk ke token `QuranColors`, `QuranTypography`, `Spacing`, dan `Shapes` di `DESIGN.md`.

---

## 3. Struktur Modul & Tanggung Jawab (Sprint 2)

```
shared/commonMain/
├── core/
│   ├── di/                       # Modul Koin (DatabaseModule, NetworkModule, AudioModule, LlmModule)
│   ├── database/                 # Room Database v2 (Quran, TajwidRules, Waqaf, Gharib, Chat)
│   ├── audio/                    # Core Audio Player Abstraction (Play, Pause, Seek, Repeat, Speed)
│   └── utils/                    # FlowUtils, DispatcherProvider, StringAnnotator
└── features/
    ├── quran/                    # Reader, Word-by-Word, Search FTS5, Bookmarks
    ├── tajwid/                   # Granular Tajwid Engine, Legend, Interactive Sheet
    ├── gharib/                   # Direktori 9 Bacaan Gharib, Sajdah, Saktah, Imalah, Isymam
    ├── waqaf/                    # Panduan Tanda Waqaf & Ibtida'
    ├── audio/                    # Murattal Controller, Resumable Download Manager
    ├── tahsin/                   # Kitab Tahsin, Makharij, Sifat, Quiz Tajwid
    ├── chatbot/                  # RAG Engine, sqlite-vec, Transparent Sourcing, Persona
    └── settings/                 # App Preferences (DataStore)
```

---

## 4. Standar Pola Kode (Design Patterns)

### 4.1 UI State Unidirectional Data Flow (UDF)
```kotlin
// Sealed interface untuk representasi state yang komprehensif
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}
```

### 4.2 Pattern UseCase Terfokus (Single Responsibility)
```kotlin
class GetAyahWithTajwidUseCase(
    private val quranRepository: QuranRepository,
    private val tajwidRepository: TajwidRepository
) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int): Flow<AyahWithTajwid> =
        combine(
            quranRepository.getAyah(surahNumber, ayahNumber),
            tajwidRepository.getActiveRules()
        ) { ayah, rules ->
            AyahWithTajwid(ayah = ayah, annotatedText = TajwidAnnotator.applyRules(ayah.textArabic, rules))
        }
}
```

### 4.3 Pattern Granular Tajwid Token Parsing
```kotlin
object TajwidAnnotator {
    fun applyRules(
        rawArabic: String,
        activeRules: Map<TajwidRuleType, Boolean>
    ): AnnotatedString {
        return buildAnnotatedString {
            // Evaluasi token tajwid dan sematkan SpanStyle berdasarkan activeRules
            // Fallback ke QuranColors.TextArabic default jika rule di-toggle OFF
        }
    }
}
```

### 4.4 Pattern LLM Streaming & Transparent Sourcing
```kotlin
class RagQueryUseCase(
    private val embedder: OnDeviceEmbedder,
    private val vectorStore: VectorStore,
    private val llmInference: LocalLlmInference
) {
    fun execute(query: String, persona: SystemPersona): Flow<RagStreamEvent> = callbackFlow {
        val queryVector = embedder.embed(query)
        val relevantDocs = vectorStore.searchSimilar(queryVector, topK = 5)
        
        trySend(RagStreamEvent.SourcesLoaded(relevantDocs.map { it.toSourceCitation() }))
        
        val prompt = buildAugmentedPrompt(persona, query, relevantDocs)
        llmInference.generateStream(prompt) { token, isFinished ->
            trySend(RagStreamEvent.TokenGenerated(token))
            if (isFinished) {
                trySend(RagStreamEvent.Completed)
                close()
            }
        }
        awaitClose { llmInference.cancelCurrentSession() }
    }
}
```

---

## 5. Aturan Khusus Fitur Sprint 2

### 5.1 Al-Qur'an & Tajwid
- Teks tajwid wajib berupa `AnnotatedString` native Compose, bukan evaluasi HTML WebView yang berat.
- Setiap hukum tajwid harus memiliki ID unik agar sinkron dengan toggle `Pengaturan Tajwid`.
- Mode Kata-per-Kata (*Word-by-word*) wajib menyediakan referensi morfologi dasar / akar kata (*root word*).

### 5.2 Bacaan Gharib & Ayat Sajdah
- Direktori Gharib wajib memiliki kemampuan navigasi balik (*deep link*) ke halaman membaca surah:ayat terkait.
- Pada kasus Isymam (QS. Yusuf: 11), sertakan ilustrasi/vektor posisi bibir di samping pemutar audio.
- Pada kasus Sifir Mustatil, sediakan 2 tombol pemutar: `Putar Wasal` (dibaca pendek) dan `Putar Waqaf` (dibaca panjang).

### 5.3 Audio Murattal & Download Manager
- Integrasi audio menggunakan **Jetpack Media3 / ExoPlayer** di layer Android.
- Download audio wajib menggunakan HTTP *Range Header* dan disimpan dengan ekstensi `.part` sebelum validasi checksum SHA-256 dan rename permanen.
- Pengaturan kecepatan audio wajib mendukung rentang `0.5x`, `0.75x`, `1.0x`, dan `1.25x`.

### 5.4 Chatbot RAG AI Lokal
- Model file tidak boleh dibundle di dalam APK. Gunakan SAF (*Storage Access Framework*) atau download on-demand.
- Eksekusi embedding dibatasi maksimal 2 thread coroutine pada `Dispatchers.Default` untuk mencegah *thermal throttling* pada SoC mobile.
- Kutipan sitasi harus menampilkan nama Surah:Ayat atau Kitab Hadis yang dapat diklik langsung membuka referensi aslinya.

---

## 6. Checklist Verifikasi Sebelum Menyerahkan Kode

Sebelum menyatakan tugas implementasi selesai, Agent wajib memvalidasi:
- [ ] Tidak ada warning linting kritis atau deprecated API.
- [ ] Tidak ada hardcoded warna, ukuran font, atau string mentah di file Composable.
- [ ] Semua state error dan loading ditangani secara eksplisit pada UI (`EmptyState`, `ErrorBanner`, `Shimmer`).
- [ ] Touch target untuk seluruh tombol dan area klik interaktif memenuhi standar minimal 48dp × 48dp.
- [ ] Unit test UseCase dan ViewModel lulus uji dengan Turbine & MockK.
