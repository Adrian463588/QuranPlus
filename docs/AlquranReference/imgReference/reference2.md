# Daftar Fitur Aplikasi Al-Qur'an Digital

Disusun dari 2 sumber pada lampiran:
- **Sumber A** (gambar 4–5): tangkapan layar `Pengaturan` dan `Pengaturan Tajwid` pada aplikasi.
- **Sumber B** (gambar 1–3): halaman "Tanda-Tanda Baca / Panduan Hukum-Hukum Tajwid" dari mushaf cetak — dijadikan basis konten untuk fitur referensi/legenda di dalam app.

> Catatan akurasi: teks Arab pada tabel di bawah ditranskripsi dari hasil foto halaman fisik, jadi disarankan dicek ulang terhadap mushaf sumber sebelum dipakai sebagai data produksi (terutama harakat/tasydid).

---

## 1. Pengaturan Tampilan

| Fitur | Deskripsi | Nilai contoh di screenshot |
|---|---|---|
| Tema Aplikasi | Mengubah skema warna app | Gelap (Dark) |
| Mode Baca Qur'an | Mengatur tata letak ayat saat dibaca | Baris Per Ayat |

## 2. Pengaturan Teks Arab

| Fitur | Deskripsi | Nilai contoh |
|---|---|---|
| Jenis Penulisan Arabic | Memilih jenis rasm/mushaf (mis. gaya penulisan Asia vs Utsmani) | IndoPak (Asia) |
| Tajwid Berwarna | Toggle untuk mewarnai huruf sesuai hukum tajwid beserta penjelasannya (terhubung ke Fitur 7) | OFF |
| Ukuran Font Arabic | Slider/pilihan ukuran huruf Arab | 18 px |

## 3. Fitur Transliterasi Latin

| Fitur | Deskripsi | Nilai contoh |
|---|---|---|
| Aktifkan Latin | Menampilkan transliterasi Latin dari teks Arab | ON |
| Ukuran Font Latin | Mengatur ukuran teks transliterasi | 16 px |

## 4. Fitur Terjemahan

| Fitur | Deskripsi | Nilai contoh |
|---|---|---|
| Aktifkan Terjemahan | Menampilkan terjemahan Bahasa Indonesia | ON |
| Penerjemah | Sumber/rujukan terjemahan yang dipakai | Kemenag-RI |
| Ukuran Font Terjemahan | Mengatur ukuran teks terjemahan | 16 px |
| Kata Demi Kata (beta) | Menampilkan terjemahan per-kata di bawah tiap kata Arab | OFF, masih versi beta |

## 5. Fitur Audio Murattal

| Fitur | Deskripsi | Nilai contoh |
|---|---|---|
| Qori Murattal | Memilih qari untuk audio bacaan | Mishary Rashid |
| Audio Manager | Unduh/hapus file audio murattal, mendukung multi-select | — |

## 6. Fitur Lainnya

| Fitur | Deskripsi | Nilai contoh |
|---|---|---|
| Aksi Popup Ayat | Menentukan trigger munculnya popup opsi ayat (tafsir/audio/dll) | Diklik |
| Biarkan Layar Menyala | Mencegah layar mati otomatis saat membaca | ON |
| Layar Penuh | Sembunyikan status/notification bar saat membaca | OFF |

---

## 7. Fitur Tajwid Berwarna — Detail per Hukum

Setiap hukum di bawah punya **toggle on/off tersendiri**, deskripsi definisi, cara baca, dan contoh ayat dengan tombol putar audio (▶).

### 7.1 Ghunnah — ON
- **Definisi:** berlaku ketika huruf Nun ber-tasydid (نّ) atau Mim ber-tasydid (مّ).
- **Cara baca:** didengungkan dan ditahan ± 2 harakat sebelum huruf Nun/Mim dilafalkan jelas.
- **Catatan:** jika huruf sebelumnya nun mati/tanwin, hukum berubah jadi *Idgham Bighunnah*.
- **Contoh:** ثُمَّ لَتُرَوُنَّهَا عَيْنَ الْيَقِيْنِ

### 7.2 Idgham Bighunnah — ON
- **Definisi:** Nun mati (نْ) atau tanwin bertemu salah satu dari 4 huruf **و م ن ي**.
- **Cara baca:** melebur dengan huruf berikutnya, huruf N "hilang", didengungkan ± 2 harakat.
- **Pengecualian:** bila Nun mati bertemu Wau/Ya dalam satu kata (tanpa spasi), hukumnya jadi *Idzhar Wajib* (jelas, tanpa dengung) — contoh: QS. Al-Baqarah:86 pada kata "dun-yaa". Kasus ini sengaja tidak diberi warna khusus di app.
- **Contoh:** فَمَنْ يَّعْمَلْ مِثْقَالَ ذَرَّةٍ خَيْرًا يَّرَهُ

