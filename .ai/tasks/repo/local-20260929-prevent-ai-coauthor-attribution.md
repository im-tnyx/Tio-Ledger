# Prevent AI Co-Author Attribution

Status: In Progress
Objective: Stop AI-assisted workflows from adding AI provider/model co-author attribution to commits, squash merges, and PR descriptions (issue #66).
Branch: `chore/prevent-ai-coauthor-attribution`
Scope: repository governance only (`AGENTS.md`, `.ai` continuity); no product, runtime, financial, CI, or Git identity change
Created: `2026-09-29`
Last Updated: `2026-09-29`
Issue: `#66`

## Required Context

- `.ai/core/workflow-rules.md`
- `AGENTS.md` (Git And Push Workflow, Commit Attribution)
- `.github/PUSH_TEMPLATE.md`

## Constraints

- No published-history rewrite; historical trailers stay unless separately approved.
- Human Git author/committer identity unchanged.
- Do not merge the #66 PR without explicit authorization.

## Findings

- Repository-controlled sources: none. No commit template, hooks, `core.hooksPath`, AI tool config (`CLAUDE.md`, `.claude/`, Copilot/Codex files), or doc requiring AI co-authors.
- Local agent behavior: the AI coding agent's default commit guidance appended `Co-Authored-By: Claude …` (`8a9c029`, and branch commits of #62/#63/#65/#67).
- GitHub squash behavior: `gh pr merge --squash` without an explicit body copies branch-commit trailers (`991159d` #63). Explicit bodies that included the trailer caused `b37376b` (#62) and `7d5cbf0` (#65). An explicit clean body produced `7434744` (#67) with no AI trailer.
- Historical, out of scope: `25ea2ff` (#1) carries a GitHub Copilot co-author. GitHub also adds a same-person `Co-authored-by: Santosh Jangid <im-tnyx@users.noreply.github.com>` when the local commit email differs from the account's primary noreply address; this is human identity, not AI, and is not changed here.

## Decisions

- Single canonical rule in `AGENTS.md` (`## Commit Attribution`), the instruction surface every AI agent loads first; not duplicated elsewhere.

## Progress

- [x] Audit repository vs tool vs GitHub squash behavior.
- [x] Add the `AGENTS.md` rule.
- [ ] Governance commit verified on GitHub (no AI co-author).
- [ ] PR opened; merge awaits explicit authorization.

## Validation

- `git diff --check`.

## Changed Files

- `AGENTS.md`
- `.ai/current.md`, `.ai/tasks/repo/local-20260929-prevent-ai-coauthor-attribution.md`, `.ai/tasks/android/local-20260812-reminder-validation-followup.md`, `.ai/archive/2026/local-20260929-budget-reminder-timezone-identity.md`

## Next Action

Open the #66 PR, verify the branch commit attribution on GitHub, and wait for merge authorization.
