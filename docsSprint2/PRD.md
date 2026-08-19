# PRD — Quran Plus (Sprint 2)
**Platform:** Android (Jetpack Compose KMM Multiplatform)
**Target Device:** Poco X7 Pro (Dimensity 9300+, 8–12 GB RAM) dan perangkat Android 12+ (API 32+)
**Versi:** 2.0.0
**Tanggal:** 2026-08-19

---

## 1. Ringkasan Eksekutif Produk

**Quran Plus** adalah aplikasi *offline-first* komprehensif yang mengintegrasikan pengalaman membaca Al-Qur'an tingkat lanjut (mushaf Utsmani & IndoPak, tajwid berwarna interaktif, audio murattal multi-qari, bacaan gharib, kata-per-kata), modul pembelajaran tahsin & tajwid terstruktur, serta **Chatbot RAG AI Lokal** yang berjalan 100% *on-device* dengan basis pengetahuan sahih (Al-Qur'an, Tafsir, dan Kutubus Sittah).

### Nilai Utama Produk
| Pilar | Deskripsi |
|---|---|
| **Privasi & Offline Absolut** | Seluruh pemrosesan teks, audio offline, database vector, dan inferensi LLM dieksekusi di perangkat tanpa kebocoran data. |
| **Pengalaman Membaca Holistik** | Teks Arab Utsmani/IndoPak, tajwid berwarna granular (bisa di-toggle per hukum), terjemahan per ayat & kata-demi-kata, audio manager, dan tanda waqaf interaktif. |
| **Ensiklopedia Bacaan Gharib** | Navigasi dan visualisasi bacaan khusus (Imalah, Isymam dengan animasi gerakan bibir, Tashil, Naql, Saktah, Sifir, Nun Wiqayah, dan Ayat Sajdah). |
| **AI Islami Berbasis RAG Sahih** | Chatbot lokal dengan ground truth Al-Qur'an dan Hadis Sahih, sitasi transparan yang dapat di-tap langsung membuka ayat terkait di reader. |
| **Tahsin & Latihan Interaktif** | Pembelajaran makharij, sifat huruf, hukum tajwid, quiz interaktif, dan komparasi audio pelafalan. |

---

## 2. Fitur & Spesifikasi Fungsional (Sprint 2)

### 2.1 Modul Pembacaan Al-Qur'an & Tampilan

#### [F-01] Mode Pembacaan Fleksibel
- **F-01.1 Tampilan Baris Per Ayat:** Menampilkan ayat Al-Qur'an dalam format list vertikal berurutan dengan nomor ayat, teks Arab, transliterasi Latin, dan terjemahan.
- **F-01.2 Navigasi Surah, Juz, dan Halaman:** Dukungan pemilihan 114 surah, 30 juz, serta penandaan halaman standar Mushaf Madani.
- **F-01.3 Last Read Position Sync:** Pencatatan otomatis ayat dan posisi scroll terakhir yang dibaca pengguna.
- **F-01.4 Keep Screen On:** Opsi menjaga layar perangkat tetap menyala selama aktivitas membaca.
- **F-01.5 Layar Penuh (Immersive Mode):** Menyembunyikan status bar dan system navigation bar saat mode membaca aktif.

#### [F-02] Kustomisasi Teks & Font
- **F-02.1 Jenis Rasm / Penulisan Arab:** Pilihan font Rasm Utsmani (Madani KFGQPC) dan IndoPak / Asia Style.
- **F-02.2 Slider Ukuran Font Arab:** Pengaturan ukuran font teks Arab (18px – 48px) dengan preview instan.
- **F-02.3 Transliterasi Latin:** Toggle aktifkan/nonaktifkan teks Latin beserta slider pengaturan ukuran font mandiri (12px – 24px).
- **F-02.4 Terjemahan Bahasa Indonesia & Inggris:** Toggle terjemahan, pilihan sumber (Kemenag-RI, King Saud University, Tafsir Ringkas), dan slider ukuran font terjemahan (12px – 24px).
- **F-02.5 Terjemahan Kata Demi Kata (Word-by-Word):**
  - Teks terjemahan per kata di bawah masing-masing lafaz Arab.
  - On-tap popup kata: menampilkan teks Arab, transliterasi, arti kata, akar kata (root word), dan audio lafaz kata individual.