### 7.3 Idgham Bilaghunnah — OFF
- **Definisi:** Nun mati/tanwin bertemu salah satu dari 2 huruf **ل ر**.
- **Cara baca:** melebur tanpa dengung, ditahan ± 1–2 harakat.
- **Contoh:** وَيْلٌ لِّكُلِّ هُمَزَةٍ لُّمَزَةٍ

### 7.4 Idgham Mitslain (Mimi) — OFF
- **Definisi:** Mim mati (مْ) bertemu Mim berharakat (mim hidup).
- **Cara baca:** melebur dan didengungkan ± 2 harakat sebelum Mim hidup dilafalkan jelas.
- **Contoh:** رَانَ عَلٰى قُلُوْبِهِمْ مَّا كَانُوْا يَكْسِبُوْنَ

### 7.5 Iqlab — ON
- **Definisi:** Nun mati/tanwin bertemu huruf **ب**.
- **Cara baca:** nun mati/tanwin diganti bunyi mim mati, ditahan ± 2 harakat sebelum Ba dilafalkan jelas.
- **Catatan mushaf:** sebagian mushaf menuliskan mim kecil di antara huruf, sebagian tidak.
- **Contoh:** اَلِيْمٌ بِمَا كَانُوْا يَكْذِبُوْنَ

### 7.6 Ikhfa — ON
- **Definisi:** Nun mati/tanwin bertemu salah satu dari 15 huruf: **ت ث ج د ذ ز س ش ص ض ط ظ ف ق ك**.
- **Cara baca:** disamarkan seperti bunyi "NG", ditahan ± 2 harakat.
- **Sub-kategori:**
  - *Ikhfa Aqrob* (Dekat) — NG kurang jelas, 3 huruf: **ت د ط**
  - *Ikhfa Ausath* (Pertengahan) — NG sedang, 10 huruf: **ث ج ذ س ز ش ص ض ظ ف**
  - *Ikhfa Ab'ad* (Jauh) — NG jelas, 2 huruf: **ق ك**
- **Contoh:** وَكُنْتُمْ اَمْوَاتًا فَاَحْيَاكُمْ

### 7.7 Ikhfa Syafawi — ON
- **Definisi:** Mim mati (مْ) bertemu huruf **ب**.
- **Cara baca:** disamarkan seperti bunyi "MNG", ditahan ± 2 harakat sebelum Ba jelas.
- **Contoh:** لَسْتَ عَلَيْهِمْ بِمُصَيْطِرٍ

### 7.8 Qalqalah — ON
- **Definisi:** salah satu dari 5 huruf **ق ط ب ج د** mati (sukun) di tengah kalimat.
  - Di tengah kalimat → **Qalqalah Sughra** (kecil)
  - Diwaqafkan/di akhir ayat → **Qalqalah Kubra** (besar) — jika bacaan tidak dihentikan, tidak di-qalqalah-kan.
- **Cara baca:** dipantulkan.
- **Contoh:** اِقْرَأْ بِاسْمِ رَبِّكَ الَّذِيْ خَلَقَ

---

## 8. Fitur Referensi Simbol Tajwid (Legenda)

Konten berikut cocok dijadikan halaman "Bantuan/Panduan Tajwid" di dalam app, berisi legenda simbol yang muncul pada teks Arab:

| Simbol/Istilah | Keterangan |
|---|---|
| Tanda hukum Iqlab | 2 harakat |
| Hukum Idgham | — |
| Tanda Bigairi Gunnah | — |
| Tanda Qalqalah Sughra | — |
| Mad Wajib Muttasil | 4/5 harakat |
| Mad Ja'iz Munfasil | 4/5 harakat |
| Hukum Idgaam | — |
| Hukum Ikhfa | 2 harakat |
| Hukum Izhar | tanpa dengung |
| Mad Silah Kubra | 4/5 harakat |
| Tanda Mad Lazim | 6 harakat (wajib) |
| Tanda Mad Farq | 6 harakat (wajib) |
| Mad Tabi'i (Mad Asli) | 2 harakat |
| Huruf berwarna biru | tidak perlu dilafalkan |

## 9. Fitur Referensi Tanda Waqaf

