# Quran Plus

Quran Plus adalah aplikasi Android offline-first berbasis Jetpack Compose, Clean Architecture, MVVM, Room, FTS5, Koin, ONNX Runtime, LiteRT-LM, SAF, WorkManager, dan sqlite-vec.

## Status implementasi

- Quran: 114 surah, 6.236 ayat, tajwid `AnnotatedString`, waqaf, marker akhir ayat, pencarian FTS5, bookmark, dan reader responsive.
- Word-by-word: 77.429 unit sumber untuk 6.236 ayat. Arabic, transliterasi Latin, dan makna English tersedia per kata dari [Islamic.app Word API](https://docs.islamic.app/api-reference/words), yang mendokumentasikan sumber Quran.com API v4. Unit komposit seperti `إِلْ يَاسِينَ` tetap ditampilkan sebagai satu makna sumber; transliterasi tidak dipotong secara heuristik. Terjemahan Indonesia belum dibundel karena sumber berlisensinya belum diverifikasi.
- Tahsin: 54 materi Room-backed tentang makharij, sifat huruf, hukum tajwid, mad, dan waqaf. Contoh ayat diperbarui oleh `scripts/refresh_tahsin.py` dari teks Quran yang ada di database.
- Hadist: katalog section/list dengan `Kutubus Sittah` dan `Hadis Lainnya`, pencarian, serta importer JSON melalui Settings. Empat koleksi offline yang sudah ada menampilkan terjemahan Indonesia nyata pada 6.333 record yang cocok dengan teks Arab sumber; record yang tidak cocok tidak dipaksa dipetakan dan menampilkan status unavailable/English. Terjemahan berasal dari [gadingnst/hadith-api](https://github.com/gadingnst/hadith-api), yang mendokumentasikan sumber Bahasa Indonesia dan lisensi repositori MIT. Koleksi lain tetap ditampilkan sebagai katalog dan hanya dapat dibaca jika kontennya benar-benar tersedia lokal atau diimpor pengguna.
- RAG: index sqlite-vec memakai embedding nyata untuk Quran, Hadist yang diimpor, dan dokumen pengguna. Chunking memakai 512 kata dengan overlap 50; retrieval default top-k 5.
- AI: jawaban hanya dibuat ketika model chatbot, embedder, corpus, dan index siap. Selain itu UI menampilkan `MODEL_UNAVAILABLE`, `EMBEDDER_UNAVAILABLE`, atau `INDEX_UNAVAILABLE`.

## Model yang dapat diunduh

Katalog memakai manifest minimal dengan URL HTTPS yang dipin, ukuran artifact, format/runtime, dan SHA-256. Checksum hanya dipakai untuk memastikan unduhan tidak korup atau tertukar.

- Qwen 2.5 1.5B Instruct LiteRT-LM — [source](https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/tree/fc180c8fdd5092041a35d416dea8a6c0f771f5a2), 1.567.364.648 bytes, SHA-256 `98c289e1c43cc592ac535594d5de4bdde449e8dc012ac66909064b6880f8b717`.
- Gemma 4 E2B IT (instruction-tuned) LiteRT-LM — [source](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/6e5c4f1e395deb959c494953478fa5cec4b8008f), 2.588.147.712 bytes, SHA-256 `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`. Model ini berasal dari base Gemma 4 E2B dan varian instruction-tuned-nya; penggunaan Gemma mengikuti [Gemma Terms of Use](https://ai.google.dev/gemma/terms).
- all-MiniLM-L6-v2 ONNX untuk RAG — [source](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/1110a243fdf4706b3f48f1d95db1a4f5529b4d41), 23.026.053 bytes, 384 dimensi, SHA-256 `4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474`.
- Gemma 3, Alif Islamic, Qwen GGUF, dan Qwen3 Embedding tetap tampil sebagai sumber tambahan, tetapi tidak dapat diunduh atau dipakai sebelum format dan runtime-nya benar-benar cocok dengan aplikasi.

## Audio Quran: unduh dan putar offline

Audio murottal diunduh per ayat dari [katalog EveryAyah](https://everyayah.com/recitations_ayat.html). Implementasi memakai folder qari yang sudah dipetakan ke URL HTTPS nyata, manifest `000_checksum.md5` dari sumber, HTTP Range untuk melanjutkan file `.tmp`, WorkManager dengan batas retry jaringan, lalu publish atomik setelah MD5 cocok. [Dokumentasi Quran Foundation](https://api-docs.quran.com/docs/content_apis_versioned/4.0.0/list-surah-recitation/) juga mendokumentasikan pola URL audio per ayat yang digunakan oleh layanan Quran.

- Qari tersedia: Mishary Alafasy, Mahmud Khalil Al-Husary, dan Abdurrahman As-Sudais.
- Audio tidak dibundel ke APK. Pilih surah pada More → Audio, tunggu status `Audio surah selesai diunduh`, lalu buka ayat dan pilih `Putar Audio Murottal`.
- File terverifikasi disimpan sebagai cache app-private di `filesDir/audio`; cache ini dapat dihapus dari Audio Manager dan akan ikut terhapus saat aplikasi di-uninstall. Model AI dan dokumen RAG memiliki alur SAF terpisah agar tetap berada di folder pengguna.
- EveryAyah menjadi sumber rekaman dan checksum; aplikasi tidak mengubah atau mengklaim lisensi rekaman tersebut. Hak penggunaan tetap mengikuti sumber dan qari.

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
