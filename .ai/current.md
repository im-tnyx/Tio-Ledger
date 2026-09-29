# Active Task

Status: In Progress
Active Task: `.ai/tasks/repo/local-20260929-prevent-ai-coauthor-attribution.md`
Branch: `chore/prevent-ai-coauthor-attribution`
Platform Scope: `repo`
Last Updated: `2026-09-29`
Next Action: `Open and verify the #66 governance PR (AGENTS.md commit-attribution rule, no AI co-author on the branch commit); wait for merge authorization. #56 stays paused (EMI blocked by #61).`

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
