# Quran Plus

Quran Plus adalah aplikasi Android offline-first berbasis Jetpack Compose, Clean Architecture, MVVM, Room, FTS5, dan Koin. Status di bawah ini adalah status berbasis bukti; fitur tanpa sumber, checksum, model, index, atau acceptance device tetap ditampilkan sebagai `blocked`.

## Yang sudah diperbaiki

- Reader Quran minimal dengan back, judul/subjudul yang aman, dan satu hamburger menu.
- Tajwid dirender memakai `AnnotatedString` dan parser fail-closed untuk tag tidak dikenal/malformed.
- Katalog Waqaf terverifikasi untuk `لا`, `صلى`, `ج`, `م`, `قلى`, mu‘anaqah, saktah, dan marker akhir ayat yang dirender deterministik dari nomor ayat.
- Word-by-word Room berisi 77.430 baris untuk 6.236 ayat. Arabic dan English source ditampilkan; transliterasi, root, audio, dan Indonesian per-word tidak ditebak jika mapping/provenance belum valid.
- Bottom bar compact berisi lima slot: `Al-Qur'an`, `Hadist`, `Tanya AI`, `Tahsin`, dan `More`. Bookmark, pengaturan, waqaf, gharib, dan audio tetap tersedia melalui More.
- Route Hadist, repository, use case, ViewModel, metadata koleksi, pencarian, citation fields, dan readiness gate sudah tersedia.
- Model/RAG memakai SAF sebagai source of truth untuk `QuranPlus/models/`, `QuranPlus/rag/source/`, `QuranPlus/rag/index/`, dan `QuranPlus/manifests/`. File memakai `.part`/`.tmp`, SHA-256, dan publish setelah verifikasi.
- AI tidak menghasilkan jawaban atau embedding ketika prerequisite tidak tersedia.

## Bukti data Quran

`python scripts/validate_quran_corpus.py app/src/main/assets/databases/quranplus.db` menghasilkan:

| Check | Hasil |
| --- | ---: |
| Surah | 114 |
| Ayat | 6.236 |
| Word-by-word | 77.430 |
| English source per-word | 77.430 |
| Indonesian per-word | 0, fail-closed karena provenance/lisensi dataset belum lolos |
| Alignment/sequence failure | 0 |
| Marker akhir ayat yang dirender | 6.236 |

Teks sumber tidak menyimpan glyph `۝` di setiap baris; reader menambahkan `۝` dan digit Arab dari `ayah_number`, sehingga marker tidak diambil dari data rekaan.

## Hadist dan RAG

Reference lokal terdeteksi sebagai 17 koleksi dan 50.884 record. Audit terakhir menemukan 3.567 record tidak lengkap, tanpa duplicate ID atau invalid chapter reference. Lisensi dan record-level grading belum diverifikasi, sehingga `bundle_allowed=false`: database aplikasi hanya memuat metadata provenance 17 koleksi dan **0 teks hadist**. Tidak ada terjemahan Indonesia yang dibuat oleh aplikasi.

AI tetap `MODEL_UNAVAILABLE`, `EMBEDDER_UNAVAILABLE`, dan `INDEX_UNAVAILABLE` sampai manifest model, tokenizer, corpus, embedding ONNX 384-dimensi, dan sqlite-vec index nyata lolos checksum/provenance. Chunk contract adalah 512 token, overlap 50, dan top-k 5; tidak ada fallback scan Room yang menyamar sebagai vector retrieval.

Audit Hadist yang mengembalikan exit code 2 adalah guard yang diharapkan selama lisensi/kelengkapan belum lolos:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-hadith-reference.ps1 -AsJson
```

Manifest ringkas yang boleh masuk repository berada di [data/hadith-provenance.json](data/hadith-provenance.json). Raw reference, model weights, credential, dan dokumen internal tidak masuk APK atau GitHub.

## SAF dan model

Pengguna memilih folder melalui `ACTION_OPEN_DOCUMENT_TREE`. Aplikasi menyimpan persistable URI permission, membuat struktur berikut, memindai manifest saat relink, dan meminta relink bila permission hilang:

```text
QuranPlus/
├── models/
├── rag/source/
├── rag/index/
└── manifests/
```

Working cache internal boleh dibuat ulang setelah uninstall; model dan source RAG yang sudah dipublish tetap berada di folder SAF pengguna. Download memakai WorkManager, network constraint, resume/checksum, retry terbatas, dan tidak melakukan retry otomatis untuk kegagalan konfigurasi/storage.

### Katalog model tambahan

Model tambahan tampil sebagai kandidat source-only sampai format runtime, revision immutable, checksum artifact, tokenizer, ukuran, dan lisensi turunannya direview. Tidak ada kandidat di bawah ini yang otomatis aktif atau boleh diunduh dari aplikasi:

- [Alif Islamic v4 Base](https://huggingface.co/ahmedtamseer3/alif-islamic-v4-base) — artifact `.task` untuk MediaPipe/LiteRT; belum kompatibel dengan loader LiteRT-LM aplikasi.
- [Qwen2.5 1.5B Instruct LiteRT/GGUF](https://huggingface.co/DuoNeural/Qwen2.5-1.5B-Instruct-LiteRT) — format/runtime community belum tervalidasi untuk LiteRT-LM aplikasi.
- [Gemma 3 1B IT MNN](https://huggingface.co/darkmaniac7/Gemma-3-1B-IT-MNN) — MNN bukan runtime aplikasi dan profil safety community belum direview.
- [Qwen3 Embedding 0.6B](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B) — embedding candidate; pipeline saat ini membutuhkan ONNX 384-dimensi, sehingga tidak boleh menggantikan index secara diam-diam.
- [all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2) — kandidat ONNX 384-dimensi; model, tokenizer, asset pendamping, dan checksum belum dipublish ke SAF.

Katalog memisahkan `CHATBOT` dan `EMBEDDING`. Model GGUF, MNN, `.task`, dan embedding tidak pernah dikirim ke `LiteRtLmRunner`; AI tetap blocked sampai `ModelGate` lengkap.

## Arsitektur

```text
Compose UI + ViewModel
        ↓
