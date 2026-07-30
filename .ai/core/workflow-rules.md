# Workflow Rules

## Start Or Resume

1. Read `AGENTS.md`.
2. Read `.ai/current.md`.
3. Read only its active task file, when present.
4. Read only the core and canonical files named by that task.
5. Verify Git root, branch, status, and relevant runtime source before editing.

## Task State

- Keep one primary objective and one active pointer.
- Store active and paused work under `.ai/tasks/<scope>/`.
- Use `local-YYYYMMDD-short-slug.md` for locally initiated tasks.
- Keep task notes concise: objective, constraints, decisions, progress,
  validation, changed files, and next action.
- Do not store chat transcripts, speculative plans, copied canonical docs, or
  exhaustive command output.
- Update the active task after a material decision or before ending a session.

Allowed task status values:

- `In Progress`
- `Blocked`
- `Ready for Review`
- `Complete`

## Switch Or Complete

- Before switching objectives, update the existing task and `.ai/current.md`.
- After completion and applicable merge synchronization, move the task to
  `.ai/archive/<year>/`, set `Status: Idle` and `Active Task: none` in
  `.ai/current.md`, and record the next objective only when it is explicitly
  started.
- Never point `.ai/current.md` at an archived task.
- Do not infer product status from archived tasks; use runtime and canonical
  docs.

## Git Boundary

- Use focused branches and preserve unrelated changes.
- Before commit, push, or PR work, follow `.github/PUSH_TEMPLATE.md`.
- After merge, follow `.github/POST_MERGE_SYNC.md` before starting a branch.
