# Task Archive

This directory stores completed task records only.

- Group archived tasks by completion year: `.ai/archive/<year>/`.
- Move a task here only after its work is complete and applicable merge
  synchronization has finished.
- Never point `.ai/current.md` at an archived task.
- Never preload or scan this directory during session startup.
- Use canonical docs and runtime source for current product status.

## Legacy Path Mapping

The pre-2026-07-30 AI context layout remains recoverable from Git history.
Historical validation reports may accurately mention these former paths:

- `.ai/project-context.md` -> volatile state now belongs in `.ai/current.md`
  and its active task.
- `.ai/architecture-summary.md` -> `.ai/core/architecture.md`.
- `.ai/coding-rules.md` -> `.ai/core/coding-rules.md`.
- `.ai/financial-rules.md` -> `.ai/core/financial-rules.md`.
- `.ai/ui-rules.md` -> `.ai/core/ui-rules.md`.
- `.ai/workflow.md` -> `.ai/core/workflow-rules.md` plus the active task.
- `.ai/instructions.md.md` -> retired; `AGENTS.md` and canonical docs govern.
