# Coding Rules

- Prefer readable, focused changes over clever abstractions.
- Inspect existing source and reuse established patterns before adding APIs.
- Respect module ownership and inward dependency direction.
- Keep business logic in shared KMP modules when platform-independent.
- Presentation must not contain financial calculations or persistence access.
- Do not invent product behavior, contracts, or architecture.
- Preserve unrelated user changes and keep commits reviewable.
- Validate proportionally to the affected scope and report only checks run.
- Update canonical docs when product status, architecture, or engineering
  practice changes.

Canonical references:

- [Engineering Guidelines](../../docs/engineering-guidelines.md)
- [Definition Of Done](../../docs/definition-of-done.md)
- [Architecture](../../docs/architecture.md)
