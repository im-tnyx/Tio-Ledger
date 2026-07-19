package com.tioledger.data.repository

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanCompoundingFrequency
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanEmiCalculationMethod
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanInterestType
import com.tioledger.domain.model.LoanPaymentFrequency
import com.tioledger.domain.model.LoanStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoanRepositoryIntegrationTest {
    private val inr = CurrencyCode("INR")
    private lateinit var repository: SQLDelightLoanRepository

    @BeforeTest
    fun setUp() {
        val driver = createTestSqlDriver()
        TioLedgerDatabase.Schema.create(driver)
        val database = TioLedgerDatabase(driver)
        database.tioLedgerDatabaseQueries.insertCurrency("INR", "Indian Rupee", "₹", 2L, "en-IN")

        val accountRepository = SQLDelightAccountRepository(database)
        accountRepository.create(account("loan-account", "Home Loan", AccountType.LOAN_LINKED))
        accountRepository.create(account("bank-account", "Bank", AccountType.BANK))
        repository = SQLDelightLoanRepository(database)
    }

    @Test
    fun createsAndReturnsLoanDetailsInDeterministicOrder() {
        val vehicle = loanDetails("vehicle", "Vehicle Loan")
        val home = loanDetails("home", "Home Loan")

        assertEquals(home, assertIs<LedgerResult.Success<LoanDetails>>(repository.create(home)).value)
        assertEquals(vehicle, assertIs<LedgerResult.Success<LoanDetails>>(repository.create(vehicle)).value)

        val loans = assertIs<LedgerResult.Success<List<Loan>>>(repository.findAll()).value
        assertEquals(listOf("home", "vehicle"), loans.map(Loan::id))
        assertEquals(inr, loans.first().principal.currency)

        val persisted = assertIs<LedgerResult.Success<LoanDetails>>(repository.findDetails("home")).value
        assertEquals("home", persisted.loan.id)
        assertEquals(listOf(1, 2), persisted.schedule.map(LoanInstallment::installmentNumber))
        assertEquals(Money.zero(inr), persisted.schedule.last().closingBalance)

        val duplicate = assertIs<LedgerResult.Failure>(repository.create(home))
        assertEquals(LedgerError.DuplicateLoanId("home"), duplicate.error)

        val missing = assertIs<LedgerResult.Failure>(repository.findDetails("missing"))
        assertEquals(LedgerError.LoanNotFound("missing"), missing.error)
    }

    @Test
    fun scheduleFailureRollsBackLoanAndAllInstallments() {
        val loan = loan("broken", "Broken Loan")
        val first = installment("duplicate-installment", loan.id, 1, 100_000L, 50_000L)
        val second = installment("duplicate-installment", loan.id, 2, 50_000L, 0L)

        assertIs<LedgerResult.Failure>(
            repository.create(
                LoanDetails(
                    loan = loan,
                    schedule = listOf(first, second),
                ),
            ),
        )

        val missing = assertIs<LedgerResult.Failure>(repository.findDetails(loan.id))
        assertEquals(LedgerError.LoanNotFound(loan.id), missing.error)
    }

    private fun loanDetails(
        id: String,
        name: String,
    ): LoanDetails {
        val loan = loan(id, name)
        return LoanDetails(
            loan = loan,
            schedule =
                listOf(
                    installment("$id-installment-2", id, 2, 50_000L, 0L),
                    installment("$id-installment-1", id, 1, 100_000L, 50_000L),
                ),
        )
    }

    private fun loan(
        id: String,
        name: String,
    ): Loan =
        Loan(
            id = id,
            name = name,
            principal = Money(100_000L, inr),
            annualInterestRateBasisPoints = 1_000,
            interestType = LoanInterestType.REDUCING,
            emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
            compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
            paymentFrequency = LoanPaymentFrequency.MONTHLY,
            tenureMonths = 2,
            startDate = 1_000L,
            accountId = "loan-account",
            disbursedAccountId = "bank-account",
            processingFee = Money.zero(inr),
            insuranceAmount = Money.zero(inr),
            status = LoanStatus.ACTIVE,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun installment(
        id: String,
        loanId: String,
        number: Int,
        openingBalance: Long,
        closingBalance: Long,
    ): LoanInstallment {
        val principal = openingBalance - closingBalance
        val interest = if (number == 1) 1_000L else 500L
        return LoanInstallment(
            id = id,
            loanId = loanId,
            installmentNumber = number,
            dueDate = 1_000L + number,
            openingBalance = Money(openingBalance, inr),
            payment = Money(principal + interest, inr),
            principalComponent = Money(principal, inr),
            interestComponent = Money(interest, inr),
            closingBalance = Money(closingBalance, inr),
            status = LoanInstallmentStatus.PENDING,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }

    private fun account(
        id: String,
        name: String,
        type: AccountType,
    ): Account =
        Account(
            id = id,
            name = name,
            type = type,
            currencyCode = inr.normalized,
            createdAt = 1L,
            updatedAt = 1L,
        )
}
