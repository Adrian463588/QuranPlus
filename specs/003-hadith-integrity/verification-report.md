# Sprint 3 & Addendum Verification Report — Hadith Database Integrity

## 1. Corpus Build & Automated Validation (All 9 Collections)

- **Bundle Asset:** `app/src/main/assets/hadith/sunnah-canonical-bundle.zip` (`34,737,340` bytes)
- **Bundle SHA-256:** `4b49f7b773f097363fa096cc85cdc87ec6d78a3526466986e052882103344a7b`
- **Source Revision:** `df57907be35291c91ad6a6691180e22ca9920784+mit9+corr2` (`Unlicense+MIT`)
- **Authoritative Correction Registry:** `app/src/main/assets/hadith/content-corrections.json` (`CORR-ABUDAWUD-3837-01`, `CORR-NASAI-5111-01`, `CORR-NASAI-5112-01`)
- **Active Verified Records:** `65,716` across 9 collections (`0` integrity issues, `2,825` documented edition/source slots separated cleanly from active rows)
- **Machine & Human Validation Reports:**
  - `specs/003-hadith-integrity/audit/corpus-validation-report.json`
  - `specs/003-hadith-integrity/audit/corpus-validation-summary.md`
  - `specs/003-hadith-integrity/audit/semantic-audit-report.json` (`103` adornment/tattoo/teeth-filing/eyebrow candidates inspected: `90` `VERIFIED_CORRECT`, `0` uncorrected `VERIFIED_INCORRECT`, `10` `NEEDS_REVIEW`, `3` `SOURCE_UNAVAILABLE`)

### Validator Commands Executed & Observed Outputs
1. `python scripts/validate_dzikir_corpus.py` -> `PASS: 146 entries; 18 exact Quran passages; Latin unchanged`
2. `python scripts/validate_hadith_numbering.py` -> `PASS: 29888 content-matched references; Abu Dawud 3340 -> 3837; ambiguous matches excluded`
3. `python scripts/validate_hadith_corpus.py` -> `PASS: 65716 records verified across 9 collections; HF-001..HF-008 passed`

## 2. Golden Regression Fixtures (HF-001 .. HF-008)

