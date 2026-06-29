# ADR-0014: Smart Automation Philosophy

## Status

Accepted

## Context

Tio Ledger can improve speed and quality through automation such as SMS parsing, category suggestion, merchant detection, loan suggestions, and duplicate detection. Financial data is sensitive, and incorrect silent changes would damage user trust.

## Decision

Automation should assist users, never replace user decisions.

Rules:

- Never silently modify financial data.
- Every automation must be reviewable.
- Every suggestion can be edited.
- User confirmation is required before committing changes.

This applies to SMS parsing, category suggestion, merchant detection, loan suggestions, duplicate detection, and future automation.

## Consequences

Positive:

- Preserves user control.
- Reduces accidental data corruption.
- Keeps automation aligned with accounting-first workflows.
- Provides a consistent product rule for future intelligent features.

Negative:

- Some workflows require an extra confirmation step.
- Review UI must be efficient enough that assistance still feels fast.
- Automation APIs need suggestion and confirmation states, not direct mutation.
