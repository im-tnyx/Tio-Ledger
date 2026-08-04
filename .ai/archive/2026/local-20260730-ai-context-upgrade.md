# Compact AI Context Upgrade

Status: Complete
Objective: Establish a single-pointer AI continuity system that survives
conversation compaction without loading stale or unrelated task history.
Branch: `codex/ai-context-upgrade`
Scope: `AGENTS.md`, `.ai/`, and directly affected canonical status docs
Created: `2026-07-30`
Last Updated: `2026-07-30`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/21`

## Required Context

- `.ai/core/workflow-rules.md`
- `AGENTS.md`
- `README.md`
- `docs/README.md`
- `docs/implementation-roadmap.md`
- `docs/architecture-changelog.md`

## Constraints

- Runtime source and canonical docs remain authoritative.
- Only one task may be active in `.ai/current.md`.
- `.ai/tasks/` and `.ai/archive/` must never be preloaded.
- Historical validation reports must retain the paths they actually reviewed.
- No runtime, database, financial, or UI behavior changes are in scope.

## Decisions

- Use `.ai/current.md` as the only session continuity pointer.
- Separate stable summaries under `.ai/core/` from volatile task state.
- Keep active or paused work under `.ai/tasks/<scope>/`.
- Archive completed records under `.ai/archive/<year>/`.
- Retire `.ai/instructions.md.md` because it duplicates `AGENTS.md` and
  contains a precision rule that conflicts with integer-minor-unit money.
- Retire stale milestone prose instead of carrying it into stable core files.

## Progress

- [x] Verify the Git root and clean starting state.
- [x] Audit legacy `.ai` paths and repository references.
- [x] Add the single-pointer loader and task/archive lifecycle.
- [x] Migrate stable rules into `.ai/core/`.
- [x] Align canonical milestone status with implemented Reports v1.
- [x] Validate links, formatting, and final diff.
- [x] Mark PR #21 ready for review after checks passed.
- [x] Merge PR #21 to `main` and complete local post-merge sync.
- [x] Archive the completed task and clear `.ai/current.md`.

## Validation

- `git diff --check`: PASS.
- `.ai` relative Markdown link scan: PASS.
- Active pointer, required-context paths, legacy-file retirement, and archive
  tracking checks: PASS.
- Live milestone text and conflicting `BigDecimal` mandate scans: PASS.
- Draft PR #21 created against `main`: PASS.
- PR #21 checks passed: `SQLDelight migration verification`,
  `Targeted KMP validation`.
- PR #21 marked ready for review: PASS.
- PR #21 merged to `main`: PASS.
- Local post-merge sync and remote prune completed: PASS.
- Gradle checks not run because runtime source and build configuration did not
  change.

## Changed Files

- `.ai/README.md`
- `.ai/current.md`
- `.ai/core/architecture.md`
- `.ai/core/coding-rules.md`
- `.ai/core/financial-rules.md`
- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `.ai/tasks/README.md`
- `.ai/tasks/docs/local-20260730-ai-context-upgrade.md`
- `.ai/archive/README.md`
- `.ai/architecture-summary.md` (retired)
- `.ai/coding-rules.md` (retired)
- `.ai/financial-rules.md` (retired)
- `.ai/instructions.md.md` (retired)
- `.ai/project-context.md` (retired)
- `.ai/ui-rules.md` (retired)
- `.ai/workflow.md` (retired)
- `.gitignore`
- `AGENTS.md`
- `README.md`
- `docs/README.md`
- `docs/implementation-roadmap.md`
- `docs/architecture-changelog.md`

## Closure

PR #21 was merged on `2026-07-30`, local `main` was synchronized with
`origin/main`, the remote feature branch was pruned, and the continuity pointer
was reset to idle state.
