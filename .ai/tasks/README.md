# Task Records

Task files preserve only the context needed to resume one objective.

- Group tasks by scope, for example `android`, `ios`, `wear`, `shared`, or
  `docs`.
- Create scope folders only when needed.
- Keep at most one task referenced by `.ai/current.md`.
- Do not scan this directory during normal session startup.
- Move completed tasks to `.ai/archive/<year>/` after applicable merge
  synchronization.

## Template

```markdown
# <Task Name>

Status: In Progress
Objective: <one concrete outcome>
Implementation Owner: <human or agent identity>
Branch: `<branch>`
Base SHA: `<commit SHA>`
Inspected Head SHA: `<exact commit SHA inspected>`
Scope: `<repository area>`
Created: `YYYY-MM-DD`
Last Updated: `YYYY-MM-DD`

## Required Context

- `.ai/core/<relevant-file>.md`
- `<canonical doc or source path>`

## Approved Scope And Constraints

- Scope: <explicitly approved boundary>
- Non-goals: <explicit non-goals>
- <important truth boundary>

## Decisions

- <durable decision and reason>

## Progress

- [ ] <next verifiable unit>

## Validation Evidence

- Checks executed: `<list actual commands run and exit results>`
- Checks skipped / unavailable: `<list and reasons>`

## Changed Files

- None yet.

## Review Evidence And Findings

- Review Status: `[ ] REVIEW_PENDING  [ ] REVIEW_COMPLETE  [ ] OWNER_DEFERRED_REVIEW`
- Independent Reviewer: `<reviewer identity or None yet>`
- Review classification: `[ ] Independent review  [ ] Self-review  [ ] Tool/bot assessment`
- Review decision: `[ ] Approved  [ ] Changes requested  [ ] Comments / Non-binding`
- Owner Deferral Authorization: `<owner authorization reference, or None>`
- Deferral Reason & Accepted Risks: `<reason and residual risks, or None>`
- Follow-up Review Owner & Checkpoint: `<follow-up reviewer and next checkpoint, or None>`
- Outstanding findings: `<open findings or None>`
- Remaining risks: `<residual risks or None>`

## Next Action And Handoff

- Owner: <who owns the next action>
- Next Action: <single concrete continuation step>
```
