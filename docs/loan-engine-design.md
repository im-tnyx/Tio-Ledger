# Loan Engine Design

## Purpose

The Loan Engine is a shared Kotlin module responsible for deterministic loan and EMI calculations across Android, iOS, and Wear OS. It must be pure, testable, platform-independent, and isolated from persistence and UI concerns.

## Supported Capabilities

- EMI calculation.
- Amortization schedule generation.
- Principal versus interest split.
- Outstanding balance tracking.
- Prepayment application.
- Interest savings calculation.
- Tenure reduction simulation.
- Revised schedule generation.
- Original versus revised schedule comparison.

## Core Concepts

### Loan Terms

Input model representing the contractual loan:

- Principal.
- Annual interest rate.
- Tenure.
- Start date.
- Payment frequency.
- EMI due day or schedule rule.
- Compounding or rate conversion policy.
- Rounding policy.

### EMI

The fixed periodic payment amount calculated from principal, periodic interest rate, and number of periods.

Formula:

```text
EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
```

Where:

- `P` is principal.
- `r` is periodic interest rate.
- `n` is total number of payment periods.

If `r` is zero, EMI becomes `P / n`.

### Amortization Entry

One row in the repayment schedule:

- Period number.
- Due date.
- Opening balance.
- EMI amount.
- Interest component.
- Principal component.
- Prepayment amount.
- Closing balance.
- Cumulative interest paid.
- Cumulative principal paid.
- Flags for final payment or adjusted payment.

### Prepayment

A payment above the scheduled EMI that reduces outstanding principal. Prepayments can be modeled as events:

- Date.
- Amount.
- Application policy.
- Fee, optional future extension.

Supported policies:

- Tenure reduction: EMI stays the same and loan ends earlier.
- EMI reduction: tenure stays similar and future EMI decreases.
- Custom recast: future schedule is recalculated from a specified point.

Initial implementation should prioritize tenure reduction because it is central to interest savings and common in EMI products.

## Calculation Flow

```text
LoanTerms
  -> Validate inputs
  -> Calculate original EMI
  -> Generate baseline schedule
  -> Apply payment events and prepayments
  -> Generate revised schedule
  -> Compare baseline and revised schedule
  -> Produce summary
```

## Public API Shape

The module should expose a narrow service-style API:

```kotlin
interface LoanCalculator {
    fun calculateEmi(terms: LoanTerms): EmiQuote
    fun generateSchedule(request: ScheduleRequest): AmortizationSchedule
    fun simulatePrepayment(request: PrepaymentSimulationRequest): PrepaymentSimulationResult
}
```

The implementation should be stateless.

## Precision And Rounding

Rules:

- Persist money as integer minor units.
- Avoid `Double` for money.
- Interest rate calculations may use a fixed-scale decimal implementation or carefully isolated decimal math.
- Rounding policy must be explicit and tested.
- The final payment should adjust to close the remaining balance cleanly.

Rounding policies to support:

- Round half up.
- Banker's rounding if required later.
- Currency minor-unit rounding.

## Validation Rules

Reject or return typed errors for:

- Principal less than or equal to zero.
- Negative interest rate.
- Tenure less than one period.
- Unsupported payment frequency.
- Prepayment amount less than or equal to zero.
- Prepayment date before loan start.
- Prepayment after loan closure.

## Schedule Behavior

For each period:

1. Compute interest from opening balance.
2. Compute scheduled principal as EMI minus interest.
3. Apply prepayment according to policy.
4. Compute closing balance.
5. Stop when closing balance reaches zero.
6. Adjust final EMI if remaining balance is below scheduled EMI.

## Interest Savings

Interest savings are calculated as:

```text
baseline total interest - revised total interest
```

The result should include:

- Absolute money saved.
- Number of periods reduced.
- Original closure date.
- Revised closure date.
- Total interest before and after.

## Test Matrix

Minimum required tests:

- Zero-interest loans.
- Standard monthly EMI.
- High interest rate.
- Short tenure.
- Long tenure.
- Final payment adjustment.
- Single prepayment.
- Multiple prepayments.
- Prepayment in first period.
- Prepayment near closure.
- Tenure reduction comparison.
- EMI reduction comparison, when implemented.
- Rounding edge cases.
- Invalid inputs.

## Extension Points

Future capabilities:

- Floating interest rates.
- Step-up or step-down EMI.
- Moratorium periods.
- Processing fees.
- Partially paid EMI tracking.
- Late fees.
- Balloon payments.
- Multiple disbursements.
- Regional day-count conventions.
