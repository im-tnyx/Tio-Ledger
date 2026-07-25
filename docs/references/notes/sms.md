# SMS-Assisted Transaction Review Reference Note

## Screen Name

SMS-Assisted Transaction Review

## Primary Reference

- Checked-in approved screenshot: unavailable at milestone start.
- Approved fallback: issue #15 acceptance criteria, ADR-0011, ADR-0016, `docs/sms-assisted-transaction-capture.md`, and established Tio Ledger Transaction Entry production patterns.

## Supporting References

- Existing `TransactionEntryScreen`, `TransactionEntryViewModel`, account/category pickers, validation states, date selection, and save feedback.
- Existing Tio design-system components and navigation conventions.
- Approved Tio Ledger mockups, when later supplied.
- `docs/references/realbyteapps/` only for workflow, hierarchy, terminology, grouping, dialog flow, screen relationships, feature discovery, or interaction-pattern analysis where higher-priority sources are insufficient.

## Workflow Summary

```text
provided message text
    -> deterministic parse
    -> ignored / unsupported / suggestion result
    -> editable transaction review
    -> explicit Save or Reject
    -> normal ledger-backed transaction recording
```

The workflow must never save automatically. The user reviews all detected fields, supplies missing required data, may edit any pre-filled value, and explicitly selects Save. Reject closes the suggestion without creating a transaction.

## Information Hierarchy

1. Screen title and privacy/assistance context.
2. Parse status and explainable confidence.
3. Warning or missing-field message where required.
4. Editable transaction type/direction.
5. Editable amount and currency.
6. Account selection.
7. Category selection for income/expense.
8. Destination account for transfer candidates.
9. Merchant/counterparty or note.
10. Transaction date/time.
11. Detected payment rail and compact evidence summary.
12. Primary Save action and secondary Reject action.

## Required UI States

- feature disabled
- parsing
- ignored/non-transaction
- unsupported/insufficient
- low-confidence suggestion
- medium/high-confidence suggestion
- reference-data loading
- editable review
- validation error
- persistence error
- saving
- save success
- rejected/dismissed

## Navigation Definition

- The shared v1 review screen accepts platform-provided message text or an already prepared candidate.
- The review screen is not an automatic background destination.
- Save success returns to the normal transaction workflow/history according to the host callback.
- Reject returns to the calling surface without persistence.
- Android SMS permission and ingestion navigation are separate platform milestones.
- iOS paste/share/import entry points are separate platform milestones.

## Interaction Rules

- All pre-filled fields remain editable.
- Low confidence must name missing or uncertain fields instead of silently selecting defaults.
- Save remains disabled or validation-blocked until the normal transaction requirements are satisfied.
- Reject must be available without saving.
- Raw SMS text is not displayed unnecessarily and is not persisted by default.
- Parse evidence should be concise, explainable, and free of sensitive raw-message duplication.
- Failed, declined, OTP, promotional, and balance-only messages must not appear as normal transaction suggestions.

## Intentional Deviations

- No production SMS permission request or inbox/receiver UI in this milestone.
- No auto-save, one-tap silent confirmation, or background posting.
- No proprietary Money Manager screen/resource reproduction.
- No raw-message history screen.
- No ML confidence meter or opaque score.
- Confidence is expressed as understandable levels and missing-field guidance.

## Reason For Deviations

- Privacy and user control require explicit review and confirmation.
- Platform SMS capabilities and store policies differ by platform.
- The first milestone establishes portable parser/review foundations before platform ingestion.
- Existing Transaction Entry patterns reduce workflow inconsistency and preserve ledger-backed validation.
- Deterministic evidence is more auditable than opaque model output for financial data.

## Accessibility Considerations

- Confidence must be communicated with text, not color alone.
- Missing/uncertain fields need explicit labels and actionable guidance.
- Editable controls require stable labels and logical focus order.
- Save and Reject must have distinct accessible names.
- Error messages must identify whether the issue is parse, validation, reference-data, or persistence related.
- Light/dark themes, dynamic text, keyboard navigation, and responsive layouts must follow the Tio design system.
- Evidence rows must remain readable without exposing full sensitive message text.

## Acceptance Checklist

- [ ] Feature-disabled state hides or blocks the experimental review entry point.
- [ ] Positive fixture families produce deterministic suggestions.
- [ ] OTP, promotion, failed/declined, and balance-only messages do not become transaction suggestions.
- [ ] Confidence and missing fields are understandable without inspecting raw parser internals.
- [ ] Amount, type, account, category/destination, note, and date remain editable.
- [ ] Save requires explicit user action and normal Application validation.
- [ ] Reject creates no ledger transaction.
- [ ] Raw message text is not persisted by default.
- [ ] Transaction persistence uses existing ledger-backed use cases.
- [ ] Loading, ignored, unsupported, review, error, saving, success, and rejection states are covered.
- [ ] Previews/tests cover light/dark and representative confidence states.

## Pixel Review Plan

Because no approved screenshot exists, initial review compares the implementation against documented hierarchy, established Transaction Entry spacing/components, Tio typography, accessibility, and responsive behavior. A later approved screenshot may refine visual details without changing the confirmed privacy and explicit-confirmation workflow.

## JADX Boundary

No copied Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, assets, or proprietary implementation details are allowed in this note or implementation.
