# Tasks — 004 Release v1.0.13

- [x] **T01 (BMAD Phase 1–2):** Complete environment audit and author `openspec/specs/02-sprint3-release-v1-0-13.md` + `specs/004-release-v1-0-13/*`.
- [x] **T02 (Version Bump):** Update `app/build.gradle.kts` to `versionCode = 14`, `versionName = "1.0.13"`.
- [x] **T03 (README Update):** Update `README.md` badges, install link, Hadith corpus stats & sources, and `v1.0.13` changelog row.
- [x] **T04 (Build & QA Gates):** Run Python corpus validators, Gradle JVM tests (`:app:testDebugUnitTest :shared:testDebugUnitTest`), `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`.
- [x] **T05 (Non-Destructive ADB Upgrade):** Execute `adb -s RR2N800H9DR install -r -d` with `app-debug.apk`, verify `versionCode=14` / `firstInstallTime` preservation / clean launch.
- [ ] **T06 (Git, CI & Release Publish):** Commit scoped files cleanly, push `origin/main`, verify GitHub Actions `regression.yml` green, push tag `v1.0.13`, and publish GitHub Release `v1.0.13` with `app-debug.apk`.
