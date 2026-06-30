# Architecture Changelog

This changelog records architectural decisions that affect project structure, data flow, module boundaries, or engineering practices.

Do not use this file for feature changes or bug fixes.

## 2026-06-30 - SQLDelight Data Layer Implementation & Freeze

- Implemented SQLDelight repositories (`AccountRepository`, `CategoryRepository`, `LedgerRepository`, `TransactionRepository`) in `shared:data` satisfying the Domain Layer repository contracts.
- Isolated all virtual system account checks from mappers into `SystemAccountResolver`.
- Split SQLDelight named query definitions from `TioLedgerDatabase.sq` into separate files: `accounts.sq`, `categories.sq`, `transactions.sq`, and `ledger.sq` under `com.tioledger.database.query`.
- Introduced typed `DataResult` and `DataError` inside the Data Layer to handle persistence outcomes, completely decoupling domain `LedgerError` models from SQLDelight concepts.
- Created `MapperTest.kt` for pure mapping unit validations, and `DataLayerTest.kt` for transaction rollback, constraint check, and duplicate protection tests.
- Marked SQLDelight Data Layer as Frozen v1.

## 2026-06-30 - Application Layer And Repository Contracts

- Added `shared:application` as the pure Kotlin use case orchestration module.
- Kept repository contracts interface-only under `shared:domain`.
- Added lightweight domain events for account, category, and transaction operations.
- Routed transaction record use cases through the Frozen v1 Ledger Engine without changing the engine.
- Confirmed SQLDelight repository implementations remain deferred to the next milestone.

## 2026-06-30 - Ledger Engine Frozen v1

- Removed JVM-only arithmetic APIs from `shared:core` common code.
- Added pure Kotlin overflow-safe money arithmetic.
- Replaced unsafe posting strategy casts with generic type-safe strategies.
- Moved posting strategy resolution into `PostingStrategyRegistry`.
- Marked Ledger Engine Frozen v1 after financial correctness, architecture, build, ktlint, detekt, unit, and property-style validation passed.
- Prepared next milestone: Use Cases & Repository Interfaces.

## v1.0.0

- Established Kotlin Multiplatform monorepo.
- Adopted Clean Architecture and MVVM.
- Introduced Ledger First accounting model.
- Added deterministic financial calculation rules.
- Introduced Reference-Driven UI policy.
- Added Human-Confirmed SMS Assisted Transaction Capture.
- Introduced Feature Flag architecture.

## Future Entries

Record only architectural decisions that affect project structure, data flow, module boundaries, or engineering practices.

Do not use this file for feature changes or bug fixes.
