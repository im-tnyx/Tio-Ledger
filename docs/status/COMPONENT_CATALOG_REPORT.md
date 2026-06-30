# Component Catalog Report

Date: 2026-06-30
Milestone: UI Foundation v1

## Design Tokens

| Token Group | Location |
| --- | --- |
| Colors | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioColors.kt` |
| Typography and Material theme | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioTheme.kt` |
| Shapes | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioTheme.kt` |
| Spacing | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioDimensions.kt` |
| Elevation | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioDimensions.kt` |
| Corner radius | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioDimensions.kt` |
| Icons | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioIconToken.kt` |
| Motion durations | `shared/ui/src/commonMain/kotlin/com/tioledger/ui/design/TioMotionDurations.kt` |

## Stateless Components

| Component | Purpose |
| --- | --- |
| `TioAppBar` | Shared top app bar |
| `TioBottomNavigation` | Shared bottom navigation |
| `TioNavigationRail` | Shared tablet/expanded navigation rail |
| `TioFloatingActionButton` | Primary floating action |
| `TioPrimaryButton` | Filled primary action button |
| `TioSecondaryButton` | Outlined secondary action button |
| `TioCard` | Standard card container |
| `TioListItem` | Reusable list row |
| `TioTransactionRow` | Transaction-style row |
| `TioAccountRow` | Account-style row |
| `TioAccountCard` | Account summary card |
| `TioCategoryRow` | Category-style row |
| `TioCategoryChip` | Category chip |
| `TioAmountText` | Signed/neutral amount display |
| `TioCurrencyText` | Currency text label |
| `TioCurrencyBadge` | Currency code badge |
| `TioSummaryCard` | Metric/amount summary card |
| `TioSearchField` | Search/filter text field |
| `TioSearchBar` | Search bar API wrapper |
| `TioEmptyState` | Empty list/content state |
| `TioLoadingState` | Loading state |
| `TioErrorState` | Error state with optional retry |
| `TioSectionHeader` | Section heading row |
| `TioFilterChip` | Filter selection chip |
| `TioBadge` | Small status/count badge |
| `TioDialog` | Confirmation/information dialog shell |
| `TioBottomSheetContent` | Bottom sheet content container |
| `TioIcon` and `TioIconAvatar` | Token-driven icon presentation |

## Screen Templates

| Template | Purpose |
| --- | --- |
| `TioListScreenTemplate` | List screen scaffold with search, empty state, and add action |
| `TioDetailScreenTemplate` | Detail scaffold with summary and row list |
| `TioFormScreenTemplate` | Form scaffold with fields and actions |
| `TioDashboardTemplate` | Dashboard-style scaffold with navigation placeholders |

## Preview Coverage

| Preview | Coverage |
| --- | --- |
| `ComponentCatalogLightPreview` | Component catalog in light theme |
| `ComponentCatalogDarkPreview` | Component catalog in dark theme |
| `ComponentCatalogCompactPreview` | Compact component catalog |
| `ComponentCatalogTabletPreview` | Tablet/expanded component catalog with navigation rail |
| `ListTemplatePreview` | List template |
| `DetailTemplatePreview` | Detail template |
| `FormTemplatePreview` | Form template |
| `DashboardTemplatePreview` | Dashboard template |
| `DialogComponentPreview` | Dialog shell |

## Notes

The catalog contains placeholder display data only. It does not execute ViewModels, repositories, use cases, SQL, ledger access, financial calculations, or business workflows.