#### [F-03] Aksi Popup / Bottom Sheet Ayat
Ketika pengguna mengetuk sebuah ayat, muncul bottom sheet aksi terpadu:
- `Putar Ayat (Audio)`: Memutar murottal ayat terpilih.
- `Ulangi (Repeat)`: Mengulang pemutaran ayat (1–10x atau loop tak terbatas).
- `Bookmark & Catatan`: Menyimpan ayat ke daftar bookmark dan menambahkan catatan refleksi pribadi.
- `Salin Ayat / Referensi`: Menyalin teks Arab, transliterasi, terjemahan, atau tautan format `QS. NamaSurah:NomorAyat`.
- `Bagikan (Share)`: Berbagi ayat sebagai teks atau kartu ayat visual.
- `Detail Tafsir`: Menampilkan penjelasan tafsir (Tafsir Jalalayn / Ringkas Kemenag).
- `Detail Tajwid`: Membuka rincian hukum tajwid yang terkandung dalam ayat tersebut.

---

### 2.2 Modul Tajwid Berwarna Granular & Interaktif

#### [F-04] Mesin Pewarnaan Tajwid Granular
Pewarnaan berbasis `AnnotatedString` dengan toggle ON/OFF independen per masing-masing hukum:

| Hukum Tajwid | Warna Token | Hex Code | Deskripsi & Durasi Harakat |
|---|---|---|---|
| **Ghunnah** | `TajwidGhunnah` | `#EC407A` | Nun & Mim bertasydid, dengung ditahan ±2 harakat. |
| **Idgham Bighunnah** | `TajwidIdgham` | `#4CAF50` | Nun mati/tanwin bertemu **ي ن م و**, lebur berdengung ±2 harakat. |
| **Idgham Bilaghunnah** | `TajwidIdghamBila` | `#81C784` | Nun mati/tanwin bertemu **ل ر**, lebur tanpa dengung ±1–2 harakat. |
| **Idgham Mitslain (Mimi)** | `TajwidIdghamMimi`| `#2E7D32` | Mim mati bertemu Mim berharakat, lebur berdengung ±2 harakat. |
| **Iqlab** | `TajwidIqlab` | `#EF5350` | Nun mati/tanwin bertemu **ب**, ganti bunyi Mim samar ±2 harakat. |
| **Ikhfa Haqiqi** | `TajwidIkhfa` | `#42A5F5` | Nun mati/tanwin bertemu 15 huruf Ikhfa (*Aqrab, Ausath, Ab'ad*). |
| **Ikhfa Syafawi** | `TajwidIkhfaSyafawi`| `#1E88E5` | Mim mati bertemu **ب**, samar berdengung ±2 harakat. |
| **Qalqalah** | `TajwidQalqalah` | `#FF7043` | Huruf **ق ط ب ج د** sukun (Sughra di tengah, Kubra saat waqaf). |
| **Izhar Halqi / Syafawi**| `TajwidIzhar` | `#78909C` | Dibaca jelas tanpa dengung. |
| **Mad Tabi'i** | `TajwidMadTabii` | `#BA68C8` | Panjang 2 harakat. |
| **Mad Wajib Muttasil** | `TajwidMadWajib` | `#8E24AA` | Panjang 4–5 harakat (wajib dalam satu kata). |
| **Mad Jaiz Munfasil** | `TajwidMadJaiz` | `#AB47BC` | Panjang 4–5 harakat (terpisah kata). |
| **Mad Silah Kubra** | `TajwidMadSilah` | `#7B1FA2` | Panjang 4–5 harakat setelah Ha Dhomir. |
| **Mad Lazim (Kilmi/Harfi)**| `TajwidMadLazim` | `#4A148C` | Wajib panjang 6 harakat. |
| **Mad Farq** | `TajwidMadFarq` | `#6A1B9A` | Wajib panjang 6 harakat. |

#### [F-05] Detail Tajwid Interaktif Saat Teks Ditekan
Ketika teks berwarna di dalam ayat diklik:
- Tampilkan Modal/Sheet berisi: Nama hukum, status hukum, huruf penyebab, aturan cara membaca, jumlah harakat, potongan ayat ber-highlight, dan tombol `Putar Audio Contoh Hukum`.

#### [F-06] Legenda & Panduan Warna Tajwid
Halaman ensiklopedia ringkas berisi seluruh daftar warna, nama hukum, simbol, cara pelafalan, dan tombol audio contoh.

---

### 2.3 Modul Waqaf, Ibtida', dan Tanda Baca Mushaf

#### [F-07] Panduan Simbol Tanda Waqaf Interaktif
Dukungan deteksi dan aksi pada tanda waqaf:
- `لا` (Dilarang berhenti kecuali akhir ayat).
- `صلى` (Al-Washlu Ula — Lebih baik diteruskan).
- `ج` (Waqaf Jaiz — Boleh berhenti atau lanjut).
- `∴ ∴` (Waqaf Mu'anaqah — Berhenti pada salah satu tanda, bukan keduanya).
- `م` (Waqaf Lazim — Diharuskan/diutamakan berhenti).
- `قلى` (Al-Waqfu Ula — Lebih baik berhenti).

#### [F-08] Pedoman Praktis Berwaqaf & Ibtida'
Panduan edukatif cara berhenti pada ayat panjang, regulasi pernapasan saat membaca, dan cara memulai kembali (*Ibtida'*) dengan makna yang tepat.

---

### 2.4 Ensiklopedia Bacaan Khusus / Gharib & Ayat Sajdah

#### [F-09] Direktori Bacaan Gharib Terpadu
Navigasi langsung (*Deep Link*) dari daftar hukum ke nomor surah & ayat terkait, dilengkapi pemutar audio dan komparasi cara membaca:

1. **Nun Wiqayah / Nun Wasal:**
   - Pertemuan tanwin dengan hamzah wasal dibaca nun berharakat kasrah.
   - Contoh terindeks: QS. Al-Baqarah: 180, Yusuf: 8, Al-Kahf: 88, An-Najm: 50, Al-Jumu'ah: 11.
2. **Ayat-Ayat Sajdah (15 Ayat):**
   - Penanda ikon sajda khusus, dialog penjelasan tata cara Sujud Tilawah dan bacaan doanya.
   - Daftar ayat: QS. Al-A'raf: 206, Ar-Ra'd: 15, An-Nahl: 50, Al-Isra': 109, Maryam: 58, Al-Hajj: 18 & 77, Al-Furqan: 60, An-Naml: 26, As-Sajdah: 15, Sad: 24, Fussilat: 38, An-Najm: 62, Al-Insyiqaq: 21, Al-'Alaq: 19.
3. **Sifir Mustadir (Bulatan Bulat):**
   - Huruf tidak dibaca panjang baik ketika waqaf maupun wasal.
   - Contoh: QS. Yusuf: 87, Al-Kahf: 23, Al-A'raf: 103, Yunus: 75, Az-Zukhruf: 46, Ar-Rum: 39, Al-Insan: 4 & 16.
4. **Sifir Mustatil (Bulatan Lonjong):**
   - Huruf dibaca panjang saat waqaf, namun dibaca pendek saat wasal.
   - Komparasi audio: Tombol `Putar Wasal` vs `Putar Waqaf`.
   - Contoh: QS. Al-Kafirun: 4, Al-Kahf: 38, Al-Ahzab: 10, Al-Insan: 15.
5. **Ayat-Ayat Saktah (4 Lokasi):**
   - Berhenti sejenak tanpa bernapas selama ±2 harakat.
   - Indikator jeda pada audio player.
   - Contoh: QS. Al-Kahf: 1, Yasin: 52, Al-Qiyamah: 27, Al-Mutaffifin: 14.
6. **Imalah (QS. Hud: 41):**
   - Bunyi fathah dimiringkan ke kasrah (pada kata *Majrehaa*).
   - Audio kecepatan normal & perlahan (0.75x).
7. **Isymam (QS. Yusuf: 11):**
   - Isyarat memoncongkan bibir tanda dhammah tersembunyi tanpa suara (pada kata *Laa Ta'mannaa*).
   - Dilengkapi diagram ilustrasi/animasi gerakan bibir dan audio.
8. **Tashil (QS. Fussilat: 44):**
   - Meringankan pengucapan hamzah kedua antara hamzah dan alif (pada kata *A'a'jamiyyun*).
9. **Naql (QS. Al-Hujurat: 11):**
   - Pemindahan harakat hamzah ke huruf sukun sebelumnya (pada kata *Bi'salismu*).

---

### 2.5 Audio Murattal & Audio Manager

#### [F-10] Pemutar Audio Murattal Per Ayat
- **Multi-Qari Support:** Pilihan qari ternama (Mishary Rashid Alafasy, Mahmud Khalil Al-Husary, Abdurrahman As-Sudais).
- **Playback Controls:** Play, Pause, Replay, Next Ayah, Previous Ayah, Repeat Ayah (1–10x / Loop).
- **Speed Pitch Control:** 0.5x, 0.75x, 1.0x, 1.25x (sangat berguna untuk muraja'ah dan belajar tajwid).
- **Audio Tajwid Khusus:** Pemutaran audio khusus per potongan hukum tajwid.

#### [F-11] Resumable Audio Manager
- Pengunduhan audio per surah atau multi-surah sekaligus.
- Mendukung *Range Request*, auto-pause saat kehilangan koneksi, dan auto-resume.
- Indikator meteran kapasitas penyimpanan internal yang digunakan.
- Opsi manajemen hapus audio offline untuk menghemat memori.

---

### 2.6 Belajar & Latihan Tajwid (Learning Mode)

#### [F-12] Modul Latihan & Quiz
- Kuis tebak hukum tajwid dari cuplikan ayat acak.
- Kuis identifikasi tanda waqaf dan tindakan yang tepat.
- Kuis penentuan jumlah ketukan/harakat mad.
- Dashboard statistik pembelajaran: persentase akurasi, materi yang dikuasai, dan materi yang perlu diulang.

---

### 2.7 Chatbot RAG AI Lokal (Quran + Sunnah Ground Truth)

#### [F-13] Arsitektur Inferensi LLM & RAG Lokal
- **Engine:** Google LiteRT-LM (`.litertlm` / `.task`) teroptimasi GPU/NPU.
- **Model Rekomendasi:**
  - *Primary:* Gemma 4 E2B Instruct (~2.58 GB) / Gemma-3-1B-IT (4-bit ~600 MB).
  - *Specialized:* Qwen2.5-1.5B-Instruct (fine-tuned Islamic QA).
- **Embeddings:** ONNX Runtime INT8 `all-MiniLM-L6-v2` (384-dim, ~23 MB) / Qwen3-Embedding LiteRT.
- **Vector DB:** `sqlite-vec` (vektor search cosine similarity di dalam SQLite).
- **Corpus Ground Truth:** Al-Qur'an 30 Juz, Tafsir Ringkas Kemenag, Hadis Shahih Bukhari & Muslim.
- **ModelGate System:** Menjaga UI dalam status `MODEL_UNAVAILABLE` secara jujur sampai file model terverifikasi (SHA-256 + atomic rename `.tmp`).

#### [F-14] Sitasi Transparan & Deep-Linking
- AI wajib menyertakan kutipan ayat atau nomor hadis rujukan pada akhir respons.
- Setiap badge sitasi ayat bersifat interaktif: saat ditekan, langsung membuka Reader Al-Qur'an pada surah dan ayat yang bersangkutan.

#### [F-15] Personalisasi AI Persona
- Pengaturan persona sistem di DataStore:
  - **Ustadz / Pendidik:** Ramah, edukatif, runtut dengan dalil.
  - **Mufti Akademis:** Formal, presisi, mencantumkan perbandingan pendapat ulama.
  - **Sahabat Belajar:** Kasual, ringkas, memotivasi.

---

### 2.8 Modul Tahsin & Transliterasi

#### [F-16] Kitab Tahsin Digital Terstruktur
- **Makharij al-Huruf:** Visualisasi 17 titik artikulasi huruf hijaiyah (Al-Jauf, Al-Halq, Al-Lisan, Asy-Syafatain, Al-Khaisyum).
- **Sifat al-Huruf:** Penjelasan sifat huruf berlawanan (Hams vs Jahr, Syiddah vs Rakhawah, dll.) dan tidak berlawanan (Qalqalah, Shafir, dll.).
- **Komparasi Pelafalan Fonetis:** Panduan simbol fonetis IPA yang disederhanakan untuk lisan Indonesia.

---

## 3. Arsitektur Teknis & Modul KMM

### 3.1 Struktur Multiplatform Feature-Modular
```
shared/
├── commonMain/
│   ├── core/
│   │   ├── di/               # Koin Dependency Injection
│   │   ├── database/         # Room 2.7+ Database (Quran, Tajwid, Waqaf, Gharib, Chat)
│   │   ├── vector/           # sqlite-vec bindings
│   │   ├── audio/            # Core Audio Player interface
│   │   └── utils/            # DispatcherProvider, Extensions
│   └── features/
│       ├── quran/            # Reader, Word-by-Word, Search FTS5, Bookmarks
│       ├── tajwid/           # Tajwid Engine, Legend, Interactive Tap, Settings
│       ├── gharib/           # Special Readings, Sajdah, Saktah, Imalah, Isymam
│       ├── waqaf/            # Waqaf & Ibtida' guides
│       ├── audio/            # Murattal Player, Audio Manager, Download worker
│       ├── tahsin/           # Makharij, Sifat, Learning & Quiz Engine
│       ├── chatbot/          # RAG Pipeline, Sitasi, Persona, LLM Orchestrator
│       └── settings/         # Theme, Fonts, Preferences
└── androidMain/
    ├── llm/                  # LiteRT-LM Android Engine
    ├── embedding/            # ONNX Runtime Embedder
    ├── audio/                # Media3 / ExoPlayer Implementation
    └── download/             # WorkManager Resumable Downloader
```

---

## 4. Rencana Rilis & Milestone Sprint 2

| Milestone | Target Waktu | Output Deliverables |
|---|---|---|
| **M1: Data & Audio Foundation** | Minggu 1 | Room schema (Waqaf, Gharib, Tajwid Rules), Media3 Audio Engine & Manager. |
| **M2: Reader & Tajwid Interactive** | Minggu 2 | Quran Reader per ayat, Tajwid Granular Engine, Word-by-word parser, Ayat action sheet. |
| **M3: Gharib & Learning Module** | Minggu 3 | Direktori 9 Bacaan Gharib, Sajdah dialog, Isymam visual, Quiz engine. |
| **M4: Local RAG Chatbot Integration** | Minggu 4 | LiteRT-LM + sqlite-vec integration, transparent citation linking ke reader. |
| **M5: Polish, Audit & Verification** | Minggu 5 | Anti-slop UI audit, performance tuning (60 FPS scroll), Baseline profiles. |
