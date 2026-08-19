# Quran Plus

Quran Plus adalah aplikasi Android offline-first berbasis Jetpack Compose, Clean Architecture, MVVM, Room, FTS5, Koin, ONNX Runtime, LiteRT-LM, SAF, WorkManager, dan sqlite-vec.

## Status implementasi

- Quran: 114 surah, 6.236 ayat, tajwid `AnnotatedString`, waqaf, marker akhir ayat, pencarian FTS5, bookmark, dan reader responsive.
- Word-by-word: 77.430 baris untuk 6.236 ayat. Arabic dan English tersedia dari sumber; transliterasi per kata dan Indonesia tidak dibuat jika sumbernya tidak tersedia.
- Tahsin: 54 materi Room-backed tentang makharij, sifat huruf, hukum tajwid, mad, dan waqaf. Contoh ayat diperbarui oleh `scripts/refresh_tahsin.py` dari teks Quran yang ada di database.
- Hadist: menu, pencarian, dan importer JSON untuk koleksi di `docs/HadistReference/reference2/db/by_book`. Teks tidak dibundel dari folder referensi internal; pengguna mengimpor file melalui Settings agar sumbernya tetap berada di storage pengguna. Record tanpa Arabic atau English text dilewati dan hasil import tidak diklaim sebagai corpus lengkap.
- RAG: index sqlite-vec memakai embedding nyata untuk Quran, Hadist yang diimpor, dan dokumen pengguna. Chunking memakai 512 kata dengan overlap 50; retrieval default top-k 5.
- AI: jawaban hanya dibuat ketika model chatbot, embedder, corpus, dan index siap. Selain itu UI menampilkan `MODEL_UNAVAILABLE`, `EMBEDDER_UNAVAILABLE`, atau `INDEX_UNAVAILABLE`.

## Model yang dapat diunduh

Katalog memakai manifest minimal dengan URL HTTPS yang dipin, ukuran artifact, format/runtime, dan SHA-256. Checksum hanya dipakai untuk memastikan unduhan tidak korup atau tertukar.

- Qwen 2.5 1.5B Instruct LiteRT-LM — [source](https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/tree/fc180c8fdd5092041a35d416dea8a6c0f771f5a2), 1.567.364.648 bytes, SHA-256 `98c289e1c43cc592ac535594d5de4bdde449e8dc012ac66909064b6880f8b717`.
- all-MiniLM-L6-v2 ONNX untuk RAG — [source](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/1110a243fdf4706b3f48f1d95db1a4f5529b4d41), 23.026.053 bytes, 384 dimensi, SHA-256 `4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474`.
- Gemma 3, Alif Islamic, Qwen GGUF, dan Qwen3 Embedding tetap tampil sebagai sumber tambahan, tetapi tidak dapat diunduh atau dipakai sebelum format dan runtime-nya benar-benar cocok dengan aplikasi.

## Folder SAF dan uninstall

Pilih folder melalui `ACTION_OPEN_DOCUMENT_TREE` pada Settings. Aplikasi membuat:

```text
QuranPlus/
├── models/
├── rag/source/
├── rag/index/
└── manifests/
```

Model dan dokumen RAG yang sudah dipublish berada di folder milik pengguna, bukan hanya `filesDir`, sehingga tidak ikut terhapus ketika aplikasi di-uninstall. Setelah install ulang, pilih folder yang sama untuk relink dan materialisasi cache internal. Unduhan memakai file sementara, HTTP Range, WorkManager, dan verifikasi SHA-256 sebelum dipublish.

## Materi Tahsin

Materi ringkas disusun untuk pemula dari sumber berikut:

- [Tajweed For Beginners](https://dua.org.za/content/tajweed-beginners-1)
- [Simplified Makhaarij & Tajweed Rules](https://resources.safarpublications.org/2016/11/24/simplified-makhaarij-tajweed-rules/)
- [Quran Foundation Tajweed content API](https://api-docs.quran.com/docs/content_apis_versioned/4.0.0/quran-verses-uthmani-tajweed/)

Tidak ada file audio Tahsin yang diklaim tersedia. Tombol audio hanya aktif untuk audio Quran yang benar-benar tersedia dari `AudioPlayerManager`.

## Build dan test

```powershell
.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --console=plain
.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat :app:lintDebug --no-daemon --console=plain
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon --console=plain
```

Acceptance Android terakhir dijalankan pada `RRCN3008VYE` / Samsung SM-G988B Android 13. Device `QSWSEMRKNFZ9LJRC` tidak terhubung dan tidak diklaim lulus. Model chatbot, model embedding, import corpus Hadist, indexing penuh, audio, dan seluruh kelas ukuran layar tetap membutuhkan asset/prasyarat nyata masing-masing.

## Preview device

Screenshot di bawah harus diregenerasi dari APK/commit terbaru; screenshot bukan bukti model atau corpus sudah siap.

| Quran home | Reader | Word-by-word |
| --- | --- | --- |
| ![Quran home](art/device-sm-g988b-current-home.png) | ![Reader](art/device-sm-g988b-current-reader.png) | ![Word-by-word](art/device-sm-g988b-current-word.png) |

| Hadist | AI ModelGate |
| --- | --- |
| ![Hadist](art/device-sm-g988b-current-hadith.png) | ![AI ModelGate](art/device-sm-g988b-current-ai.png) |

## Security

Jangan commit `docs/`, reference project, credential, raw model weights, archive, atau database hasil import pengguna. Sebelum delivery, review staged diff, jalankan `git diff --cached --check`, secret scan, dan verifikasi SHA remote.

RTK, CAVEMAN, dan PONYTAIL adalah authoring guidance sesuai `AGENTS.md`, bukan dependency runtime.
