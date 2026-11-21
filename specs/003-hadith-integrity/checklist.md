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
