# Research & Data Provenance — `surah-name-meaning-id`

## 1. Repository Audit Findings

- **Database Schema (`app/src/main/assets/databases/quranplus.db`, Room v16):** `surahs` table contains `id`, `number`, `name_arabic`, `name_latin`, `name_english` (`"The Opening"`, `"The Cow"`, ...), `revelation_type`, `ayah_count`. Indonesian Surah name meanings were absent.
- **Domain Model (`shared/src/commonMain/kotlin/com/quranplus/shared/features/quran/domain/Models.kt`):** `Surah` held `number`, `nameArabic`, `nameLatin`, `nameEnglish`, `revelationType`, `ayahCount`, lacking an Indonesian Surah name meaning field (`nameMeaningId`).
- **Existing Static 114-Surah Catalog (`app/src/main/java/com/quranplus/app/core/utils/SurahMapper.kt`):** Already maintains the single Kotlin `SURAHS` list (`1..114`, `number`, `latinName`, `ayahCount`) used by `QuranRepositoryImpl`, `SearchScreen`, `QuranReaderScreen`, and `SurahAndAyahSearchTest`.
- **UI Gap (`SurahListScreen.kt`, `SearchScreen.kt`):**
  - `SurahItemRow` rendered only `nameLatin` + `revelationType • ayahCount Ayat`.
  - `SurahDetailPane` displayed Arabic title, `nameLatin`, `revelationType • ayahCount Ayat`, and the generic filler sentence `"Baca surah ini dengan tampilan reader yang mendukung tajwid, terjemahan, transliterasi, dan bookmark."`
  - `SearchScreen` displayed `surah.nameEnglish` in search result cards instead of Indonesian Surah meanings and did not match Indonesian Surah meanings.

## 2. Authoritative Source & Permitted Usage

- **Primary Source:** **Lajnah Pentashihan Mushaf Al-Qur'an (LPMQ), Kementerian Agama RI** (`https://web-api.qurankemenag.net/quran-surah`, *Al-Qur'an dan Terjemahannya Edisi Penyempurnaan 2019*, public government reference used by `quran.kemenag.go.id`). Matches `quranplus.db`'s existing `Kemenag RI` provenance in `tafsirs`.
- **Secondary Kemenag Reference:** **Kementerian Agama RI *Al-Qur'an dan Terjemahannya* (Mushaf Standar / Edisi 2002)** (`equran.id/api/v2/surat` & public Kemenag index) for full classical Indonesian forms.
- **Permitted Usage:** Public educational/non-commercial religious reference attributed in `README.md`. Zero LLM-generated translations.

## 3. Editorial Reconciliation Across Kemenag Editions

Where official Kemenag editions differ slightly (e.g., 2019 LPMQ shortened forms vs classical Kemenag Mushaf Standar titles required by project fixtures), 8 entries use the verified classical Kemenag RI form while preserving exact LPMQ 2019 meanings for all remaining 106 Surahs:

| Surah # | Surah | LPMQ API (2019) | Approved Meaning | Rationale |
|---:|---|---|---|---|
| 1 | Al-Fatihah | Pembuka | **Pembukaan** | Classical Kemenag Mushaf Standar form (`#1` fixture). |
| 2 | Al-Baqarah | Sapi | **Sapi Betina** | Classical Kemenag Mushaf Standar feminine noun (`#2` fixture). |
| 4 | An-Nisa' | Perempuan | **Perempuan** | Preserved from LPMQ 2019 (`AC-02`, vs 2002 `"Wanita"`). |
| 7 | Al-A'raf | Tempat Tertinggi | **Tempat yang Tertinggi** | Classical Kemenag Mushaf Standar form (`#7` fixture). |
| 38 | Sad | Ṣād | **Sad** | Kemenag 2002 ASCII-clean Indonesian orthography. |
| 47 | Muhammad | Nabi Muhammad | **Muhammad** | Kemenag 2002 form; uniform proper-name policy with Yunus/Yusuf/Ibrahim/Maryam/Luqman/Nuh. |
| 97 | Al-Qadr | Al-Qadar | **Kemuliaan** | Classical Kemenag Indonesian translation instead of Arabic repeat. |
| 101 | Al-Qari'ah | Al-Qāri‘ah | **Hari Kiamat** | Classical Kemenag Indonesian translation instead of Arabic repeat. |
| 109 | Al-Kafirun | Orang-Orang kafir | **Orang-Orang Kafir** | Title-case consistency with LPMQ 2019 headings. |
