# Accounts Credit Card Specialization Specification

Status: Blocked
Blocker: Owner/product financial-semantics decision remains pending.
Active Repository Objective: No; #56 reminder validation remains active via `.ai/current.md`.
Objective: Define the reference-backed Accounts credit-card presentation boundary from the approved screenshot without inventing financial semantics or a Credit Card Details workflow.
Branch: `docs/accounts-credit-card-specialization`
Scope: `docs + reference specification`
Created: `2026-09-29`
Last Updated: `2026-09-30`
Issue: `#74`

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/financial-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/references/README.md`
- `docs/references/notes/account.md`
- `docs/product-requirements.md`
- `docs/definition-of-done.md`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/account/AccountUseCases.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsViewModel.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsScreen.kt`

## Constraints

- GitHub-only specification work; no local build or emulator required.
- No production Kotlin, Gradle, SQLDelight, schema, route, or financial behavior change.
- The approved Accounts screenshot is the highest-priority reference.
- Do not infer formulas from the screenshot.
- Do not invent a Credit Card Details screen or account-row navigation.
- Preserve ledger-first, deterministic derived balances and immutable history.
- #56/#43 reminder validation remains active with genuine late-boot delivery/deep-link evidence recorded on main; do not change its continuity or device fixture.
- No production implementation is authorized; keep this non-active task open while #74 awaits owner/product decisions.

## Decisions

- The approved screenshot supports a distinct Credit Card group with `Balance Payable` and `Outst. Balance` presentation.
- Current Tio Application state exposes only one generic account balance, so presentation-only implementation is not financially sufficient.
- The architecture audit confirms that only current ledger-derived credit-card liability is deterministic today; statement and billing concepts are not persisted.
- A future implementation requires an approved ledger-first definition and Application read contract for both values.
- Dedicated Credit Card Details remains undecided and requires separate reference evidence/approval.

## Progress

- [x] Fresh repository/reference audit.
- [x] Create focused issue #74.
- [x] Record screenshot-supported presentation facts.
- [x] Record unresolved financial semantics and implementation gate.
- [x] Open docs-only PR #75 and verify CI #434 succeeded on head `b3d1344b3883dc67c44d5fb8a3e813b2a18fe8c8`.
- [x] Complete the ledger, Application, and SQLDelight architecture audit for #74.
- [x] Owner-authorized PR #75 squash merged on 2026-09-30 as `508f70e5ca6974fca268de648f8326cc925bf6e2`.
- [ ] Obtain the owner/product financial-semantics decision before any production implementation.

## Validation

- GitHub Actions CI #434 succeeded on PR #75 head `b3d1344b3883dc67c44d5fb8a3e813b2a18fe8c8`; reverify CI on any later head.
- Architecture audit completed from current Domain, Application, finance-engine, and SQLDelight source without device validation.
- Refreshed PR #75 preserved both merged #56 continuity files unchanged; exact-head CI #439 was reverified before merge.
- Refreshed PR head `9bc66fab5dcd095add9b23030434645bbc60d21f` passed CI #439 before merge; post-merge main passed CI #440. No runtime change was included.

## Changed Files

- `docs/references/notes/account.md`
- `.ai/tasks/docs/local-20260929-accounts-credit-card-specialization.md`

## Next Action

PR #75 is merged. Await an owner/product decision on ledger-first financial semantics before any implementation. Reference-boundary work and the architecture audit are complete. Keep Accounts v1 generic and #56 as the active repository objective; do not archive this task while #74 remains open.
