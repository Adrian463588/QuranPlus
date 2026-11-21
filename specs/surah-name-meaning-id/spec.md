# Specification — `surah-name-meaning-id` (114 Indonesian Surah Name Meanings)

## 1. Current vs Expected Behavior
- **Current:** Surah list rows (`SurahItemRow`) and dual-pane detail (`SurahDetailPane`) display Arabic title, transliterated name (`nameLatin`), classification (`MAKKIYAH`/`MADANIYAH`), and `ayahCount`. Detail pane also shows a generic reader explanation sentence. `SearchScreen` displays `nameEnglish` in Surah cards and ignores Indonesian Surah name meanings.
- **Expected:** Every one of all 114 Surahs displays its verified Indonesian name meaning (`nameMeaningId`) in both list items and the selected detail pane, removes redundant generic copy in the detail pane, supports searching by Indonesian Surah meaning cleanly inside existing search flows, and preserves all Arabic names, ayah counts, classifications, ayah translations, tajwid, and user bookmarks/markers.

## 2. Functional Requirements (FR-01 .. FR-07)
- **FR-01 (Domain Field):** Add `val nameMeaningId: String = ""` to `com.quranplus.shared.features.quran.domain.Surah` (`nameMeaningId` distinguishes Surah name meanings from Ayah `translationId`).
- **FR-02 (Single Source of Truth):** Store all 114 verified Kemenag RI Indonesian meanings once in `SurahMapper.SURAHS` (`meaningId`) and expose `SurahMapper.getMeaningId(surahNumber)`.
- **FR-03 (Repository & MVVM Propagation):** Hydrate `nameMeaningId` inside `QuranRepositoryImpl.getAllSurahs()` and `QuranRepositoryImpl.getSurah(surahNumber)` via a shared mapper so `GetSurahListUseCase`, `GetSurahDetailUseCase`, and `QuranViewModel` expose it automatically over `StateFlow`.
- **FR-04 (Surah List UI):** Render `surah.nameMeaningId` beneath `surah.nameLatin` in `SurahItemRow` using secondary Material 3 typography (`bodySmall`, `onSurfaceVariant`, `maxLines = 2`, `TextOverflow.Ellipsis`), clearly subordinate to `nameLatin` and above `REVELATION • N Ayat`.
- **FR-05 (Selected Surah Detail Pane):** Render `surah.nameMeaningId` beneath `surah.nameLatin` (`titleMedium`, `onSurfaceVariant`) above `REVELATION • N Ayat`, preserve `surah.nameArabic` (`displaySmall`, RTL/End), keep `[Buka reader]`, remove redundant generic explanatory copy, and preserve `selectedSurahNumber` across rotation via `rememberSaveable`.
- **FR-06 (Search Compatibility):** Extend `SurahMapper.matchesSurah(...)` to match `nameMeaningId` (`"Pembukaan"`, `"Sapi Betina"`, `"Perempuan"`, `"Hidangan"`, etc.) without altering Arabic prefix stripping or SQLite FTS5 Ayah search, and display `nameMeaningId` in `SearchScreen` Surah match cards and pass `meaningId` in `QuranReaderScreen` jump picker.
- **FR-07 (Automated Content Validation):** Extend `scripts/validate_quran_corpus.py` to validate all 114 entries in `SurahMapper.kt` against `quranplus.db` (114 IDs, non-blank meanings, matching ayah counts, golden fixtures).

## 3. Non-Functional Requirements
- **NFR-01:** Offline-first (zero network calls required).
- **NFR-02:** Zero unnecessary Room DB migration or 45 MB binary SQLite rewrite.
- **NFR-03:** Adaptive layout compatibility across compact phones, expanded tablets, portrait/landscape orientations, and `1.3x`–`2.0x` accessibility font scale without text clipping or overlap.

## 4. Acceptance Criteria (`AC-01` .. `AC-10`)
- **AC-01:** Loading Surahs returns all 114 Surahs with non-blank verified `nameMeaningId`.
- **AC-02:** Surah 4 (`An-Nisaa`) maps to `"Perempuan"`; `#1 Pembukaan`, `#2 Sapi Betina`, `#3 Keluarga Imran`, `#5 Hidangan`, `#6 Binatang Ternak`, `#7 Tempat yang Tertinggi`, `#8 Rampasan Perang`, `#9 Pengampunan`, `#10 Yunus`.
- **AC-03:** `SurahDetailPane` displays Arabic title, `nameLatin`, `nameMeaningId` once beneath `nameLatin`, classification + ayah count, and `[Buka reader]` without redundant promotional copy.
- **AC-04:** Arabic names, ayah counts (`6,236` total), Makkiyah/Madaniyah, ayah translations, tajwid, and bookmarks/markers remain unchanged.
- **AC-05:** Works 100% offline with Wi-Fi and mobile data disabled.
- **AC-06:** Readable without overlap at compact width and tablet landscape/portrait and 2.0x font scaling.
- **AC-07:** Tapping `"Buka reader"` or Surah row navigates to the selected Surah at Ayah 1.
- **AC-08:** Existing `MARKER #1` and bookmarks survive non-destructive upgrade and remain clickable.
- **AC-09:** Search queries `"Pembukaan"`, `"Sapi Betina"`, `"Perempuan"`, `"Hidangan"` return the corresponding Surahs alongside existing number/Latin/Arabic search.
- **AC-10:** Source provenance and 8-entry Kemenag reconciliation are documented in `specs/surah-name-meaning-id/research.md`.
