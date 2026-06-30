# Accessibility Report

Status: Passed
Milestone: Accounts Screen v1
Date: 2026-06-30

## Screen Reader Support

- Summary row exposes a combined semantic label for assets, liabilities, and total.
- Account rows expose account name, type label, and displayed balance.
- Top app bar action icons include content descriptions.
- Loading, empty, and error states use readable text labels.

## Touch Targets

- Account rows use the existing `TioDimensions.accountRowHeight`.
- Bottom navigation uses the existing design-system navigation height.
- Search field and retry action use Material components with platform-standard interaction sizing.

## Dynamic Type And Layout

- Text uses Material theme typography instead of fixed viewport-scaled font sizing.
- Amounts are constrained to single-line ellipsis behavior where needed.
- List layout remains vertical and scrollable for small screens.

## Theme Support

- Light theme preview exists.
- Dark theme preview exists.
- Runtime screen uses the existing `TioLedgerTheme` color scheme.

## Responsive Support

- Phone layout is the primary supported layout for Accounts Screen v1.
- Tablet and foldable compatibility is preserved through fill-width rows and scrollable content.
- Wear app continues to compile against the shared shell; deeper Wear-specific Accounts UX remains a later milestone.

## Result

No accessibility blocker was identified for Accounts Screen v1.