Use case + domain contracts
        ↓
Room/FTS5 + SAF + ONNX/LiteRT adapters + sqlite-vec gate
```

`shared/commonMain` memuat kontrak KMM yang dibutuhkan; implementasi Android berada di `app`. RTK, CAVEMAN, dan PONYTAIL hanya authoring guidance sesuai `AGENTS.md`, bukan dependency runtime.

## Build dan test

```powershell
rtk proxy .\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --console=plain
rtk proxy .\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain
rtk proxy .\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
rtk proxy .\gradlew.bat :app:lintDebug --no-daemon --console=plain
rtk proxy .\gradlew.bat :app:assembleDebug --no-daemon --console=plain
rtk proxy .\gradlew.bat :app:connectedDebugAndroidTest --no-daemon --console=plain
```

Gate terakhir yang lolos:

- compile shared/app dan unit test: pass;
- lint debug dan debug assembly: pass, 0 error (warning existing tetap dilaporkan Gradle/Lint);
- 15/15 Android instrumentation tests: pass pada `RRCN3008VYE` / Samsung SM-G988B, Android 13;
- APK debug terbaru di-install dan diluncurkan dengan `adb install -r` pada `RRCN3008VYE`;
- `QSWSEMRKNFZ9LJRC` tidak terhubung pada acceptance run ini, sehingga tidak diklaim lulus pada APK/commit ini;
- DB runtime setelah upgrade: 6.236 ayat, 77.430 word rows, 17 hadist collection metadata, 0 hadist text;
- accessibility tree memverifikasi hamburger, bottom-bar five-slot, selected state, marker `۝`, mu‘anaqah `ۛ`, word selection, dan ModelGate blocker;
- APK SHA-256: `5A9738E34D2CBFA975C79CFD8AD5E06B3FDD63EE88F5427BF941B5E088BA8127` (245,000,579 bytes).

Landscape, font-scale 200%, TalkBack/IME journey, audio, model lokal nyata, corpus hadist distributable, embedding, dan sqlite-vec index tetap memerlukan acceptance terpisah/prerequisite nyata; tidak diklaim lulus dari run portrait ini.

## Preview aktual

Preview utama berikut diambil dari APK debug terbaru memakai `adb exec-out screencap -p` pada Samsung SM-G988B, Android 13, portrait, physical 1440×3200, density override 560. Metadata APK, waktu capture, route, dan responsive smoke tercatat di [art/device-preview-manifest.json](art/device-preview-manifest.json).

| Quran home | Reader Waqaf/Tajwid | Word-by-word |
| --- | --- | --- |
| ![Quran home](art/device-sm-g988b-current-home.png) | ![Reader Waqaf and Tajwid](art/device-sm-g988b-current-reader.png) | ![Word by word](art/device-sm-g988b-current-word.png) |

| Hadist blocked state | AI ModelGate |
| --- | --- | --- |
| ![Hadist provenance gate](art/device-sm-g988b-current-hadith.png) | ![AI readiness gate](art/device-sm-g988b-current-ai.png) |

Preview adalah smoke evidence, bukan bukti bahwa model, audio, lisensi hadist, atau semua device class sudah release-ready. Screenshot API 35 tidak ditampilkan sebagai acceptance terkini karena device tersebut tidak terhubung pada run ini.

## Security

- Jangan stage `docs*`, reference project, raw model/embedding/index, archive, credential, atau local instruction Markdown.
- Review `git diff --cached`, jalankan `git diff --cached --check` dan secret scan sebelum commit/push.
- Jangan mengklaim completion dari screenshot, browser shell, mock executor, compile-only, atau data fabricated.
- Source/license review wajib dilakukan sebelum mendistribusikan Quran translation, hadist, audio, font, model, atau derived index.
