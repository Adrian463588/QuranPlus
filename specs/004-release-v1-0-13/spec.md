# Specification — 004 Release v1.0.13 & Non-Destructive ADB Upgrade

- **Feature ID**: `004-release-v1-0-13`
- **Companion OpenSpec / BMAD**: `openspec/specs/02-sprint3-release-v1-0-13.md`
- **Previous Verified Implementation Spec**: `specs/003-hadith-integrity/`

## Requirements

- **REQ-01 (Version Increment)**: Bump `app/build.gradle.kts` from `versionCode = 13` / `versionName = "1.0.12"` to `versionCode = 14` / `versionName = "1.0.13"`.
- **REQ-02 (README Synchronization)**: Update `README.md` top download badge, direct download link, Ensiklopedia 9 Kitab Hadist stats (`65.716` active hadis, `120.616` mapped references), source attributions (`fawazahmed0/hadith-api` Unlicense + `irsyadulibad/hadits-database` MIT), and release history table (`v1.0.13` as `Terbaru (Latest)`, `v1.0.12` archived).
- **REQ-03 (Non-Destructive ADB Deployment)**: Install updated `app-debug.apk` on connected ADB device (`SM-T975` / `RR2N800H9DR`) using `adb install -r -d` without uninstalling or clearing user data, and verify `versionCode=14`, `versionName=1.0.13`, preserved `firstInstallTime=2026-09-19 23:08:19`, and clean app startup.
- **REQ-04 (GitHub Push, CI & Release Publication)**: Commit only scoped release/spec files cleanly to `main`, push to `origin/main`, wait for green GitHub Actions `regression.yml` CI, create annotated tag `v1.0.13`, and publish GitHub Release `v1.0.13` attaching `app-debug.apk` so `README.md` download links resolve live.
