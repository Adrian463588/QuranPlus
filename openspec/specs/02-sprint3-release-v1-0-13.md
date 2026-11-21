# OpenSpec SDD: 02-sprint3-release-v1-0-13

## Metadata
- **Specification ID**: REL-002
- **Target Release**: `v1.0.13` (`versionCode = 14`, `versionName = "1.0.13"`)
- **Methodology**: BMAD + OpenSpec Spec-Driven Development (SDD) + GitHub Spec Kit (`specs/004-release-v1-0-13/`)
- **Discipline**: Ponytail (minimal-diff), RTK AI (token efficiency), Caveman, YAGNI, DRY, SOLID

---

## 1. BMAD Role Architecture

- **Architect**: Defines release boundary (`versionCode = 14`, `versionName = "1.0.13"`), artifact compatibility (`app-debug.apk` debug-key upgrade continuity + `:app:assembleRelease` R8 gate), and documentation SSOT constraints.
- **Analyst**: Audits current empirical state from `app/build.gradle.kts`, `app/src/main/assets/hadith/corpus-manifest.json`, GitHub Releases (`v1.0.12`), and connected ADB device (`SM-T975` / `RR2N800H9DR`).
- **Developer**: Applies minimal diffs strictly to spec files, `app/build.gradle.kts`, and `README.md` with zero speculative production code edits.
- **QA / Safety Officer**: Executes Python corpus validators, JVM tests, Android Lint, R8 + Debug builds, non-destructive `adb install -r -d`, post-install data preservation check, GitHub Actions CI gate, and GitHub Release publication.

---

## 2. Context & Non-Negotiable Invariants

- **INV-01 (Non-Destructive Upgrade & User Data Preservation)**: `adb install -r -d` MUST install `versionCode = 14` (`v1.0.13`) over existing `versionCode = 13` (`v1.0.12`, `firstInstallTime=2026-09-19 23:08:19`) without clearing `/data/user/0/com.quranplus.app`, Room tables, bookmarks, reading markers, or SAF configuration.
- **INV-02 (Signing Continuity)**: Device APK installation and GitHub Release downloadable APK MUST match the installed `Signing KeySets` (`app-debug.apk` debug key, consistent with `v1.0.0..v1.0.12`) so `-r -d` upgrades succeed without signature mismatch or forced uninstall.
- **INV-03 (R8 DevSecOps Verification)**: `:app:assembleRelease` (`isMinifyEnabled = true`, `isShrinkResources = true`) MUST pass cleanly after the version bump even though user-facing upgrades use `app-debug.apk`.
- **INV-04 (Empirical Documentation Accuracy)**: Every statistic, fix summary, source attribution, and release download URL in `README.md` MUST match verified repository assets (`65,716` records, `120,616` references, `Unlicense` + `MIT`, `v1.0.13`).
- **INV-05 (Green CI Before Tag/Release)**: Annotated tag `v1.0.13` and GitHub Release `v1.0.13` MUST only be created from a pushed `main` commit whose GitHub Actions `regression.yml` workflow has finished with `conclusion = success`.

---

## 3. BDD Acceptance Scenarios

### Scenario 1: Version Bump & R8 + Debug Parity
- **GIVEN** `app/build.gradle.kts` is at `versionCode = 13`, `versionName = "1.0.12"`
- **WHEN** `versionCode` is updated to `14` and `versionName` to `"1.0.13"`
- **THEN** `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` all succeed with `0` failures and `0` Lint errors.

### Scenario 2: Empirical README & Release History Alignment
- **GIVEN** `README.md` still references `v1.0.12` as latest and legacy `38.102` / `gadingnst`-only corpus stats
- **WHEN** `README.md` is updated for `v1.0.13`
- **THEN** badges, direct install link, Hadith feature metrics (`65.716 Hadist`, `120.616 rujukan`), source attributions (`fawazahmed0/hadith-api` Unlicense + `irsyadulibad/hadits-database` MIT), and the `Riwayat Rilis` table reflect `v1.0.13` accurately while archiving `v1.0.12`.

### Scenario 3: Non-Destructive ADB Upgrade (`-r -d`)
- **GIVEN** connected device `RR2N800H9DR` (`SM-T975`) has `com.quranplus.app` `versionCode=13` (`firstInstallTime=2026-09-19 23:08:19`) with existing user markers
- **WHEN** `adb -s RR2N800H9DR install -r -d app/build/outputs/apk/debug/app-debug.apk` executes
- **THEN** installation exits with `Success`, `dumpsys package` shows `versionCode=14`, `versionName=1.0.13`, unchanged `firstInstallTime=2026-09-19 23:08:19`, and cold app launch preserves existing user markers without crashes.

### Scenario 4: Scoped Git Push, CI Gate & Live GitHub Release
- **GIVEN** all local gates and ADB upgrade checks have passed
- **WHEN** scoped changes are committed cleanly, pushed to `origin/main`, verified green in GitHub Actions, tagged `v1.0.13`, and published via `gh release create v1.0.13` with `app-debug.apk`
- **THEN** `origin/main` matches local `HEAD`, `regression.yml` is `success`, and `https://github.com/Adrian463588/QuranPlus/releases/download/v1.0.13/app-debug.apk` resolves to the published asset.

---

## 4. Verification Gates

1. **Gate 1 (Specs)**: `openspec/specs/02-sprint3-release-v1-0-13.md` + `specs/004-release-v1-0-13/` authored before implementation edits.
2. **Gate 2 (Validators & Gradle)**: Python corpus validators + `242/242` JVM tests + `:app:lintDebug` (`0` errors) + `:app:assembleDebug` + `:app:assembleRelease`.
3. **Gate 3 (ADB Preserve Upgrade)**: `adb -s RR2N800H9DR install -r -d` succeeds, `versionCode=14` / `versionName=1.0.13`, `firstInstallTime` unchanged, cold launch + pre-existing data intact.
4. **Gate 4 (Git, CI & Release)**: Scoped commit on `main`, GitHub Actions `regression.yml` `success`, tag & Release `v1.0.13` published with `app-debug.apk`.
