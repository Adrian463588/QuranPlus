# Plan — 004 Release v1.0.13

1. **BMAD Phase 1 (Analysis — Complete)**: Confirmed `app/build.gradle.kts` (`13` / `1.0.12`), `corpus-manifest.json` (`65,716` hadiths / `120,616` references, `Unlicense+MIT`), GitHub Release `v1.0.12` (`app-debug.apk`), and `SM-T975` (`RR2N800H9DR`) installed debug package state (`versionCode=13`, `firstInstallTime=2026-09-19 23:08:19`).
2. **BMAD Phase 2 (Spec Kit + OpenSpec)**: Author `openspec/specs/02-sprint3-release-v1-0-13.md` and `specs/004-release-v1-0-13/{spec,plan,tasks,checklist}.md` before code/doc edits.
3. **BMAD Phase 3–4 (Minimal Diff Execution)**:
   - Update `versionCode` / `versionName` in `app/build.gradle.kts`.
   - Update `README.md` minimally using exact verified facts.
4. **BMAD Phase 5 (Verification, ADB `-r -d`, Git Push, CI & Release)**:
   - Execute Python validators + `:app:testDebugUnitTest` + `:shared:testDebugUnitTest` + `:app:lintDebug` + `:app:assembleDebug` + `:app:assembleRelease`.
   - Install via `adb -s RR2N800H9DR install -r -d app/build/outputs/apk/debug/app-debug.apk` and verify package state, data preservation, and launch UI.
   - Commit scoped files, push `origin/main`, confirm green CI, then create & push tag `v1.0.13` + `gh release create v1.0.13`.
