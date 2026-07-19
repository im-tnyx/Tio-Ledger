package com.tioledger.application.usecase.budget

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.repository.BudgetRepository
import com.tioledger.domain.repository.CategoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BudgetUseCasesTest {
    @Test
    fun createsNormalizedBudgetForExpenseCategory() {
        val budgetRepository = FakeBudgetRepository()
        val categoryRepository = FakeCategoryRepository(category("food", CategoryType.EXPENSE))

        val result =
            CreateBudgetUseCase(budgetRepository, categoryRepository)(
                CreateBudgetCommand(
                    id = " food-budget ",
                    name = " Food ",
                    amount = 50_000L,
                    currencyCode = "inr",
                    categoryId = " food ",
                    periodType = BudgetPeriodType.MONTHLY,
                    createdAt = 10L,
                ),
            )

        val created = assertIs<ApplicationResult.Success<Budget>>(result).outcome.value
        assertEquals("food-budget", created.id)
        assertEquals("Food", created.name)
        assertEquals(Money(50_000L, CurrencyCode("INR")), created.amount)
        assertEquals("food", created.categoryId)
        assertEquals(1, budgetRepository.createCalls)
    }

    @Test
    fun rejectsIncomeCategory() {
        val budgetRepository = FakeBudgetRepository()
        val categoryRepository = FakeCategoryRepository(category("salary", CategoryType.INCOME))

        val result =
            CreateBudgetUseCase(budgetRepository, categoryRepository)(
                CreateBudgetCommand(
                    id = "salary-budget",
                    name = "Salary",
                    amount = 10_000L,
                    currencyCode = "INR",
                    categoryId = "salary",
                    periodType = BudgetPeriodType.MONTHLY,
                    createdAt = 10L,
                ),
            )

        val failure = assertIs<ApplicationResult.Failure>(result)
        assertEquals(
            ApplicationError.Validation("categoryId", "budget category must be an expense category"),
            failure.error,
        )
        assertEquals(0, budgetRepository.createCalls)
    }

    @Test
    fun rejectsDuplicateCategoryAndPeriodScope() {
        val existing = budget("food-budget", "food", BudgetPeriodType.MONTHLY)
        val budgetRepository = FakeBudgetRepository(mutableListOf(existing))
        val categoryRepository = FakeCategoryRepository(category("food", CategoryType.EXPENSE))

        val result =
            CreateBudgetUseCase(budgetRepository, categoryRepository)(
                CreateBudgetCommand(
                    id = "second-food-budget",
                    name = "Food 2",
                    amount = 60_000L,
                    currencyCode = "INR",
                    categoryId = "food",
                    periodType = BudgetPeriodType.MONTHLY,
                    createdAt = 20L,
                ),
            )

        val failure = assertIs<ApplicationResult.Failure>(result)
        assertEquals(
            ApplicationError.Validation("categoryId", "budget already exists for this category and period"),
            failure.error,
        )
        assertEquals(0, budgetRepository.createCalls)
    }
}

private class FakeBudgetRepository(
    private val budgets: MutableList<Budget> = mutableListOf(),
) : BudgetRepository {
    var createCalls: Int = 0
        private set

    override fun findAll(): LedgerResult<List<Budget>> = LedgerResult.Success(budgets.toList())

    override fun findById(budgetId: String): LedgerResult<Budget> =
        budgets.firstOrNull { it.id == budgetId }
            ?.let { budget -> LedgerResult.Success(budget) }
            ?: LedgerResult.Failure(LedgerError.BudgetNotFound(budgetId))

    override fun create(budget: Budget): LedgerResult<Budget> {
        createCalls += 1
        budgets += budget
        return LedgerResult.Success(budget)
    }

    override fun update(budget: Budget): LedgerResult<Budget> {
        val index = budgets.indexOfFirst { it.id == budget.id }
        if (index < 0) return LedgerResult.Failure(LedgerError.BudgetNotFound(budget.id))
        budgets[index] = budget
        return LedgerResult.Success(budget)
    }
}

private class FakeCategoryRepository(
    private val category: Category,
) : CategoryRepository {
    override fun findAll(): LedgerResult<List<Category>> = LedgerResult.Success(listOf(category))

    override fun findById(categoryId: String): LedgerResult<Category> =
        if (category.id == categoryId) {
            LedgerResult.Success(category)
        } else {
            LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))
        }

    override fun create(category: Category): LedgerResult<Category> = LedgerResult.Success(category)

    override fun update(category: Category): LedgerResult<Category> = LedgerResult.Success(category)
}

private fun category(
    id: String,
    type: CategoryType,
): Category =
    Category(
        id = id,
        name = id,
        type = type,
        createdAt = 1L,
        updatedAt = 1L,
    )

private fun budget(
    id: String,
    categoryId: String?,
    periodType: BudgetPeriodType,
): Budget =
    Budget(
        id = id,
        name = id,
        amount = Money(10_000L, CurrencyCode("INR")),
        categoryId = categoryId,
        periodType = periodType,
        createdAt = 1L,
        updatedAt = 1L,
    )
