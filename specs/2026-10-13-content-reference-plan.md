# Content and reference correction

## Intent

Arabic reading, hadith content and numbering, and primary-only chatbot fallback must agree with their cited sources. Preserve the existing Latin transliterations and user data. Keep the implementation small; do not add process tools as runtime dependencies.

## Approach

Use a lightweight spec-driven workflow: investigate, record observable GIVEN/WHEN/THEN contracts, implement the root correction, review, validate, and publish. References: [OpenSpec writing specs](https://github.com/Fission-AI/OpenSpec/blob/main/docs/writing-specs.md) and [BMAD build a change](https://github.com/bmad-code-org/BMAD-METHOD/blob/main/docs/build/build-a-change.md). This follows their workflow principles without claiming either CLI is installed.

## Acceptance scenarios

1. GIVEN Abu Dawud 3837 in the Sunnah.com numbering scheme WHEN a user requests that reference THEN its evidence describes dates and butter, and a different edition's number never silently substitutes different content.
2. GIVEN an imported hadith collection WHEN reading, copying, bookmarking, searching, or grounding an answer THEN its displayed reference identifies the actual numbering scheme/source; an external reference must retain its own canonical URL.
3. GIVEN missing or mismatched local evidence WHEN online retrieval is enabled THEN primary Quran/Hadith retrieval runs automatically without a confirmation dialog; unrelated local evidence cannot satisfy a specific-reference query.
4. GIVEN failed online retrieval or no adequate primary evidence WHEN answering THEN the response honestly states insufficient evidence and generates no unsourced religious answer.
5. GIVEN all hizib, wirid, and doa items WHEN rendering with tajwid enabled or disabled THEN the Arabic text is preserved, annotations align with the corrected text, and Latin remains byte-for-byte unchanged.
6. GIVEN source-backed Arabic corrections WHEN checking the corpus THEN the exact Quran excerpts, marked letter names, and applicable stopping boundaries pass automated regression checks.
7. GIVEN an application upgrade WHEN launching on the connected ADB device THEN corrected data/references are used while existing bookmarks, markers, SAF data, and conversations remain available.
8. GIVEN passing software and available device gates WHEN publishing THEN GitHub contains the scoped commit, a new immutable version tag, downloadable APK/checksum, and release history with verified links.

## Tasks

- [x] Audit current data, numbering, source integrity, Arabic annotations, and fallback paths.
- [x] Implement source-backed corrections and regression scenarios.
- [x] Run corpus validation, complete unit suites, Android lint, and APK build.
- [x] Perform connected-device QA and record the acceptance scope.
- [x] Review the scoped diff and publication safety.
- Delivery sequence: commit, push, verify CI, publish the APK/checksum, and verify latest/historical links. GitHub tag, Actions run, and [v1.0.11 release metadata](https://github.com/Adrian463588/QuranPlus/releases/tag/v1.0.11) are the delivery receipts; this pre-publication plan does not claim a future push or upload has completed.

## Evidence

Starting point: main at 3ea0e0d, app 1.0.10. The bundled seed has seven hadith rows; the nine-book reader corpus comes from a pinned gadingnst/hadith-api archive. Sunnah.com Abu Dawud 3837 describes dates and butter: https://sunnah.com/abudawud:3837. Device was absent from both ADB and mobile MCP at intake; reconnect requested while independent work continues.

Completed software acceptance: 212 unit tests, zero failures/errors/skips; shared test task has no test sources. Android lint: zero errors, 100 warnings, one informational finding. Scoped Ruff and both corpus validators pass. All 146 Latin fields retain the reviewed digest. Eighteen Quran passages match the bundled Mushaf exactly; seven corrupt annotation strings were replaced with their display Arabic. Unsupported ghunnah/qalqalah tags no longer color unrelated consonants.

Numbering crosswalk: 28,347 unique content matches across six collections, with pinned input revisions/digests and a per-row Arabic/translation digest. Ambiguous/unmapped records retain explicit archive-edition labels. Muslim variants retain the Arabic base number plus in-book reference; no unverified variant URL is constructed. Archive identities are unchanged, including bookmark/marker navigation. Extracted SAF books must match the original pinned book digest before receiving verified provenance.

Device acceptance: SM-T975, upgrade 1.0.7 -> 1.0.11, versionCode 12. Four instrumented tests passed against installed corpus and the real primary Quran provider. Manual reader/chat request Abu Dawud 3837 shows dates and butter; its citation opens that same row. Missing reference 999999 produces insufficient-evidence text without confirmation. Hizib Bahr letter names and Al-Ma'tsurat Quran typography/waqaf were inspected. All seven original bookmark rows and all 21 original message rows are byte-for-byte preserved; two QA conversations add four messages. Installed hadith count remains 38,102. This does not certify every unreviewed devotional recension or unrestricted model-answer entailment.

Publication review: credential pattern scan clean across 67 reachable commits and scoped changed/new sources; the user's untracked dump_screen.xml is excluded. APK is a debug build; minimum Android API 32. Artifact SHA-256: 0c7bdcd8b67c46699d30d034a299e37038d1485ffca968871b65d62fa014745d.

Final edition guards: bookmark/marker labels identify archive ordinals. Reader queries `arsip 3837` and chat queries `Abu Dawud arsip 3837` preserve that edition; unsupported archive/invalid references do not masquerade as Sunnah.com numbers. Same-ordinal local and online evidence use separate source identities, so weak local evidence cannot hide the online fallback. Official `Sunan Abi Dawud` naming is recognized. The final four device tests passed and preservation counts remained unchanged.
