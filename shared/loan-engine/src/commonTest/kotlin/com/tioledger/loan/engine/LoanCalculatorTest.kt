package com.tioledger.loan.engine

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoanCalculatorTest {
    private val usd = CurrencyCode("USD")
    private val calculator: LoanCalculator = MonthlyReducingBalanceLoanCalculator()

    @Test
    fun representativeRateLoanMatchesGoldenSchedule() {
        val quote =
            successfulQuote(
                calculator.calculate(
                    terms(
                        principal = 10_000_000L,
                        // Regression fixture only; production rates are supplied through LoanTerms.
                        annualRateBasisPoints = 875,
                        tenureMonths = 60,
                        startDate = LocalDate(2026, 7, 19),
                    ),
                ),
            )

        assertEquals(Money(206_373L, usd), quote.emi)
        assertEquals(Money(2_382_331L, usd), quote.totalInterest)
        assertEquals(Money(12_382_331L, usd), quote.totalPayable)
        assertEquals(60, quote.schedule.size)

        val first = quote.schedule.first()
        assertEquals(LocalDate(2026, 8, 19), first.dueDate)
        assertEquals(Money(10_000_000L, usd), first.openingBalance)
        assertEquals(Money(206_373L, usd), first.payment)
        assertEquals(Money(133_456L, usd), first.principalComponent)
        assertEquals(Money(72_917L, usd), first.interestComponent)
        assertEquals(Money(9_866_544L, usd), first.closingBalance)

        val final = quote.schedule.last()
        assertEquals(LocalDate(2031, 7, 19), final.dueDate)
        assertEquals(Money(206_324L, usd), final.payment)
        assertEquals(Money(204_830L, usd), final.principalComponent)
        assertEquals(Money(1_494L, usd), final.interestComponent)
        assertEquals(Money.zero(usd), final.closingBalance)
        assertTrue(final.isFinal)

        assertScheduleInvariants(quote)
    }

    @Test
    fun zeroInterestLoanUsesExactPrincipalSplit() {
        val quote =
            successfulQuote(
                calculator.calculate(
                    terms(
                        principal = 120_000L,
                        annualRateBasisPoints = 0,
                        tenureMonths = 12,
                    ),
                ),
            )

        assertEquals(Money(10_000L, usd), quote.emi)
        assertEquals(Money.zero(usd), quote.totalInterest)
        assertEquals(Money(120_000L, usd), quote.totalPayable)
        assertEquals(12, quote.schedule.size)
        assertTrue(quote.schedule.all { it.interestComponent.isZero() })
        assertScheduleInvariants(quote)
    }

    @Test
    fun dueDatesClampToCalendarMonthEndAndFinalPaymentAdjusts() {
        val quote =
            successfulQuote(
                calculator.calculate(
                    terms(
                        principal = 100_000L,
                        annualRateBasisPoints = 1_000,
                        tenureMonths = 3,
                        startDate = LocalDate(2024, 1, 31),
                    ),
                ),
            )

        assertEquals(Money(33_891L, usd), quote.emi)
        assertEquals(
            listOf(
                LocalDate(2024, 2, 29),
                LocalDate(2024, 3, 31),
                LocalDate(2024, 4, 30),
            ),
            quote.schedule.map(AmortizationInstallment::dueDate),
        )
        assertEquals(Money(33_889L, usd), quote.schedule.last().payment)
        assertEquals(Money.zero(usd), quote.schedule.last().closingBalance)
        assertScheduleInvariants(quote)
    }

    @Test
    fun monthlyInterestUsesExplicitHalfUpRounding() {
        val quote =
            successfulQuote(
                calculator.calculate(
                    terms(
                        principal = 60_000L,
                        annualRateBasisPoints = 1,
                        tenureMonths = 1,
                    ),
                ),
            )

        assertEquals(Money(1L, usd), quote.totalInterest)
        assertEquals(Money(60_001L, usd), quote.emi)
        assertEquals(Money(60_001L, usd), quote.totalPayable)
    }

    @Test
    fun invalidTermsReturnTypedErrors() {
        val invalidPrincipal = calculator.calculate(terms(principal = 0L))
        val negativeRate = calculator.calculate(terms(annualRateBasisPoints = -1))
        val invalidTenure = calculator.calculate(terms(tenureMonths = 0))
        val unsupportedFrequency =
            calculator.calculate(
                terms(paymentFrequency = LoanPaymentFrequency.QUARTERLY),
            )

        assertEquals(
            LoanCalculationError.InvalidPrincipal,
            assertIs<LoanCalculationResult.Failure>(invalidPrincipal).error,
        )
        assertEquals(
            LoanCalculationError.NegativeInterestRate,
            assertIs<LoanCalculationResult.Failure>(negativeRate).error,
        )
        assertEquals(
            LoanCalculationError.InvalidTenure,
            assertIs<LoanCalculationResult.Failure>(invalidTenure).error,
        )
        assertEquals(
            LoanCalculationError.UnsupportedPaymentFrequency(LoanPaymentFrequency.QUARTERLY),
            assertIs<LoanCalculationResult.Failure>(unsupportedFrequency).error,
        )
    }

    @Test
    fun arithmeticOverflowReturnsTypedError() {
        val result =
            calculator.calculate(
                terms(
                    principal = Long.MAX_VALUE,
                    annualRateBasisPoints = 120_000,
                    tenureMonths = 1,
                ),
            )

        assertEquals(
            LoanCalculationError.ArithmeticOverflow,
            assertIs<LoanCalculationResult.Failure>(result).error,
        )
    }

    private fun terms(
        principal: Long = 100_000L,
        annualRateBasisPoints: Int = 1_000,
        tenureMonths: Int = 12,
        startDate: LocalDate = LocalDate(2026, 1, 15),
        paymentFrequency: LoanPaymentFrequency = LoanPaymentFrequency.MONTHLY,
    ): LoanTerms =
        LoanTerms(
            principal = Money(principal, usd),
            annualInterestRateBasisPoints = annualRateBasisPoints,
            tenureMonths = tenureMonths,
            startDate = startDate,
            paymentFrequency = paymentFrequency,
        )

    private fun successfulQuote(result: LoanCalculationResult<LoanQuote>): LoanQuote =
        assertIs<LoanCalculationResult.Success<LoanQuote>>(result).value

    private fun assertScheduleInvariants(quote: LoanQuote) {
        val schedule = quote.schedule
        assertTrue(schedule.isNotEmpty())
        assertEquals(quote.terms.principal, schedule.first().openingBalance)
        assertEquals(Money.zero(usd), schedule.last().closingBalance)
        assertEquals(
            quote.terms.principal.amount,
            schedule.sumOf { it.principalComponent.amount },
        )
        assertEquals(
            quote.totalInterest.amount,
            schedule.sumOf { it.interestComponent.amount },
        )
        assertEquals(
            quote.totalPayable.amount,
            schedule.sumOf { it.payment.amount },
        )
        schedule.zipWithNext().forEach { (current, next) ->
            assertEquals(current.closingBalance, next.openingBalance)
            assertTrue(!current.isFinal)
        }
    }
}
