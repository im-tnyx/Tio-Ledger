## Summary

- Describe the change.

## Type Of Change

- [ ] Documentation
- [ ] Repository foundation
- [ ] Build or tooling
- [ ] Architecture
- [ ] Product behavior
- [ ] Financial logic
- [ ] UI
- [ ] Tests only

## Scope

Changed areas:

- List changed folders/files.

Out of scope:

- List intentional non-goals.

## Documentation And ADRs

- [ ] Relevant documentation updated.
- [ ] ADR updated if architecture changed.
- [ ] Architecture changelog updated if applicable.
- [ ] No documentation change needed.

## Definition Of Done

- [ ] Requirements implemented.
- [ ] Business rules verified.
- [ ] Ledger integrity preserved, if applicable.
- [ ] Financial calculations validated, if applicable.
- [ ] Unit tests pass.
- [ ] Integration tests pass, where applicable.
- [ ] No TODO, FIXME, or placeholder code in production paths.
- [ ] No compiler warnings introduced.
- [ ] UI matches approved reference, if applicable.
- [ ] Light and dark themes verified, if applicable.
- [ ] Responsive layouts verified, if applicable.
- [ ] Accessibility reviewed, if applicable.
- [ ] No measurable performance regression.

## Validation

Follow `.github/PUSH_TEMPLATE.md` before push. List only checks actually run.

- [ ] `./gradlew :shared:core:compileKotlinMetadata :shared:domain:compileKotlinMetadata :shared:finance-engine:compileKotlinMetadata :shared:application:compileKotlinMetadata :shared:data:compileKotlinMetadata :shared:database:compileKotlinMetadata :shared:bootstrap:compileKotlinMetadata :shared:ui:compileKotlinMetadata --no-daemon --console=plain --stacktrace`
- [ ] `./gradlew :shared:finance-engine:test :shared:application:test :shared:data:test :shared:ui:test --no-daemon --console=plain --stacktrace`
- [ ] `./gradlew :shared:database:verifyCommonMainTioLedgerDatabaseMigration --no-daemon --console=plain --stacktrace`
- [ ] `./gradlew ktlintCheck --no-daemon --console=plain --stacktrace`
- [ ] `./gradlew detekt --no-daemon --console=plain --stacktrace`
- [ ] `git diff --check`
- [ ] Other:
- [ ] Not run. Reason:

## Safety Notes

- [ ] No generated/cache/secrets committed.
- [ ] No unrelated refactor included.
- [ ] No package, module, app ID, root project name, or persistence contract renamed without documented impact.
- [ ] Financial history remains immutable, if applicable.
- [ ] Automation still requires user confirmation for financial mutations, if applicable.

## Commit Attribution And Merge Handoff

- [ ] Inspect **every branch commit** for unintended AI co-author and self-credit trailers; this is not guaranteed solely by a passing fixture test.
- [ ] Before the owner-approved merge, inspect the **final** squash/merge subject, body and trailers; do not accept generated AI attribution. A PR-head check cannot scan a newly created merge message.
- [ ] After merge, check the resulting `main` commit and the separate **Post-merge attribution audit**; failures are post-merge detection, not pre-merge prevention.
- [ ] Require the protected `main` branch to be **up to date before merging** (`strict_required_status_checks_policy: true`) before claiming the App guard enforces current-main policy; update/re-run stale PR checks after `main` advances.
- [ ] Do not treat App-owned `Commit attribution guard` as required until its exact App-source status check is verified and pinned in the protected-main ruleset.

## Notes

Add implementation notes, intentional deviations, skipped checks, or non-applicable checklist explanations.
