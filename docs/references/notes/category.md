# Category Screen Reference Note And Management Gap

## Screen And Readiness

Tio Ledger `Categories` v1 exists on the primary bottom navigation. This note reconciles that runtime with the checked-in Money Manager category-management screenshots. The reference evidence is now available, but the expanded management workflow is **not yet approved for implementation**. [Issue #8](https://github.com/im-tnyx/Tio-Ledger/issues/8) covered v1 list/create; nested hierarchy and deletion of referenced categories were explicitly outside its scope.

## Primary References

- [Configuration entry points](<../Screenshot_20260630_164139_Money Manager.jpg>)
- [Income Category management](<../Screenshot_20260630_164241_Money Manager.jpg>)
- [Expense Category management](<../Screenshot_20260630_164246_Money Manager.jpg>)

## Supporting References

- [Product Requirements: Categories](../../product-requirements.md) requires customizable defaults, useful parent-child hierarchy, category budgets, and analytics.
- [Budget Setting screenshot](<../Screenshot_20260630_164302_Money Manager.jpg>) shows the relationship between category labels and budget configuration, without specifying Tio behavior.
- Existing `CategoriesScreen`, `CategoriesViewModel`, and `CategoryUseCases` are the current runtime contracts. No JADX material was needed for this note.

## Observed Reference Workflow And Hierarchy

The reference reaches category management through Settings/Configuration, with separate Income and Expense entries. Each category-management screen places add at the top, shows a subcategory control, then a list of category rows. Rows expose visible edit, remove, and ordering affordances; some expense rows summarize children. A screenshot alone does not establish whether remove deletes, archives, hides, or reparents a category, nor how ordering and the subcategory control persist.

## Tio Ledger Runtime And Intentional Deviations

`CategoriesScreen` lists active Income/Expense groups, handles loading/empty/error/retry, and opens an Add dialog for name and type. `CategoriesViewModel` uses list/create use cases. Application also has update/archive use cases, but the production Categories UI does not expose editing, archiving, ordering, or parent selection. The existing category model has a `parentId`; that is not evidence that a complete hierarchy workflow is implemented. A production default-category seed was not established by this audit; verify bootstrap behavior before promising defaults.

Tio's primary Categories destination differs from the reference Settings/Configuration entry. Preserve the approved five-destination navigation unless the owner explicitly approves a change. A future Settings shortcut may point to the existing category-management surface, but must not create a second category source of truth. The v1 Add dialog and grouped list remain in place until a separately reviewed change supersedes them.

## Decisions Needed For A Management Specification

1. Entry and ownership: retain `MainRoute.Categories` as the owner; decide whether Settings links to it or a narrower editor sub-route. Do not silently duplicate the screen.
2. Editing: define allowed name/type/parent changes and duplicate-name scope, including what happens to historical transactions and existing budget associations.
3. Removal: decide archive/hide versus deletion, default-category protection, and behavior for categories referenced by immutable history. Destructive deletion is not implied by the red reference icon.
4. Hierarchy: define depth, parent/child type rules, cycle prevention, and how budgets/analytics aggregate parent and child amounts. A toggle in the reference is not sufficient to choose these rules.
5. Ordering and defaults: decide whether manual reorder and seeded defaults are product requirements for the next slice, with deterministic persistence and migration implications reviewed first.

## Future Functional And Navigation Checklist

- [ ] Owner approves one bounded management slice and an updated Tio UI specification.
- [ ] A single route/source of truth handles category list and any Settings entry.
- [ ] Update/archive operations preserve transaction and ledger history; no silent financial mutation.
- [ ] Parent/child rules, duplicate validation, defaults, and ordering are explicitly specified before UI work.
- [ ] Income and Expense remain distinguishable by text and screen-reader labels, not color alone.
- [ ] Add/edit/archive failures keep persisted state truthful and offer recovery.
- [ ] Existing transaction entry and budget category selection remain compatible.
- [ ] Light/dark, large text, touch targets, focus order, and populated/empty/error states are reviewed.

## Approval And Proprietary Boundary

This is an evidence and planning correction, not retroactive v1 UI approval or authorization for category-management code. Future intentional deviations require owner approval under [ADR-0015](../../adr/0015-reference-driven-ui-development.md). No reference code, XML, resources, strings, icons, colors, dimensions, animations, or assets are copied into Tio Ledger.
