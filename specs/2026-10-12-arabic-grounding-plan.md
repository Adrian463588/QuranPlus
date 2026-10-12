# Arabic reading and Quran/Hadith grounding repair

## Specification and plan

Use BDD Given/When/Then scenarios and the lightweight OpenSpec change workflow,
informed by BMAD's small-change build/review path. No runtime dependencies.
References:
- https://github.com/Fission-AI/OpenSpec/blob/main/docs/concepts.md
- https://github.com/bmad-code-org/BMAD-METHOD/blob/main/docs/build/build-a-change.md

User instruction overrides PRD F-10.4: fallback runs automatically without a
confirmation dialog; only Quran and Hadith texts can ground an answer. Keep
source citations and the existing opt-out for online requests.

## Acceptance criteria

1. Given a dzikir/wirid/hizib entry, when tajwid is toggled, then Arabic letters,
   harakat, punctuation and waqaf stay identical to the reviewed source text.
   Latin transliteration remains unchanged.
2. Given shaddah before or after a vowel mark, when formatting, then the same
   consonant cluster receives the tajwid style; no harakat are rewritten.
3. Given an end of verse, paragraph or explicit stop punctuation, when finding
   the next consonant, then connected-reading colors do not cross that stop.
4. Given inadequate local evidence, when online lookup is enabled, then the
   Quran/Hadith providers are tried automatically; editorial/wiki/user documents
   cannot become answer evidence.
5. Given no adequate primary evidence, offline lookup, timeout or provider
   failure, then answer abstains without invented religious claims.
6. Given generated text with missing/invalid citation markers, then replace it
   with the verified source excerpt fallback.
7. Given the release, then unit tests and Android lint pass, APK reports the new
   version, GitHub contains the scoped commit and downloadable release, and
   README preserves download links for historical versions.

## Tasks

- [x] Audit catalog Arabic against bundled Quran and identifiable source texts;
      record exact corrections and source links; keep transliteration unchanged.
- [x] Repair shared formatter and primary-only retrieval/prompt boundaries.
- [x] Add BDD regression tests covering source text and fallback branches.
- [x] Run unit tests, lint, assemble, diff/security review.
- [ ] Bump 1.0.8 / code 9; commit scoped files; push and publish APK/checksum.
- [ ] Verify remote commit, release asset and download history links.

## Evidence boundaries

Build/unit/lint do not prove real-device LLM quality or a scholarly review of
every devotional passage. Report only checks actually executed.

## Review findings

- `matsurat_02` and `bahr_05` now use exact bundled Arabic and tajwid text for
  Quran 2:1–5 and 36:1–9. `bahr_03`, `bahr_06`, `bahr_07` restore marked
  letter names from https://quran.nu.or.id/wirid/hizib/hizib-bahr .
- Nawawi's repeated opening is kept because it matches the selected Latin
  recension. No unsupported reconstruction from Latin is performed.
- Corpus validation freezes all 146 Latin entries and compares canonical
  passages directly with the bundled database, read-only.
- The old HTTP reader requested exactly 512 KiB + 1, causing EOF on smaller
  responses. A bounded buffer request now accepts short/chunked bodies and
  rejects oversized bodies. Nested Hadith markup keeps its canonical link.
- Local and online lookup have independent timeouts. Only traceable primary
  Quran/Hadith citations pass retrieval and prompt boundaries. Generated
  citations are checked before display processing strips their markers.
- Old assistant history and native conversation caches cannot reintroduce
  previous web/document answers. Invalid output is replaced with excerpts.
- A live Quran API request for `puasa` returned HTTP 200 and 52 matches.
  Direct Sunnah HTTP access encountered an anti-bot challenge; parser and
  unavailable-provider behavior are covered by controlled unit tests.
- CI repeats corpus validation, app/shared unit tests, lint and APK build.
  Publication outcomes belong to the GitHub release created after this commit.
- Local final gate: 185 app tests passed, 0 skipped/failed; shared module has no
  unit tests. Android lint: 0 errors, 98 warnings. APK: 1.0.8 / versionCode 9.
- Reachable text history scan found no credential-pattern matches; history
  contains no APK, AAR, keystore, private key, or notebook paths.
- Historical v1.0.4 is an empty draft; its broken README download link now
  states that no public APK exists, without inventing a historical binary.
