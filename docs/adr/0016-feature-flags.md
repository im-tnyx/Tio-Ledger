# ADR-0016: Feature Flags

## Status

Accepted

## Context

The product roadmap includes experimental capabilities that could affect sensitive financial workflows. These features should be introduced gradually without destabilizing the core accounting experience.

## Decision

Experimental capabilities must be isolated behind feature flags.

Examples:

- AI Insights.
- OCR Receipt Scanner.
- SMS Parser.
- Cloud Sync.
- Family Accounts.
- Investment Tracking.
- Wear Tiles.
- Predictive Budgeting.

Feature flags should default to conservative production behavior. Core ledger, transaction entry, balances, and loan calculations must remain stable regardless of experimental feature rollout.

## Consequences

Positive:

- Enables gradual rollout.
- Reduces risk to core accounting workflows.
- Allows platform-specific experimentation.
- Makes incomplete capabilities easier to isolate.

Negative:

- Requires disciplined flag lifecycle management.
- Test matrix grows with important flag combinations.
- Dead flags must be removed after rollout decisions.
