# AI Context System

This directory preserves the minimum durable context needed to resume work
after conversation compaction. It is an operational index, not a replacement
for runtime source, repository instructions, canonical documentation, ADRs, or
Git history.

## Session Loading

1. Read repository `AGENTS.md`.
2. Read [current.md](current.md).
3. When `Active Task` is not `none`, read only the referenced task file.
4. Read only the `.ai/core/` files named under that task's `Required Context`.
5. Inspect canonical docs and runtime source required by the requested change.

Do not scan or preload `.ai/tasks/` or `.ai/archive/`.

## Directory Map

- `current.md`: the single session pointer and current branch/scope state.
- `core/`: stable, concise summaries loaded only when relevant.
- `tasks/`: active or paused task records grouped by scope.
- `archive/`: completed task records grouped by year; never session-preloaded.

Stable summaries:

- [Architecture](core/architecture.md)
- [Coding Rules](core/coding-rules.md)
- [Financial Rules](core/financial-rules.md)
- [UI Rules](core/ui-rules.md)
- [Workflow Rules](core/workflow-rules.md)

Lifecycle details and the task template live in
[tasks/README.md](tasks/README.md). Archive policy and legacy-path mapping live
in [archive/README.md](archive/README.md).

## Truth Boundary

- Runtime source and configuration define actual behavior.
- `README.md`, `docs/`, and accepted ADRs define current product and
  architecture intent.
- `AGENTS.md` defines repository operating instructions.
- `.ai/current.md` and task files record continuity only.
- If `.ai` content conflicts with runtime or canonical docs, update or retire
  the stale `.ai` content; do not duplicate a new source of truth.

## Maintenance Rules

- Keep `current.md` limited to pointer metadata, loading rules, and one next
  action.
- Keep an active task near 150 lines or fewer; summarize completed detail
  before it grows into a transcript.
- Keep core files stable, concise, and linked to canonical sources.
- Archive completed tasks only after applicable merge synchronization.
- Never copy full canonical documents or command logs into `.ai`.
