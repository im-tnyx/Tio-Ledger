# Main Currency Settings Specification

## Scope / Relationship To More Screen And Settings

- `docs/references/notes/settings-configuration.md` catalogs the high-level settings roadmap. This note defines the bounded functional specification for `MainRoute.CurrencySettings`.
- `MainRoute.CurrencySettings` is an additive settings workflow reachable from `MoreScreen` ("Main Currency Setting").
- In `MoreScreen`, the "Main Currency Setting" list item dynamically displays the currently persisted currency code and symbol in its subtitle (default: `INR(₹)`).
- This feature governs display and formatting preferences across the application. It does not alter stored monetary values in the database, nor does it weaken multi-currency validation or immutable ledger contracts.

## Reference Readiness

- **Primary Visual References:** Three approved product design mockups provided during the Task 1 / Task 1A session:
  1. Main Currency Setting Screen: TopBar with Back navigation, centered title, outlined "Change" action; selected currency label and live amount preview header; full-width edge-to-edge section dividers; "Unit position" selection row; "Decimal point" selection row; full-width primary "Save" button in coral accent (`#F25A4B`).
  2. Decimal Point Selection Modal Bottom Sheet: Options (`None`, `1.0`, `1.00`, `1.000`, `1.0000`, `1.00000`, `1.00000000`) with rounded surface container and separate "Cancel" button.
  3. Unit Position Selection Modal Bottom Sheet: Options (`Front`, `End`) with rounded surface container and separate "Cancel" button.
- **Surrounding UI Reference:** `shared/ui/src/commonMain/kotlin/com/tioledger/ui/more/MoreScreen.kt` and `RootNavigationHost.kt`.

## Approval Basis

Authorized by explicit product-owner instruction in Task 1 / Task 1A:
- Settings → Main Currency Setting establishes the default currency preference for the entire application.
- Selections edit an in-memory draft; only tapping "Save" atomically persists changes.
- Persisted configuration must survive application process restart.
- Back button, Cancel button, and outside dismissal discard all draft modifications without persistence.
- Supported initial currencies: INR, USD, EUR, GBP. Default configuration: `INR`, `Front`, `TWO` decimal places (`₹ 1.00`).

## Workflow Summary

1. User taps "Main Currency Setting" from `MoreScreen`.
2. The screen loads the active persisted configuration into an editable local draft:
   - Preview shows the draft currency, unit position, and decimal places formatted with sample amount `1.00`.
   - "Unit position" displays current draft value (`Front` or `End`).
   - "Decimal point" displays current draft value (`None`, `1.0`, `1.00`, etc.).
3. Tapping "Change" opens a modal selection sheet with supported currencies (`INR`, `USD`, `EUR`, `GBP`). Selecting an option updates draft currency and preview.
4. Tapping "Unit position" opens a modal bottom sheet with `Front` and `End`. Selecting an option updates draft position and preview.
5. Tapping "Decimal point" opens a modal bottom sheet with supported decimal formats. Selecting an option updates draft precision and preview.
6. Tapping "Cancel" or dismissing any sheet leaves the draft intact.
7. Tapping "Save" persists the draft atomically via `CurrencyPreferenceRepository`:
   - On success, the screen navigates back to `MoreScreen`, which immediately reflects the updated currency subtitle (e.g. `USD($)`).
   - On failure, an inline error is displayed and the draft remains editable without navigating away.
8. Tapping the Back icon or system back discards the draft without saving and navigates back to `MoreScreen`.

## Information Hierarchy & Layout

1. **TopBar:**
   - Navigation Icon: Private Canvas-drawn vector arrow (`CurrencyBackIcon`) with accessibility content description `"Navigate back"`.
   - Title: `"Main Currency Setting"`.
   - Action: Outlined rounded button labeled `"Change"`.
2. **Preview Header:**
   - Subtitle: Active currency display name and symbol (e.g. `INR - India (₹)`).
   - Amount Preview: Live preview formatted with selected draft parameters (e.g. `₹ 1.00` or `1.000 $`).
3. **Dividers:** Edge-to-edge `HorizontalDivider()` elements spanning full screen width without outer horizontal padding.
4. **Configuration Rows:**
   - "Unit position" row with full-width touch target and horizontal content padding.
   - "Decimal point" row with full-width touch target and horizontal content padding.
5. **Primary Action:**
   - Rounded button with coral background (`#F25A4B`), white text `"Save"`, and loading indicator during asynchronous persistence.
   - Centered error message below button on failure.

## Technical Architecture & Contracts

- **Domain Model:** `MainCurrencyConfig` (`SupportedCurrency`, `CurrencyUnitPosition`, `CurrencyDecimalDisplay`). Pure immutable value class with deterministic `formatPreview(Double)` method.
- **Repository Contract:** `CurrencyPreferenceRepository` exposing `getMainCurrencyConfig(): MainCurrencyConfig`, `observeMainCurrencyConfig(): Flow<MainCurrencyConfig>`, and `saveMainCurrencyConfig(config: MainCurrencyConfig)`.
- **Application Layer Use Cases:** `GetMainCurrencyConfigUseCase`, `ObserveMainCurrencyConfigUseCase`, `SaveMainCurrencyConfigUseCase`.
- **UI State Holder:** `CurrencyPreferenceStore` provides shared reactive `StateFlow<MainCurrencyConfig>` for `MoreScreen` and other consumer screens.
- **Persistence Implementations:**
  - Production (Android): `SharedPreferencesCurrencyPreferenceRepository` storing `currency_code`, `unit_position`, and `decimal_display` in private app preferences.
  - Fallback / Tests: `InMemoryCurrencyPreferenceRepository`.
- **Dependency Injection:** Koin modules in `shared:bootstrap` (`dataModule`, `applicationModule`), `shared:ui` (`tioUiModule`), and `apps:android` (`TioAndroidApplication`).
