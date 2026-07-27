# Workflow

Current workflow:

1. Treat the checked-in documentation and ADRs as the product and architecture source of truth.
2. Keep the Gradle Kotlin Multiplatform repository foundation healthy.
3. Make small, reviewable changes on focused branches.
4. Verify build, lint, static analysis, and tests before pushing.
5. Keep financial and ledger behavior covered by focused tests when affected.

Database Schema, Ledger Engine, Application Layer, and Data Layer are Frozen v1. Application Bootstrap v1 and UI Foundation v1 are established. Navigation Graph v1 is approved and tagged. Phase 6 Finance Features is complete. The current active product milestone is Phase 7 Spending Analytics / Reports Screen v1.

Do not start a new milestone unless explicitly requested.

For Phase 7 Spending Analytics / Reports Screen v1 work:

- Follow the approved roadmap and existing module boundaries.
- Inspect existing immutable transaction-history and summary read contracts before adding new APIs.
- Add approved Reports references and acceptance notes before production UI implementation.
- Keep aggregation and financial calculations outside Compose and ViewModels.
- Use integer minor units and deterministic period boundaries for monetary summaries.
- Expose immutable UI-ready state through Application use cases and ViewModels.
- Reuse existing Application Layer, Repository Contracts, Data Layer, Bootstrap, UI Foundation, and typed navigation.
- Keep unrelated refactors out of feature branches.
- Update documentation only when architecture, workflow, or documented behavior changes.
- Update the architecture changelog only for project structure, data flow, module boundary, or engineering practice changes.
- Satisfy the Definition of Done before merge.
- Before commit, push, or PR creation, read and follow `../.github/PUSH_TEMPLATE.md`.
- After a PR is merged, follow `../.github/POST_MERGE_SYNC.md` for local synchronization and branch cleanup.

Canonical references:

- [../docs/implementation-roadmap.md](../docs/implementation-roadmap.md)
- [../docs/definition-of-done.md](../docs/definition-of-done.md)
- [../docs/architecture.md](../docs/architecture.md)
- [../docs/module-design.md](../docs/module-design.md)
- [../docs/references/README.md](../docs/references/README.md)
- [../.github/PUSH_TEMPLATE.md](../.github/PUSH_TEMPLATE.md)
- [../.github/POST_MERGE_SYNC.md](../.github/POST_MERGE_SYNC.md)