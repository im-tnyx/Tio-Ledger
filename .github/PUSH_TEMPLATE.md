# Push Verification Checklist

Use this checklist before committing, pushing, or opening a Pull Request. It is written for both humans and AI assistants so validation is explicit and repeatable.

## 1. Scope And Repository State

- [ ] Confirm the checkout is the expected repository and branch:
  ```powershell
  git remote -v
  git status --short --branch
  ```
- [ ] Keep unrelated local changes out of the commit.
- [ ] Review staged changes before committing:
  ```powershell
  git diff --cached
  ```
- [ ] Do not commit generated output, caches, secrets, keystores, APK/AAB files, `.env` files, or local IDE/build artifacts.

## 2. Environment Preflight

- [ ] Confirm Java is available before running Gradle:
  ```powershell
  java -version
  ```
- [ ] On Windows, if Java is not on `PATH`, use the Android Studio JBR for this shell:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  ```

## 3. Required Validation Before Push

Run these from the repository root. Use `./gradlew` on bash/macOS/Linux and `.\gradlew.bat` on Windows PowerShell.

- [ ] Build and unit-test the Gradle project:
  ```powershell
  .\gradlew.bat build --stacktrace
  ```
- [ ] Run Kotlin formatting validation:
  ```powershell
  .\gradlew.bat ktlintCheck --stacktrace
  ```
- [ ] Run static analysis:
  ```powershell
  .\gradlew.bat detekt --stacktrace
  ```

Do not use `./gradlew clean compileKotlin` as a root validation command. In this KMP/Android project, `compileKotlin` is ambiguous from the root project.

## 4. Optional Local Formatting

Only run formatter tasks when you intend to include the resulting formatting changes in the same commit.

- [ ] Format Kotlin sources if needed:
  ```powershell
  .\gradlew.bat ktlintFormat
  ```
- [ ] Re-check the diff after formatting:
  ```powershell
  git diff
  git diff --cached
  ```

## 5. Financial And Ledger Invariants

For changes touching ledger, finance, loan, budget, persistence, or posting behavior:

- [ ] Every posted transaction satisfies `sum(debits) == sum(credits)` across all affected entries.
- [ ] Account balances remain derived from ledger entries, not manually mutated totals.
- [ ] Money calculations do not use `Float` or `Double`.
- [ ] Currency mismatches are rejected or handled by documented conversion rules.
- [ ] Focused regression tests cover changed financial behavior.

## 6. PR Readiness

- [ ] Commit message uses a concise conventional format such as `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, or `ci:`.
- [ ] Pull Request body follows `.github/PULL_REQUEST_TEMPLATE.md`.
- [ ] Validation commands actually run are listed in the PR.
- [ ] Any skipped validation is called out with the reason.
