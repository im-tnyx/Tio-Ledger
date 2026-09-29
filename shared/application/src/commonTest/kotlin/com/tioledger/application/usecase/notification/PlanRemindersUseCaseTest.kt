package com.tioledger.application.usecase.notification

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.budget.BudgetSummary
import com.tioledger.application.usecase.budget.ListBudgetSummariesUseCase
import com.tioledger.application.usecase.budget.ListBudgetsUseCase
import com.tioledger.budget.engine.BudgetPeriodCalculator
import com.tioledger.budget.engine.BudgetProgressCalculator
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanCompoundingFrequency
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanEmiCalculationMethod
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanInterestType
import com.tioledger.domain.model.LoanPaymentFrequency
import com.tioledger.domain.model.LoanStatus
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.BudgetRepository
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.LoanRepository
import com.tioledger.domain.repository.TransactionHistoryRepository
import com.tioledger.notifications.ReminderPlanner
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlanRemindersUseCaseTest {
    private val inr = CurrencyCode("INR")

    @Test
    fun returnsApplicationOwnedEmiAndBudgetPlansWithoutFinancialWrites() {
        val loanRepository = FakeLoanRepository(listOf(testLoanDetails()))
        val useCase = createUseCase(loanRepository)
        val currentTimestamp = timestamp(LocalDate(2026, 8, 6), 8)

        val result =
            useCase(
                PlanRemindersCommand(
                    currentTimestamp = currentTimestamp,
                    timeZoneId = "UTC",
                    emiRemindersEnabled = true,
                    budgetRemindersEnabled = true,
                ),
            )

        val plans = assertIs<ApplicationResult.Success<List<ReminderPlanView>>>(result).outcome.value
        assertEquals(3, plans.size)
        assertEquals(ReminderPlanTypeView.BUDGET, plans[0].type)
        assertEquals(currentTimestamp, plans[0].deliveryTimestamp)
        assertEquals(ReminderDestinationView.Budgets, plans[0].destination)
        assertEquals(ReminderPlanTypeView.EMI, plans[1].type)
        assertEquals(ReminderDestinationView.LoanDetails("loan-1"), plans[1].destination)
        assertEquals(ReminderPlanTypeView.EMI, plans[2].type)
        assertEquals(1, loanRepository.findAllCalls)
        assertEquals(1, loanRepository.findDetailsCalls)
        assertEquals(0, loanRepository.createCalls)
    }

    @Test
    fun suppressesDeliveredBudgetIdentityThroughApplicationCommand() {
        val loanRepository = FakeLoanRepository(emptyList())
        val useCase = createUseCase(loanRepository)
        val currentTimestamp = timestamp(LocalDate(2026, 8, 6), 8)
        val first =
            assertIs<ApplicationResult.Success<List<ReminderPlanView>>>(
                useCase(
                    PlanRemindersCommand(
                        currentTimestamp = currentTimestamp,
                        timeZoneId = "UTC",
                        emiRemindersEnabled = false,
                        budgetRemindersEnabled = true,
                    ),
                ),
            ).outcome.value.single()

        val second =
            useCase(
                PlanRemindersCommand(
                    currentTimestamp = currentTimestamp,
                    timeZoneId = "UTC",
                    emiRemindersEnabled = false,
                    budgetRemindersEnabled = true,
                    deliveredBudgetIdentityKeys = setOf(first.identityKey),
                ),
            )

        assertEquals(
            emptyList(),
            assertIs<ApplicationResult.Success<List<ReminderPlanView>>>(second).outcome.value,
        )
    }

    @Test
    fun budgetIdentityStaysStableAcrossTimeZonesResolvingTheSameLocalPeriod() {
        val loanRepository = FakeLoanRepository(emptyList())
        val useCase = createUseCase(loanRepository)
        val currentTimestamp = timestamp(LocalDate(2026, 8, 6), 8)

        val kolkata = budgetPlans(useCase, currentTimestamp, "Asia/Kolkata")
        val newYork = budgetPlans(useCase, currentTimestamp, "America/New_York")
        val afterTimeZoneChange =
            budgetPlans(
                useCase = useCase,
                currentTimestamp = currentTimestamp,
                timeZoneId = "America/New_York",
                deliveredBudgetIdentityKeys = setOf(kolkata.single().identityKey),
            )

        assertEquals("budget|budget-1|MONTHLY|2026-08-01|WARNING", kolkata.single().identityKey)
        assertEquals(kolkata.map { it.identityKey }, newYork.map { it.identityKey })
        assertEquals(emptyList(), afterTimeZoneChange)
        assertEquals(0, loanRepository.createCalls)
    }

    @Test
    fun timeZoneCrossingALocalPeriodBoundaryResolvesThatPeriodIdentity() {
        val loanRepository = FakeLoanRepository(emptyList())
        val septemberFirstInKolkata = timestamp(LocalDate(2026, 8, 31), 19)
        val useCase =
            createUseCase(
                loanRepository = loanRepository,
                transactions =
                    listOf(
                        expense("august", timestamp(LocalDate(2026, 8, 5), 12), 8_000L),
                        expense("september-in-kolkata", septemberFirstInKolkata, 8_000L),
                    ),
            )
        val currentTimestamp = timestamp(LocalDate(2026, 8, 31), 20)

        val kolkata = budgetPlans(useCase, currentTimestamp, "Asia/Kolkata")
        val newYork = budgetPlans(useCase, currentTimestamp, "America/New_York")

        assertEquals("budget|budget-1|MONTHLY|2026-09-01|WARNING", kolkata.single().identityKey)
        assertEquals("budget|budget-1|MONTHLY|2026-08-01|EXCEEDED", newYork.single().identityKey)
        assertEquals(0, loanRepository.createCalls)
    }

    @Test
    fun budgetCandidateUsesAuthoritativeSummaryPeriod() {
        val currentTimestamp = timestamp(LocalDate(2026, 8, 6), 8)
        val summary =
            assertIs<ApplicationResult.Success<List<BudgetSummary>>>(
                budgetSummaries()(anchorTimestamp = currentTimestamp, timeZoneId = "Asia/Kolkata"),
            ).outcome.value.single()

        val plan = budgetPlans(createUseCase(FakeLoanRepository(emptyList())), currentTimestamp, "Asia/Kolkata").single()

        assertEquals(
            "budget|${summary.id}|${summary.periodType.name}|${summary.periodStartDate}|${summary.status.name}",
            plan.identityKey,
        )
    }

    @Test
    fun mapsLoanRepositoryFailure() {
        val loanRepository =
            object : LoanRepository {
                override fun findAll(): LedgerResult<List<Loan>> = LedgerResult.Failure(LedgerError.StorageUnavailable)

                override fun findDetails(loanId: String): LedgerResult<LoanDetails> = error("must not be called")

                override fun create(details: LoanDetails): LedgerResult<LoanDetails> = error("must not be called")
            }
        val useCase = createUseCase(loanRepository)

        val result =
            useCase(
                PlanRemindersCommand(
                    currentTimestamp = timestamp(LocalDate(2026, 8, 6), 8),
                    timeZoneId = "UTC",
                    emiRemindersEnabled = true,
                    budgetRemindersEnabled = false,
                ),
            )

        val failure = assertIs<ApplicationResult.Failure>(result)
        assertEquals(ApplicationError.Repository(LedgerError.StorageUnavailable), failure.error)
    }

    @Test
    fun disabledReminderTypesSkipRepositoryReads() {
        val loanRepository = FakeLoanRepository(listOf(testLoanDetails()))
        val useCase = createUseCase(loanRepository)

        val result =
            useCase(
                PlanRemindersCommand(
                    currentTimestamp = timestamp(LocalDate(2026, 8, 6), 8),
                    timeZoneId = "UTC",
                    emiRemindersEnabled = false,
                    budgetRemindersEnabled = false,
                ),
            )

        assertEquals(
            emptyList(),
            assertIs<ApplicationResult.Success<List<ReminderPlanView>>>(result).outcome.value,
        )
        assertEquals(0, loanRepository.findAllCalls)
        assertEquals(0, loanRepository.findDetailsCalls)
        assertEquals(0, loanRepository.createCalls)
    }

    @Test
    fun rejectsInvalidTimeZoneBeforeRepositoryReads() {
        val loanRepository = FakeLoanRepository(listOf(testLoanDetails()))
        val useCase = createUseCase(loanRepository)

        val result =
            useCase(
                PlanRemindersCommand(
                    currentTimestamp = timestamp(LocalDate(2026, 8, 6), 8),
                    timeZoneId = "Not/AZone",
                    emiRemindersEnabled = true,
                    budgetRemindersEnabled = true,
                ),
            )

        val failure = assertIs<ApplicationResult.Failure>(result)
        assertEquals("timeZoneId", assertIs<ApplicationError.Validation>(failure.error).field)
        assertEquals(0, loanRepository.findAllCalls)
    }

    private fun createUseCase(
        loanRepository: LoanRepository,
        transactions: List<TransactionHistoryRecord> = defaultTransactions(),
    ): PlanRemindersUseCase =
        PlanRemindersUseCase(
            loanRepository = loanRepository,
            listBudgetSummariesUseCase = budgetSummaries(transactions),
            reminderPlanner = ReminderPlanner(),
        )

    private fun budgetSummaries(transactions: List<TransactionHistoryRecord> = defaultTransactions()): ListBudgetSummariesUseCase =
        ListBudgetSummariesUseCase(
            listBudgetsUseCase = ListBudgetsUseCase(FakeBudgetRepository()),
            categoryRepository = EmptyCategoryRepository,
            transactionHistoryRepository = FakeTransactionHistoryRepository(transactions),
            periodCalculator = BudgetPeriodCalculator(),
            progressCalculator = BudgetProgressCalculator(),
        )

    private fun budgetPlans(
        useCase: PlanRemindersUseCase,
        currentTimestamp: Long,
        timeZoneId: String,
        deliveredBudgetIdentityKeys: Set<String> = emptySet(),
    ): List<ReminderPlanView> =
        assertIs<ApplicationResult.Success<List<ReminderPlanView>>>(
            useCase(
                PlanRemindersCommand(
                    currentTimestamp = currentTimestamp,
                    timeZoneId = timeZoneId,
                    emiRemindersEnabled = false,
                    budgetRemindersEnabled = true,
                    deliveredBudgetIdentityKeys = deliveredBudgetIdentityKeys,
                ),
            ),
        ).outcome.value

    private fun defaultTransactions(): List<TransactionHistoryRecord> =
        listOf(expense("expense-1", timestamp(LocalDate(2026, 8, 5), 12), 8_000L))

    private fun expense(
        id: String,
        timestamp: Long,
        amount: Long,
    ): TransactionHistoryRecord =
        TransactionHistoryRecord(
            id = id,
            timestamp = timestamp,
            description = "Groceries",
            type = TransactionType.EXPENSE,
            splits =
                listOf(
                    TransactionHistorySplit(
                        id = "split-$id",
                        accountId = "bank-account",
                        accountName = "Bank",
                        accountType = AccountType.BANK,
                        amount = Money(amount, inr),
                        categoryId = null,
                        categoryName = null,
                        entryType = LedgerEntryType.CREDIT,
                    ),
                ),
        )

    private fun testLoanDetails(): LoanDetails {
        val loan =
            Loan(
                id = "loan-1",
                name = "Home loan",
                principal = Money(100_000L, inr),
                annualInterestRateBasisPoints = 1_000,
                interestType = LoanInterestType.FIXED,
                emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
                compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
                paymentFrequency = LoanPaymentFrequency.MONTHLY,
                tenureMonths = 12,
                startDate = timestamp(LocalDate(2026, 7, 9), 0),
                accountId = "loan-account",
                disbursedAccountId = "bank-account",
                processingFee = Money.zero(inr),
                insuranceAmount = Money.zero(inr),
                status = LoanStatus.ACTIVE,
                createdAt = 1L,
                updatedAt = 1L,
            )
        val installment =
            LoanInstallment(
                id = "installment-1",
                loanId = loan.id,
                installmentNumber = 1,
                dueDate = timestamp(LocalDate(2026, 8, 9), 0),
                openingBalance = loan.principal,
                payment = Money(10_000L, inr),
                principalComponent = Money(9_000L, inr),
                interestComponent = Money(1_000L, inr),
                closingBalance = Money(91_000L, inr),
                status = LoanInstallmentStatus.PENDING,
                createdAt = 1L,
                updatedAt = 1L,
            )
        return LoanDetails(loan, listOf(installment))
    }

    private inner class FakeBudgetRepository : BudgetRepository {
        private val budget =
            Budget(
                id = "budget-1",
                name = "Food",
                amount = Money(10_000L, inr),
                categoryId = null,
                periodType = BudgetPeriodType.MONTHLY,
                createdAt = 1L,
                updatedAt = 1L,
            )

        override fun findAll(): LedgerResult<List<Budget>> = LedgerResult.Success(listOf(budget))

        override fun findById(budgetId: String): LedgerResult<Budget> = LedgerResult.Success(budget)

        override fun create(budget: Budget): LedgerResult<Budget> = error("reminder planning must not write budgets")

        override fun update(budget: Budget): LedgerResult<Budget> = error("reminder planning must not write budgets")
    }

    private object EmptyCategoryRepository : CategoryRepository {
        override fun findAll(): LedgerResult<List<Category>> = LedgerResult.Success(emptyList())

        override fun findById(categoryId: String): LedgerResult<Category> = LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))

        override fun create(category: Category): LedgerResult<Category> = LedgerResult.Success(category)

        override fun update(category: Category): LedgerResult<Category> = LedgerResult.Success(category)
    }

    private class FakeTransactionHistoryRepository(
        private val transactions: List<TransactionHistoryRecord>,
    ) : TransactionHistoryRepository {
        override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = LedgerResult.Success(transactions)
    }

    private class FakeLoanRepository(
        details: List<LoanDetails>,
    ) : LoanRepository {
        private val detailsById = details.associateBy { it.loan.id }
        var findAllCalls: Int = 0
        var findDetailsCalls: Int = 0
        var createCalls: Int = 0

        override fun findAll(): LedgerResult<List<Loan>> {
            findAllCalls += 1
            return LedgerResult.Success(detailsById.values.map { it.loan })
        }

        override fun findDetails(loanId: String): LedgerResult<LoanDetails> {
            findDetailsCalls += 1
            return detailsById[loanId]?.let { details -> LedgerResult.Success(details) }
                ?: LedgerResult.Failure(LedgerError.LoanNotFound(loanId))
        }

        override fun create(details: LoanDetails): LedgerResult<LoanDetails> {
            createCalls += 1
            return LedgerResult.Success(details)
        }
    }

    private fun timestamp(
        date: LocalDate,
        hour: Int,
    ): Long =
        LocalDateTime(date, LocalTime(hour, 0))
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()
}
