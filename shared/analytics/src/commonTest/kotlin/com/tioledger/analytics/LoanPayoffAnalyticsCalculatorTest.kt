package com.tioledger.analytics

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanCompoundingFrequency
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanEmiCalculationMethod
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanInterestType
import com.tioledger.domain.model.LoanPaymentFrequency
import com.tioledger.domain.model.LoanStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LoanPayoffAnalyticsCalculatorTest {
    private val calculator = LoanPayoffAnalyticsCalculator()

    @Test
    fun calculatesPartialPayoffFromPaidInstallmentsOnly() {
        val result =
            calculator.calculate(
                details(
                    principal = 100_000L,
                    installments =
                        listOf(
                            installment(
                                number = 1,
                                dueDate = 100L,
                                principal = 30_000L,
                                interest = 1_000L,
                                status = LoanInstallmentStatus.PAID,
                            ),
                            installment(
                                number = 2,
                                dueDate = 200L,
                                principal = 35_000L,
                                interest = 700L,
                                status = LoanInstallmentStatus.PENDING,
                            ),
                            installment(
                                number = 3,
                                dueDate = 300L,
                                principal = 35_000L,
                                interest = 300L,
                                status = LoanInstallmentStatus.OVERDUE,
                            ),
                        ),
                ),
            )

        assertEquals(money(30_000L), result.principalPaid)
        assertEquals(money(70_000L), result.principalRemaining)
        assertEquals(3_000, result.principalProgressBasisPoints)
        assertEquals(money(1_000L), result.interestPaid)
        assertEquals(money(1_000L), result.interestRemaining)
        assertEquals(money(2_000L), result.totalScheduledInterest)
        assertEquals(money(31_000L), result.amountPaid)
        assertEquals(money(71_000L), result.amountRemaining)
        assertEquals(1, result.paidInstallments)
        assertEquals(2, result.remainingInstallments)
        assertEquals(200L, result.nextDueDate)
        assertEquals(300L, result.projectedPayoffDate)
    }

    @Test
    fun roundsPrincipalProgressHalfUpInBasisPoints() {
        val result =
            calculator.calculate(
                details(
                    principal = 3L,
                    installments =
                        listOf(
                            installment(
                                number = 1,
                                dueDate = 100L,
                                principal = 2L,
                                interest = 0L,
                                status = LoanInstallmentStatus.PAID,
                            ),
                            installment(
                                number = 2,
                                dueDate = 200L,
                                principal = 1L,
                                interest = 0L,
                                status = LoanInstallmentStatus.PENDING,
                            ),
                        ),
                ),
            )

        assertEquals(6_667, result.principalProgressBasisPoints)
    }

    @Test
    fun keepsEveryNonPaidStatusInRemainingMetrics() {
        val result =
            calculator.calculate(
                details(
                    principal = 30_000L,
                    installments =
                        listOf(
                            installment(1, 100L, 10_000L, 100L, LoanInstallmentStatus.OVERDUE),
                            installment(2, 200L, 10_000L, 100L, LoanInstallmentStatus.WAIVED),
                            installment(3, 300L, 10_000L, 100L, LoanInstallmentStatus.ADJUSTED),
                        ),
                ),
            )

        assertEquals(0, result.paidInstallments)
        assertEquals(3, result.remainingInstallments)
        assertEquals(money(30_000L), result.principalRemaining)
        assertEquals(money(300L), result.interestRemaining)
        assertEquals(0, result.principalProgressBasisPoints)
    }

    @Test
    fun fullyPaidLoanUsesFinalScheduleDateAndZeroRemainingValues() {
        val result =
            calculator.calculate(
                details(
                    principal = 20_000L,
                    installments =
                        listOf(
                            installment(1, 100L, 10_000L, 0L, LoanInstallmentStatus.PAID),
                            installment(2, 200L, 10_000L, 0L, LoanInstallmentStatus.PAID),
                        ),
                ),
            )

        assertEquals(money(20_000L), result.principalPaid)
        assertEquals(money(0L), result.principalRemaining)
        assertEquals(10_000, result.principalProgressBasisPoints)
        assertEquals(2, result.paidInstallments)
        assertEquals(0, result.remainingInstallments)
        assertNull(result.nextDueDate)
        assertEquals(200L, result.projectedPayoffDate)
    }

    @Test
    fun emptyScheduleKeepsFullPrincipalOutstanding() {
        val result = calculator.calculate(details(principal = 50_000L, installments = emptyList()))

        assertEquals(money(0L), result.principalPaid)
        assertEquals(money(50_000L), result.principalRemaining)
        assertEquals(money(0L), result.totalScheduledInterest)
        assertEquals(money(0L), result.amountPaid)
        assertEquals(money(0L), result.amountRemaining)
        assertEquals(0, result.paidInstallments)
        assertEquals(0, result.remainingInstallments)
        assertNull(result.nextDueDate)
        assertNull(result.projectedPayoffDate)
    }

    @Test
    fun rejectsScheduleCurrencyMismatch() {
        val mismatched =
            installment(
                number = 1,
                dueDate = 100L,
                principal = 10_000L,
                interest = 100L,
                status = LoanInstallmentStatus.PENDING,
            ).copy(
                interestComponent = Money(100L, CurrencyCode("USD")),
            )

        assertFailsWith<IllegalArgumentException> {
            calculator.calculate(details(principal = 10_000L, installments = listOf(mismatched)))
        }
    }

    @Test
    fun rejectsPaidPrincipalAboveOriginalPrincipal() {
        assertFailsWith<IllegalArgumentException> {
            calculator.calculate(
                details(
                    principal = 10_000L,
                    installments =
                        listOf(
                            installment(1, 100L, 10_001L, 0L, LoanInstallmentStatus.PAID),
                        ),
                ),
            )
        }
    }

    @Test
    fun reportsArithmeticOverflowFromScheduleTotals() {
        assertFailsWith<ArithmeticException> {
            calculator.calculate(
                details(
                    principal = Long.MAX_VALUE,
                    installments =
                        listOf(
                            installment(1, 100L, 1L, Long.MAX_VALUE, LoanInstallmentStatus.PAID),
                            installment(2, 200L, 1L, 1L, LoanInstallmentStatus.PAID),
                        ),
                ),
            )
        }
    }

    private fun details(
        principal: Long,
        installments: List<LoanInstallment>,
    ): LoanDetails =
        LoanDetails(
            loan =
                Loan(
                    id = "loan-1",
                    name = "Home loan",
                    principal = money(principal),
                    annualInterestRateBasisPoints = 800,
                    interestType = LoanInterestType.FIXED,
                    emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
                    compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
                    paymentFrequency = LoanPaymentFrequency.MONTHLY,
                    tenureMonths = maxOf(installments.size, 1),
                    startDate = 0L,
                    accountId = "loan-account",
                    disbursedAccountId = "bank-account",
                    processingFee = money(0L),
                    insuranceAmount = money(0L),
                    status = LoanStatus.ACTIVE,
                    createdAt = 0L,
                    updatedAt = 0L,
                ),
            schedule = installments,
        )

    private fun installment(
        number: Int,
        dueDate: Long,
        principal: Long,
        interest: Long,
        status: LoanInstallmentStatus,
    ): LoanInstallment =
        LoanInstallment(
            id = "installment-$number",
            loanId = "loan-1",
            installmentNumber = number,
            dueDate = dueDate,
            openingBalance = money(principal),
            payment = money(principal + interest),
            principalComponent = money(principal),
            interestComponent = money(interest),
            closingBalance = money(0L),
            status = status,
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun money(amount: Long): Money = Money(amount, CurrencyCode("INR"))
}
