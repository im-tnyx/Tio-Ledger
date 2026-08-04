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
- [ ] On Windows, if Java is not on `PATH`, set `JAVA_HOME` for this shell.
- [ ] When local sandboxed Gradle state is noisy, keep `GRADLE_USER_HOME` and `ANDROID_USER_HOME` explicit for repeatable validation.

## 3. Required Validation Before Push

Run these from the repository root. Use `./gradlew` on bash/macOS/Linux and `./gradlew.bat` on Windows PowerShell.

- [ ] Compile shared metadata:
  ```powershell
  .\gradlew.bat :shared:core:compileKotlinMetadata :shared:domain:compileKotlinMetadata :shared:finance-engine:compileKotlinMetadata :shared:analytics:compileKotlinMetadata :shared:application:compileKotlinMetadata :shared:data:compileKotlinMetadata :shared:database:compileKotlinMetadata :shared:bootstrap:compileKotlinMetadata :shared:ui:compileKotlinMetadata --no-daemon --console=plain --stacktrace
  ```
- [ ] Run critical tests:
  ```powershell
  .\gradlew.bat :shared:finance-engine:test :shared:analytics:test :shared:application:test :shared:data:test :shared:ui:test --no-daemon --console=plain --stacktrace
  ```
- [ ] Verify SQLDelight migrations explicitly:
  ```powershell
  .\gradlew.bat :shared:database:verifyCommonMainTioLedgerDatabaseMigration --no-daemon --console=plain --stacktrace
  ```
- [ ] Run Kotlin formatting validation:
  ```powershell
  .\gradlew.bat ktlintCheck --no-daemon --console=plain --stacktrace
  ```
- [ ] Run static analysis:
  ```powershell
  .\gradlew.bat detekt --no-daemon --console=plain --stacktrace
  ```
- [ ] Check whitespace and patch integrity before push:
  ```powershell
  git diff --check
  ```

Do not rely on broad root `build` or broad root `check` as the default CI-equivalent validation path for this repository.

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
