# Active Task

Status: In Progress
Active Task: `.ai/tasks/android/local-20260812-reminder-validation-followup.md`
Branch: `docs/reminder-accessibility-api35-evidence`
Platform Scope: `android`
Last Updated: `2026-10-04`
Next Action: `Rebuild the debug APK from exact clean main@ee19eb0a75695dbb073fd9c53e46e59ae9a75f73 before any API35 installation, calculate its fresh hash, then install and hash-verify that rebuilt artifact only on Pixel_9_API35 after normal ADB authorization and a fresh target inventory. The ignored September 30 APK/hash is historical build evidence, not a repository-persisted binary. The October 3 due-day instant has passed without a recorded observation; do not boot protected API30 solely for evidence or trigger work manually. Keep #56/#43 open; TalkBack and rendered preference-error evidence remain pending.`

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
