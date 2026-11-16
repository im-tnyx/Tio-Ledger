# Active Task

Status: In Review
Active Task: none
Branch: `feat/ios-github-debug`
Platform Scope: `Android debug APK and iOS simulator/iPhone sideload`
PR: `#101 <https://github.com/im-tnyx/Tio-Ledger/pull/101>`
Base SHA: `6642d905430000397046fd41cbd40cf88440678c` (PR base after #100 merge)
Current HEAD: resolve from GitHub PR #101; do not cache a mutable head SHA here.
Last Updated: `2026-11-16`
Next Action: `Verify exact-head KMP/iOS checks, downloaded simulator archive installation, and independent review before merge. Physical iPhone installation remains user-side.`

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
