package com.tioledger.application.usecase.budget

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.budget.engine.BudgetPeriodCalculator
import com.tioledger.budget.engine.BudgetProgressCalculator
import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.BudgetRepository
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.TransactionHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BudgetSummaryUseCaseTest {
    private val usd = CurrencyCode("USD")

    @Test
    fun returnsCurrentPeriodSummaryWithCategorySpend() {
        val budget = testBudget()
        val category =
            Category(
                id = "food",
                name = "Food",
                type = CategoryType.EXPENSE,
                createdAt = 1L,
                updatedAt = 1L,
            )
        val useCase =
            createUseCase(
                budgets = listOf(budget),
                categories = listOf(category),
                transactions =
                    listOf(
                        expense("inside", JULY_5_2026_UTC, "food", 2_500L),
                        expense("other", JULY_5_2026_UTC, "travel", 4_000L),
                    ),
            )

        val result = useCase(anchorTimestamp = JULY_19_2026_UTC, timeZoneId = "UTC")

        val success = assertIs<ApplicationResult.Success<List<BudgetSummary>>>(result)
        val summary = success.outcome.value.single()
        assertEquals("Food", summary.categoryName)
        assertEquals(JULY_1_2026_UTC, summary.periodStartInclusive)
        assertEquals(AUGUST_1_2026_UTC, summary.periodEndExclusive)
        assertEquals(Money(2_500L, usd), summary.spent)
        assertEquals(Money(7_500L, usd), summary.remaining)
        assertEquals(250, summary.utilizationPermille)
        assertEquals(BudgetProgressStatus.ON_TRACK, summary.status)
    }

    @Test
    fun mapsTransactionRepositoryFailure() {
        val budgetRepository = FakeBudgetRepository(emptyList())
        val categoryRepository = FakeCategoryRepository(emptyList())
        val transactionRepository =
            object : TransactionHistoryRepository {
                override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> {
                    return LedgerResult.Failure(LedgerError.StorageUnavailable)
                }
            }
        val useCase =
            ListBudgetSummariesUseCase(
                listBudgetsUseCase = ListBudgetsUseCase(budgetRepository),
                categoryRepository = categoryRepository,
                transactionHistoryRepository = transactionRepository,
                periodCalculator = BudgetPeriodCalculator(),
                progressCalculator = BudgetProgressCalculator(),
            )

        val result = useCase(anchorTimestamp = JULY_19_2026_UTC, timeZoneId = "UTC")

        val failure = assertIs<ApplicationResult.Failure>(result)
        assertEquals(
            ApplicationError.Repository(LedgerError.StorageUnavailable),
            failure.error,
        )
    }

    @Test
    fun rejectsInvalidTimeZone() {
        val useCase = createUseCase(listOf(testBudget()), emptyList(), emptyList())

        val result = useCase(anchorTimestamp = JULY_19_2026_UTC, timeZoneId = "Not/AZone")

        val failure = assertIs<ApplicationResult.Failure>(result)
        val error = assertIs<ApplicationError.Validation>(failure.error)
        assertEquals("timeZoneId", error.field)
    }

    private fun createUseCase(
        budgets: List<Budget>,
        categories: List<Category>,
        transactions: List<TransactionHistoryRecord>,
    ): ListBudgetSummariesUseCase =
        ListBudgetSummariesUseCase(
            listBudgetsUseCase = ListBudgetsUseCase(FakeBudgetRepository(budgets)),
            categoryRepository = FakeCategoryRepository(categories),
            transactionHistoryRepository = FakeTransactionHistoryRepository(transactions),
            periodCalculator = BudgetPeriodCalculator(),
            progressCalculator = BudgetProgressCalculator(),
        )

    private fun testBudget(): Budget =
        Budget(
            id = "budget-food",
            name = "Food",
            amount = Money(10_000L, usd),
            categoryId = "food",
            periodType = BudgetPeriodType.MONTHLY,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun expense(
        id: String,
        timestamp: Long,
        categoryId: String,
        amount: Long,
    ): TransactionHistoryRecord =
        TransactionHistoryRecord(
            id = id,
            timestamp = timestamp,
            description = id,
            type = TransactionType.EXPENSE,
            splits =
                listOf(
                    TransactionHistorySplit(
                        id = "$id-split",
                        accountId = "account",
                        accountName = "Account",
                        accountType = AccountType.BANK,
                        amount = Money(amount, usd),
                        categoryId = categoryId,
                        categoryName = categoryId,
                        entryType = LedgerEntryType.CREDIT,
                    ),
                ),
        )

    private class FakeBudgetRepository(
        private val budgets: List<Budget>,
    ) : BudgetRepository {
        override fun findAll(): LedgerResult<List<Budget>> = LedgerResult.Success(budgets)

        override fun findById(budgetId: String): LedgerResult<Budget> {
            val budget = budgets.firstOrNull { it.id == budgetId }
            return if (budget != null) {
                LedgerResult.Success(budget)
            } else {
                LedgerResult.Failure(LedgerError.BudgetNotFound(budgetId))
            }
        }

        override fun create(budget: Budget): LedgerResult<Budget> = LedgerResult.Success(budget)

        override fun update(budget: Budget): LedgerResult<Budget> = LedgerResult.Success(budget)
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>,
    ) : CategoryRepository {
        override fun findAll(): LedgerResult<List<Category>> = LedgerResult.Success(categories)

        override fun findById(categoryId: String): LedgerResult<Category> {
            val category = categories.firstOrNull { it.id == categoryId }
            return if (category != null) {
                LedgerResult.Success(category)
            } else {
                LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))
            }
        }

        override fun create(category: Category): LedgerResult<Category> = LedgerResult.Success(category)

        override fun update(category: Category): LedgerResult<Category> = LedgerResult.Success(category)
    }

    private class FakeTransactionHistoryRepository(
        private val transactions: List<TransactionHistoryRecord>,
    ) : TransactionHistoryRepository {
        override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = LedgerResult.Success(transactions)
    }

    private companion object {
        const val JULY_1_2026_UTC = 1_782_864_000_000L
        const val JULY_5_2026_UTC = 1_783_209_600_000L
        const val JULY_19_2026_UTC = 1_784_419_200_000L
        const val AUGUST_1_2026_UTC = 1_785_542_400_000L
    }
}
