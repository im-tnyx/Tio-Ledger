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

- [ ] `./gradlew build --stacktrace`
- [ ] `./gradlew ktlintCheck --stacktrace`
- [ ] `./gradlew detekt --stacktrace`
- [ ] Other:
- [ ] Not run. Reason:

## Safety Notes

- [ ] No generated/cache/secrets committed.
- [ ] No unrelated refactor included.
- [ ] No package, module, app ID, root project name, or persistence contract renamed without documented impact.
- [ ] Financial history remains immutable, if applicable.
- [ ] Automation still requires user confirmation for financial mutations, if applicable.

## Notes

Add implementation notes, intentional deviations, skipped checks, or non-applicable checklist explanations.
