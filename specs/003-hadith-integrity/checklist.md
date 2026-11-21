# Checklist — Sprint 3 Hadith Translation Integrity (Nasai 5111 / 5022)

## Spec & Content Integrity
- [x] **CHK-01:** `sunnah:nasai:5111` (`archive 5022`) Arabic matn matches `sunnah.com/nasai:5111` (`قَالَ بَلَغَنَا` space preserved, `الْوَشْرِ وَالْوَشْمِ`).
- [x] **CHK-02:** `sunnah:nasai:5111` Indonesian translation conveys `"mengkikir gigi dan membuat tato."`, preserves verified sanad, and removes `"mencabut bulu alis"`.
- [x] **CHK-03:** Sibling `sunnah:nasai:5112` (`archive 5023`) matches `sunnah.com/nasai:5112` and receives the identical fix.
- [x] **CHK-04:** Scholarly grades (`Abu Ghuddah: Sahih`, `Al-Albani: Sahih`, `Zubair Ali Zai: Daif`) remain accurately attributed on `5111` & `5112`.
- [x] **CHK-05:** Valid related hadiths (`nasai:5109`, `bukhari:5931`, `ahmad:3759`, `4202`) preserve `"mencabut bulu alis"` where `الْمُتَنَمِّصَاتِ` appears.
- [x] **CHK-06:** Semantic corpus audit covers 65,716 bundled records and outputs `audit/semantic-audit-report.json`.

## Single Source of Truth, Database, Search & AI/RAG
- [x] **CHK-07:** Single `app/src/main/assets/hadith/content-corrections.json` governs both Python corpus build and Kotlin `HadithContentCorrector`.
- [x] **CHK-08:** Correction pipeline is idempotent and rejects unexpected upstream source SHA-256 digests (`AC-10`, `AC-11`).
- [x] **CHK-09:** Room upgrade re-syncs corrected `nasai:5111` / `5112` + `hadiths_fts5`, preserves all bookmarks/last-read/chat history, and invalidates stale RAG vector index metadata/hadith chunks.
- [x] **CHK-10:** Exact numeric lookup (`5111` Sunnah.com, `5022` Arsip), FTS5 search (`"membuat tato"`), UI rendering, share/copy, and AI/RAG grounding return corrected text offline.

## DevSecOps R1–R8 & Delivery
- [x] **CHK-11:** Regressions `HF-001`..`HF-006` pass in Python validators, JVM unit tests (`236`), and `SM-T975` instrumentation tests (`33`).
- [x] **CHK-12:** Android Lint (`0` errors), `:app:minifyReleaseWithR8` + `:app:assembleRelease`, secret scan, scoped commit, push to `origin/main`, and GitHub Actions `regression.yml` verified.
- [x] **CHK-13:** `BUG-S3-HADITH-AHMAD-4302` verified across editions (`ar_risalah:4302` -> `mit:ahmad:4075` Ibn Mas'ud; `archive_gading:4302` -> `mit:ahmad:4302` / `ar_risalah:4531` Ibn Umar `لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ`; zero `4526` contamination on `4302`).
- [x] **CHK-14:** Monotonic `ar_risalah` crosswalk repair removes sanad-only false matches (`4526->4297`, `4528->4299`, `4531->4302`, `4532->4304`) and upgrades bundle deterministically (`+mit9+corr2`).
- [x] **CHK-15:** Production `HadithRepositoryImpl.searchDetailed` exhaustively verified across all `26,363` `archive_gading` records, all mapped `ar_risalah` records, all mandatory checkpoints, unmapped `ar_risalah:4000`, invalid refs, and cross-collection `4302` isolation.
- [x] **CHK-16:** Natural queries (`"4302"`, `"No.4302"`, `"ahmad:4302"`, `"ahmad arsip 4302"`, `"Hadis Musnad Ahmad nomor 4302"`) resolve accurately in UI and AI/RAG.
- [x] **CHK-17:** Python validators, JVM unit tests (`239/239`), Android Lint (`0` errors), R8 Release build (`205,995,452` bytes), physical device SQLite/Compose QA (`SM-T975`), secret scan, and delivery artifacts verified.
- [x] **CHK-18:** `BUG-S3-HADITH-DARIMI-2841` source audit verified (`gadingnst` 2,949 omission vs MIT `3,367` `archive_gading`), Candidate A (`mit:darimi:2841` Ali Mauquf) and Candidate B (`mit:darimi:2805` grandmother 1/6) verified, zero fabricated `sunnah.com/darimi` URLs or unverified `3023/3027/2975` links.
- [x] **CHK-19:** AI/RAG (`HadithReferenceRequest` + `ChatRepositoryImpl` + `RagPipeline`) retrieves `"Hadis Sunan al-Darimi nomor 2841"` cleanly via `(Al-Alamiyah)` local bundle without prophetic misattribution.
- [x] **CHK-20:** All-9-collection production `HadithRepositoryImpl.searchDetailed` audit covers all `65,716` active records (`120,616` supported references across `HF-001..HF-008`, checkpoints `1..3367` Darimi, `26,363` Ahmad, `7` Kutub collections + `2,622` unmapped slots) with 100% deterministic retrieval.
- [x] **CHK-21:** Python validators, JVM tests (`242/242`), Lint (`0` errors), R8 Release APK (`205,995,452` bytes), `SM-T975` (`35/35`) offline/landscape/portrait scroll-preserved/restart verification, secret scan, and Spec Kit artifacts verified.
