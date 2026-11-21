# OpenSpec SDD: 03-surah-name-meaning-id

## Metadata
- **Specification ID**: QUR-114
- **Feature**: Indonesian Surah Name Meanings (All 114 Surahs)
- **Target Platform**: Android 12+ (API 32+), Kotlin 2.0+, Jetpack Compose, KMM Shared Domain
- **Methodology**: BMAD + OpenSpec Spec-Driven Development (SDD) + Spec Kit (`specs/surah-name-meaning-id/`)
- **Discipline**: Ponytail (minimal-diff), KISS, YAGNI, DRY, SOLID, Clean Architecture, MVVM

---

## 1. BMAD Role Architecture

- **Architect**: Defines Clean Architecture flow (`SurahMapper` Data SSOT -> `QuranRepositoryImpl` -> `shared` `Surah.nameMeaningId` -> existing `GetSurahListUseCase` / `GetSurahDetailUseCase` -> `QuranViewModel` `StateFlow` -> Compose UI) with zero unnecessary DB migrations or duplicate models.
- **Analyst**: Audits `quranplus.db`, `surahs_seed.json`, and `SurahMapper.kt`, and reconciles authoritative Kementerian Agama RI sources (`web-api.qurankemenag.net` LPMQ 2019 + Mushaf Standar Kemenag RI *Al-Qur'an dan Terjemahannya*).
- **Developer**: Applies minimal diffs to `Models.kt`, `SurahMapper.kt`, `QuranRepositoryImpl.kt`, `SurahListScreen.kt`, `SearchScreen.kt`, `QuranReaderScreen.kt`, and `validate_quran_corpus.py`.
- **QA / Safety Officer**: Executes Python 114-Surah corpus validator, JVM unit/repository/ViewModel tests, Compose UI & font-scaling tests, R8 build, Android Lint, secret scan, and real-device ADB QA across portrait/landscape/offline/reader flows.

---

## 2. Invariants

- **INV-01 (114-Surah Completeness)**: Exactly 114 unique Surahs (`1..114`), one verified non-blank Indonesian name meaning (`nameMeaningId`) per Surah, zero duplicates.
- **INV-02 (Corpus & User Data Preservation)**: Zero changes to Arabic Surah names, Ayah counts (`6,236` total), Makkiyah/Madaniyah classification, Quran Arabic text, Ayah translations, Tajwid markup, or user bookmarks/markers (`firstInstallTime` preserved on upgrade).
- **INV-03 (Authoritative Provenance & Zero LLM Translation)**: Every Indonesian meaning traces directly to official Kementerian Agama RI publications; no speculative/LLM-generated etymologies.
- **INV-04 (Clean Architecture & Ponytail Minimalism)**: Zero Room schema migration required, zero new backend/API calls, zero new ViewModels or Use Cases, and zero hardcoded 114-entry lists inside Composables.
- **INV-05 (Adaptive Readability)**: Compact phones, tablets (portrait/landscape), and 2.0x accessibility font scaling render Surah meanings clearly without clipping, overlap, or redundant filler copy.

---

## 3. BDD Scenarios & Verification Gates

- **AC-01..AC-10**: Covered in `specs/surah-name-meaning-id/spec.md` and automated in `SurahAndAyahSearchTest`, `QuranDatabaseInstrumentedTest`, and `scripts/validate_quran_corpus.py`.
- **Gates**:
  1. Spec Kit & BMAD documents committed cleanly (`specs/surah-name-meaning-id/` & `openspec/specs/03-surah-name-meaning-id.md`).
  2. Python `validate_quran_corpus.py` passes (`114/114` Surahs + `6,236` Ayahs + `77,429` words).
  3. `:app:testDebugUnitTest`, `:app:lintDebug` (`0` errors), `:app:assembleDebug`, and `:app:assembleRelease` (R8) pass.
  4. Physical device (`SM-T975`) UI/instrumentation tests pass offline across compact/landscape/portrait/reader/bookmarks/search/large font scale.
  5. Scoped Git commit pushed to `origin/main` with green GitHub Actions CI.
