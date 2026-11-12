# GitHub iOS Simulator and iPhone Debug Build

Status: In Progress
Objective: Build and launch the iOS debug app in GitHub Actions and publish simulator and unsigned device IPA artifacts for Windows sideloading.
Implementation Owner: Codex
Branch: `feat/ios-github-debug`
Base SHA: `30ad6d07a990018db4d8ef26de71fec06b511723`
Inspected Head SHA: `badf76635d7b9ed0aa7e2ec87122ffe38414cd26`
Scope: `apps/ios`, `.github/workflows/ios-debug.yml`, `.gitignore`, `.ai` continuity, targeted `shared/ui` iOS compile unblock
Created: `2026-11-12`
Last Updated: `2026-11-12`

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
- [ ] Re-run macOS CI build, simulator launch, and device IPA packaging on GitHub.
- [ ] Review exact CI result and fix any build or launch failures.

## Validation Evidence

- Checks executed before the Kotlin/Native compile fix: Combined PUSH_TEMPLATE Gradle command for metadata, critical tests, migration verification, ktlint and detekt completed `BUILD SUCCESSFUL`; YAML and plist parsing PASS; `git diff --check` PASS.
- Task outcomes from that run: critical tests, ktlint and detekt completed; metadata compilation and SQLDelight migration verification were reported `SKIPPED` by Gradle on this Windows run.
- Current fix validation: `git diff --check` PASS. The PUSH_TEMPLATE Gradle command could not start because the sandbox could not resolve `services.gradle.org` to download Gradle 8.10.2; the new macOS GitHub run is required to validate Kotlin/Native compilation.
- Checks incomplete / unavailable: Xcode device/simulator builds and simulator launch require macOS; current GitHub Actions run is the platform validation.
- GitHub run `43189289597` on `19f088a` failed during the simulator Xcode build because `./gradlew` was not executable; no IPA or simulator artifacts were produced.
- GitHub run `43189549677` on `badf766` reached Kotlin/Native compilation but failed in `shared/ui/.../AccountsViewModel.kt` because `toSortedMap()` is unavailable in common code; replacing it with sorted map entries is the scoped fix and awaits local plus macOS validation.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/ios/github-simulator-debug.md`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsViewModel.kt`
- `.gitignore`
- `.github/workflows/ios-debug.yml`
- `apps/ios/README.md`
- `apps/ios/build.gradle.kts`
- `apps/ios/iosApp/Info.plist`
- `apps/ios/iosApp/TioLedgerApp.swift`
- `apps/ios/project.yml`
- `apps/ios/src/iosMain/kotlin/com/tioledger/apps/ios/IosApp.kt`

## Review Evidence And Findings

- Review Status: REVIEW_PENDING
- Independent Reviewer: None yet
- Remaining risks: macOS build, simulator launch, and unsigned iPhone IPA packaging have not been verified.

## Next Action And Handoff

- Owner: Codex
- Next Action: Publish the `shared/ui` common-code sorting fix to the existing branch, then inspect the exact macOS build, simulator launch, and unsigned IPA artifact results.
