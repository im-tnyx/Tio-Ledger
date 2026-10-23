# Active Task

Status: Ready for Review
Active Task: `.ai/tasks/docs/local-20261023-reference-reconciliation.md`
Branch: `tnyx/docs-reference-inventory-planning`
Platform Scope: `docs/references, canonical roadmap, and documentation continuity`
Last Updated: `2026-10-23`
Next Action: `The docs-only branch has been committed and pushed. Obtain independent review at its exact published head before any PR/merge decision. PR #82 and PR #86 are merged; their historical device evidence is not validation of this documentation branch. No PR creation or merge is authorized for this slice.`

## Usage

- Read this file at the beginning of every work session after `AGENTS.md`.
- When `Active Task` is not `none`, read only the referenced task file.
- Load only the `.ai/core/` files explicitly listed by that task.
- Do not preload or scan `.ai/tasks/` or `.ai/archive/`.
- Keep at most one active task pointer.
- Update this pointer before switching the primary objective, branch, or scope.
- Verify Git root, branch, and worktree; runtime and Git state override a stale
  pointer.
- Canonical docs and runtime source remain authoritative; this file stores
  continuity state only.
