# Baseline Audit — Sprint 3 (Hadith Integrity & Search Restoration)

- **Date:** 2026-11-20
- **Branch / Head:** `main` @ `fe29924` (`v1.0.12`, `versionCode = 13`)
- **Connected Physical Target:** `SM-G988B` (`adb` serial `RRCN3008VYE`), installed package `com.quranplus.app` `versionCode=13` `versionName=1.0.12`, active `/sdcard/QuranPlus/rag/source/hadith-indonesia-bundle.zip` (`15,036,414` bytes, SHA-256 `1f7ffe09ccf0be44293d3b68de036a37d4c54fc36d7f81390d8e5faeed547b1c`).
- **Baseline Checks (before edits):**
  - `python scripts/validate_dzikir_corpus.py`: PASS (`146 entries; 18 exact Quran passages; Latin unchanged`)
  - `python scripts/validate_hadith_numbering.py`: PASS (`28347 content-matched references; Abu Dawud 3340 -> 3837; ambiguous matches excluded`)
  - `gradlew :app:testDebugUnitTest :shared:testDebugUnitTest`: PASS (exit code 0)

## Root Causes Verified Against Installed Assets & Crosswalk

1. **Incomplete Local Bundle (`gadingnst/hadith-api`, rev `8e15b4f9e7585822426a0d470e8dfec27e2a1707`, MIT):**
   - Installed 9-book archive contains `38,102` Indonesian-only archive records (`bukhari`: `6,638`, `muslim`: `4,930`, `abudawud`: `4,419`, `tirmidhi`: `3,625`, `nasai`: `5,364`, `ibnmajah`: `4,285`, `malik`: `1,587`, `ahmad`: `4,305`, `darimi`: `2,949`).
   - Direct search of `scratch_device.db` (`38,102` rows) confirms **Bukhari 3617** (Christian apostate / earth expulsion), **Abu Dawud 4000** (`مَالِكِ يَوْمِ الدِّينِ` vs `مَلِكِ يَوْمِ الدِّينِ`), and **Tirmidhi 3000** (Abu Umamah & Khawarij `كلاب النار`) are **completely absent** from the `gadingnst` archive.
2. **One-Sided Crosswalk & Silent Unmapped Failure (`HadithRepositoryImpl` + `HadithNumbering`):**
   - `numbering.json` only mapped `28,347` records across 6 collections (omitting `malik` completely, and omitting any record where `ind-*` lacked text or had >1 match).
   - `HadithRepositoryImpl.searchReferenceNumber()` returned `emptyList()` whenever `numbering.archiveNumbers(collectionId, number)` was empty, causing the UI to display *"Hadist Tidak Ditemukan"* and claim `"Pastikan bundle Hadist lokal sudah diunduh"` even when the bundle was installed and verified.
3. **Missing Multi-Scheme, Multi-Translation, and Multi-Authority Grade Model:**
   - Room `hadiths` (DB v14) lacked `stable_id`, `hadith_references`, `hadith_translations`, `hadith_grades`, and `hadith_source_manifests`.
   - `HadithReferenceImporter.parseIndonesianBundle()` stored `chapters = emptyList()`, `grade = null`, and overwrote collections destructively via `deleteHadithsForCollection(collectionId)`.
4. **Canonical Source Available & Legally Permissible (`fawazahmed0/hadith-api`, rev `df57907be35291c91ad6a6691180e22ca9920784`, Unlicense):**
   - Contains complete Sunnah.com numbering, Arabic matn (`ara-*`), English (`eng-*`), Indonesian (`ind-*` where available), chapters (`info.json`/`metadata.sections`), and multi-authority scholarly grades across 7 collections (`bukhari`, `muslim`, `abudawud`, `tirmidhi`, `nasai`, `ibnmajah`, `malik`).
   - Verified presence of all 4 golden fixtures (`bukhari:3617`, `abudawud:4000`, `tirmidhi:3000`, `tirmidhi:813`).
   - `docs/HadistReference/reference2` (`hadith-json`, ISC) lacks Sunnah.com reference numbers (`idInBook` is sequential, e.g. Bukhari 3465, Abu Dawud 4001, Tirmidhi 3084) and fails license verification (`bundle_allowed = false`), so it is NOT bundled.
