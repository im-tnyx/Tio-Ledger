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

## Implementation Ownership And Review Roles

- **Single Implementation Owner:** Each bounded slice or task has exactly one
  active implementation owner (human or agent) authorized to edit files.
- **Independent Review Boundary:** Other agents may perform read-only audits or
  independent reviews at any time, but must not silently modify implementation
  files or claim implementation ownership without explicit authorization.
- **Conflicting Activity:** If another agent claims or exhibits active
  conflicting implementation work on the same scope, do not overwrite or race.
  Stop, verify actual Git and tracker state, and resolve ownership explicitly.
- **No Implicit Transfer:** Tool recommendations, review feedback, bot
  suggestions, or issue assignments do not transfer implementation ownership.
  Ownership transfer requires explicit owner authorization.

## Review Policy And Deferral

### Default Review Policy

Independent review is required by default before final merge readiness.

- **Role Distinction:** Implementation Owner and Independent Reviewer must be
  clearly distinguishable.
- **Self-Review:** Self-review does not count as independent review.
- **Passing CI:** Green CI or passing automated checks do not substitute for
  independent review.
- **Automated Tool Reviews:** Automated or bot assessments are non-binding
  unless an applicable policy explicitly recognizes their evidence.
- **AI Agent Reviewers:** An AI agent may perform independent review when
  genuinely separate from implementation authorship.
- **No Inferred Approvals:** A formal GitHub `APPROVED` review must never be
  invented, assumed, or inferred from a chat or tool audit.
- **No Agent Waiving:** No agent may waive or dismiss review requirements on
  its own authority.

### Owner-Controlled Review Deferral

The repository owner may explicitly authorize a specific task or PR to proceed
while independent review remains pending.

- **Explicit Authorization:** Deferral requires specific, owner-given
  authorization identifying the exact task, PR, or commit scope.
- **Scope-Specific:** An exception applies only to the authorized scope; it
  never silently extends to other tasks, PRs, or future commits.
- **No Automatic Deferral:** A reviewer being unavailable, delayed, or quota-
  exhausted does not constitute authorization for deferral.
- **Documentation Requirements:** For any deferred review, record:
  1. Specific owner authorization reference.
  2. Targeted task or PR and relevant HEAD SHA.
  3. Deferral reason and accepted risks.
  4. Follow-up review owner and next review checkpoint or deadline.
- **Manual Owner Review:** The owner may perform a manual review in lieu of or
  subsequent to deferral. Manual owner review must be identified factually and
  must not be mislabeled as independent review when the owner authored the
  changes.
- **Separation From Merge Authorization:** Review deferral does not grant
  blanket permission to commit, push, create PRs, or merge. Merge authorization
  remains a distinct gate requiring separate, explicit owner authorization and
  full compliance with effective repository protection rules.

### Review Status Classifications

- `REVIEW_PENDING`: Independent review is required but not yet complete.
- `REVIEW_COMPLETE`: Independent review is complete with recorded findings and
  dispositions.
- `OWNER_DEFERRED_REVIEW`: The repository owner explicitly authorized
  continuation before independent review completion, with documented risks and
  follow-up checkpoints.

## Approval Gates

- **Authority Reference:** `AGENTS.md` defines the canonical authorization
  boundaries for file modifications, GitHub records, and restricted operations.
- **Explicit Owner Approval Required For:**
  - New independently scoped product, architecture, or workflow changes.
  - Changes altering established UI workflows or screen specifications.
  - Alterations to frozen financial, ledger, loan, or budget contracts.
  - Database schema, table structure, or SQLDelight migration changes.
  - Repository publication, branch pushes, PR creation, or PR merge decisions.
  - Destructive Git operations (force-push, history rewrite, branch deletion).
- **In-Scope Subtasks:** Ordinary implementation subtasks strictly within an
  already approved task scope do not require repeated gate approval unless they
  expand the approved scope or touch restricted operations.

## Safe Agent Handoffs

Before accepting or resuming a task handoff, verify the actual state directly:

1. **Repository and Branch:** Confirm working directory, remote URL, and active
   branch.
2. **Working Tree and Commit SHA:** Inspect `git status --short --branch`,
   current base SHA, and HEAD SHA.
3. **Approved Objective and Boundaries:** Verify the exact approved scope and
   identify any non-goals.
4. **Implementation Owner:** Identify who authored the work and who owns the
   next step.
5. **Changed Files:** Confirm exact modified files match authorized scope.
6. **Validation Evidence:** Verify which checks actually ran on the current HEAD
   and which were skipped or failed. Never accept a prior agent's summary or
   historical CI from a different HEAD as proof of current HEAD validity.
7. **Findings and Review Evidence:** Check for open findings, unresolved review
   comments, or remaining limitations.
8. **Next Authorized Action:** Ensure the next step is explicitly defined and
   authorized.

## Stale Task Evidence And Discrepancies

- **Authoritative Source:** Actual Git status, runtime source, and GitHub state
  always supersede stale `.ai/` notes or summaries.
- **Discrepancy Resolution:**
  - If `.ai/current.md` references an already merged PR or non-existent branch,
    reconcile against latest `main` before proceeding.
  - If task status disagrees with GitHub tracker evidence, verify tracker state
    first.
  - If a task is claimed "Complete" without publication or merge evidence,
    verify actual repository history.
- **No Silent Mutating:** Do not silently modify `.ai/current.md`, archive
  active tasks, or switch active task pointers without explicit owner
  authorization and handoff.

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
