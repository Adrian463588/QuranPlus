# Sprint 3 & Addendum Verification Report — Hadith Database Integrity

## 1. Corpus Build & Automated Validation (All 9 Collections)

- **Bundle Asset:** `app/src/main/assets/hadith/sunnah-canonical-bundle.zip` (`34,735,973` bytes)
- **Bundle SHA-256:** `b3ab43be9b3b3a050deb6349d8d40682a831f5af348899cbe51f17ba63427d38`
- **Source Revision:** `df57907be35291c91ad6a6691180e22ca9920784+mit9` (`Unlicense+MIT`)
- **Active Verified Records:** `65,716` across 9 collections (`0` integrity issues, `3,004` documented edition/source slots separated cleanly from active rows)
- **Machine & Human Validation Reports:**
  - `specs/003-hadith-integrity/audit/corpus-validation-report.json`
  - `specs/003-hadith-integrity/audit/corpus-validation-summary.md`

### Validator Commands Executed & Observed Outputs
1. `python scripts/validate_dzikir_corpus.py` -> `PASS: 146 entries; 18 exact Quran passages; Latin unchanged`
2. `python scripts/validate_hadith_numbering.py` -> `PASS: 29888 content-matched references; Abu Dawud 3340 -> 3837; ambiguous matches excluded`
3. `python scripts/validate_hadith_corpus.py` -> `PASS: 65716 records verified across 9 collections; HF-001..HF-005 passed`

## 2. Golden Regression Fixtures (HF-001 .. HF-005)

| Fixture | Collection & Scheme | Reference | Verified Identity & Content | Result |
|---|---|---|---|---|
| `HF-001` | Sahih al-Bukhari (`sunnah`) | `3617` (`https://sunnah.com/bukhari:3617`) | Christian convert narration (`نَصْرَانِيًّا`, `لَفَظَتْهُ الأَرْضُ` / `Christian`) | PASS |
| `HF-002` | Sunan Abi Dawud (`sunnah`) | `4000` (`https://sunnah.com/abudawud:4000`) | Preserves qira'at `مَالِكِ يَوْمِ الدِّينِ` vs `مَلِكِ يَوْمِ الدِّينِ`, `Al-Albani: Daif Isnaad` | PASS |
| `HF-003` | Jami' at-Tirmidhi (`sunnah`) | `3000` (`https://sunnah.com/tirmidhi:3000`) | Abu Umamah & Khawarij (`أَبُو أُمَامَةَ`, `كِلاَبُ النَّارِ`, no Hajj confusion) | PASS |
| `HF-004` | Jami' at-Tirmidhi (`sunnah`) | `813` (`https://sunnah.com/tirmidhi:813`) | Hajj provisions & transportation (`الزَّادُ وَالرَّاحِلَةُ`), distinct `stable_id` from `3000` | PASS |
| `HF-005a` | Musnad Ahmad (`ar_risalah`, default) | `4001` (`https://hadits.net/ahmad:4001`) | Abdullah bin Mas'ud Li'an narration (`عَشِيَّةَ الْجُمُعَةِ`, `آيَةُ اللِّعَانِ`), aligns with MIT `#3800`, rejects `kupu-kupu` mismatch | PASS |
| `HF-005b` | Musnad Ahmad (`archive_gading` / Al-Alamiyah) | `4001` | Abdullah bin Mas'ud reliance narration (`مَنْ نَزَلَتْ بِهِ فَاقَةٌ`), distinct `stable_id` from `ar_risalah:4001` | PASS |

## 3. Gradle Build, Unit Test, Lint & R8 Verification

Executed:
`.\gradlew.bat :app:testDebugUnitTest :shared:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease --no-daemon --console=plain`

- **Result:** `BUILD SUCCESSFUL in 4m 41s`
- **JVM Unit Tests (`:app:testDebugUnitTest`):** `234` tests across 38 classes (`fail=0`, `err=0`, `skip=0`)
  - Includes `CanonicalHadithRegressionTest` (8/8), `HadithNumberingTest` (8/8), `HadithReferenceRequestTest` (5/5), `AppSecurityManagerTest` (10/10), `SecurityKeyStoreProviderTest` (4/4), `RagPipelineTest` (5/5), `ChatGroundingFallbackTest` (20/20).
- **KMP Shared Tests (`:shared:testDebugUnitTest`):** Executed cleanly (`NO-SOURCE`).
- **Android Lint (`:app:lintDebug` & `:app:lintVitalRelease`):** `0` errors (`107` pre-existing non-blocking warnings).
- **Release R8 (`:app:minifyReleaseWithR8` & `:app:assembleRelease`):** Passed (`app-release-unsigned.apk`, `205,976,088` bytes).
- **Debug & AndroidTest APKs:** `app-debug.apk` (`280,552,273` bytes) & `app-debug-androidTest.apk` built cleanly.

## 4. Physical Device Verification (`SM-G988B` / `RRCN3008VYE`)

- **Instrumentation Command:** `adb -s RRCN3008VYE shell am instrument -w com.quranplus.app.test/androidx.test.runner.AndroidJUnitRunner`
- **Outcome:** `OK (33 tests)` in `184.927s` (`0` failures, `0` skipped)
  - `AdaptiveNavigationSemanticsTest` (3)
  - `CitationChipNavigationTest` (4)
  - `CitationJsonCodecInstrumentedTest` (1)
  - `HadithMigrationAndIntegrityInstrumentedTest` (2 — real SQLite `v1 -> v14 -> v16` migration with user bookmarks/last_read/chat/custom row survival + streaming 65,716-row sync + `HadithScreen` Compose UI & `>=48dp` touch target verification for `ahmad:4001` and `abudawud:4000`)
  - `HadithNumberingDeviceTest` (2 — `abudawud` `3837` vs `arsip 3837 -> 4420` + `HF-001..HF-005` on live DB)
  - `InternetResearcherTest` (5), `MainActivitySmokeTest` (1), `MainNavigationSmokeTest` (9), `PrimaryFallbackDeviceTest` (1), `QuranDatabaseInstrumentedTest` (5)
- **Offline Visual Inspection (`SM-G988B` with Wi-Fi & mobile data disabled):**
  - Catalog confirms `Musnad Ahmad` displays count vs reference span cleanly: `26363 hadist · No. 1–27647`
  - Searching `4001` under `No. Ar-Risalah` resolves offline to `HR. Musnad Ahmad · No. 4001 (Ar-Risalah)` with authentic Li'an matn and Indonesian translation (`Musnad Abdullah bin Mas'ud`).
  - Switching to `No. Arsip` resolves offline to `HR. Musnad Ahmad · No. 4001 (Al-Alamiyah)` with authentic `مَنْ نَزَلَتْ بِهِ فَاقَةٌ` matn without cross-edition substitution.
