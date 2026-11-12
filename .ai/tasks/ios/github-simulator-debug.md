# GitHub iOS Simulator and iPhone Debug Build

Status: In Progress
Objective: Build and launch the iOS debug app in GitHub Actions and publish simulator and unsigned device IPA artifacts for Windows sideloading.
Implementation Owner: Codex
Branch: `feat/ios-github-debug`
Base SHA: `30ad6d07a990018db4d8ef26de71fec06b511723`
Inspected Head SHA: `30ad6d07a990018db4d8ef26de71fec06b511723`
Scope: `apps/ios`, `.github/workflows/ios-debug.yml`, `.gitignore`, `.ai` continuity
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
- [ ] Run macOS CI build, simulator launch, and device IPA packaging on GitHub.
- [ ] Review exact CI result and fix any build or launch failures.

## Validation Evidence

- Checks executed: Combined PUSH_TEMPLATE Gradle command for metadata, critical tests, migration verification, ktlint and detekt completed `BUILD SUCCESSFUL`; YAML and plist parsing PASS; `git diff --check` PASS.
- Task outcomes: critical tests, ktlint and detekt completed; metadata compilation and SQLDelight migration verification were reported `SKIPPED` by Gradle on this Windows run.
- Checks incomplete / unavailable: Xcode device/simulator builds and simulator launch require macOS; pending GitHub Actions run is the platform validation.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/ios/github-simulator-debug.md`
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
- Remaining risks: macOS build and runtime behavior have not been verified.

## Next Action And Handoff

- Owner: Codex
- Next Action: Publish the AltServer IPA changes to the existing branch, run the macOS workflow, and resolve its exact failures.
