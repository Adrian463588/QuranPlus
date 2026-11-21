# Tasks — `surah-name-meaning-id`

- [x] **T01 (Spec & Research):** Inspect Surah schema/UI and author `openspec/specs/03-surah-name-meaning-id.md` + `specs/surah-name-meaning-id/{research,spec,plan,tasks}.md` (`AC-10`).
- [x] **T02 (Domain & Single Source of Truth):** Add `nameMeaningId` to shared `Surah`, populate all 114 verified Kemenag RI meanings in `SurahMapper.SURAHS`, and add `getMeaningId` + `matchesSurah` Indonesian meaning matching (`AC-01`, `AC-02`, `AC-09`).
- [x] **T03 (Repository & MVVM Wiring):** Update `QuranRepositoryImpl` with shared `toDomainSurah()` mapper so `GetSurahListUseCase` / `GetSurahDetailUseCase` / `QuranViewModel` expose `nameMeaningId` (`AC-01`, `AC-04`).
- [x] **T04 (Compose UI & Adaptive Layout):** Update `SurahItemRow`, `SurahDetailPane` (remove redundant copy, show meaning once cleanly, use `rememberSaveable` for `selectedSurahNumber`), `SearchScreen`, and `QuranReaderScreen` jump picker (`AC-03`, `AC-06`, `AC-07`, `AC-08`).
- [x] **T05 (Automated Validation & Tests):** Update `scripts/validate_quran_corpus.py` (`114/114` check vs DB), expand `SurahAndAyahSearchTest.kt` (all 114 Surahs, fixtures, repo, ViewModel, search), and add Room + Compose UI tests in `QuranDatabaseInstrumentedTest.kt` (`AC-01..AC-09`).
- [ ] **T06 (DevSecOps R1–R8, Device QA & Git Delivery):** Run Python validators, unit tests, Android Lint, Debug + R8 Release APKs, real-device `SM-T975` offline/rotation/font-scale/reader verification, secret scan, update `tasks.md`, commit, push, and verify GitHub Actions CI green.
