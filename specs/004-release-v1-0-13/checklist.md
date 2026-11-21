# Checklist — 004 Release v1.0.13

- [x] **CHK-REL-01:** OpenSpec (`openspec/specs/02-sprint3-release-v1-0-13.md`) & Spec Kit (`specs/004-release-v1-0-13/`) authored prior to edits.
- [x] **CHK-REL-02:** `app/build.gradle.kts` set to `versionCode = 14`, `versionName = "1.0.13"`.
- [x] **CHK-REL-03:** `README.md` matches empirical corpus metrics (`65.716` hadis, `120.616` rujukan, `fawazahmed0` + `irsyadulibad`) and links `v1.0.13/app-debug.apk`.
- [x] **CHK-REL-04:** Python validators, `242/242` JVM tests, `0`-error Lint, `:app:assembleDebug`, and `:app:assembleRelease` pass after final edit.
- [x] **CHK-REL-05:** `adb -s RR2N800H9DR install -r -d` succeeds without clearing data (`firstInstallTime` unchanged, `versionCode=14`, clean cold launch).
- [ ] **CHK-REL-06:** Scoped commit pushed to `origin/main`, CI `regression.yml` green, tag `v1.0.13` & GitHub Release `v1.0.13` live with `app-debug.apk`.
