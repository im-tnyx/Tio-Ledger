# GitHub Android and iOS Manual Debug Builds

Status: Complete (CI artifacts verified)
Objective: Manually select Android or iOS in GitHub Actions; publish an Android debug APK or the iOS simulator and unsigned device IPA artifacts, while keeping automatic iOS push/PR validation.
Implementation Owner: Codex
Branch: `feat/ios-github-debug`
Base SHA: `30ad6d07a990018db4d8ef26de71fec06b511723`
Inspected Head SHA: `6f9f00eb3f1de2f357e6a8abbe87373c6be32261`
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
- [x] Ran the manual Android selection; APK build and upload passed on GitHub.
- [x] Fourth macOS run built and uploaded the iPhone IPA but exposed missing `CFBundleExecutable` during simulator installation; added that plist key and executable checks for device and simulator app bundles.
- [x] Re-ran iOS after the bundle metadata fix; simulator launched, and the unsigned IPA, simulator app, and launch screenshot uploaded successfully.
- [x] Reviewed exact Android and iOS run results and artifact records.

## Validation Evidence

- Checks executed before the Kotlin/Native compile fix: Combined PUSH_TEMPLATE Gradle command for metadata, critical tests, migration verification, ktlint and detekt completed `BUILD SUCCESSFUL`; YAML and plist parsing PASS; `git diff --check` PASS.
- Task outcomes from that run: critical tests, ktlint and detekt completed; metadata compilation and SQLDelight migration verification were reported `SKIPPED` by Gradle on this Windows run.
- Current validation: workflow YAML and iOS Info.plist parse; `CFBundleExecutable`, the `android`/`ios` choices, job conditions, APK artifact path, and app-executable checks were inspected. `git diff --check` passes. iOS run `43423984672` on `6f9f00e` completed successfully; simulator app, launch screenshot, and unsigned IPA artifacts are available until `2027-02-11`.
- Manual Android run `43422634476` on `4c23e74` completed successfully; debug APK artifact `tio-ledger-android-debug-apk-8-1` is available until `2026-11-20`.
- The PUSH_TEMPLATE Gradle command could not start locally because the sandbox could not resolve `services.gradle.org` to download Gradle 8.10.2; GitHub Actions supplies the platform build validation.
- Not verified: the unsigned IPA has not been installed on a physical iPhone in this workspace; use AltServer on Windows as documented in `apps/ios/README.md`.
- GitHub run `43189289597` on `19f088a` failed during the simulator Xcode build because `./gradlew` was not executable; no IPA or simulator artifacts were produced.
- GitHub run `43189549677` on `badf766` reached Kotlin/Native compilation but failed in `shared/ui/.../AccountsViewModel.kt` because `toSortedMap()` is unavailable in common code; replacing it with sorted map entries is the scoped fix and awaits local plus macOS validation.
- GitHub run `43191905124` on `411fe4a` passed the Kotlin/Native framework build but failed the app executable link because the static framework's SQLDelight native driver requires Xcode `-lsqlite3`; device IPA and simulator launch steps were skipped.
- GitHub run `43420799441` on `995661e` built and uploaded the unsigned device IPA but failed simulator install because `TioLedger.app/Info.plist` had no valid `CFBundleExecutable`; the workflow's simulator-app and screenshot uploads were skipped.
- GitHub run `43422395663` on `4c23e74` had the same pre-fix simulator installation failure; superseded by successful run `43423984672`.
- GitHub manual Android run `43422634476` on `4c23e74` passed the APK build and upload; the iOS job was skipped as selected.
- GitHub run `43423984672` on `6f9f00e` passed the simulator Debug build, device Debug build, IPA upload, simulator launch, simulator-app upload, and screenshot upload.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/ios/github-simulator-debug.md`
- `apps/android/README.md`
- `.github/workflows/ios-debug.yml`
- `apps/ios/README.md`
- `apps/ios/iosApp/Info.plist`
- `apps/ios/project.yml`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsViewModel.kt`
- `.gitignore`
- `apps/ios/iosApp/TioLedgerApp.swift`
- `apps/ios/build.gradle.kts`
- `apps/ios/src/iosMain/kotlin/com/tioledger/apps/ios/IosApp.kt`

## Review Evidence And Findings

- Review Status: REVIEW_PENDING
- Independent Reviewer: None yet
- Remaining risks: physical iPhone installation has not been exercised; the delivered IPA is unsigned and must be signed by AltServer for device installation.

## Next Action And Handoff

- Owner: Codex
- Next Action: User downloads the Android APK or iOS IPA from the successful workflow run and installs on a test device.
