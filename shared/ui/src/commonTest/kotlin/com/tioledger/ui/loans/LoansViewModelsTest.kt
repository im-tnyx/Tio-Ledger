package com.tioledger.ui.loans

import com.tioledger.analytics.LoanPayoffAnalyticsCalculator
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.loan.CreateLoanUseCase
import com.tioledger.application.usecase.loan.GetLoanDetailsAnalyticsUseCase
import com.tioledger.application.usecase.loan.GetLoanDetailsUseCase
import com.tioledger.application.usecase.loan.ListLoansUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanCompoundingFrequency
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanEmiCalculationMethod
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanInterestType
import com.tioledger.domain.model.LoanPaymentFrequency
import com.tioledger.domain.model.LoanStatus
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.LedgerRepository
import com.tioledger.domain.repository.LoanRepository
import com.tioledger.finance.engine.BalanceCalculator
import com.tioledger.loan.engine.MonthlyReducingBalanceLoanCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoansViewModelsTest {
    @Test
    fun loadsLoansAndEligibleAccountOptions() {
        val repository = FakeLoanRepository(existingDetails())
        val viewModel = createLoansViewModel(repository)

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf("existing-loan"), state.loans.map { it.id })
        assertEquals(listOf("loan-inr", "loan-usd"), state.loanAccountOptions.map { it.id })
        assertEquals(listOf("bank-inr", "bank-usd", "cash-inr"), state.disbursedAccountOptions.map { it.id })
        assertNull(state.loadErrorMessage)
    }

    @Test
    fun createsLoanFromEditorAndRefreshesPersistedList() {
        val repository = FakeLoanRepository()
        val ids = SequenceIdGenerator()
        val viewModel = createLoansViewModel(repository, ids)

        viewModel.onAction(LoansAction.AddClicked)
        viewModel.onAction(LoansAction.NameChanged(" Home Loan "))
        viewModel.onAction(LoansAction.PrincipalChanged("1200.00"))
        viewModel.onAction(LoansAction.InterestRateChanged("8.75"))
        viewModel.onAction(LoansAction.TenureChanged("12"))
        viewModel.onAction(LoansAction.StartDateChanged("2026-07-20"))
        viewModel.onAction(LoansAction.SaveClicked)

        val state = viewModel.uiState.value
        val persisted = assertNotNull(repository.created)

        assertNull(state.editor)
        assertFalse(state.isSaving)
        assertEquals("Home Loan added", state.successMessage)
        assertEquals(listOf("id-1"), state.loans.map { it.id })
        assertEquals(Money(120_000L, CurrencyCode("INR")), persisted.loan.principal)
        assertEquals(875, persisted.loan.annualInterestRateBasisPoints)
        assertEquals("loan-inr", persisted.loan.accountId)
        assertEquals("bank-inr", persisted.loan.disbursedAccountId)
        assertEquals(12, persisted.schedule.size)
        assertEquals((2..13).map { "id-$it" }, persisted.schedule.map { it.id })
    }

    @Test
    fun rejectsInvalidRatePrecisionBeforeCallingApplication() {
        val repository = FakeLoanRepository()
        val viewModel = createLoansViewModel(repository)

        viewModel.onAction(LoansAction.AddClicked)
        viewModel.onAction(LoansAction.NameChanged("Loan"))
        viewModel.onAction(LoansAction.PrincipalChanged("1000"))
        viewModel.onAction(LoansAction.InterestRateChanged("8.755"))
        viewModel.onAction(LoansAction.TenureChanged("12"))
        viewModel.onAction(LoansAction.SaveClicked)

        val state = viewModel.uiState.value

        assertEquals(
            "Enter a non-negative annual rate with at most 2 decimal places.",
            state.validationErrorMessage,
        )
        assertNotNull(state.editor)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun changingLoanAccountFiltersAndClearsCurrencyMismatchedDisbursedAccount() {
        val viewModel = createLoansViewModel(FakeLoanRepository())

        viewModel.onAction(LoansAction.AddClicked)
        assertEquals("loan-inr", viewModel.uiState.value.editor?.loanAccountId)
        assertEquals("bank-inr", viewModel.uiState.value.editor?.disbursedAccountId)

        viewModel.onAction(LoansAction.LoanAccountSelected("loan-usd"))
        val state = viewModel.uiState.value

        assertEquals("loan-usd", state.editor?.loanAccountId)
        assertNull(state.editor?.disbursedAccountId)
        assertEquals(listOf("bank-usd"), state.disbursedAccountOptions.map { it.id })
    }

    @Test
    fun detailsLoadPayoffAnalyticsAccountLabelsAndPersistedInstallments() {
        val repository = FakeLoanRepository(existingDetails())
        val viewModel = createLoanDetailsViewModel(repository)

        viewModel.load("existing-loan")
        val state = viewModel.uiState.value
        val details = assertNotNull(state.details)

        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("Existing Loan", details.name)
        assertEquals("INR 100.00", details.outstandingLabel)
        assertEquals("50.00% repaid", details.principalProgressLabel)
        assertEquals("INR 100.00", details.principalPaidLabel)
        assertEquals("INR 100.00", details.principalRemainingLabel)
        assertEquals("INR 0.00", details.interestPaidLabel)
        assertEquals("INR 0.00", details.interestRemainingLabel)
        assertEquals("1 of 2", details.installmentsCompletedLabel)
        assertEquals("2026-09-20", details.projectedPayoffDateLabel)
        assertEquals("Loan INR", details.loanAccountLabel)
        assertEquals("Bank INR", details.disbursedAccountLabel)
        assertEquals(2, details.schedule.size)
        assertEquals("Paid", details.schedule.first().statusLabel)
        assertEquals("2026-08-20", details.schedule.first().dueDateLabel)
    }

    @Test
    fun detailsFailureIsVisibleAndRetryable() {
        val repository = FakeLoanRepository()
        val viewModel = createLoanDetailsViewModel(repository)

        viewModel.load("missing")

        assertEquals("Unable to load loan details.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.details)
        viewModel.retry()
        assertEquals("Unable to load loan details.", viewModel.uiState.value.errorMessage)
    }

    private fun createLoansViewModel(
        repository: FakeLoanRepository,
        ids: SequenceIdGenerator = SequenceIdGenerator(),
    ): LoansViewModel =
        LoansViewModel(
            listLoansUseCase = ListLoansUseCase(repository),
            listAccountSummariesUseCase = accountSummariesUseCase(),
            createLoanUseCase =
                CreateLoanUseCase(
                    loanRepository = repository,
                    accountRepository = FakeAccountRepository(testAccounts()),
                    loanCalculator = MonthlyReducingBalanceLoanCalculator(),
                    idGenerator = ids,
                ),
            idGenerator = ids,
            nowProvider = { 100L },
            currentDateProvider = { LocalDate(2026, 7, 20) },
        )

    private fun createLoanDetailsViewModel(repository: LoanRepository): LoanDetailsViewModel =
        LoanDetailsViewModel(
            getLoanDetailsAnalyticsUseCase =
                GetLoanDetailsAnalyticsUseCase(
                    getLoanDetailsUseCase = GetLoanDetailsUseCase(repository),
                    payoffCalculator = LoanPayoffAnalyticsCalculator(),
                ),
            listAccountSummariesUseCase = accountSummariesUseCase(),
        )

    private fun accountSummariesUseCase(): ListAccountSummariesUseCase =
        ListAccountSummariesUseCase(
            accountRepository = FakeAccountRepository(testAccounts()),
            ledgerRepository = EmptyLedgerRepository,
            balanceCalculator = BalanceCalculator(),
        )

    private fun testAccounts(): List<Account> =
        listOf(
            account("loan-inr", "Loan INR", AccountType.LOAN_LINKED, "INR"),
            account("loan-usd", "Loan USD", AccountType.LOAN_LINKED, "USD"),
            account("bank-inr", "Bank INR", AccountType.BANK, "INR"),
            account("bank-usd", "Bank USD", AccountType.BANK, "USD"),
            account("cash-inr", "Cash INR", AccountType.CASH, "INR"),
            account("card-inr", "Card INR", AccountType.CREDIT_CARD, "INR"),
            account("archived", "Archived", AccountType.BANK, "INR").copy(isArchived = true),
        )

    private fun existingDetails(): LoanDetails {
        val currency = CurrencyCode("INR")
        val loan =
            Loan(
                id = "existing-loan",
                name = "Existing Loan",
                principal = Money(20_000L, currency),
                annualInterestRateBasisPoints = 0,
                interestType = LoanInterestType.FIXED,
                emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
                compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
                paymentFrequency = LoanPaymentFrequency.MONTHLY,
                tenureMonths = 2,
                startDate = dateMillis(2026, 7, 20),
                accountId = "loan-inr",
                disbursedAccountId = "bank-inr",
                processingFee = Money.zero(currency),
                insuranceAmount = Money.zero(currency),
                status = LoanStatus.ACTIVE,
                createdAt = 1L,
                updatedAt = 1L,
            )
        val schedule =
            listOf(
                installment(
                    id = "one",
                    number = 1,
                    dueDate = dateMillis(2026, 8, 20),
                    opening = 20_000L,
                    closing = 10_000L,
                    currency = currency,
                    status = LoanInstallmentStatus.PAID,
                ),
                installment(
                    id = "two",
                    number = 2,
                    dueDate = dateMillis(2026, 9, 20),
                    opening = 10_000L,
                    closing = 0L,
                    currency = currency,
                    status = LoanInstallmentStatus.PENDING,
                ),
            )
        return LoanDetails(loan, schedule)
    }

    private fun installment(
        id: String,
        number: Int,
        dueDate: Long,
        opening: Long,
        closing: Long,
        currency: CurrencyCode,
        status: LoanInstallmentStatus,
    ): LoanInstallment =
        LoanInstallment(
            id = id,
            loanId = "existing-loan",
            installmentNumber = number,
            dueDate = dueDate,
            openingBalance = Money(opening, currency),
            payment = Money(10_000L, currency),
            principalComponent = Money(10_000L, currency),
            interestComponent = Money.zero(currency),
            closingBalance = Money(closing, currency),
            status = status,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun account(
        id: String,
        name: String,
        type: AccountType,
        currencyCode: String,
    ): Account =
        Account(
            id = id,
            name = name,
            type = type,
            currencyCode = currencyCode,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun dateMillis(
        year: Int,
        month: Int,
        day: Int,
    ): Long =
        LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
}

private class FakeLoanRepository(
    vararg initial: LoanDetails,
) : LoanRepository {
    private val values = initial.associateBy { it.loan.id }.toMutableMap()
    var created: LoanDetails? = null
    var createCalls: Int = 0

    override fun findAll(): LedgerResult<List<Loan>> = LedgerResult.Success(values.values.map { it.loan })

    override fun findDetails(loanId: String): LedgerResult<LoanDetails> =
        values[loanId]?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.LoanNotFound(loanId))

    override fun create(details: LoanDetails): LedgerResult<LoanDetails> {
        createCalls += 1
        if (values.containsKey(details.loan.id)) {
            return LedgerResult.Failure(LedgerError.DuplicateLoanId(details.loan.id))
        }
        values[details.loan.id] = details
        created = details
        return LedgerResult.Success(details)
    }
}

private class FakeAccountRepository(
    accounts: List<Account>,
) : AccountRepository {
    private val values = accounts.associateBy(Account::id)

    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> =
        LedgerResult.Success(values.values.filter { includeArchived || !it.isArchived })

    override fun findById(accountId: String): LedgerResult<Account> =
        values[accountId]?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

    override fun create(account: Account): LedgerResult<Account> = LedgerResult.Failure(LedgerError.Unknown("not supported"))

    override fun update(account: Account): LedgerResult<Account> = LedgerResult.Failure(LedgerError.Unknown("not supported"))
}

private data object EmptyLedgerRepository : LedgerRepository {
    override fun findEntriesByAccount(accountId: String): LedgerResult<List<LedgerEntry>> {
        return LedgerResult.Success(emptyList())
    }

    override fun findEntriesByTransaction(transactionId: String): LedgerResult<List<LedgerEntry>> {
        return LedgerResult.Success(emptyList())
    }
}

private class SequenceIdGenerator : IdGenerator {
    private var next = 0

    override fun nextId(): String {
        next += 1
        return "id-$next"
    }
}
