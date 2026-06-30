# AGENTS.md

Repository: im-tnyx/Tio-Ledger

These instructions apply to AI agents working in this repository.

## Communication

- Respond in Hindi unless the user explicitly asks for English.
- Keep code, file names, folder names, APIs, classes, functions, commands, and technical terms in English.
- Be direct, practical, and production-focused.

## Source Of Truth

Before code changes, inspect the actual repo and read the relevant source-of-truth docs:

1. `README.md`
2. `docs/README.md`
3. `docs/engineering-guidelines.md`
4. `docs/definition-of-done.md`
5. `docs/architecture.md`
6. `docs/adr/README.md`
7. `.ai/README.md`
8. `.ai/workflow.md`

Runtime source/config wins for actual behavior. Product docs and ADRs win for intended architecture and product rules. If docs and runtime disagree, call out the stale doc clearly.

## Repo Ownership

- `apps/android` owns the Android app shell.
- `apps/wear` owns the Wear OS app shell.
- `apps/ios` owns the iOS app shell/framework wiring.
- `shared/core` owns shared primitives.
- `shared/domain` owns domain models and use cases.
- `shared/data` owns repositories/data coordination.
- `shared/database` owns SQLDelight persistence.
- `shared/finance-engine` owns ledger and posting rules.
- `shared/loan-engine` owns loan and EMI calculations.
- `shared/budget-engine` owns budgeting logic.
- `shared/analytics` owns analytics logic.
- `shared/notifications` owns notification logic.
- `shared/ui` owns shared Compose UI components.
- `docs` owns canonical product and architecture docs.
- `.github` owns contribution, PR, push, CI, and post-merge workflow docs.
- `.ai` owns concise AI orientation files.

## Engineering Rules

- Keep changes small, focused, and reviewable.
- Respect module boundaries.
- Reuse existing patterns before adding new abstractions.
- Do not duplicate financial logic across apps or modules.
- Do not use `Float` or `Double` for money.
- Keep ledger entries immutable and derive balances from ledger entries.
- Require explicit user confirmation for automation that affects financial records.
- Do not invent APIs, business rules, UI workflows, or architecture that conflict with checked-in docs/source.

## Kotlin Multiplatform And UI Rules

- Business logic belongs in shared KMP modules.
- Presentation layers must not contain financial calculations.
- UI should follow approved references under `docs/references/` when applicable.
- Do not rename `rootProject.name`, package names, app IDs, or module paths without explaining impact and updating dependent config/imports.

## Git And Push Workflow

Before commit, push, or PR creation:

1. Read `.github/PUSH_TEMPLATE.md`.
2. Confirm repository state with `git status --short --branch`.
3. Keep unrelated local changes out of the commit.
4. Run the applicable validation commands from `.github/PUSH_TEMPLATE.md`.
5. List validations actually run in the PR.

Do not use `./gradlew clean compileKotlin` as root validation. It is ambiguous in this KMP/Android repo.

For Pull Requests, follow `.github/PULL_REQUEST_TEMPLATE.md`.

After a PR merge, follow `.github/POST_MERGE_SYNC.md` before starting the next branch.

## Never Commit

Never commit generated/cache/secrets or local machine state, including:

- `node_modules`, `dist`, `.next`, `.turbo`
- `.gradle`, `.kotlin`, `build`, Gradle outputs
- APK/AAB files
- `.env` files
- service accounts, keystores, private keys, signing files
- IDE user state and local machine paths