| Simbol | Arti |
|---|---|
| لا | Dilarang berhenti kecuali di akhir ayat |
| صلى | Lebih baik diteruskan |
| ج | Boleh berhenti atau meneruskan bacaan |
| ٭ ٭ (mu'anaqah) | Berhenti pada salah satu tanda (bukan keduanya) |
| م | Diharuskan berhenti |
| قلى | Lebih baik berhenti |

**Pedoman praktis berwaqaf** (ditampilkan sebagai tips/FAQ dalam fitur ini):
1. Senantiasa berwaqaf pada akhir ayat dan berlanjut ke ayat berikutnya tanpa perlu mengulang.
2. Jika menemukan ayat panjang, berwaqaf pada tanda waqaf di atas tanpa perlu mengulang — kecuali pada tanda لا, di mana disunnahkan tetap berhenti di akhir ayat menurut sebagian ulama.
3. Jika akhir ayat masih panjang dan tidak ada tanda waqaf, berwaqaf pada akhir napas dengan mengulang beberapa kata sebelumnya saat memulai bacaan lagi.

---

## 10. Fitur Database Ayat Khusus (Referensi/Pencarian Ayat)

Bagian ini berpotensi menjadi fitur **"cari ayat dengan hukum bacaan khusus"** di dalam app — berguna untuk pengguna yang ingin berlatih kasus-kasus tajwid tidak umum.

### 10.1 Nun Wiqayah (Nun Waṣal)
*Nun kecil di bawah huruf wasal, dibaca kasrah saat tanwin bertemu hamzah wasal.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Al Baqarah | 180 | 27 |
| 2 | Yusuf | 8 | 236 |
| 3 | Al Kahf | 88 | 303 |
| 4 | An Najm | 50 | 528 |
| 5 | Al Jumu'ah | 11 | 554 |

### 10.2 Ayat-ayat Sajdah
*Ayat yang mengandung perintah sujud — disunnahkan sujud tilawah saat membaca/mendengarnya. Total 15 ayat.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Al-A'raf | 206 | 176 |
| 2 | Ar-Ra'd | 15 | 251 |
| 3 | An-Nahl | 50 | 272 |
| 4 | Al-Isra' | 109 | 293 |
| 5 | Maryam | 58 | 309 |
| 6 | Al-Hajj | 18 | 334 |
| 7 | Al-Hajj | 77 | 341 |
| 8 | Al-Furqan | 60 | 365 |
| 9 | An-Naml | 26 | 379 |
| 10 | As-Sajdah | 15 | 416 |
| 11 | Sad | 24 | 454 |
| 12 | Fussilat | 38 | 480 |
| 13 | An-Najm | 62 | 528 |
| 14 | Al-Insyiqaq | 21 | 589 |
| 15 | Al-'Alaq | 19 | 597 |

### 10.3 Tanda Sifir Mustadir (bulatan kecil bulat)
*Huruf tidak dibaca panjang baik saat waqaf maupun wasal.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Yusuf | 87 | 246 |
| 2 | Al-Kahf | 23 | 296 |
| 3 | Al-A'raf | 103 | 163 |
| 4 | Yunus | 75 | 217 |
| 5 | Az-Zukhruf | 46 | 492 |
| 6 | Ar-Rum | 39 | 408 |
| 7 | Al-Insan | 4 | 578 |
| 8 | Al-Insan | 16 | 579 |

### 10.4 Tanda Sifir Mustatil (bulatan lonjong)
*Huruf dibaca panjang saat waqaf, tapi pendek saat wasal.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Al-Kafirun | 4 | 603 |
| 2 | Al-Kahf | 38 | 298 |
| 3 | Al-Ahzab | 10 | 419 |
| 4 | Al-Insan | 15 | 579 |

### 10.5 Ayat-ayat Saktah
*Berhenti sejenak tanpa bernapas selama 2 harakat.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Al-Kahf | 1 | 293 |
| 2 | Yasin | 52 | 443 |
| 3 | Al-Qiyamah | 27 | 578 |
| 4 | Al-Mutaffifin | 14 | 588 |

### 10.6 Imalah
*Huruf berharakat fathah dibaca dimiringkan ke arah kasrah.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Hud | 41 | 226 |

### 10.7 Isymam
*Menempatkan dhammah yang "dibuang" dengan isyarat bibir (tanpa suara).*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Yusuf | 11 | 236 |

### 10.8 Tashil
*Membaca hamzah kedua (dari 2 hamzah beruntun) dengan suara ringan/lunak.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Fussilat | 44 | 481 |

### 10.9 Naql
*Memindahkan harakat hamzah ke huruf sukun sebelumnya.*

| No | Surah | Ayat | Halaman |
|---|---|---|---|
| 1 | Al-Hujurat | 11 | 516 |

---

## Ringkasan Kategori Fitur

1. **Pengaturan Umum** — tampilan, font, latin, terjemahan, audio, lainnya (9 sub-fitur, 6 kategori).
2. **Tajwid Berwarna** — 8 hukum tajwid, masing-masing togglable independen, lengkap definisi + cara baca + contoh audio.
3. **Referensi/Legenda** — simbol tajwid mushaf & tanda waqaf, cocok jadi halaman bantuan in-app.
4. **Database Ayat Khusus** — 9 kategori kasus bacaan langka (total ±40 entri ayat) yang bisa dijadikan fitur pencarian/index referensi di app.