| Fixture | Collection & Scheme | Reference | Verified Identity & Content | Result |
|---|---|---|---|---|
| `HF-001` | Sahih al-Bukhari (`sunnah`) | `3617` (`https://sunnah.com/bukhari:3617`) | Christian convert narration (`نَصْرَانِيًّا`, `لَفَظَتْهُ الأَرْضُ` / `Christian`) | PASS |
| `HF-002` | Sunan Abi Dawud (`sunnah`) | `4000` (`https://sunnah.com/abudawud:4000`) | Preserves qira'at `مَالِكِ يَوْمِ الدِّينِ` vs `مَلِكِ يَوْمِ الدِّينِ`, `Al-Albani: Daif Isnaad` | PASS |
| `HF-003` | Jami' at-Tirmidhi (`sunnah`) | `3000` (`https://sunnah.com/tirmidhi:3000`) | Abu Umamah & Khawarij (`أَبُو أُمَامَةَ`, `كِلاَبُ النَّارِ`, no Hajj confusion) | PASS |
| `HF-004` | Jami' at-Tirmidhi (`sunnah`) | `813` (`https://sunnah.com/tirmidhi:813`) | Hajj provisions & transportation (`الزَّادُ وَالرَّاحِلَةُ`), distinct `stable_id` from `3000` | PASS |
| `HF-005a` | Musnad Ahmad (`ar_risalah`, default) | `4001` (`https://hadits.net/ahmad:4001`) | Abdullah bin Mas'ud Li'an narration (`عَشِيَّةَ الْجُمُعَةِ`, `آيَةُ اللِّعَانِ`), aligns with MIT `#3800`, rejects `kupu-kupu` mismatch | PASS |
| `HF-005b` | Musnad Ahmad (`archive_gading` / Al-Alamiyah) | `4001` | Abdullah bin Mas'ud reliance narration (`مَنْ نَزَلَتْ بِهِ فَاقَةٌ`), distinct `stable_id` from `ar_risalah:4001` | PASS |
| `HF-006` | Sunan an-Nasa'i (`sunnah` & `archive_gading`) | `5111` / `5022` & sibling `5112` / `5023` (`https://sunnah.com/nasai:5111`) | Arabic `قَالَ بَلَغَنَا` space preserved, `الْوَشْرِ وَالْوَشْمِ` → `mengkikir gigi dan membuat tato.`, `Al-Albani: Sahih` vs `Zubair Ali Zai: Daif` preserved, `nasai:5109` & `bukhari:5931` preserved | PASS |
| `HF-007` | Musnad Ahmad (`ar_risalah` & `archive_gading`) | `4302` (`mit:ahmad:4075` Ar-Risalah vs `mit:ahmad:4302` Al-Alamiyah / `ar_risalah:4531`) | Ar-Risalah `4302` = `نَسْتَخْصِي` (Ibn Mas'ud); Al-Alamiyah `4302` = `لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ` (`Janganlah sebagian kalian menjual...`); zero spurious `4526` (`الشغار` restored to `4297`), `26,363/26,363` `archive_gading` & all mapped `ar_risalah` records search-verified via production `HadithRepositoryImpl` | PASS |
| `HF-008` | Sunan al-Darimi (`archive_gading`) | `2841` (`mit:darimi:2841`, `3,367` total verified MIT records `1..3,367`) | Matches Candidate A verbatim (Atsar Mauquf of `[Ali] radlyallahu 'anhu` on *li'an* child inheritance), zero `Rasulullah` misattribution, `canonical_url = null` (zero fabricated `sunnah.com/darimi:*` URL), distinct `#2805`/`#2975`/`#3023`/`#3027`, plus all `65,716` active records (`120,616` supported refs + `2,622` unmapped slots across all 9 collections) verified via `HadithRepositoryImpl` | PASS |

## 3. Gradle Build, Unit Test, Lint & R8 Verification

Executed:
`.\gradlew.bat :app:testDebugUnitTest :shared:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease --no-daemon --console=plain`

- **Result:** `BUILD SUCCESSFUL` (`0` test failures, `0` Lint errors)
- **JVM Unit Tests (`:app:testDebugUnitTest`):** `242` tests across 38 classes (`fail=0`, `err=0`, `skip=0`)
  - Includes `CanonicalHadithRegressionTest` (14/14, including `HF-006` Nasai `5111/5022`, `HF-007` Ahmad `4302`, `HF-008` Darimi `2841` + AI/RAG Mauquf attribution, and exhaustive **all-9-collection** production `HadithRepositoryImpl.searchDetailed` audit over `65,716` active records / `120,616` supported references / `2,622` unmapped slots), `HadithNumberingTest` (8/8), `HadithReferenceRequestTest` (7/7), `AppSecurityManagerTest` (10/10), `SecurityKeyStoreProviderTest` (4/4), `RagPipelineTest` (5/5), `ChatGroundingFallbackTest` (20/20).
- **KMP Shared Tests (`:shared:testDebugUnitTest`):** Executed cleanly (`NO-SOURCE`).
- **Android Lint (`:app:lintDebug` & `:app:lintVitalRelease`):** `0` errors (`107` pre-existing non-blocking warnings).
- **Release R8 (`:app:minifyReleaseWithR8` & `:app:assembleRelease`):** Passed (`app-release-unsigned.apk`, `205,995,452` bytes).
- **Debug APK:** `app-debug.apk` (`315,294,315` bytes) built cleanly.

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

## 5. Physical Device Verification (`SM-T975` Tablet / `RR2N800H9DR`)

- **Upgrade Provenance:** Upgraded in-place from prior production installation `versionCode=12` (`versionName=1.0.11`, installed `2026-09-19`) with `adb -s RR2N800H9DR install -r -t`; pre-existing marker (`MARKER #1 QS. Al-Hadid (Ayat 7)`) survived upgrade intact.
- **Instrumentation Command (`+mit9+corr2`):**
  1. Full offline sweep (`svc wifi disable; svc data disable; am instrument -w`): **`33/33` offline tests passed** on `+mit9+corr2` (only `PrimaryFallbackDeviceTest#GIVEN_validatedInternet...` failed due to intentional network shutoff).
  2. Full post-edit `SM-T975` run (`adb -s RR2N800H9DR shell am instrument -w -r com.quranplus.app.test/androidx.test.runner.AndroidJUnitRunner`): **`OK (35 tests)` in `176.729s` (`0` failures, `0` skipped)** — includes `HadithMigrationAndIntegrityInstrumentedTest` (4/4 — adding `GIVEN_darimi2841AndAllCheckpointsOnRoomSql_WHEN_searchedAndRendered_THEN_passesOfflineAndAiGrounding` checking `3,367` rows + 20 checkpoints `1..3367` + AI RAG `[Ali] radlyallahu 'anhu` + Compose `darimi:2841` & empty `999999`), `HadithNumberingDeviceTest` (2/2), `PrimaryFallbackDeviceTest` (1/1), `MainNavigationSmokeTest` (9/9), and `QuranDatabaseInstrumentedTest` (5/5).
- **Offline Visual Inspection (`SM-T975` with Wi-Fi & mobile data disabled):**
  - Searching `5111` under `No. Sunnah.com` renders `HR. Sunan al-Nasa'i · No. 5111 (Sunnah.com)`, `قَالَ بَلَغَنَا`, `"melarang mengkikir gigi dan membuat tato."`, `Derajat: Sahih (Abu Ghuddah) · Sahih (Al-Albani) · Daif (Zubair Ali Zai)`, and twin `5112` below.
  - Switching to `No. Arsip` and searching `5022` renders the exact same narration as `No. 5022 (Arsip)` (`5679 hadist · No. 1–5662`) with link parity `Sunnah.com no. 5111`.
  - Searching `4302` in `Musnad Ahmad` (`26363 hadist · No. 1–27647 · مسند أحمد`) under `No. Ar-Risalah` renders `HR. Musnad Ahmad · No. 4302 (Ar-Risalah)` (`حَدَّثَنَا يَحْيَى بْنُ زَكَرِيَّا...`), and switching to `No. Arsip` renders `HR. Musnad Ahmad · No. 4302 (Al-Alamiyah)` (`لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ` / `"Janganlah sebagian kalian menjual di atas penjualan sebagian lain."`, `Rujukan: Musnad Ahmad No. 4302 (Al-Alamiyah) • Ar-Risalah no. 4531`), followed immediately by `No. 4303 (Al-Alamiyah)`.
  - Opening `Sunan al-Darimi` (`3367 hadist · سنن الدارمي`) offline and searching `2841` renders `HR. Sunan al-Darimi · No. 2841 (Al-Alamiyah)` (`[Ali] radlyallahu 'anhu` / `لِأُمِّهِ وَجَعَلَهَا عَصَبَتَهُ`, `Rujukan: Sunan al-Darimi No. 2841 (Al-Alamiyah)`); rotating landscape <-> portrait preserves card `No. 2841` pinned at top; offline cold restart preserves app stability and pre-existing `MARKER #1 QS. Al-Hadid (Ayat 7)`.
