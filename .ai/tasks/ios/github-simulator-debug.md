# GitHub iOS Simulator Debug Build

Status: In Progress
Objective: Build and launch the iOS debug app in GitHub Actions and publish a simulator artifact.
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
- Start with an unsigned simulator build and launch. Physical iPhone installation requires separate Apple signing assets.
- Preserve the pre-existing untracked `.gradle-user-home/` and unrelated account-settings branch.

## Decisions

- Generate the Xcode project from `apps/ios/project.yml` on a macOS runner.
- Use Kotlin direct framework integration and the existing shared Compose shell.

## Progress

- [x] Created an isolated local branch from cached `origin/main`.
- [x] Added iOS host and simulator workflow source.
- [ ] Run macOS CI build and simulator launch on GitHub.
- [ ] Review exact CI result and fix any build or launch failures.

## Validation Evidence

- Checks executed: YAML and plist parsing PASS; `git diff --check` PASS.
- Checks incomplete / unavailable: iOS compile and simulator launch require macOS; local Gradle metadata and ktlint checks were interrupted after dependency setup stalled without task output.

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
- Next Action: Publish this branch once GitHub access returns, run the macOS workflow, and resolve its exact failures.
