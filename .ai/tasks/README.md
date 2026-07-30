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
Branch: `<branch>`
Scope: `<repository area>`
Created: `YYYY-MM-DD`
Last Updated: `YYYY-MM-DD`

## Required Context

- `.ai/core/<relevant-file>.md`
- `<canonical doc or source path>`

## Constraints

- <important truth boundary>

## Decisions

- <durable decision and reason>

## Progress

- [ ] <next verifiable unit>

## Validation

- Not run yet.

## Changed Files

- None yet.

## Next Action

<single concrete continuation step>
```
