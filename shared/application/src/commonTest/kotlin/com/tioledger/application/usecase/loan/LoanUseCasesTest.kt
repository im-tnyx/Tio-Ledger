package com.tioledger.application.usecase.loan

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.event.DomainEvent
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
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.LoanRepository
import com.tioledger.loan.engine.LoanCalculationError
import com.tioledger.loan.engine.LoanCalculationResult
import com.tioledger.loan.engine.LoanCalculator
import com.tioledger.loan.engine.LoanQuote
import com.tioledger.loan.engine.LoanTerms
import com.tioledger.loan.engine.MonthlyReducingBalanceLoanCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoanUseCasesTest {
    private val inr = CurrencyCode("INR")

    @Test
    fun createsActiveLoanAndPersistsGeneratedSchedule() {
        val loanRepository = FakeLoanRepository()
        val useCase =
            CreateLoanUseCase(
                loanRepository = loanRepository,
                accountRepository =
                    FakeAccountRepository(
                        loanAccount("loan-account", "INR"),
                        assetAccount("bank-account", "INR"),
                    ),
                loanCalculator = MonthlyReducingBalanceLoanCalculator(),
                idGenerator = SequenceIdGenerator(),
            )

        val result =
            useCase(
                CreateLoanCommand(
                    id = " home-loan ",
                    name = " Home Loan ",
                    principalAmount = 120_000L,
                    annualInterestRateBasisPoints = 0,
                    tenureMonths = 12,
                    startDate = LocalDate(2026, 1, 31),
                    accountId = " loan-account ",
                    disbursedAccountId = " bank-account ",
                    createdAt = 10L,
                    deviceId = "device",
                ),
            )

        val success = assertIs<ApplicationResult.Success<LoanDetailsView>>(result)
        val view = success.outcome.value
        val persisted = requireNotNull(loanRepository.created)

        assertEquals("home-loan", view.overview.loan.id)
        assertEquals("Home Loan", view.overview.loan.name)
        assertEquals(Money(120_000L, inr), view.overview.loan.principal)
        assertEquals(LoanStatus.ACTIVE, view.overview.loan.status)
        assertEquals("loan-account", view.overview.loan.accountId)
        assertEquals("bank-account", view.overview.loan.disbursedAccountId)
        assertEquals(dateMillis(2026, 1, 31), view.overview.loan.startDate)
        assertEquals(Money(10_000L, inr), view.overview.scheduledEmi)
        assertEquals(Money.zero(inr), view.overview.totalInterest)
        assertEquals(Money(120_000L, inr), view.overview.totalPayable)
        assertEquals(Money(120_000L, inr), view.overview.outstandingPrincipal)
        assertEquals(12, view.overview.remainingInstallments)
        assertEquals(dateMillis(2026, 2, 28), view.overview.nextDueDate)
        assertEquals((1..12).map { "installment-$it" }, persisted.schedule.map { it.id })
        assertTrue(persisted.schedule.all { it.status == LoanInstallmentStatus.PENDING })
        assertEquals(
            listOf(DomainEvent.LoanCreated("home-loan", 10L)),
            success.outcome.events,
        )
        assertEquals(1, loanRepository.createCalls)
    }

    @Test
    fun rejectsInvalidAccountSelectionsAndCurrencyMismatch() {
        assertValidation(
            accounts =
                listOf(
                    assetAccount("loan", "INR"),
                    assetAccount("bank", "INR"),
                ),
            expected = ApplicationError.Validation("accountId", "must reference a LOAN_LINKED account"),
        )
        assertValidation(
            accounts =
                listOf(
                    loanAccount("loan", "INR"),
                    account("bank", AccountType.CREDIT_CARD, "INR"),
                ),
            expected =
                ApplicationError.Validation(
                    "disbursedAccountId",
                    "must reference a non-loan asset account",
                ),
        )
        assertValidation(
            accounts =
                listOf(
                    loanAccount("loan", "INR"),
                    assetAccount("bank", "USD"),
                ),
            expected =
                ApplicationError.Validation(
                    "disbursedAccountId",
                    "must use the same currency as the linked loan account",
                ),
        )

        val repository = FakeLoanRepository()
        val sameAccountResult =
            createUseCase(
                loanRepository = repository,
                accounts = listOf(loanAccount("loan", "INR")),
            )(
                validCommand().copy(
                    accountId = "loan",
                    disbursedAccountId = "loan",
                ),
            )

        assertEquals(
            ApplicationError.Validation(
                "disbursedAccountId",
                "must differ from the linked loan account",
            ),
            assertIs<ApplicationResult.Failure>(sameAccountResult).error,
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun mapsCalculatorFailureWithoutPersisting() {
        val loanRepository = FakeLoanRepository()
        val useCase =
            CreateLoanUseCase(
                loanRepository = loanRepository,
                accountRepository =
                    FakeAccountRepository(
                        loanAccount("loan", "INR"),
                        assetAccount("bank", "INR"),
                    ),
                loanCalculator = FailingLoanCalculator(LoanCalculationError.ArithmeticOverflow),
                idGenerator = SequenceIdGenerator(),
            )

        val result = useCase(validCommand())

        assertEquals(
            ApplicationError.Validation(
                "loanTerms",
                "calculation exceeds the supported numeric range",
            ),
            assertIs<ApplicationResult.Failure>(result).error,
        )
        assertEquals(0, loanRepository.createCalls)
    }

    @Test
    fun listAndDetailsBuildSummariesFromPersistedSchedule() {
        val alpha =
            details(
                id = "alpha",
                name = "Alpha",
                statuses = listOf(LoanInstallmentStatus.PAID, LoanInstallmentStatus.PENDING),
            )
        val zeta =
            details(
                id = "zeta",
                name = "Zeta",
                statuses = listOf(LoanInstallmentStatus.PENDING, LoanInstallmentStatus.PENDING),
            )
        val repository = FakeLoanRepository(alpha, zeta)

        val list =
            assertIs<ApplicationResult.Success<List<LoanOverview>>>(
                ListLoansUseCase(repository)(),
            ).outcome.value
        val alphaOverview = list.first()
        val detailsView =
            assertIs<ApplicationResult.Success<LoanDetailsView>>(
                GetLoanDetailsUseCase(repository)(" alpha "),
            ).outcome.value

        assertEquals(listOf("alpha", "zeta"), list.map { it.loan.id })
        assertEquals(Money(5_000L, inr), alphaOverview.scheduledEmi)
        assertEquals(Money.zero(inr), alphaOverview.totalInterest)
        assertEquals(Money(10_000L, inr), alphaOverview.totalPayable)
        assertEquals(Money(5_000L, inr), alphaOverview.outstandingPrincipal)
        assertEquals(1, alphaOverview.remainingInstallments)
        assertEquals(2L, alphaOverview.nextDueDate)
        assertEquals(alpha.schedule, detailsView.schedule)
    }

    @Test
    fun repositoryFailuresArePropagated() {
        val repository = FakeLoanRepository()
        repository.findAllResult = LedgerResult.Failure(LedgerError.StorageUnavailable)

        val listResult = ListLoansUseCase(repository)()
        val detailsResult = GetLoanDetailsUseCase(repository)("missing")

        assertEquals(
            ApplicationError.Repository(LedgerError.StorageUnavailable),
            assertIs<ApplicationResult.Failure>(listResult).error,
        )
        assertEquals(
            ApplicationError.Repository(LedgerError.LoanNotFound("missing")),
            assertIs<ApplicationResult.Failure>(detailsResult).error,
        )
    }

    private fun assertValidation(
        accounts: List<Account>,
        expected: ApplicationError.Validation,
    ) {
        val repository = FakeLoanRepository()
        val result = createUseCase(repository, accounts)(validCommand())
        assertEquals(expected, assertIs<ApplicationResult.Failure>(result).error)
        assertEquals(0, repository.createCalls)
    }

    private fun createUseCase(
        loanRepository: FakeLoanRepository,
        accounts: List<Account>,
    ): CreateLoanUseCase {
        return CreateLoanUseCase(
            loanRepository = loanRepository,
            accountRepository = FakeAccountRepository(*accounts.toTypedArray()),
            loanCalculator = MonthlyReducingBalanceLoanCalculator(),
            idGenerator = SequenceIdGenerator(),
        )
    }

    private fun validCommand(): CreateLoanCommand {
        return CreateLoanCommand(
            id = "loan-id",
            name = "Loan",
            principalAmount = 120_000L,
            annualInterestRateBasisPoints = 0,
            tenureMonths = 12,
            startDate = LocalDate(2026, 1, 15),
            accountId = "loan",
            disbursedAccountId = "bank",
            createdAt = 10L,
        )
    }

    private fun loanAccount(
        id: String,
        currency: String,
    ): Account {
        return account(id, AccountType.LOAN_LINKED, currency)
    }

    private fun assetAccount(
        id: String,
        currency: String,
    ): Account {
        return account(id, AccountType.BANK, currency)
    }

    private fun account(
        id: String,
        type: AccountType,
        currency: String,
    ): Account {
        return Account(
            id = id,
            name = id,
            type = type,
            currencyCode = currency,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }

    private fun details(
        id: String,
        name: String,
        statuses: List<LoanInstallmentStatus>,
    ): LoanDetails {
        val loan =
            Loan(
                id = id,
                name = name,
                principal = Money(10_000L, inr),
                annualInterestRateBasisPoints = 0,
                interestType = LoanInterestType.FIXED,
                emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
                compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
                paymentFrequency = LoanPaymentFrequency.MONTHLY,
                tenureMonths = 2,
                startDate = 0L,
                accountId = "loan-account",
                disbursedAccountId = "bank-account",
                processingFee = Money.zero(inr),
                insuranceAmount = Money.zero(inr),
                status = LoanStatus.ACTIVE,
                createdAt = 1L,
                updatedAt = 1L,
            )
        val schedule =
            statuses.mapIndexed { index, status ->
                val opening = 10_000L - index * 5_000L
                LoanInstallment(
                    id = "$id-${index + 1}",
                    loanId = id,
                    installmentNumber = index + 1,
                    dueDate = (index + 1).toLong(),
                    openingBalance = Money(opening, inr),
                    payment = Money(5_000L, inr),
                    principalComponent = Money(5_000L, inr),
                    interestComponent = Money.zero(inr),
                    closingBalance = Money(opening - 5_000L, inr),
                    status = status,
                    createdAt = 1L,
                    updatedAt = 1L,
                )
            }
        return LoanDetails(loan, schedule)
    }

    private fun dateMillis(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        return LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
}

private class FakeLoanRepository(
    vararg initial: LoanDetails,
) : LoanRepository {
    private val detailsById = initial.associateBy { it.loan.id }.toMutableMap()
    var findAllResult: LedgerResult<List<Loan>>? = null
    var created: LoanDetails? = null
    var createCalls: Int = 0

    override fun findAll(): LedgerResult<List<Loan>> {
        return findAllResult ?: LedgerResult.Success(detailsById.values.map { it.loan })
    }

    override fun findDetails(loanId: String): LedgerResult<LoanDetails> {
        val details = detailsById[loanId]
        return if (details != null) {
            LedgerResult.Success(details)
        } else {
            LedgerResult.Failure(LedgerError.LoanNotFound(loanId))
        }
    }

    override fun create(details: LoanDetails): LedgerResult<LoanDetails> {
        createCalls += 1
        if (detailsById.containsKey(details.loan.id)) {
            return LedgerResult.Failure(LedgerError.DuplicateLoanId(details.loan.id))
        }
        detailsById[details.loan.id] = details
        created = details
        return LedgerResult.Success(details)
    }
}

private class FakeAccountRepository(
    vararg accounts: Account,
) : AccountRepository {
    private val accountsById = accounts.associateBy(Account::id)

    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> {
        return LedgerResult.Success(accountsById.values.filter { includeArchived || !it.isArchived })
    }

    override fun findById(accountId: String): LedgerResult<Account> {
        val account = accountsById[accountId]
        return if (account != null) {
            LedgerResult.Success(account)
        } else {
            LedgerResult.Failure(LedgerError.AccountNotFound(accountId))
        }
    }

    override fun create(account: Account): LedgerResult<Account> {
        return LedgerResult.Failure(LedgerError.Unknown("not supported"))
    }

    override fun update(account: Account): LedgerResult<Account> {
        return LedgerResult.Failure(LedgerError.Unknown("not supported"))
    }
}

private class SequenceIdGenerator : IdGenerator {
    private var value = 0

    override fun nextId(): String {
        value += 1
        return "installment-$value"
    }
}

private class FailingLoanCalculator(
    private val error: LoanCalculationError,
) : LoanCalculator {
    override fun calculate(terms: LoanTerms): LoanCalculationResult<LoanQuote> {
        return LoanCalculationResult.Failure(error)
    }
}
