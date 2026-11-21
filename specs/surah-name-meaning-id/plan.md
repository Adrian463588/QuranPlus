# Architecture & Implementation Plan — `surah-name-meaning-id`

1. **Shared Domain (`shared/src/commonMain/kotlin/com/quranplus/shared/features/quran/domain/Models.kt`):**
   - Add `val nameMeaningId: String = ""` to `data class Surah`.
2. **Data Single Source of Truth (`app/src/main/java/com/quranplus/app/core/utils/SurahMapper.kt`):**
   - Extend `SurahRef` with `val meaningId: String = ""` and populate all 114 verified Kemenag RI meanings.
   - Add `getMeaningId(surahNumber: Int): String`.
   - Extend `matchesSurah(...)` with `nameMeaningId: String = ""` (falling back to `getMeaningId(surahNumber)` if blank), matching clean lowercase/hyphen-insensitive Indonesian phrases accurately.
3. **Data Repository (`app/src/main/java/com/quranplus/app/features/quran/data/QuranRepositoryImpl.kt`):**
   - Extract DRY `private fun SurahEntity.toDomainSurah(): Surah` setting `nameMeaningId = SurahMapper.getMeaningId(number)`.
   - Reuse in `getAllSurahs()` and `getSurah(surahNumber)` — preserving Room `v16` and existing DAO/UseCase/ViewModel contracts unchanged.
4. **Presentation Layer (`SurahListScreen.kt`, `SearchScreen.kt`, `QuranReaderScreen.kt`):**
   - `SurahListScreen`: make `selectedSurahNumber` `rememberSaveable`; render `surah.nameMeaningId` in `SurahItemRow` (`bodySmall`, `maxLines = 2`); in `SurahDetailPane`, render `surah.nameMeaningId` below `nameLatin` and remove the redundant explanatory sentence.
   - `SearchScreen` & `QuranReaderScreen`: pass `nameMeaningId` into `Surah` fallback objects and `SurahMapper.matchesSurah(...)`, and display `surah.nameMeaningId` in `SearchScreen` Surah match card subtitle.
5. **Verification & Delivery:**
   - Update `scripts/validate_quran_corpus.py` to audit `SurahMapper.kt` 114/114 entries against `quranplus.db`.
   - Add JVM unit/repository/ViewModel tests in `SurahAndAyahSearchTest.kt` and Compose UI + Room instrumentation tests in `QuranDatabaseInstrumentedTest.kt`.
   - Run Python validators, Gradle unit tests, Android Lint, Debug & Release R8 builds, real-device `SM-T975` QA, secret scan, and Git commit/push + CI verification.
