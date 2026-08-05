package com.tioledger.application.usecase.loan

import com.tioledger.analytics.LoanPayoffAnalyticsCalculator
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
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
import com.tioledger.domain.repository.LoanRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoanPayoffAnalyticsUseCaseTest {
    private val currency = CurrencyCode("INR")

    @Test
    fun returnsLoanDetailsAndPayoffAnalyticsFromOneRepositoryRead() {
        val repository = CountingLoanRepository(details())
        val useCase =
            GetLoanDetailsAnalyticsUseCase(
                getLoanDetailsUseCase = GetLoanDetailsUseCase(repository),
                payoffCalculator = LoanPayoffAnalyticsCalculator(),
            )

        val result = assertIs<ApplicationResult.Success<LoanDetailsAnalyticsView>>(useCase(" loan-1 "))
        val value = result.outcome.value

        assertEquals(1, repository.findDetailsCalls)
        assertEquals("loan-1", value.details.overview.loan.id)
        assertEquals(Money(6_000L, currency), value.payoff.principalPaid)
        assertEquals(Money(4_000L, currency), value.payoff.principalRemaining)
        assertEquals(6_000, value.payoff.principalProgressBasisPoints)
        assertEquals(Money(500L, currency), value.payoff.interestPaid)
        assertEquals(Money(200L, currency), value.payoff.interestRemaining)
        assertEquals(1, value.payoff.paidInstallments)
        assertEquals(1, value.payoff.remainingInstallments)
        assertEquals(200L, value.payoff.nextDueDate)
        assertEquals(200L, value.payoff.projectedPayoffDate)
    }

    @Test
    fun propagatesRepositoryFailureWithoutCalculating() {
        val repository = CountingLoanRepository()
        val result =
            GetLoanDetailsAnalyticsUseCase(
                getLoanDetailsUseCase = GetLoanDetailsUseCase(repository),
                payoffCalculator = LoanPayoffAnalyticsCalculator(),
            )("missing")

        assertEquals(
            ApplicationError.Repository(LedgerError.LoanNotFound("missing")),
            assertIs<ApplicationResult.Failure>(result).error,
        )
        assertEquals(1, repository.findDetailsCalls)
    }

    @Test
    fun mapsInvalidPayoffScheduleToLedgerFailure() {
        val invalid =
            details().copy(
                schedule =
                    listOf(
                        installment(
                            id = "too-much",
                            number = 1,
                            dueDate = 100L,
                            principal = 10_001L,
                            interest = 0L,
                            status = LoanInstallmentStatus.PAID,
                        ),
                    ),
            )
        val result =
            GetLoanDetailsAnalyticsUseCase(
                getLoanDetailsUseCase = GetLoanDetailsUseCase(CountingLoanRepository(invalid)),
                payoffCalculator = LoanPayoffAnalyticsCalculator(),
            )("loan-1")

        val error = assertIs<ApplicationError.Ledger>(assertIs<ApplicationResult.Failure>(result).error)
        assertEquals(
            LedgerError.Unknown("loan payoff analytics could not be calculated"),
            error.error,
        )
    }

    private fun details(): LoanDetails =
        LoanDetails(
            loan =
                Loan(
                    id = "loan-1",
                    name = "Loan",
                    principal = Money(10_000L, currency),
                    annualInterestRateBasisPoints = 800,
                    interestType = LoanInterestType.FIXED,
                    emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
                    compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
                    paymentFrequency = LoanPaymentFrequency.MONTHLY,
                    tenureMonths = 2,
                    startDate = 0L,
                    accountId = "loan-account",
                    disbursedAccountId = "bank-account",
                    processingFee = Money.zero(currency),
                    insuranceAmount = Money.zero(currency),
                    status = LoanStatus.ACTIVE,
                    createdAt = 0L,
                    updatedAt = 0L,
                ),
            schedule =
                listOf(
                    installment(
                        id = "paid",
                        number = 1,
                        dueDate = 100L,
                        principal = 6_000L,
                        interest = 500L,
                        status = LoanInstallmentStatus.PAID,
                    ),
                    installment(
                        id = "pending",
                        number = 2,
                        dueDate = 200L,
                        principal = 4_000L,
                        interest = 200L,
                        status = LoanInstallmentStatus.PENDING,
                    ),
                ),
        )

    private fun installment(
        id: String,
        number: Int,
        dueDate: Long,
        principal: Long,
        interest: Long,
        status: LoanInstallmentStatus,
    ): LoanInstallment =
        LoanInstallment(
            id = id,
            loanId = "loan-1",
            installmentNumber = number,
            dueDate = dueDate,
            openingBalance = Money(principal, currency),
            payment = Money(principal + interest, currency),
            principalComponent = Money(principal, currency),
            interestComponent = Money(interest, currency),
            closingBalance = Money.zero(currency),
            status = status,
            createdAt = 0L,
            updatedAt = 0L,
        )
}

private class CountingLoanRepository(
    private val details: LoanDetails? = null,
) : LoanRepository {
    var findDetailsCalls: Int = 0

    override fun findAll(): LedgerResult<List<Loan>> = LedgerResult.Success(details?.let { listOf(it.loan) }.orEmpty())

    override fun findDetails(loanId: String): LedgerResult<LoanDetails> {
        findDetailsCalls += 1
        return details?.takeIf { it.loan.id == loanId }?.let(LedgerResult::Success)
            ?: LedgerResult.Failure(LedgerError.LoanNotFound(loanId))
    }

    override fun create(details: LoanDetails): LedgerResult<LoanDetails> =
        LedgerResult.Failure(LedgerError.Unknown("not supported"))
}
