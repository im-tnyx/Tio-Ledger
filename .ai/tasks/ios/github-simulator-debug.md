# GitHub Android and iOS Manual Debug Builds

Status: In Progress
Objective: Manually select Android or iOS in GitHub Actions; publish an Android debug APK or the iOS simulator and unsigned device IPA artifacts, while keeping automatic iOS push/PR validation.
Implementation Owner: Codex
Branch: `feat/ios-github-debug`
Base SHA: `30ad6d07a990018db4d8ef26de71fec06b511723`
Inspected Head SHA: `411fe4a0b512407a23dae807e27d823a223c8bb1`
Scope: `apps/android`, `apps/ios`, `.github/workflows/ios-debug.yml`, `.gitignore`, `.ai` continuity, targeted iOS compile/link unblocks
Created: `2026-11-12`
Last Updated: `2026-11-13`

## Required Context

- `docs/module-design.md`
- `apps/ios/README.md`

## Approved Scope And Constraints

- Owner requested GitHub-based iOS build and testing because the local machine runs Windows.
- Build the simulator app and an unsigned device IPA. Use AltServer on Windows to sign and install it with the user's Apple Account; this free-account install expires after seven days.
- Paid Apple Developer Program signing and Ad Hoc distribution are out of scope.
- Preserve the pre-existing untracked `.gradle-user-home/` and unrelated account-settings branch.

## Decisions

- Generate the Xcode project from `apps/ios/project.yml` on a macOS runner.
- Use Kotlin direct framework integration and the existing shared Compose shell.

## Progress

- [x] Created an isolated local branch from cached `origin/main`.
- [x] Added iOS host and simulator workflow source.
- [x] Added unsigned iPhone IPA packaging for AltServer and Windows install instructions.
- [x] First macOS run reached Xcode and found the checkout's Gradle wrapper was not executable; added an explicit chmod step.
- [x] Second macOS run exposed a JVM-only `toSortedMap()` call in common UI code; replaced it with common collection sorting.
- [x] Third macOS run passed Kotlin/Native compilation but exposed unresolved `sqlite3_*` symbols in the Xcode app link; added the native SQLite linker flag to the generated app project settings.
- [x] Added an Android/iOS manual platform selector and a separate Android debug APK artifact job; automatic push/PR iOS validation remains enabled.
- [ ] Re-run macOS CI build, simulator launch, and device IPA packaging on GitHub.
- [ ] Run and verify the manually selected Android APK build on GitHub.
- [ ] Review exact CI result and fix any build or launch failures.

## Validation Evidence

- Checks executed before the Kotlin/Native compile fix: Combined PUSH_TEMPLATE Gradle command for metadata, critical tests, migration verification, ktlint and detekt completed `BUILD SUCCESSFUL`; YAML and plist parsing PASS; `git diff --check` PASS.
- Task outcomes from that run: critical tests, ktlint and detekt completed; metadata compilation and SQLDelight migration verification were reported `SKIPPED` by Gradle on this Windows run.
- Current fix validation: the manual workflow YAML parses, its `android`/`ios` choices, job conditions, and APK artifact path were checked, and `git diff --check` passes. The PUSH_TEMPLATE Gradle command could not start because the sandbox could not resolve `services.gradle.org` to download Gradle 8.10.2; GitHub Actions must validate the new workflow and Android APK.
- Checks incomplete / unavailable: Xcode device/simulator builds and simulator launch require macOS; Android APK packaging requires a GitHub Android SDK runner.
- GitHub run `43189289597` on `19f088a` failed during the simulator Xcode build because `./gradlew` was not executable; no IPA or simulator artifacts were produced.
- GitHub run `43189549677` on `badf766` reached Kotlin/Native compilation but failed in `shared/ui/.../AccountsViewModel.kt` because `toSortedMap()` is unavailable in common code; replacing it with sorted map entries is the scoped fix and awaits local plus macOS validation.
- GitHub run `43191905124` on `411fe4a` passed the Kotlin/Native framework build but failed the app executable link because the static framework's SQLDelight native driver requires Xcode `-lsqlite3`; device IPA and simulator launch steps were skipped.
- GitHub run `43420799441` on `995661e` remained in progress at the last check while its simulator build step was running; its final status must be checked separately from this workflow revision.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/ios/github-simulator-debug.md`
- `apps/android/README.md`
- `.github/workflows/ios-debug.yml`
- `apps/ios/README.md`
- `apps/ios/project.yml`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsViewModel.kt`
- `.gitignore`
- `apps/ios/iosApp/Info.plist`
- `apps/ios/iosApp/TioLedgerApp.swift`
- `apps/ios/build.gradle.kts`
- `apps/ios/src/iosMain/kotlin/com/tioledger/apps/ios/IosApp.kt`

## Review Evidence And Findings

- Review Status: REVIEW_PENDING
- Independent Reviewer: None yet
- Remaining risks: latest macOS app linking, simulator launch, unsigned iPhone IPA packaging, and the manually selected Android APK artifact remain unverified.

## Next Action And Handoff

- Owner: Codex
- Next Action: Publish the manual Android/iOS platform selector and inspect the exact iOS run; then run Android and verify its separate APK artifact.
