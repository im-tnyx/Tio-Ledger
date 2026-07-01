# Build Verification Report

Date: 2026-07-01
Outcome: PASS
Milestone: Navigation Graph v1

## Environment

- OS shell: Windows PowerShell
- JDK: Android Studio bundled JBR via `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- Gradle wrapper: `gradlew.bat`

## Commands Run

| Command | Result |
| --- | --- |
| `./gradlew.bat build --stacktrace --no-daemon` | PASS |
| `./gradlew.bat check --stacktrace --no-daemon` | PASS |
| `./gradlew.bat ktlintCheck --stacktrace --no-daemon` | PASS |
| `./gradlew.bat detekt --stacktrace --no-daemon` | PASS |
| `git diff --check` | PASS |

## Verified Areas

| Area | Result |
| --- | --- |
| Navigation route model compilation | PASS |
| Root and main graph tests | PASS |
| Placeholder destination host compilation | PASS |
| Android app module build | PASS |
| Wear OS app module build | PASS |
| iOS shared wiring available in Gradle build | PASS |
| Kotlin style checks | PASS |
| Static analysis | PASS |

## Freeze Compliance

| Frozen Layer | Result |
| --- | --- |
| Database (`shared:database`) | PASS: no changes |
| Ledger Engine (`shared:finance-engine`) | PASS: no changes |
| Application Layer (`shared:application`) | PASS: no changes |
| Data Layer (`shared:data`) | PASS: no changes |
| Bootstrap (`shared:bootstrap`) | PASS: no changes |
| UI Foundation (`shared:ui` tokens/components/templates) | PASS: navigation changes reused the frozen foundation without altering design tokens, reusable components, or templates |

## Notes

- The repository currently contains newer feature work outside this navigation-only milestone, but this validation pass was limited to the navigation graph changes and their build impact.
- The Gradle wrapper required user-approved access to the user-level Gradle cache outside the workspace.
- Native iOS execution was not run on Windows; Gradle validated the configured shared/iOS wiring available in this environment.

## Decision

All requested verification gates passed for Navigation Graph v1.
